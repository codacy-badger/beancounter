package com.beancounter.position.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.beancounter.auth.common.TokenUtils;
import com.beancounter.auth.server.AuthorityRoleConverter;
import com.beancounter.client.AssetService;
import com.beancounter.client.services.PortfolioServiceClient;
import com.beancounter.client.services.StaticService;
import com.beancounter.common.contracts.AssetRequest;
import com.beancounter.common.contracts.AssetUpdateResponse;
import com.beancounter.common.contracts.PositionResponse;
import com.beancounter.common.input.AssetInput;
import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Portfolio;
import com.beancounter.common.model.Position;
import com.beancounter.common.model.Positions;
import com.beancounter.common.model.SystemUser;
import com.beancounter.common.model.Trn;
import com.beancounter.common.model.TrnType;
import com.beancounter.position.service.Accumulator;
import com.beancounter.position.service.Valuation;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebAppConfiguration
@AutoConfigureStubRunner(
    stubsMode = StubRunnerProperties.StubsMode.LOCAL,
    ids = "org.beancounter:svc-data:+:stubs:10999")
@ActiveProfiles("test")
@Tag("slow")
@Slf4j
@SpringBootTest
class StubbedFxValuations {

  private final AuthorityRoleConverter authorityRoleConverter = new AuthorityRoleConverter();
  private final ObjectMapper mapper = new ObjectMapper();
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private Accumulator accumulator;
  private MockMvc mockMvc;
  @Autowired
  private Valuation valuation;
  @Autowired
  private StaticService staticService;
  @Autowired
  private AssetService assetService;
  @Autowired
  private PortfolioServiceClient portfolioService;
  private Jwt token;

  @Autowired
  void mockServices() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();

    // Setup a user account
    SystemUser user = SystemUser.builder()
        .id("user")
        .email("user@testing.com")
        .build();
    token = TokenUtils.getUserToken(user);

  }

  private Positions getPositions(Asset asset) {

    Trn trn = Trn.builder()
        .trnType(TrnType.BUY)
        .asset(asset)
        .tradeAmount(new BigDecimal(2000))
        .quantity(new BigDecimal(100)).build();

    Portfolio portfolio = portfolioService.getPortfolioByCode("TEST");
    trn.setTradeCurrency(
        staticService.getCurrency(asset.getMarket().getCurrency().getCode()));

    Positions positions = new Positions(portfolio);
    positions.setAsAt("2019-10-18");

    Position position = accumulator.accumulate(trn, portfolio,
        Position.builder().asset(asset).build());

    positions.add(position);
    return positions;
  }

  private Positions getValuedPositions(Asset asset) {
    Positions positions = getPositions(asset);
    assertThat(positions).isNotNull();
    assertThat(valuation).isNotNull();
    valuation.value(positions);
    return positions;
  }


  @Test
  void is_MvcValuingPositions() throws Exception {
    Asset asset = getEbay();
    assertThat(asset).hasFieldOrProperty("name");

    Positions positions = getPositions(asset);
    PositionResponse positionResponse = PositionResponse.builder().data(positions).build();

    assertThat(mockMvc).isNotNull();
    String json = mockMvc.perform(post("/value")
        .with(jwt().jwt(token).authorities(authorityRoleConverter))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .content(mapper.writeValueAsString(positionResponse))
    ).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
        .andReturn().getResponse().getContentAsString();

    PositionResponse fromJson = new ObjectMapper()
        .readValue(json, PositionResponse.class);

    assertThat(fromJson).isNotNull().hasFieldOrProperty("data");
    Positions jsonPositions = fromJson.getData();
    assertThat(jsonPositions).isNotNull();
    assertThat(jsonPositions.getPositions()).hasSize(positions.getPositions().size());

    for (String key : jsonPositions.getPositions().keySet()) {
      assertThat(jsonPositions.getPositions().get(key).getAsset())
          .hasFieldOrPropertyWithValue("name", asset.getName());
    }
  }

  private Asset getEbay() {
    AssetUpdateResponse assetResponse = assetService.process(AssetRequest.builder()
        .data("EBAY:NASDAQ", AssetInput.builder()
            .code("EBAY")
            .market("NASDAQ")
            .build())
        .build());
    assertThat(assetResponse.getData()).hasSize(1);
    return assetResponse.getData().get("EBAY:NASDAQ");
  }

  @Test
  void is_MvcRestException() throws Exception {

    MvcResult result = mockMvc.perform(post("/value")
        .with(jwt().jwt(token).authorities(authorityRoleConverter))
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString("{asdf}"))
    ).andExpect(status().is4xxClientError()).andReturn();

    Optional<HttpMessageNotReadableException> someException =
        Optional.ofNullable((HttpMessageNotReadableException)
            result.getResolvedException());

    assertThat(someException.isPresent()).isTrue();

  }

  @Test
  void is_MarketValuationCalculatedAsAt() {
    Asset asset = getEbay();

    // We need to have a Quantity in order to get the price, so create a position
    Positions positions = getValuedPositions(asset);

    assertThat(positions.get(asset).getMoneyValues(Position.In.TRADE))
        .hasFieldOrPropertyWithValue("unrealisedGain", new BigDecimal("8000.00"))
        .hasFieldOrPropertyWithValue("priceData.close", new BigDecimal("100.00"))
        .hasFieldOrPropertyWithValue("marketValue", new BigDecimal("10000.00"))
        .hasFieldOrPropertyWithValue("totalGain", new BigDecimal("8000.00"))
    ;
  }

  @Test
  void is_AssetAndCurrencyHydratedFromValuationRequest() {

    Asset asset = getEbay();

    Positions positions = getValuedPositions(asset);
    Position position = positions.get(asset);
    assertThat(position)
        .hasFieldOrProperty("asset");

    assertThat(position.getAsset().getMarket())
        .hasNoNullFieldsOrPropertiesExcept("currencyId", "timezoneId", "enricher");

    assertThat(position.getMoneyValues().get(Position.In.PORTFOLIO).getCurrency())
        .hasNoNullFieldsOrProperties();
    assertThat(position.getMoneyValues().get(Position.In.BASE).getCurrency())
        .hasNoNullFieldsOrProperties();
    assertThat(position.getMoneyValues().get(Position.In.TRADE).getCurrency())
        .hasNoNullFieldsOrProperties();

  }


}
