package com.beancounter.ingest;

import static com.beancounter.common.utils.AssetUtils.getAsset;
import static com.beancounter.ingest.UnitTestHelper.getPortfolio;
import static org.assertj.core.api.Assertions.assertThat;

import com.beancounter.common.model.Transaction;
import com.beancounter.ingest.reader.Filter;
import org.junit.jupiter.api.Test;


class TestFilter {
  @Test
  void is_FilteredAssetsCaseInsensitive() {
    Filter filter = new Filter("Code");
    assertThat(filter.hasFilter()).isTrue();
    assertThat(filter.inFilter(Transaction.builder()
        .asset(getAsset("Code", "Market"))
        .portfolio(getPortfolio())
        .build())).isTrue();

    assertThat(filter.inFilter(Transaction.builder()
        .asset(getAsset("code", "Market"))
        .portfolio(getPortfolio())
        .build())).isTrue();

  }

  @Test
  void is_NotInFilter() {
    Filter filter = new Filter(null);
    assertThat(filter.hasFilter()).isFalse();
  }
}
