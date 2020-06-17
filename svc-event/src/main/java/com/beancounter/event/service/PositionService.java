package com.beancounter.event.service;

import com.beancounter.auth.common.TokenService;
import com.beancounter.client.AssetService;
import com.beancounter.client.services.PortfolioServiceClient;
import com.beancounter.common.contracts.PortfoliosResponse;
import com.beancounter.common.contracts.PositionResponse;
import com.beancounter.common.event.CorporateEvent;
import com.beancounter.common.input.TrustedTrnEvent;
import com.beancounter.common.input.TrustedTrnQuery;
import com.beancounter.common.model.Portfolio;
import com.beancounter.common.model.Position;
import com.beancounter.common.utils.DateUtils;
import com.beancounter.event.integration.PositionGateway;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PositionService {
  private final EventBehaviourFactory behaviourFactory;
  private final PositionGateway positionGateway;
  private final TokenService tokenService;
  private final PortfolioServiceClient portfolioService;
  private final AssetService assetService;
  private final DateUtils dateUtils = new DateUtils();

  public PositionService(EventBehaviourFactory eventBehaviourFactory,
                         PortfolioServiceClient portfolioService,
                         AssetService assetService,
                         TokenService tokenService,
                         PositionGateway positionGateway) {
    this.behaviourFactory = eventBehaviourFactory;
    this.positionGateway = positionGateway;
    this.tokenService = tokenService;
    this.assetService = assetService;
    this.portfolioService = portfolioService;

  }

  public PortfoliosResponse findWhereHeld(String assetId, LocalDate date) {
    return portfolioService.getWhereHeld(assetId, date);
  }

  public TrustedTrnEvent process(Portfolio portfolio, CorporateEvent event) {
    PositionResponse positionResponse = positionGateway
        .query(
            tokenService.getBearerToken(),
            TrustedTrnQuery.builder()
                .portfolio(portfolio)
                .tradeDate(event.getRecordDate())
                .assetId(event.getAssetId())
                .build());
    if (positionResponse.getData() != null && positionResponse.getData().hasPositions()) {
      Position position = positionResponse.getData().getPositions().values().iterator().next();
      if (position.getQuantityValues().getTotal().compareTo(BigDecimal.ZERO) != 0) {
        Event behaviour = behaviourFactory.getAdapter(event);
        assert (behaviour != null);
        return behaviour
            .calculate(positionResponse.getData().getPortfolio(), position, event);
      }
    }
    return null;
  }

  public void backFillEvents(String code, String date) {
    Portfolio portfolio = portfolioService.getPortfolioByCode(code);
    String asAt;
    if (date == null || date.equalsIgnoreCase("today")) {
      asAt = dateUtils.today();
    } else {
      asAt = dateUtils.getDateString(dateUtils.getDate(date));
    }

    PositionResponse results = positionGateway.get(tokenService.getBearerToken(),
        portfolio.getCode(),
        asAt);

    for (String key : results.getData().getPositions().keySet()) {
      Position position = results.getData().getPositions().get(key);
      if (position.getQuantityValues().getTotal().compareTo(BigDecimal.ZERO) != 0) {
        assetService.backFillEvents(position.getAsset());
      }
    }
  }
}
