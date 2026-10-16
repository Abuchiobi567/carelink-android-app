package com.example.carelink.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.carelink.data.models.AuditAction
import com.example.carelink.data.models.ConcernCategory
import com.example.carelink.data.models.Priority
import com.example.carelink.data.models.RequestStatus
import com.example.carelink.data.models.Roles
import com.example.carelink.util.PasswordUtils

// schema + column-name constants for every table
class CareLinkDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_USERS)
        db.execSQL(CREATE_REQUESTS)
        db.execSQL(CREATE_VISITS)
        db.execSQL(CREATE_REVIEWS)
        db.execSQL(CREATE_AUDIT_LOG)

        // indexes for the columns the brief lists under "Performance"
        db.execSQL("CREATE INDEX idx_requests_status ON $TABLE_REQUESTS($COL_REQ_STATUS)")
        db.execSQL("CREATE INDEX idx_requests_deadline ON $TABLE_REQUESTS($COL_REQ_DEADLINE)")
        db.execSQL("CREATE INDEX idx_requests_worker ON $TABLE_REQUESTS($COL_REQ_WORKER_ID)")
        db.execSQL("CREATE INDEX idx_users_role ON $TABLE_USERS($COL_USER_ROLE)")

        // wrap all seed inserts in one transaction — 50× faster than auto-commit
        db.beginTransaction()
        try {
            seedStaffAccounts(db)
            seedDemoResident(db)
            seedSecondWorker(db)
            seedSecondResident(db)
            seedWelfareRequests(db)
            seedSecondResidentRequests(db)
            seedVisits(db)
            seedReviews(db)
            seedAuditLog(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // simple drop-and-recreate
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_AUDIT_LOG")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REVIEWS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_VISITS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REQUESTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // demo staff accounts so the marker can sign in immediately
    private fun seedStaffAccounts(db: SQLiteDatabase) {
        val seed = listOf(
            Triple("Coordinator Demo", "coordinator@carelink.test", Roles.COORDINATOR),
            Triple("Worker Demo", "worker@carelink.test", Roles.WORKER),
            Triple("Reviewer Demo", "reviewer@carelink.test", Roles.REVIEWER)
        )
        for (s in seed) {
            val v = ContentValues()
            v.put(COL_USER_NAME, s.first)
            v.put(COL_USER_EMAIL, s.second)
            v.put(COL_USER_PASSWORD, PasswordUtils.hash("password123"))
            v.put(COL_USER_ROLE, s.third)
            db.insert(TABLE_USERS, null, v)
        }
    }

    // demo resident so the coordinator/worker/reviewer screens have requests to show
    // immediately — logs in as resident@carelink.test / password123
    private fun seedDemoResident(db: SQLiteDatabase) {
        val v = ContentValues()
        v.put(COL_USER_NAME, "Margaret Thompson")
        v.put(COL_USER_EMAIL, "resident@carelink.test")
        v.put(COL_USER_PASSWORD, PasswordUtils.hash("password123"))
        v.put(COL_USER_ROLE, Roles.RESIDENT)
        db.insert(TABLE_USERS, null, v)
        // Margaret gets userId = 4 (three staff rows are inserted first)
    }

    // second support worker so the coordinator can demonstrate assigning to
    // different workers and the worker inbox shows variety
    private fun seedSecondWorker(db: SQLiteDatabase) {
        val v = ContentValues()
        v.put(COL_USER_NAME, "Jane Foster")
        v.put(COL_USER_EMAIL, "worker2@carelink.test")
        v.put(COL_USER_PASSWORD, PasswordUtils.hash("password123"))
        v.put(COL_USER_ROLE, Roles.WORKER)
        db.insert(TABLE_USERS, null, v)
        // Jane gets userId = 5
    }

    // second resident so the coordinator inbox shows requests from two different
    // people and the demo looks like a real multi-user system
    private fun seedSecondResident(db: SQLiteDatabase) {
        val v = ContentValues()
        v.put(COL_USER_NAME, "Robert Davies")
        v.put(COL_USER_EMAIL, "resident2@carelink.test")
        v.put(COL_USER_PASSWORD, PasswordUtils.hash("password123"))
        v.put(COL_USER_ROLE, Roles.RESIDENT)
        db.insert(TABLE_USERS, null, v)
        // Robert gets userId = 6
    }

    // eight requests spread across every status so every role has something
    // to interact with on first launch
    private fun seedWelfareRequests(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val residentId = 4L  // Margaret Thompson, inserted by seedDemoResident
        val workerId = 2L    // Worker Demo, second row in seedStaffAccounts

        // helper so each request needs fewer lines
        fun req(
            title: String,
            description: String,
            category: String,
            address: String,
            notes: String,
            status: String,
            priority: String,
            deadlineMillis: Long?,
            assignedWorkerId: Long?,
            internalNotes: String,
            groupTag: String,
            createdAt: Long,
            updatedAt: Long
        ) {
            val v = ContentValues()
            v.put(COL_REQ_RESIDENT_ID, residentId)
            v.put(COL_REQ_TITLE, title)
            v.put(COL_REQ_DESCRIPTION, description)
            v.put(COL_REQ_CATEGORY, category)
            v.put(COL_REQ_ADDRESS, address)
            v.put(COL_REQ_NOTES, notes)
            v.put(COL_REQ_STATUS, status)
            v.put(COL_REQ_PRIORITY, priority)
            if (deadlineMillis != null) v.put(COL_REQ_DEADLINE, deadlineMillis) else v.putNull(COL_REQ_DEADLINE)
            if (assignedWorkerId != null) v.put(COL_REQ_WORKER_ID, assignedWorkerId) else v.putNull(COL_REQ_WORKER_ID)
            v.put(COL_REQ_INTERNAL_NOTES, internalNotes)
            v.put(COL_REQ_GROUP_TAG, groupTag)
            v.put(COL_REQ_CREATED, createdAt)
            v.put(COL_REQ_UPDATED, updatedAt)
            db.insert(TABLE_REQUESTS, null, v)
        }

        // ── SUBMITTED ────────────────────────────────────────────────────────
        req(
            title = "Father not answering calls",
            description = "My father Arthur (82) has not answered his phone for two days. " +
                "Neighbours say they have not seen him. He lives alone and has a heart condition.",
            category = ConcernCategory.MISSED,
            address = "12 Elm Street, Northampton, NN1 4AB",
            notes = "Spare key with next-door neighbour at No. 14. GP is Dr Patel at Northampton Surgery.",
            status = RequestStatus.SUBMITTED,
            priority = Priority.HIGH,
            deadlineMillis = now - day,           // already overdue → red chip in demo
            assignedWorkerId = null,
            internalNotes = "",
            groupTag = "",
            createdAt = now - 2 * day,
            updatedAt = now - 2 * day
        )
        req(
            title = "Weekly wellbeing check — Doris",
            description = "Routine weekly check for Doris (78). She has been feeling low since " +
                "her husband passed last month and her daughter is concerned.",
            category = ConcernCategory.ROUTINE,
            address = "7 Oak Avenue, Northampton, NN2 7LR",
            notes = "Prefers morning visits before 11 am. She has a cat — please be aware.",
            status = RequestStatus.SUBMITTED,
            priority = Priority.MEDIUM,
            deadlineMillis = now + 3 * day,
            assignedWorkerId = null,
            internalNotes = "",
            groupTag = "",
            createdAt = now - day,
            updatedAt = now - day
        )

        // ── UNDER REVIEW ─────────────────────────────────────────────────────
        req(
            title = "Confused and distressed — urgent",
            description = "Resident appeared confused and distressed during my call. " +
                "She kept asking for her late husband and did not recognise my voice at first.",
            category = ConcernCategory.WELLBEING,
            address = "3 Maple Close, Northampton, NN3 2CD",
            notes = "Power of attorney held by son James (07700 900321). Similar episode in March.",
            status = RequestStatus.UNDER_REVIEW,
            priority = Priority.HIGH,
            deadlineMillis = now + day,
            assignedWorkerId = null,
            internalNotes = "Flagged for priority review — possible dementia escalation.",
            groupTag = "",
            createdAt = now - 3 * day,
            updatedAt = now - 2 * day
        )

        // ── ASSIGNED (worker sees these in My Visits) ─────────────────────────
        req(
            title = "No response since Monday",
            description = "Resident George (75) has not responded to three scheduled calls " +
                "since Monday. His carer is abroad and cannot be reached until Thursday.",
            category = ConcernCategory.MISSED,
            address = "18 Birch Road, Northampton, NN4 5FG",
            notes = "Emergency contact: niece Susan on 07700 900456. Friendly Labrador on site.",
            status = RequestStatus.ASSIGNED,
            priority = Priority.HIGH,
            deadlineMillis = now - (day / 2),     // overdue
            assignedWorkerId = workerId,
            internalNotes = "Visit 9–11 am. Neighbour at No. 16 holds a spare key.",
            groupTag = "missed-contact-june",
            createdAt = now - 4 * day,
            updatedAt = now - day
        )
        req(
            title = "Routine monthly visit — Edna",
            description = "Monthly welfare visit for Edna (81). She is mobile but increasingly " +
                "forgetful. Family want a general check on her home environment and mood.",
            category = ConcernCategory.ROUTINE,
            address = "22 Cedar Lane, Northampton, NN1 9HJ",
            notes = "Allow at least 45 minutes — Edna loves a chat and usually has biscuits ready.",
            status = RequestStatus.ASSIGNED,
            priority = Priority.MEDIUM,
            deadlineMillis = now + 2 * day,
            assignedWorkerId = workerId,
            internalNotes = "Part of the Cedar Lane cluster — see group tag.",
            groupTag = "cedar-lane-cluster",
            createdAt = now - 5 * day,
            updatedAt = now - 2 * day
        )

        // ── VISIT COMPLETED (reviewer queue) ─────────────────────────────────
        req(
            title = "Post-discharge wellbeing check",
            description = "Welfare visit following hip-replacement surgery. Checking resident " +
                "is managing at home, has adequate support, and is taking medication correctly.",
            category = ConcernCategory.WELLBEING,
            address = "5 Pine Avenue, Northampton, NN2 3KL",
            notes = "Hospital discharge summary in physical file. OT assessment still pending.",
            status = RequestStatus.VISIT_COMPLETED,
            priority = Priority.MEDIUM,
            deadlineMillis = now - 2 * day,
            assignedWorkerId = workerId,
            internalNotes = "Re-assigned after escalation. District nurse confirmed medication resolved. Second visit completed — recommend verification.",
            groupTag = "",
            createdAt = now - 14 * day,
            updatedAt = now - day
        )

        // ── VERIFIED (happy-path end state) ──────────────────────────────────
        req(
            title = "Routine quarterly check — Harold",
            description = "Quarterly check for Harold (79). Mobile, independent, no current concerns. " +
                "Standard schedule visit.",
            category = ConcernCategory.ROUTINE,
            address = "9 Willow Street, Northampton, NN3 8MN",
            notes = "Harold volunteers at the local library on Tuesdays — not available before 2 pm that day.",
            status = RequestStatus.VERIFIED,
            priority = Priority.LOW,
            deadlineMillis = now - 5 * day,
            assignedWorkerId = workerId,
            internalNotes = "Closed: all well. No further action required.",
            groupTag = "",
            createdAt = now - 14 * day,
            updatedAt = now - 4 * day
        )

        // ── ESCALATED ────────────────────────────────────────────────────────
        req(
            title = "Safeguarding concern — possible financial abuse",
            description = "During a routine visit the worker noticed several unfamiliar individuals " +
                "at the property and a large quantity of cash on the table. Resident seemed anxious " +
                "and reluctant to speak freely.",
            category = ConcernCategory.WELLBEING,
            address = "14 Ash Grove, Northampton, NN4 1PQ",
            notes = "Resident has no close family. Previously flagged as vulnerable to doorstep fraud.",
            status = RequestStatus.ESCALATED,
            priority = Priority.HIGH,
            deadlineMillis = now - 3 * day,
            assignedWorkerId = workerId,
            internalNotes = "ESCALATED: Safeguarding risk identified. Referred to adult social care. Police informed.",
            groupTag = "safeguarding-june",
            createdAt = now - 10 * day,
            updatedAt = now - 3 * day
        )
    }

    // four more requests from Robert Davies (userId = 6, inserted by seedSecondResident)
    // covering all the major statuses so the coordinator inbox is clearly multi-resident
    private fun seedSecondResidentRequests(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val residentId = 6L   // Robert Davies
        val worker1Id = 2L    // Worker Demo
        val worker2Id = 5L    // Jane Foster

        fun req(
            title: String,
            description: String,
            category: String,
            address: String,
            notes: String,
            status: String,
            priority: String,
            deadlineMillis: Long?,
            assignedWorkerId: Long?,
            internalNotes: String,
            groupTag: String,
            createdAt: Long,
            updatedAt: Long
        ) {
            val v = ContentValues()
            v.put(COL_REQ_RESIDENT_ID, residentId)
            v.put(COL_REQ_TITLE, title)
            v.put(COL_REQ_DESCRIPTION, description)
            v.put(COL_REQ_CATEGORY, category)
            v.put(COL_REQ_ADDRESS, address)
            v.put(COL_REQ_NOTES, notes)
            v.put(COL_REQ_STATUS, status)
            v.put(COL_REQ_PRIORITY, priority)
            if (deadlineMillis != null) v.put(COL_REQ_DEADLINE, deadlineMillis) else v.putNull(COL_REQ_DEADLINE)
            if (assignedWorkerId != null) v.put(COL_REQ_WORKER_ID, assignedWorkerId) else v.putNull(COL_REQ_WORKER_ID)
            v.put(COL_REQ_INTERNAL_NOTES, internalNotes)
            v.put(COL_REQ_GROUP_TAG, groupTag)
            v.put(COL_REQ_CREATED, createdAt)
            v.put(COL_REQ_UPDATED, updatedAt)
            db.insert(TABLE_REQUESTS, null, v)
        }

        // ── Request 9: SUBMITTED (new, unassigned) ────────────────────────────
        req(
            title = "Mother not answering calls — two days",
            description = "My mother Vera (84) has not picked up the phone for two days. " +
                "She lives alone in sheltered housing and has a history of falls. I am very " +
                "concerned as she usually calls me every morning.",
            category = ConcernCategory.MISSED,
            address = "4 Hawthorn Court, Northampton, NN2 5TR",
            notes = "Warden for the sheltered housing block is Alan (07700 900812). " +
                "Vera has a fall detector but it has not been triggered.",
            status = RequestStatus.SUBMITTED,
            priority = Priority.HIGH,
            deadlineMillis = now + day,
            assignedWorkerId = null,
            internalNotes = "",
            groupTag = "",
            createdAt = now - day,
            updatedAt = now - day
        )

        // ── Request 10: ASSIGNED to Jane Foster (shows in her My Visits) ─────
        req(
            title = "Post-op recovery check — Vera",
            description = "Follow-up welfare visit after Vera's cataract surgery last week. " +
                "She lives alone and her son Robert is worried she may not be managing " +
                "her eye-drops correctly.",
            category = ConcernCategory.WELLBEING,
            address = "4 Hawthorn Court, Northampton, NN2 5TR",
            notes = "Eye-drop schedule is three times daily. Instructions from the hospital " +
                "discharge letter are pinned to the fridge.",
            status = RequestStatus.ASSIGNED,
            priority = Priority.MEDIUM,
            deadlineMillis = now + 2 * day,
            assignedWorkerId = worker2Id,
            internalNotes = "Assigned to Jane Foster — she covers the Hawthorn Court area.",
            groupTag = "",
            createdAt = now - 3 * day,
            updatedAt = now - day
        )

        // ── Request 11: VISIT_COMPLETED — sits in the reviewer queue ─────────
        req(
            title = "Environmental hazard concern — damp and mould",
            description = "Worker visit found significant damp and mould in the bathroom " +
                "and bedroom of resident Brian (77). Poor ventilation and suspected roof " +
                "leak. Resident has mild asthma and the environment poses a health risk.",
            category = ConcernCategory.WELLBEING,
            address = "31 Sycamore Drive, Northampton, NN4 8RT",
            notes = "Landlord is Northampton Council Housing. Tenant reference: NHC-2241. " +
                "Resident has an inhaler but it expired two months ago.",
            status = RequestStatus.VISIT_COMPLETED,
            priority = Priority.HIGH,
            deadlineMillis = now - day,
            assignedWorkerId = worker1Id,
            internalNotes = "Worker raised environmental hazard flag on site. Photos taken. " +
                "Recommend escalation to council housing team and pharmacy referral.",
            groupTag = "",
            createdAt = now - 6 * day,
            updatedAt = now - day
        )

        // ── Request 12: VERIFIED — completed happy path ───────────────────────
        req(
            title = "Routine bi-monthly check — Brian",
            description = "Scheduled bi-monthly welfare check for Brian (77). General " +
                "wellbeing, home environment, and social connections assessment.",
            category = ConcernCategory.ROUTINE,
            address = "31 Sycamore Drive, Northampton, NN4 8RT",
            notes = "Brian is hard of hearing — please speak clearly and ensure hearing aid " +
                "is in. He usually has tea on the go.",
            status = RequestStatus.VERIFIED,
            priority = Priority.LOW,
            deadlineMillis = now - 7 * day,
            assignedWorkerId = worker2Id,
            internalNotes = "Closed: all well. No further action required.",
            groupTag = "",
            createdAt = now - 20 * day,
            updatedAt = now - 6 * day
        )
    }

    // seed review decisions for the three already-resolved requests so every
    // reviewer-facing screen has real data to display on first launch
    private fun seedReviews(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val reviewerId = 3L  // Reviewer Demo, third row in seedStaffAccounts

        fun review(requestId: Long, decision: String, reason: String, reviewedAt: Long) {
            val v = ContentValues()
            v.put(COL_REVIEW_REQUEST_ID, requestId)
            v.put(COL_REVIEW_REVIEWER_ID, reviewerId)
            v.put(COL_REVIEW_DECISION, decision)
            v.put(COL_REVIEW_REASON, reason)
            v.put(COL_REVIEW_AT, reviewedAt)
            db.insert(TABLE_REVIEWS, null, v)
        }

        // Request 6 is VISIT_COMPLETED and sits in the reviewer's queue.
        // Adding a prior ESCALATED review here means the "Previous reviews"
        // section is visible when the reviewer opens it — demonstrating the
        // Reviewer Notes History extra feature.
        review(
            requestId = 6L,
            decision = RequestStatus.ESCALATED,
            reason = "Medical concern - Worker noted resident was confused about medication " +
                "dosage and had missed two days. Referred to district nurse for follow-up " +
                "before re-visit.",
            reviewedAt = now - 8 * day
        )

        // Request 7 (Harold) — full happy-path end state: VERIFIED
        review(
            requestId = 7L,
            decision = RequestStatus.VERIFIED,
            reason = "",
            reviewedAt = now - 4 * day
        )

        // Request 8 (Safeguarding concern) — ESCALATED with a structured reason
        review(
            requestId = 8L,
            decision = RequestStatus.ESCALATED,
            reason = "Safeguarding risk identified - Multiple unknown individuals present at " +
                "the property, large quantity of cash visible on table. Resident appeared " +
                "anxious and would not speak freely. Case referred to adult social care " +
                "and police notified.",
            reviewedAt = now - 3 * day
        )

        // Request 12 (Brian routine check) — VERIFIED, Jane Foster's visit
        review(
            requestId = 12L,
            decision = RequestStatus.VERIFIED,
            reason = "",
            reviewedAt = now - 6 * day
        )
    }

    // visit records for every request that reached VISIT_COMPLETED or beyond.
    // request 6 gets two visits (first triggered the escalation, second is current).
    private fun seedVisits(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val minute = 60_000L

        fun visit(requestId: Long, workerIdParam: Long, startedAt: Long?, completedAt: Long, notes: String) {
            val v = ContentValues()
            v.put(COL_VISIT_REQUEST_ID, requestId)
            v.put(COL_VISIT_WORKER_ID, workerIdParam)
            v.put(COL_VISIT_NOTES, notes)
            if (startedAt != null) v.put(COL_VISIT_STARTED, startedAt) else v.putNull(COL_VISIT_STARTED)
            v.put(COL_VISIT_COMPLETED, completedAt)
            db.insert(TABLE_VISITS, null, v)
        }

        // Request 6 — first visit (raised the medical concern, triggered escalation)
        val r6first = now - 9 * day
        visit(
            requestId = 6L,
            workerIdParam = 2L,
            startedAt = r6first - 45 * minute,
            completedAt = r6first,
            notes = "Resident appeared confused and was unable to recall her discharge " +
                "medication schedule. District nurse had not yet followed up. Home " +
                "environment adequate but resident visibly anxious. Raising for safeguarding " +
                "review — medical concern."
        )
        // Request 6 — second visit (after re-referral; now awaiting verification)
        val r6second = now - 2 * day
        visit(
            requestId = 6L,
            workerIdParam = 2L,
            startedAt = r6second - 35 * minute,
            completedAt = r6second,
            notes = "Second visit following escalation and district nurse follow-up. Resident " +
                "much improved — medication blister pack correctly filled and labelled. Home " +
                "clean and safe, no outstanding concerns. Recommend verification."
        )

        // Request 7 (VERIFIED — Harold)
        val r7completed = now - 6 * day
        visit(
            requestId = 7L,
            workerIdParam = 2L,
            startedAt = r7completed - 30 * minute,
            completedAt = r7completed,
            notes = "Harold in excellent spirits. Had just returned from his Tuesday library " +
                "shift. Home tidy, fridge well-stocked, neighbours checking in regularly. " +
                "No concerns identified. All welfare indicators positive."
        )

        // Request 8 (ESCALATED — Safeguarding)
        val r8completed = now - 5 * day
        visit(
            requestId = 8L,
            workerIdParam = 2L,
            startedAt = r8completed - 25 * minute,
            completedAt = r8completed,
            notes = "On arrival found two unknown males and a woman at the property. A large " +
                "quantity of cash was visible on the kitchen table. Resident appeared anxious " +
                "and was reluctant to speak freely. Asked the individuals to step outside; " +
                "resident then indicated she felt pressured. Ended visit immediately and " +
                "escalated to safeguarding team. Police and adult social care notified."
        )

        // Request 11 (VISIT_COMPLETED — environmental hazard, Robert's request)
        val r11completed = now - day
        visit(
            requestId = 11L,
            workerIdParam = 2L,
            startedAt = r11completed - 40 * minute,
            completedAt = r11completed,
            notes = "Found significant black mould on the bedroom wall and ceiling, and damp " +
                "patches in the bathroom. Window frame in the bedroom is rotting — suspected " +
                "cause of the roof leak. Resident's inhaler is expired. Took three photographs. " +
                "Advised resident not to sleep in the bedroom until remediated. Escalating to " +
                "council housing and recommending pharmacy referral for a new inhaler."
        )

        // Request 12 (VERIFIED — Brian routine check, Jane Foster's visit)
        val r12completed = now - 8 * day
        visit(
            requestId = 12L,
            workerIdParam = 5L,
            startedAt = r12completed - 35 * minute,
            completedAt = r12completed,
            notes = "Brian answered the door promptly — hearing aid in and working well. Home " +
                "clean and tidy, food in the fridge, no hazards noted. He mentioned his son " +
                "visits every weekend. Mood upbeat. No welfare concerns at this time. " +
                "Recommend routine schedule to continue as normal."
        )
    }

    // audit trail covering the full history of every seeded request so the
    // audit log screen has a realistic, populated feed on first launch
    private fun seedAuditLog(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val hour = 3_600_000L
        val minute = 60_000L

        val coordinatorId = 1L
        val workerId = 2L
        val reviewerId = 3L
        val worker2Id = 5L
        val residentId = 4L    // Margaret Thompson
        val resident2Id = 6L   // Robert Davies

        fun audit(actor: Long, action: String, target: Long?, details: String, at: Long) {
            val v = ContentValues()
            v.put(COL_AUDIT_ACTOR, actor)
            v.put(COL_AUDIT_ACTION, action)
            if (target != null) v.put(COL_AUDIT_TARGET, target) else v.putNull(COL_AUDIT_TARGET)
            v.put(COL_AUDIT_TIMESTAMP, at)
            v.put(COL_AUDIT_DETAILS, details)
            db.insert(TABLE_AUDIT_LOG, null, v)
        }

        // Margaret registers on first use
        audit(residentId, AuditAction.REGISTER, null, "", now - 14 * day)
        audit(residentId, AuditAction.LOGIN, null, "", now - 14 * day + 30 * minute)

        // ── Request 6 (Post-discharge) — longest history ─────────────────────
        audit(residentId, AuditAction.DRAFT_CREATED, 6L, "", now - 14 * day + hour)
        audit(residentId, AuditAction.DRAFT_UPDATED, 6L, "", now - 14 * day + 2 * hour)
        audit(residentId, AuditAction.REQUEST_SUBMITTED, 6L, "", now - 14 * day + 3 * hour)
        audit(coordinatorId, AuditAction.LOGIN, null, "", now - 13 * day)
        audit(coordinatorId, AuditAction.REQUEST_ASSIGNED, 6L,
            "Assigned to Worker Demo. Priority: MEDIUM.", now - 13 * day + 2 * hour)
        audit(workerId, AuditAction.LOGIN, null, "", now - 9 * day)
        audit(workerId, AuditAction.VISIT_COMPLETED, 6L,
            "On site 45 minutes.", now - 9 * day + 45 * minute)
        audit(reviewerId, AuditAction.LOGIN, null, "", now - 8 * day)
        audit(reviewerId, AuditAction.REQUEST_ESCALATED, 6L,
            "Medical concern - Resident unable to manage post-discharge medication. Referred to district nurse.",
            now - 8 * day + hour)
        audit(coordinatorId, AuditAction.REQUEST_ASSIGNED, 6L,
            "Re-assigned following escalation. District nurse follow-up confirmed.",
            now - 7 * day)
        audit(workerId, AuditAction.VISIT_COMPLETED, 6L,
            "On site 35 minutes. Resident recovered.", now - 2 * day)

        // ── Request 7 (Harold) ────────────────────────────────────────────────
        audit(residentId, AuditAction.DRAFT_CREATED, 7L, "", now - 14 * day + 4 * hour)
        audit(residentId, AuditAction.REQUEST_SUBMITTED, 7L, "", now - 14 * day + 5 * hour)
        audit(coordinatorId, AuditAction.REQUEST_ASSIGNED, 7L,
            "Assigned to Worker Demo. Priority: LOW. Routine schedule.", now - 11 * day)
        audit(workerId, AuditAction.VISIT_COMPLETED, 7L,
            "On site 30 minutes. No concerns.", now - 6 * day)
        audit(reviewerId, AuditAction.REQUEST_VERIFIED, 7L, "", now - 4 * day)

        // ── Request 8 (Safeguarding) ──────────────────────────────────────────
        audit(residentId, AuditAction.DRAFT_CREATED, 8L, "", now - 10 * day)
        audit(residentId, AuditAction.REQUEST_SUBMITTED, 8L, "", now - 10 * day + hour)
        audit(coordinatorId, AuditAction.REQUEST_ASSIGNED, 8L,
            "Assigned to Worker Demo. Priority: HIGH.", now - 9 * day)
        audit(workerId, AuditAction.VISIT_COMPLETED, 8L,
            "On site 25 minutes. Escalation raised on site.", now - 5 * day)
        audit(reviewerId, AuditAction.REQUEST_ESCALATED, 8L,
            "Safeguarding risk identified - Police and adult social care notified.",
            now - 3 * day)

        // ── Request 4 (No response since Monday) ─────────────────────────────
        audit(residentId, AuditAction.DRAFT_CREATED, 4L, "", now - 4 * day)
        audit(residentId, AuditAction.REQUEST_SUBMITTED, 4L, "", now - 4 * day + hour)
        audit(coordinatorId, AuditAction.REQUEST_ASSIGNED, 4L,
            "Assigned to Worker Demo. Priority: HIGH. Overdue response — urgent.",
            now - 3 * day)

        // ── Request 5 (Edna) ─────────────────────────────────────────────────
        audit(residentId, AuditAction.DRAFT_CREATED, 5L, "", now - 5 * day)
        audit(residentId, AuditAction.REQUEST_SUBMITTED, 5L, "", now - 5 * day + 2 * hour)
        audit(coordinatorId, AuditAction.REQUEST_ASSIGNED, 5L,
            "Assigned to Worker Demo. Priority: MEDIUM.", now - 4 * day)

        // ── Request 3 (Confused and distressed) ──────────────────────────────
        audit(residentId, AuditAction.DRAFT_CREATED, 3L, "", now - 3 * day)
        audit(residentId, AuditAction.REQUEST_SUBMITTED, 3L, "", now - 3 * day + hour)
        audit(coordinatorId, AuditAction.REQUEST_UNDER_REVIEW, 3L,
            "Flagged for priority review — possible dementia escalation.", now - 2 * day)

        // ── Request 1 (Father not answering) ─────────────────────────────────
        audit(residentId, AuditAction.DRAFT_CREATED, 1L, "", now - 2 * day)
        audit(residentId, AuditAction.REQUEST_SUBMITTED, 1L, "", now - 2 * day + 2 * hour)

        // ── Request 2 (Doris) ─────────────────────────────────────────────────
        audit(residentId, AuditAction.DRAFT_CREATED, 2L, "", now - day)
        audit(residentId, AuditAction.REQUEST_SUBMITTED, 2L, "", now - day + 30 * minute)

        // ── Robert Davies (resident2) — requests 9–12 ─────────────────────────
        audit(resident2Id, AuditAction.REGISTER, null, "", now - 20 * day)
        audit(resident2Id, AuditAction.LOGIN, null, "", now - 20 * day + 30 * minute)

        // Request 12 (Brian routine check) — oldest, full happy path
        audit(resident2Id, AuditAction.DRAFT_CREATED, 12L, "", now - 20 * day + hour)
        audit(resident2Id, AuditAction.REQUEST_SUBMITTED, 12L, "", now - 20 * day + 2 * hour)
        audit(coordinatorId, AuditAction.REQUEST_ASSIGNED, 12L,
            "Assigned to Jane Foster. Priority: LOW. Routine schedule.", now - 18 * day)
        audit(worker2Id, AuditAction.VISIT_COMPLETED, 12L,
            "On site 35 minutes. No concerns.", now - 8 * day)
        audit(reviewerId, AuditAction.REQUEST_VERIFIED, 12L, "", now - 6 * day)

        // Request 11 (Environmental hazard — Brian)
        audit(resident2Id, AuditAction.DRAFT_CREATED, 11L, "", now - 6 * day)
        audit(resident2Id, AuditAction.REQUEST_SUBMITTED, 11L, "", now - 6 * day + hour)
        audit(coordinatorId, AuditAction.REQUEST_ASSIGNED, 11L,
            "Assigned to Worker Demo. Priority: HIGH. Environmental concern.", now - 5 * day)
        audit(workerId, AuditAction.VISIT_COMPLETED, 11L,
            "On site 40 minutes. Mould and damp found. Escalation recommended.", now - day)

        // Request 10 (Post-op check — assigned to Jane Foster)
        audit(resident2Id, AuditAction.DRAFT_CREATED, 10L, "", now - 3 * day)
        audit(resident2Id, AuditAction.REQUEST_SUBMITTED, 10L, "", now - 3 * day + hour)
        audit(coordinatorId, AuditAction.REQUEST_ASSIGNED, 10L,
            "Assigned to Jane Foster. Priority: MEDIUM. Hawthorn Court area.", now - 2 * day)

        // Request 9 (Mother not answering — just submitted)
        audit(resident2Id, AuditAction.DRAFT_CREATED, 9L, "", now - day)
        audit(resident2Id, AuditAction.REQUEST_SUBMITTED, 9L, "", now - day + 30 * minute)

        // Recent logins — makes the audit log look active for the demo
        audit(coordinatorId, AuditAction.LOGIN, null, "", now - day)
        audit(residentId, AuditAction.LOGIN, null, "", now - day + hour)
        audit(workerId, AuditAction.LOGIN, null, "", now - 12 * hour)
        audit(reviewerId, AuditAction.LOGIN, null, "", now - 6 * hour)
        audit(resident2Id, AuditAction.LOGIN, null, "", now - 3 * hour)
        audit(worker2Id, AuditAction.LOGIN, null, "", now - hour)
    }

    companion object {
        const val DATABASE_NAME = "carelink.db"
        const val DATABASE_VERSION = 6

        // users
        const val TABLE_USERS = "users"
        const val COL_USER_ID = "id"
        const val COL_USER_NAME = "name"
        const val COL_USER_EMAIL = "email"
        const val COL_USER_PASSWORD = "password"
        const val COL_USER_ROLE = "role"

        private const val CREATE_USERS =
            "CREATE TABLE $TABLE_USERS (" +
                "$COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_USER_NAME TEXT NOT NULL, " +
                "$COL_USER_EMAIL TEXT NOT NULL UNIQUE, " +
                "$COL_USER_PASSWORD TEXT NOT NULL, " +
                "$COL_USER_ROLE TEXT NOT NULL)"

        // welfare_requests
        const val TABLE_REQUESTS = "welfare_requests"
        const val COL_REQ_ID = "id"
        const val COL_REQ_RESIDENT_ID = "resident_id"
        const val COL_REQ_TITLE = "title"
        const val COL_REQ_DESCRIPTION = "description"
        const val COL_REQ_CATEGORY = "category"
        const val COL_REQ_ADDRESS = "address"
        const val COL_REQ_NOTES = "notes"
        const val COL_REQ_STATUS = "status"
        const val COL_REQ_PRIORITY = "priority"
        const val COL_REQ_DEADLINE = "deadline_millis"
        const val COL_REQ_WORKER_ID = "assigned_worker_id"
        const val COL_REQ_INTERNAL_NOTES = "internal_notes"
        const val COL_REQ_GROUP_TAG = "group_tag"
        const val COL_REQ_CREATED = "created_at"
        const val COL_REQ_UPDATED = "updated_at"

        private const val CREATE_REQUESTS =
            "CREATE TABLE $TABLE_REQUESTS (" +
                "$COL_REQ_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_REQ_RESIDENT_ID INTEGER NOT NULL, " +
                "$COL_REQ_TITLE TEXT NOT NULL, " +
                "$COL_REQ_DESCRIPTION TEXT NOT NULL, " +
                "$COL_REQ_CATEGORY TEXT NOT NULL, " +
                "$COL_REQ_ADDRESS TEXT NOT NULL, " +
                "$COL_REQ_NOTES TEXT NOT NULL, " +
                "$COL_REQ_STATUS TEXT NOT NULL, " +
                "$COL_REQ_PRIORITY TEXT NOT NULL, " +
                "$COL_REQ_DEADLINE INTEGER, " +
                "$COL_REQ_WORKER_ID INTEGER, " +
                "$COL_REQ_INTERNAL_NOTES TEXT NOT NULL, " +
                "$COL_REQ_GROUP_TAG TEXT NOT NULL, " +
                "$COL_REQ_CREATED INTEGER NOT NULL, " +
                "$COL_REQ_UPDATED INTEGER NOT NULL)"

        // visits
        const val TABLE_VISITS = "visits"
        const val COL_VISIT_ID = "id"
        const val COL_VISIT_REQUEST_ID = "request_id"
        const val COL_VISIT_WORKER_ID = "worker_id"
        const val COL_VISIT_NOTES = "outcome_notes"
        const val COL_VISIT_STARTED = "started_at"
        const val COL_VISIT_COMPLETED = "completed_at"

        private const val CREATE_VISITS =
            "CREATE TABLE $TABLE_VISITS (" +
                "$COL_VISIT_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_VISIT_REQUEST_ID INTEGER NOT NULL, " +
                "$COL_VISIT_WORKER_ID INTEGER NOT NULL, " +
                "$COL_VISIT_NOTES TEXT NOT NULL, " +
                "$COL_VISIT_STARTED INTEGER, " +
                "$COL_VISIT_COMPLETED INTEGER NOT NULL)"

        // reviews
        const val TABLE_REVIEWS = "reviews"
        const val COL_REVIEW_ID = "id"
        const val COL_REVIEW_REQUEST_ID = "request_id"
        const val COL_REVIEW_REVIEWER_ID = "reviewer_id"
        const val COL_REVIEW_DECISION = "decision"
        const val COL_REVIEW_REASON = "reason"
        const val COL_REVIEW_AT = "reviewed_at"

        private const val CREATE_REVIEWS =
            "CREATE TABLE $TABLE_REVIEWS (" +
                "$COL_REVIEW_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_REVIEW_REQUEST_ID INTEGER NOT NULL, " +
                "$COL_REVIEW_REVIEWER_ID INTEGER NOT NULL, " +
                "$COL_REVIEW_DECISION TEXT NOT NULL, " +
                "$COL_REVIEW_REASON TEXT NOT NULL, " +
                "$COL_REVIEW_AT INTEGER NOT NULL)"

        // audit_log
        const val TABLE_AUDIT_LOG = "audit_log"
        const val COL_AUDIT_ID = "id"
        const val COL_AUDIT_ACTOR = "actor_user_id"
        const val COL_AUDIT_ACTION = "action"
        const val COL_AUDIT_TARGET = "target_request_id"
        const val COL_AUDIT_TIMESTAMP = "timestamp"
        const val COL_AUDIT_DETAILS = "details"

        private const val CREATE_AUDIT_LOG =
            "CREATE TABLE $TABLE_AUDIT_LOG (" +
                "$COL_AUDIT_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_AUDIT_ACTOR INTEGER NOT NULL, " +
                "$COL_AUDIT_ACTION TEXT NOT NULL, " +
                "$COL_AUDIT_TARGET INTEGER, " +
                "$COL_AUDIT_TIMESTAMP INTEGER NOT NULL, " +
                "$COL_AUDIT_DETAILS TEXT NOT NULL)"
    }
}