package eu.hxreborn.discoveradsfilter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import eu.hxreborn.discoveradsfilter.R

private val GoogleSansFlex =
    FontFamily(
        listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
            Font(
                resId = R.font.google_sans_flex,
                weight = weight,
                variationSettings =
                    FontVariation.Settings(
                        FontVariation.weight(weight.weight),
                        FontVariation.Setting("ROND", 0f),
                    ),
            )
        },
    )

val DiscoverAdsFilterTypography = Typography(fontFamily = GoogleSansFlex)
