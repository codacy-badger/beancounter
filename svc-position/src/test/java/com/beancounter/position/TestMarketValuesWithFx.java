package com.beancounter.position;

import static org.assertj.core.api.Assertions.assertThat;

import com.beancounter.common.model.Asset;
import com.beancounter.common.model.FxRate;
import com.beancounter.common.model.IsoCurrencyPair;
import com.beancounter.common.model.MarketData;
import com.beancounter.common.model.MoneyValues;
import com.beancounter.common.model.Portfolio;
import com.beancounter.common.model.Position;
import com.beancounter.common.model.Positions;
import com.beancounter.common.model.Trn;
import com.beancounter.common.model.TrnType;
import com.beancounter.common.utils.AssetUtils;
import com.beancounter.common.utils.CurrencyUtils;
import com.beancounter.common.utils.MathUtils;
import com.beancounter.position.accumulation.AccumulationStrategy;
import com.beancounter.position.accumulation.BuyBehaviour;
import com.beancounter.position.accumulation.SellBehaviour;
import com.beancounter.position.valuation.Gains;
import com.beancounter.position.valuation.MarketValue;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestMarketValuesWithFx {
  private final CurrencyUtils currencyUtils = new CurrencyUtils();

  @Test
  void is_MarketValue() {
    Portfolio portfolio = Portfolio.builder()
        .code("MV")
        .currency(currencyUtils.getCurrency("NZD"))
        .build();

    Asset asset = AssetUtils.getAsset("Test", "ABC");
    BigDecimal simpleRate = new BigDecimal("0.20");

    Trn buyTrn = Trn.builder()
        .trnType(TrnType.BUY)
        .asset(asset)
        .tradeAmount(new BigDecimal("2000.00"))
        .tradeCurrency(asset.getMarket().getCurrency())
        .tradePortfolioRate(simpleRate)
        .quantity(new BigDecimal("100")).build();

    BuyBehaviour buyBehaviour = new BuyBehaviour();

    Position position = Position.builder().asset(asset).build();
    buyBehaviour.accumulate(buyTrn, portfolio, position);
    Positions positions = new Positions(portfolio);
    positions.add(position);

    MarketData marketData = MarketData.builder()
        .asset(asset)
        .close(new BigDecimal("10.00"))
        .build();

    Map<IsoCurrencyPair, FxRate> fxRateMap = new HashMap<>();

    fxRateMap.put(
        IsoCurrencyPair.from(
            portfolio.getCurrency(),
            asset.getMarket().getCurrency()
        ),
        FxRate.builder().rate(simpleRate).build());

    // Revalue based on marketData prices
    new MarketValue(new Gains()).value(positions, marketData, fxRateMap);

    assertThat(position.getMoneyValues(Position.In.TRADE))
        .isEqualToComparingFieldByField(MoneyValues.builder()
            .price(marketData.getClose())
            .averageCost(new BigDecimal("20.00"))
            .currency(currencyUtils.getCurrency("USD"))
            .purchases(buyTrn.getTradeAmount())
            .costBasis(buyTrn.getTradeAmount())
            .costValue(buyTrn.getTradeAmount())
            .totalGain(new BigDecimal("-1000.00"))
            .unrealisedGain(new BigDecimal("-1000.00"))
            .marketValue(MathUtils.multiply(buyTrn.getQuantity(), marketData.getClose()))
            .build());


    assertThat(position.getMoneyValues(Position.In.BASE))
        .isEqualToComparingFieldByField(MoneyValues.builder()
            .price(marketData.getClose())
            .averageCost(new BigDecimal("20.00"))
            .currency(currencyUtils.getCurrency("USD"))
            .purchases(buyTrn.getTradeAmount())
            .costBasis(buyTrn.getTradeAmount())
            .costValue(buyTrn.getTradeAmount())
            .totalGain(new BigDecimal("-1000.00"))
            .unrealisedGain(new BigDecimal("-1000.00"))
            .marketValue(MathUtils.multiply(buyTrn.getQuantity(), marketData.getClose()))
            .build());

    // Basically 10% of the non-portfolio values due to the simpleRate value used
    assertThat(position.getMoneyValues(Position.In.PORTFOLIO))
        .isEqualToComparingFieldByField(MoneyValues.builder()
            .currency(portfolio.getCurrency())
            .costBasis(new BigDecimal("10000.00"))
            .purchases(new BigDecimal("10000.00"))
            .price(new BigDecimal("2.00"))
            .marketValue(new BigDecimal("200.00"))
            .averageCost(new BigDecimal("100.00"))
            .costValue(new BigDecimal("10000.00"))
            .unrealisedGain(new BigDecimal("-9800.00"))
            .totalGain(new BigDecimal("-9800.00"))
            .build());

  }

  @Test
  void is_GainsOnSell() {
    Portfolio portfolio = Portfolio.builder()
        .code("MV")
        .currency(currencyUtils.getCurrency("NZD"))
        .build();

    Asset asset = AssetUtils.getAsset("Test", "ABC");
    Map<IsoCurrencyPair, FxRate> fxRateMap = new HashMap<>();

    BigDecimal simpleRate = new BigDecimal("0.20");

    fxRateMap.put(
        IsoCurrencyPair.from(
            portfolio.getCurrency(),
            asset.getMarket().getCurrency()),
        FxRate.builder()
            .rate(simpleRate).build());

    Trn buyTrn = Trn.builder()
        .trnType(TrnType.BUY)
        .asset(asset)
        .tradeAmount(new BigDecimal("2000.00"))
        .tradeCurrency(asset.getMarket().getCurrency())
        .tradePortfolioRate(simpleRate)
        .quantity(new BigDecimal("100")).build();

    AccumulationStrategy buyBehaviour = new BuyBehaviour();

    Position position = Position.builder().asset(asset).build();
    buyBehaviour.accumulate(buyTrn, portfolio, position);
    Positions positions = new Positions(portfolio);
    positions.add(position);

    Trn sellTrn = Trn.builder()
        .trnType(TrnType.SELL)
        .asset(asset)
        .tradeAmount(new BigDecimal("3000.00"))
        .tradeCurrency(asset.getMarket().getCurrency())
        .tradePortfolioRate(simpleRate)
        .quantity(new BigDecimal("100")).build();

    AccumulationStrategy sellBehaviour = new SellBehaviour();
    sellBehaviour.accumulate(sellTrn, portfolio, position);
    MarketData marketData = MarketData.builder()
        .asset(asset)
        .close(new BigDecimal("10.00"))
        .build();

    new MarketValue(new Gains()).value(positions, marketData, fxRateMap);

    assertThat(position.getMoneyValues(Position.In.TRADE))
        .isEqualToComparingFieldByField(
            MoneyValues.builder()
                .price(new BigDecimal("10.00"))
                .marketValue(new BigDecimal("0.00"))
                .averageCost(BigDecimal.ZERO)
                .currency(currencyUtils.getCurrency("USD"))
                .purchases(buyTrn.getTradeAmount())
                .sales(sellTrn.getTradeAmount())
                .costValue(BigDecimal.ZERO)
                .realisedGain(new BigDecimal("1000.00"))
                .unrealisedGain(BigDecimal.ZERO)
                .totalGain(new BigDecimal("1000.00"))
                .build());


    assertThat(position.getMoneyValues(Position.In.BASE))
        .isEqualToComparingFieldByField(
            MoneyValues.builder()
                .price(new BigDecimal("10.00"))
                .marketValue(new BigDecimal("0.00"))
                .averageCost(BigDecimal.ZERO)
                .currency(currencyUtils.getCurrency("USD"))
                .purchases(buyTrn.getTradeAmount())
                .sales(sellTrn.getTradeAmount())
                .costValue(BigDecimal.ZERO)
                .realisedGain(new BigDecimal("1000.00"))
                .unrealisedGain(BigDecimal.ZERO)
                .totalGain(new BigDecimal("1000.00"))
                .build());

    // Basically 10% of the non-portfolio values due to the simpleRate value used
    assertThat(position.getMoneyValues(Position.In.PORTFOLIO))
        .isEqualToComparingFieldByField(
            MoneyValues.builder()
                .currency(portfolio.getCurrency())
                .price(new BigDecimal("2.00"))
                .marketValue(new BigDecimal("0.00"))
                .averageCost(BigDecimal.ZERO)
                .purchases(MathUtils.divide(buyTrn.getTradeAmount(), simpleRate))
                .sales(MathUtils.divide(sellTrn.getTradeAmount(), simpleRate))
                .costValue(BigDecimal.ZERO)
                .realisedGain(new BigDecimal("5000.00"))
                .unrealisedGain(BigDecimal.ZERO)
                .totalGain(new BigDecimal("5000.00"))
                .build());

  }
}
