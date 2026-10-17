package com.audienceverdict.app.core

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "av_secure_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    fun read(): String? = prefs.getString("access_token", null)
    fun write(token: String?) { prefs.edit().apply { if (token == null) remove("access_token") else putString("access_token", token) }.apply() }
}
