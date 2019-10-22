package com.beancounter.position;

import static com.beancounter.position.TestUtils.convert;
import static com.beancounter.position.TestUtils.getPortfolio;
import static org.assertj.core.api.Assertions.assertThat;

import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Portfolio;
import com.beancounter.common.model.Position;
import com.beancounter.common.model.Positions;
import com.beancounter.common.model.Transaction;
import com.beancounter.common.model.TrnType;
import com.beancounter.common.utils.AssetUtils;
import com.beancounter.position.accumulation.Buy;
import com.beancounter.position.accumulation.Sell;
import com.beancounter.position.accumulation.Split;
import com.google.common.annotations.VisibleForTesting;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Corporate Actions - Stock Splits.  These do not affect Cost.
 *
 * @author mikeh
 * @since 2019-02-20
 */
class TestStockSplits {

  @Test
  @VisibleForTesting
  void is_QuantityWorkingForSplit() {

    Asset apple = AssetUtils.getAsset("AAPL", "NASDAQ");

    Positions positions = new Positions(Portfolio.builder().code("TEST").build());

    Position position = positions.get(apple);

    assertThat(position)
        .isNotNull();

    LocalDate today = LocalDate.now();

    Transaction buyTrn = Transaction.builder()
        .trnType(TrnType.BUY)
        .asset(apple)
        .portfolio(getPortfolio("TEST"))
        .tradeAmount(new BigDecimal("2000"))
        .tradeDate(convert(today))
        .quantity(new BigDecimal("100")).build();

    Buy buy = new Buy();
    buy.value(buyTrn, position);

    assertThat(position.getQuantityValues())
        .hasFieldOrPropertyWithValue("total", new BigDecimal(100))
    ;

    BigDecimal costBasis = position.getMoneyValue(Position.In.TRADE).getCostBasis();

    Transaction stockSplit = Transaction.builder()
        .trnType(TrnType.SPLIT)
        .asset(apple)
        .portfolio(getPortfolio("TEST"))
        .tradeDate(convert(today))
        .quantity(new BigDecimal("7")).build();

    Split split = new Split();
    split.value(stockSplit, position);

    // 7 for one split
    assertThat(position.getQuantityValues())
        .hasFieldOrPropertyWithValue("total", new BigDecimal(700))
    ;

    assertThat(position.getMoneyValue(Position.In.TRADE))
        .hasFieldOrPropertyWithValue("costBasis", costBasis)
    ;

    // Another buy at the adjusted price
    buy.value(buyTrn, position);

    // 7 for one split
    assertThat(position.getQuantityValues())
        .hasFieldOrPropertyWithValue("total", new BigDecimal(800));

    Transaction sell = Transaction.builder()
        .trnType(TrnType.SELL)
        .asset(apple)
        .portfolio(getPortfolio("TEST"))
        .tradeAmount(new BigDecimal("2000"))
        .tradeDate(convert(today))
        .quantity(new BigDecimal("800")).build();

    // Sell the entire position
    new Sell().value(sell, position);

    assertThat(position.getQuantityValues())
        .hasFieldOrPropertyWithValue("total", BigDecimal.ZERO)
    ;

    // Repurchase; total should be equal to the quantity we just purchased
    buy.value(buyTrn, position);
    assertThat(position.getQuantityValues())
        .hasFieldOrPropertyWithValue("total", buyTrn.getQuantity())
    ;

  }
}
