package com.beancounter.marketdata.providers.alpha;

import com.beancounter.common.contracts.AssetSearchResponse;
import com.beancounter.common.contracts.AssetSearchResult;
import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Market;
import com.beancounter.marketdata.assets.AssetEnricher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AlphaEnricher implements AssetEnricher {
  private final ObjectMapper objectMapper = new AlphaPriceAdapter().getAlphaMapper();
  private final AlphaConfig alphaConfig;
  private AlphaProxyCache alphaProxyCache;
  @Value("${beancounter.market.providers.ALPHA.key:demo}")
  private String apiKey;

  public AlphaEnricher(AlphaConfig alphaConfig) {
    this.alphaConfig = alphaConfig;
  }

  @Autowired(required = false)
  void setAlphaProxyCache(AlphaProxyCache alphaProxyCache) {
    this.alphaProxyCache = alphaProxyCache;
  }

  @SneakyThrows
  @Override
  public Asset enrich(Market market, String code, String name) {
    String marketCode = alphaConfig.translateMarketCode(market);
    String symbol = alphaConfig.translateSymbol(code);
    if (marketCode != null) {
      symbol = symbol + "." + marketCode;
    }
    String result = alphaProxyCache.search(symbol, apiKey).get();
    AssetSearchResult assetResult = getAssetSearchResult(market, result);
    if (assetResult == null) {
      return null;
    }
    return Asset.builder()
        .code(code.toUpperCase())
        .name(assetResult.getName())
        .priceSymbol(assetResult.getSymbol())
        .market(market)
        .marketCode(market.getCode().toUpperCase())
        .category(assetResult.getType())
        .build();
  }

  private AssetSearchResult getAssetSearchResult(Market market, String result)
      throws JsonProcessingException {

    AssetSearchResponse assetSearchResponse =
        objectMapper.readValue(result, AssetSearchResponse.class);
    if (assetSearchResponse.getData() == null || assetSearchResponse.getData().isEmpty()) {
      return null;
    }
    AssetSearchResult assetResult = assetSearchResponse.getData().iterator().next();
    if (!assetResult.getCurrency().equals(market.getCurrencyId())) {
      // Fuzzy search result returned and asset from a different exchange
      return null;
    }
    return assetResult;
  }

  @Override
  public boolean canEnrich(Asset asset) {
    return asset.getName() == null;
  }
}
