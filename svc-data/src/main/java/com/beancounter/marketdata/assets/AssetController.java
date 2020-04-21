package com.beancounter.marketdata.assets;

import com.beancounter.auth.server.RoleHelper;
import com.beancounter.common.contracts.AssetRequest;
import com.beancounter.common.contracts.AssetResponse;
import com.beancounter.common.contracts.AssetUpdateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("hasRole('" + RoleHelper.OAUTH_USER + "')")
public class AssetController {

  private final AssetService assetService;

  @Autowired
  AssetController(AssetService assetService) {
    this.assetService = assetService;
  }

  @GetMapping(value = "/{market}/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
  AssetResponse getAsset(@PathVariable String market, @PathVariable String code) {
    return AssetResponse.builder().data(assetService.find(market, code)).build();
  }

  @GetMapping(value = "/{assetId}")
  AssetResponse getAsset(@PathVariable String assetId) {
    return AssetResponse.builder().data(assetService.find(assetId)).build();

  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  AssetUpdateResponse update(@RequestBody AssetRequest assetRequest) {
    return assetService.process(assetRequest);
  }

}
