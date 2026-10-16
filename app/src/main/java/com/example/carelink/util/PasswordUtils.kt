package com.example.carelink.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object PasswordUtils {

    fun hash(password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return "${Base64.encodeToString(salt, Base64.NO_WRAP)}:${Base64.encodeToString(hash, Base64.NO_WRAP)}"
    }

    fun verify(password: String, stored: String): Boolean {
        return try {
            val idx = stored.indexOf(':')
            if (idx < 0) return false
            val salt = Base64.decode(stored.substring(0, idx), Base64.NO_WRAP)
            val expected = Base64.decode(stored.substring(idx + 1), Base64.NO_WRAP)
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(salt)
            MessageDigest.isEqual(expected, digest.digest(password.toByteArray(Charsets.UTF_8)))
        } catch (_: Exception) { false }
    }
}
