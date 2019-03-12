package com.beancounter.marketdata.providers.wtd;

import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.exception.SystemException;
import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Market;
import com.beancounter.common.model.MarketData;
import com.beancounter.marketdata.providers.ProviderArguments;
import com.beancounter.marketdata.service.MarketDataProvider;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AlphaAdvantage - www.alphavantage.co.
 *
 * @author mikeh
 * @since 2019-03-03
 */
@Service
@Slf4j
public class WtdProviderService implements MarketDataProvider {
  public static final String ID = "WTD";
  @Value("${beancounter.marketdata.provider.worldTradingData.key:demo}")
  private String apiKey;
  @Value("${beancounter.marketdata.provider.worldTradingData.batchSize:2}")
  private Integer batchSize;

  @Value("${beancounter.marketdata.provider.worldTradingData.markets}")
  private String markets;

  private WtdRequestor wtdRequestor;

  @Autowired
  WtdProviderService(WtdRequestor wtdRequestor) {
    this.wtdRequestor = wtdRequestor;
  }

  @Override
  public MarketData getCurrent(Asset asset) {
    Collection<Asset> assets = new ArrayList<>();
    assets.add(asset);
    Collection<MarketData> response = getCurrent(assets);

    return response.iterator().next();
  }

  @Override
  public Collection<MarketData> getCurrent(Collection<Asset> assets) {
    String date = "2019-03-11";
    ProviderArguments providerArguments = ProviderArguments.getInstance(assets, date, this);

    Map<Integer, Future<WtdResponse>> batchedRequests = new ConcurrentHashMap<>();

    for (Integer integer : providerArguments.getBatch().keySet()) {
      batchedRequests.put(integer, wtdRequestor.getMarketData(
          providerArguments.getBatch().get(integer), date, apiKey));
    }

    return getMarketData(providerArguments, batchedRequests);
  }

  private Collection<MarketData> getMarketData(ProviderArguments providerArguments,
                                               Map<Integer, Future<WtdResponse>> requests) {

    Collection<MarketData> results = new ArrayList<>();
    boolean empty = requests.isEmpty();

    while (!empty) {
      for (Integer batch : requests.keySet()) {
        if (requests.get(batch).isDone()) {
          results.addAll(getFromResponse(providerArguments, batch, requests.get(batch)));
          requests.remove(batch);
        }
        empty = requests.isEmpty();
      }
    }

    return results;

  }

  private Collection<MarketData> getFromResponse(ProviderArguments providerArguments,
                                                 Integer batchId, Future<WtdResponse> response) {
    Collection<MarketData> results = new ArrayList<>();
    try {
      WtdResponse wtdResponse = response.get();

      String[] assets = providerArguments.getAssets(batchId);
      for (String dpAsset : assets) {

        // Ensure we return a MarketData result for each requested asset
        Asset bcAsset = providerArguments.getDpToBc().get(dpAsset);
        if (wtdResponse.getMessage() != null) {
          // Entire call failed
          log.error("{} - {}", wtdResponse.getMessage(), providerArguments.getAssets(batchId));
          if (wtdResponse.getData() == null) {
            throw new BusinessException(wtdResponse.getMessage());
          }
        }

        MarketData marketData = null;
        if (wtdResponse.getData() != null) {
          marketData = wtdResponse.getData().get(dpAsset);
        }

        if (marketData == null) {
          // Not contained in the response
          marketData = getDefault(bcAsset, dpAsset);
        } else {
          marketData.setAsset(bcAsset);
        }
        marketData.setDate(providerArguments.getDate());
        results.add(marketData);
      }
      return results;
    } catch (InterruptedException | ExecutionException e) {
      throw new SystemException(e.getMessage());
    }
  }


  private MarketData getDefault(Asset asset, String dpAsset) {
    log.warn("Unable to locate a price for {} using code {}. Returning a default", asset, dpAsset);
    return MarketData.builder()
        .asset(asset)
        .close(BigDecimal.ZERO).build();
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public Integer getBatchSize() {
    return batchSize;
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

    if (bcMarketCode.equalsIgnoreCase("NASDAQ")
        || bcMarketCode.equalsIgnoreCase("NYSE")
        || bcMarketCode.equalsIgnoreCase("AMEX")

    ) {
      return null;
    }
    return bcMarketCode;

  }


}
