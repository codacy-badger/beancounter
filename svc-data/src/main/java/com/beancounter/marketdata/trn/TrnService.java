package com.beancounter.marketdata.trn;

import com.beancounter.common.contracts.TrnRequest;
import com.beancounter.common.contracts.TrnResponse;
import com.beancounter.common.identity.TrnId;
import com.beancounter.common.model.Portfolio;
import com.beancounter.common.model.Trn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
public class TrnService {
  private TrnRepository trnRepository;
  private TrnAdapter trnAdapter;

  TrnService(TrnRepository trnRepository,
             TrnAdapter trnAdapter) {
    this.trnRepository = trnRepository;
    this.trnAdapter = trnAdapter;
  }

  public TrnResponse save(Portfolio portfolio, TrnRequest trnRequest) {
    log.info("Received request to write {} transactions {}",
        trnRequest.getData().size(), portfolio.getCode());
    TrnResponse results = trnAdapter.convert(portfolio, trnRequest);
    Iterable<Trn> saved = trnRepository.saveAll(results.getData());
    Collection<Trn> trns = new ArrayList<>();
    saved.forEach(trns::add);
    results.setData(trns);
    log.info("Wrote {} transactions", results.getData().size());
    return results;
  }

  public TrnResponse find(TrnId trnId) {
    Optional<Trn> found = trnRepository.findById(trnId);
    return found.map(transaction -> hydrate(Collections.singleton(transaction)))
        .orElseGet(() -> TrnResponse.builder().build());
  }

  public TrnResponse find(Portfolio portfolio, String assetId) {
    Collection<Trn> results = trnRepository.findByPortfolioIdAndAssetId(portfolio.getId(),
        assetId,
        Sort.by("asset.code")
            .and(Sort.by("tradeDate").descending()));
    log.debug("Found {} for portfolio {} and asset {}",
        results.size(),
        portfolio.getCode(),
        assetId
    );
    return hydrate(results);
  }

  public TrnResponse find(Portfolio portfolio) {
    Collection<Trn> results = trnRepository.findByPortfolioId(portfolio.getId(),
        Sort.by("asset.code")
            .and(Sort.by("tradeDate")));
    log.debug("Found {} for portfolio {}", results.size(), portfolio.getCode());
    return hydrate(results);
  }

  /**
   * Purge transactions for a portfolio.
   *
   * @param portfolio portfolio owned by the caller
   * @return number of deleted transactions
   */
  public long purge(Portfolio portfolio) {
    return trnRepository.deleteByPortfolioId(portfolio.getId());
  }

  private Trn setAssets(Trn trn) {
    trn.setAsset(trnAdapter.hydrate(trn.getAsset()));
    trn.setCashAsset(trnAdapter.hydrate(trn.getCashAsset()));
    return trn;
  }

  private TrnResponse hydrate(Iterable<Trn> trns) {
    TrnResponse trnResponse = TrnResponse.builder()
        .build();
    Collection<Trn> trnCollection = new ArrayList<>();
    for (Trn trn : trns) {
      trnCollection.add(setAssets(trn));
    }
    trnResponse.setData(trnCollection);
    return trnResponse;
  }
}
