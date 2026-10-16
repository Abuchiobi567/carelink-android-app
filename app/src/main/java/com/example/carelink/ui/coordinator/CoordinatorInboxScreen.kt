package com.example.carelink.ui.coordinator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
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
import com.example.carelink.ui.components.RequestRow
import com.example.carelink.ui.components.RoleTopBar
import com.example.carelink.ui.components.WorkerDropdown
import com.example.carelink.util.MILLIS_PER_DAY

@Composable
fun CoordinatorInboxScreen(
    requestRepository: RequestRepository,
    userRepository: UserRepository,
    auditRepository: AuditRepository,
    sessionManager: SessionManager,
    onAssignSingle: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAuditLog: () -> Unit,
    onLogout: () -> Unit
) {
    var requests by remember { mutableStateOf<List<WelfareRequest>>(emptyList()) }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var priorityFilter by remember { mutableStateOf<String?>(null) }
    var groupByTag by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }

    // bulk-assign state
    var selectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showBulkPanel by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTick, statusFilter, priorityFilter) {
        requests = withContext(Dispatchers.IO) { requestRepository.findInbox(statusFilter, priorityFilter) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            RoleTopBar(
                title = "Coordinator Inbox",
                subtitle = "Review, prioritise and assign welfare requests",
                onSettings = onOpenSettings,
                onLogout = onLogout,
                onAuditLog = onOpenAuditLog
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statusLabel = statusFilter
                FilterMenu(
                    label = "Status",
                    current = if (statusLabel == null) "All" else RequestStatus.displayName(statusLabel),
                    options = listOf<String?>(null) + RequestStatus.TIMELINE_ORDER + listOf(RequestStatus.ESCALATED),
                    renderOption = { value -> if (value == null) "All" else RequestStatus.displayName(value) },
                    onSelect = { statusFilter = it },
                    modifier = Modifier.weight(1f)
                )
                FilterMenu(
                    label = "Priority",
                    current = priorityFilter ?: "All",
                    options = listOf<String?>(null) + Priority.ALL,
                    renderOption = { it ?: "All" },
                    onSelect = { priorityFilter = it },
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = { refreshTick++ }) { Text("Refresh") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = groupByTag,
                    onCheckedChange = { groupByTag = it }
                )
                Text(text = "Group by tag")

                Spacer(modifier = Modifier.weight(1f))

                if (selectMode) {
                    Text(
                        text = "${selectedIds.size} selected",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        selectMode = false
                        selectedIds = emptySet()
                    }) { Text("Cancel") }
                } else {
                    TextButton(onClick = { selectMode = true }) { Text("Select multiple") }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (requests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No requests match the current filters")
                }
            } else {
                val grouped = if (groupByTag) groupRequestsByTag(requests) else null
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (grouped != null) {
                        grouped.forEach { (tag, group) ->
                            item(key = "header-$tag") {
                                Text(
                                    text = if (tag.isBlank()) "Ungrouped" else tag,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(group, key = { "row-${it.id}" }) { req ->
                                InboxRow(
                                    req = req,
                                    selectMode = selectMode,
                                    selected = selectedIds.contains(req.id),
                                    onToggleSelect = { selectedIds = toggle(selectedIds, req.id) },
                                    onOpen = { openRequest(req, onAssignSingle, onOpenDetail) }
                                )
                            }
                        }
                    } else {
                        items(requests, key = { it.id }) { req ->
                            InboxRow(
                                req = req,
                                selectMode = selectMode,
                                selected = selectedIds.contains(req.id),
                                onToggleSelect = { selectedIds = toggle(selectedIds, req.id) },
                                onOpen = { openRequest(req, onAssignSingle, onOpenDetail) }
                            )
                        }
                    }
                }
            }

            if (selectMode && selectedIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showBulkPanel = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Bulk assign (${selectedIds.size})")
                }
            }
        }

        if (showBulkPanel) {
            BulkAssignPanel(
                selectedRequestIds = selectedIds.toList(),
                requestRepository = requestRepository,
                userRepository = userRepository,
                auditRepository = auditRepository,
                sessionManager = sessionManager,
                onDismiss = { showBulkPanel = false },
                onAssigned = {
                    showBulkPanel = false
                    selectMode = false
                    selectedIds = emptySet()
                    refreshTick++
                }
            )
        }
    }
}

// assignable rows go to the assign screen, everything else to read-only detail
private fun openRequest(
    req: WelfareRequest,
    onAssignSingle: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    val assignable = req.status == RequestStatus.SUBMITTED ||
        req.status == RequestStatus.UNDER_REVIEW
    if (assignable) onAssignSingle(req.id) else onOpenDetail(req.id)
}

private fun toggle(selected: Set<Long>, id: Long): Set<Long> {
    return if (selected.contains(id)) selected - id else selected + id
}

// groups by group_tag - empty-tag requests fall under "Ungrouped" at the end
private fun groupRequestsByTag(requests: List<WelfareRequest>): List<Pair<String, List<WelfareRequest>>> {
    val withTag = requests.filter { it.groupTag.isNotBlank() }.groupBy { it.groupTag }
    val withoutTag = requests.filter { it.groupTag.isBlank() }
    val result = mutableListOf<Pair<String, List<WelfareRequest>>>()
    withTag.forEach { (tag, list) -> result.add(tag to list) }
    if (withoutTag.isNotEmpty()) result.add("" to withoutTag)
    return result
}

@Composable
private fun InboxRow(
    req: WelfareRequest,
    selectMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit
) {
    val assignable = req.status == RequestStatus.SUBMITTED ||
        req.status == RequestStatus.UNDER_REVIEW
    val canSelect = selectMode && assignable
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (selectMode) {
            Checkbox(
                checked = selected,
                onCheckedChange = { if (canSelect) onToggleSelect() },
                enabled = canSelect
            )
        }
        RequestRow(
            request = req,
            onClick = {
                if (selectMode) {
                    if (canSelect) onToggleSelect()
                } else {
                    onOpen()
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FilterMenu(
    label: String,
    current: String,
    options: List<String?>,
    renderOption: (String?) -> String,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "$label: $current")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(renderOption(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// inline panel for assigning many requests at once (the chosen extra feature)
@Composable
private fun BulkAssignPanel(
    selectedRequestIds: List<Long>,
    requestRepository: RequestRepository,
    userRepository: UserRepository,
    auditRepository: AuditRepository,
    sessionManager: SessionManager,
    onDismiss: () -> Unit,
    onAssigned: () -> Unit
) {
    var workers by remember { mutableStateOf<List<User>>(emptyList()) }
    var selectedWorker by remember { mutableStateOf<User?>(null) }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var deadlineDays by remember { mutableIntStateOf(3) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var assigning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        workers = withContext(Dispatchers.IO) { userRepository.findByRole(Roles.WORKER) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // empty onClick so taps inside the card don't bubble to the scrim
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(onClick = {})
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Bulk Assignment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${selectedRequestIds.size} requests will be assigned with the same priority, deadline and worker",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Priority", fontWeight = FontWeight.SemiBold)
                Row {
                    Priority.ALL.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            RadioButton(
                                selected = priority == option,
                                onClick = { priority = option }
                            )
                            Text(option.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Deadline", fontWeight = FontWeight.SemiBold)
                Column {
                    listOf(1, 3, 7, 14).forEach { days ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            RadioButton(
                                selected = deadlineDays == days,
                                onClick = { deadlineDays = days }
                            )
                            Text(text = "$days ${if (days == 1) "day" else "days"} from now")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Support Worker", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                WorkerDropdown(
                    workers = workers,
                    selected = selectedWorker,
                    onSelected = { selectedWorker = it }
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !assigning
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            errorMessage = null
                            if (selectedWorker == null) {
                                errorMessage = "Please choose a Support Worker"
                                return@Button
                            }
                            assigning = true
                            val workerSnapshot = selectedWorker!!
                            scope.launch {
                                val now = System.currentTimeMillis()
                                val deadlineMillis = now + deadlineDays.toLong() * MILLIS_PER_DAY
                                val actorId = sessionManager.getUserId()
                                val successCount = withContext(Dispatchers.IO) {
                                    var count = 0
                                    for (id in selectedRequestIds) {
                                        val req = requestRepository.findById(id) ?: continue
                                        if (req.status == RequestStatus.SUBMITTED) {
                                            requestRepository.markUnderReview(id, now)
                                            auditRepository.log(
                                                actorUserId = actorId,
                                                action = AuditAction.REQUEST_UNDER_REVIEW,
                                                targetRequestId = id
                                            )
                                        }
                                        val ok = requestRepository.assign(
                                            requestId = id,
                                            workerId = workerSnapshot.id,
                                            priority = priority,
                                            deadlineMillis = deadlineMillis,
                                            internalNotes = req.internalNotes,
                                            now = now
                                        )
                                        if (ok) {
                                            auditRepository.log(
                                                actorUserId = actorId,
                                                action = AuditAction.REQUEST_ASSIGNED,
                                                targetRequestId = id,
                                                details = "Bulk assigned to ${workerSnapshot.name} ($priority)"
                                            )
                                            count++
                                        }
                                    }
                                    count
                                }
                                assigning = false
                                if (successCount == 0) {
                                    errorMessage = "No requests could be assigned"
                                } else {
                                    onAssigned()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !assigning
                    ) {
                        Text(if (assigning) "Assigning..." else "Assign all")
                    }
                }
            }
        }
    }
}
