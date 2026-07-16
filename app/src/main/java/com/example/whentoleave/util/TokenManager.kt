package com.example.whentoleave.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenManager {

    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_IS_GUEST = "is_guest"

    private fun getPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(context: Context, token: String) {
        getPrefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putBoolean(KEY_IS_GUEST, false)
            .apply()
    }

    fun getToken(context: Context): String? =
        getPrefs(context).getString(KEY_ACCESS_TOKEN, null)

    fun saveUserInfo(context: Context, userId: Long, nickname: String) {
        getPrefs(context).edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_NICKNAME, nickname)
            .apply()
    }

    fun getUserId(context: Context): Long =
        getPrefs(context).getLong(KEY_USER_ID, -1L)

    fun getNickname(context: Context): String? =
        getPrefs(context).getString(KEY_NICKNAME, null)

    // 게스트 로그인
    fun saveGuest(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_IS_GUEST, true)
            .putString(KEY_NICKNAME, "게스트")
            .apply()
    }

    fun isGuest(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_IS_GUEST, false)

    // 로그인 여부 (일반 + 게스트 둘 다 포함)
    fun isLoggedIn(context: Context): Boolean =
        getToken(context) != null || isGuest(context)

    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().commit()
    }
}