export type BadgePermissionStatus = 'granted' | 'denied' | 'undetermined';

export type AndroidBadgeOptions = {
  /**
   * Show a silent notification so stock Android (Pixel, AOSP) can display a
   * launcher badge. Default: `true`.
   */
  useNotification?: boolean;
  /** Notification channel name shown in system settings. */
  channelName?: string;
  /** Notification title. Defaults to the app name. */
  contentTitle?: string;
  /** Notification body. Use `%count%` as a placeholder. Defaults to a single space. */
  contentText?: string;
};

export type ConfigureOptions = {
  android?: AndroidBadgeOptions;
};

export type NativeNotificationBadgeManager = {
  setBadgeCount(count: number): Promise<number>;
  getBadgeCount(): Promise<number>;
  clearBadge(): Promise<void>;
  increment(by: number): Promise<number>;
  decrement(by: number): Promise<number>;
  requestPermission(): Promise<boolean>;
  checkPermission(): Promise<string>;
  configure(options: ConfigureOptions): Promise<void>;
};
