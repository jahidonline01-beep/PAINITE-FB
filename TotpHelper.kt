package com.painitefb.app

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * Standard RFC 6238 TOTP (same as Google Authenticator / Facebook 2FA).
 * Secret must be Base32 (e.g. from Facebook Security → Authentication app).
 */
object TotpHelper {

    private const val STEP_SECONDS = 30L
    private const val DIGITS = 6

    fun generate(secret: String): Pair<String, Int> {
        val clean = normalizeSecret(secret)
        if (clean.isEmpty()) return "000000" to STEP_SECONDS.toInt()

        val key = try {
            decodeBase32(clean)
        } catch (_: Exception) {
            return "000000" to STEP_SECONDS.toInt()
        }
        if (key.isEmpty()) return "000000" to STEP_SECONDS.toInt()

        val now = System.currentTimeMillis() / 1000L
        val remaining = (STEP_SECONDS - (now % STEP_SECONDS)).toInt()
        val counter = now / STEP_SECONDS
        val code = hotp(key, counter)
        return code to remaining
    }

    /** Strip spaces, dashes; uppercase — Facebook keys often have spaces. */
    fun normalizeSecret(secret: String): String {
        return secret
            .replace(" ", "")
            .replace("-", "")
            .replace("\n", "")
            .replace("\t", "")
            .uppercase()
            .filter { it in 'A'..'Z' || it in '2'..'7' }
    }

    private fun hotp(key: ByteArray, counter: Long): String {
        val data = ByteArray(8)
        var c = counter
        for (i in 7 downTo 0) {
            data[i] = (c and 0xFF).toByte()
            c = c ushr 8
        }
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(data)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary =
            ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)
        val otp = binary % 10.0.pow(DIGITS.toDouble()).toInt()
        return otp.toString().padStart(DIGITS, '0')
    }

    /** RFC 4648 Base32 decode */
    private fun decodeBase32(encoded: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val cleaned = encoded.uppercase().filter { it in alphabet }
        if (cleaned.isEmpty()) return ByteArray(0)

        var buffer = 0
        var bitsLeft = 0
        val out = ArrayList<Byte>()
        for (ch in cleaned) {
            val value = alphabet.indexOf(ch)
            if (value < 0) continue
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                out.add((buffer shr (bitsLeft - 8) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return out.toByteArray()
    }
}
