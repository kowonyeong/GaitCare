package com.inhatc.gaitcare

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.inhatc.gaitcare.ui.auth.LoginActivity
import com.inhatc.gaitcare.ui.main.MainInstitutionActivity
import com.inhatc.gaitcare.utils.PreferenceManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager(this)
        val targetClass = if (prefs.isLoggedIn()) MainInstitutionActivity::class.java else LoginActivity::class.java
        val intent = Intent(this, targetClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
