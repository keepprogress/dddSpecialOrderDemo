import { Component, inject, ChangeDetectionStrategy, computed } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../auth/services/auth.service';

/**
 * 導航列元件 (Angular 21+ Standalone, OnPush, Signals)
 */
@Component({
  selector: 'app-nav-bar',
  standalone: true,
  imports: [],
  template: `
    <nav class="nav-bar">
      <div class="nav-brand">
        <span class="brand-name">特殊訂單系統</span>
        @if (channelName()) {
          <span class="channel-badge">{{ channelName() }}</span>
        }
      </div>

      <div class="nav-info">
        <div class="user-display">
          <span class="user-name">{{ userName() }}</span>
          @if (storeName()) {
            <span class="store-name">{{ storeName() }}</span>
          }
        </div>

        <div class="nav-actions">
          <button
            class="btn-switch"
            title="切換店別/系統別"
            (click)="switchSelection()"
          >
            &#128260;
          </button>
          <button class="btn-logout" title="登出" (click)="logout()">
            登出
          </button>
        </div>
      </div>
    </nav>
  `,
  styles: [
    `
      .nav-bar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.75rem 1.5rem;
        background-color: #2c3e50;
        color: white;
      }

      .nav-brand {
        display: flex;
        align-items: center;
        gap: 0.75rem;
      }

      .brand-name {
        font-size: 1.25rem;
        font-weight: 600;
      }

      .channel-badge {
        padding: 0.25rem 0.5rem;
        background-color: #3498db;
        border-radius: 4px;
        font-size: 0.8rem;
      }

      .nav-info {
        display: flex;
        align-items: center;
        gap: 1.5rem;
      }

      .user-display {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        font-size: 0.9rem;
      }

      .user-name {
        font-weight: 500;
      }

      .store-name {
        font-size: 0.8rem;
        color: #bdc3c7;
      }

      .nav-actions {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .btn-switch {
        padding: 0.5rem;
        background: transparent;
        border: 1px solid rgba(255, 255, 255, 0.3);
        border-radius: 4px;
        color: white;
        cursor: pointer;
        font-size: 1rem;
        transition: all 0.2s;
      }

      .btn-switch:hover {
        background-color: rgba(255, 255, 255, 0.1);
        border-color: rgba(255, 255, 255, 0.5);
      }

      .btn-logout {
        padding: 0.5rem 1rem;
        background-color: #e74c3c;
        border: none;
        border-radius: 4px;
        color: white;
        cursor: pointer;
        font-size: 0.9rem;
        transition: background-color 0.2s;
      }

      .btn-logout:hover {
        background-color: #c0392b;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavBarComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // Computed from AuthService
  readonly userName = computed(
    () => this.authService.loginContext()?.userName || ''
  );

  readonly storeName = computed(() => {
    const store = this.authService.selectedStore();
    return store?.storeName || '';
  });

  readonly channelName = computed(() => {
    const channel = this.authService.selectedChannel();
    return channel?.channelName || '';
  });

  /**
   * 切換店別/系統別選擇
   */
  switchSelection(): void {
    // 保留 userId, userName, systemFlags，清除選擇
    const context = this.authService.loginContext();
    if (context) {
      this.authService.updateSelectedStore(null, []);
    }
    this.router.navigate(['/store-selection']);
  }

  /**
   * 登出
   */
  async logout(): Promise<void> {
    await this.authService.logout();
  }
}
