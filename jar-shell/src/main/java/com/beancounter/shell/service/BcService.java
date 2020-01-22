package com.beancounter.shell.service;

import com.beancounter.common.contracts.AssetRequest;
import com.beancounter.common.contracts.AssetResponse;
import com.beancounter.common.contracts.CurrencyResponse;
import com.beancounter.common.contracts.MarketResponse;
import com.beancounter.common.contracts.PortfolioRequest;
import com.beancounter.common.contracts.TrnRequest;
import com.beancounter.common.contracts.TrnResponse;
import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.model.Portfolio;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BcService {
  private BcGateway bcGateway;

  @Autowired
  void setBcGateway(BcGateway bcGateway) {
    this.bcGateway = bcGateway;
  }

  @Retry(name = "data")
  public MarketResponse getMarkets() {
    return bcGateway.getMarkets();
  }

  public CurrencyResponse getCurrencies() {
    return bcGateway.getCurrencies();
  }

  public Portfolio getPortfolioById(String portfolioId) {
    PortfolioRequest request = bcGateway.getPortfolioById(portfolioId);
    if (request == null || request.getData() == null) {
      throw new BusinessException(String.format("Unable to locate portfolio id [%s]", portfolioId));
    }
    return request.getData().iterator().next();
  }

  public Portfolio getPortfolioByCode(String portfolioCode) {
    PortfolioRequest request = bcGateway.getPortfolioByCode(portfolioCode);
    if (request == null || request.getData() == null) {
      throw new BusinessException(
          String.format("Unable to locate portfolio code [%s]", portfolioCode));
    }
    return request.getData().iterator().next();

  }

  public AssetResponse getAssets(AssetRequest assetRequest) {
    return bcGateway.assets(assetRequest);
  }

  public TrnResponse write(TrnRequest trnRequest) {
    return bcGateway.write(trnRequest);
  }
}