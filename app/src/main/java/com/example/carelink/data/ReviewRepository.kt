package com.example.carelink.data

import android.content.ContentValues
import android.database.Cursor
import com.example.carelink.data.models.Review

// one row per safeguarding review. each decision is its own row so the
// history of who decided what is preserved.
class ReviewRepository(private val database: CareLinkDatabase) {

    fun insertReview(
        requestId: Long,
        reviewerId: Long,
        decision: String,
        reason: String,
        reviewedAt: Long
    ): Long {
        val db = database.writableDatabase
        val v = ContentValues()
        v.put(CareLinkDatabase.COL_REVIEW_REQUEST_ID, requestId)
        v.put(CareLinkDatabase.COL_REVIEW_REVIEWER_ID, reviewerId)
        v.put(CareLinkDatabase.COL_REVIEW_DECISION, decision)
        v.put(CareLinkDatabase.COL_REVIEW_REASON, reason.trim())
        v.put(CareLinkDatabase.COL_REVIEW_AT, reviewedAt)
        return db.insert(CareLinkDatabase.TABLE_REVIEWS, null, v)
    }

    // backs the "reviewer notes history" extra feature
    fun findAllForRequest(requestId: Long): List<Review> {
        val db = database.readableDatabase
        val query =
            "SELECT * FROM ${CareLinkDatabase.TABLE_REVIEWS} " +
                "WHERE ${CareLinkDatabase.COL_REVIEW_REQUEST_ID} = ? " +
                "ORDER BY ${CareLinkDatabase.COL_REVIEW_AT} DESC"
        val cursor = db.rawQuery(query, arrayOf(requestId.toString()))
        val list = mutableListOf<Review>()
        while (cursor.moveToNext()) {
            list.add(cursorToReview(cursor))
        }
        cursor.close()
        return list
    }

    private fun cursorToReview(c: Cursor): Review {
        return Review(
            id = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REVIEW_ID)),
            requestId = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REVIEW_REQUEST_ID)),
            reviewerId = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REVIEW_REVIEWER_ID)),
            decision = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REVIEW_DECISION)),
            reason = c.getString(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REVIEW_REASON)),
            reviewedAt = c.getLong(c.getColumnIndexOrThrow(CareLinkDatabase.COL_REVIEW_AT))
        )
    }
}