package com.beancounter.client.sharesight

import com.beancounter.client.ingest.AssetIngestService
import com.beancounter.client.ingest.Filter
import com.beancounter.client.ingest.TrnAdapter
import com.beancounter.common.exception.BusinessException
import com.beancounter.common.input.TrnInput
import com.beancounter.common.input.TrustedTrnImportRequest
import com.beancounter.common.model.Asset
import com.beancounter.common.model.CallerRef
import com.beancounter.common.model.TrnType
import com.beancounter.common.utils.DateUtils
import com.beancounter.common.utils.MathUtils.Companion.multiply
import com.beancounter.common.utils.MathUtils.Companion.parse
import com.beancounter.common.utils.NumberUtils
import com.google.common.base.CharMatcher
import com.google.common.base.Splitter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.lang.NonNull
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.text.ParseException

/**
 * Converts from the ShareSight dividend format.
 *
 *
 * ShareSight amounts are in Portfolio currency; BC expects values in trade currency.
 *
 * @author mikeh
 * @since 2019-02-08
 */
@Service
class ShareSightDividendAdapter(
    private val shareSightConfig: ShareSightConfig,
    private val assetIngestService: AssetIngestService
) : TrnAdapter {
    private val dateUtils = DateUtils()
    private var filter = Filter(null)
    private val numberUtils = NumberUtils()
    @Autowired(required = false)
    fun setFilter(filter: Filter) {
        this.filter = filter
    }

    @NonNull
    override fun from(trustedTrnImportRequest: TrustedTrnImportRequest?): TrnInput {
        assert(trustedTrnImportRequest != null)
        val row = trustedTrnImportRequest!!.row
        return try {
            val asset = resolveAsset(row)
            if (asset == null) {
                log.error("Unable to resolve asset [{}]", row)
                throw BusinessException(String.format("Unable to resolve asset [%s]", row))
            }
            val tradeRate = parse(row[fxRate], shareSightConfig.numberFormat)
            val tradeAmount = multiply(
                parse(
                    row[net],
                    shareSightConfig.numberFormat
                ),
                tradeRate
            )!!
            val trnInput = TrnInput(
                CallerRef(trustedTrnImportRequest.portfolio.id, callerId = row[id]),
                asset.id,
                TrnType.DIVI,
                BigDecimal.ZERO,
                row[currency],
                dateUtils.getDate(
                    row[date],
                    shareSightConfig.dateFormat,
                    dateUtils.getZoneId()
                ),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                tradeAmount,
                row[comments]
            )
            trnInput.tax = multiply(BigDecimal(row[tax]), tradeRate)
            trnInput.cashAmount = multiply(
                parse(row[net], shareSightConfig.numberFormat),
                tradeRate
            )
            trnInput.tradeCashRate = if (shareSightConfig.isCalculateRates ||
                numberUtils.isUnset(tradeRate)
            ) null else tradeRate
            trnInput
        } catch (e: NumberFormatException) {
            val message = e.message
            log.error(
                "{} - {} Parsing row {}",
                message,
                "DIVI",
                row
            )
            throw BusinessException(message)
        } catch (e: ParseException) {
            val message = e.message
            log.error(
                "{} - {} Parsing row {}",
                message,
                "DIVI",
                row
            )
            throw BusinessException(message)
        }
    }

    override fun isValid(row: List<String>): Boolean {
        val rate = row[fxRate].toUpperCase()
        return rate.contains(".") // divis have an fx rate in this column
    }

    override fun resolveAsset(row: List<String>): Asset? {
        val values = parseAsset(row[code])
        val asset = assetIngestService.resolveAsset(
            values[1].toUpperCase(), values[0]
        )
        return if (!filter.inFilter(asset)) {
            null
        } else asset
    }

    private fun parseAsset(input: String?): List<String> {
        if (input == null || input.isEmpty()) {
            throw BusinessException("Unable to resolve Asset code")
        }
        val values = Splitter
            .on(CharMatcher.anyOf(".:-"))
            .trimResults()
            .splitToList(input)
        if (values.isEmpty() || values[0] == input) {
            throw BusinessException(String.format("Unable to parse %s", input))
        }
        return values
    }

    companion object {
        const val id = 0
        const val code = 1
        const val name = 2
        const val date = 3
        const val fxRate = 4
        const val currency = 5
        const val net = 6
        const val tax = 7
        const val gross = 8
        const val comments = 9
        private val log = LoggerFactory.getLogger(ShareSightDividendAdapter::class.java)
    }
}
