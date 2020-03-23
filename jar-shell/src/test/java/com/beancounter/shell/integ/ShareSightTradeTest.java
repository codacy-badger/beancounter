package com.beancounter.shell.integ;

import static com.beancounter.common.utils.CurrencyUtils.getCurrency;
import static com.beancounter.common.utils.PortfolioUtils.getPortfolio;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.beancounter.client.PortfolioService;
import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.input.TrnInput;
import com.beancounter.common.model.Portfolio;
import com.beancounter.common.model.TrnType;
import com.beancounter.common.utils.MathUtils;
import com.beancounter.shell.ingest.Filter;
import com.beancounter.shell.ingest.TrnAdapter;
import com.beancounter.shell.sharesight.ShareSightConfig;
import com.beancounter.shell.sharesight.ShareSightFactory;
import com.beancounter.shell.sharesight.ShareSightRowAdapter;
import com.beancounter.shell.sharesight.ShareSightService;
import com.beancounter.shell.sharesight.ShareSightTradeAdapter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.test.context.ActiveProfiles;

/**
 * Sharesight Transaction to BC model.Transaction.
 *
 * @author mikeh
 * @since 2019-02-12
 */
@Slf4j
@ActiveProfiles("test")
@AutoConfigureStubRunner(
    stubsMode = StubRunnerProperties.StubsMode.LOCAL,
    ids = "org.beancounter:svc-data:+:stubs:10999")
@SpringBootTest(classes = {ShareSightConfig.class})
class ShareSightTradeTest {

  @Autowired
  private ShareSightRowAdapter shareSightRowProcessor;

  @Autowired
  private ShareSightService shareSightService;

  @Autowired
  private PortfolioService portfolioService;

  @Autowired
  private ShareSightFactory shareSightFactory;

  static List<Object> getRow(String tranType, String fxRate, String tradeAmount) {
    return getRow("AMP", "ASX", tranType, fxRate, tradeAmount);
  }

  static List<Object> getRow(String code, String market,
                             String tranType,
                             String fxRate,
                             String tradeAmount) {
    List<Object> row = new ArrayList<>();

    row.add(ShareSightTradeAdapter.market, market);
    row.add(ShareSightTradeAdapter.code, code);
    row.add(ShareSightTradeAdapter.name, "Test Asset");
    row.add(ShareSightTradeAdapter.type, tranType);
    row.add(ShareSightTradeAdapter.date, "21/01/2019");
    row.add(ShareSightTradeAdapter.quantity, "10");
    row.add(ShareSightTradeAdapter.price, "12.23");
    row.add(ShareSightTradeAdapter.brokerage, "12.99");
    row.add(ShareSightTradeAdapter.currency, "AUD");
    row.add(ShareSightTradeAdapter.fxRate, fxRate);
    row.add(ShareSightTradeAdapter.value, tradeAmount);
    row.add(ShareSightTradeAdapter.comments, "Test Comment");
    return row;
  }

  @Test
  void is_SplitTransformerFoundForRow() {
    List<Object> row = new ArrayList<>();
    row.add(ShareSightTradeAdapter.market, "ASX");
    row.add(ShareSightTradeAdapter.code, "SLB");
    row.add(ShareSightTradeAdapter.name, "Test Asset");
    row.add(ShareSightTradeAdapter.type, "split");
    row.add(ShareSightTradeAdapter.date, "21/01/2019");
    row.add(ShareSightTradeAdapter.quantity, "10");
    row.add(ShareSightTradeAdapter.price, "12.23");
    row.add(ShareSightTradeAdapter.brokerage, "12.99");
    row.add(ShareSightTradeAdapter.currency, "AUD");

    TrnAdapter trnAdapter = shareSightFactory.adapter(row);
    assertThat(trnAdapter).isInstanceOf(ShareSightTradeAdapter.class);
  }

  @Test
  void is_RowWithoutFxConverted() {

    List<Object> row = getRow("buy", "0.0", "2097.85");
    List<List<Object>> values = new ArrayList<>();
    values.add(row);

    // Portfolio is in NZD
    Portfolio portfolio = portfolioService.getPortfolioByCode("TEST");

    TrnInput trn = shareSightRowProcessor.transform(portfolio, values, "Blah")
        .iterator().next();

    assertThat(trn)
        .hasFieldOrPropertyWithValue("trnType", TrnType.BUY)
        .hasFieldOrPropertyWithValue("quantity", new BigDecimal(10))
        .hasFieldOrPropertyWithValue("price", new BigDecimal("12.23"))
        .hasFieldOrPropertyWithValue("fees", new BigDecimal("12.99"))

        .hasFieldOrPropertyWithValue("tradeAmount",
            MathUtils.multiply(new BigDecimal("2097.85"), new BigDecimal("0")))
        .hasFieldOrPropertyWithValue("comments", "Test Comment")
        .hasFieldOrPropertyWithValue("tradeCurrency", "AUD")
        .hasFieldOrPropertyWithValue("tradeCashRate", null)
        .hasFieldOrProperty("tradeDate")
    ;

  }

  @Test
  void is_RowWithNoCommentTransformed() {

    List<Object> row = getRow("buy", "0.8988", "2097.85");
    row.remove(ShareSightTradeAdapter.comments);
    List<List<Object>> values = new ArrayList<>();
    values.add(row);

    TrnInput trn =
        shareSightRowProcessor.transform(
            getPortfolio("Test", getCurrency("NZD")), values, "twee")
            .iterator().next();

    assertThat(trn)
        .hasFieldOrPropertyWithValue("TrnType", TrnType.BUY)
        .hasFieldOrPropertyWithValue("quantity", new BigDecimal(10))
        .hasFieldOrPropertyWithValue("price", new BigDecimal("12.23"))
        .hasFieldOrPropertyWithValue("comments", null)
        .hasFieldOrProperty("tradeDate");

  }

  @Test
  void is_RowFilterWorking() {

    List<Object> inFilter = getRow("buy", "0.8988", "2097.85");
    List<Object> notInFilter = getRow(
        "ABC",
        "MOCK",
        "buy",
        "0.8988",
        "2097.85"
    );
    inFilter.remove(ShareSightTradeAdapter.comments);
    List<List<Object>> values = new ArrayList<>();
    values.add(inFilter);
    values.add(notInFilter);
    shareSightService.setFilter(new Filter("AMP"));
    Collection<TrnInput> trns =
        shareSightRowProcessor.transform(
            getPortfolio("Test", getCurrency("NZD")),
            values,
            "twee");

    assertThat(trns).hasSize(1);
    TrnInput trn = trns.iterator().next();
    assertThat(trn)
        .hasFieldOrPropertyWithValue("TrnType", TrnType.BUY)
        .hasFieldOrPropertyWithValue("quantity", new BigDecimal(10))
        .hasFieldOrPropertyWithValue("price", new BigDecimal("12.23"))
        .hasFieldOrPropertyWithValue("comments", null)
        .hasFieldOrProperty("tradeDate")
    ;

  }

  @Test
  void is_SplitTransactionTransformed() {

    List<Object> row = getRow("split", null, null);
    List<List<Object>> values = new ArrayList<>();
    values.add(row);
    Portfolio portfolio = getPortfolio("Test", getCurrency("NZD"));
    TrnInput trn = shareSightRowProcessor
        .transform(portfolio, values, "blah").iterator().next();

    assertThat(trn)
        .hasFieldOrPropertyWithValue("TrnType", TrnType.SPLIT)
        .hasFieldOrPropertyWithValue("quantity", new BigDecimal("10"))
        .hasFieldOrPropertyWithValue("price", new BigDecimal("12.23"))
        .hasFieldOrPropertyWithValue("tradeAmount", BigDecimal.ZERO)
        .hasFieldOrPropertyWithValue("comments", "Test Comment")
        .hasFieldOrPropertyWithValue("tradeCurrency", "AUD")
        .hasFieldOrProperty("tradeDate")
    ;

  }

  @Test
  void is_IllegalNumberFound() {
    List<Object> row = getRow("buy", "0.8988e", "2097.85");
    List<List<Object>> values = new ArrayList<>();
    values.add(row);
    assertThrows(BusinessException.class, () ->
        shareSightRowProcessor.transform(getPortfolio("Test", getCurrency("NZD")),
            values, "twee"));
  }

  @Test
  void is_IllegalDateFound() {
    List<Object> row = getRow("buy", "0.8988", "2097.85");
    row.add(ShareSightTradeAdapter.date, "21/01/2019'");
    List<List<Object>> values = new ArrayList<>();
    values.add(row);

    assertThrows(BusinessException.class, () ->
        shareSightRowProcessor.transform(getPortfolio("Test", getCurrency("NZD")),
            values, "twee"));
  }

}
