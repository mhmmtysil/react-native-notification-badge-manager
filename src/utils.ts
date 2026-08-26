export function normalizeBadgeCount(count: number): number {
  if (!Number.isFinite(count)) {
    return 0;
  }

  return Math.max(0, Math.floor(count));
}

export function normalizeDelta(value: number | undefined, fallback = 1): number {
  if (value == null || !Number.isFinite(value)) {
    return fallback;
  }

  return Math.max(0, Math.floor(value));
}

export function parsePermissionStatus(value: string): 'granted' | 'denied' | 'undetermined' {
  if (value === 'granted' || value === 'denied' || value === 'undetermined') {
    return value;
  }

  return 'undetermined';
}
