package com.example.tatratracker.ui

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tatratracker.ui.theme.TatraTextMuted

/**
 * Edycja nazwy i notatki aktywności. Daty i danych z GPS nie edytujemy.
 * Zapis robi wywołujący (ViewModel); okno tylko zbiera wartości.
 */
@Composable
internal fun ActivityEditDialog(
    initialName: String,
    initialNote: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, note: String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var note by rememberSaveable { mutableStateOf(initialNote.orEmpty()) }

    val canSave = name.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edytuj aktywność")
        },
        text = {
            Column {
                Text(
                    text = "Możesz zmienić nazwę i dodać notatkę. Data i dane z GPS pozostają bez zmian.",
                    color = TatraTextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true,
                    isError = !canSave,
                    label = { Text("Nazwa") },
                    supportingText = if (canSave) null else {
                        { Text("Nazwa nie może być pusta.") }
                    }
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    minLines = 3,
                    maxLines = 6,
                    label = { Text("Notatka (opcjonalnie)") },
                    placeholder = { Text("Np. warunki na szlaku, z kim szliśmy") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, note) },
                enabled = canSave
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
