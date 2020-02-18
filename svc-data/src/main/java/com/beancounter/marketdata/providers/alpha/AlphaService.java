package com.beancounter.marketdata.providers.alpha;

import static com.beancounter.marketdata.providers.ProviderArguments.getInstance;

import com.beancounter.common.contracts.PriceRequest;
import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Market;
import com.beancounter.common.model.MarketData;
import com.beancounter.marketdata.providers.ProviderArguments;
import com.beancounter.marketdata.service.MarketDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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
public class AlphaService implements MarketDataProvider {
  public static final String ID = "ALPHA";
  @Value("${beancounter.marketdata.provider.ALPHA.key:demo}")
  private String apiKey;

  private AlphaRequester alphaRequester;
  private AlphaConfig alphaConfig;
  private AlphaResponseHandler alphaResponseHandler;

  AlphaService(AlphaRequester alphaRequester,
               AlphaConfig alphaConfig,
               AlphaResponseHandler alphaResponseHandler) {
    this.alphaRequester = alphaRequester;
    this.alphaConfig = alphaConfig;
    this.alphaResponseHandler = alphaResponseHandler;

  }

  @PostConstruct
  void logStatus() {
    log.info("Running with apiKey {}{}", apiKey.substring(0, 4).toUpperCase(),
        apiKey.equalsIgnoreCase("demo") ? "" : "***");
  }

  @Override
  public MarketData getMarketData(Asset asset) {
    Collection<Asset> assets = new ArrayList<>();
    assets.add(asset);

    return getMarketData(PriceRequest.builder().assets(assets).build()).iterator().next();
  }

  @Override
  public Collection<MarketData> getMarketData(PriceRequest priceRequest) {

    ProviderArguments providerArguments = getInstance(priceRequest, alphaConfig);

    Map<Integer, Future<String>> requests = new ConcurrentHashMap<>();

    for (Integer batchId : providerArguments.getBatch().keySet()) {
      requests.put(batchId,
          alphaRequester.getMarketData(providerArguments.getBatch().get(batchId), apiKey));
    }

    return getMarketData(providerArguments, requests);

  }

  private Collection<MarketData> getMarketData(ProviderArguments providerArguments,
                                               Map<Integer, Future<String>> requests) {
    Collection<MarketData> results = new ArrayList<>();
    boolean empty = requests.isEmpty();

    while (!empty) {
      for (Integer batch : requests.keySet()) {
        if (requests.get(batch).isDone()) {
          results.addAll(
              alphaResponseHandler.get(providerArguments, batch, requests.get(batch))
          );
          requests.remove(batch);
        }
        empty = requests.isEmpty();
      }
    }

    return results;
  }


  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isMarketSupported(Market market) {
    if (alphaConfig.getMarkets() == null) {
      return false;
    }
    return alphaConfig.getMarkets().contains(market.getCode());
  }


}
