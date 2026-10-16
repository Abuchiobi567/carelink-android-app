package com.example.carelink.ui.resident

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carelink.data.RequestRepository
import com.example.carelink.data.ReviewRepository
import com.example.carelink.data.VisitRepository
import com.example.carelink.data.models.RequestStatus
import com.example.carelink.data.models.Review
import com.example.carelink.data.models.Visit
import com.example.carelink.data.models.WelfareRequest
import com.example.carelink.ui.components.DeadlineChip
import com.example.carelink.ui.components.StatusTimeline
import com.example.carelink.util.DateTimeFormat

@Composable
fun RequestDetailScreen(
    requestId: Long,
    requestRepository: RequestRepository,
    visitRepository: VisitRepository,
    reviewRepository: ReviewRepository,
    onBack: () -> Unit
) {
    var request by remember { mutableStateOf<WelfareRequest?>(null) }
    var visit by remember { mutableStateOf<Visit?>(null) }
    var review by remember { mutableStateOf<Review?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(requestId) {
        request = withContext(Dispatchers.IO) { requestRepository.findById(requestId) }
        visit = withContext(Dispatchers.IO) { visitRepository.findLatestForRequest(requestId) }
        review = withContext(Dispatchers.IO) { reviewRepository.findAllForRequest(requestId).firstOrNull() }
        loaded = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("< Back") }

        val req = request
        if (req == null) {
            Text(
                text = if (loaded) "Request not found" else "Loading...",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = req.title.ifBlank { "(Untitled request)" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel("Progress")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatusTimeline(currentStatus = req.status)
                }
            }

            if (req.deadlineMillis != null) {
                Spacer(modifier = Modifier.height(12.dp))
                DeadlineChip(deadlineMillis = req.deadlineMillis)
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionLabel("Request details")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabelledValue("Category", req.category.ifBlank { "—" })
                    LabelledValue("Address", req.address.ifBlank { "—" })
                    LabelledValue("Description", req.description.ifBlank { "—" })
                    if (req.notes.isNotBlank()) {
                        LabelledValue("Additional notes", req.notes)
                    }
                    LabelledValue("Submitted", DateTimeFormat.dateTime(req.createdAt))
                    LabelledValue("Last updated", DateTimeFormat.dateTime(req.updatedAt))
                }
            }

            // visit outcome appears once a Support Worker has completed the visit
            if (visit != null) {
                Spacer(modifier = Modifier.height(16.dp))
                SectionLabel("Visit outcome")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabelledValue("Notes", visit?.outcomeNotes?.ifBlank { "—" } ?: "—")
                        LabelledValue("Completed at", DateTimeFormat.dateTime(visit!!.completedAt))
                    }
                }
            }

            // reviewer's decision appears once a Safeguarding Reviewer has decided
            if (review != null) {
                Spacer(modifier = Modifier.height(16.dp))
                SectionLabel("Safeguarding decision")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabelledValue("Decision", RequestStatus.displayName(review!!.decision))
                        if (review!!.reason.isNotBlank()) {
                            LabelledValue("Reason", review!!.reason)
                        }
                        LabelledValue("Reviewed at", DateTimeFormat.dateTime(review!!.reviewedAt))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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

@Composable
private fun LabelledValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
