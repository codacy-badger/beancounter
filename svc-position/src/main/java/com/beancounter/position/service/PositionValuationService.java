package com.beancounter.position.service;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.beancounter.common.contracts.FxResponse;
import com.beancounter.common.contracts.PriceRequest;
import com.beancounter.common.contracts.PriceResponse;
import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.input.AssetInput;
import com.beancounter.common.model.FxRate;
import com.beancounter.common.model.IsoCurrencyPair;
import com.beancounter.common.model.MarketData;
import com.beancounter.common.model.MoneyValues;
import com.beancounter.common.model.Position;
import com.beancounter.common.model.Positions;
import com.beancounter.common.model.Totals;
import com.beancounter.common.utils.MathUtils;
import com.beancounter.position.model.ValuationData;
import com.beancounter.position.utils.FxUtils;
import com.beancounter.position.valuation.MarketValue;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PositionValuationService {
  private final AsyncMdService asyncMdService;
  private final MarketValue marketValue;
  private final FxUtils fxUtils;

  PositionValuationService(
      AsyncMdService asyncMdService,
      MarketValue marketValue,
      FxUtils fxUtils) {
    this.asyncMdService = asyncMdService;
    this.marketValue = marketValue;
    this.fxUtils = fxUtils;
  }

  public Positions value(Positions positions, Collection<AssetInput> assets) {
    if (assets.isEmpty()) {
      return positions; // Nothing to value
    }
    log.debug("Requested valuation of {} positions for {}...",
        positions.getPositions().size(),
        positions.getPortfolio().getCode());

    // Set market data into the positions
    // There's an issue here that without a price, gains are not computed
    ValuationData valuationData = getValuationData(positions, assets);
    if (valuationData.getPriceResponse() == null) {
      log.info("No prices found on date {}", positions.getAsAt());
      return positions; // Prevent NPE
    }

    FxResponse fxResponse = valuationData.getFxResponse();
    if (fxResponse == null) {
      throw new BusinessException("Unable to obtain FX Rates ");
    }

    Map<IsoCurrencyPair, FxRate> rates = fxResponse.getData().getRates();
    Totals baseTotals = new Totals();
    Totals refTotals = new Totals();
    if (valuationData.getPriceResponse() != null) {
      valuationData.getPriceResponse().getData();

      for (MarketData marketData : valuationData.getPriceResponse().getData()) {
        Position position = marketValue.value(positions, marketData, rates);
        BigDecimal baseAmount = position.getMoneyValues(
            Position.In.BASE, positions.getPortfolio().getBase()).getMarketValue();

        baseTotals.setTotal(baseTotals.getTotal().add(baseAmount));

        BigDecimal refAmount = position.getMoneyValues(
            Position.In.PORTFOLIO, position.getAsset().getMarket().getCurrency()).getMarketValue();
        refTotals.setTotal(refTotals.getTotal().add(refAmount));

      }
    }
    positions.setTotal(Position.In.BASE, baseTotals);
    for (Position position : positions.getPositions().values()) {
      MoneyValues moneyValues = position.getMoneyValues(Position.In.BASE);
      assert moneyValues != null;
      moneyValues.setWeight(
          MathUtils.percent(moneyValues.getMarketValue(), baseTotals.getTotal()));

      moneyValues = position.getMoneyValues(Position.In.PORTFOLIO);
      assert moneyValues != null;
      moneyValues.setWeight(
          MathUtils.percent(moneyValues.getMarketValue(), refTotals.getTotal()));

      moneyValues = position.getMoneyValues(Position.In.TRADE);
      assert moneyValues != null;
      moneyValues.setWeight(
          MathUtils.percent(moneyValues.getMarketValue(), refTotals.getTotal()));

    }
    log.debug("Completed valuation of {} positions for {}...",
        positions.getPositions().size(),
        positions.getPortfolio().getCode());

    return positions;
  }

  @SneakyThrows
  private ValuationData getValuationData(Positions positions, Collection<AssetInput> assets) {
    CompletableFuture<FxResponse> futureFxResponse =
        asyncMdService.getFxData(fxUtils.buildRequest(
            positions.getPortfolio().getBase(),
            positions));

    CompletableFuture<PriceResponse> futurePriceResponse =
        asyncMdService.getMarketData(
            new PriceRequest(positions.getAsAt(), assets));

    return new ValuationData(futurePriceResponse.get(180, SECONDS),
        futureFxResponse.get(30, SECONDS));
  }

}
