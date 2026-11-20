package de.mrdocmoris.familienkalender.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.mrdocmoris.familienkalender.data.CalendarEvent
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val GERMAN = Locale.GERMAN
private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_TITLE = DateTimeFormatter.ofPattern("EEEE, d. MMMM", GERMAN)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: MainViewModel) {
    val month by vm.month.collectAsStateWithLifecycle()
    val selected by vm.selectedDate.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val family by vm.family.collectAsStateWithLifecycle()

    var menuOpen by remember { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CalendarEvent?>(null) }
    var creating by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<CalendarEvent?>(null) }

    val dayEvents = events.filter { it.isOn(selected) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(family?.name ?: "Kalender") },
                actions = {
                    TextButton(onClick = { vm.selectDate(LocalDate.now()) }) { Text("Heute") }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menü")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Einladungscode anzeigen") },
                            onClick = { menuOpen = false; showInvite = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Abmelden") },
                            onClick = { menuOpen = false; vm.signOut() },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Termin hinzufügen")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MonthHeader(month, onPrev = vm::previousMonth, onNext = vm::nextMonth)
            MonthGrid(
                month = month,
                selected = selected,
                hasEvents = { d -> events.any { it.isOn(d) } },
                onSelect = vm::selectDate,
            )
            HorizontalDivider()
            Text(
                selected.format(DAY_TITLE),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
            )
            if (dayEvents.isEmpty()) {
                Text(
                    "Keine Termine",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            LazyColumn(Modifier.fillMaxWidth()) {
                items(dayEvents, key = { it.id }) { ev ->
                    ListItem(
                        headlineContent = { Text(ev.title) },
                        supportingContent = {
                            val time = if (ev.allDay) "Ganztägig"
                            else "${ev.startLocal.format(TIME)} – ${ev.endLocal.format(TIME)}"
                            val who = if (ev.createdByName.isNotBlank()) " · ${ev.createdByName}" else ""
                            Text(time + who + if (ev.note.isNotBlank()) "\n${ev.note}" else "")
                        },
                        trailingContent = {
                            IconButton(onClick = { toDelete = ev }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Löschen")
                            }
                        },
                        modifier = Modifier.clickable { editing = ev },
                    )
                }
                item { Spacer(Modifier.height(88.dp)) } // Platz für den FAB
            }
        }
    }

    if (creating || editing != null) {
        EventDialog(
            initial = editing,
            defaultDate = selected,
            onDismiss = { creating = false; editing = null },
            onSave = { ev -> vm.saveEvent(ev); creating = false; editing = null },
        )
    }

    toDelete?.let { ev ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Termin löschen?") },
            text = { Text("„${ev.title}“ wird für alle gelöscht.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteEvent(ev); toDelete = null }) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Abbrechen") } },
        )
    }

    if (showInvite) {
        AlertDialog(
            onDismissRequest = { showInvite = false },
            title = { Text("Einladungscode") },
            text = {
                Column {
                    Text("Deine Frau registriert sich in der App und gibt diesen Code ein:")
                    Text(
                        family?.inviteCode ?: "…",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showInvite = false }) { Text("OK") } },
        )
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Vorheriger Monat")
        }
        Text(
            "${month.month.getDisplayName(TextStyle.FULL_STANDALONE, GERMAN)} ${month.year}",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Nächster Monat")
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    hasEvents: (LocalDate) -> Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    val weekdays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    val offset = month.atDay(1).dayOfWeek.value - 1 // Montag = 0
    val days = month.lengthOfMonth()
    val rows = (offset + days + 6) / 7
    val today = LocalDate.now()

    Column(Modifier.padding(horizontal = 8.dp)) {
        Row {
            weekdays.forEach {
                Text(
                    it, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                )
            }
        }
        for (r in 0 until rows) {
            Row {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1.1f), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..days) {
                            val date = month.atDay(dayNum)
                            DayCell(
                                day = dayNum,
                                isSelected = date == selected,
                                isToday = date == today,
                                hasEvents = hasEvents(date),
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, isSelected: Boolean, isToday: Boolean, hasEvents: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg = when {
        isSelected -> cs.primary
        isToday -> cs.primaryContainer
        else -> Color.Transparent
    }
    val fg = when {
        isSelected -> cs.onPrimary
        isToday -> cs.onPrimaryContainer
        else -> cs.onSurface
    }
    Column(
        Modifier.size(40.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("$day", color = fg, style = MaterialTheme.typography.bodyMedium)
        Box(
            Modifier.padding(top = 2.dp).size(5.dp).clip(CircleShape)
                .background(if (hasEvents) (if (isSelected) cs.onPrimary else cs.primary) else Color.Transparent)
        )
    }
}
