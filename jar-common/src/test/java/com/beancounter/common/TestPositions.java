package com.beancounter.common;

import static com.beancounter.common.utils.AssetUtils.getAsset;
import static com.beancounter.common.utils.CurrencyUtils.getCurrency;
import static com.beancounter.common.utils.PortfolioUtils.getPortfolio;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.beancounter.common.contracts.PositionResponse;
import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Currency;
import com.beancounter.common.model.DateValues;
import com.beancounter.common.model.MoneyValues;
import com.beancounter.common.model.Portfolio;
import com.beancounter.common.model.Position;
import com.beancounter.common.model.Positions;
import com.beancounter.common.model.QuantityValues;
import com.beancounter.common.model.Transaction;
import com.beancounter.common.utils.DateUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestPositions {

  @Test
  void is_PositionResponseChainSerializing() throws Exception {
    Map<Position.In, MoneyValues> moneyValuesMap = new HashMap<>();
    moneyValuesMap.put(Position.In.TRADE, MoneyValues.builder()
        .dividends(new BigDecimal(100d))
        .build());

    Positions positions = new Positions(Portfolio.builder()
        .code("T")
        .currency(Currency.builder().code("SGD").build())
        .build());

    Asset asset = getAsset("TEST", "TEST");
    positions.add(Position.builder()
        .asset(asset)
        .moneyValues(moneyValuesMap)
        .quantityValues(QuantityValues.builder()
            .purchased(new BigDecimal(200))
            .build())
        .build()
    );
    Position position = positions.get(asset);

    DateValues dateValues = DateValues.builder()
        .opened(DateUtils.today())
        .closed(DateUtils.today())
        .last(DateUtils.today())
        .build();

    position.setDateValues(dateValues);

    PositionResponse positionResponse = PositionResponse.builder().data(positions).build();
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(positionResponse);

    PositionResponse fromJson = mapper.readValue(json, PositionResponse.class);

    assertThat(fromJson).isEqualToComparingFieldByField(positionResponse);
  }

  @Test
  void is_DateValuesSetFromTransaction() {

    Asset asset = getAsset("Dates", "Code");

    Date tradeDate = DateUtils.getDate("2018-12-01");
    Transaction initial = Transaction.builder()
        .tradeDate(tradeDate)
        .portfolio(getPortfolio("CODE"))
        .asset(asset)
        .build();

    Positions positions = new Positions(getPortfolio("Twee"));
    Position position = positions.get(initial);
    assertThat(position.getDateValues())
        .hasFieldOrPropertyWithValue("opened", "2018-12-01");
  }

  @Test
  void is_GetPositionNonNull() {

    Positions positions = new Positions(getPortfolio("Test"));
    Asset asset = getAsset("TEST", "TEST");
    Position position = positions.get(asset);
    assertThat(position).isNotNull().hasFieldOrPropertyWithValue("asset", asset);

  }

  @Test
  void is_MoneyValuesFromPosition() {

    Position position = Position.builder()
        .asset(getAsset("Twee", "Twee"))
        .build();

    // Requesting a non existent MV.  Without a currency, it can't be created
    assertThat(position.getMoneyValue(Position.In.TRADE)).isNull();
    // Retrieve with a currency will create if missing
    assertThat(position.getMoneyValue(Position.In.TRADE, getCurrency("SGD")))
        .isNotNull()
        .hasFieldOrPropertyWithValue("currency", getCurrency("SGD"));
  }
}
