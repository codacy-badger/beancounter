package com.beancounter.common.model

import com.beancounter.common.input.AssetInput
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import javax.persistence.*

/**
 * A representation of an instrument traded on a market.
 *
 * @author mikeh
 * @since 2019-01-27
 */
@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["code", "marketCode"])])
data class Asset constructor(var code: String) {
    init {
        code = code.toUpperCase()
    }

    @Id
    var id: String? = null

    @JsonInclude(JsonInclude.Include.NON_NULL)
    var name: String? = null

    @JsonInclude(JsonInclude.Include.NON_NULL)
    var category = "Equity"

    // Market is managed externally as static data; marketCode alone is persisted.
    @Transient
    lateinit var market: Market

    // Caller doesn't see marketCode
    @JsonIgnore
    var marketCode: String? = null
    var priceSymbol: String? = null

    constructor(id: String?, code: String, name: String?, category: String?,
                market: Market, marketCode: String?, priceSymbol: String?) : this(code) {
        this.id = id
        this.name = name
        this.category = category ?: "Equity"
        this.market = market
        this.marketCode = marketCode
        this.priceSymbol = priceSymbol
    }

    constructor(input: AssetInput, market: Market)
            : this(null, input.code, input.name, null, market, market.code, input.code)

    constructor(code: String, market: Market) : this(code) {
        this.market = market
        this.marketCode = market.code
    }

    // Is this asset stored locally?
    @get:JsonIgnore
    @get:Transient
    val isKnown: Boolean
        get() = id != null && !code.equals(id, ignoreCase = true)

    override fun toString(): String {
        return "Asset(code=" + code + ", name=" + name + ", market=" + market + ")"
    }

}