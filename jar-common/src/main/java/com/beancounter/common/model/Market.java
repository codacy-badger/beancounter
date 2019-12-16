package com.beancounter.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import javax.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A stock exchange.
 *
 * @author mikeh
 * @since 2019-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Market {
  private String id;
  private String code;
  @Transient
  private Currency currency;
  @JsonIgnore
  private String currencyCode;
  private TimeZone timezone;
  private String timezoneId;

  @Builder.Default
  @JsonIgnore
  private Map<String, String> aliases = new HashMap<>();

}
