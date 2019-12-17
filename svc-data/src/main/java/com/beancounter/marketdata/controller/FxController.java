package com.beancounter.marketdata.controller;

import com.beancounter.common.contracts.FxRequest;
import com.beancounter.common.contracts.FxResponse;
import com.beancounter.marketdata.service.FxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@RequestMapping("/fx")
public class FxController {

  private FxService fxService;

  @Autowired
  FxController(FxService fxService) {
    this.fxService = fxService;
  }

  @PostMapping
  @CrossOrigin
  FxResponse getRates(@RequestBody FxRequest fxRequest) {
    return fxService.getRates(fxRequest);
  }

}