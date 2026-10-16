package com.example.carelink.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.carelink.data.AuditRepository
import com.example.carelink.data.SessionManager
import com.example.carelink.data.UserRepository
import com.example.carelink.data.models.AuditAction
import com.example.carelink.data.models.Roles

// only Resident/Carer accounts can self-register here.
// staff accounts are seeded by CareLinkDatabase.onCreate.
@Composable
fun RegisterScreen(
    userRepository: UserRepository,
    sessionManager: SessionManager,
    auditRepository: AuditRepository,
    onRegistered: () -> Unit,
    onBack: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create your account",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Residents and carers can sign up here",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; errorMessage = null },
            label = { Text("Full name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                capitalization = KeyboardCapitalization.None
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Password (min 6 characters)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; errorMessage = null },
            label = { Text("Confirm password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val validation = validateRegistrationInput(name, email, password, confirmPassword)
                if (validation != null) {
                    errorMessage = validation
                    return@Button
                }
                scope.launch {
                    val newUserId = withContext(Dispatchers.IO) {
                        userRepository.register(
                            name = name,
                            email = email,
                            password = password,
                            role = Roles.RESIDENT
                        )
                    }
                    if (newUserId == -1L) {
                        errorMessage = "An account with that email already exists"
                        return@launch
                    }
                    withContext(Dispatchers.IO) {
                        sessionManager.saveSession(newUserId, Roles.RESIDENT)
                        auditRepository.log(actorUserId = newUserId, action = AuditAction.REGISTER)
                    }
                    onRegistered()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create account")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onBack) {
            Text("Back to sign in")
        }
    }
}

// returns null when input is fine, or an error message to show
private fun validateRegistrationInput(
    name: String,
    email: String,
    password: String,
    confirmPassword: String
): String? {
    if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
        return "Please fill in all fields"
    }
    // very basic email check - "x@y" is enough for the assignment scope
    if (!email.contains("@") || !email.contains(".")) {
        return "Please enter a valid email"
    }
    if (password.length < 6) {
        return "Password must be at least 6 characters"
    }
    if (password != confirmPassword) {
        return "Passwords do not match"
    }
    return null
}
