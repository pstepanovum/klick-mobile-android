package com.klicmobile.app.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.klicmobile.app.R
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

    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.Asset("animations/12.json"))
    val lottieProgress by animateLottieCompositionAsState(lottieComposition, iterations = LottieConstants.IterateForever)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LottieAnimation(
            composition = lottieComposition,
            progress = { lottieProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(top = 48.dp),
        )

        Image(
            painter = painterResource(R.drawable.ic_klic_logo),
            contentDescription = "Klic",
            modifier = Modifier
                .width(130.dp)
                .padding(top = 20.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
        )

        Text(
            if (isRegistering) "Create your account" else "Welcome back",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, bottom = 28.dp),
        )

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

        Spacer(Modifier.height(40.dp))
    }
}
