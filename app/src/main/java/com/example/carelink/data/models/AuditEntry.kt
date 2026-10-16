package com.example.carelink.data.models

// one row in the audit_log table
data class AuditEntry(
    val id: Long = 0,
    val actorUserId: Long,
    val action: String,
    // null when the action isn't tied to a specific request (e.g. LOGIN)
    val targetRequestId: Long? = null,
    val timestampMillis: Long,
    // free-text e.g. "Status: SUBMITTED -> UNDER_REVIEW"
    val details: String = ""
)