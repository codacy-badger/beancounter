package com.beancounter.client.integ;

import static com.beancounter.client.integ.ShareSightTradeTest.getRow;
import static com.beancounter.common.utils.PortfolioUtils.getPortfolio;
import static org.assertj.core.api.Assertions.assertThat;

import com.beancounter.client.config.ClientConfig;
import com.beancounter.client.ingest.TrnAdapter;
import com.beancounter.client.sharesight.ShareSightConfig;
import com.beancounter.client.sharesight.ShareSightDividendAdapter;
import com.beancounter.client.sharesight.ShareSightFactory;
import com.beancounter.client.sharesight.ShareSightRowAdapter;
import com.beancounter.common.input.TrnInput;
import com.beancounter.common.input.TrustedTrnImportRequest;
import com.beancounter.common.model.Portfolio;
import com.beancounter.common.model.TrnType;
import com.beancounter.common.utils.CurrencyUtils;
import com.beancounter.common.utils.MathUtils;
import com.github.tomakehurst.wiremock.common.Json;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("infile")
@AutoConfigureStubRunner(
    stubsMode = StubRunnerProperties.StubsMode.LOCAL,
    ids = "org.beancounter:svc-data:+:stubs:10999")
@SpringBootTest(classes = {ShareSightConfig.class, ClientConfig.class})
public class ShareSightRatesInTrn {
  private static final Logger log = LoggerFactory.getLogger(ShareSightRatesInTrn.class);
  private final CurrencyUtils currencyUtils = new CurrencyUtils();
  @Autowired
  private ShareSightFactory shareSightFactory;
  @Autowired
  private ShareSightConfig shareSightConfig;
  @Autowired
  private ShareSightRowAdapter shareSightRowProcessor;

  @BeforeEach
  void is_IgnoreRatesDefaultCorrect() {
    // Assumptions for all tests in this class
    assertThat(shareSightConfig.isCalculateRates()).isFalse();
    assertThat(shareSightConfig.isCalculateAmount()).isFalse();
  }

  @Test
  void is_DividendRowWithFxConverted() {
    List<String> row = new ArrayList<>();

    // Portfolio is in NZD
    Portfolio portfolio = getPortfolio("TEST");
    assertThat(portfolio).isNotNull();

    // Trade is in USD
    row.add(ShareSightDividendAdapter.id, "ABC");
    row.add(ShareSightDividendAdapter.code, "ABBV.NYS");
    row.add(ShareSightDividendAdapter.name, "Test Asset");
    row.add(ShareSightDividendAdapter.date, "21/01/2019");
    String rate = "0.8074"; // Sharesight Trade to Reference Rate
    row.add(ShareSightDividendAdapter.fxRate, rate);
    row.add(ShareSightDividendAdapter.currency, "USD"); // TradeCurrency
    row.add(ShareSightDividendAdapter.net, "15.85");
    row.add(ShareSightDividendAdapter.tax, "0");
    row.add(ShareSightDividendAdapter.gross, "15.85");
    row.add(ShareSightDividendAdapter.comments, "Test Comment");

    TrnAdapter dividends = shareSightFactory.adapter(row);
    TrustedTrnImportRequest trustedTrnImportRequest = new TrustedTrnImportRequest(portfolio, row);

    TrnInput trn = dividends.from(trustedTrnImportRequest);

    BigDecimal fxRate = new BigDecimal(rate);
    assertThat(trn)
        // Id comes from svc-data/contracts/assets
        .hasFieldOrPropertyWithValue("callerRef.callerId", "ABC")
        .hasFieldOrPropertyWithValue("assetId", "BguoVZpoRxWeWrITp7DEuw")
        .hasFieldOrPropertyWithValue("tradeCashRate", fxRate)
        .hasFieldOrPropertyWithValue("tradeAmount",
            MathUtils.multiply(new BigDecimal("15.85"), fxRate))
        .hasFieldOrPropertyWithValue("cashAmount",
            MathUtils.multiply(new BigDecimal("15.85"), fxRate))
        .hasFieldOrPropertyWithValue("tax", BigDecimal.ZERO)
        .hasFieldOrPropertyWithValue("comments", "Test Comment")
        .hasFieldOrPropertyWithValue("tradeCurrency", "USD")

        .hasFieldOrProperty("tradeDate")
    ;

  }

  @Test
  void is_TradeRowWithFxConverted() throws Exception {

    List<String> row = getRow("buy", "0.8988", "2097.85");
    // Portfolio is in NZD
    Portfolio portfolio = getPortfolio("Test", currencyUtils.getCurrency("NZD"));
    // System base currency
    TrustedTrnImportRequest trustedTrnImportRequest = new TrustedTrnImportRequest(portfolio, row);

    TrnInput trn = shareSightRowProcessor.transform(trustedTrnImportRequest);

    log.info(Json.getObjectMapper().writeValueAsString(trn));
    assertThat(trn)
        .hasFieldOrPropertyWithValue("trnType", TrnType.BUY)
        .hasFieldOrPropertyWithValue("quantity", new BigDecimal(10))
        .hasFieldOrPropertyWithValue("price", new BigDecimal("12.23"))
        .hasFieldOrPropertyWithValue("fees", new BigDecimal("14.45"))
        .hasFieldOrPropertyWithValue("tradeAmount",
            MathUtils.multiply(new BigDecimal("2097.85"), new BigDecimal("0.8988")))
        .hasFieldOrPropertyWithValue("comments", "Test Comment")
        .hasFieldOrProperty("tradeCurrency")
        .hasFieldOrPropertyWithValue("tradeCashRate", new BigDecimal("0.8988"))
        .hasFieldOrProperty("tradeDate")
    ;

  }


}
