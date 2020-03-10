package com.beancounter.common.contracts;

import com.beancounter.common.model.Currency;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class CurrencyResponse implements Payload<Collection<Currency>> {
  private Collection<Currency> data;
}
