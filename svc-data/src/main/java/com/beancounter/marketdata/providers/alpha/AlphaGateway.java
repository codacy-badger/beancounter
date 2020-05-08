package com.beancounter.marketdata.providers.alpha;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Api calls to alphavantage.
 *
 * @author mikeh
 * @since 2019-03-03
 */
@Configuration
@FeignClient(
    name = "alphaVantage",
    url = "${beancounter.marketdata.provider.ALPHA.url:https://www.alphavantage.co}")

public interface AlphaGateway {

  @RequestMapping(
      method = RequestMethod.GET,
      headers = {"Content-Type: text/plain"},
      value = "/query?function=GLOBAL_QUOTE&symbol={assetId}&apikey={apiKey}"
  )
  String getCurrent(@PathVariable("assetId") String assetId,
                    @PathVariable("apiKey") String apiKey);

  @RequestMapping(
      method = RequestMethod.GET,
      headers = {"Content-Type: text/plain"},
      value = "/query?function=TIME_SERIES_DAILY&symbol={assetId}&apikey={apiKey}"
  )
  String getHistoric(@PathVariable("assetId") String assetId,
                     @PathVariable("apiKey") String apiKey);

  @RequestMapping(
      method = RequestMethod.GET,
      headers = {"Content-Type: text/plain"},
      value = "/query?function=SYMBOL_SEARCH&keywords={symbol}&apikey={apiKey}"
  )
  String search(@PathVariable("symbol") String symbol,
                @PathVariable("apiKey") String apiKey);
}
