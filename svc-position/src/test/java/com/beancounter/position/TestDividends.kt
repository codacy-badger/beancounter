package com.beancounter.position

import com.beancounter.common.model.*
import com.beancounter.common.utils.AssetUtils.Companion.getAsset
import com.beancounter.common.utils.CurrencyUtils
import com.beancounter.common.utils.PortfolioUtils.Companion.getPortfolio
import com.beancounter.position.service.Accumulator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest(classes = [Accumulator::class])
internal class TestDividends {
    private val currencyUtils = CurrencyUtils()

    @Autowired
    private lateinit var accumulator: Accumulator

    @Test
    fun is_CashDividendAccumulated() {
        val asx = Market("ASX", currencyUtils.getCurrency("AUD"))
        val asset = getAsset(asx, "MO")
        val trn = Trn(TrnType.DIVI, asset)
        trn.tradeCashRate = BigDecimal("0.8988")
        trn.tradeAmount = BigDecimal("12.99")
        val positions = Positions(getPortfolio("TEST"))
        val position = positions[asset]
        accumulator.accumulate(trn, positions.portfolio, position)
        assertThat(position.getMoneyValues(Position.In.TRADE, asset.market.currency))
                .hasFieldOrPropertyWithValue("dividends", trn.tradeAmount)
    }
}