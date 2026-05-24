package com.notilogger

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class SecurityActivity : AppCompatActivity() {

    private lateinit var layoutAuth: LinearLayout
    private lateinit var layoutSettings: LinearLayout
    private lateinit var layoutLockSetup: LinearLayout
    
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var btnVerify: MaterialButton
    
    private lateinit var switchAppLock: SwitchMaterial
    private lateinit var rgLockType: RadioGroup
    private lateinit var rbPassword: RadioButton
    private lateinit var rbPin: RadioButton
    
    private lateinit var rgTimeout: RadioGroup
    private lateinit var rbImmediate: RadioButton
    private lateinit var rb1Min: RadioButton
    private lateinit var rb2Min: RadioButton

    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnSave: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security)

        initViews()

        val isLocked = SecurityManager.isAppLocked(this)
        
        if (isLocked) {
            layoutAuth.visibility = View.VISIBLE
            layoutSettings.visibility = View.GONE
            
            // Set input type for auth based on saved type
            val type = SecurityManager.getLockType(this)
            if (type == SecurityManager.TYPE_PIN) {
                etCurrentPassword.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            } else {
                etCurrentPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
        } else {
            layoutAuth.visibility = View.GONE
            layoutSettings.visibility = View.VISIBLE
            populateSettings()
        }

        setupListeners()
    }

    private fun initViews() {
        layoutAuth = findViewById(R.id.layoutAuth)
        layoutSettings = findViewById(R.id.layoutSettings)
        layoutLockSetup = findViewById(R.id.layoutLockSetup)
        
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        btnVerify = findViewById(R.id.btnVerify)
        
        switchAppLock = findViewById(R.id.switchAppLock)
        rgLockType = findViewById(R.id.rgLockType)
        rbPassword = findViewById(R.id.rbPassword)
        rbPin = findViewById(R.id.rbPin)
        
        rgTimeout = findViewById(R.id.rgTimeout)
        rbImmediate = findViewById(R.id.rbImmediate)
        rb1Min = findViewById(R.id.rb1Min)
        rb2Min = findViewById(R.id.rb2Min)
        
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun populateSettings() {
        switchAppLock.isChecked = SecurityManager.isAppLocked(this)
        enableSetupViews(switchAppLock.isChecked)
        
        val type = SecurityManager.getLockType(this)
        if (type == SecurityManager.TYPE_PIN) {
            rbPin.isChecked = true
            updateInputTypes(true)
        } else {
            rbPassword.isChecked = true
            updateInputTypes(false)
        }

        when (SecurityManager.getLockTimeout(this)) {
            SecurityManager.TIMEOUT_IMMEDIATE -> rbImmediate.isChecked = true
            SecurityManager.TIMEOUT_1_MIN -> rb1Min.isChecked = true
            SecurityManager.TIMEOUT_2_MIN -> rb2Min.isChecked = true
            else -> rbImmediate.isChecked = true
        }
    }

    private fun enableSetupViews(isEnabled: Boolean) {
        rbPassword.isEnabled = isEnabled
        rbPin.isEnabled = isEnabled
        rbImmediate.isEnabled = isEnabled
        rb1Min.isEnabled = isEnabled
        rb2Min.isEnabled = isEnabled
        
        val tilPassword = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilPassword)
        val tilConfirmPassword = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilConfirmPassword)
        
        tilPassword.isEnabled = isEnabled
        tilConfirmPassword.isEnabled = isEnabled
    }

    private fun setupListeners() {
        btnVerify.setOnClickListener {
            val input = etCurrentPassword.text.toString()
            if (SecurityManager.verifyPassword(this, input)) {
                layoutAuth.visibility = View.GONE
                layoutSettings.visibility = View.VISIBLE
                populateSettings()
            } else {
                etCurrentPassword.error = "Incorrect Password"
            }
        }

        switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            enableSetupViews(isChecked)
        }

        rgLockType.setOnCheckedChangeListener { _, checkedId ->
            val isPin = checkedId == R.id.rbPin
            updateInputTypes(isPin)
        }

        btnSave.setOnClickListener {
            val timeout = when (rgTimeout.checkedRadioButtonId) {
                R.id.rb1Min -> SecurityManager.TIMEOUT_1_MIN
                R.id.rb2Min -> SecurityManager.TIMEOUT_2_MIN
                else -> SecurityManager.TIMEOUT_IMMEDIATE
            }
            SecurityManager.setLockTimeout(this, timeout)

            if (!switchAppLock.isChecked) {
                SecurityManager.setAppLocked(this, false)
                Toast.makeText(this, "App Lock Disabled", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            val pass = etPassword.text.toString()
            val confirm = etConfirmPassword.text.toString()

            if (pass.isEmpty()) {
                if (SecurityManager.isAppLocked(this)) {
                    // Password already exists, just update timeout and potentially lock type if changed
                    // (Though type usually needs new password input to re-hash)
                    Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    etPassword.error = "Please set a password"
                }
                return@setOnClickListener
            }

            if (pass != confirm) {
                etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            val type = if (rbPin.isChecked) SecurityManager.TYPE_PIN else SecurityManager.TYPE_PASSWORD
            SecurityManager.setPassword(this, pass, type)
            SecurityManager.setAppLocked(this, true)

            Toast.makeText(this, "Security Settings Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun updateInputTypes(isPin: Boolean) {
        val inputType = if (isPin) {
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        etPassword.inputType = inputType
        etConfirmPassword.inputType = inputType
        // Reset text when type changes to prevent issues
        etPassword.setText("")
        etConfirmPassword.setText("")
    }
}