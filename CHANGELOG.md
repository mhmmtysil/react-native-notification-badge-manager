# Changelog

## 1.0.1

- Android default is icon badge only (no shade notification), matching iOS. Pixel dots stay opt-in via `useNotification: true`
- Fix Android badges never applying: invalid launcher icon as notification `smallIcon`, `IMPORTANCE_MIN` hiding Pixel dots, and notification failures blocking OEM launcher APIs
- Recreate the badge notification channel, add a white notification icon, Android 11 `<queries>`, and explicit launcher broadcasts

## 1.0.0

- First public release as `react-native-notification-badge-manager`
- iOS badge set/get/clear via `UNUserNotificationCenter` (iOS 16+) and `applicationIconBadgeNumber`
- Android badge support for stock notifications plus Samsung, Huawei, Honor, Xiaomi, OPPO, vivo, Sony, HTC, Asus, Nova, Apex, and ZTE launchers
- `increment`, `decrement`, permission helpers, Android `configure`, and `useNotificationBadge`
