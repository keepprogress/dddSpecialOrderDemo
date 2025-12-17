# 27. Rollback Plan - 回滾計畫

## 回滾觸發條件

| 條件 | 閾值 | 決策時間 |
|-----|------|---------|
| 錯誤率 | > 1% | 5 分鐘 |
| p95 延遲 | > 1000ms | 10 分鐘 |
| 服務不可用 | > 1 分鐘 | 立即 |
| 資料損毀 | 發現任何 | 立即 |
| 重大 Bug | P0 級別 | 30 分鐘內評估 |

## 回滾步驟

### 1. 應用程式回滾

```bash
# Kubernetes Deployment 回滾
kubectl rollout undo deployment/order-service --namespace=production

# 驗證回滾狀態
kubectl rollout status deployment/order-service --namespace=production

# 檢查 Pod 狀態
kubectl get pods -l app=order-service --namespace=production
```

### 2. 資料庫回滾

```sql
-- Flyway 回滾到特定版本
flyway migrate -target=1.0.0

-- 從備份還原 (最壞情況)
-- 1. 停止應用
-- 2. 還原備份
RMAN> RESTORE DATABASE FROM BACKUP TAG='BEFORE_MIGRATION';
-- 3. 啟動應用
```

### 3. 流量切換

```bash
# 將 100% 流量切回舊版本 (Blue)
kubectl patch service order-service \
  --patch '{"spec":{"selector":{"version":"v1.0.0"}}}'
```

### 4. 驗證檢查

- [ ] 服務健康檢查通過
- [ ] 錯誤率 < 0.1%
- [ ] p95 延遲 < 500ms
- [ ] 業務指標正常 (訂單建立、付款)

## 回滾決策流程

```plaintext
監控告警
    ↓
評估嚴重程度 (5 分鐘)
    ↓
    ├─ P0 (Critical) → 立即回滾
    ├─ P1 (High) → 嘗試修復 (15 min) → 失敗 → 回滾
    └─ P2 (Medium) → 記錄 Issue, 計畫修復
```

## 溝通計畫

**回滾通知**:
1. Slack #ops 頻道通知
2. Email 通知利害關係人
3. 更新狀態頁面

**回滾後檢討**:
- 48 小時內召開 Post-Mortem 會議
- 撰寫事件報告 (5 Why Analysis)
- 更新 Runbook

---

**參考文件**:
- `23-Roadmap-Phase5-Testing-Launch.md`
- `26-Monitoring-Metrics.md`

**文件版本**: v1.0
**最後更新**: 2025-10-27
