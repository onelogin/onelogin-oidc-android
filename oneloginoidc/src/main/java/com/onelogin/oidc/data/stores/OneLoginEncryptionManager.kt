package com.onelogin.oidc.data.stores

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal

/**
 * Created by trish.huynh@onelogin.com on 11/26/2019.
 */

@Suppress("DEPRECATION")
class OneLoginEncryptionManager(
    private val context: Context
) : EncryptionManager {

    private val sharedPreferences: SharedPreferences

    init {
        try {
            sharedPreferences = context.getSharedPreferences(ONELOGIN_SHARED_PREFERENCES, MODE_PRIVATE)
            if (!containsAlias()) {
                sharedPreferences.edit().clear().apply()
                generateKeys()
            } else {
                if (!verifyKeys()) {
                    removeKeys()
                    sharedPreferences.edit().clear().apply()
                    generateKeys()
                }
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun encrypt(plainData: String): String {
        val data = plainData.toByteArray(Charsets.UTF_8)

        val cipher = prepareCipher().apply {
            init(Cipher.ENCRYPT_MODE, getSecretKey())
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val encryptedData =
                mapOf(
                    ENCRYPTED_MAP_IV to Base64.encodeToString(cipher.iv, Base64.DEFAULT),
                    ENCRYPTED_MAP_DATA to Base64.encodeToString(
                        cipher.doFinal(data),
                        Base64.DEFAULT
                    )
                )
            Gson().toJson(encryptedData)
        } else {
            val encryptedData =
                mapOf(
                    ENCRYPTED_MAP_DATA to Base64.encodeToString(
                        cipher.doFinal(data),
                        Base64.DEFAULT
                    )
                )
            Gson().toJson(encryptedData)
        }
    }

    override fun decrypt(encryptedData: String): String {
        val typeToken = object : TypeToken<Map<String, String>>() {}.type
        val map = Gson().fromJson<Map<String, String>>(encryptedData, typeToken)
        val encryptedBytes = Base64.decode(map[ENCRYPTED_MAP_DATA], Base64.DEFAULT)
        val cipher = prepareCipher()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val ivBytes = Base64.decode(map[ENCRYPTED_MAP_IV], Base64.DEFAULT)
            val spec = GCMParameterSpec(128, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        } else {
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey())
        }

        val decrypted = cipher.doFinal(encryptedBytes)
        return String(decrypted)
    }

    private fun containsAlias(): Boolean {
        val keyStore = KeyStore.getInstance(PROVIDER).apply {
            load(null)
        }
        return keyStore.containsAlias(ALIAS)
    }

    private fun generateKeys() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        } else {
            val keyGenerator = KeyPairGenerator.getInstance(RSA_ENCRYPTION, PROVIDER)
            val start = Calendar.getInstance()
            val end = Calendar.getInstance()
            end.add(Calendar.YEAR, 20)

            val builder = KeyPairGeneratorSpec.Builder(context)
                .setAlias(ALIAS)
                .setSubject(X500Principal("CN=$ALIAS"))
                .setSerialNumber(BigInteger.TEN)
                .setStartDate(start.time)
                .setEndDate(end.time)

            keyGenerator.initialize(builder.build())
            keyGenerator.generateKeyPair()

            generateAndStoreAESKey()
        }
    }

    private fun verifyKeys(): Boolean {
        var isKeyValid = false
        KeyStore.getInstance(PROVIDER).apply {
            load(null)
            val keyEntry = getEntry(ALIAS, null)
            if (keyEntry is KeyStore.SecretKeyEntry && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isKeyValid = true
            }
            if (keyEntry is KeyStore.PrivateKeyEntry && Build.VERSION.SDK_INT < Build.VERSION_CODES.M && getAESEncrypted() != "") {
                isKeyValid = true
            }
        }
        return isKeyValid
    }

    private fun getSecretKey(): SecretKey {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val keyStore = KeyStore.getInstance(PROVIDER).apply {
                load(null)
            }
            val secretKeyEntry = keyStore?.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry
            secretKeyEntry.secretKey
        } else {
            val encryptedKey = Base64.decode(getAESEncrypted(), Base64.DEFAULT)
            val decryptedKey = rsaDecryptKey(encryptedKey)
            SecretKeySpec(decryptedKey, "AES")
        }
    }

    private fun removeKeys() {
        saveAESEncrypted("")
        KeyStore.getInstance(PROVIDER).apply {
            load(null)
            deleteEntry(ALIAS)
        }
    }

    @SuppressLint("GetInstance")
    private fun prepareCipher(): Cipher {
        val cipher: Cipher
        try {
            cipher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Cipher.getInstance(
                AES_MODE_M_OR_GREATER
            ) else Cipher.getInstance(
                AES_MODE_LESS_THAN_M,
                CIPHER_PROVIDER_NAME_ENCRYPTION_DECRYPTION_AES
            )
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException(e)
        }
        return cipher
    }

    // Methods only used for API < 23

    private fun generateAndStoreAESKey() {
        if (getAESEncrypted() == "") {
            val key = ByteArray(16)
            SecureRandom().apply {
                nextBytes(key)
            }
            saveAESEncrypted(Base64.encodeToString(rsaEncryptKey(key), Base64.DEFAULT))
        }
    }

    private fun rsaEncryptKey(secret: ByteArray): ByteArray {

        val keyStore = KeyStore.getInstance(PROVIDER).apply {
            load(null)
        }
        val privateKeyEntry = keyStore.getEntry(ALIAS, null) as KeyStore.PrivateKeyEntry
        val inputCipher =
            Cipher.getInstance(RSA_MODE, CIPHER_PROVIDER_NAME_ENCRYPTION_DECRYPTION_RSA)
        inputCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.certificate.publicKey)

        val outputStream = ByteArrayOutputStream()
        val cipherOutputStream = CipherOutputStream(outputStream, inputCipher)
        cipherOutputStream.write(secret)
        cipherOutputStream.close()

        return outputStream.toByteArray()
    }

    private fun rsaDecryptKey(encryptedSecret: ByteArray): ByteArray {
        val keyStore = KeyStore.getInstance(PROVIDER).apply {
            load(null)
        }
        val privateKeyEntry = keyStore.getEntry(ALIAS, null) as KeyStore.PrivateKeyEntry
        val outputCipher =
            Cipher.getInstance(RSA_MODE, CIPHER_PROVIDER_NAME_ENCRYPTION_DECRYPTION_RSA)
        outputCipher.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)
        val cipherInputStream = CipherInputStream(
            ByteArrayInputStream(encryptedSecret), outputCipher
        )

        return cipherInputStream.readBytes()
    }

    private fun getAESEncrypted() = sharedPreferences.getString(AES_ENCRYPTED_KEY, "")

    private fun saveAESEncrypted(value: String) = with(sharedPreferences.edit()) {
        putString(AES_ENCRYPTED_KEY, value)
        commit()
    }

    companion object {
        private const val AES_MODE_M_OR_GREATER = "AES/GCM/NoPadding"
        private const val AES_MODE_LESS_THAN_M = "AES/ECB/PKCS7Padding"
        private const val ALIAS = "ONELOGIN_PORTAL"
        private const val CIPHER_PROVIDER_NAME_ENCRYPTION_DECRYPTION_RSA = "AndroidOpenSSL"
        private const val CIPHER_PROVIDER_NAME_ENCRYPTION_DECRYPTION_AES = "BC"
        private const val ENCRYPTED_MAP_IV = "iv"
        private const val ENCRYPTED_MAP_DATA = "data"
        private const val PROVIDER = "AndroidKeyStore"
        private const val RSA_ENCRYPTION = "RSA"
        private const val RSA_MODE = "RSA/ECB/PKCS1Padding"
        private const val AES_ENCRYPTED_KEY = "oneloginAesEncrypted"
        internal const val ONELOGIN_SHARED_PREFERENCES = "oneloginPreferences"
    }
}
