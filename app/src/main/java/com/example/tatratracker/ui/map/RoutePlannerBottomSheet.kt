package com.example.tatratracker.ui.map

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp


/*
 * =========================================================
 * POZYCJE PANELU
 * =========================================================
 */

enum class RoutePlannerSheetLevel(
    val heightFraction: Float
) {

    /*
     * Mały panel.
     *
     * Dobry kiedy chcemy przede wszystkim
     * oglądać mapę.
     */
    COMPACT(
        0.32f
    ),

    /*
     * Standardowy widok.
     */
    MEDIUM(
        0.60f
    ),

    /*
     * Prawie cały ekran.
     *
     * Przydatne np. do:
     *
     * - wyszukiwarki,
     * - wielu przystanków,
     * - profilu wysokościowego.
     */
    EXPANDED(
        0.90f
    )
}


/*
 * =========================================================
 * BOTTOM SHEET
 * =========================================================
 */

@Composable
internal fun RoutePlannerBottomSheet(
    level: RoutePlannerSheetLevel,
    onLevelChange: (
        RoutePlannerSheetLevel
    ) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {

    /*
     * =====================================================
     * PŁYNNA ZMIANA WYSOKOŚCI
     * =====================================================
     */

    val animatedHeightFraction by
    animateFloatAsState(

        targetValue =
            level.heightFraction,

        animationSpec =
            tween(
                durationMillis = 220
            ),

        label =
            "routePlannerHeight"
    )


    /*
     * =====================================================
     * PRÓG PRZECIĄGNIĘCIA
     * =====================================================
     */

    val density =
        LocalDensity.current

    val dragThreshold =
        with(
            density
        ) {

            55.dp.toPx()
        }


    /*
     * =====================================================
     * SUMA RUCHU PALCA
     * =====================================================
     */

    var totalDrag by
    remember {

        mutableStateOf(
            0f
        )
    }


    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .fillMaxHeight(
                    animatedHeightFraction
                )
    ) {

        /*
         * =================================================
         * WŁAŚCIWY PANEL
         * =================================================
         */

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {

            content()
        }


        /*
         * =================================================
         * OBSZAR PRZECIĄGANIA
         * =================================================
         *
         * Znajduje się tylko na samej górze,
         * dokładnie nad uchwytem.
         *
         * Dzięki temu przewijanie zawartości
         * planera nie walczy z przeciąganiem
         * całego panelu.
         */

        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.TopCenter
                    )
                    .fillMaxWidth()
                    .height(
                        24.dp
                    )
                    .pointerInput(
                        level
                    ) {

                        detectVerticalDragGestures(

                            onDragStart = {

                                totalDrag =
                                    0f
                            },

                            onVerticalDrag = {
                                    _,
                                    dragAmount ->

                                totalDrag +=
                                    dragAmount
                            },

                            onDragEnd = {

                                /*
                                 * =================================
                                 * RUCH W GÓRĘ
                                 * =================================
                                 *
                                 * dragAmount < 0
                                 */

                                if (
                                    totalDrag <
                                    -dragThreshold
                                ) {

                                    when (
                                        level
                                    ) {

                                        RoutePlannerSheetLevel.COMPACT -> {

                                            onLevelChange(
                                                RoutePlannerSheetLevel.MEDIUM
                                            )
                                        }

                                        RoutePlannerSheetLevel.MEDIUM -> {

                                            onLevelChange(
                                                RoutePlannerSheetLevel.EXPANDED
                                            )
                                        }

                                        RoutePlannerSheetLevel.EXPANDED -> {

                                            /*
                                             * Już jesteśmy
                                             * maksymalnie wysoko.
                                             */
                                        }
                                    }
                                }

                                /*
                                 * =================================
                                 * RUCH W DÓŁ
                                 * =================================
                                 *
                                 * dragAmount > 0
                                 */

                                else if (
                                    totalDrag >
                                    dragThreshold
                                ) {

                                    when (
                                        level
                                    ) {

                                        RoutePlannerSheetLevel.EXPANDED -> {

                                            onLevelChange(
                                                RoutePlannerSheetLevel.MEDIUM
                                            )
                                        }

                                        RoutePlannerSheetLevel.MEDIUM -> {

                                            onLevelChange(
                                                RoutePlannerSheetLevel.COMPACT
                                            )
                                        }

                                        RoutePlannerSheetLevel.COMPACT -> {

                                            /*
                                             * Nie zamykamy panelu
                                             * przeciągnięciem.
                                             *
                                             * Do zamknięcia nadal
                                             * służy X.
                                             */
                                        }
                                    }
                                }

                                totalDrag =
                                    0f
                            },

                            onDragCancel = {

                                totalDrag =
                                    0f
                            }
                        )
                    }
        )
    }
}