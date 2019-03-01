package com.beancounter.marketdata.providers.mock;

import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.model.Asset;
import com.beancounter.common.model.MarketData;
import com.beancounter.marketdata.service.MarketDataProvider;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import org.springframework.stereotype.Service;

/**
 * For testing purposes.
 * @author mikeh
 * @since 2019-03-01
 */

@Service
public class MockProviderService implements MarketDataProvider {
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
    return "MOCK";
  }


}
