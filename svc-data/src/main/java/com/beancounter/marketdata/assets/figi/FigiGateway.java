package com.beancounter.marketdata.assets.figi;

import java.util.Collection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Api calls to OpenFIGI.
 *
 * @author mikeh
 * @since 2019-03-03
 */
@FeignClient(
    name = "figi",
    url = "${beancounter.marketdata.provider.FIGI.url:https://api.openfigi.com}")
@Configuration
public interface FigiGateway {
  // https://bsym.bloomberg.com/api#post-v2-search
  @RequestMapping(
      method = RequestMethod.POST,
      value = "/v2/mapping",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Collection<FigiResponse> search(Collection<FigiSearch> searchBody,
                                  @RequestHeader("X-OPENFIGI-APIKEY") String apiKey);

}
