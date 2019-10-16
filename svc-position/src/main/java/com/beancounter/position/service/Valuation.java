package com.beancounter.position.service;

import com.beancounter.position.model.Positions;

/**
 * Valuation services are responsible for computing
 * the market value of Positions.
 * 
 * @author mikeh
 * @since 2019-02-24
 */
public interface Valuation {

  /**
   * Values positions. This should also set the Asset details as the caller has only
   * minimal knowledge.  MarketData contains asset and market details
   *
   * @param positions to value
   * @return positions with values and hydrated Asset objects
   */
  Positions value(Positions positions);


}
