package com.beancounter.marketdata;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.model.Market;
import com.beancounter.common.utils.DateUtils;
import com.beancounter.common.utils.UtilConfig;
import com.beancounter.marketdata.config.StaticConfig;
import com.beancounter.marketdata.currency.CurrencyService;
import com.beancounter.marketdata.markets.MarketService;
import com.beancounter.marketdata.providers.mock.MockProviderService;
import com.beancounter.marketdata.providers.wtd.WtdService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Market related tests.
 *
 * @author mikeh
 * @since 2019-03-19
 */
@SpringBootTest(classes = {
    UtilConfig.class,
    CurrencyService.class,
    MarketService.class,
    StaticConfig.class})
class TestStaticData {

  private final StaticConfig staticConfig;
  private final MarketService marketService;
  private final CurrencyService currencyService;
  private final DateUtils dateUtils = new DateUtils();

  @Autowired
  TestStaticData(StaticConfig staticConfig,
                 MarketService marketService,
                 CurrencyService currencyService
  ) {
    this.marketService = marketService;
    this.staticConfig = staticConfig;
    this.currencyService = currencyService;
  }

  @Test
  void is_FoundForAlias() {
    Market nyse = marketService.getMarket("NYSE");
    Market nzx = marketService.getMarket("NZX");
    Market asx = marketService.getMarket("ASX");
    Market nasdaq = marketService.getMarket("NASDAQ");
    assertThat(marketService.getMarket("nys")).isEqualTo(nyse);
    assertThat(marketService.getMarket("NZ")).isEqualTo(nzx);
    assertThat(marketService.getMarket("AX")).isEqualTo(asx);
    assertThat(marketService.getMarket("NAS")).isEqualTo(nasdaq);
  }

  @Test
  void does_MockMarketConfigurationExist() {

    assertThat(staticConfig).isNotNull();
    Market market = marketService.getMarket(MockProviderService.ID);
    assertThat(market)
        .isNotNull()
        .hasFieldOrPropertyWithValue("timezone", TimeZone.getTimeZone(UTC))
        .hasFieldOrProperty("currency")
    ;

    assertThat(market.getCurrency())
        .hasFieldOrPropertyWithValue("code", "USD");

  }

  @Test
  void is_serTzComputed() {

    //  The java.util.Date has no concept of time zone, and only represents
    //  the number of seconds passed since the Unix epoch time – 1970-01-01T00:00:00Z.
    //  But, if you print the Date object directly, it is always printed with the default
    //  system time zone.

    String dateFormat = "yyyy-MM-dd hh:mm:ss";
    String dateInString = "2019-04-14 10:30:00";
    // Users requested date "today in timezone"

    LocalDate sunday = LocalDate
        .parse(dateInString, DateTimeFormatter.ofPattern(dateFormat));

    LocalDate resolvedDate = dateUtils.getLastMarketDate(
        sunday,
        marketService.getMarket("NYSE").getTimezone().toZoneId());

    assertThat(resolvedDate)
        .isEqualTo(LocalDate.of(2019, 4, 12))
    ;

    resolvedDate = dateUtils.getLastMarketDate(sunday,
        marketService.getMarket("NYSE").getTimezone().toZoneId());

    assertThat(resolvedDate)
        .isEqualTo(LocalDate.of(2019, 4, 12))
    ;

  }

  @Test
  void is_IgnoreAliasLookup() {
    // Alias exists, but no PK with this code
    assertThrows(BusinessException.class, () ->
        marketService.getMarket("US", false));
  }

  @Test
  void is_AliasForWtdAndNzxResolving() {
    Market market = marketService.getMarket("NZX");
    assertThat(market)
        .isNotNull()
        .hasFieldOrProperty("aliases");

    assertThat(market.getCurrency())
        .hasFieldOrPropertyWithValue("code", "NZD");

    assertThat(market.getAliases()
        .get(WtdService.ID))
        .isEqualTo("NZ")
        .isNotNull();
  }

  @Test
  void does_MarketDataAliasNasdaqResolveToNull() {
    Market market = marketService.getMarket("NASDAQ");
    assertThat(market)
        .isNotNull()
        .hasFieldOrProperty("aliases");

    assertThat(market.getAliases()
        .get(WtdService.ID))
        .isBlank();
  }

  @Test
  void is_CurrencyDataLoading() {

    assertThat(currencyService.getCode("USD"))
        .isNotNull();

    assertThat(currencyService.getBase())
        .isNotNull();
  }
}
