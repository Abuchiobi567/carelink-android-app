package com.example.carelink.nav

// every navigation route the NavHost knows about. having them here means
// autocomplete works and we can't accidentally typo a route name.
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val RESIDENT_HOME = "resident_home"
    // route arg pattern: "submit_request/{requestId}" - the home creates a
    // draft first and then navigates here with that id
    const val SUBMIT_REQUEST = "submit_request/{requestId}"
    fun submitRequest(requestId: Long) = "submit_request/$requestId"
    const val REQUEST_DETAIL = "request_detail/{requestId}"
    fun requestDetail(requestId: Long) = "request_detail/$requestId"

    const val COORDINATOR_INBOX = "coordinator_inbox"
    const val ASSIGN_TASK = "assign_task/{requestId}"
    fun assignTask(requestId: Long) = "assign_task/$requestId"

    const val MY_VISITS = "my_visits"
    const val COMPLETE_VISIT = "complete_visit/{requestId}"
    fun completeVisit(requestId: Long) = "complete_visit/$requestId"

    const val REVIEW_QUEUE = "review_queue"
    const val REVIEW_DETAIL = "review_detail/{requestId}"
    fun reviewDetail(requestId: Long) = "review_detail/$requestId"

    const val AUDIT_LOG = "audit_log"
    const val SETTINGS = "settings"
}