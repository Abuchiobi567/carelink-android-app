package com.example.carelink.data

import android.content.ContentValues
import android.database.Cursor
import com.example.carelink.data.models.AuditEntry

// appends rows to audit_log. every screen that changes state should call
// log() so we have the "who did what and when" trail the brief asks for.
class

AuditRepository(private val database: CareLinkDatabase) {

    fun log(
        actorUserId: Long,
        action: String,
        targetRequestId: Long? = null,
        details: String = "",
        timestampMillis: Long = System.currentTimeMillis()
    ): Long {
        val db = database.writableDatabase
        val v = ContentValues()
        v.put(CareLinkDatabase.COL_AUDIT_ACTOR, actorUserId)
        v.put(CareLinkDatabase.COL_AUDIT_ACTION, action)
        if (targetRequestId == null) {
            v.putNull(CareLinkDatabase.COL_AUDIT_TARGET)
        } else {
            v.put(CareLinkDatabase.COL_AUDIT_TARGET, targetRequestId)
        }
        v.put(CareLinkDatabase.COL_AUDIT_TIMESTAMP, timestampMillis)
        v.put(CareLinkDatabase.COL_AUDIT_DETAILS, details)
        return db.insert(CareLinkDatabase.TABLE_AUDIT_LOG, null, v)
    }

    // newest first - the audit screen filters this list client-side
    fun findAll(): List<AuditEntry> {
        val db = database.readableDatabase
        val query =
            "SELECT * FROM ${CareLinkDatabase.TABLE_AUDIT_LOG} " +
                "ORDER BY ${CareLinkDatabase.COL_AUDIT_TIMESTAMP} DESC"
        val cursor = db.rawQuery(query, null)
        val list = mutableListOf<AuditEntry>()
        while (cursor.moveToNext()) {
            list.add(cursorToEntry(cursor))
        }
        cursor.close()
        return list
    }

    fun findForRequest(requestId: Long): List<AuditEntry> {
        val db = database.readableDatabase
        val query =
            "SELECT * FROM ${CareLinkDatabase.TABLE_AUDIT_LOG} " +
                "WHERE ${CareLinkDatabase.COL_AUDIT_TARGET} = ? " +
                "ORDER BY ${CareLinkDatabase.COL_AUDIT_TIMESTAMP} ASC"
        val cursor = db.rawQuery(query, arrayOf(requestId.toString()))
        val list = mutableListOf<AuditEntry>()
        while (cursor.moveToNext()) {
            list.add(cursorToEntry(cursor))
        }
        cursor.close()
        return list
    }

    private fun cursorToEntry(c: Cursor): AuditEntry {
        val targetCol = c.getColumnIndexOrThrow(CareLinkDatabase.COL_AUDIT_TARGET)
        val target = if (c.isNull(targetCol)) null else c.getLong(targetCol)
        return AuditEntry(
            id = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_AUDIT_ID)),
            actorUserId = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_AUDIT_ACTOR)),
            action = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_AUDIT_ACTION)),
            targetRequestId = target,
            timestampMillis = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_AUDIT_TIMESTAMP)),
            details = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_AUDIT_DETAILS))
        )
    }
}