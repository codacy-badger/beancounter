package com.beancounter.marketdata.integ;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.beancounter.auth.common.TokenUtils;
import com.beancounter.auth.server.AuthorityRoleConverter;
import com.beancounter.common.contracts.FxPairResults;
import com.beancounter.common.contracts.FxRequest;
import com.beancounter.common.contracts.FxResponse;
import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.model.FxRate;
import com.beancounter.common.model.IsoCurrencyPair;
import com.beancounter.common.model.SystemUser;
import com.beancounter.common.utils.DateUtils;
import com.beancounter.marketdata.providers.fxrates.EcbDate;
import com.beancounter.marketdata.utils.AlphaMockUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Tag("slow")
class FxMvcTests {

  private static WireMockRule mockInternet;
  private ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private WebApplicationContext context;
  private MockMvc mockMvc;
  private AuthorityRoleConverter authorityRoleConverter = new AuthorityRoleConverter();
  private Jwt token;
  private DateUtils dateUtils = new DateUtils();

  @Autowired
  void mockServices() {
    // ToDo: Figure out RandomPort + Feign.  Config issues :(
    if (mockInternet == null) {
      mockInternet = new WireMockRule(options().port(7777));
      mockInternet.start();
    }

    this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();

    SystemUser user = SystemUser.builder()
        .id("user")
        .email("user@testing.com")
        .build();
    token = TokenUtils.getUserToken(user);
    TestRegistrationMvc.registerUser(mockMvc, token);

  }

  @Test
  void is_FxRateResultsObjectReturned() throws Exception {
    File rateResponse = new ClassPathResource("contracts/ecb/fx-current-rates.json").getFile();
    AlphaMockUtils.mockGetResponse(
        mockInternet,
        // Matches all supported currencies
        "/2019-08-27?base=USD&symbols=AUD%2CSGD%2CEUR%2CGBP%2CUSD%2CNZD",
        rateResponse);
    String date = "2019-08-27";
    IsoCurrencyPair nzdUsd = IsoCurrencyPair.builder().from("NZD").to("USD").build();
    IsoCurrencyPair usdNzd = IsoCurrencyPair.builder().from("USD").to("NZD").build();
    IsoCurrencyPair usdUsd = IsoCurrencyPair.builder().from("USD").to("USD").build();
    IsoCurrencyPair nzdNzd = IsoCurrencyPair.builder().from("NZD").to("NZD").build();

    FxRequest fxRequest = FxRequest.builder().rateDate(date).build();
    fxRequest.add(nzdUsd).add(usdNzd).add(usdUsd).add(nzdNzd);
    MvcResult mvcResult = mockMvc.perform(
        post("/fx")
            .with(jwt().jwt(token).authorities(authorityRoleConverter))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(fxRequest)
            )
    ).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    FxResponse fxResponse = objectMapper
        .readValue(mvcResult.getResponse().getContentAsString(), FxResponse.class);

    FxPairResults results = fxResponse.getData();
    assertThat(results.getRates()).isNotNull().hasSize(fxRequest.getPairs().size());
    Map<IsoCurrencyPair, FxRate> theRates = results.getRates();
    assertThat(theRates)
        .containsKeys(nzdUsd, usdNzd);

    for (IsoCurrencyPair isoCurrencyPair : theRates.keySet()) {
      assertThat(results.getRates().get(isoCurrencyPair).getDate()).isNotNull();
    }

  }

  @Test
  void is_NullDateReturningCurrent() throws Exception {

    File rateResponse = new ClassPathResource("contracts/ecb/fx-current-rates.json").getFile();
    String today = dateUtils.today();
    AlphaMockUtils.mockGetResponse(
        mockInternet,
        // Matches all supported currencies
        "/" + today + "?base=USD&symbols=AUD%2CSGD%2CEUR%2CGBP%2CUSD%2CNZD",
        rateResponse);

    IsoCurrencyPair nzdUsd = IsoCurrencyPair.builder().from("USD").to("NZD").build();

    ObjectMapper objectMapper = new ObjectMapper();

    FxRequest fxRequest = FxRequest.builder().build();
    fxRequest.add(nzdUsd);
    MvcResult mvcResult = mockMvc.perform(
        post("/fx")
            .with(jwt().jwt(token).authorities(authorityRoleConverter))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(fxRequest)
            )
    ).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();

    FxResponse fxResponse = objectMapper
        .readValue(mvcResult.getResponse().getContentAsString(), FxResponse.class);

    FxPairResults results = fxResponse.getData();
    assertThat(results.getRates()).isNotNull().hasSize(fxRequest.getPairs().size());
    Map<IsoCurrencyPair, FxRate> theRates = results.getRates();
    assertThat(theRates)
        .containsKeys(nzdUsd);

    for (IsoCurrencyPair isoCurrencyPair : theRates.keySet()) {
      assertThat(results.getRates().get(isoCurrencyPair).getDate()).isNotNull();
    }

  }

  @Test
  void is_EarliestRateDateValid() {
    EcbDate ecbDate = new EcbDate();
    assertThat(ecbDate.getValidDate("1990-01-01"))
        .isEqualTo(EcbDate.earliest);
  }

  @Test
  void is_InvalidCurrenciesReturned() throws Exception {
    String date = "2019-08-27";
    IsoCurrencyPair invalid = IsoCurrencyPair.builder().from("ANC").to("SDF").build();

    FxRequest fxRequest = FxRequest.builder().rateDate(date).build();
    fxRequest.add(invalid);
    MvcResult mvcResult = mockMvc.perform(
        post("/fx")
            .with(jwt().jwt(token).authorities(authorityRoleConverter))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(fxRequest)
            )
    ).andExpect(status().is4xxClientError())
        .andReturn();

    Optional<BusinessException> someException = Optional.ofNullable((BusinessException)
        mvcResult.getResolvedException());

    assertThat(someException.isPresent()).isTrue();
    assertThat(someException.get()).hasMessageContaining("ANC").hasMessageContaining("SDF");

  }


}
