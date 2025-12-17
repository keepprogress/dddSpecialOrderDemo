import { Component, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { ErrorHandlerService } from '../../../core/services/error-handler.service';

/**
 * 錯誤提示元件 (Angular 21+ Standalone, OnPush, Signals)
 * 顯示全域錯誤訊息
 */
@Component({
  selector: 'app-error-toast',
  standalone: true,
  imports: [],
  template: `
    @if (showError()) {
      <div class="error-toast" role="alert">
        <div class="error-content">
          <div class="error-icon">!</div>
          <div class="error-text">
            <p class="error-message">{{ errorMessage() }}</p>
            @if (errorDetails()) {
              <p class="error-details">{{ errorDetails() }}</p>
            }
          </div>
          <button class="error-close" (click)="dismiss()" aria-label="關閉">
            &times;
          </button>
        </div>
      </div>
    }
  `,
  styles: [
    `
      .error-toast {
        position: fixed;
        top: 1rem;
        right: 1rem;
        max-width: 400px;
        background-color: #f8d7da;
        border: 1px solid #f5c6cb;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        z-index: 9998;
        animation: slideIn 0.3s ease-out;
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

      .error-content {
        display: flex;
        align-items: flex-start;
        padding: 1rem;
        gap: 0.75rem;
      }

      .error-icon {
        flex-shrink: 0;
        width: 24px;
        height: 24px;
        background-color: #dc3545;
        color: white;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: bold;
        font-size: 0.9rem;
      }

      .error-text {
        flex: 1;
      }

      .error-message {
        margin: 0;
        color: #721c24;
        font-weight: 500;
        font-size: 0.95rem;
      }

      .error-details {
        margin: 0.25rem 0 0;
        color: #856404;
        font-size: 0.85rem;
      }

      .error-close {
        flex-shrink: 0;
        background: none;
        border: none;
        color: #721c24;
        font-size: 1.5rem;
        line-height: 1;
        cursor: pointer;
        padding: 0;
        opacity: 0.7;
        transition: opacity 0.2s;
      }

      .error-close:hover {
        opacity: 1;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorToastComponent {
  private readonly errorHandler = inject(ErrorHandlerService);

  readonly showError = computed(() => this.errorHandler.showError());
  readonly errorMessage = computed(() => this.errorHandler.lastError()?.message);
  readonly errorDetails = computed(() => this.errorHandler.lastError()?.details);

  dismiss(): void {
    this.errorHandler.dismissError();
  }
}
