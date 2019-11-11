package com.beancounter.ingest.sharesight;

import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Market;
import com.beancounter.common.model.TrnType;
import com.beancounter.common.utils.MathUtils;
import com.beancounter.ingest.config.ExchangeConfig;
import com.beancounter.ingest.service.StaticDataService;
import com.google.api.client.util.Strings;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Helper methods for converting ShareSight file format into BC domain objects.
 *
 * @author mikeh
 * @since 2019-02-12
 */
@Service
@Slf4j
@Data
public class ShareSightService {

  private final ExchangeConfig exchangeConfig;
  private DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
  private StaticDataService staticDataService;
  @Value("${out.file:#{null}}")
  private String outFile;
  @Value("${ratesIgnored:false}")
  private boolean ratesIgnored = false; // Use rates in source file to compute values, but have BC
  @Value("${range:All Trades Report}")
  private String range;
  // retrieve rates from market data service

  @Autowired
  public ShareSightService(ExchangeConfig exchangeConfig) {
    this.exchangeConfig = exchangeConfig;
  }

  @Autowired
  private void setStaticDataService(StaticDataService staticDataService) {
    this.staticDataService = staticDataService;
  }

  Date parseDate(String date) throws ParseException {
    return formatter.parse(date);
  }

  public BigDecimal parseDouble(Object o) throws ParseException {
    return new BigDecimal(NumberFormat.getInstance(Locale.US).parse(o.toString()).toString());
  }

  TrnType resolveType(String type) {
    return TrnType.valueOf(type.toUpperCase());
  }

  /**
   * Split a string that contains both ID and market by various delimiters.
   *
   * @param input "ASSET:MARKET"
   * @return resolvable Asset
   */
  public Asset resolveAsset(String input) {

    if (Strings.isNullOrEmpty(input)) {
      throw new BusinessException("Unable to resolve Asset code");
    }

    List<String> values = Splitter
        .on(CharMatcher.anyOf(".:-"))
        .trimResults()
        .splitToList(input);

    if (values.isEmpty() || values.get(0).equals(input)) {
      throw new BusinessException(String.format("Unable to parse %s", input));
    }

    return Asset.builder()
        .code(values.get(0))
        .market(resolveMarket(values.get(1)))
        .build();
  }

  private Market resolveMarket(String market) {
    return Market.builder()
        .code(exchangeConfig.resolveAlias(market)).build();
  }

  BigDecimal safeDivide(BigDecimal money, BigDecimal rate) {
    return MathUtils.divide(money, rate);
  }

  BigDecimal getValueWithFx(BigDecimal money, BigDecimal rate) {
    return MathUtils.multiply(money, rate);
  }

  boolean isUnset(BigDecimal value) {
    return MathUtils.isUnset(value);
  }
}