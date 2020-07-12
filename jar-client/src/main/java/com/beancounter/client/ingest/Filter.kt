package com.beancounter.client.ingest

import com.beancounter.common.model.Asset
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
class Filter(@Value("\${filter:#{null}}") filter: String?) {
    private val filteredAssets: MutableCollection<String> = ArrayList()
    private fun init(filter: String?) {
        if (filter != null) {
            val values = filter.split(",").toTypedArray()
            for (value in values) {
                filteredAssets.add(value.toUpperCase())
            }
        }
    }

    fun inFilter(asset: Asset): Boolean {
        return if (!filteredAssets.isEmpty()) {
            filteredAssets.contains(asset.code.toUpperCase())
        } else true
    }

    fun hasFilter(): Boolean {
        return !filteredAssets.isEmpty()
    }

    init {
        init(filter)
    }
}