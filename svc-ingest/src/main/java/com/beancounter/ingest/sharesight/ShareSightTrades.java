package com.beancounter.ingest.sharesight;

import com.beancounter.common.exception.BusinessException;
import com.beancounter.common.model.Asset;
import com.beancounter.common.model.Currency;
import com.beancounter.common.model.Market;
import com.beancounter.common.model.Portfolio;
import com.beancounter.common.model.Transaction;
import com.beancounter.common.model.TrnType;
import com.beancounter.ingest.reader.Transformer;
import com.beancounter.ingest.sharesight.common.ShareSightHelper;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Converts from the ShareSight trade format.
 *
 * <p>ShareSight amounts are in Portfolio currency; BC expects values in trade currency.
 *
 * @author mikeh
 * @since 2019-02-08
 */
@Service
@Slf4j
public class ShareSightTrades implements Transformer {

  public static final int market = 0;
  public static final int code = 1;
  public static final int name = 2;
  public static final int type = 3;
  public static final int date = 4;
  public static final int quantity = 5;
  public static final int price = 6;
  public static final int brokerage = 7;
  public static final int currency = 8;
  public static final int fxRate = 9;
  public static final int value = 10;
  public static final int comments = 11;
  private final ShareSightHelper shareSightHelper;

  @Autowired
  public ShareSightTrades(ShareSightHelper shareSightHelper) {
    this.shareSightHelper = shareSightHelper;
  }

  @Override
  public Transaction from(List row, Portfolio portfolio, Currency baseCurrency)
      throws ParseException {
    try {
      TrnType trnType = shareSightHelper.resolveType(row.get(type).toString());
      if (trnType == null) {
        throw new BusinessException(String.format("Unsupported type %s", row.get(type).toString()));
      }

      Asset asset = Asset.builder().code(
          row.get(code).toString())
          .name(row.get(name).toString())
          .market(Market.builder().code(row.get(market).toString()).build())
          .build();

      String comment = (row.size() == 12 ? row.get(comments).toString() : null);


      BigDecimal tradeRate = BigDecimal.ZERO;
      BigDecimal tradeAmount = BigDecimal.ZERO;
      if (trnType != TrnType.SPLIT) {
        tradeRate = new BigDecimal(row.get(fxRate).toString());
        tradeAmount = shareSightHelper.parseDouble(row.get(value));
      }

      return Transaction.builder()
          .asset(asset)
          .trnType(trnType)
          .portfolio(portfolio)
          .baseCurrency(baseCurrency)
          .quantity(shareSightHelper.parseDouble(row.get(quantity).toString()))
          .price(shareSightHelper.parseDouble(row.get(price).toString()))
          .fees(shareSightHelper.safeDivide(
              new BigDecimal(row.get(brokerage).toString()), tradeRate))
          .tradeAmount(shareSightHelper.getValueWithFx(tradeAmount, tradeRate))
          .tradeDate(shareSightHelper.parseDate(row.get(date).toString()))
          .tradeCurrency(Currency.builder().code(row.get(currency).toString()).build())
          .tradeRate(tradeRate) // Trade to Portfolio Reference rate
          .comments(comment)
          .build()
          ;
    } catch (RuntimeException re) {
      log.error(String.valueOf(row));
      throw re;
    }

  }

  @Override
  public boolean isValid(List row) {
    if (row.size() > 6) {
      return !row.get(0).toString().equalsIgnoreCase("market");
    }
    return false;
  }


}
