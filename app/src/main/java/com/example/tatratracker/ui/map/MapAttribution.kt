package com.example.tatratracker.ui.map

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tatratracker.R
import com.example.tatratracker.ui.theme.TatraDark
import org.maplibre.android.maps.MapLibreMap
import kotlin.math.roundToInt

/*
 * Logo i autorzy danych na mapie głównej, jednakowo na obu podkładach.
 * Plan Free MapTilera wymaga logo MapTiler i stale widocznego napisu
 * „© MapTiler © OpenStreetMap contributors” (maptiler.com/copyright).
 * Pełną listę źródeł, z dostawcami zdjęć, pokazuje przycisk „i” MapLibre.
 */

internal const val MAP_DATA_ATTRIBUTION = "© MapTiler © OpenStreetMap contributors"

// Logo MapTiler stoi tam, gdzie MapLibre rysuje swoje (lewy dolny róg, 4 dp od krawędzi).
private const val LOGO_MARGIN_DP = 4
private const val LOGO_WIDTH_DP = 67
private const val LOGO_HEIGHT_DP = 20

@Composable
internal fun MapTilerLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_maptiler_logo),
        contentDescription = "MapTiler",
        modifier = modifier
            .padding(start = LOGO_MARGIN_DP.dp, bottom = LOGO_MARGIN_DP.dp)
            .size(width = LOGO_WIDTH_DP.dp, height = LOGO_HEIGHT_DP.dp)
    )
}

@Composable
internal fun MapDataAttribution(modifier: Modifier = Modifier) {
    Text(
        text = MAP_DATA_ATTRIBUTION,
        modifier = modifier
            .background(
                color = Color.White.copy(alpha = 0.75f),
                shape = RoundedCornerShape(topStart = 6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = TatraDark,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        maxLines = 1
    )
}

/**
 * Wyłącza logo MapLibre (zastępuje je [MapTilerLogo]) i ustawia przycisk „i” tuż za logo MapTiler.
 */
internal fun MapLibreMap.useMapTilerLogo(context: Context) {
    val density = context.resources.displayMetrics.density
    fun px(dp: Int): Int = (dp * density).roundToInt()

    uiSettings.isLogoEnabled = false
    uiSettings.setAttributionMargins(
        px(LOGO_MARGIN_DP + LOGO_WIDTH_DP + 4),
        px(LOGO_MARGIN_DP),
        px(LOGO_MARGIN_DP),
        px(LOGO_MARGIN_DP)
    )
}
