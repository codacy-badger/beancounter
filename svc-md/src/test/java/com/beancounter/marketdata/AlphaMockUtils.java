package com.beancounter.marketdata;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.springframework.http.MediaType;

/**
 * Alpha Vantage Mocking support.
 *
 * @author mikeh
 * @since 2019-03-09
 */
public class AlphaMockUtils {

  public static final String alphaContracts = "/contracts/alphavantage";
  private static ObjectMapper mapper = new ObjectMapper();

  /**
   * Convenience function to stub a response for ABC symbol.
   *
   * @param wireMockRule wiremock
   * @param jsonFile     response file to return
   * @throws IOException anything
   */
  public static void mockAlphaResponse(WireMockRule wireMockRule, File jsonFile)
      throws IOException {
    mockGetResponse(wireMockRule,
        "/query?function=TIME_SERIES_DAILY&symbol=ABC.AX&apikey=demo", jsonFile);
  }

  /**
   * Convenience function to stub a GET/200 response.
   *
   * @param wireMockRule wiremock
   * @param url          url to stub
   * @param jsonFile     response file to return
   * @throws IOException anything
   */
  public static void mockGetResponse(WireMockRule wireMockRule, String url, File jsonFile)
      throws IOException {
    wireMockRule
        .stubFor(
            get(urlEqualTo(url))
                .willReturn(aResponse()
                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(mapper.writeValueAsString(mapper.readValue(jsonFile, HashMap.class)))
                    .withStatus(200)));
  }


}
