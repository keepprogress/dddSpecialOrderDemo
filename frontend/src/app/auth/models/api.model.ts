/**
 * 通用 API 回應
 */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

/**
 * 錯誤回應
 */
export interface ErrorResponse {
  timestamp: string;
  status: number;
  errorCode: string;
  message: string;
  details?: string;
}
