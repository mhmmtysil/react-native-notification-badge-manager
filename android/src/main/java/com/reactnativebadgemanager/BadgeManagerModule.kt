package com.reactnativebadgemanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class BadgeManagerModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String = "BadgeManager"

  @ReactMethod
  fun setBadgeCount(count: Double, promise: Promise) {
    try {
      applyBadge(normalizeCount(count))
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("badge_error", e.message, e)
    }
  }

  @ReactMethod
  fun clearBadge(promise: Promise) {
    try {
      applyBadge(0)
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("badge_error", e.message, e)
    }
  }

  private fun applyBadge(count: Int) {
    applyNotificationBadge(count)
    applyLauncherBadges(count)
  }

  private fun applyNotificationBadge(count: Int) {
    val manager = NotificationManagerCompat.from(reactContext)
    ensureBadgeChannel()

    if (count <= 0) {
      manager.cancel(BADGE_NOTIFICATION_ID)
      return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
          ContextCompat.checkSelfPermission(
              reactContext,
              android.Manifest.permission.POST_NOTIFICATIONS,
          ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }

    val appInfo = reactContext.applicationInfo
    val appName = reactContext.packageManager.getApplicationLabel(appInfo).toString()
    val icon = if (appInfo.icon != 0) appInfo.icon else android.R.drawable.sym_def_app_icon

    val builder =
        NotificationCompat.Builder(reactContext, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(appName)
            .setContentText(" ")
            .setNumber(count)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setSilent(true)
            .setAutoCancel(false)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)

    val notification = builder.build()
    applyXiaomiCount(notification, count)
    manager.notify(BADGE_NOTIFICATION_ID, notification)
  }

  private fun ensureBadgeChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (nm.getNotificationChannel(CHANNEL_ID) != null) return

    val appName =
        reactContext.packageManager.getApplicationLabel(reactContext.applicationInfo).toString()
    val channel =
        NotificationChannel(CHANNEL_ID, appName, NotificationManager.IMPORTANCE_MIN).apply {
          setShowBadge(true)
          enableLights(false)
          enableVibration(false)
          setSound(null, null)
        }
    nm.createNotificationChannel(channel)
  }

  private fun applyXiaomiCount(notification: android.app.Notification, count: Int) {
    try {
      val extra = notification.javaClass.getDeclaredField("extraNotification").get(notification)
      extra.javaClass
          .getDeclaredMethod("setMessageCount", Int::class.javaPrimitiveType)
          .invoke(extra, count)
    } catch (_: Exception) {
    }
  }

  private fun applyLauncherBadges(count: Int) {
    val packageName = reactContext.packageName
    val className = launcherClassName() ?: return

    sendBadgeBroadcast(packageName, className, count)
    applySamsungBadge(packageName, className, count)
    applyHuaweiBadge(packageName, className, count)
    applyHonorBadge(packageName, className, count)
    applySonyBadge(packageName, className, count)
  }

  private fun sendBadgeBroadcast(packageName: String, className: String, count: Int) {
    val intent =
        Intent("android.intent.action.BADGE_COUNT_UPDATE").apply {
          putExtra("badge_count", count)
          putExtra("badge_count_package_name", packageName)
          putExtra("badge_count_class_name", className)
        }
    try {
      reactContext.sendBroadcast(intent)
    } catch (_: Exception) {
    }
  }

  private fun applySamsungBadge(packageName: String, className: String, count: Int) {
    try {
      val values =
          ContentValues().apply {
            put("package", packageName)
            put("class", className)
            put("badgecount", count)
          }
      reactContext.contentResolver.insert(Uri.parse(SAMSUNG_BADGE_URI), values)
    } catch (_: Exception) {
    }
  }

  private fun applyHuaweiBadge(packageName: String, className: String, count: Int) {
    callLauncherBadge(HUAWEI_BADGE_URI, packageName, className, count)
  }

  private fun applyHonorBadge(packageName: String, className: String, count: Int) {
    callLauncherBadge(HONOR_BADGE_URI, packageName, className, count)
  }

  private fun callLauncherBadge(uri: String, packageName: String, className: String, count: Int) {
    try {
      val extras =
          Bundle().apply {
            putString("package", packageName)
            putString("class", className)
            putInt("badgenumber", count)
          }
      reactContext.contentResolver.call(Uri.parse(uri), "change_badge", null, extras)
    } catch (_: Exception) {
    }
  }

  private fun applySonyBadge(packageName: String, className: String, count: Int) {
    try {
      val intent =
          Intent().apply {
            action = "com.sonyericsson.home.action.UPDATE_BADGE"
            putExtra("com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME", className)
            putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE", count > 0)
            putExtra("com.sonyericsson.home.intent.extra.badge.MESSAGE", count.toString())
            putExtra("com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME", packageName)
          }
      reactContext.sendBroadcast(intent)
    } catch (_: Exception) {
    }
  }

  private fun launcherClassName(): String? {
    val intent =
        Intent(Intent.ACTION_MAIN).apply {
          addCategory(Intent.CATEGORY_LAUNCHER)
          setPackage(reactContext.packageName)
        }
    val resolved = reactContext.packageManager.queryIntentActivities(intent, 0)
    return resolved.firstOrNull()?.activityInfo?.name
  }

  private fun normalizeCount(count: Double): Int {
    if (count.isNaN() || count.isInfinite()) return 0
    return maxOf(0, count.toInt())
  }

  companion object {
    private const val CHANNEL_ID = "rn.badge.manager"
    private const val BADGE_NOTIFICATION_ID = 71001
    private const val SAMSUNG_BADGE_URI = "content://com.sec.badge/apps?notify=true"
    private const val HUAWEI_BADGE_URI = "content://com.huawei.android.launcher.settings/badge/"
    private const val HONOR_BADGE_URI = "content://com.hihonor.android.launcher.settings/badge/"
  }
}
