package com.klic.app.feature.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.klic.app.data.CallSession
import com.klic.app.feature.KlicViewModel
import com.klic.app.ui.components.CircleControl
import com.klic.app.ui.theme.Background
import com.klic.app.ui.theme.Danger
import com.klic.app.ui.theme.OnPrimary
import com.klic.app.ui.theme.SurfaceRaised
import com.klic.app.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun CallScreen(vm: KlicViewModel, call: CallSession, peerName: String, onEnd: () -> Unit) {
    val scope = rememberCoroutineScope()
    val manager = vm.callManager
    val isConnected by manager.isConnected.collectAsState()
    val micEnabled by manager.micEnabled.collectAsState()
    val cameraEnabled by manager.cameraEnabled.collectAsState()

    LaunchedEffect(call.callId) {
        manager.join(call.livekitUrl, call.token, video = call.kind == "VIDEO")
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(120.dp).background(SurfaceRaised, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted) }
        Spacer(Modifier.height(16.dp))
        Text(peerName, style = MaterialTheme.typography.titleLarge)
        Text(
            if (isConnected) "Connected" else "Calling…",
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted,
        )
        Spacer(Modifier.height(48.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleControl(
                icon = if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = "Toggle microphone",
            ) { scope.launch { manager.toggleMic() } }

            CircleControl(
                icon = Icons.Default.CallEnd,
                contentDescription = "End call",
                fill = Danger,
                tint = OnPrimary,
                diameter = 72,
            ) { vm.endCall(); onEnd() }

            CircleControl(
                icon = if (cameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                contentDescription = "Toggle camera",
            ) { scope.launch { manager.toggleCamera() } }
        }
    }
}
