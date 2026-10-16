package com.example.carelink.ui.resident

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carelink.data.AuditRepository
import com.example.carelink.data.RequestRepository
import com.example.carelink.data.SessionManager
import com.example.carelink.data.models.AuditAction
import com.example.carelink.data.models.ConcernCategory
import com.example.carelink.data.models.RequestStatus
import com.example.carelink.util.NotificationsHelper

@Composable
fun SubmitRequestScreen(
    requestId: Long,
    requestRepository: RequestRepository,
    auditRepository: AuditRepository,
    sessionManager: SessionManager,
    onSubmitted: () -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ConcernCategory.ROUTINE) }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var loaded by remember { mutableStateOf(false) }
    var isStillDraft by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(requestId) {
        val req = withContext(Dispatchers.IO) { requestRepository.findById(requestId) }
        if (req != null) {
            title = req.title
            description = req.description
            category = if (req.category.isBlank()) ConcernCategory.ROUTINE else req.category
            address = req.address
            notes = req.notes
            isStillDraft = req.status == RequestStatus.DRAFT
        }
        loaded = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (isStillDraft) "New Welfare Request" else "Welfare Request",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (isStillDraft) "Fill in the details, then save or submit"
            else "This request has been submitted and can no longer be edited",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                statusMessage = null
                errorMessage = null
            },
            label = { Text("Title") },
            singleLine = true,
            enabled = isStillDraft,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                statusMessage = null
                errorMessage = null
            },
            label = { Text("Describe your concern") },
            enabled = isStillDraft,
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Concern category",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        // simple radio group - one of the controls covered in the Compose lecture
        Column(modifier = Modifier.padding(start = 4.dp)) {
            ConcernCategory.ALL.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    RadioButton(
                        selected = category == option,
                        onClick = { if (isStillDraft) category = option },
                        enabled = isStillDraft
                    )
                    Text(text = option, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Resident's address") },
            enabled = isStillDraft,
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Additional notes (optional)") },
            enabled = isStillDraft,
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        if (statusMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = statusMessage ?: "",
                color = Color(0xFF2E7D32),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isStillDraft) {
            // primary actions: save draft / submit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        statusMessage = null
                        errorMessage = null
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                requestRepository.updateDraftFields(
                                    requestId = requestId,
                                    title = title,
                                    description = description,
                                    category = category,
                                    address = address,
                                    notes = notes,
                                    now = System.currentTimeMillis()
                                )
                            }
                            if (ok) {
                                withContext(Dispatchers.IO) {
                                    auditRepository.log(
                                        actorUserId = sessionManager.getUserId(),
                                        action = AuditAction.DRAFT_UPDATED,
                                        targetRequestId = requestId
                                    )
                                }
                                statusMessage = "Draft saved"
                            } else {
                                errorMessage = "Could not save draft"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Draft")
                }
                Button(
                    onClick = {
                        statusMessage = null
                        errorMessage = null
                        val validation = validateForSubmit(title, description, address)
                        if (validation != null) {
                            errorMessage = validation
                            return@Button
                        }
                        scope.launch {
                            val saveOk = withContext(Dispatchers.IO) {
                                requestRepository.updateDraftFields(
                                    requestId = requestId,
                                    title = title,
                                    description = description,
                                    category = category,
                                    address = address,
                                    notes = notes,
                                    now = System.currentTimeMillis()
                                )
                            }
                            if (!saveOk) {
                                errorMessage = "Could not save your changes"
                                return@launch
                            }
                            val submitOk = withContext(Dispatchers.IO) {
                                requestRepository.submit(
                                    requestId = requestId,
                                    now = System.currentTimeMillis()
                                )
                            }
                            if (!submitOk) {
                                errorMessage = "Could not submit this request"
                                return@launch
                            }
                            withContext(Dispatchers.IO) {
                                auditRepository.log(
                                    actorUserId = sessionManager.getUserId(),
                                    action = AuditAction.REQUEST_SUBMITTED,
                                    targetRequestId = requestId
                                )
                            }
                            NotificationsHelper.sendSubmissionConfirmation(context, title)
                            onSubmitted()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Submit")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) { requestRepository.deleteDraft(requestId) }
                        if (ok) {
                            withContext(Dispatchers.IO) {
                                auditRepository.log(
                                    actorUserId = sessionManager.getUserId(),
                                    action = AuditAction.DRAFT_DELETED,
                                    targetRequestId = requestId
                                )
                            }
                            onDeleted()
                        } else {
                            errorMessage = "Could not delete this draft"
                        }
                    }
                }
            ) {
                Text("Delete draft", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onBack) {
            Text("Back")
        }

        // shows briefly while we're loading the row
        if (!loaded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Loading...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun validateForSubmit(title: String, description: String, address: String): String? {
    if (title.isBlank()) return "Please add a title"
    if (description.isBlank()) return "Please describe the concern"
    if (address.isBlank()) return "Please add the resident's address"
    return null
}
