'use strict';

/**
 * Expo config plugin for react-native-notification-badge-manager.
 *
 * Native iOS/Android code and Android permissions are autolinked. Add this
 * plugin in app.json so Expo prebuild keeps a stable reference:
 *
 * {
 *   "expo": {
 *     "plugins": ["react-native-notification-badge-manager"]
 *   }
 * }
 *
 * Expo Go is not supported. Use a development build.
 */
function withNotificationBadgeManager(config) {
  return config;
}

module.exports = withNotificationBadgeManager;
