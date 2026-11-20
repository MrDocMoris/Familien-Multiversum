package de.mrdocmoris.familienkalender.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun App(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize()) {
        when (val s = state) {
            AppState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            AppState.LoggedOut -> AuthScreen(vm)
            is AppState.NeedsFamily -> FamilyScreen(vm, s.uid)
            is AppState.Ready -> CalendarScreen(vm)
        }
    }

    error?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.error.value = null },
            confirmButton = { TextButton(onClick = { vm.error.value = null }) { Text("OK") } },
            title = { Text("Fehler") },
            text = { Text(msg) },
        )
    }
}
