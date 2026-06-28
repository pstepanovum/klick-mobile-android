# klick-mobile-android

Native Android client for **Klic** — Kotlin + Jetpack Compose, talking to `klick-server` for auth,
chat, and call signaling, and to LiveKit for audio/video media.

## Requirements
- Android Studio (Ladybug+), JDK 17, Android SDK 35
- An emulator or device on API 26+

## Run
Open the folder in Android Studio and let Gradle sync, then Run. By default the app targets the
**live server** `https://api.89.34.230.2.sslip.io` (see `data/Network.kt`). To use a local backend,
set `BASE_HTTP = "http://10.0.2.2:3000"` (emulator → host) and start `klick-server` first.

CLI: `./gradlew :app:installDebug`

## Stack
- **Jetpack Compose** + Material 3 (dark-first theme)
- **Retrofit + OkHttp + kotlinx.serialization** — REST
- **socket.io-client** — realtime messaging + call signaling
- **livekit-android** — audio/video rooms
- **DataStore** — token storage (move to EncryptedSharedPreferences before release)

## Structure

```
app/src/main/java/com/klic/app/
├── KlicApplication.kt      # manual DI container (repository, socket, callManager)
├── MainActivity.kt         # Compose nav host (auth gate → home → chat → call)
├── ui/theme/               # Color, Type (TikTok Sans), Theme (dark)
├── ui/components/          # PillButton, KlicTextField, CircleControl
├── data/                   # Models, KlicApi (Retrofit), TokenStore, KlicRepository, Network
├── realtime/SocketService  # mirrors server events
├── calling/CallManager     # LiveKit room + mic/camera toggles
└── feature/                # KlicViewModel + auth · conversations · chat · call screens
res/font/                   # curated TikTok Sans subset
res/mipmap-anydpi-v26/      # adaptive launcher icon
design/icons/{Bold,Line}    # brand SVG source of truth
scripts/generate-icons.sh   # SVG → vector drawables
```

## Design system
Dark-first. Background `#0E0F16`, primary **punch-red `#ED122B`**, TikTok Sans. Buttons fully
rounded and flat — no shadows, strokes, or emoji (`ui/components/Components.kt`).

## Icons
The brand set lives in `design/icons/`. For M0, screens use Material icons (mic, videocam, call…)
so they tint cleanly. Run `scripts/generate-icons.sh` to convert the brand SVGs into vector
drawables (`R.drawable.ic_<variant>_<name>`), then swap the Material icons for them.

## Roadmap
M1 auth/profile · M2 messaging + read receipts + FCM push · M3 calling (LiveKit + Telecom ConnectionService).
