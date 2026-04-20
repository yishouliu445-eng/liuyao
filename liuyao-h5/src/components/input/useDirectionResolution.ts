import { useState } from 'react';
import { ApiRequestError } from '../../api/client';
import type { DirectionResolutionDTO } from '../../types/session';

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function toStringValue(value: unknown, fallback = ''): string {
  if (typeof value === 'string') return value;
  if (typeof value === 'number' || typeof value === 'boolean') return String(value);
  return fallback;
}

function normalizeResolution(value: unknown): DirectionResolutionDTO | null {
  if (!isRecord(value)) return null;
  return {
    detectedDirection: toStringValue(value.detectedDirection),
    userSelectedDirection: toStringValue(value.userSelectedDirection),
    finalDirection: toStringValue(value.finalDirection),
    suggestedDirection: toStringValue(value.suggestedDirection),
    requiresConfirmation: Boolean(value.requiresConfirmation),
    source: toStringValue(value.source),
    confidence: typeof value.confidence === 'number' ? value.confidence : 0,
  };
}

export function useDirectionResolution() {
  const [pendingResolution, setPendingResolution] = useState<DirectionResolutionDTO | null>(null);

  function consumeError(error: unknown): boolean {
    if (!(error instanceof ApiRequestError) || error.code !== 'DIRECTION_CONFIRMATION_REQUIRED') {
      return false;
    }
    const resolution = normalizeResolution(error.data);
    if (!resolution) {
      return false;
    }
    setPendingResolution(resolution);
    return true;
  }

  function clearPendingResolution() {
    setPendingResolution(null);
  }

  return {
    pendingResolution,
    consumeError,
    clearPendingResolution,
  };
}
