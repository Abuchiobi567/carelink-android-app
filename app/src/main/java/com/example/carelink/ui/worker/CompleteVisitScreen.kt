package com.example.carelink.ui.worker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carelink.data.AuditRepository
import com.example.carelink.data.RequestRepository
import com.example.carelink.data.SessionManager
import com.example.carelink.data.VisitRepository
import com.example.carelink.data.models.AuditAction
import com.example.carelink.data.models.WelfareRequest
import com.example.carelink.ui.components.DeadlineChip
import com.example.carelink.util.DateTimeFormat

@Composable
fun CompleteVisitScreen(
    requestId: Long,
    requestRepository: RequestRepository,
    visitRepository: VisitRepository,
    auditRepository: AuditRepository,
    sessionManager: SessionManager,
    onCompleted: () -> Unit,
    onBack: () -> Unit
) {
    var request by remember { mutableStateOf<WelfareRequest?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf("") }
    var startedAtMillis by rememberSaveable { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(requestId) {
        request = withContext(Dispatchers.IO) { requestRepository.findById(requestId) }
        loaded = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("< Back") }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Complete Visit",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        val req = request
        if (req == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = if (loaded) "Visit not found" else "Loading...")
        } else {

        Spacer(modifier = Modifier.height(16.dp))

        SummaryCard(req)

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Visit timer (optional)")
        TimerSection(
            startedAtMillis = startedAtMillis,
            onStart = { startedAtMillis = System.currentTimeMillis() },
            onClear = { startedAtMillis = 0L }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Outcome notes")
        OutlinedTextField(
            value = notes,
            onValueChange = {
                notes = it
                errorMessage = null
            },
            label = { Text("What happened during the visit?") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Notes are required before you can mark the visit completed",
            style = MaterialTheme.typography.labelMedium
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // mandatory notes gate - the chosen extra feature.
        // disable the button until notes are non-blank.
        val notesReady = notes.trim().isNotEmpty()
        Button(
            onClick = {
                errorMessage = null
                if (!notesReady) {
                    errorMessage = "Please add outcome notes first"
                    return@Button
                }
                val capturedStart = startedAtMillis
                scope.launch {
                    val now = System.currentTimeMillis()
                    val workerId = sessionManager.getUserId()
                    val visitId = withContext(Dispatchers.IO) {
                        visitRepository.insertVisit(
                            requestId = requestId,
                            workerId = workerId,
                            outcomeNotes = notes,
                            startedAt = if (capturedStart > 0L) capturedStart else null,
                            completedAt = now
                        )
                    }
                    if (visitId == -1L) {
                        errorMessage = "Could not save the visit"
                        return@launch
                    }
                    val statusOk = withContext(Dispatchers.IO) {
                        requestRepository.markVisitCompleted(requestId, now)
                    }
                    if (!statusOk) {
                        errorMessage = "This visit can no longer be marked completed"
                        return@launch
                    }
                    val durationDetails = if (capturedStart > 0L) {
                        val minutes = (now - capturedStart) / 60000L
                        "Visit duration: $minutes ${if (minutes == 1L) "minute" else "minutes"}"
                    } else ""
                    withContext(Dispatchers.IO) {
                        auditRepository.log(
                            actorUserId = workerId,
                            action = AuditAction.VISIT_COMPLETED,
                            targetRequestId = requestId,
                            details = durationDetails
                        )
                    }
                    onCompleted()
                }
            },
            enabled = notesReady,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Mark Visit Completed")
        }

        Spacer(modifier = Modifier.height(24.dp))
        } // end else (req != null)
    }
}

@Composable
private fun TimerSection(
    startedAtMillis: Long,
    onStart: () -> Unit,
    onClear: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (startedAtMillis <= 0L) {
                Text(
                    text = "Tap Start when you arrive at the visit",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onStart) {
                    Text("Start visit timer")
                }
            } else {
                Text(
                    text = "Visit started at ${DateTimeFormat.dateTime(startedAtMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Duration will be recorded when you mark the visit completed",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onClear) {
                    Text("Reset timer")
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(req: WelfareRequest) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = req.title.ifBlank { "(Untitled visit)" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (req.address.isNotBlank()) {
                Text(text = req.address, style = MaterialTheme.typography.bodyMedium)
            }
            if (req.description.isNotBlank()) {
                Text(text = req.description, style = MaterialTheme.typography.bodyMedium)
            }
            if (req.internalNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Coordinator notes",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = req.internalNotes, style = MaterialTheme.typography.bodyMedium)
            }
            if (req.deadlineMillis != null) {
                Spacer(modifier = Modifier.height(8.dp))
                DeadlineChip(deadlineMillis = req.deadlineMillis)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}