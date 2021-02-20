package com.beancounter.marketdata.providers.wtd

import com.beancounter.common.contracts.PriceRequest
import com.beancounter.common.contracts.PriceResponse
import com.beancounter.common.model.Asset
import com.beancounter.common.model.Market
import com.beancounter.common.model.MarketData
import com.beancounter.marketdata.providers.ProviderArguments
import com.beancounter.marketdata.providers.ProviderArguments.Companion.getInstance
import com.beancounter.marketdata.service.MarketDataProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import javax.annotation.PostConstruct

/**
 * Facade for WorldTradingData.
 *
 * @author mikeh
 * @since 2019-03-03
 */
@Service
class WtdService @Autowired internal constructor(
    private val wtdProxy: WtdProxy,
    private val wtdConfig: WtdConfig,
    private val wtdAdapter: WtdAdapter
) : MarketDataProvider {
    @Value("\${beancounter.market.providers.WTD.key:demo}")
    private val apiKey: String? = null

    @PostConstruct
    fun logStatus() {
        log.info(
            "BEANCOUNTER_MARKET_PROVIDERS_WTD_KEY: {}",
            if (apiKey.equals("DEMO", ignoreCase = true)) "DEMO" else "** Redacted **"
        )
    }

    override fun getMarketData(priceRequest: PriceRequest): Collection<MarketData> {
        val providerArguments = getInstance(priceRequest, wtdConfig)
        val batchedRequests: MutableMap<Int, Future<WtdResponse>> = ConcurrentHashMap()
        for (batch in providerArguments.batch.keys) {
            batchedRequests[batch] = wtdProxy.getPrices(
                providerArguments.batch[batch],
                providerArguments.getBatchConfigs()[batch]?.date,
                apiKey
            )
        }
        log.trace("Assets price processing complete.")
        return getMarketData(providerArguments, batchedRequests)
    }

    private fun getMarketData(
        providerArguments: ProviderArguments,
        requests: MutableMap<Int, Future<WtdResponse>>
    ): Collection<MarketData> {
        val results: MutableCollection<MarketData> = ArrayList()
        var empty = requests.isEmpty()
        while (!empty) {
            for (batch in requests.keys) {
                if (requests[batch]!!.isDone) {
                    val batchResult = wtdAdapter[providerArguments, batch, requests[batch]]
                    if (!batchResult.isNullOrEmpty()) {
                        results.addAll(batchResult)
                    }
                    requests.remove(batch)
                }
                empty = requests.isEmpty()
            }
            Thread.sleep(1000)
        }
        return results
    }

    override fun getId(): String {
        return ID
    }

    override fun isMarketSupported(market: Market): Boolean {
        return if (wtdConfig.markets!!.isBlank()) {
            false
        } else wtdConfig.markets!!.contains(market.code)
    }

    override fun getDate(market: Market, priceRequest: PriceRequest): LocalDate {
        return wtdConfig.getMarketDate(market, priceRequest.date)
    }

    override fun backFill(asset: Asset): PriceResponse {
        return PriceResponse()
    }

    companion object {
        const val ID = "WTD"
        private val log = LoggerFactory.getLogger(WtdService::class.java)
    }
}
