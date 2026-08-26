import { NativeModules } from 'react-native';

type BadgeManagerNativeModule = {
  setBadgeCount(count: number): Promise<void>;
  clearBadge(): Promise<void>;
};

const nativeModule = NativeModules.BadgeManager as
  | BadgeManagerNativeModule
  | undefined;

function getNative(): BadgeManagerNativeModule | null {
  return nativeModule ?? null;
}

function normalizeBadgeCount(count: number): number {
  if (!Number.isFinite(count)) {
    return 0;
  }
  return Math.max(0, Math.floor(count));
}

export const BadgeManager = {
  async setBadgeCount(count: number): Promise<void> {
    const native = getNative();
    if (!native) {
      return;
    }
    await native.setBadgeCount(normalizeBadgeCount(count));
  },

  async clearBadge(): Promise<void> {
    const native = getNative();
    if (!native) {
      return;
    }
    await native.clearBadge();
  },
};

export default BadgeManager;
