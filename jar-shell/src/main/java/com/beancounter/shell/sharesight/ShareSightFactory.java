package com.beancounter.shell.sharesight;

import com.beancounter.shell.ingest.TrnAdapter;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Factory for getting the appropriate row transformer.
 *
 * @author mikeh
 * @since 2019-03-10
 */
@Service
public class ShareSightFactory {

  private static final Set<String> TRADE_TYPES = Set.of("BUY", "SELL", "SPLIT");
  private ShareSightTradeAdapter shareSightTrade;
  private ShareSightDividendAdapater shareSightDivi;

  public ShareSightFactory(ShareSightDividendAdapater shareSightDivi,
                           ShareSightTradeAdapter shareSightTrade) {
    this.shareSightDivi = shareSightDivi;
    this.shareSightTrade = shareSightTrade;
  }

  /**
   * Figure out if we're dealing with a Trade or Dividend row.
   *
   * @param row analyze this
   * @return appropriate transformer
   */
  public TrnAdapter adapter(List<Object> row) {
    if (TRADE_TYPES.contains(row.get(ShareSightTradeAdapter.type).toString().toUpperCase())) {
      return shareSightTrade;
    }
    return shareSightDivi;
  }

}
