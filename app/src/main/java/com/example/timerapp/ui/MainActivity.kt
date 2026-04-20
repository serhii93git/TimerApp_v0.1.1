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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.timerapp.R
import com.example.timerapp.ui.theme.PastelTimerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

private val Context.dataStore by preferencesDataStore(name = "settings")
val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
private const val FSI_PERMISSION_PREFS = "timer_permissions"
private const val KEY_FSI_ASKED = "fsi_asked"
private const val KEY_OVERLAY_ASKED = "overlay_asked"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    true  -> {
                        TimerScreen()
                        // For users who completed onboarding before these permissions
                        // existed: explain what they unlock, then hand off to settings.
                        PostOnboardingPermissionDialogs()
                    }
                }
            }
        }
    }
}

// Permission types the dialogs walk the user through after onboarding.
private enum class PostOnboardingPerm { Fsi, Overlay }

@Composable
private fun PostOnboardingPermissionDialogs() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(FSI_PERMISSION_PREFS, Context.MODE_PRIVATE)
    }

    // Build the queue once; dialogs are only shown for perms that are still
    // missing AND haven't been dismissed/offered before. canUseFullScreenIntent()
    // below API 34 is always true, so the FSI branch self-skips on older devices.
    val queue = remember {
        mutableStateListOf<PostOnboardingPerm>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val nm = context.getSystemService(NotificationManager::class.java)
                if (nm?.canUseFullScreenIntent() == false &&
                    !prefs.getBoolean(KEY_FSI_ASKED, false)
                ) add(PostOnboardingPerm.Fsi)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(context) &&
                !prefs.getBoolean(KEY_OVERLAY_ASKED, false)
            ) add(PostOnboardingPerm.Overlay)
        }
    }

    val current = queue.firstOrNull() ?: return

    val title: String
    val body: String
    val openSettings: () -> Unit
    val prefsKey: String

    when (current) {
        PostOnboardingPerm.Fsi -> {
            title = stringResource(R.string.perm_dialog_fsi_title)
            body  = stringResource(R.string.perm_dialog_fsi_body)
            prefsKey = KEY_FSI_ASKED
            openSettings = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:${context.packageName}")
                )
                runCatching { context.startActivity(intent) }.onFailure {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:${context.packageName}"))
                        )
                    }
                }
            }
        }
        PostOnboardingPerm.Overlay -> {
            title = stringResource(R.string.perm_dialog_overlay_title)
            body  = stringResource(R.string.perm_dialog_overlay_body)
            prefsKey = KEY_OVERLAY_ASKED
            openSettings = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                runCatching { context.startActivity(intent) }
            }
        }
    }

    val markAskedAndNext: () -> Unit = {
        prefs.edit().putBoolean(prefsKey, true).apply()
        queue.removeAt(0)
    }

    AlertDialog(
        onDismissRequest = markAskedAndNext,
        title = { Text(title) },
        text  = { Text(body) },
        confirmButton = {
            TextButton(onClick = {
                openSettings()
                markAskedAndNext()
            }) { Text(stringResource(R.string.perm_dialog_grant)) }
        },
        dismissButton = {
            TextButton(onClick = markAskedAndNext) {
                Text(stringResource(R.string.perm_dialog_later))
            }
        }
    )
}
