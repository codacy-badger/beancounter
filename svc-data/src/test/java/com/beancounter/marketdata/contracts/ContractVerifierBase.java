package com.beancounter.marketdata.contracts;

import static com.beancounter.common.utils.AssetUtils.getAsset;
import static com.beancounter.marketdata.utils.EcbMockUtils.get;
import static com.beancounter.marketdata.utils.EcbMockUtils.getRateMap;
import static org.assertj.core.api.Assertions.assertThat;

import com.beancounter.auth.common.TokenUtils;
import com.beancounter.common.contracts.AssetRequest;
import com.beancounter.common.contracts.AssetResponse;
import com.beancounter.common.contracts.AssetUpdateResponse;
import com.beancounter.common.contracts.PortfolioResponse;
import com.beancounter.common.contracts.PortfoliosRequest;
import com.beancounter.common.contracts.PortfoliosResponse;
import com.beancounter.common.contracts.RegistrationResponse;
import com.beancounter.common.contracts.TrnRequest;
import com.beancounter.common.contracts.TrnResponse;
import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Portfolio;
import com.beancounter.common.model.SystemUser;
import com.beancounter.common.utils.BcJson;
import com.beancounter.common.utils.DateUtils;
import com.beancounter.marketdata.MarketDataBoot;
import com.beancounter.marketdata.assets.AssetService;
import com.beancounter.marketdata.portfolio.PortfolioService;
import com.beancounter.marketdata.providers.fxrates.EcbRates;
import com.beancounter.marketdata.providers.fxrates.FxGateway;
import com.beancounter.marketdata.providers.wtd.WtdGateway;
import com.beancounter.marketdata.providers.wtd.WtdMarketData;
import com.beancounter.marketdata.providers.wtd.WtdResponse;
import com.beancounter.marketdata.registration.SystemUserService;
import com.beancounter.marketdata.trn.TrnService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Spring Contract base class.  Mocks out calls to various gateways that can be imported
 * and run as stubs in other services.  Any data required from an integration call in a
 * dependent service, should be mocked in this class.
 */
@SpringBootTest(classes = MarketDataBoot.class,
    properties = {"auth.enabled=false"},
    webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DirtiesContext
@AutoConfigureMessageVerifier
@AutoConfigureWireMock(port = 0)
@WebAppConfiguration
@ActiveProfiles("contracts")
public class ContractVerifierBase {

  public static final Asset AAPL = getAsset("NASDAQ", "AAPL");
  public static final Asset MSFT = getAsset("NASDAQ", "MSFT");
  public static final Asset MSFT_INVALID = getAsset("NASDAQ", "MSFTx");
  public static final Asset AMP = getAsset("ASX", "AMP");
  @Autowired
  private DateUtils dateUtils;
  private final ObjectMapper objectMapper = new BcJson().getObjectMapper();
  @MockBean
  private JwtDecoder jwtDecoder;
  @MockBean
  private FxGateway fxGateway;
  @MockBean
  private WtdGateway wtdGateway;
  @MockBean
  private PortfolioService portfolioService;
  @MockBean
  private TrnService trnService;
  @MockBean
  private AssetService assetService;
  @MockBean
  private SystemUserService systemUserService;
  @Autowired
  private WebApplicationContext context;

  @BeforeEach
  public void initMocks() throws Exception {
    MockMvc mockMvc = MockMvcBuilders
        .webAppContextSetup(context)
        .build();
    RestAssuredMockMvc.mockMvc(mockMvc);

    mockPortfolios();
    systemUsers();
    ecbRates();
    mockTrnGetResponses();
    mockAssets();
  }

  private void systemUsers() throws Exception {
    File jsonFile =
        new ClassPathResource("contracts/register/response.json").getFile();
    RegistrationResponse response = objectMapper.readValue(jsonFile, RegistrationResponse.class);
    String email = "blah@blah.com";

    Jwt jwt = new TokenUtils().getUserToken(new SystemUser(email, email));
    Mockito.when(jwtDecoder.decode(email)).thenReturn(jwt);

    SecurityContextHolder.getContext().setAuthentication(
        new JwtAuthenticationToken(jwtDecoder.decode(email)));

    Mockito.when(systemUserService.register(jwt)).thenReturn(response);

  }

  private void ecbRates() {
    Map<String, BigDecimal> rates;
    rates = getRateMap("0.8973438622", "1.3652189519", "0.7756191673",
        "1.5692749462", "1.4606963388");

    mockEcbRates(rates, get("2019-10-20", rates));
    rates = getRateMap("0.8973438622", "1.3652189519", "0.7756191673",
        "10.0", "1.4606963388");
    mockEcbRates(rates, get("2019-01-01", rates));

    rates = getRateMap("0.8973438622", "1.3652189519", "0.7756191673",
        "1.5692749462", "1.4606963388");
    mockEcbRates(rates, get("2019-10-18", rates));

    // Current
    mockEcbRates(rates, get(dateUtils.today(), rates));

    rates = getRateMap("0.897827258", "1.3684683067", "0.8047495062",
        "1.5053869635", "1.4438857964");
    mockEcbRates(rates, get("2019-07-26", rates));
    // Saturday results are the same as Fridays
    mockEcbRates(rates, get("2019-07-26", rates), "2019-07-27");

    rates = getRateMap("0.9028530155", "1.3864210906", "0.8218941856",
        "1.5536294691", "1.4734561213");
    mockEcbRates(rates, get("2019-08-16", rates));

    rates = getRateMap("0.9078529278", "1.36123468", "0.7791193827",
        "1.5780299591", "1.460463005");
    mockEcbRates(rates, get("2019-11-12", rates));

    rates = getRateMap("0.8482483671", "1.6586648571", "0.6031894139",
        "1.8855712953", "1.6201543812");
    mockEcbRates(rates, get("1999-01-04", rates));

  }

  private void mockTrnGetResponses() throws Exception {
    mockTrnPostResponse(getTestPortfolio());
    mockTrnGetResponse(getTestPortfolio(), "contracts/trn/TEST-response.json");
    mockTrnGetResponse(getEmptyPortfolio(), "contracts/trn/EMPTY-response.json");
    Mockito.when(trnService.findByPortfolioAsset(getTestPortfolio(),
        "KMI", Objects.requireNonNull(
            dateUtils.getDate("2020-05-01", dateUtils.getZoneId()))))
        .thenReturn(objectMapper.readValue(
            new ClassPathResource("contracts/trn/trn-for-asset.json").getFile(),
            TrnResponse.class));

  }

  void mockTrnGetResponse(Portfolio portfolio, String trnFile) throws Exception {
    File jsonFile = new ClassPathResource(trnFile).getFile();
    TrnResponse trnResponse = objectMapper.readValue(jsonFile, TrnResponse.class);
    Mockito.when(trnService.findForPortfolio(
        portfolio,
        Objects.requireNonNull(dateUtils.getDate()))).thenReturn(trnResponse);
  }

  void mockTrnPostResponse(Portfolio portfolio) throws Exception {
    Mockito.when(trnService.save(portfolio, objectMapper.readValue(
        new ClassPathResource("contracts/trn/CSV-write.json").getFile(), TrnRequest.class)))
        .thenReturn(objectMapper.readValue(
            new ClassPathResource("contracts/trn/CSV-response.json").getFile(), TrnResponse.class));

  }

  public void mockPortfolios() throws Exception {
    mockPortfolio(getEmptyPortfolio());
    mockPortfolio(getTestPortfolio());
    // All Portfolio
    Mockito.when(portfolioService.getPortfolios()).thenReturn(
        objectMapper.readValue(
            new ClassPathResource("contracts/portfolio/portfolios.json").getFile(),
            PortfoliosResponse.class).getData()
    );

    Mockito.when(portfolioService.findWhereHeld(
        "KMI",
        dateUtils.getDate("2020-05-01", dateUtils.getZoneId())))
        .thenReturn(
            objectMapper.readValue(
                new ClassPathResource("contracts/portfolio/where-held-response.json").getFile(),
                PortfoliosResponse.class)
        );


    Mockito.when(portfolioService.save(
        objectMapper.readValue(new ClassPathResource("contracts/portfolio/add-request.json")
                .getFile(),
            PortfoliosRequest.class).getData()))
        .thenReturn(
            objectMapper.readValue(new ClassPathResource("contracts/portfolio/add-response.json")
                    .getFile(),
                PortfoliosResponse.class)
                .getData());
  }

  private Portfolio getTestPortfolio() throws IOException {
    File jsonFile =
        new ClassPathResource("contracts/portfolio/test.json").getFile();

    return getPortfolio(jsonFile);
  }

  private Portfolio getEmptyPortfolio() throws IOException {
    File jsonFile =
        new ClassPathResource("contracts/portfolio/empty.json").getFile();

    return getPortfolio(jsonFile);
  }

  private Portfolio getPortfolio(File jsonFile) throws IOException {
    PortfolioResponse portfolioResponse = objectMapper.readValue(jsonFile, PortfolioResponse.class);
    return portfolioResponse.getData();
  }

  private void mockPortfolio(Portfolio portfolio) {
    // For the sake of convenience when testing; id and code are the same
    Mockito.when(portfolioService.find(portfolio.getId()))
        .thenReturn(portfolio);

    Mockito.when(portfolioService.findByCode(portfolio.getCode()))
        .thenReturn(portfolio);
  }

  private void mockAssets() throws Exception {
    Mockito.when(assetService.find("KMI"))
        .thenReturn(
            objectMapper.readValue(
                new ClassPathResource("contracts/assets/kmi-asset-by-id.json").getFile(),
                AssetResponse.class).getData());

    mockAssetCreateResponses(
        new ClassPathResource("contracts/assets/request.json").getFile(),
        new ClassPathResource("contracts/assets/response.json").getFile());
    mockAssetCreateResponses(
        new ClassPathResource("contracts/assets/ebay-request.json").getFile(),
        new ClassPathResource("contracts/assets/ebay-response.json").getFile());
    mockAssetCreateResponses(
        new ClassPathResource("contracts/assets/msft-request.json").getFile(),
        new ClassPathResource("contracts/assets/msft-response.json").getFile());
    mockAssetCreateResponses(
        new ClassPathResource("contracts/assets/bhp-request.json").getFile(),
        new ClassPathResource("contracts/assets/bhp-response.json").getFile());
    mockAssetCreateResponses(
        new ClassPathResource("contracts/assets/bhp-lse-request.json").getFile(),
        new ClassPathResource("contracts/assets/bhp-lse-response.json").getFile());
    mockAssetCreateResponses(
        new ClassPathResource("contracts/assets/abbv-request.json").getFile(),
        new ClassPathResource("contracts/assets/abbv-response.json").getFile());
    mockAssetCreateResponses(
        new ClassPathResource("contracts/assets/amp-request.json").getFile(),
        new ClassPathResource("contracts/assets/amp-response.json").getFile());

    Map<String, WtdMarketData> result = new HashMap<>();
    WtdMarketData ebayMd = new WtdMarketData(
        new BigDecimal("39.21"),
        new BigDecimal("100.00"),
        new BigDecimal("38.74"),
        new BigDecimal("39.35"),
        Integer.decode("6274307")
    );

    result.put("EBAY", ebayMd);
    WtdResponse wtdResponse = new WtdResponse("2019-10-18", result);
    Mockito.when(wtdGateway
        .getPrices("EBAY", "2019-10-18", "demo"))
        .thenReturn(wtdResponse);
  }

  private void mockAssetCreateResponses(File jsonRequest, File jsonResponse) throws Exception {
    AssetRequest assetRequest =
        objectMapper.readValue(jsonRequest, AssetRequest.class);

    AssetUpdateResponse assetUpdateResponse =
        objectMapper.readValue(jsonResponse, AssetUpdateResponse.class);

    Mockito.when(assetService.process(assetRequest)).thenReturn(assetUpdateResponse);
    Set<String> keys = assetUpdateResponse.getData().keySet();
    for (String key : keys) {
      Asset theAsset = assetUpdateResponse.getData().get(key);
      theAsset.getId();
      Mockito.when(assetService.find(theAsset.getId())).thenReturn(theAsset);
      Mockito.when(assetService.findLocally(
          theAsset.getMarket().getCode().toUpperCase(),
          theAsset.getCode().toUpperCase()))
          .thenReturn(theAsset);
    }
  }

  private void mockEcbRates(Map<String, BigDecimal> rates, EcbRates ecbRates) {
    mockEcbRates(rates, ecbRates, dateUtils.getDateString(ecbRates.getDate()));
  }

  private void mockEcbRates(Map<String, BigDecimal> rates, EcbRates ecbRates, String rateDate) {
    Mockito.when(fxGateway.getRatesForSymbols(rateDate, "USD",
        String.join(",", rates.keySet())))
        .thenReturn(ecbRates);
  }

  @Test
  public void is_Started() {
    assertThat(wtdGateway).isNotNull();
    assertThat(fxGateway).isNotNull();
    assertThat(assetService).isNotNull();
    assertThat(trnService).isNotNull();
    assertThat(portfolioService).isNotNull();
  }
}