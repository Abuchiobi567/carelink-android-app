package com.example.carelink.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carelink.data.AuditRepository
import com.example.carelink.data.UserRepository
import com.example.carelink.data.models.AuditEntry
import com.example.carelink.util.DateTimeFormat

@Composable
fun AuditLogScreen(
    auditRepository: AuditRepository,
    userRepository: UserRepository,
    onBack: () -> Unit
) {
    var entries by remember { mutableStateOf<List<AuditEntry>>(emptyList()) }
    // simple in-memory cache so we don't hit the users table on every row
    var actorNames by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var actionFilter by remember { mutableStateOf<String?>(null) }
    var actorSearch by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val all = withContext(Dispatchers.IO) { auditRepository.findAll() }
        entries = all
        val ids = all.map { it.actorUserId }.toSet()
        val users = withContext(Dispatchers.IO) { userRepository.findByIds(ids) }
        val map = mutableMapOf<Long, String>()
        for (id in ids) {
            map[id] = users[id]?.name ?: "User #$id"
        }
        actorNames = map
    }

    val knownActions = remember(entries) {
        entries.map { it.action }.toSet().sorted()
    }

    val filtered = remember(entries, actorNames, actionFilter, actorSearch) {
        entries.filter { entry ->
            val matchesAction = actionFilter == null || entry.action == actionFilter
            val name = actorNames[entry.actorUserId] ?: ""
            val matchesActor = actorSearch.isBlank() ||
                name.contains(actorSearch, ignoreCase = true)
            matchesAction && matchesActor
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("< Back") }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Audit Log",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Every action recorded by the system, newest first",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionFilterMenu(
                current = actionFilter,
                options = listOf<String?>(null) + knownActions,
                onSelect = { actionFilter = it },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = actorSearch,
                onValueChange = { actorSearch = it },
                label = { Text("Search user") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No matching audit entries")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered, key = { it.id }) { entry ->
                    AuditRow(
                        entry = entry,
                        actorName = actorNames[entry.actorUserId] ?: "User #${entry.actorUserId}"
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionFilterMenu(
    current: String?,
    options: List<String?>,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Action: ${current ?: "All"}")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option ?: "All") },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AuditRow(entry: AuditEntry, actorName: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = entry.action,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$actorName  ·  ${DateTimeFormat.dateTime(entry.timestampMillis)}",
                style = MaterialTheme.typography.labelMedium
            )
            if (entry.targetRequestId != null) {
                Text(
                    text = "Request #${entry.targetRequestId}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (entry.details.isNotBlank()) {
                Text(
                    text = entry.details,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}