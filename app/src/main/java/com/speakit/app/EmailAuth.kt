package com.speakit.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection

data class EmailSession(val userId: String, val email: String, val accessToken: String, val refreshToken: String, val expiresAt: Long)

/** Only public client configuration belongs here. Never add SMTP or service-role secrets. */
class EmailAuth(context: Context) {
    companion object {
        const val BASE_URL = "https://potquhooxffwxybytkbc.supabase.co"
        const val PUBLISHABLE_KEY = "sb_publishable_8DQ4wZwndjqcmZ4ia26NWw_V5VPQsIv"
    }
    private val storage = context.getSharedPreferences("speakit_verified_session", Context.MODE_PRIVATE)
    private val keyAlias = "speakit-auth-v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun restore(): EmailSession? = try {
        val encoded = storage.getString("session", null)
        if(encoded == null) null else {
            val parts = encoded.split(':', limit = 2)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
            fromJson(JSONObject(String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)))
        }
    } catch (_: Exception) { clear(); null }
    private fun persist(session: EmailSession) {
        val json = JSONObject().put("access_token", session.accessToken).put("refresh_token", session.refreshToken)
            .put("expires_at", session.expiresAt).put("user", JSONObject().put("id",session.userId).put("email",session.email))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        check(storage.edit().putString("session", Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()) { "Could not save your sign-in. Please try again." }
    }
    fun clear() { storage.edit().clear().commit() }
    private fun fromJson(json: JSONObject): EmailSession {
        val user = json.getJSONObject("user")
        val id = user.getString("id")
        require(Regex("[a-fA-F0-9-]{36}").matches(id)) { "Invalid account response." }
        val token = json.getString("access_token")
        require(token.isNotBlank()) { "Verification did not return a session." }
        return EmailSession(id, user.getString("email"), token, json.getString("refresh_token"),
            json.optLong("expires_at", System.currentTimeMillis()/1000 + json.optLong("expires_in",3600)))
    }
    suspend fun sendCode(email: String) = withContext(Dispatchers.IO) {
        post("/otp", JSONObject().put("email",email).put("create_user",true))
        Unit
    }
    suspend fun verify(email: String, code: String): EmailSession = withContext(Dispatchers.IO) {
        val response = post("/verify", JSONObject().put("email",email).put("token",code).put("type","email"))
        val session = fromJson(response)
        require(session.email.equals(email,true)) { "The verified email did not match." }
        persist(session)
        session
    }
    suspend fun logout(session: EmailSession): Boolean = withContext(Dispatchers.IO) {
        // Local credentials are removed even if internet is unavailable.
        clear()
        try {
            val current = if(session.expiresAt <= System.currentTimeMillis()/1000 + 30)
                fromJson(post("/token?grant_type=refresh_token", JSONObject().put("refresh_token",session.refreshToken))) else session
            post("/logout?scope=local", JSONObject(), current.accessToken)
            true
        } catch (_: Exception) { false }
    }
    private fun post(path: String, data: JSONObject, bearer: String? = null): JSONObject {
        val connection = URL("$BASE_URL/auth/v1$path").openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = "POST"; connection.connectTimeout = 15000; connection.readTimeout = 20000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", PUBLISHABLE_KEY)
            connection.setRequestProperty("Content-Type", "application/json")
            if(bearer != null) connection.setRequestProperty("Authorization", "Bearer $bearer")
            connection.doOutput = true
            connection.outputStream.use { it.write(data.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if(status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = try { if(body.isBlank()) JSONObject() else JSONObject(body) } catch (_: Exception) { JSONObject() }
            if(status !in 200..299) {
                val code = json.optString("error_code")
                val message = when {
                    status == 429 -> "Too many requests. Wait before asking for another code."
                    code == "otp_expired" -> "That code is invalid or expired. Check the newest email or request a new code."
                    code.contains("email_address") -> "This email cannot receive sign-in codes. Check the address and sender setup."
                    status >= 500 -> "The email service could not send or verify the code. Check Supabase and Brevo email logs."
                    else -> json.optString("msg", json.optString("message", json.optString("error_description", "Sign-in failed (HTTP $status)."))).take(240)
                }
                throw IllegalStateException(message)
            }
            return json
        } finally { connection.disconnect() }
    }
}
