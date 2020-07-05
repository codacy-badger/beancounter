package com.beancounter.marketdata.utils

import com.beancounter.common.utils.BcJson.objectMapper
import com.beancounter.marketdata.assets.figi.FigiResponse
import com.beancounter.marketdata.assets.figi.FigiSearch
import com.fasterxml.jackson.core.type.TypeReference
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType
import java.io.File
import java.util.*

object FigiMockUtils {
    @JvmStatic
    private val figiApi = WireMockRule(WireMockConfiguration.options().port(6666))

    @JvmStatic
    fun getFigiApi(): WireMockRule {
        if (!figiApi.isRunning) {
            this.figiApi.start()
            val prefix = "/contracts/figi"

            mock(ClassPathResource(prefix + "/common-stock-response.json").getFile(),
                    "US",
                    "MSFT",
                    "Common Stock")

            mock(ClassPathResource(prefix + "/adr-response.json").getFile(),
                    "US",
                    "BAIDU",
                    "Depositary Receipt")

            mock(ClassPathResource(prefix + "/reit-response.json").getFile(),
                    "US",
                    "OHI",
                    "REIT")

            mock(ClassPathResource(prefix + "/mf-response.json").getFile(),
                    "US",
                    "XLF",
                    "REIT")

            mock(ClassPathResource(prefix + "/brkb-response.json").getFile(),
                    "US",
                    "BRK/B",
                    "Common Stock")

            figiApi.stubFor(any(anyUrl())
                    .atPriority(10)
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody("[{\"error\": \"No identifier found.\"\n"
                                    + "    }\n"
                                    + "]")))

        }
        return this.figiApi
    }

    @Throws(Exception::class)
    @JvmStatic
    private fun mock(jsonFile: File,
             market: String,
             code: String,
             securityType: String) {

        val search = FigiSearch(code, market, securityType, true)
        val searchCollection: MutableCollection<FigiSearch> = ArrayList()
        searchCollection.add(search)
        val response: Collection<FigiResponse> = objectMapper.readValue<Collection<FigiResponse>>(
                jsonFile, object : TypeReference<Collection<FigiResponse>>() {})
        getFigiApi().stubFor(
                post(urlEqualTo("/v2/mapping"))
                        .withRequestBody(
                                equalToJson(objectMapper.writeValueAsString(searchCollection)))
                        .withHeader("X-OPENFIGI-APIKEY", matching("demoxx"))
                        .willReturn(
                                aResponse()
                                        .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(objectMapper.writeValueAsString(response))
                                        .withStatus(200)))
    }
}