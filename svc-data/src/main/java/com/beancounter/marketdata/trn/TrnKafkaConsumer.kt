package com.beancounter.marketdata.trn

import com.beancounter.common.contracts.TrnResponse
import com.beancounter.common.input.TrustedTrnEvent
import com.beancounter.common.input.TrustedTrnImportRequest
import com.beancounter.common.utils.BcJson.objectMapper
import com.fasterxml.jackson.core.JsonProcessingException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.io.IOException

@Service
@ConditionalOnProperty(value = ["kafka.enabled"], matchIfMissing = true)
class TrnKafkaConsumer() {

    private lateinit var trnImport: TrnImport

    @Autowired
    fun setTrnImportService(trnImport: TrnImport) {
        this.trnImport = trnImport
    }

    @KafkaListener(topics = ["#{@trnCsvTopic}"], errorHandler = "bcErrorHandler")
    @Throws(IOException::class)
    fun fromCsvImport(payload: String?): TrnResponse {
        return trnImport.fromCsvImport(objectMapper.readValue(payload, TrustedTrnImportRequest::class.java))!!
    }

    @KafkaListener(topics = ["#{@trnEventTopic}"], errorHandler = "bcErrorHandler")
    @Throws(JsonProcessingException::class)
    fun fromTrnRequest(payload: String?): TrnResponse {
        return trnImport.fromTrnRequest(objectMapper.readValue(payload, TrustedTrnEvent::class.java))!!
    }

}