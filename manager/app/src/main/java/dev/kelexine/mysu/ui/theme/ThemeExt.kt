package dev.kelexine.mysu.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

val VoidDarkBackground = Color(0xFF08090E)
val VoidDarkSurface = Color(0xFF0E1017)
val VoidDarkSurfaceContainer = Color(0xFF131622)
val VoidDarkSurfaceContainerHigh = Color(0xFF1A1D2C)
val VoidDarkSurfaceContainerHighest = Color(0xFF222636)

fun ColorScheme.voidThemedBackground(isDark: Boolean, amoled: Boolean): ColorScheme {
    return if (amoled) {
        copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF070707),
            surfaceContainer = Color(0xFF0C0C0C),
            surfaceContainerHigh = Color(0xFF141414),
            surfaceContainerHighest = Color(0xFF1C1C1C),
            outline = Color(0xFF282828),
            outlineVariant = Color(0xFF1A1A1A),
        )
    } else if (isDark) {
        copy(
            background = VoidDarkBackground,
            surface = VoidDarkSurface,
            surfaceDim = VoidDarkBackground,
            surfaceContainerLowest = VoidDarkBackground,
            surfaceContainerLow = VoidDarkSurface,
            surfaceContainer = VoidDarkSurfaceContainer,
            surfaceContainerHigh = VoidDarkSurfaceContainerHigh,
            surfaceContainerHighest = VoidDarkSurfaceContainerHighest,
        )
    } else {
        this
    }
}

@Composable
fun rememberMySUColorScheme(
    seedColor: Color,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
): ColorScheme {
    val context = LocalContext.current
    val seed = if (seedColor == Color.Unspecified) {
        (if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).primary
    } else {
        seedColor
    }
    return rememberDynamicColorScheme(
        seedColor = seed,
        isDark = isDark,
        isAmoled = isAmoled,
        style = paletteStyle,
        specVersion = colorSpec.effectiveFor(paletteStyle),
    ).voidThemedBackground(isDark, isAmoled)
}

@Composable
fun ColorScheme.animateAsState(): ColorScheme {
    @Composable
    fun animateColor(color: Color): Color = animateColorAsState(
        targetValue = color,
        animationSpec = spring(),
        label = "theme_color_animation"
    ).value

    return ColorScheme(
        primary = animateColor(primary),
        onPrimary = animateColor(onPrimary),
        primaryContainer = animateColor(primaryContainer),
        onPrimaryContainer = animateColor(onPrimaryContainer),
        inversePrimary = animateColor(inversePrimary),
        secondary = animateColor(secondary),
        onSecondary = animateColor(onSecondary),
        secondaryContainer = animateColor(secondaryContainer),
        onSecondaryContainer = animateColor(onSecondaryContainer),
        tertiary = animateColor(tertiary),
        onTertiary = animateColor(onTertiary),
        tertiaryContainer = animateColor(tertiaryContainer),
        onTertiaryContainer = animateColor(onTertiaryContainer),
        background = animateColor(background),
        onBackground = animateColor(onBackground),
        surface = animateColor(surface),
        onSurface = animateColor(onSurface),
        surfaceVariant = animateColor(surfaceVariant),
        onSurfaceVariant = animateColor(onSurfaceVariant),
        surfaceTint = animateColor(surfaceTint),
        inverseSurface = animateColor(inverseSurface),
        inverseOnSurface = animateColor(inverseOnSurface),
        error = animateColor(error),
        onError = animateColor(onError),
        errorContainer = animateColor(errorContainer),
        onErrorContainer = animateColor(onErrorContainer),
        outline = animateColor(outline),
        outlineVariant = animateColor(outlineVariant),
        scrim = animateColor(scrim),
        surfaceBright = animateColor(surfaceBright),
        surfaceDim = animateColor(surfaceDim),
        surfaceContainer = animateColor(surfaceContainer),
        surfaceContainerHigh = animateColor(surfaceContainerHigh),
        surfaceContainerHighest = animateColor(surfaceContainerHighest),
        surfaceContainerLow = animateColor(surfaceContainerLow),
        surfaceContainerLowest = animateColor(surfaceContainerLowest),

        primaryFixed = animateColor(primaryFixed),
        primaryFixedDim = animateColor(primaryFixedDim),
        onPrimaryFixed = animateColor(onPrimaryFixed),
        onPrimaryFixedVariant = animateColor(onPrimaryFixedVariant),
        secondaryFixed = animateColor(secondaryFixed),
        secondaryFixedDim = animateColor(secondaryFixedDim),
        onSecondaryFixed = animateColor(onSecondaryFixed),
        onSecondaryFixedVariant = animateColor(onSecondaryFixedVariant),
        tertiaryFixed = animateColor(tertiaryFixed),
        tertiaryFixedDim = animateColor(tertiaryFixedDim),
        onTertiaryFixed = animateColor(onTertiaryFixed),
        onTertiaryFixedVariant = animateColor(onTertiaryFixedVariant)
    )
}