package com.example.carelink.ui.coordinator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carelink.data.AuditRepository
import com.example.carelink.data.RequestRepository
import com.example.carelink.data.SessionManager
import com.example.carelink.data.UserRepository
import com.example.carelink.data.models.AuditAction
import com.example.carelink.data.models.Priority
import com.example.carelink.data.models.RequestStatus
import com.example.carelink.data.models.Roles
import com.example.carelink.data.models.User
import com.example.carelink.data.models.WelfareRequest
import com.example.carelink.ui.components.WorkerDropdown
import com.example.carelink.util.DateTimeFormat
import com.example.carelink.util.MILLIS_PER_DAY

// quick-pick deadline options - simpler than a date picker and covers the
// usual response windows for welfare check-ins
private data class DeadlineChoice(val label: String, val days: Int)

private val deadlineChoices = listOf(
    DeadlineChoice("Same day (1 day)", 1),
    DeadlineChoice("3 days", 3),
    DeadlineChoice("1 week", 7),
    DeadlineChoice("2 weeks", 14)
)

@Composable
fun AssignTaskScreen(
    requestId: Long,
    requestRepository: RequestRepository,
    userRepository: UserRepository,
    auditRepository: AuditRepository,
    sessionManager: SessionManager,
    onAssigned: () -> Unit,
    onBack: () -> Unit
) {
    var request by remember { mutableStateOf<WelfareRequest?>(null) }
    var workers by remember { mutableStateOf<List<User>>(emptyList()) }
    var selectedWorker by remember { mutableStateOf<User?>(null) }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    // 0 means "not picked yet"
    var deadlineDays by remember { mutableStateOf(0) }
    var internalNotes by remember { mutableStateOf("") }
    var groupTag by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(requestId) {
        val req = withContext(Dispatchers.IO) { requestRepository.findById(requestId) }
        request = req
        workers = withContext(Dispatchers.IO) { userRepository.findByRole(Roles.WORKER) }
        if (req != null) {
            priority = req.priority
            internalNotes = req.internalNotes
            groupTag = req.groupTag
            if (req.status == RequestStatus.SUBMITTED) {
                val ok = withContext(Dispatchers.IO) {
                    requestRepository.markUnderReview(requestId, System.currentTimeMillis())
                }
                if (ok) {
                    withContext(Dispatchers.IO) {
                        auditRepository.log(
                            actorUserId = sessionManager.getUserId(),
                            action = AuditAction.REQUEST_UNDER_REVIEW,
                            targetRequestId = requestId,
                            details = "Status: ${RequestStatus.SUBMITTED} -> ${RequestStatus.UNDER_REVIEW}"
                        )
                    }
                    request = withContext(Dispatchers.IO) { requestRepository.findById(requestId) }
                }
            }
        }
        loaded = true
    }

    val req = request

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("< Back") }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Assign Welfare Request",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        if (req == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = if (loaded) "Request not found" else "Loading...")
        } else {

        Spacer(modifier = Modifier.height(16.dp))

        SummaryCard(req)

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Priority")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Priority.ALL.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    RadioButton(
                        selected = priority == option,
                        onClick = { priority = option }
                    )
                    Text(text = option.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Response deadline")
        Column {
            deadlineChoices.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    RadioButton(
                        selected = deadlineDays == option.days,
                        onClick = { deadlineDays = option.days }
                    )
                    Text(text = option.label, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Assign to Support Worker")
        WorkerDropdown(
            workers = workers,
            selected = selectedWorker,
            onSelected = { selectedWorker = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = internalNotes,
            onValueChange = { internalNotes = it },
            label = { Text("Internal notes (visible to coordinators only)") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = groupTag,
            onValueChange = { groupTag = it },
            label = { Text("Group tag (optional, e.g. address)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                errorMessage = null
                if (selectedWorker == null) {
                    errorMessage = "Please choose a Support Worker"
                    return@Button
                }
                if (deadlineDays <= 0) {
                    errorMessage = "Please pick a deadline"
                    return@Button
                }
                val workerSnapshot = selectedWorker!!
                scope.launch {
                    val now = System.currentTimeMillis()
                    val deadlineMillis = now + deadlineDays.toLong() * MILLIS_PER_DAY
                    val ok = withContext(Dispatchers.IO) {
                        requestRepository.assign(
                            requestId = requestId,
                            workerId = workerSnapshot.id,
                            priority = priority,
                            deadlineMillis = deadlineMillis,
                            internalNotes = internalNotes,
                            now = now
                        )
                    }
                    if (!ok) {
                        errorMessage = "This request can't be assigned (it may already be in progress)"
                        return@launch
                    }
                    withContext(Dispatchers.IO) {
                        requestRepository.setGroupTag(requestId, groupTag, now)
                        auditRepository.log(
                            actorUserId = sessionManager.getUserId(),
                            action = AuditAction.REQUEST_ASSIGNED,
                            targetRequestId = requestId,
                            details = "Assigned to ${workerSnapshot.name} ($priority)"
                        )
                    }
                    onAssigned()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Assign")
        }

        Spacer(modifier = Modifier.height(24.dp))
        } // end else (req != null)
    }
}

// short read-only summary of the request the coordinator is acting on
@Composable
private fun SummaryCard(req: WelfareRequest) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = req.title.ifBlank { "(Untitled request)" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (req.category.isNotBlank()) {
                Text(text = req.category, style = MaterialTheme.typography.bodyMedium)
            }
            if (req.address.isNotBlank()) {
                Text(text = req.address, style = MaterialTheme.typography.bodyMedium)
            }
            if (req.description.isNotBlank()) {
                Text(text = req.description, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "Submitted ${DateTimeFormat.dateTime(req.createdAt)}",
                style = MaterialTheme.typography.labelMedium
            )
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