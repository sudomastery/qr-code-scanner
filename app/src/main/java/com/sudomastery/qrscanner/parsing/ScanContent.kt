package com.sudomastery.qrscanner.parsing

import android.net.Uri
import java.util.Locale

sealed class ScanContent {
    abstract val raw: String

    data class Url(override val raw: String, val cleaned: String) : ScanContent()

    data class Otp(
        override val raw: String,
        val type: String,
        val issuer: String,
        val account: String,
        val secret: String,
        val algorithm: String,
        val digits: String,
        val period: String
    ) : ScanContent()

    data class Wifi(
        override val raw: String,
        val ssid: String,
        val password: String,
        val security: String,
        val hidden: Boolean
    ) : ScanContent()

    data class Email(override val raw: String, val address: String, val subject: String, val body: String) :
        ScanContent()

    data class Phone(override val raw: String, val number: String) : ScanContent()

    data class Sms(override val raw: String, val number: String, val message: String) : ScanContent()

    data class Geo(override val raw: String, val lat: String, val lng: String) : ScanContent()

    data class Contact(override val raw: String) : ScanContent()

    data class PlainText(override val raw: String) : ScanContent()

    val typeName: String
        get() = when (this) {
            is Url -> "URL"
            is Otp -> "OTP"
            is Wifi -> "WIFI"
            is Email -> "EMAIL"
            is Phone -> "PHONE"
            is Sms -> "SMS"
            is Geo -> "GEO"
            is Contact -> "CONTACT"
            is PlainText -> "TEXT"
        }

    companion object {

        fun parse(raw: String): ScanContent {
            val trimmed = raw.trim()
            val lower = trimmed.lowercase(Locale.ROOT)
            return when {
                lower.startsWith("otpauth://") -> parseOtp(trimmed)
                lower.startsWith("otpauth-migration://") -> Otp(
                    raw = trimmed,
                    type = "migration",
                    issuer = "",
                    account = "",
                    secret = "",
                    algorithm = "",
                    digits = "",
                    period = ""
                )
                lower.startsWith("wifi:") -> parseWifi(trimmed)
                lower.startsWith("mailto:") -> parseMailto(trimmed)
                lower.startsWith("matmsg:") -> parseMatmsg(trimmed)
                lower.startsWith("tel:") -> Phone(trimmed, trimmed.substring(4))
                lower.startsWith("smsto:") || lower.startsWith("sms:") -> parseSms(trimmed)
                lower.startsWith("geo:") -> parseGeo(trimmed)
                lower.startsWith("begin:vcard") || lower.startsWith("mecard:") -> Contact(trimmed)
                lower.startsWith("http://") || lower.startsWith("https://") ->
                    Url(trimmed, UrlCleaner.clean(trimmed))
                else -> PlainText(trimmed)
            }
        }

        private fun parseOtp(raw: String): ScanContent {
            return try {
                val uri = Uri.parse(raw)
                val type = uri.host.orEmpty().lowercase(Locale.ROOT)
                val label = Uri.decode(uri.path.orEmpty().removePrefix("/"))
                val labelIssuer = label.substringBefore(':', "")
                val account = if (label.contains(':')) label.substringAfter(':').trim() else label
                val issuer = uri.getQueryParameter("issuer") ?: labelIssuer
                Otp(
                    raw = raw,
                    type = type,
                    issuer = issuer.trim(),
                    account = account,
                    secret = uri.getQueryParameter("secret").orEmpty(),
                    algorithm = uri.getQueryParameter("algorithm") ?: "SHA1",
                    digits = uri.getQueryParameter("digits") ?: "6",
                    period = uri.getQueryParameter("period") ?: "30"
                )
            } catch (e: Exception) {
                PlainText(raw)
            }
        }

        // WIFI:S:MyNetwork;T:WPA;P:secret;H:true;;
        private fun parseWifi(raw: String): ScanContent {
            val fields = mutableMapOf<String, String>()
            var i = 5 // skip "WIFI:"
            while (i < raw.length) {
                val sep = raw.indexOf(':', i)
                if (sep < 0) break
                val key = raw.substring(i, sep).uppercase(Locale.ROOT)
                val value = StringBuilder()
                var j = sep + 1
                while (j < raw.length) {
                    val c = raw[j]
                    if (c == '\\' && j + 1 < raw.length) {
                        value.append(raw[j + 1])
                        j += 2
                    } else if (c == ';') {
                        break
                    } else {
                        value.append(c)
                        j += 1
                    }
                }
                fields[key] = value.toString()
                i = j + 1
            }
            return Wifi(
                raw = raw,
                ssid = fields["S"].orEmpty(),
                password = fields["P"].orEmpty(),
                security = fields["T"].orEmpty().ifEmpty { "None" },
                hidden = fields["H"].equals("true", ignoreCase = true)
            )
        }

        private fun parseMailto(raw: String): ScanContent {
            val uri = Uri.parse(raw)
            val address = raw.removePrefix("mailto:").substringBefore('?')
            return Email(
                raw = raw,
                address = Uri.decode(address),
                subject = uri.getQueryParameter("subject").orEmpty(),
                body = uri.getQueryParameter("body").orEmpty()
            )
        }

        // MATMSG:TO:a@b.com;SUB:Hello;BODY:Hi;;
        private fun parseMatmsg(raw: String): ScanContent {
            fun field(name: String): String {
                val marker = "$name:"
                val start = raw.indexOf(marker, ignoreCase = true)
                if (start < 0) return ""
                val from = start + marker.length
                val end = raw.indexOf(';', from)
                return if (end < 0) raw.substring(from) else raw.substring(from, end)
            }
            return Email(raw, field("TO"), field("SUB"), field("BODY"))
        }

        private fun parseSms(raw: String): ScanContent {
            val body = raw.substringAfter(':')
            val parts = body.split(':', limit = 2)
            return Sms(raw, parts[0], parts.getOrElse(1) { "" })
        }

        private fun parseGeo(raw: String): ScanContent {
            val coords = raw.removePrefix("geo:").substringBefore('?').split(',')
            return Geo(raw, coords.getOrElse(0) { "" }, coords.getOrElse(1) { "" })
        }
    }
}
