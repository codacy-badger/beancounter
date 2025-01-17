package com.beancounter.marketdata.contracts

import com.beancounter.common.contracts.AssetRequest
import com.beancounter.common.contracts.AssetResponse
import com.beancounter.common.contracts.AssetUpdateResponse
import com.beancounter.common.model.SystemUser
import com.beancounter.common.utils.BcJson
import com.beancounter.marketdata.assets.AssetService
import contracts.ContractHelper
import io.restassured.module.mockmvc.RestAssuredMockMvc
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.ClassPathResource
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.io.File
import java.util.Locale

/**
 * Asset Contract Tests. Called by Spring Cloud Contract Verifier
 */
class AssetsBase : ContractVerifierBase() {
    @MockBean
    private lateinit var assetService: AssetService
    private lateinit var systemUser: SystemUser

    @BeforeEach
    fun mock() {
        val mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .build()
        systemUser = ContractHelper.defaultUser(
            jwtDecoder = jwtDecoder,
            tokenService = tokenService,
            systemUserRepository = systemUserRepository
        )
        RestAssuredMockMvc.mockMvc(mockMvc)
        mockAssets(assetService)
    }

    fun mockAssets(assetService: AssetService) {
        Mockito.`when`(assetService.find("KMI"))
            .thenReturn(
                BcJson().objectMapper.readValue(
                    ClassPathResource("contracts/assets/kmi-asset-by-id.json").file,
                    AssetResponse::class.java
                ).data
            )
        mockAssetCreateResponses(
            ClassPathResource("contracts/assets/create-request.json").file,
            ClassPathResource("contracts/assets/create-response.json").file,
            assetService
        )
        mockAssetCreateResponses(
            ClassPathResource("contracts/assets/ebay-request.json").file,
            ClassPathResource("contracts/assets/ebay-response.json").file,
            assetService
        )
        mockAssetCreateResponses(
            ClassPathResource("contracts/assets/msft-request.json").file,
            ClassPathResource("contracts/assets/msft-response.json").file,
            assetService
        )
        mockAssetCreateResponses(
            ClassPathResource("contracts/assets/bhp-asx-request.json").file,
            ClassPathResource("contracts/assets/bhp-asx-response.json").file,
            assetService
        )
        mockAssetCreateResponses(
            ClassPathResource("contracts/assets/bhp-lse-request.json").file,
            ClassPathResource("contracts/assets/bhp-lse-response.json").file,
            assetService
        )
        mockAssetCreateResponses(
            ClassPathResource("contracts/assets/abbv-request.json").file,
            ClassPathResource("contracts/assets/abbv-response.json").file,
            assetService
        )
        mockAssetCreateResponses(
            ClassPathResource("contracts/assets/amp-request.json").file,
            ClassPathResource("contracts/assets/amp-response.json").file,
            assetService
        )
    }

    @Throws(Exception::class)
    private fun mockAssetCreateResponses(jsonRequest: File, jsonResponse: File, assetService: AssetService) {
        val assetRequest = BcJson().objectMapper.readValue(jsonRequest, AssetRequest::class.java)
        val assetUpdateResponse = BcJson().objectMapper.readValue(jsonResponse, AssetUpdateResponse::class.java)
        Mockito.`when`(assetService.process(assetRequest))
            .thenReturn(assetUpdateResponse)

        val keys = assetUpdateResponse.data.keys
        for (key in keys) {
            val theAsset = assetUpdateResponse.data[key]
            theAsset!!.id
            Mockito.`when`(assetService.find(theAsset.id)).thenReturn(theAsset)
            Mockito.`when`(
                assetService.findLocally(
                    theAsset.market.code.uppercase(Locale.getDefault()),
                    theAsset.code.uppercase(Locale.getDefault())
                )
            )
                .thenReturn(theAsset)
        }
    }
}
