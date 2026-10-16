package com.example.carelink.data.models

// one welfare check-in request - shows up on the resident detail screen,
// the coordinator inbox, and the worker's visit list
data class WelfareRequest(
    val id: Long = 0,
    val residentId: Long,
    val title: String,
    val description: String,
    val category: String,
    val address: String,
    val notes: String,
    val status: String,
    val priority: String,
    // epoch millis. null while it's still a draft / unassigned.
    val deadlineMillis: Long? = null,
    val assignedWorkerId: Long? = null,
    val internalNotes: String = "",
    // free-text tag the coordinator can use to bundle related requests.
    // empty string means no group.
    val groupTag: String = "",
    val createdAt: Long,
    val updatedAt: Long
)