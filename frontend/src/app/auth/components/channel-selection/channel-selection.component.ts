import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
  signal,
  computed,
} from '@angular/core';
import { Router } from '@angular/router';

import { ChannelService } from '../../services/channel.service';
import { AuthService } from '../../services/auth.service';
import { ChannelResponse, ChannelInfo } from '../../models';

/**
 * 系統別選擇元件 (Angular 21+ Standalone, OnPush, Signals)
 */
@Component({
  selector: 'app-channel-selection',
  standalone: true,
  imports: [],
  template: `
    <div class="selection-container">
      <div class="selection-card">
        <h2>選擇系統別</h2>

        @if (isLoading()) {
          <div class="loading">
            <div class="spinner"></div>
            <p>載入系統別資料中...</p>
          </div>
        } @else {
          @if (channels().length === 0) {
            <div class="no-data">
              <p>您沒有任何可用的系統別權限</p>
              <button class="btn-secondary" (click)="logout()">登出</button>
            </div>
          } @else {
            <div class="channel-list">
              @for (channel of channels(); track channel.channelId) {
                <button
                  class="channel-card"
                  [class.selected]="selectedChannel()?.channelId === channel.channelId"
                  (click)="selectChannel(channel)"
                >
                  <div class="channel-name">{{ channel.channelName }}</div>
                  @if (channel.channelDesc) {
                    <div class="channel-desc">{{ channel.channelDesc }}</div>
                  }
                </button>
              }
            </div>

            <!-- 錯誤訊息 -->
            @if (error()) {
              <div class="error-message">{{ error() }}</div>
            }

            <!-- 按鈕 -->
            <div class="actions">
              <button
                class="btn-primary"
                (click)="proceed()"
                [disabled]="!selectedChannel() || isSubmitting()"
              >
                @if (isSubmitting()) {
                  處理中...
                } @else {
                  進入系統
                }
              </button>
            </div>
          }
        }
      </div>
    </div>
  `,
  styles: [
    `
      .selection-container {
        display: flex;
        justify-content: center;
        align-items: center;
        min-height: 100vh;
        background-color: #f5f5f5;
        padding: 1rem;
      }

      .selection-card {
        background: white;
        padding: 2rem;
        border-radius: 8px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        width: 100%;
        max-width: 500px;
      }

      .selection-card h2 {
        margin-bottom: 1.5rem;
        color: #333;
        text-align: center;
      }

      .loading {
        text-align: center;
        padding: 2rem;
      }

      .spinner {
        width: 40px;
        height: 40px;
        margin: 0 auto 1rem;
        border: 4px solid #f3f3f3;
        border-top: 4px solid #3498db;
        border-radius: 50%;
        animation: spin 1s linear infinite;
      }

      @keyframes spin {
        0% {
          transform: rotate(0deg);
        }
        100% {
          transform: rotate(360deg);
        }
      }

      .no-data {
        text-align: center;
        padding: 2rem;
        color: #666;
      }

      .channel-list {
        display: grid;
        gap: 1rem;
        margin-bottom: 1.5rem;
      }

      .channel-card {
        display: block;
        width: 100%;
        padding: 1rem 1.5rem;
        background: white;
        border: 2px solid #dee2e6;
        border-radius: 8px;
        cursor: pointer;
        text-align: left;
        transition: all 0.2s;
      }

      .channel-card:hover {
        border-color: #3498db;
        background-color: #f8f9fa;
      }

      .channel-card.selected {
        border-color: #3498db;
        background-color: #e3f2fd;
      }

      .channel-name {
        font-size: 1.1rem;
        font-weight: 600;
        color: #333;
        margin-bottom: 0.25rem;
      }

      .channel-desc {
        font-size: 0.9rem;
        color: #666;
      }

      .error-message {
        background-color: #f8d7da;
        border: 1px solid #f5c6cb;
        color: #721c24;
        padding: 0.75rem;
        border-radius: 4px;
        margin-bottom: 1rem;
      }

      .actions {
        display: flex;
        justify-content: center;
      }

      .btn-primary {
        background-color: #3498db;
        color: white;
        border: none;
        padding: 0.75rem 2rem;
        border-radius: 4px;
        cursor: pointer;
        font-size: 1rem;
        transition: background-color 0.2s;
      }

      .btn-primary:hover:not(:disabled) {
        background-color: #2980b9;
      }

      .btn-primary:disabled {
        background-color: #bdc3c7;
        cursor: not-allowed;
      }

      .btn-secondary {
        background-color: white;
        color: #333;
        border: 1px solid #ddd;
        padding: 0.75rem 1.5rem;
        border-radius: 4px;
        cursor: pointer;
        font-size: 1rem;
        margin-top: 1rem;
      }

      .btn-secondary:hover {
        background-color: #f5f5f5;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChannelSelectionComponent implements OnInit {
  private readonly channelService = inject(ChannelService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // Signals
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly channels = signal<ChannelResponse[]>([]);
  readonly selectedChannel = signal<ChannelResponse | null>(null);

  ngOnInit(): void {
    this.loadChannels();
  }

  /**
   * 載入系統別資料
   */
  private loadChannels(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.channelService.getAvailableChannels().subscribe({
      next: (response) => {
        this.isLoading.set(false);

        if (response.success && response.data) {
          this.channels.set(response.data);

          // 如果只有一個系統別，自動選擇並跳過
          if (response.data.length === 1) {
            this.autoSelectAndProceed(response.data[0]);
          }
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        this.error.set(err.error?.message || '載入系統別資料失敗');
      },
    });
  }

  /**
   * 自動選擇並進入系統
   */
  private autoSelectAndProceed(channel: ChannelResponse): void {
    const channelInfo: ChannelInfo = {
      channelId: channel.channelId,
      channelName: channel.channelName,
      channelDesc: channel.channelDesc,
    };
    this.authService.updateSelectedChannel(channelInfo);
    this.router.navigate(['/home']);
  }

  /**
   * 選擇系統別
   */
  selectChannel(channel: ChannelResponse): void {
    this.selectedChannel.set(channel);
  }

  /**
   * 進入系統
   */
  proceed(): void {
    const selected = this.selectedChannel();
    if (!selected) return;

    this.isSubmitting.set(true);
    this.error.set(null);

    this.channelService.selectChannel({ channelId: selected.channelId }).subscribe({
      next: () => {
        this.isSubmitting.set(false);

        // 更新 LoginContext
        const channelInfo: ChannelInfo = {
          channelId: selected.channelId,
          channelName: selected.channelName,
          channelDesc: selected.channelDesc,
        };
        this.authService.updateSelectedChannel(channelInfo);

        // 導向首頁
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.error.set(err.error?.message || '選擇系統別失敗');
      },
    });
  }

  /**
   * 登出
   */
  async logout(): Promise<void> {
    await this.authService.logout();
  }
}
