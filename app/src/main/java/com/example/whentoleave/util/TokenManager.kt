package com.example.whentoleave.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenManager {

    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_NICKNAME = "nickname"

    private fun getPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        return getPrefs(context).getString(KEY_ACCESS_TOKEN, null)
    }

    fun saveUserInfo(context: Context, userId: Long, nickname: String) {
        getPrefs(context).edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_NICKNAME, nickname)
            .apply()
    }

    fun getUserId(context: Context): Long {
        return getPrefs(context).getLong(KEY_USER_ID, -1L)
    }

    fun getNickname(context: Context): String? {
        return getPrefs(context).getString(KEY_NICKNAME, null)
    }

    fun isLoggedIn(context: Context): Boolean {
        return getToken(context) != null
    }

    /** 토큰이 없거나 userId가 -1이면 게스트로 판단 */
    fun isGuest(context: Context): Boolean {
        return getToken(context) == null || getUserId(context) == -1L
    }

    /** 게스트 모드로 진입 (토큰 없이 nickname만 "게스트"로 저장) */
    fun saveGuest(context: Context) {
        getPrefs(context).edit()
            .putString(KEY_NICKNAME, "게스트")
            .putLong(KEY_USER_ID, -1L)
            .apply()
    }

    /** Authorization 헤더용 "Bearer {token}" 반환. 토큰 없으면 빈 문자열 */
    fun getBearerToken(context: Context): String {
        val token = getToken(context) ?: return ""
        return "Bearer $token"
    }

    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    // ── 현재 참여 중인 방 세션 (일반 SharedPreferences — 민감 정보 아님) ─────
    private fun roomPrefs(context: Context) =
        context.getSharedPreferences("room_session", Context.MODE_PRIVATE)

    fun saveCurrentRoom(context: Context, roomId: Long, inviteCode: String) {
        roomPrefs(context).edit()
            .putLong("room_id", roomId)
            .putString("room_invite", inviteCode)
            .apply()
    }

    /** 저장된 방 ID. 없으면 0L */
    fun getCurrentRoomId(context: Context): Long =
        roomPrefs(context).getLong("room_id", 0L)

    fun getCurrentRoomInvite(context: Context): String =
        roomPrefs(context).getString("room_invite", "") ?: ""

    fun clearCurrentRoom(context: Context) {
        roomPrefs(context).edit().clear().apply()
    }
}