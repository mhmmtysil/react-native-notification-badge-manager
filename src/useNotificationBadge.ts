import { useCallback, useEffect, useState } from 'react';
import { NotificationBadgeManager } from './manager';

export type UseNotificationBadgeResult = {
  count: number;
  loading: boolean;
  setCount: (count: number) => Promise<number>;
  increment: (by?: number) => Promise<number>;
  decrement: (by?: number) => Promise<number>;
  clear: () => Promise<void>;
  refresh: () => Promise<void>;
};

/**
 * Keeps the app icon badge count in React state and syncs it with the native
 * launcher badge.
 */
export function useNotificationBadge(): UseNotificationBadgeResult {
  const [count, setCountState] = useState(0);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const next = await NotificationBadgeManager.getCount();
      setCountState(next);
    } catch {
      setCountState(0);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const setCount = useCallback(async (value: number) => {
    const next = await NotificationBadgeManager.setCount(value);
    setCountState(next);
    return next;
  }, []);

  const increment = useCallback(async (by?: number) => {
    const next = await NotificationBadgeManager.increment(by);
    setCountState(next);
    return next;
  }, []);

  const decrement = useCallback(async (by?: number) => {
    const next = await NotificationBadgeManager.decrement(by);
    setCountState(next);
    return next;
  }, []);

  const clear = useCallback(async () => {
    await NotificationBadgeManager.clear();
    setCountState(0);
  }, []);

  return { count, loading, setCount, increment, decrement, clear, refresh };
}
