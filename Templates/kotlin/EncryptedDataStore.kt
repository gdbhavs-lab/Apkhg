package {{PACKAGE}}

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.webkit.JavascriptInterface
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject
import org.json.JSONArray

/**
 * Encrypted DataStore - تخزين مشفر آمن
 * يوفر تشفير AES-256-GCM للبيانات الحساسة
 */
class EncryptedDataStore(private val context: Context) {
    
    companion object {
        private const val KEYSTORE_ALIAS = "AppSecureKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val PREFS_NAME = "encrypted_datastore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    init {
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            generateKey()
        }
    }
    
    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        
        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }
    
    private fun getSecretKey(): SecretKey {
        return (keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * حفظ قيمة مشفرة
     */
    @JavascriptInterface
    fun save(key: String, value: String): Boolean {
        return try {
            val encrypted = encrypt(value)
            prefs.edit().putString(key, encrypted).apply()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * استرجاع قيمة مشفرة
     */
    @JavascriptInterface
    fun get(key: String): String {
        return try {
            val encrypted = prefs.getString(key, null) ?: return ""
            decrypt(encrypted)
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * حفظ JSON Object
     */
    @JavascriptInterface
    fun saveJson(key: String, jsonString: String): Boolean {
        return try {
            JSONObject(jsonString) // validate JSON
            save(key, jsonString)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * استرجاع JSON Object
     */
    @JavascriptInterface
    fun getJson(key: String): String {
        return get(key)
    }
    
    /**
     * حذف قيمة
     */
    @JavascriptInterface
    fun delete(key: String): Boolean {
        return try {
            prefs.edit().remove(key).apply()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * التحقق من وجود مفتاح
     */
    @JavascriptInterface
    fun exists(key: String): Boolean {
        return prefs.contains(key)
    }
    
    /**
     * الحصول على جميع المفاتيح
     */
    @JavascriptInterface
    fun getAllKeys(): String {
        return JSONArray(prefs.all.keys.toList()).toString()
    }
    
    /**
     * مسح جميع البيانات
     */
    @JavascriptInterface
    fun clear(): Boolean {
        return try {
            prefs.edit().clear().apply()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * الحصول على حجم البيانات المخزنة
     */
    @JavascriptInterface
    fun count(): Int {
        return prefs.all.size
    }
    
    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }
    
    private fun decrypt(encryptedText: String): String {
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
        
        val iv = combined.copyOfRange(0, IV_SIZE)
        val encrypted = combined.copyOfRange(IV_SIZE, combined.size)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
}
