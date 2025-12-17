import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { TabManagerService } from '../../services/tab-manager.service';

/**
 * 驗證元件 (Angular 21+ Standalone, OnPush, Signals)
 * 執行 6-checkpoint 驗證並處理結果
 */
@Component({
  selector: 'app-validate',
  standalone: true,
  imports: [],
  template: `
    <div class="validate-container">
      <div class="validate-card">
        @if (isValidating()) {
          <h2>正在驗證您的帳號...</h2>
          <div class="spinner"></div>
          <p>請稍候，我們正在確認您的權限</p>
        } @else if (error()) {
          <div class="icon error-icon">&#10060;</div>
          <h2>驗證失敗</h2>
          <p class="error-message">{{ error() }}</p>
          <button class="btn-primary" (click)="retry()">重新嘗試</button>
        }
      </div>
    </div>
  `,
  styles: [
    `
      .validate-container {
        display: flex;
        justify-content: center;
        align-items: center;
        height: 100vh;
        background-color: #f5f5f5;
      }

      .validate-card {
        background: white;
        padding: 2rem;
        border-radius: 8px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        text-align: center;
        min-width: 350px;
      }

      .validate-card h2 {
        margin-bottom: 1rem;
        color: #333;
      }

      .validate-card p {
        color: #666;
        margin-bottom: 1.5rem;
      }

      .spinner {
        width: 40px;
        height: 40px;
        margin: 1rem auto;
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

      .icon {
        font-size: 4rem;
        margin-bottom: 1rem;
      }

      .error-icon {
        color: #e74c3c;
      }

      .error-message {
        color: #e74c3c;
        font-size: 1.1rem;
      }

      .btn-primary {
        background-color: #3498db;
        color: white;
        border: none;
        padding: 0.75rem 1.5rem;
        border-radius: 4px;
        cursor: pointer;
        font-size: 1rem;
        transition: background-color 0.2s;
      }

      .btn-primary:hover {
        background-color: #2980b9;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ValidateComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly tabManager = inject(TabManagerService);
  private readonly router = inject(Router);

  // Local signals
  readonly isValidating = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    // 檢查是否被其他分頁封鎖
    if (this.tabManager.isBlocked()) {
      this.router.navigate(['/tab-blocked']);
      return;
    }

    this.validate();
  }

  /**
   * 執行驗證
   */
  private validate(): void {
    this.isValidating.set(true);
    this.error.set(null);

    this.authService.validateUser().subscribe({
      next: (response) => {
        this.isValidating.set(false);

        if (response.success) {
          // 驗證成功，導向店別選擇
          this.router.navigate(['/store-selection']);
        } else {
          // 驗證失敗，顯示錯誤
          this.error.set(response.message);
        }
      },
      error: (err) => {
        this.isValidating.set(false);
        this.error.set(err.error?.message || '驗證失敗，請稍後再試');
      },
    });
  }

  /**
   * 重新嘗試
   */
  retry(): void {
    this.authService.clearLoginContext();
    this.router.navigate(['/login']);
  }
}
