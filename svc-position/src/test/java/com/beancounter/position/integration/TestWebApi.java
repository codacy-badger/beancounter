package com.beancounter.position.integration;

import static com.beancounter.position.TestUtils.getPortfolio;
import static com.beancounter.position.integration.TestMarketValues.getPositions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Transaction;
import com.beancounter.common.utils.AssetUtils;
import com.beancounter.position.model.Positions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.util.Collection;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@SpringBootTest
@ImportAutoConfiguration({FeignAutoConfiguration.class})
@AutoConfigureStubRunner(
    stubsMode = StubRunnerProperties.StubsMode.CLASSPATH,
    ids = "beancounter:svc-md:+:stubs:8090")
@ActiveProfiles("test")
@Slf4j
class TestWebApi {

  @Autowired
  private WebApplicationContext wac;
  private MockMvc mockMvc;

  private ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  @VisibleForTesting
  @Tag("slow")
  void is_MvcTradesToPositions() throws Exception {

    File tradeFile = new ClassPathResource("contracts/trades.json").getFile();

    CollectionType javaType = mapper.getTypeFactory()
        .constructCollectionType(Collection.class, Transaction.class);

    Collection<Transaction> results = mapper.readValue(tradeFile, javaType);

    String json = mockMvc.perform(post("/")
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .content(mapper.writeValueAsString(results))
    ).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andReturn().getResponse().getContentAsString();

    Positions positions = new ObjectMapper().readValue(json, Positions.class);

    assertThat(positions).isNotNull();
    assertThat(positions.getPositions()).isNotNull().hasSize(2);
  }

  @Test
  @VisibleForTesting
  @Tag("slow")
  void is_MvcValuingPositions() throws Exception {
    Asset asset = AssetUtils.getAsset("EBAY", "NASDAQ");
    Positions positions = new Positions(getPortfolio("TEST"));
    positions.setAsAt("2019-10-20");
    getPositions(asset, positions);

    String json = mockMvc.perform(post("/value")
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .content(mapper.writeValueAsString(positions))
    ).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andReturn().getResponse().getContentAsString();

    Positions mvcPositions = new ObjectMapper().readValue(json, Positions.class);
    assertThat(mvcPositions).isNotNull();
    assertThat(mvcPositions.getPositions()).hasSize(positions.getPositions().size());
  }

  @Test
  @VisibleForTesting
  @Tag("slow")
  void is_MvcRestException() throws Exception {
    Asset asset = AssetUtils.getAsset("EBAY", "NASDAQ");
    Positions positions = new Positions(getPortfolio("TEST"));
    getPositions(asset, positions);
    positions.setAsAt("2019-10-20'");

    MvcResult result = mockMvc.perform(post("/value")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(positions))
    ).andExpect(status().is4xxClientError()).andReturn();

    Optional<BusinessException> someException = Optional.ofNullable((BusinessException)
        result.getResolvedException());

    assertThat(someException.isPresent()).isTrue();

  }

}
