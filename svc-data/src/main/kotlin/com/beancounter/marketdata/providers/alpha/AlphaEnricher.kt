package com.beancounter.marketdata.providers.alpha

import com.beancounter.common.contracts.AssetSearchResponse
import com.beancounter.common.contracts.AssetSearchResult
import com.beancounter.common.exception.SystemException
import com.beancounter.common.model.Asset
import com.beancounter.common.model.Market
import com.beancounter.marketdata.assets.AssetEnricher
import com.fasterxml.jackson.core.JsonProcessingException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Locale

/**
 * Backfills missing asset data from a 3rd party. Basically adds asset.name for a supplied asset.code.
 */
@Service
class AlphaEnricher(private val alphaConfig: AlphaConfig) : AssetEnricher {
    private val objectMapper = AlphaPriceAdapter().alphaMapper
    private var alphaProxyCache: AlphaProxyCache? = null

    @Value("\${beancounter.market.providers.ALPHA.key:demo}")
    private val apiKey: String? = null

    @Autowired(required = false)
    fun setAlphaProxyCache(alphaProxyCache: AlphaProxyCache?) {
        this.alphaProxyCache = alphaProxyCache
    }

    override fun enrich(id: String, market: Market, code: String, defaultName: String?): Asset? {
        val marketCode = alphaConfig.translateMarketCode(market)
        var symbol = alphaConfig.translateSymbol(code)
        if (marketCode != null) {
            symbol = "$symbol.$marketCode"
        }
        val result = alphaProxyCache!!.search(symbol, apiKey)
        // var assetResult: AssetSearchResult? = null
        val assetResult = try {
            getAssetSearchResult(market, result)
        } catch (e: JsonProcessingException) {
            throw SystemException("This shouldn't have happened")
        }
        return if (assetResult == null) {
            null
        } else Asset(
            id,
            code.uppercase(Locale.getDefault()),
            name = assetResult.name,
            category = assetResult.type,
            market = market,
            marketCode = market.code,
            priceSymbol = assetResult.symbol!!,
        )
    }

    @Throws(JsonProcessingException::class)
    private fun getAssetSearchResult(market: Market, result: String?): AssetSearchResult? {
        val (data) = objectMapper.readValue(result, AssetSearchResponse::class.java)
        if (data.isEmpty()) {
            return null
        }
        val assetResult = data.iterator().next()
        return if (currencyMatch(assetResult.currency, market.currencyId)) {
            assetResult
        } else
        // Fuzzy search result returned and asset from a different exchange
            null
    }

    fun currencyMatch(currency: String?, currencyId: String): Boolean {
        var match = currency
        if (currency == "GBX") {
            match = "GBP"
        }
        return match == currencyId
    }

    override fun canEnrich(asset: Asset): Boolean {
        return asset.name == null
    }

    override fun id(): String {
        return "ALPHA"
    }
}
