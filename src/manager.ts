import { NativeModules, PermissionsAndroid, Platform } from 'react-native';
import type { Spec } from './NativeNotificationBadgeManager';
import type {
  BadgePermissionStatus,
  ConfigureOptions,
  NativeNotificationBadgeManager,
} from './types';
import { normalizeBadgeCount, normalizeDelta, parsePermissionStatus } from './utils';

const LINKING_ERROR =
  `react-native-notification-badge-manager is not linked. Rebuild the native app after installing this package. ` +
  `Expo Go is not supported — use a development build or a bare React Native app.`;

function getNative(): NativeNotificationBadgeManager {
  const native = NativeModules.NotificationBadgeManager as Spec | undefined;

  if (!native) {
    throw new Error(LINKING_ERROR);
  }

  return native;
}

function isAndroid13OrNewer(): boolean {
  return Platform.OS === 'android' && typeof Platform.Version === 'number' && Platform.Version >= 33;
}

async function requestAndroidPermission(): Promise<boolean> {
  if (!isAndroid13OrNewer()) {
    return true;
  }

  const status = await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
  );

  return status === PermissionsAndroid.RESULTS.GRANTED;
}

export const NotificationBadgeManager = {
  /**
   * Set the app icon badge count. Negative or non-finite values become `0`.
   * Returns the applied count.
   */
  async setCount(count: number): Promise<number> {
    return getNative().setBadgeCount(normalizeBadgeCount(count));
  },

  /** Read the current badge count. */
  async getCount(): Promise<number> {
    return getNative().getBadgeCount();
  },

  /** Set the badge count to `0` and hide launcher badges. */
  async clear(): Promise<void> {
    await getNative().clearBadge();
  },

  /**
   * Increase the badge count. Defaults to `1`.
   * Returns the new count.
   */
  async increment(by?: number): Promise<number> {
    return getNative().increment(normalizeDelta(by, 1));
  },

  /**
   * Decrease the badge count without going below `0`. Defaults to `1`.
   * Returns the new count.
   */
  async decrement(by?: number): Promise<number> {
    return getNative().decrement(normalizeDelta(by, 1));
  },

  /**
   * Request badge / notification permission.
   * iOS: UserNotifications badge authorization.
   * Android 13+: `POST_NOTIFICATIONS`.
   */
  async requestPermission(): Promise<boolean> {
    if (Platform.OS === 'android') {
      return requestAndroidPermission();
    }

    return getNative().requestPermission();
  },

  /** Current permission state without prompting the user. */
  async checkPermission(): Promise<BadgePermissionStatus> {
    const status = await getNative().checkPermission();
    return parsePermissionStatus(status);
  },

  /**
   * Optional Android configuration (notification channel title/body,
   * whether to use a silent notification for stock Android badges).
   */
  async configure(options: ConfigureOptions): Promise<void> {
    await getNative().configure(options ?? {});
  },

  /** Badge APIs exist on iOS and Android. OEM launcher support still varies. */
  isSupported(): boolean {
    return Platform.OS === 'ios' || Platform.OS === 'android';
  },

  /** @deprecated Use `setCount`. */
  async setBadgeCount(count: number): Promise<number> {
    return NotificationBadgeManager.setCount(count);
  },

  /** @deprecated Use `clear`. */
  async clearBadge(): Promise<void> {
    return NotificationBadgeManager.clear();
  },
};

/** Alias kept for older call sites. */
export const BadgeManager = NotificationBadgeManager;
