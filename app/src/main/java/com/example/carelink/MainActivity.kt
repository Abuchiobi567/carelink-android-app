package com.example.carelink

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.carelink.nav.AppNavigation
import com.example.carelink.ui.theme.CareLinkTheme

// single activity - compose handles every screen
class MainActivity : ComponentActivity() {

    // ask for POST_NOTIFICATIONS once at launch on Android 13+
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignored - we just want the user to see the system prompt */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = applicationContext as CareLinkApplication
            // theme state lives here so the Settings screen can update it on the fly
            var darkOverride by remember {
                mutableStateOf(app.sessionManager.getDarkModeOverride())
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            CareLinkTheme(forceDarkMode = darkOverride) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        onThemeChanged = { newOverride -> darkOverride = newOverride }
                    )
                }
            }
        }
    }
}