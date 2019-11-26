package com.beancounter.ingest.service;

import com.beancounter.common.contracts.CurrencyResponse;
import com.beancounter.common.contracts.FxRequest;
import com.beancounter.common.contracts.FxResponse;
import com.beancounter.common.contracts.MarketResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;


@FeignClient(name = "marketData",
    url = "${marketdata.url:http://localhost:9510/api}")
@Configuration
public interface BcGateway {
  @PostMapping(value = "/fx",
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  FxResponse getRates(FxRequest fxRequest);

  @GetMapping(value = "/currencies", produces = {MediaType.APPLICATION_JSON_VALUE})
  CurrencyResponse getCurrencies();

  @GetMapping(value = "/markets", produces = {MediaType.APPLICATION_JSON_VALUE})
  MarketResponse getMarkets();

}
