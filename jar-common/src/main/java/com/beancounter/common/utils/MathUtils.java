package com.beancounter.common.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import lombok.experimental.UtilityClass;

/**
 * Controls the way BC will deal with Division and Multiplication when it comes to Fx Rates.
 */
@UtilityClass
public class MathUtils {

  private final MathContext mathContext = new MathContext(10);

  private final int moneyScale = 2;
  private final int percentScale = 6;

  public BigDecimal divide(BigDecimal money, BigDecimal rate) {
    if (isUnset(rate)) {
      return money;
    }

    if (isUnset(money)) {
      return money;
    }
    return money.divide(rate, moneyScale, RoundingMode.HALF_UP);
  }

  public BigDecimal multiply(BigDecimal money, BigDecimal rate) {
    return multiply(money, rate, moneyScale);
  }

  public BigDecimal multiply(BigDecimal money, BigDecimal rate, int moneyScale) {
    if (isUnset(rate)) {
      return money;
    }

    if (isUnset(money)) {
      return money;
    }

    return money.multiply(rate).abs().setScale(moneyScale, RoundingMode.HALF_UP);
  }

  public BigDecimal percent(BigDecimal currentValue, BigDecimal oldValue) {
    return percent(currentValue, oldValue, percentScale);
  }

  public BigDecimal percent(BigDecimal previous, BigDecimal current, int percentScale) {
    if (isUnset(previous) || isUnset(current)) {
      return null;
    }

    return previous.divide(current, percentScale, RoundingMode.HALF_UP);
  }

  // Null and Zero are treated as "unSet"
  public boolean isUnset(BigDecimal value) {
    return value == null || BigDecimal.ZERO.compareTo(value) == 0;
  }

  public MathContext getMathContext() {
    return mathContext;
  }

  public BigDecimal add(BigDecimal value, BigDecimal amount) {
    return value.add(amount).setScale(moneyScale, RoundingMode.HALF_UP);
  }

  public BigDecimal parse(String value, NumberFormat numberFormat) throws ParseException {
    if (value == null) {
      return null;
    }
    if (value.isBlank()) {
      return BigDecimal.ZERO;
    }
    return new BigDecimal(numberFormat.parse(value.replace("\"", "")).toString());
  }

  public BigDecimal get(String money) {
    if (money == null) {
      return null;
    }
    return new BigDecimal(money);
  }

  public boolean hasValidRate(BigDecimal rate) {
    if (rate == null) {
      return false;
    }
    if (rate.compareTo(BigDecimal.ZERO) == 0) {
      return false;
    }
    return rate.compareTo(BigDecimal.ONE) != 0;
  }
}
