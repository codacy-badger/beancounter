package com.beancounter.client.integ;

import static org.assertj.core.api.Assertions.assertThat;

import com.beancounter.client.config.ClientConfig;
import com.beancounter.client.services.TrnService;
import com.beancounter.common.contracts.TrnResponse;
import com.beancounter.common.input.TrustedTrnQuery;
import com.beancounter.common.model.Currency;
import com.beancounter.common.model.Portfolio;
import com.beancounter.common.utils.DateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;

@AutoConfigureStubRunner(
    stubsMode = StubRunnerProperties.StubsMode.LOCAL,
    ids = "org.beancounter:svc-data:+:stubs:10999")
@ImportAutoConfiguration(ClientConfig.class)
@SpringBootTest(classes = ClientConfig.class)
public class TestTrnService {

  private static final Portfolio portfolio = Portfolio.builder()
      .id("TEST")
      .code("TEST")
      .name("NZD Portfolio")
      .currency(Currency.builder().symbol("$").name("Dollar").code("NZD").build())
      .base(Currency.builder().symbol("$").name("Dollar").code("USD").build())
      .build();
  @Autowired
  private TrnService trnService;

  @Test
  void is_TrnsReturnedForPortfolioId() {
    TrnResponse trnResponse = trnService.query(portfolio);
    assertThat(trnResponse).isNotNull().hasFieldOrProperty("data");
    assertThat(trnResponse.getData()).isNotEmpty();// Don't care about the contents here.
  }

  @Test
  void is_TrnsReturnedForPortfolioAssetId() {
    TrustedTrnQuery query = TrustedTrnQuery.builder()
        .assetId("KMI")
        .portfolio(portfolio)
        .tradeDate(new DateUtils().getDate("2020-05-01"))
        .build();
    TrnResponse queryResults = trnService.query(query);
    assertThat(queryResults).isNotNull().hasFieldOrProperty("data");
    assertThat(queryResults.getData()).isNotEmpty();// Don't care about the contents here.
  }


}