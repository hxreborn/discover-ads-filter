package eu.hxreborn.discoveradsfilter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import eu.hxreborn.discoveradsfilter.R

private fun googleSansFlex(
    weight: Int,
    opsz: Float,
): FontFamily =
    FontFamily(
        Font(
            resId = R.font.google_sans_flex,
            weight = FontWeight(weight),
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(weight),
                    FontVariation.opticalSizing(opsz.sp),
                    FontVariation.Setting("ROND", 100f),
                ),
        ),
    )

private val displayFamily = googleSansFlex(weight = 700, opsz = 48f)
private val headlineFamily = googleSansFlex(weight = 700, opsz = 32f)
private val titleLargeFamily = googleSansFlex(weight = 600, opsz = 24f)
private val titleFamily = googleSansFlex(weight = 500, opsz = 24f)
private val bodyFamily = googleSansFlex(weight = 400, opsz = 18f)
private val labelFamily = googleSansFlex(weight = 500, opsz = 14f)

private val base = Typography()

val DiscoverAdsFilterTypography =
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = displayFamily),
        displayMedium = base.displayMedium.copy(fontFamily = displayFamily),
        displaySmall = base.displaySmall.copy(fontFamily = displayFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = headlineFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = headlineFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = headlineFamily),
        titleLarge = base.titleLarge.copy(fontFamily = titleLargeFamily),
        titleMedium = base.titleMedium.copy(fontFamily = titleFamily),
        titleSmall = base.titleSmall.copy(fontFamily = titleFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = bodyFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = bodyFamily),
        bodySmall = base.bodySmall.copy(fontFamily = bodyFamily),
        labelLarge = base.labelLarge.copy(fontFamily = labelFamily),
        labelMedium = base.labelMedium.copy(fontFamily = labelFamily),
        labelSmall = base.labelSmall.copy(fontFamily = labelFamily),
    ).copy(
        displayLargeEmphasized = base.displayLargeEmphasized.copy(fontFamily = displayFamily),
        displayMediumEmphasized = base.displayMediumEmphasized.copy(fontFamily = displayFamily),
        displaySmallEmphasized = base.displaySmallEmphasized.copy(fontFamily = displayFamily),
        headlineLargeEmphasized = base.headlineLargeEmphasized.copy(fontFamily = headlineFamily),
        headlineMediumEmphasized = base.headlineMediumEmphasized.copy(fontFamily = headlineFamily),
        headlineSmallEmphasized = base.headlineSmallEmphasized.copy(fontFamily = headlineFamily),
        titleLargeEmphasized = base.titleLargeEmphasized.copy(fontFamily = titleLargeFamily),
        titleMediumEmphasized = base.titleMediumEmphasized.copy(fontFamily = titleFamily),
        titleSmallEmphasized = base.titleSmallEmphasized.copy(fontFamily = titleFamily),
        bodyLargeEmphasized = base.bodyLargeEmphasized.copy(fontFamily = bodyFamily),
        bodyMediumEmphasized = base.bodyMediumEmphasized.copy(fontFamily = bodyFamily),
        bodySmallEmphasized = base.bodySmallEmphasized.copy(fontFamily = bodyFamily),
        labelLargeEmphasized = base.labelLargeEmphasized.copy(fontFamily = labelFamily),
        labelMediumEmphasized = base.labelMediumEmphasized.copy(fontFamily = labelFamily),
        labelSmallEmphasized = base.labelSmallEmphasized.copy(fontFamily = labelFamily),
    )
