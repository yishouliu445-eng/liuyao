/** 后端统一响应包装 */
export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

export interface ApiErrorResponse<T = unknown> {
  success: boolean;
  code: string;
  message: string;
  data?: T;
}
