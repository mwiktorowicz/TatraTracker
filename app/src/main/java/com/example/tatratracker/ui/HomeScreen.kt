package com.example.tatratracker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tatratracker.R
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraWhite

@Composable
fun HomeScreen(
    onNavigateToImport: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToHistory: () -> Unit
) {

    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {

        /*
         * =====================================================
         * ZDJĘCIE TŁA
         * =====================================================
         */

        Image(
            painter =
                painterResource(
                    id = R.drawable.tatra_home_bg
                ),

            contentDescription =
                "Tatry",

            modifier =
                Modifier.fillMaxSize(),

            contentScale =
                ContentScale.Crop
        )

        /*
         * =====================================================
         * CIEMNY FILTR
         * =====================================================
         *
         * Dzięki temu białe logo i napisy
         * będą wyglądały podobnie do makiety.
         */

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(
                            alpha = 0.20f
                        )
                    )
        )

        /*
         * Mocniejsze przyciemnienie dołu.
         */
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(
                                        alpha = 0.20f
                                    ),
                                    Color.Black.copy(
                                        alpha = 0.72f
                                    ),
                                    Color.Black.copy(
                                        alpha = 0.92f
                                    )
                                )
                        )
                    )
        )

        /*
         * Delikatne przyciemnienie góry.
         */
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(
                                        alpha = 0.40f
                                    ),
                                    Color.Transparent,
                                    Color.Transparent
                                )
                        )
                    )
        )

        /*
         * =====================================================
         * LOGO + NAZWA
         * =====================================================
         */

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(
                        Alignment.Center
                    )
                    .padding(
                        horizontal = 28.dp
                    )
                    .padding(
                        bottom = 70.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Image(
                painter =
                    painterResource(
                        id = R.drawable.ic_tatra_logo
                    ),

                contentDescription =
                    "Logo TatraTracker",

                modifier =
                    Modifier
                        .fillMaxWidth(
                            0.34f
                        )
                        .height(
                            76.dp
                        )
            )

            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )

            Text(
                text =
                    "TatraTracker",

                color =
                    TatraWhite,

                fontSize =
                    34.sp,

                fontWeight =
                    FontWeight.Bold,

                letterSpacing =
                    (-0.5).sp,

                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            Text(
                text =
                    "Twoje wędrówki. Twoja historia.",

                color =
                    Color.White.copy(
                        alpha = 0.88f
                    ),

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,

                textAlign =
                    TextAlign.Center
            )
        }

        /*
         * =====================================================
         * PRZYCISKI NA DOLE
         * =====================================================
         */

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(
                        Alignment.BottomCenter
                    )
                    .padding(
                        horizontal = 22.dp
                    )
                    .padding(
                        bottom = 22.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            /*
             * Główny zielony przycisk.
             *
             * Na razie prowadzi do mapy.
             * Później podepniemy tutaj
             * planowanie trasy.
             */
            Button(
                onClick =
                    onNavigateToMap,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            50.dp
                        ),

                shape =
                    RoundedCornerShape(
                        9.dp
                    ),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            TatraGreen,
                        contentColor =
                            Color.White
                    )
            ) {

                Text(
                    text =
                        "Rozpocznij",

                    fontSize =
                        15.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )
            }

            /*
             * Drugi przycisk z makiety.
             */
            Button(
                onClick =
                    onNavigateToHistory,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            50.dp
                        )
                        .clip(
                            RoundedCornerShape(
                                9.dp
                            )
                        )
                        .border(
                            width =
                                1.dp,

                            color =
                                Color.White.copy(
                                    alpha = 0.42f
                                ),

                            shape =
                                RoundedCornerShape(
                                    9.dp
                                )
                        ),

                shape =
                    RoundedCornerShape(
                        9.dp
                    ),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color.Black.copy(
                                alpha = 0.28f
                            ),

                        contentColor =
                            Color.White
                    )
            ) {

                Text(
                    text =
                        "Moje trasy",

                    fontSize =
                        15.sp,

                    fontWeight =
                        FontWeight.Medium
                )
            }
        }
    }
}