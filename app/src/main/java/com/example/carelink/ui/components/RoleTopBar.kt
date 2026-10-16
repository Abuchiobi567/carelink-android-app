package com.example.carelink.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

// shared top bar for every role's home screen.
// title + subtitle on the left, an overflow menu on the right.
@Composable
fun RoleTopBar(
    title: String,
    subtitle: String,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    onAuditLog: (() -> Unit)? = null
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Box {
            TextButton(onClick = { menuOpen = true }) { Text("Menu") }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                if (onAuditLog != null) {
                    DropdownMenuItem(
                        text = { Text("Audit log") },
                        onClick = {
                            menuOpen = false
                            onAuditLog()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        menuOpen = false
                        onSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sign out") },
                    onClick = {
                        menuOpen = false
                        onLogout()
                    }
                )
            }
        }
    }
}