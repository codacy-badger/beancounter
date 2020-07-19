package com.beancounter.marketdata

import com.beancounter.common.contracts.PriceResponse
import com.beancounter.common.model.Currency
import com.beancounter.common.model.Market
import com.beancounter.common.model.MarketData
import com.beancounter.common.utils.AssetUtils.Companion.getAsset
import com.beancounter.common.utils.DateUtils
import com.beancounter.marketdata.providers.alpha.AlphaConfig
import com.beancounter.marketdata.providers.alpha.AlphaPriceAdapter
import com.beancounter.marketdata.providers.alpha.AlphaService
import com.beancounter.marketdata.utils.AlphaMockUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.math.BigDecimal

/**
 * Market Data integration with AlphaVantage.co
 *
 * @author mikeh
 * @since 2019-03-03
 */
internal class AlphaPriceTest {
    private val priceMapper = AlphaPriceAdapter().alphaMapper
    private val nasdaq = Market("NASDAQ", Currency("USD"), "US/Eastern")

    @Test
    @Throws(Exception::class)
    fun is_NullAsset() {
        assertThat(priceMapper.readValue(
                ClassPathResource(AlphaMockUtils.alphaContracts + "/alphavantage-empty-response.json").file,
                PriceResponse::class.java)).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun is_GlobalResponse() {
        val marketData = priceMapper.readValue(
                ClassPathResource(AlphaMockUtils.alphaContracts + "/global-response.json").file,
                PriceResponse::class.java)
        assertThat(marketData)
                .isNotNull
                .hasNoNullFieldsOrPropertiesExcept("id", "requestDate")
        assertThat(marketData.data).isNotNull.isNotEmpty
        assertThat(marketData.data.iterator().next().changePercent).isEqualTo("0.008812")
    }

    @Test
    @Throws(Exception::class)
    fun is_CollectionFromResponseReturnedWithDividend() {
        val result = priceMapper.readValue(
                ClassPathResource(AlphaMockUtils.alphaContracts + "/kmi-backfill-response.json").file,
                PriceResponse::class.java)
        assertThat(result.data).isNotNull.hasSize(5)
        val dateUtils = DateUtils()
        for (marketData in result.data) {
            assertThat(marketData)
                    .hasFieldOrProperty("volume")
                    .hasFieldOrProperty("dividend")
                    .hasFieldOrProperty("split")
            val resolvedDate = dateUtils.getDate("2020-05-01")
            assertThat(resolvedDate).isNotNull()
            assertThat(marketData.priceDate).isNotNull()
            if (marketData.priceDate!!.compareTo(resolvedDate) == 0) {
                // Dividend
                assertThat(marketData.dividend).isEqualTo(BigDecimal("0.2625"))
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun is_MutualFundGlobalResponse() {
        val marketData = priceMapper.readValue(
                ClassPathResource(AlphaMockUtils.alphaContracts + "/pence-price-response.json").file,
                PriceResponse::class.java)
        assertThat(marketData)
                .isNotNull
                .hasNoNullFieldsOrPropertiesExcept("id", "requestDate")
    }

    @Test
    @Throws(Exception::class)
    fun is_ResponseWithoutMarketCodeSetToUs() {
        val (asset) = validateResponse(
                ClassPathResource("contracts/alpha/alphavantage-nasdaq.json").file
        )
        assertThat(asset)
                .hasFieldOrPropertyWithValue("code", "NDAQ")
                .hasFieldOrPropertyWithValue("market.code", "US")
    }

    @Throws(Exception::class)
    private fun validateResponse(jsonFile: File): MarketData {
        val priceResponse = priceMapper.readValue(jsonFile, PriceResponse::class.java)
        assertThat(priceResponse.data).isNotNull.isNotEmpty
        val marketData = priceResponse.data.iterator().next()
        assertThat(marketData)
                .isNotNull
                .hasFieldOrProperty("asset")
                .hasFieldOrProperty("priceDate")
                .hasFieldOrPropertyWithValue("open", BigDecimal("119.3700"))
                .hasFieldOrPropertyWithValue("high", BigDecimal("121.6100"))
                .hasFieldOrPropertyWithValue("low", BigDecimal("119.2700"))
                .hasFieldOrPropertyWithValue("close", BigDecimal("121.3000"))
                .hasFieldOrPropertyWithValue("volume", BigDecimal("958346").intValueExact())
        return marketData
    }

    @Test
    fun is_KnownMarketVariancesHandled() {
        val alphaConfig = AlphaConfig()
        val alphaService = AlphaService(alphaConfig)
        // No configured support to handle the market
        assertThat(alphaService.isMarketSupported(Market("NZX", Currency("NZD"))))
                .isFalse()
        val msft = getAsset("NASDAQ", "MSFT")
        assertThat(alphaConfig.getPriceCode(msft)).isEqualTo("MSFT")
        val ohi = getAsset("NYSE", "OHI")
        assertThat(alphaConfig.getPriceCode(ohi)).isEqualTo("OHI")
        val abc = getAsset("AMEX", "ABC")
        assertThat(alphaConfig.getPriceCode(abc)).isEqualTo("ABC")
        val nzx = getAsset("NZX", "AIRNZ")
        assertThat(alphaConfig.getPriceCode(nzx)).isEqualTo("AIRNZ.NZX")
    }

    @Test
    fun is_PriceDateAccountingForWeekends() {
        val dateUtils = DateUtils()
        val alphaConfig = AlphaConfig()
        // Sunday
        val computedDate = alphaConfig.getMarketDate(nasdaq, "2020-04-26")
        // Resolves to Friday
        assertThat(computedDate).isEqualTo(dateUtils.getDate("2020-04-24"))
    }

    @Test
    fun is_PriceDateInThePastConstant() {
        val dateUtils = DateUtils()
        val alphaConfig = AlphaConfig()
        val computedDate = alphaConfig.getMarketDate(nasdaq, "2020-04-28")
        assertThat(computedDate).isEqualTo(dateUtils.getDate("2020-04-28"))
    }
}