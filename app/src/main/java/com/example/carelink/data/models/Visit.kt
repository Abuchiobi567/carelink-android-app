package com.example.carelink.data.models

// one completed visit recorded by a Support Worker
data class Visit(
    val id: Long = 0,
    val requestId: Long,
    val workerId: Long,
    val outcomeNotes: String,
    // null if the worker didn't use the optional visit timer
    val startedAt: Long? = null,
    val completedAt: Long
)
