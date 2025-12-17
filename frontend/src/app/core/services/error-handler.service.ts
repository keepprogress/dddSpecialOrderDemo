import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

/**
 * 錯誤資訊介面
 */
export interface ErrorInfo {
  code: string;
  message: string;
  details?: string;
  timestamp: Date;
}

/**
 * 統一錯誤處理服務 (Angular 21+ Signals)
 * 處理 API 錯誤並提供使用者友善的訊息
 */
@Injectable({ providedIn: 'root' })
export class ErrorHandlerService {
  private readonly router = inject(Router);

  // Signals
  private readonly _lastError = signal<ErrorInfo | null>(null);
  private readonly _showError = signal(false);

  // Computed
  readonly lastError = computed(() => this._lastError());
  readonly showError = computed(() => this._showError());

  /**
   * 處理 HTTP 錯誤
   */
  handleHttpError(error: HttpErrorResponse): ErrorInfo {
    let errorInfo: ErrorInfo;

    if (error.error instanceof ErrorEvent) {
      // 客戶端錯誤 (網路問題等)
      errorInfo = {
        code: 'CLIENT_ERROR',
        message: '網路連線錯誤',
        details: error.error.message,
        timestamp: new Date(),
      };
    } else if (error.status === 0) {
      // 無法連線到伺服器
      errorInfo = {
        code: 'CONNECTION_ERROR',
        message: '無法連線到伺服器',
        details: '請檢查網路連線或伺服器狀態',
        timestamp: new Date(),
      };
    } else {
      // 伺服器端錯誤
      errorInfo = this.parseServerError(error);
    }

    this._lastError.set(errorInfo);
    this._showError.set(true);

    return errorInfo;
  }

  /**
   * 解析伺服器錯誤回應
   */
  private parseServerError(error: HttpErrorResponse): ErrorInfo {
    const body = error.error;

    // 檢查是否為標準 API 回應格式
    if (body && typeof body === 'object') {
      if (body.errorCode) {
        return {
          code: body.errorCode,
          message: body.message || this.getDefaultMessage(error.status),
          details: body.details,
          timestamp: new Date(),
        };
      }
    }

    // 根據 HTTP 狀態碼返回預設訊息
    return {
      code: `HTTP_${error.status}`,
      message: this.getDefaultMessage(error.status),
      details: error.statusText,
      timestamp: new Date(),
    };
  }

  /**
   * 取得預設錯誤訊息
   */
  private getDefaultMessage(status: number): string {
    const messages: Record<number, string> = {
      400: '請求參數錯誤',
      401: '認證已過期，請重新登入',
      403: '您沒有權限執行此操作',
      404: '找不到請求的資源',
      408: '請求逾時',
      500: '伺服器發生錯誤',
      502: '伺服器暫時無法提供服務',
      503: '服務暫時不可用',
      504: '閘道逾時',
    };
    return messages[status] || '發生未知錯誤';
  }

  /**
   * 處理 Keycloak 不可用錯誤
   */
  handleKeycloakUnavailable(): void {
    const errorInfo: ErrorInfo = {
      code: 'KEYCLOAK_UNAVAILABLE',
      message: '認證服務暫時無法使用',
      details: '請稍後再試，或聯繫系統管理員',
      timestamp: new Date(),
    };
    this._lastError.set(errorInfo);
    this._showError.set(true);
    this.router.navigate(['/login']);
  }

  /**
   * 清除錯誤
   */
  clearError(): void {
    this._lastError.set(null);
    this._showError.set(false);
  }

  /**
   * 隱藏錯誤顯示
   */
  dismissError(): void {
    this._showError.set(false);
  }
}
