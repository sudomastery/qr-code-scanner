package com.sudomastery.qrscanner.parsing

import android.net.Uri
import java.util.Locale

/**
 * Strips known tracking parameters from URLs so links open clean.
 */
object UrlCleaner {

    private val trackerPrefixes = listOf(
        "utm_",       // Google Analytics campaign tags
        "pk_",        // Piwik / Matomo
        "mtm_",       // Matomo
        "hsa_",       // HubSpot ads
        "vero_",      // Vero
        "oly_"        // Omeda
    )

    private val trackerParams = setOf(
        "fbclid", "gclid", "gclsrc", "dclid", "gbraid", "wbraid",
        "msclkid", "yclid", "twclid", "ttclid", "li_fat_id",
        "mc_cid", "mc_eid", "mkt_tok", "igshid", "igsh",
        "_hsenc", "_hsmi", "hsctatracking",
        "wickedid", "irclickid", "s_cid", "scid",
        "srsltid", "spm", "scm", "share_id", "xtor",
        "ref_src", "ref_url", "cmpid", "ncid", "sr_share",
        "fb_action_ids", "fb_action_types", "fb_ref", "fb_source",
        "si"          // YouTube / Spotify share tracking
    )

    fun clean(url: String): String {
        return try {
            val uri = Uri.parse(url)
            if (uri.isOpaque || uri.query.isNullOrEmpty()) return url
            val keptParams = uri.queryParameterNames.filterNot { isTracker(it) }
            val builder = uri.buildUpon().clearQuery()
            for (name in keptParams) {
                for (value in uri.getQueryParameters(name)) {
                    builder.appendQueryParameter(name, value)
                }
            }
            builder.build().toString()
        } catch (e: Exception) {
            url
        }
    }

    fun hasTrackers(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            !uri.isOpaque && uri.queryParameterNames.any { isTracker(it) }
        } catch (e: Exception) {
            false
        }
    }

    private fun isTracker(name: String): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        return lower in trackerParams || trackerPrefixes.any { lower.startsWith(it) }
    }
}
