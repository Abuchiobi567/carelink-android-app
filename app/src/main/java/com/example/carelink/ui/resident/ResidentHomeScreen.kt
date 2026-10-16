package com.example.carelink.ui.resident

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carelink.data.AuditRepository
import com.example.carelink.data.RequestRepository
import com.example.carelink.data.SessionManager
import com.example.carelink.data.models.AuditAction
import com.example.carelink.data.models.RequestStatus
import com.example.carelink.data.models.WelfareRequest
import com.example.carelink.ui.components.RequestRow
import com.example.carelink.ui.components.RoleTopBar

@Composable
fun ResidentHomeScreen(
    requestRepository: RequestRepository,
    auditRepository: AuditRepository,
    sessionManager: SessionManager,
    onOpenDraft: (Long) -> Unit,
    onOpenRequestDetail: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val residentId = sessionManager.getUserId()
    var requests by remember { mutableStateOf<List<WelfareRequest>>(emptyList()) }
    var refreshTick by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTick) {
        requests = withContext(Dispatchers.IO) { requestRepository.findByResident(residentId) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        RoleTopBar(
            title = "My Welfare Requests",
            subtitle = "Submit and track requests for the people you care for",
            onSettings = onOpenSettings,
            onLogout = onLogout
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        val newId = withContext(Dispatchers.IO) {
                            requestRepository.insertDraft(
                                residentId = residentId,
                                now = System.currentTimeMillis()
                            )
                        }
                        if (newId != -1L) {
                            withContext(Dispatchers.IO) {
                                auditRepository.log(
                                    actorUserId = residentId,
                                    action = AuditAction.DRAFT_CREATED,
                                    targetRequestId = newId
                                )
                            }
                            onOpenDraft(newId)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("+ New Request")
            }

            // manual refresh - the screen does not auto-reload after popping
            // back from the editor / detail screens
            OutlinedButton(onClick = { refreshTick++ }) {
                Text("Refresh")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (requests.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(requests, key = { it.id }) { req ->
                    RequestRow(
                        request = req,
                        onClick = {
                            // drafts open the editor, everything else opens the read-only detail
                            if (req.status == RequestStatus.DRAFT) {
                                onOpenDraft(req.id)
                            } else {
                                onOpenRequestDetail(req.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No requests yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap + New Request to get started",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
