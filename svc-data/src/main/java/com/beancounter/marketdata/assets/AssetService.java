package com.beancounter.marketdata.assets;

import com.beancounter.common.contracts.AssetRequest;
import com.beancounter.common.contracts.AssetUpdateResponse;
import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.input.AssetInput;
import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Market;
import com.beancounter.common.utils.KeyGenUtils;
import com.beancounter.marketdata.markets.MarketService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AssetService implements com.beancounter.client.AssetService {
  private AssetRepository assetRepository;
  private MarketService marketService;
  private AssetEnricher assetEnricher;

  @Autowired
  void setAssetEnricher(AssetEnricher assetEnricher) {
    this.assetEnricher = assetEnricher;
  }

  @Autowired
  void setAssetRepository(AssetRepository assetRepository) {
    this.assetRepository = assetRepository;
  }

  @Autowired
  void setMarketService(MarketService marketService) {
    this.marketService = marketService;
  }

  private Asset create(AssetInput assetInput) {
    Asset foundAsset = findLocally(
        assetInput.getMarket().toUpperCase(),
        assetInput.getCode().toUpperCase());

    if (foundAsset == null) {
      Market market = marketService.getMarket(assetInput.getMarket());

      // Find @Bloomberg
      Asset enrichedAsset = assetEnricher.enrich(
          assetInput.getMarket(),
          assetInput.getCode());

      if (enrichedAsset == null) {
        // User Defined Asset?
        Asset asset = Asset.builder().build();
        asset.setId(KeyGenUtils.format(UUID.randomUUID()));
        asset.setCode(assetInput.getCode().toUpperCase());
        asset.setMarketCode(market.getCode());
        asset.setMarket(market);
        if (assetInput.getName() != null) {
          asset.setName(assetInput.getName().replace("\"", ""));
        }
        return hydrateAsset(
            assetRepository.save(asset)
        );
      } else {
        // Market Listed
        enrichedAsset.setId(KeyGenUtils.format(UUID.randomUUID()));
        return assetRepository.save(enrichedAsset);
      }

    }
    return backFillMissingData(foundAsset.getId(), foundAsset);
  }

  public AssetUpdateResponse process(AssetRequest asset) {
    Map<String, Asset> assets = new HashMap<>();
    for (String key : asset.getData().keySet()) {
      assets.put(
          key,
          create(asset.getData().get(key))
      );
    }
    return AssetUpdateResponse.builder().data(assets).build();
  }

  public Asset find(String marketCode, String code) {
    Asset asset = findLocally(marketCode, code);
    if (asset == null || asset.getName() == null) {
      asset = assetEnricher.enrich(marketCode, code);
      if (asset == null) {
        throw new BusinessException(String.format("No asset found for %s:%s", marketCode, code));
      }
      if (!marketCode.equalsIgnoreCase("MOCK")) {
        if (asset.getId() == null) {
          asset.setId(KeyGenUtils.format(UUID.randomUUID()));
        }
        asset = assetRepository.save(asset);
      }

    }
    return asset;
  }

  public Asset find(String id) {
    Optional<Asset> result = assetRepository.findById(id).map(this::hydrateAsset);
    if (result.isPresent()) {
      return result.get();
    }
    throw new BusinessException(String.format("Asset.id %s not found", id));
  }


  public Asset findLocally(String marketCode, String code) {
    // Search Local
    Optional<Asset> optionalAsset =
        assetRepository.findByMarketCodeAndCode(marketCode.toUpperCase(), code.toUpperCase());
    return optionalAsset.map(this::hydrateAsset).orElse(null);
  }

  public Asset backFillMissingData(String id, Asset asset) {
    if (asset.getName() == null) {
      Asset figiAsset = assetEnricher.enrich(asset.getMarket().getCode(), asset.getCode());
      if (figiAsset != null) {
        figiAsset.setId(id);
        figiAsset.setMarketCode(asset.getMarket().getCode());
        assetRepository.save(figiAsset);
        return figiAsset;
      }
    }
    return asset;
  }

  private Asset hydrateAsset(Asset asset) {
    asset.setMarket(marketService.getMarket(asset.getMarketCode()));
    return asset;
  }
}
