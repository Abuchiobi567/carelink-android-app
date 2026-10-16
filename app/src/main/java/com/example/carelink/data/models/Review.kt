package com.example.carelink.data.models

// a Safeguarding Reviewer's decision on a completed visit.
// decision is "VERIFIED" or "ESCALATED" (see RequestStatus).
data class Review(
    val id: Long = 0,
    val requestId: Long,
    val reviewerId: Long,
    val decision: String,
    val reason: String,
    val reviewedAt: Long
)