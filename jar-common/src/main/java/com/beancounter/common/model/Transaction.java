package com.beancounter.common.model;

import com.beancounter.common.identity.TransactionId;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;


/**
 * Representation of a Financial Transaction.
 *
 * @author mikeh
 * @since 2019-02-07
 */
@SuppressWarnings("UnusedAssignment")
@Data
@Builder
@ToString(of = {"asset", "trnType", "id"})
@JsonDeserialize(builder = Transaction.TransactionBuilder.class)
public class Transaction {
  private TrnType trnType;
  @NonNull
  private Asset asset;
  @NonNull
  private Portfolio portfolio;
  private Asset cashAsset;
  private TransactionId id;
  private Currency tradeCurrency;
  private Currency cashCurrency;

  private Date tradeDate;
  private Date settleDate;

  private BigDecimal quantity;
  // In trade Currency
  private BigDecimal price;
  @Builder.Default
  // In trade Currency
  private BigDecimal fees = BigDecimal.ZERO;
  @Builder.Default
  // In trade Currency
  private BigDecimal tax = BigDecimal.ZERO;
  @Builder.Default
  // In trade Currency
  private BigDecimal tradeAmount = BigDecimal.ZERO;
  private BigDecimal cashAmount;

  // Trade CCY to cash settlement currency
  private BigDecimal tradeCashRate;
  // Trade Currency to system Base Currency
  private BigDecimal tradeBaseRate;
  // Trade CCY to portfolio reference  currency
  private BigDecimal tradePortfolioRate;

  private String comments;

  @SuppressWarnings("WeakerAccess")
  @JsonPOJOBuilder(withPrefix = "")
  public static class TransactionBuilder {

  }
}
