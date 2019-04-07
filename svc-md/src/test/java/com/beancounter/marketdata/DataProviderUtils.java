package com.beancounter.marketdata;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import com.beancounter.common.model.Asset;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import org.springframework.http.MediaType;

/**
 * Convenience functions to reduce the LOC in a unit test.
 *
 * @author mikeh
 * @since 2019-03-09
 */
public class DataProviderUtils {

  private static ObjectMapper mapper = new ObjectMapper();

  /**
   * Convenience function to stub a response for ABC symbol.
   *
   * @param mockAlphaVantage wiremock
   * @param jsonFile response file to return
   * @throws IOException anything
   */
  public static void mockAlphaResponse(WireMockRule mockAlphaVantage, File jsonFile)
      throws IOException {
    mockAlphaVantage
        .stubFor(
            get(urlEqualTo("/query?function=TIME_SERIES_DAILY&symbol=ABC.AX&apikey=demo"))
                .willReturn(aResponse()
                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(mapper.writeValueAsString(mapper.readValue(jsonFile, HashMap.class)))
                    .withStatus(200)));
  }

  /**
   * Convenience function to stub a response for ABC symbol.
   *
   * @param wtdMock wiremock
   * @param assets asset codes required in the GET
   * @param jsonFile response file to return
   * @throws IOException anything
   */
  public static void mockWtdResponse(WireMockRule wtdMock, Collection<Asset> assets, File jsonFile)
      throws IOException {
    
    String assetArg = null;
    for (Asset asset : assets) {
      if (assetArg != null) {
        assetArg = assetArg + "%2C" + asset.getCode();
      } else {
        assetArg = asset.getCode();
      }
    }
    wtdMock
        .stubFor(
            get(urlEqualTo(
                "/api/v1/history_multi_single_day?symbol=" + assetArg + "&date=2019-03-11"
                    + "&api_token=demo"))
                .willReturn(aResponse()
                    .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(mapper.writeValueAsString(mapper.readValue(jsonFile, HashMap.class)))
                    .withStatus(200)));
  }
}