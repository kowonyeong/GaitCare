package com.inhatc.gaitcare.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("gaitcare_prefs", Context.MODE_PRIVATE)

    fun saveAutoLoginInfo(institutionCode: String, userId: String, password: String) {
        prefs.edit()
            .putBoolean(KEY_AUTO_LOGIN, true)
            .putString(KEY_INSTITUTION_CODE, institutionCode)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun clearAutoLoginInfo() {
        prefs.edit()
            .putBoolean(KEY_AUTO_LOGIN, false)
            .remove(KEY_PASSWORD)
            .apply()
    }

    fun isAutoLogin(): Boolean = prefs.getBoolean(KEY_AUTO_LOGIN, false)
    fun getInstitutionCode(): String? = prefs.getString(KEY_INSTITUTION_CODE, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getPassword(): String? = prefs.getString(KEY_PASSWORD, null)

    fun saveSession(institutionId: Long, institutionCode: String, institutionName: String, managerName: String) {
        prefs.edit()
            .putLong(KEY_INSTITUTION_ID, institutionId)
            .putString(KEY_INSTITUTION_CODE, institutionCode)
            .putString(KEY_INSTITUTION_NAME, institutionName)
            .putString(KEY_MANAGER_NAME, managerName)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, false)
            .remove(KEY_INSTITUTION_ID)
            .remove(KEY_INSTITUTION_NAME)
            .remove(KEY_MANAGER_NAME)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)
    fun getInstitutionId(): Long = prefs.getLong(KEY_INSTITUTION_ID, -1L)
    fun getInstitutionName(): String = prefs.getString(KEY_INSTITUTION_NAME, "") ?: ""
    fun getManagerName(): String = prefs.getString(KEY_MANAGER_NAME, "") ?: ""

    companion object {
        private const val KEY_AUTO_LOGIN = "auto_login"
        private const val KEY_INSTITUTION_CODE = "institution_code"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_PASSWORD = "password"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_INSTITUTION_ID = "institution_id"
        private const val KEY_INSTITUTION_NAME = "institution_name"
        private const val KEY_MANAGER_NAME = "manager_name"
    }
}
