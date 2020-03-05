package com.beancounter.common.contracts;

import com.beancounter.common.model.Positions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PositionResponse implements Payload<Positions> {

  private Positions data;

}
