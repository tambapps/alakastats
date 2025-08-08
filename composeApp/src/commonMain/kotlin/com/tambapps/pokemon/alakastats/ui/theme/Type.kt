package com.tambapps.pokemon.alakastats.ui.theme

import alakastats.composeapp.generated.resources.Inter_18pt_Bold
import alakastats.composeapp.generated.resources.Inter_18pt_Regular
import alakastats.composeapp.generated.resources.Outfit_Regular
import alakastats.composeapp.generated.resources.Res
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

import org.jetbrains.compose.resources.Font

val baseline = Typography()

@Composable
fun appTypography(colorScheme: ColorScheme): Typography  {
        val interRegular = Font(Res.font.Inter_18pt_Regular, FontWeight.Normal)
        val interBold = Font(Res.font.Inter_18pt_Bold, FontWeight.Bold)

        val outfitRegular = Font(Res.font.Outfit_Regular, FontWeight.Normal)
        val outfitBold = Font(Res.font.Outfit_Regular, FontWeight.Bold)

        val displayFontFamily = FontFamily(outfitRegular, outfitBold)
        val bodyFontFamily = FontFamily(interRegular, interBold)
        return Typography(
            displayLarge = baseline.displayLarge.copy(fontFamily = displayFontFamily, color = colorScheme.onSurface),
            displayMedium = baseline.displayMedium.copy(fontFamily = displayFontFamily, color = colorScheme.onSurface),
            displaySmall = baseline.displaySmall.copy(fontFamily = displayFontFamily, color = colorScheme.onSurface),
            headlineLarge = baseline.headlineLarge.copy(fontFamily = displayFontFamily, color = colorScheme.onSurface),
            headlineMedium = baseline.headlineMedium.copy(fontFamily = displayFontFamily, color = colorScheme.onSurface),
            headlineSmall = baseline.headlineSmall.copy(fontFamily = displayFontFamily, color = colorScheme.onSurface),
            titleLarge = baseline.titleLarge.copy(fontFamily = displayFontFamily, color = colorScheme.onSurface),
            titleMedium = baseline.titleMedium.copy(fontFamily = displayFontFamily, color = colorScheme.onSurface),
            titleSmall = baseline.titleSmall.copy(fontFamily = displayFontFamily, color = colorScheme.onSurface),
            bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily, color = colorScheme.onSurface),
            bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily, color = colorScheme.onSurface),
            bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily, color = colorScheme.onSurface),
            labelLarge = baseline.labelLarge.copy(fontFamily = bodyFontFamily, color = colorScheme.onSurface),
            labelMedium = baseline.labelMedium.copy(fontFamily = bodyFontFamily, color = colorScheme.onSurface),
            labelSmall = baseline.labelSmall.copy(fontFamily = bodyFontFamily, color = colorScheme.onSurface),
        )
    }

