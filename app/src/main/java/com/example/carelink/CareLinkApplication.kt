package com.example.carelink

import android.app.Application
import com.example.carelink.data.AuditRepository
import com.example.carelink.data.CareLinkDatabase
import com.example.carelink.data.RequestRepository
import com.example.carelink.data.ReviewRepository
import com.example.carelink.data.SessionManager
import com.example.carelink.data.UserRepository
import com.example.carelink.data.VisitRepository
import com.example.carelink.util.NotificationsHelper

// owns the single database instance + repositories so screens don't have to
// build them themselves. registered in AndroidManifest under <application>.
class CareLinkApplication : Application() {

    lateinit var database: CareLinkDatabase
        private set
    lateinit var sessionManager: SessionManager
        private set
    lateinit var userRepository: UserRepository
        private set
    lateinit var requestRepository: RequestRepository
        private set
    lateinit var visitRepository: VisitRepository
        private set
    lateinit var reviewRepository: ReviewRepository
        private set
    lateinit var auditRepository: AuditRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = CareLinkDatabase(applicationContext)
        sessionManager = SessionManager(applicationContext)
        userRepository = UserRepository(database)
        requestRepository = RequestRepository(database)
        visitRepository = VisitRepository(database)
        reviewRepository = ReviewRepository(database)
        auditRepository = AuditRepository(database)
        // notification channel - the lecture said to do this once at startup
        NotificationsHelper.createChannel(applicationContext)
    }
}