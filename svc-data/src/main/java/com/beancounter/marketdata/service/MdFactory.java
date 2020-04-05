package com.beancounter.marketdata.service;

import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Market;
import com.beancounter.marketdata.providers.alpha.AlphaService;
import com.beancounter.marketdata.providers.mock.MockProviderService;
import com.beancounter.marketdata.providers.wtd.WtdService;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Return a MarketData provider from a registered collection.
 *
 * @author mikeh
 * @since 2019-03-01
 */
@Service
@Slf4j
public class MdFactory {

  private Map<String, MarketDataProvider> providers = new HashMap<>();

  MdFactory(MockProviderService mockProviderService,
            AlphaService alphaService,
            WtdService wtdService) {
    providers.put(mockProviderService.getId().toUpperCase(), mockProviderService);
    providers.put(wtdService.getId().toUpperCase(), wtdService);
    providers.put(alphaService.getId().toUpperCase(), alphaService);
  }

  /**
   * Figures out how to locate a Market Data provider for the requested asset.
   * If one can't be found, then the MOCK provider is returned.
   *
   * @param asset who wants to know?
   * @return the provider that supports the asset
   */
  @Cacheable("providers")
  public MarketDataProvider getMarketDataProvider(Asset asset) {
    MarketDataProvider provider = resolveProvider(asset.getMarket());
    if (provider == null) {
      return providers.get(MockProviderService.ID);
    }
    return provider;
  }

  public MarketDataProvider getMarketDataProvider(String provider) {
    return providers.get(provider.toUpperCase());
  }

  private MarketDataProvider resolveProvider(Market market) {
    // ToDo: Map Market to Provider
    for (String key : providers.keySet()) {
      if (providers.get(key).isMarketSupported(market)) {
        return providers.get(key);
      }
    }
    log.error("Unable to identify a provider for {}", market);
    return null;
  }


}
