package com.notificationbadgemanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import kotlin.math.max

class NotificationBadgeManagerModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String = NAME

  @ReactMethod
  fun setBadgeCount(count: Double, promise: Promise) {
    try {
      promise.resolve(applyBadge(normalizeCount(count)))
    } catch (e: Exception) {
      promise.reject("badge_error", e.message, e)
    }
  }

  @ReactMethod
  fun getBadgeCount(promise: Promise) {
    promise.resolve(readStoredCount())
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

  @ReactMethod
  fun increment(by: Double, promise: Promise) {
    try {
      val next = applyBadge(readStoredCount() + normalizeCount(by))
      promise.resolve(next)
    } catch (e: Exception) {
      promise.reject("badge_error", e.message, e)
    }
  }

  @ReactMethod
  fun decrement(by: Double, promise: Promise) {
    try {
      val next = applyBadge(readStoredCount() - normalizeCount(by))
      promise.resolve(next)
    } catch (e: Exception) {
      promise.reject("badge_error", e.message, e)
    }
  }

  @ReactMethod
  fun requestPermission(promise: Promise) {
    promise.resolve(hasNotificationPermission())
  }

  @ReactMethod
  fun checkPermission(promise: Promise) {
    promise.resolve(if (hasNotificationPermission()) "granted" else "denied")
  }

  @ReactMethod
  fun configure(options: ReadableMap, promise: Promise) {
    try {
      val prefs = prefs()
      val android = if (options.hasKey("android")) options.getMap("android") else null
      if (android != null) {
        val editor = prefs.edit()
        if (android.hasKey("useNotification")) {
          editor.putBoolean(KEY_USE_NOTIFICATION, android.getBoolean("useNotification"))
        }
        if (android.hasKey("channelName") && android.getString("channelName") != null) {
          editor.putString(KEY_CHANNEL_NAME, android.getString("channelName"))
        }
        if (android.hasKey("contentTitle") && android.getString("contentTitle") != null) {
          editor.putString(KEY_CONTENT_TITLE, android.getString("contentTitle"))
        }
        if (android.hasKey("contentText") && android.getString("contentText") != null) {
          editor.putString(KEY_CONTENT_TEXT, android.getString("contentText"))
        }
        editor.apply()
      }
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("badge_error", e.message, e)
    }
  }

  private fun applyBadge(rawCount: Int): Int {
    val count = max(0, rawCount)
    writeStoredCount(count)
    try {
      if (prefs().getBoolean(KEY_USE_NOTIFICATION, false)) {
        applyNotificationBadge(count)
      } else {
        NotificationManagerCompat.from(reactContext).cancel(BADGE_NOTIFICATION_ID)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Notification badge failed", e)
    }
    try {
      applyLauncherBadges(count)
    } catch (e: Exception) {
      Log.w(TAG, "Launcher badge failed", e)
    }
    return count
  }

  private fun applyNotificationBadge(count: Int) {
    val manager = NotificationManagerCompat.from(reactContext)
    ensureBadgeChannel()

    if (count <= 0) {
      manager.cancel(BADGE_NOTIFICATION_ID)
      return
    }

    if (!hasNotificationPermission() || !manager.areNotificationsEnabled()) {
      Log.w(TAG, "POST_NOTIFICATIONS is not granted; Pixel/AOSP badges will not show")
      return
    }

    val appName =
        reactContext.packageManager.getApplicationLabel(reactContext.applicationInfo).toString()
    val title =
        (prefs().getString(KEY_CONTENT_TITLE, null)?.ifBlank { null } ?: appName).replace(
            "%count%",
            count.toString(),
        )
    val text =
        (prefs().getString(KEY_CONTENT_TEXT, null) ?: " ").replace("%count%", count.toString())

    val builder =
        NotificationCompat.Builder(reactContext, CHANNEL_ID)
            .setSmallIcon(smallIcon())
            .setContentTitle(title)
            .setContentText(text)
            .setNumber(count)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setAutoCancel(false)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setDefaults(0)
            .setSound(null)
            .setVibrate(null)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(launchPendingIntent())

    val notification = builder.build()
    applyXiaomiCount(notification, count)
    manager.notify(BADGE_NOTIFICATION_ID, notification)
  }

  private fun ensureBadgeChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = reactContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val appName =
        reactContext.packageManager.getApplicationLabel(reactContext.applicationInfo).toString()
    val channelName = prefs().getString(KEY_CHANNEL_NAME, null)?.ifBlank { null } ?: appName
    val existing = nm.getNotificationChannel(CHANNEL_ID)
    if (existing != null &&
        (existing.importance < NotificationManager.IMPORTANCE_DEFAULT || !existing.canShowBadge())) {
      nm.deleteNotificationChannel(CHANNEL_ID)
    } else if (existing != null && existing.name == channelName) {
      return
    }

    val channel =
        NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            .apply {
              setShowBadge(true)
              enableLights(false)
              enableVibration(false)
              setSound(null, null)
              description = "App icon badge"
            }
    nm.createNotificationChannel(channel)
  }

  private fun smallIcon(): Int {
    val pkg = reactContext.packageName
    val res = reactContext.resources
    for (name in arrayOf("ic_notification", "notification_icon", "ic_stat_notify")) {
      val drawableId = res.getIdentifier(name, "drawable", pkg)
      if (drawableId != 0) return drawableId
    }
    return R.drawable.notification_badge_icon
  }

  private fun launchPendingIntent(): PendingIntent? {
    val launch = reactContext.packageManager.getLaunchIntentForPackage(reactContext.packageName)
        ?: return null
    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    var flags = PendingIntent.FLAG_UPDATE_CURRENT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      flags = flags or PendingIntent.FLAG_IMMUTABLE
    }
    return PendingIntent.getActivity(reactContext, 0, launch, flags)
  }

  private fun applyXiaomiCount(notification: Notification, count: Int) {
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
    val component = "$packageName/$className"

    sendDefaultBadgeBroadcast(packageName, className, count)
    applySamsungBadge(packageName, className, count)
    applyHuaweiBadge(packageName, className, count)
    applyHonorBadge(packageName, className, count)
    applySonyBadge(packageName, className, count)
    applyOppoBadge(count)
    applyVivoBadge(packageName, className, count)
    applyHtcBadge(component, packageName, count)
    applyApexBadge(packageName, className, count)
    applyNovaBadge(packageName, className, count)
    applyXiaomiBroadcast(packageName, className, count)
    applyZteBadge(component, count)
    applyAsusBadge(packageName, className, count)
  }

  private fun sendDefaultBadgeBroadcast(packageName: String, className: String, count: Int) {
    sendBroadcast(
        Intent("android.intent.action.BADGE_COUNT_UPDATE").apply {
          putExtra("badge_count", count)
          putExtra("badge_count_package_name", packageName)
          putExtra("badge_count_class_name", className)
        }
    )
  }

  private fun applySamsungBadge(packageName: String, className: String, count: Int) {
    try {
      val values =
          ContentValues().apply {
            put("package", packageName)
            put("class", className)
            put("badgecount", count)
          }
      val uri = Uri.parse(SAMSUNG_BADGE_URI)
      val updated =
          reactContext.contentResolver.update(uri, values, "package=?", arrayOf(packageName))
      if (updated == 0) {
        reactContext.contentResolver.insert(uri, values)
      }
    } catch (e: Exception) {
      Log.d(TAG, "Samsung badge not available", e)
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
    sendBroadcast(
        Intent().apply {
          action = "com.sonyericsson.home.action.UPDATE_BADGE"
          putExtra("com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME", className)
          putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE", count > 0)
          putExtra("com.sonyericsson.home.intent.extra.badge.MESSAGE", count.toString())
          putExtra("com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME", packageName)
        }
    )
  }

  private fun applyOppoBadge(count: Int) {
    try {
      val extras = Bundle().apply { putInt("app_badge_count", count) }
      reactContext.contentResolver.call(Uri.parse(OPPO_BADGE_URI), "setAppBadgeCount", null, extras)
    } catch (_: Exception) {
    }
  }

  private fun applyVivoBadge(packageName: String, className: String, count: Int) {
    for (launcher in arrayOf("com.vivo.launcher", "com.bbk.launcher2")) {
      sendBroadcast(
          Intent("launcher.action.CHANGE_APPLICATION_NOTIFICATION_NUM").apply {
            setPackage(launcher)
            putExtra("packageName", packageName)
            putExtra("className", className)
            putExtra("notificationNum", count)
          }
      )
    }
  }

  private fun applyHtcBadge(component: String, packageName: String, count: Int) {
    sendBroadcast(
        Intent("com.htc.launcher.action.SET_NOTIFICATION").apply {
          putExtra("com.htc.launcher.extra.COMPONENT", component)
          putExtra("com.htc.launcher.extra.COUNT", count)
        }
    )
    sendBroadcast(
        Intent("com.htc.launcher.action.UPDATE_SHORTCUT").apply {
          putExtra("packagename", packageName)
          putExtra("count", count)
        }
    )
  }

  private fun applyApexBadge(packageName: String, className: String, count: Int) {
    sendBroadcast(
        Intent("com.anddoes.launcher.COUNTER_CHANGED").apply {
          putExtra("package", packageName)
          putExtra("count", count)
          putExtra("class", className)
        }
    )
  }

  private fun applyNovaBadge(packageName: String, className: String, count: Int) {
    try {
      val values =
          ContentValues().apply {
            put("tag", "$packageName/$className")
            put("count", count)
          }
      reactContext.contentResolver.insert(Uri.parse(NOVA_BADGE_URI), values)
    } catch (_: Exception) {
    }
  }

  private fun applyXiaomiBroadcast(packageName: String, className: String, count: Int) {
    val intent =
        Intent("android.intent.action.APPLICATION_MESSAGE_UPDATE").apply {
          putExtra(
              "android.intent.extra.update_application_component_name",
              "$packageName/$className",
          )
          putExtra(
              "android.intent.extra.update_application_message_text",
              if (count == 0) "" else count.toString(),
          )
        }
    sendBroadcast(intent)
    for (launcher in arrayOf("com.miui.home", "com.mi.android.globallauncher")) {
      sendBroadcast(Intent(intent).setPackage(launcher))
    }
  }

  private fun applyZteBadge(component: String, count: Int) {
    try {
      val extras =
          Bundle().apply {
            putInt("app_badge_count", count)
            putString("app_badge_component_name", component)
          }
      reactContext.contentResolver.call(Uri.parse(ZTE_BADGE_URI), "setAppUnreadCount", null, extras)
    } catch (_: Exception) {
    }
  }

  private fun applyAsusBadge(packageName: String, className: String, count: Int) {
    sendBroadcast(
        Intent("android.intent.action.BADGE_COUNT_UPDATE").apply {
          `package` = "com.asus.launcher"
          putExtra("badge_count", count)
          putExtra("badge_count_package_name", packageName)
          putExtra("badge_count_class_name", className)
        }
    )
  }

  private fun sendBroadcast(intent: Intent) {
    try {
      val pm = reactContext.packageManager
      val receivers =
          if (Build.VERSION.SDK_INT >= 33) {
            pm.queryBroadcastReceivers(intent, PackageManager.ResolveInfoFlags.of(0))
          } else {
            @Suppress("DEPRECATION") pm.queryBroadcastReceivers(intent, 0)
          }
      if (receivers.isEmpty()) {
        reactContext.sendBroadcast(intent)
        return
      }
      for (info in receivers) {
        val targeted = Intent(intent)
        targeted.component =
            ComponentName(info.activityInfo.packageName, info.activityInfo.name)
        reactContext.sendBroadcast(targeted)
      }
    } catch (e: Exception) {
      Log.d(TAG, "Badge broadcast failed for ${intent.action}", e)
    }
  }

  private fun launcherClassName(): String? {
    val launch = reactContext.packageManager.getLaunchIntentForPackage(reactContext.packageName)
    launch?.component?.className?.let {
      return it
    }
    val intent =
        Intent(Intent.ACTION_MAIN).apply {
          addCategory(Intent.CATEGORY_LAUNCHER)
          setPackage(reactContext.packageName)
        }
    val resolved =
        if (Build.VERSION.SDK_INT >= 33) {
          reactContext.packageManager.queryIntentActivities(
              intent,
              PackageManager.ResolveInfoFlags.of(0),
          )
        } else {
          @Suppress("DEPRECATION") reactContext.packageManager.queryIntentActivities(intent, 0)
        }
    return resolved.firstOrNull()?.activityInfo?.name
  }

  private fun hasNotificationPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        reactContext,
        android.Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
  }

  private fun prefs() =
      reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private fun readStoredCount(): Int = max(0, prefs().getInt(KEY_COUNT, 0))

  private fun writeStoredCount(count: Int) {
    prefs().edit().putInt(KEY_COUNT, count).apply()
  }

  private fun normalizeCount(count: Double): Int {
    if (count.isNaN() || count.isInfinite()) return 0
    return max(0, count.toInt())
  }

  companion object {
    const val NAME = "NotificationBadgeManager"
    private const val TAG = "NotificationBadgeManager"
    private const val PREFS_NAME = "rn.notification.badge.manager"
    private const val KEY_COUNT = "badge_count"
    private const val KEY_USE_NOTIFICATION = "use_notification"
    private const val KEY_CHANNEL_NAME = "channel_name"
    private const val KEY_CONTENT_TITLE = "content_title"
    private const val KEY_CONTENT_TEXT = "content_text"
    private const val CHANNEL_ID = "rn.notification.badge.manager"
    private const val BADGE_NOTIFICATION_ID = 71001
    private const val SAMSUNG_BADGE_URI = "content://com.sec.badge/apps?notify=true"
    private const val HUAWEI_BADGE_URI = "content://com.huawei.android.launcher.settings/badge/"
    private const val HONOR_BADGE_URI = "content://com.hihonor.android.launcher.settings/badge/"
    private const val OPPO_BADGE_URI = "content://com.android.badge/badge"
    private const val NOVA_BADGE_URI = "content://com.teslacoilsw.notifier/unread_count"
    private const val ZTE_BADGE_URI =
        "content://com.android.launcher3.cornermark.unreadbadge"
  }
}
