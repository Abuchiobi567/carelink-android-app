package com.example.carelink.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.carelink.data.models.User

// dropdown that lets a coordinator pick a Support Worker.
// reused on the assign screen and in the bulk-assign panel.
@Composable
fun WorkerDropdown(
    workers: List<User>,
    selected: User?,
    onSelected: (User) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = selected?.name ?: "Choose a Support Worker")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (workers.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No workers available") },
                    onClick = { expanded = false }
                )
            }
            workers.forEach { worker ->
                DropdownMenuItem(
                    text = { Text(worker.name) },
                    onClick = {
                        onSelected(worker)
                        expanded = false
                    }
                )
            }
        }
    }
}