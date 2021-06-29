package com.beancounter.marketdata.assets.figi

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Bloomberg OpenFigi configuration properties
 */
@Configuration
@Import(FigiProxy::class)
class FigiConfig {
    @Value("\${beancounter.market.providers.figi.key:demo}")
    var apiKey: String? = null

    @Value("\${beancounter.market.providers.figi.enabled:true}")
    var enabled: Boolean? = null
}
