package com.example.carelink.data

import android.content.ContentValues
import android.database.Cursor
import com.example.carelink.data.models.User
import com.example.carelink.util.PasswordUtils

// all the user-related SQL lives here so the screens stay clean
class UserRepository(private val database: CareLinkDatabase) {

    // returns the new row id, or -1L if the email is already taken
    fun register(name: String, email: String, password: String, role: String): Long {
        val db = database.writableDatabase
        val values = ContentValues()
        values.put(CareLinkDatabase.COL_USER_NAME, name.trim())
        values.put(CareLinkDatabase.COL_USER_EMAIL, email.trim().lowercase())
        values.put(CareLinkDatabase.COL_USER_PASSWORD, PasswordUtils.hash(password))
        values.put(CareLinkDatabase.COL_USER_ROLE, role)
        return db.insert(CareLinkDatabase.TABLE_USERS, null, values)
    }

    // returns null when no user matches
    fun signIn(email: String, password: String): User? {
        val db = database.readableDatabase
        val query =
            "SELECT * FROM ${CareLinkDatabase.TABLE_USERS} " +
                "WHERE ${CareLinkDatabase.COL_USER_EMAIL} = ? LIMIT 1"
        val cursor = db.rawQuery(query, arrayOf(email.trim().lowercase()))
        var user: User? = null
        if (cursor.moveToFirst()) {
            val candidate = cursorToUser(cursor)
            if (PasswordUtils.verify(password, candidate.password)) {
                user = candidate
            }
        }
        cursor.close()
        return user
    }

    fun findById(userId: Long): User? {
        val db = database.readableDatabase
        val query =
            "SELECT * FROM ${CareLinkDatabase.TABLE_USERS} " +
                "WHERE ${CareLinkDatabase.COL_USER_ID} = ? LIMIT 1"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor)
        }
        cursor.close()
        return user
    }

    // batch lookup so the audit screen can resolve all actor names in one go
    fun findByIds(userIds: Collection<Long>): Map<Long, User> {
        if (userIds.isEmpty()) return emptyMap()
        val db = database.readableDatabase
        // build a "?, ?, ?" placeholder string of the right length
        val placeholders = userIds.joinToString(",") { "?" }
        val query =
            "SELECT * FROM ${CareLinkDatabase.TABLE_USERS} " +
                "WHERE ${CareLinkDatabase.COL_USER_ID} IN ($placeholders)"
        val args = userIds.map { it.toString() }.toTypedArray()
        val cursor = db.rawQuery(query, args)
        val result = mutableMapOf<Long, User>()
        while (cursor.moveToNext()) {
            val user = cursorToUser(cursor)
            result[user.id] = user
        }
        cursor.close()
        return result
    }

    // used by the coordinator's "assign worker" dropdown
    fun findByRole(role: String): List<User> {
        val db = database.readableDatabase
        val query =
            "SELECT * FROM ${CareLinkDatabase.TABLE_USERS} " +
                "WHERE ${CareLinkDatabase.COL_USER_ROLE} = ? " +
                "ORDER BY ${CareLinkDatabase.COL_USER_NAME} ASC"
        val cursor = db.rawQuery(query, arrayOf(role))
        val list = mutableListOf<User>()
        while (cursor.moveToNext()) {
            list.add(cursorToUser(cursor))
        }
        cursor.close()
        return list
    }

    private fun cursorToUser(c: Cursor): User {
        return User(
            id = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_USER_ID)),
            name = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_USER_NAME)),
            email = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_USER_EMAIL)),
            password = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_USER_PASSWORD)),
            role = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_USER_ROLE))
        )
    }
}