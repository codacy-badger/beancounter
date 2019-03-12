package com.beancounter.marketdata.service;

import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Market;
import com.beancounter.marketdata.providers.alpha.AlphaProviderService;
import com.beancounter.marketdata.providers.mock.MockProviderService;
import com.beancounter.marketdata.providers.wtd.WtdProviderService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Return a MarketData provider from a registered collection.
 *
 * @author mikeh
 * @since 2019-03-01
 */
@Service
public class MdFactory {

  private Map<String, MarketDataProvider> providers = new HashMap<>();

  @Autowired
  MdFactory(MockProviderService mockProviderService,
            AlphaProviderService alphaProviderService,
            WtdProviderService wtdProviderService) {
    providers.put(mockProviderService.getId().toUpperCase(), mockProviderService);
    providers.put(alphaProviderService.getId().toUpperCase(), alphaProviderService);
    providers.put(wtdProviderService.getId().toUpperCase(), wtdProviderService);
  }

  /**
   * Figures out how to locate a Market Data provider for the requested asset.
   * If one can't be found, then the MOCK provider is returned.
   *
   * @param asset who wants to know?
   * @return the provider that supports the asset
   */
  MarketDataProvider getMarketDataProvider(Asset asset) {
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
    if (market.getCode().equalsIgnoreCase("MOCK")) {
      return providers.get(MockProviderService.ID);
    } else {
      return providers.get(WtdProviderService.ID);
    }
  }

}
