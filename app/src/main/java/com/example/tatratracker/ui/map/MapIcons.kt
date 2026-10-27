package com.example.tatratracker.ui.map

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.tatratracker.R
import org.maplibre.android.maps.Style

internal const val HUT_ICON_ID =
    "tt-hut-icon"

internal const val PEAK_ICON_ID =
    "tt-peak-icon"

internal const val CAVE_ICON_ID =
    "tt-cave-icon"

internal const val WATER_ICON_ID =
    "tt-water-icon"


internal fun registerMapIcons(
    context: Context,
    style: Style
) {

    addDrawableToStyle(
        context = context,
        style = style,
        imageId = HUT_ICON_ID,
        drawableId = R.drawable.ic_map_hut
    )

    addDrawableToStyle(
        context = context,
        style = style,
        imageId = PEAK_ICON_ID,
        drawableId = R.drawable.ic_map_peak
    )

    addDrawableToStyle(
        context = context,
        style = style,
        imageId = CAVE_ICON_ID,
        drawableId = R.drawable.ic_map_cave
    )

    addDrawableToStyle(
        context = context,
        style = style,
        imageId = WATER_ICON_ID,
        drawableId = R.drawable.ic_map_water
    )
}


private fun addDrawableToStyle(
    context: Context,
    style: Style,
    imageId: String,
    drawableId: Int
) {

    if (
        style.getImage(
            imageId
        ) != null
    ) {
        return
    }

    val drawable =
        ContextCompat.getDrawable(
            context,
            drawableId
        )
            ?: return

    style.addImage(
        imageId,
        drawable
    )
}