package com.example.carelink.data

import android.content.ContentValues
import android.database.Cursor
import com.example.carelink.data.models.Visit

// one row per completed visit. caller still has to flip the parent
// request's status via RequestRepository.markVisitCompleted().
class VisitRepository(private val database: CareLinkDatabase) {

    fun insertVisit(
        requestId: Long,
        workerId: Long,
        outcomeNotes: String,
        startedAt: Long?,
        completedAt: Long
    ): Long {
        val db = database.writableDatabase
        val v = ContentValues()
        v.put(CareLinkDatabase.COL_VISIT_REQUEST_ID, requestId)
        v.put(CareLinkDatabase.COL_VISIT_WORKER_ID, workerId)
        v.put(CareLinkDatabase.COL_VISIT_NOTES, outcomeNotes.trim())
        if (startedAt == null) {
            v.putNull(CareLinkDatabase.COL_VISIT_STARTED)
        } else {
            v.put(CareLinkDatabase.COL_VISIT_STARTED, startedAt)
        }
        v.put(CareLinkDatabase.COL_VISIT_COMPLETED, completedAt)
        return db.insert(CareLinkDatabase.TABLE_VISITS, null, v)
    }

    // most recent visit - shown next to the verify/escalate buttons
    fun findLatestForRequest(requestId: Long): Visit? {
        val db = database.readableDatabase
        val query =
            "SELECT * FROM ${CareLinkDatabase.TABLE_VISITS} " +
                "WHERE ${CareLinkDatabase.COL_VISIT_REQUEST_ID} = ? " +
                "ORDER BY ${CareLinkDatabase.COL_VISIT_COMPLETED} DESC LIMIT 1"
        val cursor = db.rawQuery(query, arrayOf(requestId.toString()))
        var visit: Visit? = null
        if (cursor.moveToFirst()) {
            visit = cursorToVisit(cursor)
        }
        cursor.close()
        return visit
    }

    // visit history for the worker, newest first
    fun findAllForRequest(requestId: Long): List<Visit> {
        val db = database.readableDatabase
        val query =
            "SELECT * FROM ${CareLinkDatabase.TABLE_VISITS} " +
                "WHERE ${CareLinkDatabase.COL_VISIT_REQUEST_ID} = ? " +
                "ORDER BY ${CareLinkDatabase.COL_VISIT_COMPLETED} DESC"
        val cursor = db.rawQuery(query, arrayOf(requestId.toString()))
        val list = mutableListOf<Visit>()
        while (cursor.moveToNext()) {
            list.add(cursorToVisit(cursor))
        }
        cursor.close()
        return list
    }

    private fun cursorToVisit(c: Cursor): Visit {
        val startedCol = c.getColumnIndexOrThrow(CareLinkDatabase.COL_VISIT_STARTED)
        val startedAt = if (c.isNull(startedCol)) null else c.getLong(startedCol)
        return Visit(
            id = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_VISIT_ID)),
            requestId = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_VISIT_REQUEST_ID)),
            workerId = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_VISIT_WORKER_ID)),
            outcomeNotes = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_VISIT_NOTES)),
            startedAt = startedAt,
            completedAt = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_VISIT_COMPLETED))
        )
    }
}