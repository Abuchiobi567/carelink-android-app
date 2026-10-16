package com.example.carelink.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.carelink.CareLinkApplication
import com.example.carelink.data.SessionManager
import com.example.carelink.data.models.Roles
import com.example.carelink.ui.auth.LoginScreen
import com.example.carelink.ui.auth.RegisterScreen
import com.example.carelink.ui.coordinator.AssignTaskScreen
import com.example.carelink.ui.coordinator.CoordinatorInboxScreen
import com.example.carelink.ui.resident.RequestDetailScreen
import com.example.carelink.ui.resident.ResidentHomeScreen
import com.example.carelink.ui.resident.SubmitRequestScreen
import com.example.carelink.ui.reviewer.ReviewDetailScreen
import com.example.carelink.ui.reviewer.ReviewQueueScreen
import com.example.carelink.ui.shared.AuditLogScreen
import com.example.carelink.ui.shared.SettingsScreen
import com.example.carelink.ui.worker.CompleteVisitScreen
import com.example.carelink.ui.worker.MyVisitsScreen

@Composable
fun AppNavigation(
    onThemeChanged: (Boolean?) -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    val app = LocalContext.current.applicationContext as CareLinkApplication

    val startDestination = if (app.sessionManager.isLoggedIn()) {
        homeRouteForRole(app.sessionManager.getUserRole())
    } else {
        Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            LoginScreen(
                userRepository = app.userRepository,
                sessionManager = app.sessionManager,
                auditRepository = app.auditRepository,
                onLoginSuccess = { role ->
                    navController.navigate(homeRouteForRole(role)) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                userRepository = app.userRepository,
                sessionManager = app.sessionManager,
                auditRepository = app.auditRepository,
                onRegistered = {
                    navController.navigate(Routes.RESIDENT_HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.RESIDENT_HOME) {
            ResidentHomeScreen(
                requestRepository = app.requestRepository,
                auditRepository = app.auditRepository,
                sessionManager = app.sessionManager,
                onOpenDraft = { id -> navController.navigate(Routes.submitRequest(id)) },
                onOpenRequestDetail = { id -> navController.navigate(Routes.requestDetail(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onLogout = { signOut(navController, app.sessionManager) }
            )
        }

        composable(
            route = Routes.SUBMIT_REQUEST,
            arguments = listOf(navArgument("requestId") { type = NavType.LongType })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getLong("requestId") ?: -1L
            SubmitRequestScreen(
                requestId = requestId,
                requestRepository = app.requestRepository,
                auditRepository = app.auditRepository,
                sessionManager = app.sessionManager,
                onSubmitted = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.REQUEST_DETAIL,
            arguments = listOf(navArgument("requestId") { type = NavType.LongType })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getLong("requestId") ?: -1L
            RequestDetailScreen(
                requestId = requestId,
                requestRepository = app.requestRepository,
                visitRepository = app.visitRepository,
                reviewRepository = app.reviewRepository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.COORDINATOR_INBOX) {
            CoordinatorInboxScreen(
                requestRepository = app.requestRepository,
                userRepository = app.userRepository,
                auditRepository = app.auditRepository,
                sessionManager = app.sessionManager,
                onAssignSingle = { id -> navController.navigate(Routes.assignTask(id)) },
                onOpenDetail = { id -> navController.navigate(Routes.requestDetail(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenAuditLog = { navController.navigate(Routes.AUDIT_LOG) },
                onLogout = { signOut(navController, app.sessionManager) }
            )
        }

        composable(
            route = Routes.ASSIGN_TASK,
            arguments = listOf(navArgument("requestId") { type = NavType.LongType })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getLong("requestId") ?: -1L
            AssignTaskScreen(
                requestId = requestId,
                requestRepository = app.requestRepository,
                userRepository = app.userRepository,
                auditRepository = app.auditRepository,
                sessionManager = app.sessionManager,
                onAssigned = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MY_VISITS) {
            MyVisitsScreen(
                requestRepository = app.requestRepository,
                sessionManager = app.sessionManager,
                onOpenVisit = { id -> navController.navigate(Routes.completeVisit(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onLogout = { signOut(navController, app.sessionManager) }
            )
        }

        composable(
            route = Routes.COMPLETE_VISIT,
            arguments = listOf(navArgument("requestId") { type = NavType.LongType })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getLong("requestId") ?: -1L
            CompleteVisitScreen(
                requestId = requestId,
                requestRepository = app.requestRepository,
                visitRepository = app.visitRepository,
                auditRepository = app.auditRepository,
                sessionManager = app.sessionManager,
                onCompleted = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.REVIEW_QUEUE) {
            ReviewQueueScreen(
                requestRepository = app.requestRepository,
                onOpenReview = { id -> navController.navigate(Routes.reviewDetail(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenAuditLog = { navController.navigate(Routes.AUDIT_LOG) },
                onLogout = { signOut(navController, app.sessionManager) }
            )
        }

        composable(
            route = Routes.REVIEW_DETAIL,
            arguments = listOf(navArgument("requestId") { type = NavType.LongType })
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getLong("requestId") ?: -1L
            ReviewDetailScreen(
                requestId = requestId,
                requestRepository = app.requestRepository,
                visitRepository = app.visitRepository,
                reviewRepository = app.reviewRepository,
                auditRepository = app.auditRepository,
                sessionManager = app.sessionManager,
                onDecisionMade = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.AUDIT_LOG) {
            AuditLogScreen(
                auditRepository = app.auditRepository,
                userRepository = app.userRepository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                sessionManager = app.sessionManager,
                onThemeChanged = onThemeChanged,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// where each role lands after sign-in
fun homeRouteForRole(role: String): String = when (role) {
    Roles.RESIDENT -> Routes.RESIDENT_HOME
    Roles.COORDINATOR -> Routes.COORDINATOR_INBOX
    Roles.WORKER -> Routes.MY_VISITS
    Roles.REVIEWER -> Routes.REVIEW_QUEUE
    else -> Routes.LOGIN
}

// shared logout - clears the session and rewinds the back stack to LOGIN
private fun signOut(navController: NavHostController, sessionManager: SessionManager) {
    sessionManager.clearSession()
    navController.navigate(Routes.LOGIN) {
        popUpTo(0) { inclusive = true }
    }
}