package com.example.carelink.data.models

// shared constants. plain object + const val strings so they map directly
// to TEXT columns in SQLite without any conversion.

object Roles {
    const val RESIDENT = "RESIDENT"
    const val COORDINATOR = "CARE_COORDINATOR"
    const val WORKER = "SUPPORT_WORKER"
    const val REVIEWER = "SAFEGUARDING_REVIEWER"

    fun displayName(role: String): String = when (role) {
        RESIDENT -> "Resident / Carer"
        COORDINATOR -> "Care Coordinator"
        WORKER -> "Support Worker"
        REVIEWER -> "Safeguarding Reviewer"
        else -> role
    }
}

object RequestStatus {
    const val DRAFT = "DRAFT"
    const val SUBMITTED = "SUBMITTED"
    const val UNDER_REVIEW = "UNDER_REVIEW"
    const val ASSIGNED = "ASSIGNED"
    const val VISIT_COMPLETED = "VISIT_COMPLETED"
    const val VERIFIED = "VERIFIED"
    const val ESCALATED = "ESCALATED"

    // order shown by the timeline component
    val TIMELINE_ORDER = listOf(SUBMITTED, UNDER_REVIEW, ASSIGNED, VISIT_COMPLETED, VERIFIED)

    fun displayName(status: String): String = when (status) {
        DRAFT -> "Draft"
        SUBMITTED -> "Submitted"
        UNDER_REVIEW -> "Under Review"
        ASSIGNED -> "Assigned"
        VISIT_COMPLETED -> "Visit Completed"
        VERIFIED -> "Verified"
        ESCALATED -> "Escalated"
        else -> status
    }
}

object Priority {
    const val LOW = "LOW"
    const val MEDIUM = "MEDIUM"
    const val HIGH = "HIGH"
    val ALL = listOf(LOW, MEDIUM, HIGH)

    // HIGH first when sorting
    fun rank(value: String): Int = when (value) {
        HIGH -> 0
        MEDIUM -> 1
        LOW -> 2
        else -> 3
    }
}

object ConcernCategory {
    const val ROUTINE = "Routine check"
    const val MISSED = "Missed contact"
    const val WELLBEING = "Wellbeing concern"
    val ALL = listOf(ROUTINE, MISSED, WELLBEING)
}

object AuditAction {
    const val LOGIN = "LOGIN"
    const val REGISTER = "REGISTER"
    const val DRAFT_CREATED = "DRAFT_CREATED"
    const val DRAFT_UPDATED = "DRAFT_UPDATED"
    const val DRAFT_DELETED = "DRAFT_DELETED"
    const val REQUEST_SUBMITTED = "REQUEST_SUBMITTED"
    const val REQUEST_UNDER_REVIEW = "REQUEST_UNDER_REVIEW"
    const val REQUEST_ASSIGNED = "REQUEST_ASSIGNED"
    const val VISIT_COMPLETED = "VISIT_COMPLETED"
    const val REQUEST_VERIFIED = "REQUEST_VERIFIED"
    const val REQUEST_ESCALATED = "REQUEST_ESCALATED"
}

object EscalationReason {
    const val SAFEGUARDING_RISK = "Safeguarding risk identified"
    const val MEDICAL_CONCERN = "Medical concern"
    const val ENVIRONMENTAL_HAZARD = "Environmental hazard"
    const val NO_CONTACT = "Resident not contactable"
    const val OTHER = "Other"
    val ALL = listOf(SAFEGUARDING_RISK, MEDICAL_CONCERN, ENVIRONMENTAL_HAZARD, NO_CONTACT, OTHER)
}