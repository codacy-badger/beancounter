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
    name = "alphavantage",
    url = "${com.beancounter.marketdata.provider.alpha.url:https://www.alphavantage.co/}")
public interface AlphaRequest {

  @RequestMapping(
      method = RequestMethod.GET,
      value = "query?function=TIME_SERIES_DAILY"
      + "&symbol={assetId}&apikey={apiKey}"

  )
  String getMarketData(@PathVariable("assetId") String assetId,
                              @PathVariable("apiKey") String apiKey);

}
