package com.example.tatratracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.tatratracker.ui.components.TatraBadgeStyle
import com.example.tatratracker.ui.components.TatraCard
import com.example.tatratracker.ui.components.TatraFeatureDialog
import com.example.tatratracker.ui.components.TatraListRow
import com.example.tatratracker.ui.components.TatraPrimaryButton
import com.example.tatratracker.ui.components.TatraSecondaryButton
import com.example.tatratracker.ui.components.TatraSectionHeader
import com.example.tatratracker.ui.components.TatraStatusBadge
import com.example.tatratracker.ui.components.TatraSubScreenHeader
import com.example.tatratracker.ui.theme.TatraBackground
import com.example.tatratracker.ui.theme.TatraDark
import com.example.tatratracker.ui.theme.TatraGreen
import com.example.tatratracker.ui.theme.TatraGreenVerySoft
import com.example.tatratracker.ui.theme.TatraSpacing
import com.example.tatratracker.ui.theme.TatraTextMuted

@Composable
fun BackupScreen(
    onBack: () -> Unit
) {
    var action by remember {
        mutableStateOf<String?>(null)
    }

    action?.let { title ->
        TatraFeatureDialog(
            title = title,
            message = "Mechanizm tworzenia i przywracania archiwum podepniemy po zakończeniu zmian w strukturze danych. Ekran i wszystkie akcje są już przygotowane.",
            onDismiss = {
                action = null
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
            title = "Kopia zapasowa",
            subtitle = "Zabezpiecz lokalne dane TatraTracker",
            onBack = onBack
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TatraSpacing.lg)
        ) {
            TatraCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = TatraGreenVerySoft
            ) {
                Column {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = TatraGreen
                    )

                    Text(
                        text = "Dane pozostają pod Twoją kontrolą",
                        color = TatraDark,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = TatraSpacing.sm)
                    )

                    Text(
                        text = "Kopia będzie obejmowała aktywności, punkty GPS, zapisane trasy, postęp na szlakach oraz ustawienia. Nie będzie automatycznie wysyłana na zewnętrzny serwer.",
                        color = TatraTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = TatraSpacing.xxs)
                    )
                }
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Stan kopii")

            TatraCard(modifier = Modifier.fillMaxWidth()) {
                TatraListRow(
                    title = "Ostatnia kopia",
                    subtitle = "Nie utworzono jeszcze kopii zapasowej",
                    icon = Icons.Outlined.Backup,
                    badge = {
                        TatraStatusBadge(
                            text = "Brak",
                            style = TatraBadgeStyle.Neutral
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
            TatraSectionHeader(title = "Kopia lokalna")

            TatraPrimaryButton(
                onClick = {
                    action = "Utwórz kopię zapasową"
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Backup,
                    contentDescription = null
                )

                Text(
                    text = "Utwórz kopię",
                    modifier = Modifier.padding(start = TatraSpacing.xs)
                )
            }

            Spacer(modifier = Modifier.height(TatraSpacing.sm))

            TatraSecondaryButton(
                onClick = {
                    action = "Przywróć kopię zapasową"
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Backup,
                    contentDescription = null
                )

                Text(
                    text = "Przywróć z pliku",
                    modifier = Modifier.padding(start = TatraSpacing.xs)
                )
            }

            Spacer(modifier = Modifier.height(TatraSpacing.xl))
        }
    }
}