package com.example.carelink.ui.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carelink.data.SessionManager
import com.example.carelink.util.NotificationsHelper

// stable identifiers for the three theme choices
private const val THEME_SYSTEM = "system"
private const val THEME_LIGHT = "light"
private const val THEME_DARK = "dark"

@Composable
fun SettingsScreen(
    sessionManager: SessionManager,
    onThemeChanged: (Boolean?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val initial = when (sessionManager.getDarkModeOverride()) {
        true -> THEME_DARK
        false -> THEME_LIGHT
        null -> THEME_SYSTEM
    }
    var selected by remember { mutableStateOf(initial) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("< Back") }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                ThemeOption(
                    label = "Follow system",
                    selected = selected == THEME_SYSTEM,
                    onClick = {
                        selected = THEME_SYSTEM
                        sessionManager.clearThemeOverride()
                        onThemeChanged(null)
                    }
                )
                ThemeOption(
                    label = "Light",
                    selected = selected == THEME_LIGHT,
                    onClick = {
                        selected = THEME_LIGHT
                        sessionManager.setDarkMode(false)
                        onThemeChanged(false)
                    }
                )
                ThemeOption(
                    label = "Dark",
                    selected = selected == THEME_DARK,
                    onClick = {
                        selected = THEME_DARK
                        sessionManager.setDarkMode(true)
                        onThemeChanged(true)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "CareLink can send you a confirmation when a welfare request is submitted.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { NotificationsHelper.sendSubmissionConfirmation(context, "Test request") }
                ) {
                    Text("Send a test notification")
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(text = label, modifier = Modifier.padding(start = 4.dp))
    }
}
