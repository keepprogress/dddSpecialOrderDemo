import { Injectable, signal, computed } from '@angular/core';

/**
 * Toast 訊息類型
 */
export type ToastType = 'info' | 'success' | 'warning' | 'error';

/**
 * Toast 訊息介面
 */
export interface Toast {
  id: string;
  message: string;
  type: ToastType;
  duration: number;
}

/**
 * ToastService - 輕量級 Toast 通知服務
 *
 * 使用 Angular Signals 管理狀態
 * 支援自動消失 (預設 3 秒)
 *
 * @example
 * ```typescript
 * // 在 Component 中使用
 * private toast = inject(ToastService);
 *
 * // 顯示 info 訊息
 * this.toast.show('操作成功');
 *
 * // 顯示 warning 訊息，5 秒後消失
 * this.toast.warning('直送只能訂購', 5000);
 *
 * // 顯示 error 訊息
 * this.toast.error('發生錯誤');
 * ```
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly _toasts = signal<Toast[]>([]);

  /**
   * 目前顯示中的 Toast 清單 (唯讀)
   */
  readonly toasts = this._toasts.asReadonly();

  /**
   * 是否有 Toast 顯示中
   */
  readonly hasToasts = computed(() => this._toasts().length > 0);

  /**
   * 顯示 Toast 訊息
   *
   * @param message 訊息內容
   * @param type Toast 類型 (預設: 'info')
   * @param duration 顯示時間 (毫秒，預設: 3000)
   * @returns Toast ID
   */
  show(message: string, type: ToastType = 'info', duration = 3000): string {
    const id = crypto.randomUUID();
    const toast: Toast = { id, message, type, duration };

    this._toasts.update(toasts => [...toasts, toast]);

    // 自動消失
    if (duration > 0) {
      setTimeout(() => this.dismiss(id), duration);
    }

    return id;
  }

  /**
   * 顯示 Info Toast
   */
  info(message: string, duration = 3000): string {
    return this.show(message, 'info', duration);
  }

  /**
   * 顯示 Success Toast
   */
  success(message: string, duration = 3000): string {
    return this.show(message, 'success', duration);
  }

  /**
   * 顯示 Warning Toast
   */
  warning(message: string, duration = 3000): string {
    return this.show(message, 'warning', duration);
  }

  /**
   * 顯示 Error Toast
   */
  error(message: string, duration = 5000): string {
    return this.show(message, 'error', duration);
  }

  /**
   * 關閉指定 Toast
   */
  dismiss(id: string): void {
    this._toasts.update(toasts => toasts.filter(t => t.id !== id));
  }

  /**
   * 關閉所有 Toast
   */
  dismissAll(): void {
    this._toasts.set([]);
  }
}
