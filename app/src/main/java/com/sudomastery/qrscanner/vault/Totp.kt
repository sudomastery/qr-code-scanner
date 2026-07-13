package com.sudomastery.qrscanner.vault

import java.nio.ByteBuffer
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** RFC 6238 TOTP code generation for the vault's live code display. */
object Totp {

    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    /** Decodes an RFC 4648 base32 secret; returns null on invalid input. */
    fun decodeBase32(input: String): ByteArray? {
        val cleaned = input.trim()
            .replace(" ", "")
            .replace("-", "")
            .trimEnd('=')
            .uppercase(Locale.ROOT)
        if (cleaned.isEmpty()) return null
        var buffer = 0L
        var bits = 0
        val out = ArrayList<Byte>(cleaned.length * 5 / 8)
        for (c in cleaned) {
            val value = BASE32_ALPHABET.indexOf(c)
            if (value < 0) return null
            buffer = (buffer shl 5) or value.toLong()
            bits += 5
            if (bits >= 8) {
                bits -= 8
                out.add(((buffer shr bits) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }

    /**
     * Current TOTP code for the given key, or null if the algorithm is
     * unavailable or the key is empty.
     */
    fun code(
        key: ByteArray,
        timeMillis: Long,
        periodSeconds: Int,
        digits: Int,
        algorithm: String
    ): String? {
        if (key.isEmpty() || periodSeconds <= 0 || digits !in 1..9) return null
        val macAlgorithm = when (algorithm.uppercase(Locale.ROOT)) {
            "SHA256" -> "HmacSHA256"
            "SHA512" -> "HmacSHA512"
            else -> "HmacSHA1"
        }
        val counter = (timeMillis / 1000L) / periodSeconds
        return runCatching {
            val mac = Mac.getInstance(macAlgorithm)
            mac.init(SecretKeySpec(key, macAlgorithm))
            val hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array())
            val offset = hash.last().toInt() and 0x0F
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)
            var modulus = 1
            repeat(digits) { modulus *= 10 }
            (binary % modulus).toString().padStart(digits, '0')
        }.getOrNull()
    }

    fun secondsRemaining(timeMillis: Long, periodSeconds: Int): Int {
        if (periodSeconds <= 0) return 0
        return (periodSeconds - ((timeMillis / 1000L) % periodSeconds)).toInt()
    }
}
