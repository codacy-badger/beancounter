package com.beancounter.position.model;

import com.beancounter.common.model.Position;
import com.beancounter.common.model.Price;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author mikeh
 * @since 2019-02-08
 */
@Data
@Builder
public class MarketValue {
    private Price price;
    private Position position;

    // Local Market Value in market currency
    public BigDecimal getMarketValue() {
        return price.getPrice().multiply(position.getQuantity().getTotal());
    }

    public BigDecimal getMarketCost() {
        return position.getMoneyValues().getMarketCost();
    }
}
