package com.beancounter.marketdata.trn

import com.beancounter.client.ingest.AssetIngestService
import com.beancounter.client.ingest.RowAdapter
import com.beancounter.common.exception.BusinessException
import com.beancounter.common.input.TrnInput
import com.beancounter.common.input.TrustedTrnImportRequest
import com.beancounter.common.model.TrnType
import com.beancounter.common.utils.DateUtils
import com.beancounter.common.utils.MathUtils
import com.beancounter.marketdata.trn.TrnIoDefinition.Columns
import com.beancounter.marketdata.trn.TrnIoDefinition.Companion.colDef
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Converts BC compatible delimited data to the domain model
 */
@Service
class BcRowAdapter(
    val assetIngestService: AssetIngestService,
    val cashServices: CashServices,
    val dateUtils: DateUtils = DateUtils(),
) : RowAdapter {
    override fun transform(trustedTrnImportRequest: TrustedTrnImportRequest): TrnInput {
        val marketCode = trustedTrnImportRequest.row[colDef()[Columns.Market]!!].trim()
        val assetCode = trustedTrnImportRequest.row[colDef()[Columns.Code]!!].trim()
        val asset = assetIngestService.resolveAsset(
            marketCode,
            assetCode = assetCode,
            name = trustedTrnImportRequest.row[colDef()[Columns.Name]!!].trim()
        )
        val cashAssetId = trustedTrnImportRequest.row[colDef()[Columns.CashAccount]!!].trim()
        val cashCurrency = trustedTrnImportRequest.row[colDef()[Columns.CashCurrency]!!].trim()
        val trnType = TrnType.valueOf(trustedTrnImportRequest.row[colDef()[Columns.Type]!!].trim())
        val quantity =
            MathUtils.nullSafe(MathUtils.parse(trustedTrnImportRequest.row[colDef()[Columns.Quantity]!!]))
        val price =
            MathUtils.nullSafe(MathUtils.parse(trustedTrnImportRequest.row[colDef()[Columns.Price]!!]))
        val fees = MathUtils.nullSafe(MathUtils.parse(trustedTrnImportRequest.row[colDef()[Columns.Fees]!!]))
        val tradeBaseRate = MathUtils.parse(trustedTrnImportRequest.row[colDef()[Columns.BaseRate]!!])
        val tradeAmount =
            MathUtils.nullSafe(MathUtils.parse(trustedTrnImportRequest.row[colDef()[Columns.TradeAmount]!!]))
        val tradeDate = dateUtils.getOrThrow(trustedTrnImportRequest.row[colDef()[Columns.Date]!!])
        val cashAmount = MathUtils.parse(trustedTrnImportRequest.row[colDef()[Columns.CashAmount]!!])

        if (tradeDate.isAfter(LocalDate.now())) {
            throw BusinessException("Cannot accept forward dated trade dates $tradeDate")
        }

        return TrnInput(
            trustedTrnImportRequest.callerRef,
            assetId = asset.id,
            trnType = trnType,
            quantity = quantity,
            tradeCurrency = trustedTrnImportRequest.row[colDef()[Columns.TradeCurrency]!!].trim(),
            tradeBaseRate = tradeBaseRate,
            tradeDate = tradeDate,
            tradeAmount = tradeAmount,
            cashAmount = cashAmount,
            cashCurrency = cashCurrency,
            cashAssetId = cashServices.getCashAsset(trnType, cashAssetId, cashCurrency)?.id,
            fees = fees,
            price = price,
            comments = trustedTrnImportRequest.row[colDef()[Columns.Comments]!!],
        )
    }
}
