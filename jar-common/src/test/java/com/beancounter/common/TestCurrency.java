package com.beancounter.common;

import static com.beancounter.common.utils.CurrencyUtils.getCurrency;
import static com.beancounter.common.utils.CurrencyUtils.getCurrencyPair;
import static org.assertj.core.api.Assertions.assertThat;

import com.beancounter.common.model.Currency;
import com.beancounter.common.model.CurrencyPair;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class TestCurrency {
  @Test
  void is_CurrencySerializing() throws Exception {

    Currency currency = Currency.builder()
        .code("SomeId")
        .name("Some Name")
        .symbol("$")
        .build();

    assertThat(currency).isNotNull();

    ObjectMapper om = new ObjectMapper();
    String json = om.writeValueAsString(currency);

    Currency fromJson = om.readValue(json, Currency.class);

    assertThat(fromJson).isEqualTo(currency);
  }

  @Test
  void is_FromMapConverting() {
    Currency currency = Currency.builder()
        .id("TWEE")
        .code("CODE")
        .name("Name")
        .symbol("$")
        .build();

    Map<String, Object> mapCurrency = new HashMap<>();
    mapCurrency.put("code", currency.getCode());
    mapCurrency.put("name", currency.getName());
    mapCurrency.put("symbol", currency.getSymbol());

    Currency fromMap = Currency.of(currency.getId(), mapCurrency);
    assertThat(fromMap).isEqualToComparingFieldByField(currency);
    assertThat(fromMap.toString()).isEqualTo("Currency(code=CODE)");
  }

  @Test
  @VisibleForTesting
  void is_NullCurrencyCodeWorking() {
    Assertions.assertThrows(NullPointerException.class, () -> {
      Currency.builder().code(null).build();
    });
  }

  @Test
  @VisibleForTesting
  void is_GetCurrencyWorking() {
    Currency currency = getCurrency("NZD");
    assertThat(currency).hasFieldOrPropertyWithValue("code", "NZD");
  }

  @Test
  @VisibleForTesting
  void is_CurrencyPairConsistent() {
    String trade = "NZD";
    String report = "USD";
    CurrencyPair byCode = CurrencyPair.builder()
        .from(report)
        .to(trade)
        .build();

    CurrencyPair byCurrency = CurrencyPair.from(getCurrency(report), getCurrency(trade));
    assertThat(byCode).isEqualToComparingFieldByField(byCurrency);

  }

  @Test
  void is_CorrectCurrencyPair() {
    assertThat(getCurrencyPair(null, null, null)).isNull();

    assertThat(getCurrencyPair(BigDecimal.TEN,
        getCurrency("NZD"),
        getCurrency("USD")))
        .isNull(); // We have a rate, so don't request one

    assertThat(getCurrencyPair(null, getCurrency("USD"), getCurrency("USD")))
        .isNull();

    assertThat(getCurrencyPair(null, getCurrency("NZD"), getCurrency("USD")))
        .isNotNull()
        .hasFieldOrPropertyWithValue("from", "NZD")
        .hasFieldOrPropertyWithValue("to", "USD");

  }

}
