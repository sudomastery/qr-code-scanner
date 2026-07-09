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

        // Parses KEY:value;KEY:value;; fields where '\' escapes the next
        // character, as used by both the WIFI: and MATMSG: formats.
        private fun parseDelimitedFields(raw: String): Map<String, String> {
            val fields = mutableMapOf<String, String>()
            var i = raw.indexOf(':') + 1 // skip the scheme prefix
            while (i in 1 until raw.length) {
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
            return fields
        }

        // Extracts ?key=value&key=value params without Uri.getQueryParameter,
        // which throws on opaque URIs like mailto: and sms:.
        private fun queryParams(raw: String): Map<String, String> {
            val query = raw.substringAfter('?', "")
            if (query.isEmpty()) return emptyMap()
            return query.split('&').filter { it.isNotEmpty() }.associate { param ->
                Uri.decode(param.substringBefore('=')) to
                    Uri.decode(param.substringAfter('=', ""))
            }
        }

        // WIFI:S:MyNetwork;T:WPA;P:secret;H:true;;
        private fun parseWifi(raw: String): ScanContent {
            val fields = parseDelimitedFields(raw)
            return Wifi(
                raw = raw,
                ssid = fields["S"].orEmpty(),
                password = fields["P"].orEmpty(),
                security = fields["T"].orEmpty().ifEmpty { "None" },
                hidden = fields["H"].equals("true", ignoreCase = true)
            )
        }

        private fun parseMailto(raw: String): ScanContent {
            val address = raw.substringAfter(':').substringBefore('?')
            val params = queryParams(raw)
            return Email(
                raw = raw,
                address = Uri.decode(address),
                subject = params["subject"].orEmpty(),
                body = params["body"].orEmpty()
            )
        }

        // MATMSG:TO:a@b.com;SUB:Hello;BODY:Hi;;
        private fun parseMatmsg(raw: String): ScanContent {
            val fields = parseDelimitedFields(raw)
            return Email(
                raw,
                fields["TO"].orEmpty(),
                fields["SUB"].orEmpty(),
                fields["BODY"].orEmpty()
            )
        }

        // SMSTO:number:message or sms:number?body=message
        private fun parseSms(raw: String): ScanContent {
            val rest = raw.substringAfter(':')
            return if (raw.startsWith("smsto:", ignoreCase = true)) {
                val parts = rest.split(':', limit = 2)
                Sms(raw, parts[0], parts.getOrElse(1) { "" })
            } else {
                Sms(raw, rest.substringBefore('?'), queryParams(raw)["body"].orEmpty())
            }
        }

        private fun parseGeo(raw: String): ScanContent {
            val coords = raw.removePrefix("geo:").substringBefore('?').split(',')
            return Geo(raw, coords.getOrElse(0) { "" }, coords.getOrElse(1) { "" })
        }
    }
}
