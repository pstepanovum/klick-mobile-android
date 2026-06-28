package com.klicmobile.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.klicmobile.app.feature.KlicViewModel
import com.klicmobile.app.ui.components.KlicTextField
import com.klicmobile.app.ui.components.PillButton

@Composable
fun AuthScreen(vm: KlicViewModel) {
    var isRegistering by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val error by vm.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Klic", style = MaterialTheme.typography.displayLarge)
        Text(
            if (isRegistering) "Create your account" else "Welcome back",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        KlicTextField(username, { username = it }, "Username")
        Spacer(Modifier.height(12.dp))
        if (isRegistering) {
            KlicTextField(displayName, { displayName = it }, "Display name")
            Spacer(Modifier.height(12.dp))
        }
        KlicTextField(password, { password = it }, "Password", isPassword = true)
        Spacer(Modifier.height(20.dp))

        PillButton(if (isRegistering) "Sign up" else "Log in") {
            if (isRegistering) vm.register(username, password, displayName)
            else vm.login(username, password)
        }

        TextButton(onClick = { isRegistering = !isRegistering }) {
            Text(
                if (isRegistering) "I already have an account" else "Create an account",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
        }
    }
}
