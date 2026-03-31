package com.example.timerapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val PastelLightColors = lightColorScheme(
    primary              = Lavender,
    onPrimary            = White,
    primaryContainer     = PaleLilac,
    onPrimaryContainer   = DarkPurple,
    secondary            = PastelMint,
    onSecondary          = White,
    secondaryContainer   = LightMint,
    onSecondaryContainer = DarkPurple,
    tertiary             = SoftPeach,
    onTertiary           = White,
    tertiaryContainer    = PalePeach,
    onTertiaryContainer  = DarkPurple,
    background           = OffWhiteLav,
    onBackground         = DarkPurple,
    surface              = SurfaceWhite,
    onSurface            = DarkPurple,
    surfaceVariant       = PaleLilac,
    onSurfaceVariant     = MutedPurple,
    outline              = OutlineGray,
    error                = SoftRose,
    onError              = White
)

private val PastelDarkColors = darkColorScheme(
    primary              = DarkLavender,
    onPrimary            = DarkOnLavender,
    primaryContainer     = DarkLavenderCont,
    onPrimaryContainer   = DarkOnLavCont,
    secondary            = DarkMint,
    onSecondary          = DarkOnMint,
    tertiary             = DarkPeach,
    tertiaryContainer    = DarkPeachCont,
    background           = DarkBG,
    surface              = DarkSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = DarkOnSurfaceVar,
    outline              = DarkOutline,
    error                = DarkError
)

@Composable
fun PastelTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else           dynamicLightColorScheme(context)
        }
        darkTheme -> PastelDarkColors
        else      -> PastelLightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = TimerTypography,
        content     = content
    )
}
