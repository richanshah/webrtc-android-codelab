package com.smartoffice.utils

object MQTTEncryptionUtil {

    private var cipher: MQTTAES? = null

    internal val secKey = byteArrayOf(0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf, 0x1)
    internal val iv = byteArrayOf(0x11, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f)


    private fun fillBlock(text: String): String {
        var text = text
        val spaceNum = if (text.toByteArray().size % 16 == 0) 0 else 16 - text.toByteArray().size % 16
        for (i in 0 until spaceNum)
            text += " "
        return text
    }

    /**
     *
     * Encrypt mqtt message
     *
     * @param plainText
     * @return array of bytes
     */
    fun encrypt(plainText: String): ByteArray {
        val text = fillBlock(plainText)
        val inputText = text.toByteArray()

        // cipher = new AES(secKey); // ECB test
        cipher = MQTTAES(secKey, iv) // CBC test

        return cipher!!.CBC_encrypt(inputText)
    }

    /**
     * Decrypt mqtt message
     *
     * @param cipherBytes
     * @return [String] plain string
     */
    fun decrypt(cipherBytes: ByteArray): String {
        cipher = MQTTAES(secKey, iv)
        val plainBytes = cipher!!.CBC_decrypt(cipherBytes)
        return String(plainBytes)
    }
}