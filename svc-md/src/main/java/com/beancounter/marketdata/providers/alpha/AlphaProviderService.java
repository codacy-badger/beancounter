package com.beancounter.marketdata.providers.alpha;

import static com.beancounter.marketdata.providers.ProviderArguments.getInstance;

import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Market;
import com.beancounter.common.model.MarketData;
import com.beancounter.marketdata.providers.ProviderArguments;
import com.beancounter.marketdata.service.MarketDataProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
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
public class AlphaProviderService implements MarketDataProvider {
  public static final String ID = "ALPHA";
  @Value("${beancounter.marketdata.provider.alpha.key:demo}")
  private String apiKey;
  @Value("${beancounter.marketdata.provider.alpha.batchSize:2}")
  private Integer batchSize;

  @Value("${beancounter.marketdata.provider.alpha.markets}")
  private String markets;

  private AlphaRequestor alphaRequestor;
  private ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  AlphaProviderService(AlphaRequestor alphaRequestor) {
    this.alphaRequestor = alphaRequestor;
  }

  @PostConstruct
  void logStatus() {
    if (apiKey.equalsIgnoreCase("demo")) {
      log.info("Running with the DEMO apiKey");
    } else {
      log.info("Running with an apiKey {}***", apiKey.substring(0, 4));
    }

  }

  @Override
  public MarketData getCurrent(Asset asset) {
    Collection<Asset> assets = new ArrayList<>();
    assets.add(asset);
    return getCurrent(assets).iterator().next();
  }

  @Override
  public Collection<MarketData> getCurrent(Collection<Asset> assets) {

    ProviderArguments providerArguments = getInstance(assets, null, this);

    Map<Integer, Future<String>> requests = new ConcurrentHashMap<>();

    for (Integer batchId : providerArguments.getBatch().keySet()) {
      requests.put(batchId,
          alphaRequestor.getMarketData(providerArguments.getBatch().get(batchId), apiKey));
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
          results.addAll(getFromResponse(providerArguments, batch, requests.get(batch)));
          requests.remove(batch);
        }
        empty = requests.isEmpty();
      }
    }

    return results;
  }

  private Collection<MarketData> getFromResponse(ProviderArguments providerArguments,
                                                 Integer batchId, Future<String> response) {

    Collection<MarketData> results = new ArrayList<>();
    try {

      String[] assets = providerArguments.getAssets(batchId);

      for (String dpAsset : assets) {
        Asset asset = providerArguments.getDpToBc().get(dpAsset);
        String result = response.get();
        if (!isMdResponse(asset, result)) {
          results.add(getDefault(asset));
        } else {
          MarketData marketData = objectMapper.readValue(response.get(), AlphaResponse.class);

          String assetName = marketData.getAsset().getName();
          asset.setName(assetName); // Keep the name
          marketData.setAsset(asset); // Return BC view of the asset, not MarketProviders
          log.debug("Valued {} ", marketData.getAsset());
          results.add(marketData);
        }
      }

    } catch (IOException | InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    return results;

  }

  private boolean isMdResponse(Asset asset, String result) throws IOException {
    String field = null;
    if (result.contains("Error Message")) {
      field = "Error Message";
    } else if (result.contains("\"Note\":")) {
      field = "Note";
    } else if (result.contains("\"Information\":")) {
      field = "Information";
    }

    if (field != null) {
      JsonNode resultMessage = objectMapper.readTree(result);
      log.error("{} - API returned [{}]", asset, resultMessage.get(field));
      return false;
    }
    return true;

  }

  private MarketData getDefault(Asset asset) {
    return MarketData.builder().asset(asset).close(BigDecimal.ZERO).build();
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
    if (bcMarketCode.equalsIgnoreCase("ASX")) {
      return "AX";
    }
    return bcMarketCode;

  }


}