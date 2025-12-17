import { Component, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { LoadingService } from '../../../core/services/loading.service';

/**
 * Loading 元件 (Angular 21+ Standalone, OnPush, Signals)
 * 顯示全域 loading 狀態
 */
@Component({
  selector: 'app-loading',
  standalone: true,
  imports: [],
  template: `
    @if (isLoading()) {
      <div class="loading-overlay">
        <div class="loading-spinner">
          <div class="spinner"></div>
          @if (message()) {
            <p class="loading-message">{{ message() }}</p>
          }
        </div>
      </div>
    }
  `,
  styles: [
    `
      .loading-overlay {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.5);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 9999;
      }

      .loading-spinner {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 1rem;
      }

      .spinner {
        width: 48px;
        height: 48px;
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

      .loading-message {
        color: white;
        font-size: 1rem;
        margin: 0;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoadingComponent {
  private readonly loadingService = inject(LoadingService);

  readonly isLoading = computed(() => this.loadingService.isLoading());
  readonly message = computed(() => this.loadingService.loadingMessage());
}
