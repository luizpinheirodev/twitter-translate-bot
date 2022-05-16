package com.luiz.twitterbot.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.function.Consumer
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec


/**
 * Class to generate Oauth 1.0a header for Twitter
 *
 */
class TwitterOauthHeaderGenerator(private val consumerKey: String, private val consumerSecret: String, private val token: String, private val tokenSecret: String) {
    private val signatureMethod = "HMAC-SHA1"
    private val version = "1.0"

    /**
     * Generates oAuth 1.0a header which can be passed as Authorization header
     *
     * @param httpMethod
     * @param url
     * @param requestParams
     * @return
     */
    fun generateHeader(httpMethod: String, url: String, requestParams: Map<String, String>): String {
        val base = StringBuilder()
        val nonce = nonce
        val timestamp = timestamp
        val baseSignatureString = generateSignatureBaseString(httpMethod, url, requestParams, nonce, timestamp)
        val signature = encryptUsingHmacSHA1(baseSignatureString)
        base.append("OAuth ")
        append(base, oauth_consumer_key, consumerKey)
        append(base, oauth_token, token)
        append(base, oauth_signature_method, signatureMethod)
        append(base, oauth_timestamp, timestamp)
        append(base, oauth_nonce, nonce)
        append(base, oauth_version, version)
        append(base, oauth_signature, signature)
        base.deleteCharAt(base.length - 1)
        println("header : $base")
        return base.toString()
    }

    /**
     * Generate base string to generate the oauth_signature
     *
     * @param httpMethod
     * @param url
     * @param requestParams
     * @return
     */
    private fun generateSignatureBaseString(
        httpMethod: String,
        url: String,
        requestParams: Map<String, String>,
        nonce: String,
        timestamp: String
    ): String {
        val params: MutableMap<String, String> = HashMap()
        requestParams.entries.forEach(Consumer { (key, value): Map.Entry<String, String> ->
            put(
                params,
                key,
                value
            )
        })
        put(params, oauth_consumer_key, consumerKey)
        put(params, oauth_nonce, nonce)
        put(params, oauth_signature_method, signatureMethod)
        put(params, oauth_timestamp, timestamp)
        put(params, oauth_token, token)
        put(params, oauth_version, version)

        val sortedParams = params.entries
            .sortedBy { it.key }

        val base = StringBuilder()
        sortedParams.forEach(Consumer { (key, value): Map.Entry<String, String> ->
            base.append(
                key
            ).append("=").append(value).append("&")
        })
        base.deleteCharAt(base.length - 1)
        return httpMethod.uppercase(Locale.getDefault()) + "&" + encode(url) + "&" + encode(base.toString())
    }

    private fun encryptUsingHmacSHA1(input: String): String? {
        val secret = StringBuilder().append(encode(consumerSecret)).append("&").append(encode(tokenSecret)).toString()
        val keyBytes = secret.toByteArray(StandardCharsets.UTF_8)
        val key: SecretKey = SecretKeySpec(keyBytes, HMAC_SHA1)
        val mac: Mac
        try {
            mac = Mac.getInstance(HMAC_SHA1)
            mac.init(key)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            return null
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
            return null
        }
        val signatureBytes = mac.doFinal(input.toByteArray(StandardCharsets.UTF_8))
        return String(Base64.getEncoder().encode(signatureBytes))
    }

    /**
     * Percentage encode String as per RFC 3986, Section 2.1
     *
     * @param value
     * @return
     */
    private fun encode(value: String?): String {
        var encoded = ""
        try {
            encoded = URLEncoder.encode(value, "UTF-8")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        var sb = ""
        var focus: Char
        var i = 0
        while (i < encoded.length) {
            focus = encoded[i]
            if (focus == '*') {
                sb += "%2A"
            } else if (focus == '+') {
                sb += "%20"
            } else if (focus == '%' && i + 1 < encoded.length && encoded[i + 1] == '7' && encoded[i + 2] == 'E') {
                sb += '~'
                i += 2
            } else {
                sb += focus
            }
            i++
        }
        return sb
    }

    private fun put(map: MutableMap<String, String>, key: String, value: String) {
        map[encode(key)] = encode(value)
    }

    private fun append(builder: StringBuilder, key: String, value: String?) {
        builder.append(encode(key)).append("=\"").append(encode(value)).append("\",")
    }// letter 'z'

    // numeral '0'
    private val nonce: String
        private get() {
            val leftLimit = 48 // numeral '0'
            val rightLimit = 122 // letter 'z'
            val targetStringLength = 10
            val random = Random()
            return random.ints(leftLimit, rightLimit + 1).filter { i: Int -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97) }.limit(targetStringLength.toLong())
                .collect(
                    { StringBuilder() },
                    { obj: StringBuilder, codePoint: Int -> obj.appendCodePoint(codePoint) }
                ) { obj: StringBuilder, s: StringBuilder? -> obj.append(s) }.toString()
        }

    private val timestamp: String
        private get() = Math.round(Date().time / 1000.0).toString() + ""

    companion object {
        private const val oauth_consumer_key = "oauth_consumer_key"
        private const val oauth_token = "oauth_token"
        private const val oauth_signature_method = "oauth_signature_method"
        private const val oauth_timestamp = "oauth_timestamp"
        private const val oauth_nonce = "oauth_nonce"
        private const val oauth_version = "oauth_version"
        private const val oauth_signature = "oauth_signature"
        private const val HMAC_SHA1 = "HmacSHA1"
    }
}
