package com.beancounter.marketdata.assets.figi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.boot.context.properties.ConstructorBinding

@JsonIgnoreProperties(ignoreUnknown = true)
data class FigiAsset @ConstructorBinding constructor(
        val name: String,
        val ticker: String,
        val securityType2: String
)