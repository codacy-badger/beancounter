package com.beancounter.marketdata.trn

import com.beancounter.common.contracts.TrnRequest
import com.beancounter.common.input.TrnInput
import com.beancounter.common.model.CallerRef
import com.beancounter.common.model.Currency
import com.beancounter.common.model.Trn
import com.beancounter.common.model.TrnType
import com.beancounter.common.utils.AssetKeyUtils.Companion.toKey
import com.beancounter.common.utils.AssetUtils.Companion.getAsset
import com.beancounter.common.utils.DateUtils
import com.beancounter.common.utils.PortfolioUtils.Companion.getPortfolio
import com.beancounter.common.utils.TradeCalculator
import com.beancounter.key.KeyGenUtils
import com.beancounter.marketdata.Constants.Companion.MSFT
import com.beancounter.marketdata.Constants.Companion.NASDAQ
import com.beancounter.marketdata.Constants.Companion.USD
import com.beancounter.marketdata.Constants.Companion.usdCashBalance
import com.beancounter.marketdata.assets.AssetService
import com.beancounter.marketdata.currency.CurrencyService
import com.beancounter.marketdata.markets.MarketConfig
import com.beancounter.marketdata.portfolio.PortfolioService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.math.BigDecimal
import java.util.Locale

/**
 * TRN Adapter tests.
 */
@SpringBootTest(
    classes = [
        TrnAdapter::class,
        TradeCalculator::class,
        CashServices::class,
        MarketConfig::class,
        KeyGenUtils::class
    ]
)
internal class TrnAdapterTest {

    @MockBean
    private lateinit var portfolioService: PortfolioService

    @MockBean
    private lateinit var assetService: AssetService

    @MockBean
    private lateinit var currencyService: CurrencyService

    @Autowired
    private lateinit var trnAdapter: TrnAdapter

    @Autowired
    private lateinit var keyGenUtils: KeyGenUtils

    final val asset = MSFT
    final val price: BigDecimal = BigDecimal("10.99")
    private val theDate = DateUtils().getDate("2019-10-10")
    private val one = "1"
    val portfolioId = "abc"

    private val priceProp = "price"

    private val tradeDateProp = "tradeDate"
    private val versionProp = "version"
    private val quantityProp = "quantity"

    private val trnTypeProp = "trnType"
    private val commentsProp = "comments"

    private val tradeAmountProp = "tradeAmount"

    @Test
    fun buyInputToTrnComputingTradeAmount() {
        val trnInput = TrnInput(
            CallerRef(portfolioId.uppercase(Locale.getDefault()), one, one),
            assetId = asset.id,
            trnType = TrnType.BUY,
            quantity = BigDecimal.TEN,
            price = price,
            tradeCurrency = USD.code,
            cashAssetId = toKey("USD-X", "USER"),
            tradeDate = theDate,
            cashAmount = BigDecimal("100.99"),
            cashCurrency = USD.code,
            tradeCashRate = BigDecimal("1.99"),
            tradePortfolioRate = price,
            tradeBaseRate = BigDecimal.ONE,
            comments = "Comment",
        )
        // TradeAmount should be computed for a buy
        trnInput.settleDate = theDate

        val trnRequest = TrnRequest(portfolioId, arrayOf(trnInput))
        Mockito.`when`(portfolioService.find(portfolioId))
            .thenReturn(getPortfolio(portfolioId))
        Mockito.`when`(assetService.find(trnInput.assetId))
            .thenReturn(MSFT)
        Mockito.`when`(assetService.find("USD-X:USER"))
            .thenReturn(usdCashBalance)

        Mockito.`when`(currencyService.getCode(USD.code))
            .thenReturn(Currency(USD.code))
        val trnResponse = trnAdapter.convert(portfolioService.find(portfolioId), trnRequest)
        assertThat(trnResponse).isNotNull
        assertThat(trnResponse.data).hasSize(1)
        assertThat(trnResponse.data.iterator().next())
            .hasFieldOrPropertyWithValue(quantityProp, trnInput.quantity)
            .hasFieldOrPropertyWithValue(tradeDateProp, trnInput.tradeDate)
            .hasFieldOrPropertyWithValue("settleDate", trnInput.settleDate)
            .hasFieldOrPropertyWithValue("fees", trnInput.fees)
            .hasFieldOrPropertyWithValue("cashAmount", trnInput.cashAmount)
            .hasFieldOrPropertyWithValue(priceProp, trnInput.price)
            .hasFieldOrPropertyWithValue(quantityProp, trnInput.quantity)
            .hasFieldOrPropertyWithValue(versionProp, Trn.latestVersion)
            .hasFieldOrPropertyWithValue("tradeBaseRate", trnInput.tradeBaseRate)
            .hasFieldOrPropertyWithValue("tradeCashRate", trnInput.tradeCashRate)
            .hasFieldOrPropertyWithValue("tradePortfolioRate", trnInput.tradePortfolioRate)
            .hasFieldOrPropertyWithValue("tradeBaseRate", trnInput.tradeBaseRate)
            .hasFieldOrPropertyWithValue("tradeCurrency.code", trnInput.tradeCurrency)
            .hasFieldOrPropertyWithValue("cashAsset.priceSymbol", trnInput.tradeCurrency)
            .hasFieldOrPropertyWithValue("cashCurrency.code", null)
            .hasFieldOrPropertyWithValue(tradeAmountProp, BigDecimal("109.90"))
            .hasFieldOrPropertyWithValue(trnTypeProp, trnInput.trnType)
            .hasFieldOrPropertyWithValue(commentsProp, trnInput.comments)
    }

    @Test
    fun diviInputToTrnComputingTradeAmount() {
        val tradeAmount = BigDecimal("12.22")
        val trnInput = TrnInput(
            CallerRef(portfolioId.uppercase(Locale.getDefault()), one, one),
            asset.id,
            trnType = TrnType.DIVI,
            quantity = BigDecimal.TEN,
            price = price,
            tradeAmount = tradeAmount,
        )

        val trnRequest = TrnRequest(portfolioId, arrayOf(trnInput))
        Mockito.`when`(portfolioService.find(portfolioId))
            .thenReturn(getPortfolio(portfolioId))
        Mockito.`when`(assetService.find(trnInput.assetId))
            .thenReturn(MSFT)
        Mockito.`when`(currencyService.getCode(USD.code))
            .thenReturn(Currency(USD.code))
        val trnResponse = trnAdapter.convert(portfolioService.find(portfolioId), trnRequest)
        assertThat(trnResponse).isNotNull
        assertThat(trnResponse.data).hasSize(1)
        assertThat(trnResponse.data.iterator().next())
            .hasFieldOrPropertyWithValue(quantityProp, trnInput.quantity)
            .hasFieldOrPropertyWithValue(tradeDateProp, trnInput.tradeDate)
            .hasFieldOrPropertyWithValue(priceProp, trnInput.price)
            .hasFieldOrPropertyWithValue(quantityProp, trnInput.quantity)
            .hasFieldOrPropertyWithValue(versionProp, Trn.latestVersion)
            .hasFieldOrPropertyWithValue(tradeAmountProp, tradeAmount)
            .hasFieldOrPropertyWithValue(trnTypeProp, trnInput.trnType)
            .hasFieldOrPropertyWithValue(commentsProp, trnInput.comments)
    }

    @Test
    fun tradeAmountOverridingComputedValue() {
        val tradeAmount = BigDecimal("88.88")
        val trnInput = TrnInput(
            CallerRef(portfolioId.uppercase(Locale.getDefault()), one, one),
            asset.id,
            trnType = TrnType.BUY,
            quantity = BigDecimal.TEN,
            price = price,
            tradeAmount = tradeAmount,
        )

        val trnRequest = TrnRequest(portfolioId, arrayOf(trnInput))
        Mockito.`when`(portfolioService.find(portfolioId))
            .thenReturn(getPortfolio(portfolioId))
        Mockito.`when`(assetService.find(trnInput.assetId))
            .thenReturn(getAsset(NASDAQ, MSFT.code))
        Mockito.`when`(currencyService.getCode(USD.code))
            .thenReturn(Currency(USD.code))
        val trnResponse = trnAdapter.convert(portfolioService.find(portfolioId), trnRequest)
        assertThat(trnResponse).isNotNull
        assertThat(trnResponse.data).hasSize(1)
        assertThat(trnResponse.data.iterator().next())
            .hasFieldOrPropertyWithValue(quantityProp, trnInput.quantity)
            .hasFieldOrPropertyWithValue(tradeDateProp, trnInput.tradeDate)
            .hasFieldOrPropertyWithValue(priceProp, trnInput.price)
            .hasFieldOrPropertyWithValue(quantityProp, trnInput.quantity)
            .hasFieldOrPropertyWithValue(versionProp, Trn.latestVersion)
            .hasFieldOrPropertyWithValue(tradeAmountProp, tradeAmount)
            .hasFieldOrPropertyWithValue(trnTypeProp, trnInput.trnType)
            .hasFieldOrPropertyWithValue(commentsProp, trnInput.comments)
    }
}
