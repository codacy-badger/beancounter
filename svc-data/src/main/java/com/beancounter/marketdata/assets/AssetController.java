package com.beancounter.marketdata.assets;

import com.beancounter.common.contracts.AssetRequest;
import com.beancounter.common.contracts.AssetResponse;
import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.model.Asset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Market Data MVC.
 *
 * @author mikeh
 * @since 2019-01-29
 */
@RestController
@RequestMapping("/assets")
@CrossOrigin
public class AssetController {

  private AssetService assetService;

  @Autowired
  AssetController(AssetService assetService) {
    this.assetService = assetService;
  }

  @GetMapping(value = "/{market}/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
  Asset getAsset(@PathVariable String market, @PathVariable String code) {
    Asset asset = assetService.find(market, code);
    if (asset == null) {
      throw new BusinessException(String.format("No asset found for %s/%s", market, code));
    }
    return asset;
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  AssetResponse update(@RequestBody AssetRequest assetRequest) {
    return assetService.process(assetRequest);
  }

}