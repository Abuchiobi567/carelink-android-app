package com.example.carelink.ui.reviewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.data.RequestRepository
import com.example.carelink.data.models.WelfareRequest
import com.example.carelink.ui.components.RequestRow
import com.example.carelink.ui.components.RoleTopBar

@Composable
fun ReviewQueueScreen(
    requestRepository: RequestRepository,
    onOpenReview: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAuditLog: () -> Unit,
    onLogout: () -> Unit
) {
    var queue by remember { mutableStateOf<List<WelfareRequest>>(emptyList()) }
    var refreshTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTick) {
        queue = withContext(Dispatchers.IO) { requestRepository.findReviewQueue() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        RoleTopBar(
            title = "Review Queue",
            subtitle = "Completed visits awaiting safeguarding review",
            onSettings = onOpenSettings,
            onLogout = onLogout,
            onAuditLog = onOpenAuditLog
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { refreshTick++ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (queue.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Nothing to review",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All completed visits have been reviewed",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(queue, key = { it.id }) { req ->
                    RequestRow(
                        request = req,
                        onClick = { onOpenReview(req.id) }
                    )
                }
            }
        }
    }
}
