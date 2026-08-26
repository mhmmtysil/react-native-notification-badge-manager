export type BadgePermissionStatus = 'granted' | 'denied' | 'undetermined';

export type AndroidBadgeOptions = {
  /**
   * Post a notification so Pixel / AOSP can show a launcher dot.
   * Default: `false` — icon badge only (Samsung, Xiaomi, Huawei, …), no shade notification.
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
