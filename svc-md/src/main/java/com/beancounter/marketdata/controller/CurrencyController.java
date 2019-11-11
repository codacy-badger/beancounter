package com.beancounter.marketdata.controller;

import com.beancounter.common.contracts.CurrencyResponse;
import com.beancounter.marketdata.config.StaticConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Market Data MVC.
 *
 * @author mikeh
 * @since 2019-01-29
 */
@RestController
@RequestMapping("/currencies")
public class CurrencyController {

  private StaticConfig staticConfig;

  @Autowired
  void setStaticConfig(StaticConfig staticConfig) {
    this.staticConfig = staticConfig;
  }

  @GetMapping
  @CrossOrigin
  CurrencyResponse getCurrencies() {
    return CurrencyResponse.builder()
        .data(staticConfig.getCurrencyCode())
        .build();
  }
}