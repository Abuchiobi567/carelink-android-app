package com.example.carelink.data

import android.content.ContentValues
import android.database.Cursor
import com.example.carelink.data.models.Priority
import com.example.carelink.data.models.RequestStatus
import com.example.carelink.data.models.WelfareRequest

// CRUD for welfare requests. Filtering and sorting is done in SQL so the
// indexes we set up in CareLinkDatabase actually get used.
class RequestRepository(private val database: CareLinkDatabase) {

    // ----- create / update / delete -----

    fun insertDraft(residentId: Long, now: Long): Long {
        val db = database.writableDatabase
        val v = ContentValues()
        v.put(CareLinkDatabase.COL_REQ_RESIDENT_ID, residentId)
        v.put(CareLinkDatabase.COL_REQ_TITLE, "")
        v.put(CareLinkDatabase.COL_REQ_DESCRIPTION, "")
        v.put(CareLinkDatabase.COL_REQ_CATEGORY, "")
        v.put(CareLinkDatabase.COL_REQ_ADDRESS, "")
        v.put(CareLinkDatabase.COL_REQ_NOTES, "")
        v.put(CareLinkDatabase.COL_REQ_STATUS, RequestStatus.DRAFT)
        v.put(CareLinkDatabase.COL_REQ_PRIORITY, Priority.MEDIUM)
        v.putNull(CareLinkDatabase.COL_REQ_DEADLINE)
        v.putNull(CareLinkDatabase.COL_REQ_WORKER_ID)
        v.put(CareLinkDatabase.COL_REQ_INTERNAL_NOTES, "")
        v.put(CareLinkDatabase.COL_REQ_GROUP_TAG, "")
        v.put(CareLinkDatabase.COL_REQ_CREATED, now)
        v.put(CareLinkDatabase.COL_REQ_UPDATED, now)
        return db.insert(CareLinkDatabase.TABLE_REQUESTS, null, v)
    }

    // only works while the row is still a draft - the WHERE clause enforces this
    fun updateDraftFields(
        requestId: Long,
        title: String,
        description: String,
        category: String,
        address: String,
        notes: String,
        now: Long
    ): Boolean {
        val db = database.writableDatabase
        val v = ContentValues()
        v.put(CareLinkDatabase.COL_REQ_TITLE, title.trim())
        v.put(CareLinkDatabase.COL_REQ_DESCRIPTION, description.trim())
        v.put(CareLinkDatabase.COL_REQ_CATEGORY, category)
        v.put(CareLinkDatabase.COL_REQ_ADDRESS, address.trim())
        v.put(CareLinkDatabase.COL_REQ_NOTES, notes.trim())
        v.put(CareLinkDatabase.COL_REQ_UPDATED, now)
        val rows = db.update(
            CareLinkDatabase.TABLE_REQUESTS, v,
            "${CareLinkDatabase.COL_REQ_ID} = ? AND ${CareLinkDatabase.COL_REQ_STATUS} = ?",
            arrayOf(requestId.toString(), RequestStatus.DRAFT)
        )
        return rows > 0
    }

    fun deleteDraft(requestId: Long): Boolean {
        val db = database.writableDatabase
        val rows = db.delete(
            CareLinkDatabase.TABLE_REQUESTS,
            "${CareLinkDatabase.COL_REQ_ID} = ? AND ${CareLinkDatabase.COL_REQ_STATUS} = ?",
            arrayOf(requestId.toString(), RequestStatus.DRAFT)
        )
        return rows > 0
    }

    // ----- status transitions -----
    // each method checks the expected current status in the WHERE clause so a
    // stale screen can't push a request through an invalid state

    fun submit(requestId: Long, now: Long): Boolean {
        return setStatus(requestId, RequestStatus.DRAFT, RequestStatus.SUBMITTED, now)
    }

    fun markUnderReview(requestId: Long, now: Long): Boolean {
        return setStatus(requestId, RequestStatus.SUBMITTED, RequestStatus.UNDER_REVIEW, now)
    }

    fun assign(
        requestId: Long,
        workerId: Long,
        priority: String,
        deadlineMillis: Long,
        internalNotes: String,
        now: Long
    ): Boolean {
        val db = database.writableDatabase
        val v = ContentValues()
        v.put(CareLinkDatabase.COL_REQ_STATUS, RequestStatus.ASSIGNED)
        v.put(CareLinkDatabase.COL_REQ_PRIORITY, priority)
        v.put(CareLinkDatabase.COL_REQ_DEADLINE, deadlineMillis)
        v.put(CareLinkDatabase.COL_REQ_WORKER_ID, workerId)
        v.put(CareLinkDatabase.COL_REQ_INTERNAL_NOTES, internalNotes.trim())
        v.put(CareLinkDatabase.COL_REQ_UPDATED, now)
        // allow assigning from either submitted or under_review
        val rows = db.update(
            CareLinkDatabase.TABLE_REQUESTS, v,
            "${CareLinkDatabase.COL_REQ_ID} = ? AND " +
                "${CareLinkDatabase.COL_REQ_STATUS} IN (?, ?)",
            arrayOf(
                requestId.toString(),
                RequestStatus.SUBMITTED,
                RequestStatus.UNDER_REVIEW
            )
        )
        return rows > 0
    }

    fun markVisitCompleted(requestId: Long, now: Long): Boolean {
        return setStatus(requestId, RequestStatus.ASSIGNED, RequestStatus.VISIT_COMPLETED, now)
    }

    fun verify(requestId: Long, now: Long): Boolean {
        return setStatus(requestId, RequestStatus.VISIT_COMPLETED, RequestStatus.VERIFIED, now)
    }

    // also bumps priority - covers the "priority override" extra feature
    fun escalate(requestId: Long, newPriority: String, now: Long): Boolean {
        val db = database.writableDatabase
        val v = ContentValues()
        v.put(CareLinkDatabase.COL_REQ_STATUS, RequestStatus.ESCALATED)
        v.put(CareLinkDatabase.COL_REQ_PRIORITY, newPriority)
        v.put(CareLinkDatabase.COL_REQ_UPDATED, now)
        val rows = db.update(
            CareLinkDatabase.TABLE_REQUESTS, v,
            "${CareLinkDatabase.COL_REQ_ID} = ? AND ${CareLinkDatabase.COL_REQ_STATUS} = ?",
            arrayOf(requestId.toString(), RequestStatus.VISIT_COMPLETED)
        )
        return rows > 0
    }

    // free-text tag the coordinator can use to bundle related requests
    fun setGroupTag(requestId: Long, groupTag: String, now: Long): Boolean {
        val db = database.writableDatabase
        val v = ContentValues()
        v.put(CareLinkDatabase.COL_REQ_GROUP_TAG, groupTag.trim())
        v.put(CareLinkDatabase.COL_REQ_UPDATED, now)
        val rows = db.update(
            CareLinkDatabase.TABLE_REQUESTS, v,
            "${CareLinkDatabase.COL_REQ_ID} = ?",
            arrayOf(requestId.toString())
        )
        return rows > 0
    }

    // ----- read queries -----

    fun findById(requestId: Long): WelfareRequest? {
        val db = database.readableDatabase
        val query =
            "SELECT * FROM ${CareLinkDatabase.TABLE_REQUESTS} " +
                "WHERE ${CareLinkDatabase.COL_REQ_ID} = ? LIMIT 1"
        val cursor = db.rawQuery(query, arrayOf(requestId.toString()))
        var request: WelfareRequest? = null
        if (cursor.moveToFirst()) {
            request = cursorToRequest(cursor)
        }
        cursor.close()
        return request
    }

    fun findByResident(residentId: Long): List<WelfareRequest> {
        val query =
            "SELECT * FROM ${CareLinkDatabase.TABLE_REQUESTS} " +
                "WHERE ${CareLinkDatabase.COL_REQ_RESIDENT_ID} = ? " +
                "ORDER BY ${CareLinkDatabase.COL_REQ_UPDATED} DESC"
        return runListQuery(query, arrayOf(residentId.toString()))
    }

    // coordinator inbox - everything except drafts. optional filters chained on
    fun findInbox(statusFilter: String?, priorityFilter: String?): List<WelfareRequest> {
        val sql = StringBuilder(
            "SELECT * FROM ${CareLinkDatabase.TABLE_REQUESTS} " +
                "WHERE ${CareLinkDatabase.COL_REQ_STATUS} != ?"
        )
        val args = mutableListOf(RequestStatus.DRAFT)
        if (statusFilter != null) {
            sql.append(" AND ${CareLinkDatabase.COL_REQ_STATUS} = ?")
            args.add(statusFilter)
        }
        if (priorityFilter != null) {
            sql.append(" AND ${CareLinkDatabase.COL_REQ_PRIORITY} = ?")
            args.add(priorityFilter)
        }
        sql.append(" ORDER BY ${CareLinkDatabase.COL_REQ_UPDATED} DESC")
        return runListQuery(sql.toString(), args.toTypedArray())
    }

    // worker visit list - HIGH first, then earliest deadline
    fun findAssignedToWorker(workerId: Long): List<WelfareRequest> {
        val priorityOrder =
            "CASE ${CareLinkDatabase.COL_REQ_PRIORITY} " +
                "WHEN '${Priority.HIGH}' THEN 0 " +
                "WHEN '${Priority.MEDIUM}' THEN 1 " +
                "WHEN '${Priority.LOW}' THEN 2 ELSE 3 END"
        val query =
            "SELECT * FROM ${CareLinkDatabase.TABLE_REQUESTS} " +
                "WHERE ${CareLinkDatabase.COL_REQ_WORKER_ID} = ? " +
                "AND ${CareLinkDatabase.COL_REQ_STATUS} = ? " +
                "ORDER BY $priorityOrder ASC, " +
                "${CareLinkDatabase.COL_REQ_DEADLINE} ASC"
        return runListQuery(query, arrayOf(workerId.toString(), RequestStatus.ASSIGNED))
    }

    fun findReviewQueue(): List<WelfareRequest> {
        val query =
            "SELECT * FROM ${CareLinkDatabase.TABLE_REQUESTS} " +
                "WHERE ${CareLinkDatabase.COL_REQ_STATUS} = ? " +
                "ORDER BY ${CareLinkDatabase.COL_REQ_UPDATED} ASC"
        return runListQuery(query, arrayOf(RequestStatus.VISIT_COMPLETED))
    }

    // ----- helpers -----

    private fun setStatus(requestId: Long, expected: String, target: String, now: Long): Boolean {
        val db = database.writableDatabase
        val v = ContentValues()
        v.put(CareLinkDatabase.COL_REQ_STATUS, target)
        v.put(CareLinkDatabase.COL_REQ_UPDATED, now)
        val rows = db.update(
            CareLinkDatabase.TABLE_REQUESTS, v,
            "${CareLinkDatabase.COL_REQ_ID} = ? AND ${CareLinkDatabase.COL_REQ_STATUS} = ?",
            arrayOf(requestId.toString(), expected)
        )
        return rows > 0
    }

    private fun runListQuery(query: String, args: Array<String>): List<WelfareRequest> {
        val db = database.readableDatabase
        val cursor = db.rawQuery(query, args)
        val list = mutableListOf<WelfareRequest>()
        while (cursor.moveToNext()) {
            list.add(cursorToRequest(cursor))
        }
        cursor.close()
        return list
    }

    private fun cursorToRequest(c: Cursor): WelfareRequest {
        // nullable columns - check isNull first so an unassigned row stays null
        val deadlineCol = c.getColumnIndexOrThrow(CareLinkDatabase.COL_REQ_DEADLINE)
        val workerCol = c.getColumnIndexOrThrow(CareLinkDatabase.COL_REQ_WORKER_ID)
        val deadline = if (c.isNull(deadlineCol)) null else c.getLong(deadlineCol)
        val workerId = if (c.isNull(workerCol)) null else c.getLong(workerCol)

        return WelfareRequest(
            id = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REQ_ID)),
            residentId = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REQ_RESIDENT_ID)),
            title = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REQ_TITLE)),
            description = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REQ_DESCRIPTION)),
            category = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REQ_CATEGORY)),
            address = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REQ_ADDRESS)),
            notes = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REQ_NOTES)),
            status = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REQ_STATUS)),
            priority = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REQ_PRIORITY)),
            deadlineMillis = deadline,
            assignedWorkerId = workerId,
            internalNotes = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REQ_INTERNAL_NOTES)),
            groupTag = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REQ_GROUP_TAG)),
            createdAt = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REQ_CREATED)),
            updatedAt = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REQ_UPDATED))
        )
    }
}