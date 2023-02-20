package com.netpluspay.netpossdk.utils

import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.text.DecimalFormat
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

object ExtensionFunctions {
    private fun toHex(nybble: Int): Char {
        require(!(nybble < 0 || nybble > 15))
        return "0123456789ABCDEF"[nybble]
    }

    private fun getKey(key: ByteArray): ByteArray {
        val bKey = ByteArray(24)
        var i: Int
        if (key.size == 8) {
            i = 0
            while (i < 8) {
                bKey[i] = key[i]
                bKey[i + 8] = key[i]
                bKey[i + 16] = key[i]
                i++
            }
        } else if (key.size == 16) {
            i = 0
            while (i < 8) {
                bKey[i] = key[i]
                bKey[i + 8] = key[i + 8]
                bKey[i + 16] = key[i]
                i++
            }
        } else if (key.size == 24) {
            i = 0
            while (i < 24) {
                bKey[i] = key[i]
                i++
            }
        }
        return bKey
    }

    fun encrypt(data: ByteArray?, key: ByteArray): ByteArray? {
// 		Log.d(TripleDES.class.getSimpleName(), "Data: " +  Hex2String(data));
// 		Log.d(TripleDES.class.getSimpleName(), "Key: " +  Hex2String(key));
        val sk: SecretKey = SecretKeySpec(getKey(key), "DESede")
        try {
            val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, sk)
            return cipher.doFinal(data)
        } catch (e: NoSuchPaddingException) {
        } catch (e: NoSuchAlgorithmException) {
        } catch (e: InvalidKeyException) {
        } catch (e: BadPaddingException) {
        } catch (e: IllegalBlockSizeException) {
        }
        return null
    }

    fun encrypt(data: String, key: String): String {
        val bData: ByteArray
        val bKey: ByteArray
        val bOutput: ByteArray?
        val result: String
        bData = String2Hex(data)
        bKey = String2Hex(key)
        bOutput = encrypt(bData, bKey)
        result = Hex2String(bOutput)
        return result
    }

    fun Hex2String(data: ByteArray?): String {
        if (data == null) {
            return ""
        }
        var result = ""
        for (i in data.indices) {
            var tmp: Int = data[i].toInt() shr 4
            result += Integer.toString(tmp and 0x0F, 16)
            tmp = (data[i] and 0x0F).toInt()
            result += Integer.toString(tmp and 0x0F, 16)
        }
        return result
    }

    fun String2Hex(data: String): ByteArray {
        val result: ByteArray
        result = ByteArray(data.length / 2)
        var i = 0
        while (i < data.length) {
            result[i / 2] = data.substring(i, i + 2).toInt(16).toByte()
            i += 2
        }
        return result
    }

    fun xorHex(a: String, b: String): String {
        // TODO: Validation
        val chars = CharArray(a.length)
        for (i in chars.indices) {
            chars[i] = toHex(fromHex(a[i]) xor fromHex(b[i]))
        }
        return String(chars)
    }

    private fun fromHex(c: Char): Int {
        if (c in '0'..'9') {
            return c - '0'
        }
        if (c in 'A'..'F') {
            return c - 'A' + 10
        }
        if (c in 'a'..'f') {
            return c - 'a' + 10
        }
        throw IllegalArgumentException()
    }

    fun Number.formatCurrencyAmount(currencySymbol: String = "\u20A6"): String {
        val format = DecimalFormat("#,###.00")
        return "$currencySymbol ${format.format(this)}"
    }
}
