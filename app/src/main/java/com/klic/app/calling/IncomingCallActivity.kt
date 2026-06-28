package com.klic.app.calling

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.klic.app.MainActivity
import com.klic.app.ui.theme.Background
import com.klic.app.ui.theme.Danger
import com.klic.app.ui.theme.KlicTheme
import com.klic.app.ui.theme.OnPrimary
import com.klic.app.ui.theme.SurfaceRaised
import com.klic.app.ui.theme.TextMuted

class IncomingCallActivity : ComponentActivity() {

    companion object {
        const val ACTION_ACCEPT_CALL = "com.klic.app.ACCEPT_CALL"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show over the lock screen and wake the device.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        val invite = CallInvite.fromIntent(intent)
        if (invite == null) { finish(); return }

        setContent {
            KlicTheme {
                IncomingCallScreen(
                    callerName = invite.fromName,
                    isVideo = invite.kind == "VIDEO",
                    onAccept = { accept(invite) },
                    onDecline = { decline() },
                )
            }
        }
    }

    private fun accept(invite: CallInvite) {
        CallNotifications.cancelIncomingCall(this)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                action = ACTION_ACCEPT_CALL
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtras(invite.toBundle())
            }
        )
        finish()
    }

    private fun decline() {
        CallNotifications.cancelIncomingCall(this)
        finish()
    }
}

@Composable
private fun IncomingCallScreen(callerName: String, isVideo: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Background).padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(120.dp).background(SurfaceRaised, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted)
            }
            Spacer(Modifier.height(16.dp))
            Text(callerName, style = MaterialTheme.typography.titleLarge)
            Text(if (isVideo) "Incoming video call" else "Incoming call", color = TextMuted)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onDecline, modifier = Modifier.size(72.dp).background(Danger, CircleShape)) {
                Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = OnPrimary)
            }
            IconButton(onClick = onAccept, modifier = Modifier.size(72.dp).background(Color(0xFF2BD158), CircleShape)) {
                Icon(Icons.Default.Call, contentDescription = "Accept", tint = OnPrimary)
            }
        }
    }
}
