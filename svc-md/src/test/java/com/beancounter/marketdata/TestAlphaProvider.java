package com.beancounter.marketdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.beancounter.common.model.MarketData;
import com.beancounter.marketdata.providers.alpha.AlphaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Market Data integration with AlphaVantage.co
 *
 * @author mikeh
 * @since 2019-03-03
 */
class TestAlphaProvider {

  @Test
  @VisibleForTesting
  void is_ResponseSerialized() throws Exception {
    ObjectMapper mapper = new AlphaService().getAlphaObjectMapper();
    File jsonFile = new ClassPathResource("alphavantage.json").getFile();
    MarketData marketData = mapper.readValue(jsonFile, MarketData.class);

    assertThat(marketData)
        .isNotNull()
        .hasFieldOrProperty("asset")
        .hasFieldOrProperty("date")
        .hasFieldOrPropertyWithValue("open", new BigDecimal("112.0400"))
        .hasFieldOrPropertyWithValue("high", new BigDecimal("112.8800"))
        .hasFieldOrPropertyWithValue("low", new BigDecimal("111.7300"))
        .hasFieldOrPropertyWithValue("close", new BigDecimal("112.0300"));

  }
}
