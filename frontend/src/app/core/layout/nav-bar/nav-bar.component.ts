import { Component, inject, ChangeDetectionStrategy, computed, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../auth/services/auth.service';

/**
 * 導航列選單項目
 */
interface NavMenuItem {
  label: string;
  route: string;
  icon: string;
}

/**
 * 導航列元件 (Angular 21+ Standalone, OnPush, Signals)
 */
@Component({
  selector: 'app-nav-bar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav class="nav-bar">
      <div class="nav-left">
        <div class="nav-brand">
          <span class="brand-name">特殊訂單系統</span>
          @if (channelName()) {
            <span class="channel-badge">{{ channelName() }}</span>
          }
        </div>

        <ul class="nav-menu">
          @for (item of menuItems(); track item.route) {
            <li class="nav-item">
              <a
                class="nav-link"
                [routerLink]="item.route"
                routerLinkActive="active"
              >
                <span class="nav-icon">{{ item.icon }}</span>
                {{ item.label }}
              </a>
            </li>
          }
        </ul>
      </div>

      <div class="nav-right">
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
            🔄
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
        padding: 0 1.5rem;
        background-color: #2c3e50;
        color: white;
        height: 56px;
      }

      .nav-left {
        display: flex;
        align-items: center;
        gap: 2rem;
      }

      .nav-brand {
        display: flex;
        align-items: center;
        gap: 0.75rem;
      }

      .brand-name {
        font-size: 1.1rem;
        font-weight: 600;
      }

      .channel-badge {
        padding: 0.2rem 0.5rem;
        background-color: #3498db;
        border-radius: 4px;
        font-size: 0.75rem;
      }

      .nav-menu {
        display: flex;
        list-style: none;
        margin: 0;
        padding: 0;
        gap: 0.25rem;
      }

      .nav-item {
        display: flex;
      }

      .nav-link {
        display: flex;
        align-items: center;
        gap: 0.4rem;
        padding: 0.5rem 0.75rem;
        color: rgba(255, 255, 255, 0.8);
        text-decoration: none;
        border-radius: 4px;
        font-size: 0.9rem;
        transition: all 0.2s;
      }

      .nav-link:hover {
        background-color: rgba(255, 255, 255, 0.1);
        color: white;
      }

      .nav-link.active {
        background-color: rgba(255, 255, 255, 0.15);
        color: white;
      }

      .nav-icon {
        font-size: 1rem;
      }

      .nav-right {
        display: flex;
        align-items: center;
        gap: 1.5rem;
      }

      .user-display {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        font-size: 0.85rem;
      }

      .user-name {
        font-weight: 500;
      }

      .store-name {
        font-size: 0.75rem;
        color: #bdc3c7;
      }

      .nav-actions {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .btn-switch {
        padding: 0.4rem 0.6rem;
        background: transparent;
        border: 1px solid rgba(255, 255, 255, 0.3);
        border-radius: 4px;
        color: white;
        cursor: pointer;
        font-size: 0.9rem;
        transition: all 0.2s;
      }

      .btn-switch:hover {
        background-color: rgba(255, 255, 255, 0.1);
        border-color: rgba(255, 255, 255, 0.5);
      }

      .btn-logout {
        padding: 0.4rem 0.75rem;
        background-color: #e74c3c;
        border: none;
        border-radius: 4px;
        color: white;
        cursor: pointer;
        font-size: 0.85rem;
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

  /** 導航選單項目 */
  readonly menuItems = signal<NavMenuItem[]>([
    { label: '訂單管理', route: '/orders', icon: '📋' },
    { label: '退貨管理', route: '/returns', icon: '↩️' },
    { label: '安運單管理', route: '/shipping', icon: '🚚' },
    { label: '主檔維護', route: '/master', icon: '📁' },
    { label: '報表', route: '/reports', icon: '📊' },
  ]);

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
