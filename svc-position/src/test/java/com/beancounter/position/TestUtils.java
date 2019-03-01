package com.beancounter.position;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.springframework.http.MediaType;

/**
 * General helper functions to support unit testing.
 *
 * @author mikeh
 * @since 2019-02-20
 */
class TestUtils {

  static ObjectMapper mapper = new ObjectMapper();

  public static ObjectMapper getMapper() {
    return mapper;
  }

  static Date convert(LocalDate localDate) {
    return java.util.Date.from(localDate.atStartOfDay()
        .atZone(ZoneId.systemDefault())
        .toInstant());
  }

  static void mockMarketData(WireMockRule mockMarketData, String requestBody, String responseBody) {
    mockMarketData
        .stubFor(
            post(urlPathEqualTo("/"))
                .withRequestBody(equalToJson(requestBody))
                .willReturn(aResponse()
                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(responseBody)
                    .withStatus(200)));
  }

}
