package de.mrdocmoris.familienkalender.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Erster Start: Familie anlegen (du) oder per Code beitreten (deine Frau). */
@Composable
fun FamilyScreen(vm: MainViewModel, uid: String) {
    var familyName by rememberSaveable { mutableStateOf("Familie") }
    var code by rememberSaveable { mutableStateOf("") }
    val busy by vm.busy.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().padding(24.dp).imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("Neue Familie anlegen", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = familyName, onValueChange = { familyName = it },
            label = { Text("Name, z. B. Familie Müller") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { vm.createFamily(uid, familyName) },
            enabled = familyName.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Familie anlegen") }

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        Text("Oder einer Familie beitreten", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = code, onValueChange = { code = it.uppercase().take(6) },
            label = { Text("Einladungscode (6 Zeichen)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = { vm.joinFamily(uid, code) },
            enabled = code.length == 6 && !busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Beitreten") }

        TextButton(onClick = vm::signOut, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Abmelden")
        }
    }
}
