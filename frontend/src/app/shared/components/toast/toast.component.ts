import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { ToastService, Toast } from '../../services/toast.service';

/**
 * Toast Component - 輕量級 Toast 通知元件
 *
 * Angular 21+ Standalone Component，使用 Signals 和新控制流語法
 * 畫面右上角顯示，3 秒後自動消失
 *
 * @example
 * ```html
 * <!-- 在 app.component.html 中加入 -->
 * <app-toast />
 * ```
 */
@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [],
  template: `
    <div class="toast-container" role="region" aria-label="通知訊息">
      @for (toast of toasts(); track toast.id) {
        <div
          class="toast toast-{{ toast.type }}"
          role="alert"
          [attr.aria-live]="toast.type === 'error' ? 'assertive' : 'polite'"
        >
          <div class="toast-icon">
            @switch (toast.type) {
              @case ('success') { <span>✓</span> }
              @case ('warning') { <span>!</span> }
              @case ('error') { <span>✕</span> }
              @default { <span>ℹ</span> }
            }
          </div>
          <div class="toast-message">{{ toast.message }}</div>
          <button
            class="toast-close"
            (click)="dismiss(toast.id)"
            aria-label="關閉"
          >
            ✕
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 1rem;
      right: 1rem;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      max-width: 400px;
      pointer-events: none;
    }

    .toast {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.875rem 1rem;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      animation: slideIn 0.3s ease-out;
      pointer-events: auto;
    }

    @keyframes slideIn {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }

    .toast-info {
      background-color: #e7f3ff;
      border: 1px solid #b3d7ff;
      color: #004085;
    }

    .toast-success {
      background-color: #d4edda;
      border: 1px solid #c3e6cb;
      color: #155724;
    }

    .toast-warning {
      background-color: #fff3cd;
      border: 1px solid #ffeeba;
      color: #856404;
    }

    .toast-error {
      background-color: #f8d7da;
      border: 1px solid #f5c6cb;
      color: #721c24;
    }

    .toast-icon {
      flex-shrink: 0;
      width: 24px;
      height: 24px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      font-size: 0.875rem;
    }

    .toast-info .toast-icon {
      background-color: #007bff;
      color: white;
    }

    .toast-success .toast-icon {
      background-color: #28a745;
      color: white;
    }

    .toast-warning .toast-icon {
      background-color: #ffc107;
      color: #212529;
    }

    .toast-error .toast-icon {
      background-color: #dc3545;
      color: white;
    }

    .toast-message {
      flex: 1;
      font-size: 0.9375rem;
      line-height: 1.4;
    }

    .toast-close {
      flex-shrink: 0;
      background: none;
      border: none;
      font-size: 1rem;
      cursor: pointer;
      opacity: 0.6;
      transition: opacity 0.2s;
      padding: 0;
      line-height: 1;
    }

    .toast-close:hover {
      opacity: 1;
    }

    .toast-info .toast-close { color: #004085; }
    .toast-success .toast-close { color: #155724; }
    .toast-warning .toast-close { color: #856404; }
    .toast-error .toast-close { color: #721c24; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ToastComponent {
  private readonly toastService = inject(ToastService);

  readonly toasts = this.toastService.toasts;

  dismiss(id: string): void {
    this.toastService.dismiss(id);
  }
}
