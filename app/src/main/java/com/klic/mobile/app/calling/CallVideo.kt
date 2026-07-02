package com.klic.mobile.app.calling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack

/** Renders a LiveKit [VideoTrack] inside Compose via a TextureViewRenderer. */
@Composable
fun LiveKitVideo(room: Room?, track: VideoTrack?, modifier: Modifier = Modifier) {
    if (room == null || track == null) return
    key(track.sid) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                TextureViewRenderer(ctx).apply {
                    room.initVideoRenderer(this)
                    track.addRenderer(this)
                }
            },
            onRelease = { view ->
                track.removeRenderer(view)
                view.release()
            },
        )
    }
}
