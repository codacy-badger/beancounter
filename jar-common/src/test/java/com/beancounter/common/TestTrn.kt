package com.beancounter.common

import com.beancounter.common.contracts.TrnRequest
import com.beancounter.common.contracts.TrnResponse
import com.beancounter.common.input.TrnInput
import com.beancounter.common.input.TrustedTrnImportRequest
import com.beancounter.common.model.Asset
import com.beancounter.common.model.CallerRef
import com.beancounter.common.model.Currency
import com.beancounter.common.model.Market
import com.beancounter.common.model.SystemUser
import com.beancounter.common.model.Trn
import com.beancounter.common.model.TrnType
import com.beancounter.common.utils.AssetUtils.Companion.getAsset
import com.beancounter.common.utils.AssetUtils.Companion.toKey
import com.beancounter.common.utils.BcJson
import com.beancounter.common.utils.DateUtils
import com.beancounter.common.utils.PortfolioUtils.Companion.getPortfolio
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

internal class TestTrn {
    private val mapper = BcJson().objectMapper

    @Test
    @Throws(Exception::class)
    fun is_TransactionRequestSerializing() {
        val trnInput = TrnInput(
            callerRef = CallerRef("1", "1", "ABC"),
            tradeDate = DateUtils().getDate("2019-10-10"),
            assetId = getAsset("NASDAQ", "MSFT").id,
            fees = BigDecimal.ONE,
            price = BigDecimal("10.99"),
            tradeAmount = BigDecimal("100.99"),
            comments = "Comment"
        )

        trnInput.cashCurrency = "USD"
        trnInput.cashAsset = toKey("USD-X", "USER")
        trnInput.settleDate = DateUtils().getDate("2019-10-10")
        trnInput.cashAmount = BigDecimal("100.99")
        trnInput.tradeBaseRate = BigDecimal("1.99")
        trnInput.tradePortfolioRate = BigDecimal("10.99")
        trnInput.tradeBaseRate = BigDecimal.ONE
        val trnRequest = TrnRequest("abc", arrayOf(trnInput))
        val json = mapper.writeValueAsString(trnRequest)
        val fromJson = mapper.readValue(json, TrnRequest::class.java)
        assertThat(fromJson.data).hasSize(1)
        val fromTrn: TrnInput = fromJson.data.iterator().next()
        assertThat(fromTrn.callerRef).usingRecursiveComparison().isEqualTo(trnInput.callerRef)
        assertThat(fromTrn)
            .usingRecursiveComparison().isEqualTo(trnInput)
    }

    @Test
    @Throws(Exception::class)
    fun is_TransactionResponseSerializing() {
        val trnType = TrnType.BUY
        val nyse = Market("NYSE", USD)
        val asset = getAsset(nyse, "TEST")
        val portfolio = getPortfolio("TWEE")
        portfolio.owner = SystemUser("123", "whee", true)
        val trn = Trn(trnType, asset, BigDecimal("100.01"))
        trn.id = "PK"
        trn.callerRef = CallerRef("10", "10", "TEST")
        trn.portfolio = portfolio
        trn.tradeDate = LocalDate.now()
        trn.settleDate = LocalDate.now()
        trn.price = BigDecimal("22.11")
        trn.fees = BigDecimal("10")
        trn.tradeAmount = BigDecimal("999.99")
        val trns: MutableCollection<Trn> = ArrayList()
        trns.add(trn)
        val trnResponse = TrnResponse(trns)

        val fromJson = mapper.readValue(mapper.writeValueAsString(trnResponse), TrnResponse::class.java)
        val fromAsset: Asset = fromJson.data.iterator().next().asset
        // Market.aliases are not serialized
        assertThat(fromAsset.market)
            .usingRecursiveComparison().ignoringFields("market", "aliases")
        assertThat(fromAsset)
            .usingRecursiveComparison()
        assertThat(fromJson.data).hasSize(1)
        assertThat(fromJson.data.iterator().next())
            .usingRecursiveComparison().ignoringFields("asset")
    }

    @Test
    fun is_TrnIdDefaulting() {
        val fromNull: CallerRef = CallerRef.from(null, null)
        assertThat(fromNull).hasNoNullFieldsOrProperties()
        val id = CallerRef("provider", "batch", "456")
        assertThat(CallerRef.from(id, null)).usingRecursiveComparison().isEqualTo(id)
    }

    @Test
    @Throws(Exception::class)
    fun is_TrustedTrnRequestValid() {
        val row: MutableList<String> = ArrayList()
        row.add("ABC")
        row.add("ABC")
        val ttr = TrustedTrnImportRequest(getPortfolio("TWEE"), row)
        val json = mapper.writeValueAsString(ttr)
        val fromJson = mapper.readValue(json, TrustedTrnImportRequest::class.java)
        assertThat(fromJson).usingRecursiveComparison().ignoringFields("portfolio")
    }

    @Test
    fun is_TrnIdDefaults() {
        var callerRef = CallerRef()
        assertThat(callerRef).hasAllNullFieldsOrProperties()
        // No values, so defaults should be created
        assertThat(CallerRef.from(callerRef, getPortfolio("BLAH")))
            .hasNoNullFieldsOrProperties()
            .hasFieldOrPropertyWithValue("batch", "BLAH")
        callerRef = CallerRef("ABC", "ABC", "ABC")
        assertThat(CallerRef.from(callerRef, getPortfolio("BLAH")))
            .hasFieldOrPropertyWithValue("batch", "ABC")
            .hasFieldOrPropertyWithValue("provider", "ABC")
            .hasFieldOrPropertyWithValue("callerId", "ABC")

        // Called ID not specified
        callerRef = CallerRef("ABC", "ABC")
        assertThat(CallerRef.from(callerRef, getPortfolio("BLAH")))
            .hasFieldOrPropertyWithValue("batch", "ABC")
            .hasFieldOrPropertyWithValue("provider", "ABC")
            .hasFieldOrProperty("callerId")
    }

    companion object {
        private val USD = Currency("USD")
    }

    @Test
    fun is_TradeCurrencySetFromAsset() {
        val trn = Trn(TrnType.BUY, getAsset("NYSE", "ABC"))
        assertThat(trn.asset.market.currency).isNotNull
        assertThat(trn.tradeCurrency.code).isEqualTo(trn.asset.market.currency.code)
    }
}
