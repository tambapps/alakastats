package com.example.ui.theme

import alakastats.composeapp.generated.resources.Inter_18pt_Bold
import alakastats.composeapp.generated.resources.Inter_18pt_Regular
import alakastats.composeapp.generated.resources.Outfit_Regular
import alakastats.composeapp.generated.resources.Res
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

import org.jetbrains.compose.resources.Font

val baseline = Typography()
val AppTypography: Typography
    @Composable get() {
        val interRegular = Font(Res.font.Inter_18pt_Regular, FontWeight.Normal)
        val interBold = Font(Res.font.Inter_18pt_Bold, FontWeight.Bold)

        val outfitRegular = Font(Res.font.Outfit_Regular, FontWeight.Normal)
        val outfitBold = Font(Res.font.Outfit_Regular, FontWeight.Bold)

        val displayFontFamily = FontFamily(outfitRegular, outfitBold)
        val bodyFontFamily = FontFamily(interRegular, interBold)
        return Typography(
            displayLarge = baseline.displayLarge.copy(fontFamily = displayFontFamily),
            displayMedium = baseline.displayMedium.copy(fontFamily = displayFontFamily),
            displaySmall = baseline.displaySmall.copy(fontFamily = displayFontFamily),
            headlineLarge = baseline.headlineLarge.copy(fontFamily = displayFontFamily),
            headlineMedium = baseline.headlineMedium.copy(fontFamily = displayFontFamily),
            headlineSmall = baseline.headlineSmall.copy(fontFamily = displayFontFamily),
            titleLarge = baseline.titleLarge.copy(fontFamily = displayFontFamily),
            titleMedium = baseline.titleMedium.copy(fontFamily = displayFontFamily),
            titleSmall = baseline.titleSmall.copy(fontFamily = displayFontFamily),
            bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily),
            bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily),
            bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily),
            labelLarge = baseline.labelLarge.copy(fontFamily = bodyFontFamily),
            labelMedium = baseline.labelMedium.copy(fontFamily = bodyFontFamily),
            labelSmall = baseline.labelSmall.copy(fontFamily = bodyFontFamily),
        )
    }

