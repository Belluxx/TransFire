package com.belluxx.transfire.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


const val CIPHER_ALGO = "AES/CBC/PKCS5Padding"

class Crypt {

    companion object {
        private fun deriveSalt(secretKeyString: String): ByteArray {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(secretKeyString.toByteArray(Charsets.UTF_8))
            return hash.copyOf(16)
        }

        private fun generateKey(secretKeyString: String): SecretKey {
            val salt = deriveSalt(secretKeyString)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keySpec = PBEKeySpec(secretKeyString.toCharArray(), salt, 60000, 256)

            return SecretKeySpec(factory.generateSecret(keySpec).encoded, "AES")
        }

        private fun generateIv(): IvParameterSpec {
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            return IvParameterSpec(iv)
        }

        private fun stringToIv(ivString: String): IvParameterSpec {
            return IvParameterSpec(fromBase64(ivString))
        }

        private fun toBase64(bytes: ByteArray): String {
            return Base64.getEncoder().encodeToString(bytes)
        }

        private fun fromBase64(s: String): ByteArray {
            return Base64.getDecoder().decode(s)
        }

        private fun encryptBytes(secretKeyString: String, plaintextBytes: ByteArray): Pair<String, String> {
            val secretKey = generateKey(secretKeyString)
            val iv = generateIv()

            val cipher = Cipher.getInstance(CIPHER_ALGO)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
            val ciphertext = cipher.doFinal(plaintextBytes)

            return Pair(toBase64(ciphertext), toBase64(iv.iv))
        }

        /**
         * Encrypt with AES the specified string
         *
         * @param plaintextString the UTF-8 string to encrypt
         * @return a pair of (base64 ciphertext, base64 initialization vector)
         */
        fun encryptString(secretKeyString: String, plaintextString: String): Pair<String, String> {
            val plaintextBytes = plaintextString.toByteArray(Charsets.UTF_8)

            return encryptBytes(secretKeyString, plaintextBytes)
        }

        /**
         * Encrypt with AES the specified bitmap
         *
         * @param plaintextBmp the bitmap to encrypt
         * @return a pair of (base64 ciphertext, base64 initialization vector)
         */
        fun encryptBitmap(secretKeyString: String, plaintextBmp: Bitmap): Pair<String, String> {
            val plaintext = bitmapToBytes(plaintextBmp)

            return encryptBytes(secretKeyString, plaintext)
        }

        /**
         * Decrypt with AES the specified byte array
         *
         * @param ciphertextString the base64 string to decrypt
         * @param ivString the base64 encoded initialization vector
         * @return the decrypted byte array
         */
        private fun decryptBytes(secretKeyString: String, ciphertextString: String, ivString: String): ByteArray {
            val ciphertext = fromBase64(ciphertextString)
            val secretKey = generateKey(secretKeyString)
            val iv = stringToIv(ivString)

            val cipher = Cipher.getInstance(CIPHER_ALGO)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
            val plaintext = cipher.doFinal(ciphertext)

            return plaintext
        }

        /**
         * Decrypt with AES the specified string
         *
         * @param ciphertextString the base64 string to decrypt
         * @param ivString the base64 encoded initialization vector
         * @return the decrypted string
         */
        fun decryptString(secretKeyString: String, ciphertextString: String, ivString: String): String {
            val plaintext = decryptBytes(secretKeyString, ciphertextString, ivString)

            return plaintext.toString(Charsets.UTF_8)
        }

        /**
         * Decrypt with AES the specified bitmap
         *
         * @param ciphertextString the base64 string to decrypt
         * @param ivString the base64 encoded initialization vector
         * @return the decrypted bitmap
         */
        fun decryptBitmap(secretKeyString: String, ciphertextString: String, ivString: String): Bitmap {
            val plaintext = decryptBytes(secretKeyString, ciphertextString, ivString)

            return bytesToBitmap(plaintext)
        }

        /**
         * Convert an image to a byte array
         *
         * @param bmp the bitmap
         * @return the byte array
         */
        private fun bitmapToBytes(bmp: Bitmap): ByteArray {
            val outStream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            val byteArray = outStream.toByteArray()

            return byteArray
        }

        /**
         * Convert a byte array to a bitmap
         *
         * @param bytes the byte array
         * @return the resulting bitmap
         */
        private fun bytesToBitmap(bytes: ByteArray): Bitmap {
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            return bmp
        }
    }
}
