package com.beancounter.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

/**
 * Represents an Asset held in a Portfolio.
 *
 * @author mikeh
 * @since 2019-01-28
 */
@Data
@Builder
@JsonDeserialize(builder = Position.PositionBuilder.class)
public class Position {

  @NonNull
  private Asset asset;

  @Builder.Default
  @Getter
  private QuantityValues quantityValues = QuantityValues.builder().build();

  @Builder.Default
  @Getter
  private DateValues dateValues = DateValues.builder().build();

  @Builder.Default
  @Getter
  private Map<In, MoneyValues> moneyValues = new HashMap<>();

  @JsonIgnore
  public MoneyValues getMoneyValues(In reportCurrency) {
    return moneyValues.get(reportCurrency);
  }

  /**
   * MoneyValues are tracked in various currencies.
   *
   * @param reportCurrency which model.
   * @return moneyValues in valueCurrency.
   */
  @JsonIgnore
  public MoneyValues getMoneyValues(In reportCurrency, Currency currency) {
    MoneyValues result = moneyValues.get(reportCurrency);
    if (result == null) {
      result = MoneyValues.builder()
          .currency(currency)
          .build();
      moneyValues.put(reportCurrency, result);
    }
    return result;
  }

  public enum In {
    TRADE,
    PORTFOLIO,
    BASE

  }

  @SuppressWarnings("WeakerAccess")
  @JsonPOJOBuilder(withPrefix = "")
  public static class PositionBuilder {

  }

}
