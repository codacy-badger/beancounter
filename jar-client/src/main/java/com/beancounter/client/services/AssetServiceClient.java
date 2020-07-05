package com.beancounter.client.services;

import com.beancounter.auth.common.TokenService;
import com.beancounter.client.AssetService;
import com.beancounter.common.contracts.AssetRequest;
import com.beancounter.common.contracts.AssetResponse;
import com.beancounter.common.contracts.AssetUpdateResponse;
import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.model.Asset;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Service
public class AssetServiceClient implements AssetService {

  private static final Logger log = org.slf4j.LoggerFactory.getLogger(AssetServiceClient.class);
  private final AssetGateway assetGateway;
  private final TokenService tokenService;
  @Value("${marketdata.url:http://localhost:9510/api}")
  private String marketDataUrl;

  AssetServiceClient(AssetGateway assetGateway, TokenService tokenService) {
    this.tokenService = tokenService;
    this.assetGateway = assetGateway;
  }

  @PostConstruct
  void logConfig() {
    log.info("marketdata.url: {}", marketDataUrl);
  }

  @Override
  public AssetUpdateResponse process(AssetRequest assetRequest) {
    return assetGateway.process(tokenService.getBearerToken(), assetRequest);
  }

  @Override
  @Async
  public void backFillEvents(String assetId) {
    log.debug("Back fill for {}", assetId);
    assetGateway.backFill(tokenService.getBearerToken(), assetId);
  }

  @Override
  public Asset find(String assetId) {
    AssetResponse response = assetGateway.find(tokenService.getBearerToken(), assetId);
    if (response == null) {
      throw new BusinessException(String.format("Asset %s not found", assetId));
    }
    return response.getData();
  }


  @FeignClient(name = "assets",
      url = "${marketdata.url:http://localhost:9510/api}")
  public interface AssetGateway {
    @PostMapping(value = "/assets",
        produces = {MediaType.APPLICATION_JSON_VALUE},
        consumes = {MediaType.APPLICATION_JSON_VALUE})
    AssetUpdateResponse process(@RequestHeader("Authorization") String bearerToken,
                                AssetRequest assetRequest);

    @PostMapping(value = "/assets/{id}/events",
        produces = {MediaType.APPLICATION_JSON_VALUE},
        consumes = {MediaType.APPLICATION_JSON_VALUE})
    void backFill(@RequestHeader("Authorization") String bearerToken,
                  @PathVariable("id") String assetId);

    @GetMapping(value = "/assets/{id}",
        consumes = {MediaType.APPLICATION_JSON_VALUE})
    AssetResponse find(@RequestHeader("Authorization") String bearerToken,
                       @PathVariable("id") String assetId);
  }


}
