import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  setBadgeCount(count: number): Promise<number>;
  getBadgeCount(): Promise<number>;
  clearBadge(): Promise<void>;
  increment(by: number): Promise<number>;
  decrement(by: number): Promise<number>;
  requestPermission(): Promise<boolean>;
  checkPermission(): Promise<string>;
  configure(options: Object): Promise<void>;
}

export default TurboModuleRegistry.get<Spec>('NotificationBadgeManager');
