package com.beancounter.marketdata.service;

import com.beancounter.common.model.Asset;
import com.beancounter.common.model.MarketData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service container for MarketData information.
 *
 * @author mikeh
 * @since 2019-01-28
 */
@Service
public class MarketDataService {

  private MdFactory mdFactory;
  private MarketService marketService;

  @Autowired
  MarketDataService(MdFactory mdFactory, MarketService marketService) {
    this.mdFactory = mdFactory;
    this.marketService = marketService;
  }

  /**
   * Get the current MarketData values for the supplied Asset.
   *
   * @param asset to query
   * @return MarketData - Values will be ZERO if not found or an integration problem occurs
   */
  public MarketData getCurrent(Asset asset) {
    hydrateAsset(asset);
    return mdFactory.getMarketDataProvider(asset).getCurrent(asset);
  }

  /**
   * MarketData for a Collection of assets.
   *
   * @param assets to query
   * @return results
   */
  public Collection<MarketData> getCurrent(Collection<Asset> assets) {

    for (Asset asset : assets) {
      hydrateAsset(asset);
    }
    Map<String, Collection<Asset>> factories = mdFactory.splitProviders(assets);
    Collection<MarketData> results = new ArrayList<>();

    for (String dpId : factories.keySet()) {
      results.addAll(mdFactory.getMarketDataProvider(dpId)
          .getCurrent(factories.get(dpId)));
    }
    return results;
  }

  private void hydrateAsset(Asset asset) {
    asset.setMarket(marketService.getMarket(asset.getMarket().getCode()));
  }


}
