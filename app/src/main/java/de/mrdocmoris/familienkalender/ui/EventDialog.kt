package de.mrdocmoris.familienkalender.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.mrdocmoris.familienkalender.data.CalendarEvent
import de.mrdocmoris.familienkalender.data.toTimestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FMT = DateTimeFormatter.ofPattern("EE, dd.MM.yyyy", Locale.GERMAN)
private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

/** Termin anlegen (initial == null) oder bearbeiten. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDialog(
    initial: CalendarEvent?,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var allDay by remember { mutableStateOf(initial?.allDay ?: false) }
    var date by remember { mutableStateOf(initial?.startLocal?.toLocalDate() ?: defaultDate) }
    var startTime by remember {
        mutableStateOf(initial?.startLocal?.toLocalTime() ?: LocalTime.of((LocalTime.now().hour + 1).coerceAtMost(22), 0))
    }
    var endTime by remember { mutableStateOf(initial?.endLocal?.toLocalTime() ?: startTime.plusHours(1)) }

    var pickDate by remember { mutableStateOf(false) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }

    val timeValid = allDay || !endTime.isBefore(startTime)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Neuer Termin" else "Termin bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Titel") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(date.format(DATE_FMT))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ganztägig", modifier = Modifier.weight(1f))
                    Switch(checked = allDay, onCheckedChange = { allDay = it })
                }
                if (!allDay) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pickStart = true }, modifier = Modifier.weight(1f)) {
                            Text("Von ${startTime.format(TIME_FMT)}")
                        }
                        OutlinedButton(onClick = { pickEnd = true }, modifier = Modifier.weight(1f)) {
                            Text("Bis ${endTime.format(TIME_FMT)}")
                        }
                    }
                    if (!timeValid) {
                        Text("Ende liegt vor dem Beginn", color = MaterialTheme.colorScheme.error)
                    }
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Notiz (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && timeValid,
                onClick = {
                    val start = if (allDay) date.atStartOfDay() else date.atTime(startTime)
                    val end = if (allDay) date.atTime(23, 59) else date.atTime(endTime)
                    onSave(
                        (initial ?: CalendarEvent()).copy(
                            title = title.trim(),
                            note = note.trim(),
                            allDay = allDay,
                            start = start.toTimestamp(),
                            end = end.toTimestamp(),
                        )
                    )
                },
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Abbrechen") } },
        ) { DatePicker(state = state) }
    }

    if (pickStart) {
        TimeDialog(startTime, onDismiss = { pickStart = false }) { t ->
            val duration = java.time.Duration.between(startTime, endTime)
            startTime = t
            // Ende mitverschieben, damit die Dauer gleich bleibt
            endTime = t.plus(duration).let { if (it.isBefore(t)) LocalTime.of(23, 59) else it }
            pickStart = false
        }
    }
    if (pickEnd) {
        TimeDialog(endTime, onDismiss = { pickEnd = false }) { t -> endTime = t; pickEnd = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
