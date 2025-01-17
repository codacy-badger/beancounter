package com.beancounter.marketdata

import com.beancounter.common.model.Asset
import com.beancounter.common.model.Currency
import com.beancounter.common.model.Market
import com.beancounter.common.model.SystemUser
import com.beancounter.common.utils.AssetUtils
import com.beancounter.common.utils.AssetUtils.Companion.getAsset

/**
 * Centralise constant values. Reduces duplicate object code quality warnings.
 */
class Constants {
    companion object {
        val USD = Currency("USD")
        val NASDAQ = Market("NASDAQ")
        val CASH = Market("CASH", type = "Internal")
        val NYSE = Market("NYSE")
        val ASX = Market("ASX")
        val MOCK = Market("MOCK")

        val AAPL = getAsset(NASDAQ, "AAPL")
        val MSFT = getAsset(NASDAQ, "MSFT")

        val AMP = getAsset(ASX, "AMP")
        val SGD = Currency("SGD")
        val AUD = Currency("AUD")
        val GBP = Currency(code = "GBP", symbol = "￡")
        val EUR = Currency("EUR", symbol = "€")
        val NZD = Currency("NZD")

        val NZX = Market("NZX", NZD)
        val systemUser = SystemUser("user", "user@testing.com")

        val msftInput = AssetUtils.getAssetInput(NASDAQ.code, MSFT.code)
        val aaplInput = AssetUtils.getAssetInput(NASDAQ.code, AAPL.code)
        val nzdCashBalance = Asset(
            id = "${NZD.code} BALANCE",
            code = "${NZD.code} BALANCE",
            name = "${NZD.code} Balance",
            priceSymbol = NZD.code,
            market = Market("CASH", NZD),
            category = "CASH"
        )
        val usdCashBalance = Asset(
            id = "${USD.code} BALANCE",
            code = "${USD.code} BALANCE",
            name = "${NZD.code} Balance",
            priceSymbol = USD.code,
            market = Market("CASH", USD),
            category = "CASH"
        )
    }
}
