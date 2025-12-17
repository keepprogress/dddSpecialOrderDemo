import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
  signal,
  computed,
} from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { StoreService } from '../../services/store.service';
import { AuthService } from '../../services/auth.service';
import { MastStoreResponse, SupportStoreResponse, StoreInfo } from '../../models';

/**
 * 店別選擇元件 (Angular 21+ Standalone, OnPush, Signals)
 */
@Component({
  selector: 'app-store-selection',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="selection-container">
      <div class="selection-card">
        <h2>選擇店別</h2>

        @if (isLoading()) {
          <div class="loading">
            <div class="spinner"></div>
            <p>載入店別資料中...</p>
          </div>
        } @else {
          <!-- 主店別 -->
          <div class="form-group">
            <label for="mastStore">主店別</label>
            @if (mastStore()) {
              <div class="store-display">
                {{ mastStore()?.storeName || '全區' }}
              </div>
            } @else {
              <div class="no-data">無主店別資料</div>
            }
          </div>

          <!-- 支援店別 -->
          @if (supportStores().length > 0) {
            <div class="form-group">
              <label>支援店別 (可多選)</label>
              <div class="checkbox-group">
                @for (store of supportStores(); track store.storeId) {
                  <label class="checkbox-item">
                    <input
                      type="checkbox"
                      [checked]="isSelected(store.storeId)"
                      (change)="toggleSupportStore(store)"
                    />
                    {{ store.storeName }}
                  </label>
                }
              </div>
            </div>
          }

          <!-- 錯誤訊息 -->
          @if (error()) {
            <div class="error-message">{{ error() }}</div>
          }

          <!-- 按鈕 -->
          <div class="actions">
            <button
              class="btn-primary"
              (click)="proceed()"
              [disabled]="isSubmitting()"
            >
              @if (isSubmitting()) {
                處理中...
              } @else {
                下一步
              }
            </button>
          </div>
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
        max-width: 450px;
      }

      .selection-card h2 {
        margin-bottom: 1.5rem;
        color: #333;
        text-align: center;
      }

      .form-group {
        margin-bottom: 1.5rem;
      }

      .form-group label {
        display: block;
        margin-bottom: 0.5rem;
        color: #555;
        font-weight: 500;
      }

      .store-display {
        padding: 0.75rem;
        background-color: #f8f9fa;
        border: 1px solid #dee2e6;
        border-radius: 4px;
        color: #333;
      }

      .no-data {
        padding: 0.75rem;
        background-color: #fff3cd;
        border: 1px solid #ffc107;
        border-radius: 4px;
        color: #856404;
      }

      .checkbox-group {
        max-height: 200px;
        overflow-y: auto;
        border: 1px solid #dee2e6;
        border-radius: 4px;
        padding: 0.5rem;
      }

      .checkbox-item {
        display: flex;
        align-items: center;
        padding: 0.5rem;
        cursor: pointer;
        transition: background-color 0.2s;
      }

      .checkbox-item:hover {
        background-color: #f8f9fa;
      }

      .checkbox-item input {
        margin-right: 0.5rem;
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
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StoreSelectionComponent implements OnInit {
  private readonly storeService = inject(StoreService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // Signals
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly mastStore = signal<MastStoreResponse | null>(null);
  readonly supportStores = signal<SupportStoreResponse[]>([]);
  readonly selectedSupportStoreIds = signal<Set<string>>(new Set());

  // Computed
  readonly selectedSupportStores = computed(() => {
    const ids = this.selectedSupportStoreIds();
    return this.supportStores().filter((s) => ids.has(s.storeId));
  });

  ngOnInit(): void {
    this.loadStoreData();
  }

  /**
   * 載入店別資料
   */
  private loadStoreData(): void {
    this.isLoading.set(true);
    this.error.set(null);

    forkJoin({
      mast: this.storeService.getMastStore(),
      support: this.storeService.getSupportStores(),
    }).subscribe({
      next: ({ mast, support }) => {
        this.isLoading.set(false);

        if (mast.success) {
          this.mastStore.set(mast.data);
        }

        if (support.success) {
          this.supportStores.set(support.data || []);
        }

        // 如果只有一個主店別且無支援店別，自動跳過
        if (mast.data && (!support.data || support.data.length === 0)) {
          this.autoSkip();
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        this.error.set(err.error?.message || '載入店別資料失敗');
      },
    });
  }

  /**
   * 自動跳過店別選擇
   */
  private autoSkip(): void {
    const mast = this.mastStore();
    if (mast) {
      const mastStoreInfo: StoreInfo = {
        storeId: mast.storeId,
        storeName: mast.storeName,
      };
      this.authService.updateSelectedStore(mastStoreInfo, []);
      this.router.navigate(['/channel-selection']);
    }
  }

  /**
   * 檢查支援店別是否被選中
   */
  isSelected(storeId: string): boolean {
    return this.selectedSupportStoreIds().has(storeId);
  }

  /**
   * 切換支援店別選擇
   */
  toggleSupportStore(store: SupportStoreResponse): void {
    const ids = new Set(this.selectedSupportStoreIds());
    if (ids.has(store.storeId)) {
      ids.delete(store.storeId);
    } else {
      ids.add(store.storeId);
    }
    this.selectedSupportStoreIds.set(ids);
  }

  /**
   * 繼續下一步
   */
  proceed(): void {
    this.isSubmitting.set(true);
    this.error.set(null);

    const mast = this.mastStore();
    const mastStoreInfo: StoreInfo | null = mast
      ? { storeId: mast.storeId, storeName: mast.storeName }
      : null;

    const supportStoreInfos: StoreInfo[] = this.selectedSupportStores().map(
      (s) => ({
        storeId: s.storeId,
        storeName: s.storeName,
      })
    );

    // 呼叫 API 記錄選擇
    this.storeService
      .selectStore({
        mastStoreId: mast?.storeId ?? null,
        supportStoreIds: Array.from(this.selectedSupportStoreIds()),
      })
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          // 更新 LoginContext
          this.authService.updateSelectedStore(mastStoreInfo, supportStoreInfos);
          // 導向系統別選擇
          this.router.navigate(['/channel-selection']);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.error.set(err.error?.message || '儲存店別選擇失敗');
        },
      });
  }
}
