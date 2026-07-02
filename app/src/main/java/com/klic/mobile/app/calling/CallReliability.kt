package com.klic.mobile.app.calling

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Helpers for the two OS settings that make calls reliable on Android:
 * - battery-optimization exemption (so OEM "killers" don't stop the call/socket services),
 * - full-screen-intent permission (Android 14 may revoke it for non-dialer apps).
 */
object CallReliability {

    fun isBatteryOptimized(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java)
        return !pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestDisableBatteryOptimization(context: Context) {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
        runCatching { context.startActivity(direct) }.onFailure {
            // Some OEMs don't honour the direct action — fall back to the settings list.
            runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        return context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }

    fun requestFullScreenIntent(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                    .setData(Uri.parse("package:${context.packageName}"))
            )
        }
    }

    /** True when either reliability setting still needs the user's attention. */
    fun needsAttention(context: Context): Boolean =
        isBatteryOptimized(context) || !canUseFullScreenIntent(context)
}
