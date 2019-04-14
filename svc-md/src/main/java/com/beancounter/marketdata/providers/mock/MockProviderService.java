package com.beancounter.marketdata.providers.mock;

import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Market;
import com.beancounter.common.model.MarketData;
import com.beancounter.marketdata.service.MarketDataProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * For testing purposes. Part of the main source in order to allow for an off-line provider
 * that will force certain error conditions.
 *
 * @author mikeh
 * @since 2019-03-01
 */

@Service
public class MockProviderService implements MarketDataProvider {
  public static final String ID = "MOCK";

  private Date systemDate;

  @Value("${beancounter.marketdata.provider.mock.markets}")
  private String markets;

  @Autowired
  public MockProviderService() {
    this.systemDate = new Date();

  }

  @Override
  public MarketData getCurrent(Asset asset) {
    if (asset.getCode().equalsIgnoreCase("123")) {
      throw new BusinessException(
          String.format("Invalid asset code [%s]", asset.getCode()));
    }

    return MarketData.builder()
        .asset(asset)
        .close(BigDecimal.valueOf(999.99))
        .open(BigDecimal.valueOf(999.99))
        .date(getPriceDate())
        .build();
  }

  @Override
  public Collection<MarketData> getCurrent(Collection<Asset> assets) {
    Collection<MarketData> results = new ArrayList<>(assets.size());
    for (Asset asset : assets) {
      results.add(getCurrent(asset));
    }
    return results;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public Integer getBatchSize() {
    return 1;
  }

  @Override
  public Boolean isMarketSupported(Market market) {
    if (markets == null) {
      return false;
    }
    return markets.contains(market.getCode());

  }

  @Override
  public String getMarketProviderCode(String bcMarketCode) {
    return bcMarketCode;
  }

  public Date getPriceDate() {
    return Date.from(
        ZonedDateTime.of(LocalDate.parse(getDate()).atStartOfDay(), ZoneId.of("UTC")).toInstant());
  }

  @Override
  public String getDate() {
    return "2019-11-21";
  }


}
