package com.example.carelink.data.models

// one row in the users table
data class User(
    val id: Long = 0,
    val name: String,
    val email: String,
    val password: String,
    val role: String
)