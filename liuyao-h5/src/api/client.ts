import type { ApiErrorResponse, ApiResponse } from '../types/api';

const BASE = '/api';

export class ApiRequestError<T = unknown> extends Error {
  code: string;
  data?: T;
  status: number;

  constructor(message: string, options: { code?: string; data?: T; status: number }) {
    super(message);
    this.name = 'ApiRequestError';
    this.code = options.code || 'REQUEST_FAILED';
    this.data = options.data;
    this.status = options.status;
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });

  const body = await res.json() as ApiResponse<T> | ApiErrorResponse<unknown>;

  if (!res.ok || !body.success) {
    throw new ApiRequestError(body.message || '请求失败', {
      code: body.code,
      data: body.data,
      status: res.status,
    });
  }

  return body.data as T;
}

export function post<T>(path: string, data: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function get<T>(path: string): Promise<T> {
  return request<T>(path);
}
