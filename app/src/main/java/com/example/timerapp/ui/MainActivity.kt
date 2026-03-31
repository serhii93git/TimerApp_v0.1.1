package com.example.timerapp.ui

import android.content.Context
import android.os.Bundle
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
                    true  -> TimerScreen()
                }
            }
        }
    }
}
