package com.beancounter.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.beancounter.common.contracts.AssetRequest;
import com.beancounter.common.contracts.AssetResponse;
import com.beancounter.common.contracts.AssetUpdateResponse;
import com.beancounter.common.contracts.PriceRequest;
import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Market;
import com.beancounter.common.utils.AssetUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.Test;


class TestAsset {
  private ObjectMapper om = new ObjectMapper();

  @Test
  void is_PriceRequestForAsset() {
    PriceRequest priceRequest = PriceRequest.of(AssetUtils
        .getAsset("EBAY", "NASDAQ"))
        .date("2019-10-18").build();

    assertThat(priceRequest.getAssets()).hasSize(1);
  }

  @Test
  void is_AssetResponse() throws Exception {
    AssetResponse assetResponse = AssetResponse.builder()
        .data(AssetUtils.getAsset("Blah", "as")).build();
    assetResponse.getData().setMarketCode(null);// JsonIgnore
    String json = om.writeValueAsString(assetResponse);
    AssetResponse fromJson = om.readValue(json, AssetResponse.class);

    assertThat(fromJson).isEqualToComparingFieldByField(assetResponse);
  }

  @Test
  void assetRequestSerializes() throws Exception {
    Asset asset = AssetUtils.getJsonAsset("AAA", "BBB");

    AssetRequest assetRequest = AssetRequest
        .builder()
        .data(AssetUtils.toKey(asset), asset)
        .data("second", AssetUtils.getJsonAsset("Twee", "Whee"))
        .build();

    assertThat(assetRequest.getData()).containsKeys(AssetUtils.toKey(asset));


    String json = om.writeValueAsString(assetRequest);

    AssetRequest fromJson = om.readValue(json, AssetRequest.class);

    assertThat(fromJson).isEqualTo(assetRequest);
  }

  @Test
  void assetResponseSerializes() throws Exception {
    Asset asset = AssetUtils.getJsonAsset("AAA", "BBB");

    AssetUpdateResponse assetUpdateResponse = AssetUpdateResponse
        .builder()
        .data(AssetUtils.toKey(asset), asset)
        .data("second", AssetUtils.getJsonAsset("Twee", "Whee"))
        .build();

    assertThat(assetUpdateResponse.getData()).containsKeys(AssetUtils.toKey(asset));

    ObjectMapper om = new ObjectMapper();
    String json = om.writeValueAsString(assetUpdateResponse);

    AssetUpdateResponse fromJson = om.readValue(json, AssetUpdateResponse.class);

    assertThat(fromJson).isEqualTo(assetUpdateResponse);
  }

  @Test
  void is_AssetKeyParsing() {
    Asset asset = AssetUtils.getAsset("ACODE", "MCODE");

    String keyIn = AssetUtils.toKey(asset);

    assertThat(AssetUtils.fromKey(keyIn)).isEqualTo(asset);

  }

  @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
  @Test
  void is_AssetKeyExceptionsBeingThrown() {

    assertThrows(BusinessException.class,
        () -> AssetUtils.fromKey("CodeWithNoMarket"));
    assertThrows(NullPointerException.class,
        () -> AssetUtils.fromKey(null));

    assertThrows(NullPointerException.class,
        () -> AssetUtils.getAsset(null, (Market) null));
    assertThrows(NullPointerException.class,
        () -> AssetUtils.getAsset("Twee", (Market) null));
    assertThrows(NullPointerException.class,
        () -> AssetUtils.getAsset("Twee", (String) null));
    assertThrows(NullPointerException.class,
        () -> AssetUtils.getAsset(null, (String) null));

    assertThrows(NullPointerException.class,
        () -> AssetUtils.toKey(null));
    assertThrows(NullPointerException.class,
        () -> AssetUtils.toKey("Twee", null));
    assertThrows(NullPointerException.class,
        () -> AssetUtils.toKey(null, null));
  }

  @Test
  void is_AssetsSplitByMarket() {
    Collection<Asset> assets = new ArrayList<>();
    assets.add(AssetUtils.getAsset("ABC", "AAA"));
    assets.add(AssetUtils.getAsset("123", "AAA"));
    assets.add(AssetUtils.getAsset("ABC", "BBB"));
    assets.add(AssetUtils.getAsset("123", "BBB"));
    assets.add(AssetUtils.getAsset("123", "CCC"));
    Map<String, Collection<Asset>> results = AssetUtils.split(assets);
    assertThat(results.size()).isEqualTo(3);
    assertThat(results.get("AAA")).hasSize(2);
    assertThat(results.get("BBB")).hasSize(2);
    assertThat(results.get("CCC")).hasSize(1);

  }

}
