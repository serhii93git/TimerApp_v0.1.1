package com.example.timerapp.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timerapp.R
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current

    val titleText        = stringResource(R.string.onboarding_title)
    val bodyText         = stringResource(R.string.onboarding_body)
    val permNotifText    = stringResource(R.string.permission_notifications)
    val permAlarmText    = stringResource(R.string.permission_exact_alarm)
    val permBatteryText  = stringResource(R.string.permission_battery)

    // FIX: Reduced from 10 to 3 seconds — 10s felt excessive
    var countdown by remember { mutableIntStateOf(3) }
    var step      by remember { mutableIntStateOf(0) }

    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { onComplete() }

    val alarmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        step = 3
        val batteryIntent = try {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}"))
        } catch (e: Exception) {
            try { Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) }
            catch (e2: Exception) {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}"))
            }
        }
        try { batteryLauncher.launch(batteryIntent) }
        catch (e: Exception) { onComplete() }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        step = 2
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}"))
            try { alarmLauncher.launch(alarmIntent) }
            catch (e: Exception) {
                step = 3
                val batteryIntent = try {
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}"))
                } catch (e2: Exception) {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}"))
                }
                try { batteryLauncher.launch(batteryIntent) }
                catch (e3: Exception) { onComplete() }
            }
        } else {
            step = 3
            try {
                batteryLauncher.launch(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}"))
                )
            } catch (e: Exception) { onComplete() }
        }
    }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1_000)
            countdown--
        }
        step = 1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            step = 2
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:${context.packageName}"))
                try { alarmLauncher.launch(alarmIntent) }
                catch (e: Exception) { onComplete() }
            } else {
                onComplete()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "\u23F1", fontSize = 72.sp)
            Spacer(Modifier.height(24.dp))
            Text(
                text      = titleText,
                style     = MaterialTheme.typography.headlineMedium,
                color     = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text      = bodyText,
                style     = MaterialTheme.typography.bodyLarge,
                color     = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            listOf(permNotifText, permAlarmText, permBatteryText).forEach { label ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(text = label, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(40.dp))

            if (countdown > 0) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text(
                    text  = stringResource(R.string.requesting_in_seconds, countdown),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
