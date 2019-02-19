package com.beancounter.position.counter;

import com.beancounter.common.model.MoneyValues;
import com.beancounter.common.model.Position;
import com.beancounter.common.model.QuantityValues;
import com.beancounter.common.model.Transaction;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Adds transactions into Positions.
 *
 * @author mikeh
 * @since 2019-02-07
 */
@Service
public class Accumulator {

  private TransactionConfiguration transactionConfiguration;
  private MathContext mathContext = new MathContext( 10);

  @Autowired
  public Accumulator(TransactionConfiguration transactionConfiguration) {
    this.transactionConfiguration = transactionConfiguration;
  }

  /**
   * Main calculation routine.
   *
   * @param transaction Transaction to add
   * @param position Position to accumulate the transaction into
   * @return result object
   */
  public Position accumulate(Transaction transaction, Position position) {
    if (transactionConfiguration.isPurchase(transaction)) {
      return accumulateBuy(transaction, position);
    } else if (transactionConfiguration.isSale(transaction)) {
      return accumulateSell(transaction, position);
    }
    return position;
  }

  private Position accumulateBuy(Transaction transaction, Position position) {
    QuantityValues quantityValues = position.getQuantityValues();
    quantityValues.setPurchased(quantityValues.getPurchased().add(transaction.getQuantity()));
    MoneyValues moneyValues = position.getMoneyValues();
    moneyValues.setMarketCost(
        moneyValues.getMarketCost().add(transaction.getTradeAmount()));

    moneyValues.setPurchases(
        moneyValues.getPurchases().add(transaction.getTradeAmount()));

    moneyValues.setCostBasis(moneyValues.getCostBasis().add(transaction.getTradeAmount()));
    if (!moneyValues.getCostBasis().equals(BigDecimal.ZERO)) {

      moneyValues.setAverageCost(
          moneyValues.getCostBasis()
            .divide(quantityValues.getTotal(), mathContext));

    }

    return position;
  }

  private Position accumulateSell(Transaction transaction, Position position) {
    BigDecimal soldQuantity = transaction.getQuantity();
    if (soldQuantity.doubleValue() > 0) {
      // Sign the quantities
      soldQuantity = new BigDecimal(0 - transaction.getQuantity().doubleValue());
    }

    QuantityValues quantityValues = position.getQuantityValues();
    quantityValues.setSold(quantityValues.getSold().add(soldQuantity));
    MoneyValues moneyValues = position.getMoneyValues();

    moneyValues.setSales(
        moneyValues.getSales().add(transaction.getTradeAmount()));

    if (!transaction.getTradeAmount().equals(BigDecimal.ZERO)) {
      BigDecimal tradeCost = transaction.getTradeAmount()
          .divide(transaction.getQuantity(), mathContext);
      BigDecimal unitProfit = tradeCost.subtract(moneyValues.getAverageCost());
      BigDecimal realisedGain = unitProfit.multiply(transaction.getQuantity());

      moneyValues.setRealisedGain(moneyValues.getRealisedGain().add(realisedGain));
    }

    if (quantityValues.getTotal().equals(BigDecimal.ZERO)) {
      moneyValues.setCostBasis(BigDecimal.ZERO);
      moneyValues.setAverageCost(BigDecimal.ZERO);
    }

    return position;
  }

}
