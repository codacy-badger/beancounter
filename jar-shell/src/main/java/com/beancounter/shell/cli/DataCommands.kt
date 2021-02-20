package com.beancounter.shell.cli

import com.beancounter.client.services.StaticService
import com.beancounter.common.contracts.MarketResponse
import com.beancounter.common.model.Market
import com.beancounter.common.utils.BcJson
import lombok.extern.slf4j.Slf4j
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

@ShellComponent
@Slf4j
class DataCommands internal constructor(private val staticService: StaticService) {
    private val writer = BcJson().objectMapper.writerWithDefaultPrettyPrinter()

    @ShellMethod("Supported markets")
    fun markets(
        @ShellOption(help = "Optional market code", defaultValue = "__NULL__") marketCode: String?
    ): String {
        return if (marketCode != null) {
            val market = staticService.getMarket(
                marketCode
            )
            val markets: MutableCollection<Market> = ArrayList()
            markets.add(market)
            return writer.writeValueAsString(MarketResponse(markets))
        } else {
            writer.writeValueAsString(staticService.getMarkets())
        }
    }
}
