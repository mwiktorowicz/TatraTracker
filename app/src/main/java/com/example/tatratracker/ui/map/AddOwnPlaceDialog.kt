package com.example.tatratracker.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tatratracker.ui.theme.TatraTextMuted

@Composable
internal fun AddOwnPlaceDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Dodaj własny punkt")
        },
        text = {
            Column {
                Text(
                    text = "Docelowo punkt zostanie zapisany w centrum aktualnego widoku mapy. Później dodamy również wybór dokładnej pozycji przez dotknięcie mapy.",
                    color = TatraTextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    singleLine = true,
                    label = {
                        Text("Nazwa punktu")
                    },
                    placeholder = {
                        Text("Np. miejsce na odpoczynek")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = name.trim()
                    if (clean.isNotEmpty()) {
                        onSave(clean)
                    }
                },
                enabled = name.trim().isNotEmpty()
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
