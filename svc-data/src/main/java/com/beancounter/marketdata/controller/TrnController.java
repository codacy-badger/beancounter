package com.beancounter.marketdata.controller;

import com.beancounter.auth.server.RoleHelper;
import com.beancounter.common.contracts.TrnRequest;
import com.beancounter.common.contracts.TrnResponse;
import com.beancounter.common.model.Portfolio;
import com.beancounter.marketdata.portfolio.PortfolioService;
import com.beancounter.marketdata.trn.TrnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trns")
@CrossOrigin
@PreAuthorize("hasRole('" + RoleHelper.OAUTH_USER + "')")
public class TrnController {

  private TrnService trnService;
  private PortfolioService portfolioService;

  @Autowired
  void setServices(TrnService trnService,
                   PortfolioService portfolioService) {
    this.trnService = trnService;
    this.portfolioService = portfolioService;
  }

  @GetMapping(value = "/portfolio/{portfolioId}", produces = MediaType.APPLICATION_JSON_VALUE)
  TrnResponse find(@PathVariable("portfolioId") String portfolioId) {
    Portfolio portfolio = portfolioService.find(portfolioId);

    return trnService.findForPortfolio(portfolio);
  }

  @GetMapping(value = "/{portfolioId}/{trnId}")
  TrnResponse find(
      @PathVariable("portfolioId") String portfolioId,
      @PathVariable("trnId") String trnId
  ) {
    Portfolio portfolio = portfolioService.find(portfolioId);

    return trnService.getPortfolioTrn(portfolio, trnId);
  }

  @PostMapping(
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  TrnResponse update(@RequestBody TrnRequest trnRequest) {
    Portfolio portfolio = portfolioService.find(trnRequest.getPortfolioId());
    return trnService.save(portfolio, trnRequest);
  }

  @DeleteMapping(value = "/portfolio/{portfolioId}")
  Long purge(@PathVariable("portfolioId") String portfolioId) {
    Portfolio portfolio = portfolioService.find(portfolioId);
    return trnService.purge(portfolio);
  }

  @GetMapping(value = "/{portfolioId}/asset/{assetId}", produces = MediaType.APPLICATION_JSON_VALUE)
  TrnResponse findByAsset(
      @PathVariable("portfolioId") String portfolioId,
      @PathVariable("assetId") String assetId
  ) {
    return trnService.findByPortfolioAsset(portfolioService.find(portfolioId), assetId);
  }
}
