import { Injectable, signal, computed } from '@angular/core';

/**
 * Loading 服務 (Angular 21+ Signals)
 * 管理全域 loading 狀態
 */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  // 使用計數器支援多個同時進行的請求
  private readonly _loadingCount = signal(0);
  private readonly _loadingMessage = signal<string | null>(null);

  // Computed
  readonly isLoading = computed(() => this._loadingCount() > 0);
  readonly loadingMessage = computed(() => this._loadingMessage());

  /**
   * 開始 loading
   */
  start(message?: string): void {
    this._loadingCount.update((count) => count + 1);
    if (message) {
      this._loadingMessage.set(message);
    }
  }

  /**
   * 結束 loading
   */
  stop(): void {
    this._loadingCount.update((count) => Math.max(0, count - 1));
    if (this._loadingCount() === 0) {
      this._loadingMessage.set(null);
    }
  }

  /**
   * 強制清除所有 loading 狀態
   */
  clear(): void {
    this._loadingCount.set(0);
    this._loadingMessage.set(null);
  }

  /**
   * 設定 loading 訊息
   */
  setMessage(message: string): void {
    this._loadingMessage.set(message);
  }
}
