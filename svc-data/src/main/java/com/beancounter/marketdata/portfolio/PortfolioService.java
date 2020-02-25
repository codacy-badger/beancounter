package com.beancounter.marketdata.portfolio;

import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.exception.UnauthorizedException;
import com.beancounter.common.model.Portfolio;
import com.beancounter.common.model.SystemUser;
import com.beancounter.marketdata.currency.CurrencyService;
import com.beancounter.marketdata.registration.SystemUserService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PortfolioService {
  private CurrencyService currencyService;
  private PortfolioRepository portfolioRepository;
  private SystemUserService systemUserService;

  PortfolioService(
      CurrencyService currencyService,
      PortfolioRepository portfolioRepository,
      SystemUserService systemUserService
  ) {
    this.currencyService = currencyService;
    this.portfolioRepository = portfolioRepository;
    this.systemUserService = systemUserService;
  }

  public Collection<Portfolio> save(SystemUser owner, Collection<Portfolio> portfolios) {
    verifyOwner(owner);
    Collection<Portfolio> results = new ArrayList<>();
    portfolioRepository.saveAll(prepare(owner, portfolios)).forEach(results::add);
    return results;
  }

  private void verifyOwner(SystemUser owner) {
    if (owner == null) {
      throw new BusinessException("Unable to identify the owner");
    }
    if (!owner.getActive()) {
      throw new BusinessException("User is not active");
    }
  }

  private Collection<Portfolio> prepare(SystemUser owner, Collection<Portfolio> portfolios) {
    for (Portfolio portfolio : portfolios) {
      portfolio.setCode(portfolio.getCode().toUpperCase());
      portfolio.setOwner(owner);
    }
    return portfolios;
  }

  private SystemUser getOrThrow() {
    SystemUser systemUser = systemUserService.getActiveUser();
    if (systemUser == null) {
      throw new UnauthorizedException("You are not authorized to perform this action");
    }
    return systemUser;

  }

  private boolean canView(SystemUser systemUser, Portfolio found) {
    return found.getOwner().getId().equals(systemUser.getId());
  }

  public Collection<Portfolio> getPortfolios() {
    SystemUser systemUser = getOrThrow();
    Collection<Portfolio> results = new ArrayList<>();
    Iterable<Portfolio> portfolios = portfolioRepository.findByOwner(systemUser);
    for (Portfolio portfolio : portfolios) {
      portfolio.setBase(currencyService.getCode(portfolio.getBase().getCode()));
      portfolio.setCurrency(currencyService.getCode(portfolio.getCurrency().getCode()));
      results.add(portfolio);
    }
    return results;
  }

  public Portfolio find(String id) {
    SystemUser systemUser = getOrThrow();
    // ToDo: Check active status?
    Optional<Portfolio> found = portfolioRepository.findById(id);
    Portfolio portfolio = found.orElseThrow(()
        -> new BusinessException(String.format("Could not find a portfolio with ID %s", id)));

    if (canView(systemUser, portfolio)) {
      return portfolio;
    }

    throw new BusinessException(String.format("Could not find a portfolio with ID %s", id));

  }

  public Portfolio findByCode(String code) {
    SystemUser systemUser = getOrThrow();
    Optional<Portfolio> found = portfolioRepository
        .findByCodeAndOwner(code.toUpperCase(), systemUser);
    Portfolio portfolio = found.orElseThrow(()
        -> new BusinessException(String.format("Could not find a portfolio with code %s", code)));

    if (canView(systemUser, portfolio)) {
      return portfolio;
    }
    throw new BusinessException(String.format("Could not find a portfolio with code %s", code));

  }
}
