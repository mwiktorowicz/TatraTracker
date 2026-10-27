package com.example.tatratracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraFeatureDialog
import com.example.tatratracker.ui.components.TatraPrimaryButton
import com.example.tatratracker.ui.components.TatraSecondaryButton
import com.example.tatratracker.ui.components.TatraSectionHeader
import com.example.tatratracker.ui.components.TatraStatCard
import com.example.tatratracker.ui.components.TatraSubScreenHeader
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted

@Composable
fun CompletedTrailsScreen(
    onBack: () -> Unit,
    onOpenMap: () -> Unit
) {
    var showMatchingInfo by remember {
        mutableStateOf(false)
    }

    if (showMatchingInfo) {
        TatraFeatureDialog(
            title = "Analiza ukończonych szlaków",
            message = "Automatyczne dopasowywanie zapisanych śladów GPS do sieci tatrzańskich szlaków i liczenie postępu jest w przygotowaniu.",
            onDismiss = {
                showMatchingInfo = false
            }
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(TatraBackground)
                .verticalScroll(rememberScrollState())
    ) {
        TatraSubScreenHeader(
            title = "Ukończone szlaki",
            subtitle = "Twój postęp w Tatrach",
            onBack = onBack
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TatraSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TatraSpacing.md)
        ) {
            TatraCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = TatraGreenVerySoft
            ) {
                Column {
                    Icon(
                        imageVector = Icons.Outlined.Route,
                        contentDescription = null,
                        tint = TatraGreen
                    )

                    Text(
                        text = "Postęp nie został jeszcze policzony",
                        color = TatraDark,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = TatraSpacing.sm)
                    )

                    Text(
                        text = "Po podłączeniu matchingu aktywności zobaczysz tutaj procent ukończenia całej sieci, przebyte kilometry oraz brakujące odcinki.",
                        color = TatraTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = TatraSpacing.xxs)
                    )
                }
            }

            TatraSectionHeader(
                title = "Postęp"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TatraSpacing.sm)
            ) {
                TatraStatCard(
                    label = "Ukończono",
                    value = "—%",
                    helper = "Sieci szlaków",
                    modifier = Modifier.weight(1f)
                )

                TatraStatCard(
                    label = "Przebyto",
                    value = "— km",
                    helper = "Po szlakach",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TatraSpacing.sm)
            ) {
                TatraStatCard(
                    label = "Odcinki",
                    value = "—",
                    helper = "Zaliczone",
                    modifier = Modifier.weight(1f)
                )

                TatraStatCard(
                    label = "Pozostało",
                    value = "— km",
                    helper = "Do przejścia",
                    modifier = Modifier.weight(1f)
                )
            }

            TatraPrimaryButton(
                onClick = onOpenMap
            ) {
                Icon(
                    imageVector = Icons.Outlined.Map,
                    contentDescription = null
                )

                Text(
                    text = "Pokaż szlaki na mapie",
                    modifier = Modifier.padding(start = TatraSpacing.xs)
                )
            }

            TatraSecondaryButton(
                onClick = {
                    showMatchingInfo = true
                }
            ) {
                Text("Przelicz postęp")
            }

            Spacer(
                modifier = Modifier.height(TatraSpacing.md)
            )
        }
    }
}