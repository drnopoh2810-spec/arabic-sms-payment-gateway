package me.capcom.smsgateway.modules.gateway

import android.content.SharedPreferences
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.launch
import me.capcom.smsgateway.R
import me.capcom.smsgateway.modules.logs.LogsService
import me.capcom.smsgateway.modules.logs.db.LogEntry
import org.koin.android.ext.android.inject

/**
 * شاشة إعدادات بوابة الرسائل النصية
 */
class GatewayPreferencesFragment : PreferenceFragmentCompat(), 
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val gatewayService by inject<GatewayService>()
    private val gatewaySettings by inject<GatewaySettings>()
    private val logsService by inject<LogsService>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.gateway_preferences, rootKey)
        
        setupPreferences()
        updatePreferenceSummaries()
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        updateConnectionStatus()
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun setupPreferences() {
        // إعداد اختبار الاتصال
        findPreference<Preference>("test_connection")?.setOnPreferenceClickListener {
            testConnection()
            true
        }

        // إعداد مسح السجلات
        findPreference<Preference>("clear_logs")?.setOnPreferenceClickListener {
            clearLogs()
            true
        }

        // إعداد إعادة تعيين الإعدادات
        findPreference<Preference>("reset_settings")?.setOnPreferenceClickListener {
            resetSettings()
            true
        }

        // إعداد معلومات الجهاز
        findPreference<Preference>("device_info")?.setOnPreferenceClickListener {
            showDeviceInfo()
            true
        }
    }

    private fun updatePreferenceSummaries() {
        // تحديث ملخص وضع الخادم
        val serverModePreference = findPreference<ListPreference>("server_mode")
        serverModePreference?.let { pref ->
            val currentMode = gatewaySettings.serverMode
            pref.value = currentMode.name
            pref.summary = when (currentMode) {
                GatewaySettings.ServerMode.LOCAL_ONLY -> getString(R.string.server_mode_local_only_summary)
                GatewaySettings.ServerMode.CLOUD_ONLY -> getString(R.string.server_mode_cloud_only_summary)
                GatewaySettings.ServerMode.AUTO -> getString(R.string.server_mode_auto_summary)
            }
        }

        // تحديث ملخص عنوان الخادم
        val serverUrlPreference = findPreference<EditTextPreference>("cloud_server_url")
        serverUrlPreference?.let { pref ->
            pref.text = gatewaySettings.serverUrl
            pref.summary = gatewaySettings.serverUrl
        }

        // تحديث حالة التفعيل
        val enabledPreference = findPreference<SwitchPreferenceCompat>("gateway_enabled")
        enabledPreference?.isChecked = gatewaySettings.enabled
    }

    private fun updateConnectionStatus() {
        val statusPreference = findPreference<Preference>("connection_status")
        statusPreference?.let { pref ->
            val status = gatewayService.getConnectionStatus()
            val (title, summary) = when (status) {
                GatewayService.ConnectionStatus.DISABLED -> {
                    getString(R.string.connection_status_disabled) to 
                    getString(R.string.connection_status_disabled_summary)
                }
                GatewayService.ConnectionStatus.LOCAL_ONLY -> {
                    getString(R.string.connection_status_local_only) to 
                    getString(R.string.connection_status_local_only_summary)
                }
                GatewayService.ConnectionStatus.CLOUD_CONNECTED -> {
                    getString(R.string.connection_status_cloud_connected) to 
                    getString(R.string.connection_status_cloud_connected_summary)
                }
                GatewayService.ConnectionStatus.CLOUD_DISCONNECTED -> {
                    getString(R.string.connection_status_cloud_disconnected) to 
                    getString(R.string.connection_status_cloud_disconnected_summary)
                }
            }
            
            pref.title = title
            pref.summary = summary
        }
    }

    private fun testConnection() {
        val testPreference = findPreference<Preference>("test_connection")
        testPreference?.summary = getString(R.string.connection_status_testing)
        testPreference?.isEnabled = false

        lifecycleScope.launch {
            try {
                if (gatewaySettings.serverMode == GatewaySettings.ServerMode.LOCAL_ONLY) {
                    testPreference?.summary = getString(R.string.test_connection_local_mode)
                    return@launch
                }

                val isConnected = gatewayService.testCloudConnection()
                
                val message = if (isConnected) {
                    getString(R.string.test_connection_success)
                } else {
                    getString(R.string.test_connection_failed, "خطأ في الاتصال")
                }
                
                testPreference?.summary = message
                updateConnectionStatus()
                
            } catch (e: Exception) {
                val errorMessage = getString(R.string.test_connection_failed, e.localizedMessage)
                testPreference?.summary = errorMessage
                
                logsService.insert(
                    LogEntry.Priority.ERROR,
                    "GatewayPreferences",
                    "فشل اختبار الاتصال: ${e.message}",
                    mapOf("error" to e.toString())
                )
            } finally {
                testPreference?.isEnabled = true
            }
        }
    }

    private fun clearLogs() {
        // عرض حوار تأكيد
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.clear_logs_title)
            .setMessage(R.string.confirm_clear_logs)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    try {
                        logsService.clearAll()
                        showToast(getString(R.string.logs_cleared))
                    } catch (e: Exception) {
                        showToast(getString(R.string.error_unknown))
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun resetSettings() {
        // عرض حوار تأكيد
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.reset_settings_title)
            .setMessage(R.string.confirm_reset_settings)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                try {
                    // إعادة تعيين الإعدادات للقيم الافتراضية
                    gatewaySettings.enabled = false
                    gatewaySettings.serverMode = GatewaySettings.ServerMode.LOCAL_ONLY
                    gatewaySettings.registrationInfo = null
                    
                    // تحديث واجهة المستخدم
                    updatePreferenceSummaries()
                    updateConnectionStatus()
                    
                    showToast(getString(R.string.settings_reset))
                } catch (e: Exception) {
                    showToast(getString(R.string.error_unknown))
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDeviceInfo() {
        val deviceInfo = buildString {
            append("معرف الجهاز: ${gatewaySettings.deviceId ?: "غير محدد"}\n")
            append("وضع الخادم: ${gatewaySettings.serverMode.name}\n")
            append("عنوان الخادم: ${gatewaySettings.serverUrl}\n")
            append("حالة التفعيل: ${if (gatewaySettings.enabled) "مفعل" else "معطل"}\n")
            append("رمز FCM: ${gatewaySettings.fcmToken?.take(20) ?: "غير متوفر"}...")
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.device_info_title)
            .setMessage(deviceInfo)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "gateway_enabled" -> {
                val enabled = sharedPreferences?.getBoolean(key, false) ?: false
                gatewaySettings.enabled = enabled
                updateConnectionStatus()
            }
            
            "server_mode" -> {
                val modeValue = sharedPreferences?.getString(key, "LOCAL_ONLY") ?: "LOCAL_ONLY"
                val mode = try {
                    GatewaySettings.ServerMode.valueOf(modeValue)
                } catch (e: IllegalArgumentException) {
                    GatewaySettings.ServerMode.LOCAL_ONLY
                }
                gatewaySettings.serverMode = mode
                updatePreferenceSummaries()
                updateConnectionStatus()
            }
            
            "cloud_server_url" -> {
                val url = sharedPreferences?.getString(key, "") ?: ""
                if (url.isNotBlank() && url.startsWith("https://")) {
                    // تحديث عنوان الخادم في الإعدادات
                    updatePreferenceSummaries()
                } else if (url.isNotBlank()) {
                    showToast(getString(R.string.error_invalid_url))
                }
            }
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
}