package com.beancounter.position.accumulation;

import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.model.Currency;
import com.beancounter.common.model.CurrencyPair;
import com.beancounter.common.model.FxRate;
import com.beancounter.common.model.MarketData;
import com.beancounter.common.model.MoneyValues;
import com.beancounter.common.utils.MathUtils;
import com.beancounter.position.model.FxReport;
import com.beancounter.position.model.Position;
import java.math.BigDecimal;
import java.util.Map;

public class MarketValue {
  private Gains gains = new Gains();
  private MathUtils mathUtils = new MathUtils();

  public void value(Position position,
                    FxReport fxReport,
                    MarketData marketData,
                    Map<CurrencyPair, FxRate> rates) {

    Currency trade = position.getAsset().getMarket().getCurrency();
    value(position, marketData, FxRate.ONE, Position.In.TRADE);
    value(position, marketData, rate(fxReport.getBase(), trade, rates), Position.In.BASE);
    value(position, marketData, rate(fxReport.getPortfolio(), trade, rates), Position.In.PORTFOLIO);

  }

  private void value(Position position, MarketData marketData, FxRate rate, Position.In in) {
    BigDecimal total = position.getQuantityValues().getTotal();
    MoneyValues moneyValues = position.getMoneyValue(in);
    moneyValues.setAsAt(marketData.getDate());
    moneyValues.setPrice(mathUtils.multiply(marketData.getClose(), rate.getRate()));
    moneyValues.setMarketValue(moneyValues.getPrice().multiply(total));
    gains.value(position, in);
  }

  private FxRate rate(Currency report, Currency trade, Map<CurrencyPair, FxRate> rates) {
    if (report.getCode().equals(trade.getCode())) {
      return FxRate.ONE;
    }
    FxRate fxRate = rates.get(CurrencyPair.from(report, trade));
    if (fxRate == null) {
      throw new BusinessException(String.format("No rate for %s:%s",
          report.getCode(),
          trade.getCode()
      ));
    }
    return fxRate;
  }

}
