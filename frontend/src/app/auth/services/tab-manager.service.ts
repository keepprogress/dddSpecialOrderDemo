import { Injectable, signal, computed, OnDestroy } from '@angular/core';

const TAB_ID_KEY = 'som_active_tab_id';
const CHANNEL_NAME = 'som_tab_channel';

interface TabMessage {
  type: 'NEW_TAB' | 'HEARTBEAT' | 'CLOSE';
  tabId: string;
  timestamp: number;
}

/**
 * Tab 管理服務 (Angular 21+ Signals)
 * 實現單一分頁限制功能
 */
@Injectable({ providedIn: 'root' })
export class TabManagerService implements OnDestroy {
  private readonly tabId: string;
  private channel: BroadcastChannel | null = null;
  private heartbeatInterval: ReturnType<typeof setInterval> | null = null;

  // Signals
  private readonly _isBlocked = signal(false);

  // Computed
  readonly isBlocked = computed(() => this._isBlocked());

  constructor() {
    this.tabId = crypto.randomUUID();
    this.initialize();
  }

  ngOnDestroy(): void {
    this.cleanup();
  }

  /**
   * 初始化 Tab 控制
   */
  private initialize(): void {
    // 建立 BroadcastChannel
    if (typeof BroadcastChannel !== 'undefined') {
      this.channel = new BroadcastChannel(CHANNEL_NAME);
      this.setupChannelListener();
    }

    // 設定此分頁為活動分頁
    this.setActiveTab();

    // 監聽 storage 變化 (備援機制)
    window.addEventListener('storage', this.handleStorageChange.bind(this));

    // 監聽頁面關閉
    window.addEventListener('beforeunload', this.handleBeforeUnload.bind(this));

    // 啟動心跳
    this.startHeartbeat();
  }

  /**
   * 設定 BroadcastChannel 監聽器
   */
  private setupChannelListener(): void {
    if (!this.channel) return;

    this.channel.onmessage = (event: MessageEvent<TabMessage>) => {
      if (event.data.type === 'NEW_TAB' && event.data.tabId !== this.tabId) {
        // 其他分頁開啟，此分頁應被封鎖
        this._isBlocked.set(true);
      }
    };
  }

  /**
   * 設定此分頁為活動分頁
   */
  private setActiveTab(): void {
    localStorage.setItem(TAB_ID_KEY, this.tabId);

    // 廣播新分頁訊息
    this.broadcast({ type: 'NEW_TAB', tabId: this.tabId, timestamp: Date.now() });
  }

  /**
   * 廣播訊息
   */
  private broadcast(message: TabMessage): void {
    if (this.channel) {
      this.channel.postMessage(message);
    }
  }

  /**
   * 處理 storage 變化 (備援機制)
   */
  private handleStorageChange(event: StorageEvent): void {
    if (event.key === TAB_ID_KEY && event.newValue !== this.tabId) {
      this._isBlocked.set(true);
    }
  }

  /**
   * 處理頁面關閉
   */
  private handleBeforeUnload(): void {
    this.broadcast({ type: 'CLOSE', tabId: this.tabId, timestamp: Date.now() });

    // 如果是活動分頁，清除標記
    const currentActive = localStorage.getItem(TAB_ID_KEY);
    if (currentActive === this.tabId) {
      localStorage.removeItem(TAB_ID_KEY);
    }
  }

  /**
   * 啟動心跳
   */
  private startHeartbeat(): void {
    this.heartbeatInterval = setInterval(() => {
      if (!this._isBlocked()) {
        this.broadcast({
          type: 'HEARTBEAT',
          tabId: this.tabId,
          timestamp: Date.now(),
        });
      }
    }, 30000); // 每 30 秒
  }

  /**
   * 清理資源
   */
  private cleanup(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
    }
    if (this.channel) {
      this.channel.close();
    }
    window.removeEventListener('storage', this.handleStorageChange.bind(this));
    window.removeEventListener(
      'beforeunload',
      this.handleBeforeUnload.bind(this)
    );
  }

  /**
   * 重新取得活動權 (使用者點擊「繼續使用」)
   */
  reclaim(): void {
    this._isBlocked.set(false);
    this.setActiveTab();
  }
}
