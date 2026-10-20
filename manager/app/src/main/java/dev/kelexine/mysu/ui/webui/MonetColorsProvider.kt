package dev.kelexine.mysu.ui.webui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicReference

/**
 * @author rifsxd
 * @date 2025/6/2.
 */
object MonetColorsProvider {

    private val colorsCss: AtomicReference<String?> = AtomicReference(null)

    fun getColorsCss(): String {
        return colorsCss.get() ?: ""
    }

    @Composable
    fun UpdateCss(materialColorScheme: ColorScheme? = null) {
        UpdateCssMaterial(materialColorScheme ?: MaterialTheme.colorScheme)
    }

    @Composable
    private fun UpdateCssMaterial(colorScheme: ColorScheme) {
        LaunchedEffect(colorScheme) {
            val monetColors = mapOf(
                "primary" to colorScheme.primary.toCssValue(),
                "onPrimary" to colorScheme.onPrimary.toCssValue(),
                "primaryContainer" to colorScheme.primaryContainer.toCssValue(),
                "onPrimaryContainer" to colorScheme.onPrimaryContainer.toCssValue(),
                "inversePrimary" to colorScheme.inversePrimary.toCssValue(),
                "secondary" to colorScheme.secondary.toCssValue(),
                "onSecondary" to colorScheme.onSecondary.toCssValue(),
                "secondaryContainer" to colorScheme.secondaryContainer.toCssValue(),
                "onSecondaryContainer" to colorScheme.onSecondaryContainer.toCssValue(),
                "tertiary" to colorScheme.tertiary.toCssValue(),
                "onTertiary" to colorScheme.onTertiary.toCssValue(),
                "tertiaryContainer" to colorScheme.tertiaryContainer.toCssValue(),
                "onTertiaryContainer" to colorScheme.onTertiaryContainer.toCssValue(),
                "background" to colorScheme.background.toCssValue(),
                "onBackground" to colorScheme.onBackground.toCssValue(),
                "surface" to colorScheme.surface.toCssValue(),
                "tonalSurface" to colorScheme.surfaceColorAtElevation(1.dp).toCssValue(),
                "onSurface" to colorScheme.onSurface.toCssValue(),
                "surfaceVariant" to colorScheme.surfaceVariant.toCssValue(),
                "onSurfaceVariant" to colorScheme.onSurfaceVariant.toCssValue(),
                "surfaceTint" to colorScheme.surfaceTint.toCssValue(),
                "inverseSurface" to colorScheme.inverseSurface.toCssValue(),
                "inverseOnSurface" to colorScheme.inverseOnSurface.toCssValue(),
                "error" to colorScheme.error.toCssValue(),
                "onError" to colorScheme.onError.toCssValue(),
                "errorContainer" to colorScheme.errorContainer.toCssValue(),
                "onErrorContainer" to colorScheme.onErrorContainer.toCssValue(),
                "outline" to colorScheme.outline.toCssValue(),
                "outlineVariant" to colorScheme.outlineVariant.toCssValue(),
                "scrim" to colorScheme.scrim.toCssValue(),
                "surfaceBright" to colorScheme.surfaceBright.toCssValue(),
                "surfaceDim" to colorScheme.surfaceDim.toCssValue(),
                "surfaceContainer" to colorScheme.surfaceContainer.toCssValue(),
                "surfaceContainerHigh" to colorScheme.surfaceContainerHigh.toCssValue(),
                "surfaceContainerHighest" to colorScheme.surfaceContainerHighest.toCssValue(),
                "surfaceContainerLow" to colorScheme.surfaceContainerLow.toCssValue(),
                "surfaceContainerLowest" to colorScheme.surfaceContainerLowest.toCssValue(),
                "buttonContentColor" to colorScheme.onPrimary.toCssValue(),
                "buttonContainerColor" to colorScheme.primary.toCssValue(),
                "buttonDisabledContentColor" to colorScheme.onSurface.copy(alpha = 0.38f).toCssValue(),
                "buttonDisabledContainerColor" to colorScheme.onSurface.copy(alpha = 0.12f).toCssValue(),
                "elevatedButtonContentColor" to colorScheme.primary.toCssValue(),
                "elevatedButtonContainerColor" to colorScheme.surfaceContainerLow.toCssValue(),
                "elevatedButtonDisabledContentColor" to colorScheme.onSurface.copy(alpha = 0.38f).toCssValue(),
                "elevatedButtonDisabledContainerColor" to colorScheme.onSurface.copy(alpha = 0.12f).toCssValue(),
                "filledTonalButtonContentColor" to colorScheme.onSecondaryContainer.toCssValue(),
                "filledTonalButtonContainerColor" to colorScheme.secondaryContainer.toCssValue(),
                "filledTonalButtonDisabledContentColor" to colorScheme.onSurface.copy(alpha = 0.38f).toCssValue(),
                "filledTonalButtonDisabledContainerColor" to colorScheme.onSurface.copy(alpha = 0.12f).toCssValue(),
                "filledCardContentColor" to colorScheme.onPrimaryContainer.toCssValue(),
                "filledCardContainerColor" to colorScheme.primaryContainer.toCssValue(),
                "filledCardDisabledContentColor" to colorScheme.onSurface.copy(alpha = 0.38f).toCssValue(),
                "filledCardDisabledContainerColor" to colorScheme.onSurface.copy(alpha = 0.12f).toCssValue()
            )

            colorsCss.set(monetColors.toCssVars())
        }
    }

    private fun Map<String, String>.toCssVars(): String {
        return buildString {
            append(":root {\n")
            this@toCssVars.forEach { (k, v) ->
                append("  --md-sys-color-$k: $v;\n")
            }
            append("}\n")
        }
    }

    private fun Color.toCssValue(): String {
        val r = (red * 255).toInt()
        val g = (green * 255).toInt()
        val b = (blue * 255).toInt()
        val a = alpha
        return if (a < 1.0f) {
            "rgba($r, $g, $b, $a)"
        } else {
            String.format("#%02X%02X%02X", r, g, b)
        }
    }
}
