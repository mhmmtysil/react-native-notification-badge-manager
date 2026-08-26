declare module 'react-native' {
  export const NativeModules: Record<string, unknown>;

  export const Platform: {
    OS: 'ios' | 'android' | 'macos' | 'windows' | 'web';
    Version: string | number;
  };

  export const PermissionsAndroid: {
    PERMISSIONS: {
      POST_NOTIFICATIONS: string;
    };
    RESULTS: {
      GRANTED: string;
    };
    request(permission: string): Promise<string>;
  };

  export interface TurboModule {}

  export const TurboModuleRegistry: {
    get<T>(name: string): T | null;
    getEnforcing<T>(name: string): T;
  };
}
