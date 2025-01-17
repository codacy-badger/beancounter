package com.beancounter.marketdata.currency

import com.beancounter.common.model.Currency
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.Locale
import javax.annotation.PostConstruct

/**
 * Verification of Market related functions.
 *
 * @author mikeh
 * @since 2019-03-19
 */
@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "beancounter.currency")
class CurrencyService(val currencyRepository: CurrencyRepository) {
    var base: String = "USD"
    var values: Collection<Currency> = ArrayList()
    var baseCurrency: Currency? = null

    @PostConstruct
    private fun persist() {
        log.info("Persisting {} default currencies", values.size)
        if (!values.isEmpty()) {
            val result = currencyRepository.saveAll(this.values)
            for (currency in result) {
                log.debug("Persisted {}", currency)
            }
            baseCurrency = getCode(base) // Default base currency
        }
    }

    /**
     * Resolves a currency via its ISO code (AK).
     *
     * @param code non-null code
     * @return resolved currency
     */
    @Cacheable("currency.code")
    fun getCode(code: String): Currency? {
        val result = currencyRepository.findById(code.uppercase(Locale.getDefault()))
        if (result.isPresent) {
            return result.get()
        }
        return null
    }

    @get:Cacheable("currency.all")
    val currencies: Iterable<Currency>
        get() = currencyRepository.findAllByOrderByCodeAsc()

    companion object {
        private val log = LoggerFactory.getLogger(CurrencyService::class.java)
    }
}
