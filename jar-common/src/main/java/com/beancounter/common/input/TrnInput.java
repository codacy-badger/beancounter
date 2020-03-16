package com.beancounter.common.input;

import com.beancounter.common.identity.TrnId;
import com.beancounter.common.model.TrnType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"asset", "trnType", "id"})
public class TrnInput {
  private TrnId id;
  private TrnType trnType;
  private String asset;
  private String cashAsset;
  private String tradeCurrency;
  private String cashCurrency;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @JsonSerialize(using = LocalDateSerializer.class)
  @JsonDeserialize(using = LocalDateDeserializer.class)
  private LocalDate tradeDate;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @JsonSerialize(using = LocalDateSerializer.class)
  @JsonDeserialize(using = LocalDateDeserializer.class)
  private LocalDate settleDate;
  private BigDecimal quantity;
  private BigDecimal price;   // In trade Currency
  @Builder.Default
  private BigDecimal fees = BigDecimal.ZERO; // In trade Currency
  @Builder.Default
  private BigDecimal tax = BigDecimal.ZERO; // In trade Currency
  @Builder.Default
  private BigDecimal tradeAmount = BigDecimal.ZERO; // In trade Currency
  private BigDecimal cashAmount;
  private BigDecimal tradeCashRate; // Trade CCY to cash settlement currency
  private BigDecimal tradeBaseRate; // Trade Currency to system Base Currency
  private BigDecimal tradePortfolioRate; // Trade CCY to portfolio reference  currency
  private String comments;
}
