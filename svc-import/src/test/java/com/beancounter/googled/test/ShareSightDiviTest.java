package com.beancounter.googled.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Currency;
import com.beancounter.common.model.Market;
import com.beancounter.common.model.Portfolio;
import com.beancounter.common.model.Transaction;
import com.beancounter.googled.config.ExchangeConfig;
import com.beancounter.googled.reader.Transformer;
import com.beancounter.googled.sharesight.ShareSightDivis;
import com.beancounter.googled.sharesight.ShareSightTrades;
import com.beancounter.googled.sharesight.ShareSightTransformers;
import com.beancounter.googled.sharesight.common.ShareSightHelper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;


/**
 * Sharesight Dividend converter to BC model..
 *
 * @author mikeh
 * @since 2019-02-12
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    ExchangeConfig.class,
    ShareSightTransformers.class,
    ShareSightDivis.class,
    ShareSightTrades.class,
    ShareSightHelper.class})
class ShareSightDiviTest {


  @Autowired
  private ShareSightTransformers shareSightTransformers;

  @Test
  void areCurrenciesResolvedForDividendTransaction() throws Exception {
    List<String> row = new ArrayList<>();

    // Portfolio is in NZD
    Portfolio portfolio = Portfolio.builder()
        .code("TEST")
        .currency(Currency.builder().code("NZD").build())
        .build();

    // System base currency
    Currency base = Currency.builder().code("USD").build();

    // Trade is in USD
    row.add(ShareSightDivis.code, "MO.NYS");
    row.add(ShareSightDivis.name, "Test Asset");
    row.add(ShareSightDivis.date, "21/01/2019");
    String rate = "0.8074"; // Sharesight FX Rate - transformer will convert to Trade CCY
    row.add(ShareSightDivis.fxRate, rate);
    row.add(ShareSightDivis.currency, "USD"); // TradeCurrency
    row.add(ShareSightDivis.net, "15.85");
    row.add(ShareSightDivis.tax, "0");
    row.add(ShareSightDivis.gross, "15.85");
    row.add(ShareSightDivis.comments, "Test Comment");

    Transformer dividends = shareSightTransformers.transformer(row);

    Transaction transaction = dividends.from(row, portfolio, base);

    Asset expectedAsset = Asset.builder()
        .code("MO")
        .market(Market.builder().code("NYSE").build())
        .build();

    BigDecimal fxRate = new BigDecimal(rate);
    assertThat(transaction)
        .hasFieldOrPropertyWithValue("asset", expectedAsset)
        .hasFieldOrPropertyWithValue("tradeRate", fxRate)
        .hasFieldOrPropertyWithValue("tradeAmount",
            new BigDecimal("15.85").multiply(fxRate).setScale(2, RoundingMode.HALF_UP))
        .hasFieldOrPropertyWithValue("tax", new BigDecimal("0.0000"))
        .hasFieldOrPropertyWithValue("comments", "Test Comment")
        .hasFieldOrPropertyWithValue("tradeCurrency", Currency.builder().code("USD").build())
        .hasFieldOrPropertyWithValue("baseCurrency", Currency.builder().code("USD").build())
        .hasFieldOrPropertyWithValue("portfolio", portfolio)

        .hasFieldOrProperty("tradeDate")
    ;

  }
}
