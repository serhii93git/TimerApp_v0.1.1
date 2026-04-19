package com.example.timerapp.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.timerapp.ui.theme.PastelTimerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

private val Context.dataStore by preferencesDataStore(name = "settings")
val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
private const val FSI_PERMISSION_PREFS = "timer_permissions"
private const val KEY_FSI_ASKED = "fsi_asked"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeRequestFullScreenIntentPermission()
        setContent {
            PastelTimerTheme {
                var onboardingComplete by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(Unit) {
                    dataStore.data
                        .map { prefs -> prefs[ONBOARDING_COMPLETE_KEY] ?: false }
                        .collect { complete ->
                            onboardingComplete = complete
                        }
                }

                when (onboardingComplete) {
                    null  -> { /* Loading */ }
                    false -> OnboardingScreen(
                        onComplete = {
                            lifecycleScope.launch {
                                dataStore.edit { prefs ->
                                    prefs[ONBOARDING_COMPLETE_KEY] = true
                                }
                            }
                        }
                    )
                    true  -> TimerScreen()
                }
            }
        }
    }

    // On Android 14+, USE_FULL_SCREEN_INTENT is a special app-access permission.
    // Without it setFullScreenIntent() silently degrades to a heads-up, so the
    // alarm screen never auto-launches when the phone is locked. Ask once.
    private fun maybeRequestFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.canUseFullScreenIntent()) return

        val prefs = getSharedPreferences(FSI_PERMISSION_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_FSI_ASKED, false)) return

        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = Uri.parse("package:$packageName")
        }
        try {
            startActivity(intent)
            prefs.edit().putBoolean(KEY_FSI_ASKED, true).apply()
        } catch (_: Exception) {
            // Settings screen missing on some OEM ROMs — fall back to app details.
            runCatching {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:$packageName"))
                )
                prefs.edit().putBoolean(KEY_FSI_ASKED, true).apply()
            }
        }
    }
}
