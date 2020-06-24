package com.beancounter.marketdata.service;

import com.beancounter.common.contracts.PriceRequest;
import com.beancounter.common.contracts.PriceResponse;
import com.beancounter.common.input.AssetInput;
import com.beancounter.common.model.Asset;
import com.beancounter.common.model.MarketData;
import com.beancounter.marketdata.providers.PriceService;
import com.beancounter.marketdata.providers.ProviderUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service container for MarketData information.
 *
 * @author mikeh
 * @since 2019-01-28
 */
@Service
@Slf4j
public class MarketDataService {

  private final ProviderUtils providerUtils;
  private final PriceService priceService;

  @Autowired
  MarketDataService(ProviderUtils providerUtils,
                    PriceService priceService) {
    this.providerUtils = providerUtils;
    this.priceService = priceService;
  }

  @Transactional
  public void backFill(Asset asset) {
    Collection<Asset> assets = new ArrayList<>();
    assets.add(asset);
    Map<MarketDataProvider, Collection<Asset>>  byFactory =
        providerUtils.splitProviders(providerUtils.getInputs(assets));

    for (MarketDataProvider marketDataProvider : byFactory.keySet()) {
      priceService.process(marketDataProvider.backFill(asset));
    }
  }

  @SneakyThrows
  public PriceResponse getPriceResponse(Asset asset) {
    return getFuturePriceResponse(asset).get();
  }

  /**
   * Prices for the request.
   *
   * @param priceRequest to process
   * @return results
   */
  @Transactional
  public PriceResponse getPriceResponse(PriceRequest priceRequest) {

    Map<MarketDataProvider, Collection<Asset>>
        byFactory = providerUtils.splitProviders(priceRequest.getAssets());

    Collection<MarketData> existing = new ArrayList<>();
    Collection<MarketData> apiResults = new ArrayList<>();

    for (MarketDataProvider marketDataProvider : byFactory.keySet()) {
      // Pull from the DB
      Iterator<Asset> assetIterable = byFactory.get(marketDataProvider).iterator();
      while (assetIterable.hasNext()) {
        Asset asset = assetIterable.next();
        LocalDate mpDate = marketDataProvider.getDate(asset.getMarket(), priceRequest);
        Optional<MarketData> md = priceService.getMarketData(asset.getId(), mpDate);
        if (md.isPresent()) {
          MarketData mdValue = md.get();
          mdValue.setAsset(asset);
          existing.add(mdValue);
          assetIterable.remove(); // One less external query to make
        }
      }

      // Pull the balance over external API integration
      Collection<Asset> apiAssets = byFactory.get(marketDataProvider);
      if (!apiAssets.isEmpty()) {
        Collection<AssetInput> assetInputs = providerUtils.getInputs(apiAssets);
        PriceRequest apiRequest = PriceRequest.builder()
            .date(priceRequest.getDate())
            .assets(assetInputs)
            .build();
        apiResults = marketDataProvider.getMarketData(apiRequest);
      }
    }
    // Merge results into a response
    PriceResponse response = PriceResponse.builder().data(apiResults).build();
    priceService.write(response); // Async write
    response.getData().addAll(existing);
    return response;
  }

  /**
   * Get the current MarketData values for the supplied Asset.
   *
   * @param asset to query
   * @return MarketData - Values will be ZERO if not found or an integration problem occurs
   */
  @Async
  public Future<PriceResponse> getFuturePriceResponse(Asset asset) {
    List<AssetInput> inputs = new ArrayList<>();
    inputs.add(AssetInput.builder()
        .resolvedAsset(asset).build());
    return new AsyncResult<>(getPriceResponse(PriceRequest.builder().assets(inputs).build()));
  }

  /**
   * Delete all prices.  Supports testing
   */
  public void purge() {
    priceService.purge();
  }
}
