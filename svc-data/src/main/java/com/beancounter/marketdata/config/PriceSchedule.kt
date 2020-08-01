package com.beancounter.marketdata.config

import com.beancounter.common.utils.DateUtils
import com.beancounter.marketdata.providers.PriceRefresh
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.time.LocalDateTime

@EnableScheduling
@EnableAsync
@Configuration
class PriceSchedule(private val priceRefresh: PriceRefresh, private val dateUtils: DateUtils) {

    companion object {
        private val log = LoggerFactory.getLogger(PriceSchedule::class.java)
    }

    @Bean
    fun scheduleZone(): String {
        log.info("Scheduling enabled")
        log.info("SCHEDULE_ZONE: {}", dateUtils.defaultZone)
        return dateUtils.defaultZone
    }

    @Bean
    fun assetsSchedule (@Value("\${assets.schedule:0 */30 7-18 ? * Tue-Sat}") schedule: String): String {
        log.info("ASSETS_SCHEDULE: {}, ZONE: {}", schedule, dateUtils.defaultZone)
        return schedule
    }


    @Scheduled(cron = "#{@assetsSchedule}", zone = "#{@scheduleZone}")
    fun updatePrices() {
        log.info("Scheduled price update starting {}", LocalDateTime.now(dateUtils.getZoneId()))
        priceRefresh.updatePrices()
    }


}
