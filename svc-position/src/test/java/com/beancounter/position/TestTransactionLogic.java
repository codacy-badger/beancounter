package com.beancounter.position;

import static com.beancounter.common.helper.AssetHelper.getAsset;
import static com.beancounter.position.TestUtils.convert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Portfolio;
import com.beancounter.common.model.Transaction;
import com.beancounter.common.model.TrnType;
import com.beancounter.position.config.TransactionConfiguration;
import com.beancounter.position.model.Position;
import com.beancounter.position.model.Positions;
import com.beancounter.position.service.Accumulator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

/**
 * Test rules that would prevent a transaction from accumulating.
 */
class TestTransactionLogic {
  /**
   * Transactions should be ordered.  If the date is ==, then it will be accepted but
   * unordered transactions will result in an Exception being thrown
   */
  @Test
  void unorderedTransactionsError() {
    Asset apple = getAsset("AAPL", "NASDAQ");
    Positions positions = new Positions(Portfolio.builder().code("TEST").build());

    Position position = positions.get(apple);

    assertThat(position)
        .isNotNull();

    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minus(-1, ChronoUnit.DAYS);

    Accumulator accumulator = new Accumulator(new TransactionConfiguration());

    Transaction buyYesterday = Transaction.builder()
        .trnType(TrnType.BUY)
        .asset(apple)
        .tradeAmount(new BigDecimal(2000))
        .tradeDate(convert(yesterday))
        .quantity(new BigDecimal(100)).build();

    Transaction buyToday = Transaction.builder()
        .trnType(TrnType.BUY)
        .asset(apple)
        .tradeAmount(new BigDecimal(2000))
        .tradeDate(convert(today))
        .quantity(new BigDecimal(100)).build();

    positions.add(position);
    position = accumulator.accumulate(buyYesterday, position);

    Position finalPosition = position;
    assertThrows(BusinessException.class, () -> accumulator.accumulate(buyToday, finalPosition));


  }
}