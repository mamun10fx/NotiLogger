package com.notilogger

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If not locked, go straight to MainActivity
        if (!SecurityManager.isAppLocked(this)) {
            startMainActivity()
            return
        }

        setContentView(R.layout.activity_lock)

        val etPassword = findViewById<TextInputEditText>(R.id.etLockPassword)
        val btnUnlock = findViewById<MaterialButton>(R.id.btnUnlock)

        val type = SecurityManager.getLockType(this)
        if (type == SecurityManager.TYPE_PIN) {
            etPassword.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        } else {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        btnUnlock.setOnClickListener {
            val input = etPassword.text.toString()
            if (SecurityManager.verifyPassword(this, input)) {
                SecurityManager.clearBackgroundTime(this)
                startMainActivity()
            } else {
                etPassword.error = "Incorrect Password"
                Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}