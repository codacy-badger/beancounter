package com.beancounter.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.beancounter.common.contracts.FxPairResults;
import com.beancounter.common.contracts.FxRequest;
import com.beancounter.common.contracts.FxResponse;
import com.beancounter.common.model.Currency;
import com.beancounter.common.model.FxRate;
import com.beancounter.common.model.IsoCurrencyPair;
import com.beancounter.common.utils.RateCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestFx {

  @Test
  void is_RateRequestSerializing() throws Exception {
    IsoCurrencyPair pair = IsoCurrencyPair.builder().from("THIS").to("THAT").build();

    FxRequest fxRequest = FxRequest.builder().build();
    fxRequest.setTradeBase(pair);
    fxRequest.setTradePf(pair);
    fxRequest.setTradeCash(pair);
    fxRequest.add(null); // shouldn't carry null
    assertThat(fxRequest.getPairs()).size().isEqualTo(1);

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(fxRequest);
    FxRequest fromJson = mapper.readValue(json, FxRequest.class);

    assertThat(fromJson.getPairs()).hasSize(1);
    assertThat(fromJson.getTradeBase()).isNull();
    assertThat(fromJson.getTradePf()).isNull();
    assertThat(fromJson.getTradeCash()).isNull();

  }

  @Test
  void is_FxRequestPairsIgnoringDuplicates() {
    IsoCurrencyPair pair = IsoCurrencyPair.builder().from("THIS").to("THAT").build();
    FxRequest fxRequest = FxRequest.builder().build();
    fxRequest.add(pair);
    fxRequest.add(pair);
    assertThat(fxRequest.getPairs()).hasSize(1);
  }

  @Test
  void is_FxResultSerializing() throws Exception {
    IsoCurrencyPair nzdUsd = IsoCurrencyPair.builder().from("NZD").to("USD").build();
    IsoCurrencyPair usdUsd = IsoCurrencyPair.builder().from("USD").to("USD").build();

    Collection<IsoCurrencyPair> pairs = new ArrayList<>();
    pairs.add(nzdUsd);
    pairs.add(usdUsd);
    Map<String, FxRate> rateMap = new HashMap<>();
    rateMap.put("NZD", FxRate.builder()
        .to(Currency.builder().code("NZD").build())
        .from(Currency.builder().code("USD").build())
        .rate(BigDecimal.TEN).build());

    rateMap.put("USD", FxRate.builder()
        .to(Currency.builder().code("USD").build())
        .from(Currency.builder().code("USD").build())
        .rate(FxRate.ONE.getRate()).build());

    FxPairResults rateResults = RateCalculator.compute("2019/08/27", pairs, rateMap);

    ObjectMapper objectMapper = new ObjectMapper();
    FxResponse fxResponse = FxResponse.builder().data(rateResults).build();
    String json = objectMapper.writeValueAsString(fxResponse);
    FxResponse fromJson = objectMapper.readValue(json, FxResponse.class);
    assertThat(fromJson.getData()).isNotNull();
    assertThat(fromJson)
        .isNotNull()
        .isEqualToComparingFieldByField(fxResponse);

  }

  @Test
  void is_FxOneValid() {
    assertThat(FxRate.ONE.getRate()).isEqualTo(BigDecimal.ONE);
  }

}
