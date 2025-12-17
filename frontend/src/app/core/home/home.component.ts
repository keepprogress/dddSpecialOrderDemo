import { Component, inject, ChangeDetectionStrategy, computed } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/services/auth.service';
import { NavBarComponent } from '../layout/nav-bar/nav-bar.component';

/**
 * 首頁元件 (Angular 21+ Standalone, OnPush, Signals)
 */
@Component({
  selector: 'app-home',
  standalone: true,
  imports: [NavBarComponent],
  template: `
    <div class="home-container">
      <app-nav-bar />

      <main class="main-content">
        <div class="welcome-card">
          <h1>歡迎使用特殊訂單系統</h1>

          <div class="user-info">
            <div class="info-row">
              <span class="label">使用者：</span>
              <span class="value">{{ userName() }}</span>
            </div>
            <div class="info-row">
              <span class="label">店別：</span>
              <span class="value">{{ storeName() }}</span>
            </div>
            <div class="info-row">
              <span class="label">系統別：</span>
              <span class="value">{{ channelName() }}</span>
            </div>
          </div>

          <div class="quick-actions">
            <h3>快速功能</h3>
            <div class="action-grid">
              <button class="action-card" disabled>
                <div class="action-icon">&#128221;</div>
                <div class="action-title">新增訂單</div>
              </button>
              <button class="action-card" disabled>
                <div class="action-icon">&#128269;</div>
                <div class="action-title">查詢訂單</div>
              </button>
              <button class="action-card" disabled>
                <div class="action-icon">&#128202;</div>
                <div class="action-title">報表查詢</div>
              </button>
              <button class="action-card" disabled>
                <div class="action-icon">&#9881;</div>
                <div class="action-title">系統設定</div>
              </button>
            </div>
            <p class="coming-soon">功能開發中，敬請期待</p>
          </div>
        </div>
      </main>
    </div>
  `,
  styles: [
    `
      .home-container {
        min-height: 100vh;
        background-color: #f5f5f5;
      }

      .main-content {
        padding: 2rem;
        max-width: 1200px;
        margin: 0 auto;
      }

      .welcome-card {
        background: white;
        padding: 2rem;
        border-radius: 8px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
      }

      .welcome-card h1 {
        margin-bottom: 1.5rem;
        color: #333;
        text-align: center;
      }

      .user-info {
        background-color: #f8f9fa;
        padding: 1.5rem;
        border-radius: 8px;
        margin-bottom: 2rem;
      }

      .info-row {
        display: flex;
        margin-bottom: 0.5rem;
      }

      .info-row:last-child {
        margin-bottom: 0;
      }

      .label {
        font-weight: 500;
        color: #666;
        min-width: 80px;
      }

      .value {
        color: #333;
      }

      .quick-actions {
        text-align: center;
      }

      .quick-actions h3 {
        margin-bottom: 1rem;
        color: #333;
      }

      .action-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
        gap: 1rem;
        margin-bottom: 1rem;
      }

      .action-card {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 1.5rem;
        background: white;
        border: 1px solid #dee2e6;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.2s;
      }

      .action-card:hover:not(:disabled) {
        border-color: #3498db;
        box-shadow: 0 2px 8px rgba(52, 152, 219, 0.2);
      }

      .action-card:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      .action-icon {
        font-size: 2rem;
        margin-bottom: 0.5rem;
      }

      .action-title {
        font-size: 0.9rem;
        color: #333;
      }

      .coming-soon {
        color: #999;
        font-size: 0.9rem;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // Computed from AuthService
  readonly userName = computed(
    () => this.authService.loginContext()?.userName || '未知使用者'
  );

  readonly storeName = computed(() => {
    const store = this.authService.selectedStore();
    return store?.storeName || '未選擇';
  });

  readonly channelName = computed(() => {
    const channel = this.authService.selectedChannel();
    return channel?.channelName || '未選擇';
  });
}
