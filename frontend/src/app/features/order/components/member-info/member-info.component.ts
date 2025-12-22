import { Component, inject, signal, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MemberService } from '../../services/member.service';
import { MemberResponse, TempMemberRequest } from '../../models/order.model';

/**
 * 會員資訊元件
 *
 * 功能：
 * - 會員卡號查詢
 * - 臨時卡建立（查無會員時）
 * - 顯示會員折扣資訊
 */
@Component({
  selector: 'app-member-info',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="member-info">
      <!-- 查詢模式 -->
      @if (!showTempCardForm()) {
        <div class="search-form">
          <div class="input-group">
            <label for="memberId">會員卡號</label>
            <input
              id="memberId"
              type="text"
              [(ngModel)]="memberIdInput"
              placeholder="請輸入會員卡號"
              [disabled]="isSearching()"
            />
            <small class="hint">測試用: K00123, M00001, M00002, S00001</small>
          </div>
          <div class="button-group">
            <button
              class="btn btn-primary"
              (click)="searchMember()"
              [disabled]="!memberIdInput || isSearching()"
            >
              @if (isSearching()) {
                查詢中...
              } @else {
                查詢會員
              }
            </button>
            <button
              class="btn btn-secondary"
              (click)="showTempCardForm.set(true)"
            >
              使用臨時卡
            </button>
          </div>
        </div>

        @if (searchError()) {
          <div class="error-message">
            {{ searchError() }}
          </div>
        }
      }

      <!-- 臨時卡表單 -->
      @if (showTempCardForm()) {
        <div class="temp-card-form">
          <h3>建立臨時卡</h3>
          <div class="form-grid">
            <div class="input-group">
              <label for="tempName">姓名 *</label>
              <input
                id="tempName"
                type="text"
                [(ngModel)]="tempCardData.name"
                placeholder="請輸入姓名"
              />
            </div>
            <div class="input-group">
              <label for="tempPhone">手機號碼 *</label>
              <input
                id="tempPhone"
                type="tel"
                [(ngModel)]="tempCardData.cellPhone"
                placeholder="09xxxxxxxx"
              />
            </div>
            <div class="input-group">
              <label for="tempZip">郵遞區號 *</label>
              <input
                id="tempZip"
                type="text"
                [(ngModel)]="tempCardData.zipCode"
                placeholder="xxx"
                maxlength="5"
              />
            </div>
            <div class="input-group full-width">
              <label for="tempAddr">地址 *</label>
              <input
                id="tempAddr"
                type="text"
                [(ngModel)]="tempCardData.address"
                placeholder="請輸入完整地址"
              />
            </div>
          </div>
          <div class="button-group">
            <button
              class="btn btn-secondary"
              (click)="showTempCardForm.set(false)"
            >
              返回查詢
            </button>
            <button
              class="btn btn-primary"
              (click)="createTempCard()"
              [disabled]="!isValidTempCard() || isCreating()"
            >
              @if (isCreating()) {
                建立中...
              } @else {
                建立臨時卡
              }
            </button>
          </div>
        </div>
      }

      <!-- 會員資訊顯示 -->
      @if (member()) {
        <div class="member-display">
          <div class="member-header">
            <span class="member-name">{{ member()!.name }}</span>
            @if (member()!.isTempCard) {
              <span class="badge temp">臨時卡</span>
            } @else {
              <span class="badge member">會員</span>
            }
          </div>
          <div class="member-details">
            <div class="detail-row">
              <span class="label">卡號:</span>
              <span class="value">{{ member()!.memberId }}</span>
            </div>
            <div class="detail-row">
              <span class="label">電話:</span>
              <span class="value">{{ member()!.cellPhone }}</span>
            </div>
            @if (member()!.discType) {
              <div class="detail-row">
                <span class="label">折扣類型:</span>
                <span class="value discount">{{ member()!.discTypeName }}</span>
              </div>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .member-info {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .search-form, .temp-card-form {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .temp-card-form h3 {
      margin: 0 0 0.5rem;
      font-size: 1rem;
      color: #333;
    }

    .form-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 1rem;

      .full-width {
        grid-column: 1 / -1;
      }
    }

    .input-group {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;

      label {
        font-size: 0.875rem;
        color: #666;
      }

      input {
        padding: 0.5rem 0.75rem;
        border: 1px solid #e0e0e0;
        border-radius: 4px;
        font-size: 1rem;

        &:focus {
          outline: none;
          border-color: #1565c0;
        }

        &:disabled {
          background: #f5f5f5;
        }
      }

      .hint {
        font-size: 0.75rem;
        color: #999;
        font-style: italic;
      }
    }

    .button-group {
      display: flex;
      gap: 0.5rem;
    }

    .btn {
      padding: 0.5rem 1rem;
      border: none;
      border-radius: 4px;
      font-size: 0.875rem;
      cursor: pointer;

      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }

      &.btn-primary {
        background: #1565c0;
        color: #fff;
      }

      &.btn-secondary {
        background: #f5f5f5;
        color: #333;
        border: 1px solid #e0e0e0;
      }
    }

    .error-message {
      padding: 0.5rem;
      background: #ffebee;
      color: #c62828;
      border-radius: 4px;
      font-size: 0.875rem;
    }

    .member-display {
      padding: 1rem;
      background: #f5f5f5;
      border-radius: 4px;

      .member-header {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin-bottom: 0.75rem;

        .member-name {
          font-weight: 600;
          font-size: 1.125rem;
        }

        .badge {
          padding: 0.125rem 0.5rem;
          border-radius: 4px;
          font-size: 0.75rem;

          &.member {
            background: #e3f2fd;
            color: #1565c0;
          }

          &.temp {
            background: #fff3e0;
            color: #ef6c00;
          }
        }
      }

      .member-details {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;

        .detail-row {
          display: flex;
          gap: 0.5rem;
          font-size: 0.875rem;

          .label {
            color: #666;
            min-width: 60px;
          }

          .value.discount {
            color: #2e7d32;
            font-weight: 500;
          }
        }
      }
    }
  `]
})
export class MemberInfoComponent {
  private memberService = inject(MemberService);

  // Inputs
  member = input<MemberResponse | null>(null);
  isLoading = input(false);

  // Outputs
  memberFound = output<MemberResponse>();
  tempCardCreated = output<MemberResponse>();
  error = output<string>();

  // Local state
  memberIdInput = '';
  showTempCardForm = signal(false);
  isSearching = signal(false);
  isCreating = signal(false);
  searchError = signal<string | null>(null);

  tempCardData: TempMemberRequest = {
    name: '',
    cellPhone: '',
    address: '',
    zipCode: ''
  };

  /**
   * 查詢會員
   */
  async searchMember(): Promise<void> {
    if (!this.memberIdInput) return;

    this.isSearching.set(true);
    this.searchError.set(null);

    try {
      const member = await this.memberService.getMember(this.memberIdInput);
      if (member) {
        this.memberFound.emit(member);
      } else {
        this.searchError.set(`查無會員: ${this.memberIdInput}，您可以使用臨時卡建立訂單`);
      }
    } catch (err: any) {
      this.searchError.set(err.message ?? '查詢會員失敗');
      this.error.emit(err.message ?? '查詢會員失敗');
    } finally {
      this.isSearching.set(false);
    }
  }

  /**
   * 建立臨時卡
   */
  async createTempCard(): Promise<void> {
    if (!this.isValidTempCard()) return;

    this.isCreating.set(true);
    this.searchError.set(null);

    try {
      const member = await this.memberService.createTempMember(this.tempCardData);
      this.tempCardCreated.emit(member);
      this.showTempCardForm.set(false);
    } catch (err: any) {
      this.searchError.set(err.message ?? '建立臨時卡失敗');
      this.error.emit(err.message ?? '建立臨時卡失敗');
    } finally {
      this.isCreating.set(false);
    }
  }

  /**
   * 驗證臨時卡資料
   */
  isValidTempCard(): boolean {
    return !!(
      this.tempCardData.name &&
      this.memberService.isValidCellPhone(this.tempCardData.cellPhone) &&
      this.memberService.isValidZipCode(this.tempCardData.zipCode) &&
      this.tempCardData.address
    );
  }
}
