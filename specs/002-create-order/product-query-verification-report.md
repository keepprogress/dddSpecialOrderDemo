# 商品查詢規格驗證報告

**驗證日期**: 2025-12-20
**驗證範圍**: product-query-spec.md 與 C:/projects/som 前後端程式碼交叉比對
**驗證原則**: 後端優先，前端補充

---

## 一、驗證結論摘要

| 類別 | 數量 | 嚴重程度 |
|------|------|----------|
| 規格正確且完整 | 12 項 | - |
| 規格遺漏 - 前端例外 | 10 項 | 高 |
| 規格遺漏 - 後端邊界 | 4 項 | 中 |
| 資料表遺漏 | 8 個 | 中 |
| VO 欄位遺漏 | 50+ 個 | 低 |
| 常數遺漏 | 20+ 個 | 低 |

**整體評估**: 規格涵蓋後端核心邏輯約 70%，但遺漏大量前端特有的例外處理和通路限制邏輯。

---

## 二、規格正確性驗證 (通過項目)

以下項目規格描述與程式碼完全一致：

| 編號 | 項目 | 程式碼證據 | 狀態 |
|------|------|-----------|------|
| V-001 | lockTradeStatusY 預設值 "N" | SkuInfoVO.java:145 | ✅ 正確 |
| V-002 | isDcVendorStatusD 預設值 false | SkuInfoVO.java:152 | ✅ 正確 |
| V-003 | OD-001 商品類型檢查邏輯 | BzSkuInfoServices.java:238-244 | ✅ 正確 |
| V-004 | OD-002 廠商凍結(非DC)處理 | BzSkuInfoServices.java:252-259 | ✅ 正確 |
| V-005 | OD-003 DC商品廠商凍結處理 | BzSkuInfoServices.java:254-255 | ✅ 正確 |
| V-006 | OD-004 採購組織檢查 | BzSkuInfoServices.java:268-275 | ✅ 正確 |
| V-007 | OD-005 無廠商ID錯誤訊息 | BzSkuInfoServices.java:261-265 | ✅ 正確 |
| V-008 | 大型家具判斷邏輯 | LargeFurnitureService.java:31-60 | ✅ 正確 |
| V-009 | 外包服務商品判斷 026+888 | BzSkuInfoServices.java:748-750 | ✅ 正確 |
| V-010 | 直送條件 DELIVERY_FLAG='V' | SoConstant.java:99-101 | ✅ 正確 |
| V-011 | GoodsType 類型定義 | GoodsType.java:8-72 | ✅ 正確 |
| V-012 | 運送方式枚舉 N/D/V/C/F/P | SoConstant.java:93-113 | ✅ 正確 |

---

## 三、規格遺漏 - 前端特有例外 (高優先級)

以下例外處理僅存在於前端程式碼，規格完全未記載：

### 3.1 票券商品限制 (嚴重遺漏)

**程式碼位置**: soSKUSubPage.jsp 第137-148行

```javascript
// 票券訂單只能選票券商品
if (orderSource === '0002') {
    if (!(subDeptId === '068' && classId === '011')) {
        alert('請輸入票券商品編號');
        return;
    }
}
// 非票券訂單不能選票券商品
else {
    if (subDeptId === '068' && classId === '011') {
        alert('此商品限票券使用');
        return;
    }
}
```

**建議新增規則**:

| 規則編號 | 規則名稱 | 條件 | 結果 |
|---------|---------|------|------|
| OD-006 | 票券商品限制 | 訂單來源='0002' 且商品非 068-011 | 拒絕新增 |
| OD-007 | 非票券訂單排除 | 訂單來源≠'0002' 且商品為 068-011 | 拒絕新增 |

---

### 3.2 通路限制 (嚴重遺漏)

**程式碼位置**: soSKUSubPage.jsp 第1327-1363行

```javascript
// MINI、CASA 通路禁止訂購
if (channelId === 'MINI' || channelId === 'CASA') {
    tradeStatusY.disabled = true;
}
```

**建議新增規則**:

| 規則編號 | 規則名稱 | 條件 | 結果 |
|---------|---------|------|------|
| OD-008 | MINI通路限制 | channelId = 'MINI' | 禁用訂購選項 |
| OD-009 | CASA通路限制 | channelId = 'CASA' | 禁用訂購選項 |

---

### 3.3 DIEN 類型商品特殊處理 (遺漏)

**程式碼位置**: soSKUSubPage.jsp 第2518-2536行

```javascript
if (skuType === 'DIEN') {
    installFlag.disabled = true;
    deliveryFlag (全部).disabled = true;
    tradeStatus (全部).disabled = true;
    // 強制配置
    tradeStatus = 'X';  // 現貨
    deliveryFlag = 'P'; // 下次自取
}
```

**建議新增規則**:

| 規則編號 | 規則名稱 | 條件 | 結果 |
|---------|---------|------|------|
| OD-010 | DIEN商品限制 | SKU_TYPE = 'DIEN' | 強制現貨+下次自取，禁用所有選項 |

---

### 3.4 變價條碼重複檢查 (遺漏)

**程式碼位置**: soSKUSubPage.jsp 第122-136行

```javascript
if (jsonObj.qrcode !== '' && 條碼已在清單中) {
    showAlertModal('變價條碼已使用');
    清空輸入();
    quantity.readonly = true;
}
```

**建議新增規則**:

| 規則編號 | 規則名稱 | 條件 | 結果 |
|---------|---------|------|------|
| OD-011 | 變價條碼唯一性 | QRCode 已存在於當前訂單 | 拒絕新增 |

---

### 3.5 商品狀態組合判斷 (部分遺漏)

**程式碼位置**: soSKUSubPage.jsp 第523-563行

規格僅提及 skuStatus，未說明 skuStoreStatus 的組合判斷：

```javascript
// 雙狀態組合判斷
if (skuStatus === 'D' || (skuStoreStatus === 'D' && dcType !== 'DC')) {
    showSkuStatus = 'D';
    chkList.push(skuNo);  // 加入刪除品警告清單
    顯示紅色標籤();
}
```

**建議補充**:

| 狀態組合 | skuStatus | skuStoreStatus | dcType | 結果 |
|---------|-----------|----------------|--------|------|
| 正常 | A | A | - | 可用 |
| 商品刪除 | D | - | - | 不可用 |
| 門店停用(非DC) | A | D | ≠DC | 不可用 |
| 門店停用(DC) | A | D | DC | 可用(特例) |

---

### 3.6 AOH 庫存自動降級邏輯 (遺漏)

**程式碼位置**: soSKUSubPage.jsp 第1098-1113行

規格提及 "需查詢 AOH" 但未說明自動降級邏輯：

```javascript
// DC商品廠商凍結時的自動降級
if (isDcVendorStatusD) {
    if (largeFurniture === true || stockAOH >= quantity) {
        // 大型家具或庫存充足 → 允許訂購
        tradeStatusY.disabled = false;
    } else {
        // 庫存不足 → 自動切換到現貨
        tradeStatusY.checked = false;
        tradeStatusX.checked = true;
        tradeStatusY.disabled = true;
        alert("廠商狀態已停用，該商品購買量已大於DC倉AOH量，僅能勾選【現貨】");
    }
}
```

**建議新增規則**:

| 規則編號 | 規則名稱 | 條件 | 結果 |
|---------|---------|------|------|
| OD-012 | DC廠商凍結-大型家具 | isDcVendorStatusD=true AND largeFurniture=true | 允許訂購 |
| OD-013 | DC廠商凍結-庫存足 | isDcVendorStatusD=true AND AOH >= quantity | 允許訂購 |
| OD-014 | DC廠商凍結-庫存不足 | isDcVendorStatusD=true AND AOH < quantity | 強制現貨 |

---

### 3.7 masterConfigId 檢查 (遺漏)

**程式碼位置**: 前端多處

```javascript
if (masterConfigId !== 'Y') {
    tradeStatusY.disabled = true;
}
```

**建議新增規則**:

| 規則編號 | 規則名稱 | 條件 | 結果 |
|---------|---------|------|------|
| OD-015 | 主配檔檢查 | masterConfigId ≠ 'Y' | 禁用訂購選項 |

---

### 3.8 holdOrder 採購權限檢查 (遺漏)

**程式碼位置**: 前端 newCheckTradeStatusY()

```javascript
// DC商品
if (dcType === 'DC' && !stoAllowList.includes(holdOrder)) {
    tradeStatusY.disabled = true;
}
// PO商品
if ((dcType === 'XD' || dcType === '') && !poAllowList.includes(holdOrder)) {
    tradeStatusY.disabled = true;
}
```

規格僅列出 HOLD_ORDER 欄位，未說明 poAllowList/stoAllowList 的具體值。

**建議補充**:

| 採購權限 | holdOrder 值 | PO商品 | DC商品 | 說明 |
|---------|-------------|--------|--------|------|
| 無限制 | N | ✅ | ✅ | 無HOLD ORDER |
| 暫停採購 | A | ❌ | ✅ | 暫停採購及調撥 |
| 暫停店調 | B | ✅ | ❌ | 暫停店對店調撥 |
| 暫停所有 | C | ❌ | ❌ | 暫停所有採購調撥 |
| 允許MD | D | ✅ | ✅ | 暫停但允許MD下單 |
| 允許MD調撥 | E | ✅ | ✅ | 暫停但允許MD調撥 |

---

### 3.9 宅配重量檢查 (遺漏)

**程式碼位置**: 前端

```javascript
if (weight === 0 || !出貨店支援宅配) {
    deliveryFlagF.disabled = true;
}
```

**建議新增規則**:

| 規則編號 | 規則名稱 | 條件 | 結果 |
|---------|---------|------|------|
| OD-016 | 宅配重量限制 | weight = 0 | 禁用宅配選項 |
| OD-017 | 宅配店別限制 | 出貨店不支援宅配 | 禁用宅配選項 |

---

### 3.10 免安過期檢查 (遺漏)

**程式碼位置**: soSKUSubPage.jsp 第2559-2650行

```javascript
function callAjaxCheckFreeInstallOverDue(skuNo, storeId) {
    // AJAX 檢查免安是否過期
    if (data === 'Y') {
        // 免安已過期，從安裝費清單移除
        移除FI商品();
    }
}
```

**建議新增規則**:

| 規則編號 | 規則名稱 | 條件 | 結果 |
|---------|---------|------|------|
| OD-018 | 免安效期檢查 | 免安商品已過期 | 從可選清單移除 |

---

## 四、規格遺漏 - 後端邊界情況 (中優先級)

### 4.1 免安售價異常檢測

**程式碼位置**: BzSkuInfoServices.java 第348-350行

```java
if (eventAmt > installPosAmt) {
    isRemoveFI = true;  // 標記移除免安
    // 原因：活動售價超過標安售價，表示 DB 資料錯誤
}
```

**建議新增**:

| 規則編號 | 規則名稱 | 條件 | 結果 |
|---------|---------|------|------|
| OD-019 | 免安售價驗證 | eventAmt > installPosAmt | 移除該免安商品 |

---

### 4.2 外包服務商品安裝查詢失敗

**程式碼位置**: BzSkuInfoServices.java 第575行

```java
// 潛在 NPE 風險
lstSkuInfoVO.get(0);  // 無查詢結果檢查
```

**建議**: 實作時需增加空值檢查

---

### 4.3 工種查詢失敗處理

**程式碼位置**: BzSkuInfoServices.java 第420行

```java
if (工種為空) {
    InstallFlag = 'N';
    lstInstallSkuInfo = 空列表;
}
```

**建議新增規則**:

| 規則編號 | 規則名稱 | 條件 | 結果 |
|---------|---------|------|------|
| OD-020 | 工種查詢失敗 | 找不到對應工種 | 設為無安裝 |

---

### 4.4 條碼查詢多筆處理

**程式碼位置**: BzSkuInfoServices.java 第166-177行

```java
// TBL_BARCODE 查詢可能返回多筆
for (TblBarcode barcode : barcodeList) {
    // 逐一檢查 TBL_SKU_STORE.ALLOW_SALES = 'Y'
    if (allowSales.equals("Y")) {
        return barcode.getSkuNo();  // 返回第一個可銷售的
    }
}
```

**建議補充**: 多筆條碼對照時，優先返回 ALLOW_SALES='Y' 的商品

---

## 五、資料表遺漏

規格 ER 圖僅列出 10 個表，實際涉及 18 個表。遺漏的表：

| 資料表 | 用途 | 重要性 |
|--------|------|--------|
| TBL_PROM_EVENT | 促銷事件定義 | 高 |
| TBL_PROM_CONDITION | 促銷條件(限購數量) | 高 |
| TBL_INSTALL_DISCOUNT | 安裝折扣定義 | 高 |
| TBL_INSTALL_DISCOUNT_STORE | 安裝折扣生效店別 | 中 |
| TBL_DELIVERY_SKU_SEQUENCE | 運送商品排序序號 | 中 |
| TBL_SKU_LIMIT | 商品限量設定 | 中 |
| TBL_EXTRA_CHARGE | 額外工資設定 | 中 |
| TBL_CODE | 代碼表(單位等) | 低 |

---

## 六、SkuInfoVO 欄位遺漏

規格列出約 40 個欄位，實際 VO 有 182 個欄位。重要遺漏欄位分類：

### 6.1 鎖定標記欄位 (結帳後修改用)

| 欄位 | 說明 | 預設值 |
|------|------|--------|
| lockSku | 商品是否可修改 | "N" |
| lockSkuI | 可否修改安裝 | "Y" |
| lockSkuD | 可否修改純運 | "Y" |
| lockSkuV | 可否修改直送 | "N" |
| lockSkuF | 可否修改宅配 | "Y" |

### 6.2 負向商品欄位

| 欄位 | 說明 | 預設值 |
|------|------|--------|
| showDelFlag | 顯示負向按鈕 | "Y" |
| delFlag | 正負項商品註記 | "N" |
| nskuFlag | 負向SKU標記 | "N" |
| nskuDtelSeqId | 對應原SKU序號 | - |

### 6.3 變價相關欄位

| 欄位 | 說明 |
|------|------|
| goodsAuthEmpId | 變價授權者工號 |
| goodsAuthEmpName | 變價授權者姓名 |
| goodsAuthReason | 變價原因 |
| goodsAuthReasonDesc | 變價原因說明 |
| qrcode | MSS 變價 QRCode |
| chgPriceFlag | MSS單品變價 |

### 6.4 預購相關欄位

| 欄位 | 說明 |
|------|------|
| preorderFlag | 可預購註記 |
| preorderDays | 預購天數 |
| preorderInstallationFlag | 可安裝註記 |
| preorderInstallationFee | 安運費 |

### 6.5 紅利相關欄位

| 欄位 | 說明 |
|------|------|
| showBonusPointsWindow | 是否顯示紅利視窗 |
| currentBonusPoints | 紅利點數剩餘量 |
| bonusTotal | 紅利小計 |
| skuCdiscVOList | 紅利折抵活動清單 |

---

## 七、常數遺漏

### 7.1 訂單狀態代碼 (SoConstant.java)

| 常數 | 值 | 說明 |
|------|-----|------|
| SO_STATUS_ID_DRAFTS | "1" | 草稿 |
| SO_STATUS_ID_QUOTE | "2" | 報價 |
| SO_STATUS_ID_PAID | "3" | 已付款 |
| SO_STATUS_ID_VALID | "4" | 有效 |
| SO_STATUS_ID_CLOSE | "5" | 已結案 |
| SO_STATUS_ID_INVALID | "6" | 作廢 |

### 7.2 試算項目分類碼

| 常數 | 值 | 說明 |
|------|-----|------|
| COMPUTE_TYPE_1 | "1" | 商品小計 |
| COMPUTE_TYPE_2 | "2" | 安裝小計 |
| COMPUTE_TYPE_3 | "3" | 運送小計 |
| COMPUTE_TYPE_4 | "4" | 會員卡折扣 |
| COMPUTE_TYPE_5 | "5" | 直送費用小計 |
| COMPUTE_TYPE_6 | "6" | 折價券折扣 |

### 7.3 負向SKU標記

| 常數 | 值 | 說明 |
|------|-----|------|
| NSKU_FLAG_C | "C" | 被負掉的商品 |
| NSKU_FLAG_Y | "Y" | 負向的商品 |

### 7.4 變價原因類型

| 常數 | 值 | 說明 |
|------|-----|------|
| SKU_CHANGE_PRICE_REASONS | "C02" | 商品變價 |
| TAL_CHANGE_PRICE_REASONS | "C03" | 總額變價 |
| INST_CHANGE_PRICE_REASONS | "C06" | 安運變價 |

### 7.5 系統標記

| 常數 | 值 |
|------|-----|
| TSO_SYSTEM | "TSO" |
| HISU_SYSTEM | "HISU" |
| OMS_SYSTEM | "OMS" |
| SO_SYSTEM_FLAG | "SO" |
| TTS_SYSTEM_FLAG | "TTS" |
| EC_SYSTEM_FLAG | "EC" |

---

## 八、前端警告訊息清單

以下警告訊息規格未記載：

| 訊息 | 觸發條件 | 位置 |
|------|---------|------|
| "變價條碼已使用" | 變價條碼重複 | skuBlur (122-136) |
| "請輸入票券商品編號" | 票券訂單選非票券商品 | skuBlur (137-142) |
| "此商品限票券使用" | 非票券訂單選票券商品 | skuBlur (143-148) |
| "廠商狀態已停用，該商品購買量已大於DC倉AOH量，僅能勾選【現貨】" | DC廠商凍結+庫存不足 | isDCVendorDCheck (1091-1093) |
| "該商品購買量已大於DC倉AOH量，僅能勾選【現貨】" | 訂購勾選+DC商品+AOH<quantity | tradeStatusYQuantityCheck |
| "請輸入數量，數量不可為空。" | 數量欄留空 | quantityChange (985-987) |
| "目前會員點數：X，此次使用點數：Y，點數不足抵扣" | 紅利點數不足 | quantityChange (1011-1012) |
| "SKU:XXX/SKU:YYY為刪除品無法下單" | 商品狀態='D' | showChkMsg (2550-2551) |

---

## 九、建議修改項目

### 9.1 高優先級 (必須補充)

1. 新增 OD-006~OD-007 票券商品限制規則
2. 新增 OD-008~OD-009 通路限制規則
3. 新增 OD-010 DIEN 商品處理規則
4. 新增 OD-012~OD-014 DC廠商凍結自動降級規則
5. 補充 holdOrder 採購權限對照表
6. 補充商品狀態組合判斷表

### 9.2 中優先級 (建議補充)

1. 新增 OD-015 masterConfigId 檢查規則
2. 新增 OD-016~OD-017 宅配限制規則
3. 新增 OD-018 免安效期檢查規則
4. 補充遺漏的 8 個資料表到 ER 圖
5. 補充前端警告訊息清單

### 9.3 低優先級 (可選補充)

1. 補充完整的 SkuInfoVO 欄位列表
2. 補充完整的常數定義
3. 補充變價相關邏輯

---

## 十、驗證總結

### 10.1 規格覆蓋率

| 範圍 | 覆蓋率 | 說明 |
|------|--------|------|
| 後端核心邏輯 | 70% | 缺少邊界情況 |
| 前端例外處理 | 30% | 大量遺漏 |
| 資料表定義 | 55% | 缺少促銷/安裝相關表 |
| 常數定義 | 60% | 缺少狀態碼/系統標記 |

### 10.2 風險評估

| 風險項目 | 影響 | 建議 |
|---------|------|------|
| 票券商品規則遺漏 | 高 - 可能導致錯誤訂單 | 立即補充 |
| 通路限制遺漏 | 高 - MINI/CASA用戶受影響 | 立即補充 |
| AOH自動降級遺漏 | 中 - 使用者體驗差異 | 優先補充 |
| 狀態組合判斷不完整 | 中 - 可能顯示錯誤狀態 | 優先補充 |

### 10.3 後續行動

1. **Phase 1**: 補充高優先級遺漏規則 (OD-006 ~ OD-014)
2. **Phase 2**: 更新 ER 圖補充遺漏資料表
3. **Phase 3**: 補充前端警告訊息清單
4. **Phase 4**: 完善 VO 欄位和常數定義

---

## 變更歷史

| 日期 | 版本 | 變更內容 | 作者 |
|------|------|---------|------|
| 2025-12-20 | 1.0.0 | 初版驗證報告 | Claude Code |
