package com.example.carelink.ui.reviewer

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carelink.data.AuditRepository
import com.example.carelink.data.RequestRepository
import com.example.carelink.data.ReviewRepository
import com.example.carelink.data.SessionManager
import com.example.carelink.data.VisitRepository
import com.example.carelink.data.models.AuditAction
import com.example.carelink.data.models.EscalationReason
import com.example.carelink.data.models.Priority
import com.example.carelink.data.models.RequestStatus
import com.example.carelink.data.models.Review
import com.example.carelink.data.models.Visit
import com.example.carelink.data.models.WelfareRequest
import com.example.carelink.util.DateTimeFormat

@Composable
fun ReviewDetailScreen(
    requestId: Long,
    requestRepository: RequestRepository,
    visitRepository: VisitRepository,
    reviewRepository: ReviewRepository,
    auditRepository: AuditRepository,
    sessionManager: SessionManager,
    onDecisionMade: () -> Unit,
    onBack: () -> Unit
) {
    var request by remember { mutableStateOf<WelfareRequest?>(null) }
    var visit by remember { mutableStateOf<Visit?>(null) }
    var pastReviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    var showEscalateForm by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var newPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var extraNotes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(requestId) {
        val req = withContext(Dispatchers.IO) { requestRepository.findById(requestId) }
        request = req
        visit = withContext(Dispatchers.IO) { visitRepository.findLatestForRequest(requestId) }
        pastReviews = withContext(Dispatchers.IO) { reviewRepository.findAllForRequest(requestId) }
        if (req != null) {
            newPriority = req.priority
        }
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
            text = "Safeguarding Review",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        val req = request
        if (req == null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = if (loaded) "Request not found" else "Loading...")
        } else {

        Spacer(modifier = Modifier.height(16.dp))

        SummaryCard(req)

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Visit outcome")
        VisitCard(visit)

        // reviewer notes history (extra feature) - only shown when there
        // are previous reviews for this request, e.g. after a re-escalation
        if (pastReviews.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            SectionLabel("Previous reviews")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pastReviews.forEach { r ->
                    PastReviewCard(r)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        SectionLabel("Decision")

        if (!showEscalateForm) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        errorMessage = null
                        scope.launch {
                            val now = System.currentTimeMillis()
                            val reviewerId = sessionManager.getUserId()
                            val statusOk = withContext(Dispatchers.IO) {
                                requestRepository.verify(requestId, now)
                            }
                            if (!statusOk) {
                                errorMessage = "This visit can no longer be verified"
                                return@launch
                            }
                            withContext(Dispatchers.IO) {
                                reviewRepository.insertReview(
                                    requestId = requestId,
                                    reviewerId = reviewerId,
                                    decision = RequestStatus.VERIFIED,
                                    reason = "",
                                    reviewedAt = now
                                )
                                auditRepository.log(
                                    actorUserId = reviewerId,
                                    action = AuditAction.REQUEST_VERIFIED,
                                    targetRequestId = requestId
                                )
                            }
                            onDecisionMade()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Verify")
                }
                OutlinedButton(
                    onClick = { showEscalateForm = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Escalate...")
                }
            }
        } else {
            EscalateForm(
                currentPriority = req.priority,
                selectedReason = selectedReason,
                onReasonChange = { selectedReason = it; errorMessage = null },
                newPriority = newPriority,
                onPriorityChange = { newPriority = it },
                extraNotes = extraNotes,
                onNotesChange = { extraNotes = it },
                onCancel = {
                    showEscalateForm = false
                    selectedReason = null
                    extraNotes = ""
                    newPriority = req.priority
                    errorMessage = null
                },
                onConfirm = {
                    errorMessage = null
                    val reason = selectedReason
                    if (reason == null) {
                        errorMessage = "Please choose an escalation reason"
                        return@EscalateForm
                    }
                    val combinedReason = if (extraNotes.isBlank()) reason
                    else "$reason - $extraNotes"
                    scope.launch {
                        val now = System.currentTimeMillis()
                        val reviewerId = sessionManager.getUserId()
                        val statusOk = withContext(Dispatchers.IO) {
                            requestRepository.escalate(requestId, newPriority, now)
                        }
                        if (!statusOk) {
                            errorMessage = "This visit can no longer be escalated"
                            return@launch
                        }
                        withContext(Dispatchers.IO) {
                            reviewRepository.insertReview(
                                requestId = requestId,
                                reviewerId = reviewerId,
                                decision = RequestStatus.ESCALATED,
                                reason = combinedReason,
                                reviewedAt = now
                            )
                            auditRepository.log(
                                actorUserId = reviewerId,
                                action = AuditAction.REQUEST_ESCALATED,
                                targetRequestId = requestId,
                                details = combinedReason
                            )
                        }
                        onDecisionMade()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        } // end else (req != null)
    }
}

@Composable
private fun EscalateForm(
    currentPriority: String,
    selectedReason: String?,
    onReasonChange: (String) -> Unit,
    newPriority: String,
    onPriorityChange: (String) -> Unit,
    extraNotes: String,
    onNotesChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Escalation reason",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            // structured escalation reasons (extra feature) - radio group of
            // predefined options keeps reviewers' decisions consistent
            Column {
                EscalationReason.ALL.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        RadioButton(
                            selected = selectedReason == option,
                            onClick = { onReasonChange(option) }
                        )
                        Text(text = option, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // priority override - bumps the request priority during escalation
            Text(
                text = "New priority (currently $currentPriority)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row {
                Priority.ALL.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        RadioButton(
                            selected = newPriority == option,
                            onClick = { onPriorityChange(option) }
                        )
                        Text(text = option.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = extraNotes,
                onValueChange = onNotesChange,
                label = { Text("Additional notes (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Confirm escalation")
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
                text = "Priority: ${req.priority}",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun VisitCard(visit: Visit?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (visit == null) {
                Text(text = "No visit recorded", style = MaterialTheme.typography.bodyMedium)
                return@Card
            }
            Text(
                text = "Worker's notes",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = visit.outcomeNotes.ifBlank { "(no notes recorded)" },
                style = MaterialTheme.typography.bodyMedium
            )
            if (visit.startedAt != null) {
                val durationMinutes = (visit.completedAt - visit.startedAt) / 60000L
                Text(
                    text = "On site for ${durationMinutes} ${if (durationMinutes == 1L) "minute" else "minutes"}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                text = "Completed at ${DateTimeFormat.dateTime(visit.completedAt)}",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun PastReviewCard(review: Review) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = RequestStatus.displayName(review.decision),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            if (review.reason.isNotBlank()) {
                Text(text = review.reason, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = DateTimeFormat.dateTime(review.reviewedAt),
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