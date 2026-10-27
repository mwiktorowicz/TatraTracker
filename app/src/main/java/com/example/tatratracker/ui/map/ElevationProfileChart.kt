package com.example.tatratracker.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.tatratracker.ui.theme.TatraGreen
import kotlin.math.abs


@Composable
internal fun ElevationProfileChart(
    profile: RouteElevationProfile,
    selectedSample: RouteElevationSample?,
    onSampleSelected: (
        RouteElevationSample?
    ) -> Unit,
    modifier: Modifier = Modifier
) {

    val samples =
        profile.samples

    if (
        samples.size < 2
    ) {

        return
    }

    /*
     * =====================================================
     * WYBÓR PRÓBKI NA PODSTAWIE X
     * =====================================================
     */

    fun selectSample(
        x: Float,
        width: Float
    ) {

        if (
            width <= 0f
        ) {

            return
        }

        val fraction =
            (
                    x /
                            width
                    )
                .coerceIn(
                    0f,
                    1f
                )

        val maxDistance =
            samples
                .last()
                .distanceMeters
                .coerceAtLeast(
                    1.0
                )

        val requestedDistance =
            maxDistance *
                    fraction

        val nearest =
            samples.minByOrNull {
                    sample ->

                abs(
                    sample.distanceMeters -
                            requestedDistance
                )
            }

        onSampleSelected(
            nearest
        )
    }


    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(
                    145.dp
                )
                .padding(
                    horizontal = 4.dp,
                    vertical = 8.dp
                )
    ) {

        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        125.dp
                    )

                    /*
                     * =========================================
                     * TAP
                     * =========================================
                     */

                    .pointerInput(
                        samples
                    ) {

                        detectTapGestures {
                                offset ->

                            selectSample(
                                x =
                                    offset.x,

                                width =
                                    size.width.toFloat()
                            )
                        }
                    }

                    /*
                     * =========================================
                     * PRZESUWANIE PO WYKRESIE
                     * =========================================
                     */

                    .pointerInput(
                        samples
                    ) {

                        detectHorizontalDragGestures(

                            onDragStart = {
                                    offset ->

                                selectSample(
                                    x =
                                        offset.x,

                                    width =
                                        size.width.toFloat()
                                )
                            },

                            onHorizontalDrag = {
                                    change,
                                    _ ->

                                selectSample(
                                    x =
                                        change.position.x,

                                    width =
                                        size.width.toFloat()
                                )
                            }
                        )
                    }
        ) {

            /*
             * =================================================
             * ZAKRES WYSOKOŚCI
             * =================================================
             */

            val minElevation =
                samples.minOf {
                    it.elevationMeters
                }

            val maxElevation =
                samples.maxOf {
                    it.elevationMeters
                }

            val elevationRange =
                (
                        maxElevation -
                                minElevation
                        )
                    .coerceAtLeast(
                        1.0
                    )

            val maxDistance =
                samples
                    .last()
                    .distanceMeters
                    .coerceAtLeast(
                        1.0
                    )

            /*
             * =================================================
             * SIATKA
             * =================================================
             */

            val gridColor =
                Color(
                    0xFFDCE3D7
                )

            for (
            line in 0..3
            ) {

                val y =
                    size.height *
                            line /
                            3f

                drawLine(

                    color =
                        gridColor,

                    start =
                        Offset(
                            0f,
                            y
                        ),

                    end =
                        Offset(
                            size.width,
                            y
                        ),

                    strokeWidth =
                        1.dp.toPx()
                )
            }

            /*
             * =================================================
             * PROFIL
             * =================================================
             */

            val path =
                Path()

            samples.forEachIndexed {
                    index,
                    sample ->

                val x =
                    (
                            sample.distanceMeters /
                                    maxDistance
                            )
                        .toFloat() *
                            size.width

                val normalizedHeight =
                    (
                            (
                                    sample.elevationMeters -
                                            minElevation
                                    ) /
                                    elevationRange
                            )
                        .toFloat()

                val y =
                    size.height -
                            (
                                    normalizedHeight *
                                            size.height *
                                            0.88f
                                    ) -
                            size.height *
                            0.06f

                if (
                    index == 0
                ) {

                    path.moveTo(
                        x,
                        y
                    )

                } else {

                    path.lineTo(
                        x,
                        y
                    )
                }
            }

            drawPath(

                path =
                    path,

                color =
                    TatraGreen,

                style =
                    Stroke(
                        width =
                            3.dp.toPx(),

                        cap =
                            StrokeCap.Round
                    )
            )

            /*
             * =================================================
             * AKTUALNIE WYBRANY PUNKT
             * =================================================
             */

            if (
                selectedSample != null
            ) {

                val selectedX =
                    (
                            selectedSample.distanceMeters /
                                    maxDistance
                            )
                        .toFloat() *
                            size.width

                val selectedNormalizedHeight =
                    (
                            (
                                    selectedSample.elevationMeters -
                                            minElevation
                                    ) /
                                    elevationRange
                            )
                        .toFloat()

                val selectedY =
                    size.height -
                            (
                                    selectedNormalizedHeight *
                                            size.height *
                                            0.88f
                                    ) -
                            size.height *
                            0.06f

                /*
                 * Pionowa kreska.
                 */
                drawLine(

                    color =
                        Color(
                            0xFF5B6264
                        ),

                    start =
                        Offset(
                            selectedX,
                            0f
                        ),

                    end =
                        Offset(
                            selectedX,
                            size.height
                        ),

                    strokeWidth =
                        1.5.dp.toPx()
                )

                /*
                 * Biała obwódka punktu.
                 */
                drawCircle(

                    color =
                        Color.White,

                    radius =
                        7.dp.toPx(),

                    center =
                        Offset(
                            selectedX,
                            selectedY
                        )
                )

                /*
                 * Zielony punkt.
                 */
                drawCircle(

                    color =
                        TatraGreen,

                    radius =
                        4.5.dp.toPx(),

                    center =
                        Offset(
                            selectedX,
                            selectedY
                        )
                )
            }
        }
    }
}