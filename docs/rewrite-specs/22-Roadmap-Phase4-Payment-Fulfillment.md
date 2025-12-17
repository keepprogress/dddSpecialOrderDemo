# 22. Roadmap Phase 4 - Payment & Fulfillment

## 目錄

- [1. 階段概述](#1-階段概述)
- [2. 目標與交付成果](#2-目標與交付成果)
- [3. 技術任務](#3-技術任務)
- [4. 時程規劃](#4-時程規劃)
- [5. 驗收標準](#5-驗收標準)

---

## 1. 階段概述

### 1.1 階段定位

**Phase 4: Payment & Fulfillment (付款與履約)**

```plaintext
目標: 實作付款處理與訂單履約流程

時程: 6 週 (Sprint 8-10)

關鍵成果:
├── 付款服務實作 (現金/信用卡/第三方)
├── POS 系統整合
├── 庫存預留與釋放
├── 訂單履約流程
└── Saga Pattern 分散式交易

風險等級: 🔴 極高
- 涉及金流安全
- POS 系統整合複雜
- 分散式交易一致性
- 需與外部系統整合
```

### 1.2 業務範圍

```plaintext
功能範圍:
1. 付款處理
   ├── 準備付款 (Prepare)
   ├── 處理付款 (Process)
   ├── 取消付款 (Cancel)
   └── 退款 (Refund)

2. POS 整合
   ├── 訂單下載 (Download)
   ├── 付款回調 (Callback)
   ├── 發票號碼同步
   └── 簽章驗證

3. 庫存管理
   ├── 庫存檢查
   ├── 庫存預留 (訂單確認時)
   ├── 庫存釋放 (訂單取消時)
   └── 序號管理

4. 訂單履約
   ├── 訂單狀態更新 (有效 → 已付款 → 已結案)
   ├── Saga Orchestration
   └── 補償交易 (Compensating Transaction)
```

---

## 2. 目標與交付成果

### 2.1 主要目標

| 目標 | 說明 | 優先級 |
|-----|------|-------|
| G1 | 付款服務 API 實作 | P0 |
| G2 | POS 系統整合 (SOAP) | P0 |
| G3 | 冪等性設計 (Idempotency-Key) | P0 |
| G4 | 庫存服務 API 實作 | P0 |
| G5 | Saga Pattern 實作 | P0 |
| G6 | 前端付款頁面開發 | P1 |
| G7 | 安全性強化 (簽章驗證) | P0 |

### 2.2 交付成果

```plaintext
1. Payment Service
   ├── POST /api/v1/payments/prepare      # 準備付款
   ├── POST /api/v1/payments/process      # 處理付款 (冪等)
   ├── POST /api/v1/payments/cancel       # 取消付款
   ├── POST /api/v1/payments/refund       # 退款
   └── POST /api/v1/payments/pos/callback # POS 回調

2. Inventory Service
   ├── GET  /api/v1/inventory/check       # 檢查庫存
   ├── POST /api/v1/inventory/reserve     # 預留庫存
   ├── POST /api/v1/inventory/release     # 釋放庫存
   └── POST /api/v1/inventory/commit      # 提交庫存

3. POS Integration
   ├── SOAP Client (訂單下載)
   ├── Signature Verification (簽章驗證)
   └── Callback Handler (付款回調)

4. Saga Orchestrator
   ├── Order Confirmation Saga
   ├── Payment Processing Saga
   └── Order Cancellation Saga

5. Frontend
   ├── payment-confirm.component.ts       # 付款確認頁面
   └── payment-result.component.ts        # 付款結果頁面
```

---

## 3. 技術任務

### 3.1 Task 1: Payment Service 實作 (2 週)

```java
// PaymentService.java
@Service
@Slf4j
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RedisTemplate<String, PaymentResponse> redisTemplate;

    /**
     * 處理付款 (冪等性保證)
     */
    @Transactional
    public PaymentResponse processPayment(
        String idempotencyKey,
        PaymentRequest request
    ) {
        // 1. 檢查冪等鍵
        String cacheKey = "payment:idempotency:" + idempotencyKey;
        PaymentResponse cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("Idempotency key hit: {}", idempotencyKey);
            return cached;
        }

        // 2. 建立付款記錄
        Payment payment = new Payment();
        payment.setPaymentId(generatePaymentId());
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus("PROCESSING");
        paymentRepository.save(payment);

        // 3. 處理付款 (根據付款方式)
        PaymentResponse response = switch (request.getPaymentMethod()) {
            case "CASH" -> processCashPayment(payment);
            case "CREDIT_CARD" -> processCreditCardPayment(payment);
            case "THIRD_PARTY" -> processThirdPartyPayment(payment);
            default -> throw new IllegalArgumentException("Unsupported payment method");
        };

        // 4. 更新付款狀態
        payment.setStatus(response.getStatus());
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 5. 快取結果 (24 小時)
        redisTemplate.opsForValue().set(
            cacheKey, response, Duration.ofHours(24)
        );

        return response;
    }

    /**
     * 現金付款
     */
    private PaymentResponse processCashPayment(Payment payment) {
        // 現金付款立即完成
        return PaymentResponse.builder()
            .paymentId(payment.getPaymentId())
            .status("COMPLETED")
            .paidAt(LocalDateTime.now())
            .build();
    }

    /**
     * 信用卡付款
     */
    private PaymentResponse processCreditCardPayment(Payment payment) {
        // TODO: 串接信用卡閘道
        return PaymentResponse.builder()
            .paymentId(payment.getPaymentId())
            .status("COMPLETED")
            .paidAt(LocalDateTime.now())
            .build();
    }

    /**
     * 第三方支付 (NewebPay, ECPay)
     */
    private PaymentResponse processThirdPartyPayment(Payment payment) {
        // TODO: 串接第三方支付 API
        return PaymentResponse.builder()
            .paymentId(payment.getPaymentId())
            .status("PENDING")
            .gatewayUrl("https://payment.gateway.com/...")
            .build();
    }

    /**
     * POS 回調處理
     */
    @Transactional
    public void handlePosCallback(PosCallbackRequest request) {
        // 1. 驗證簽章
        if (!verifySignature(request)) {
            throw new SecurityException("Invalid signature");
        }

        // 2. 查詢付款記錄
        Payment payment = paymentRepository.findByPosOrderNo(request.getPosOrderNo())
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        // 3. 更新付款狀態
        payment.setStatus("COMPLETED");
        payment.setPosReceiptNo(request.getReceiptNo());
        payment.setPosCallbackAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 4. 發送事件 (通知 Order Service 更新訂單狀態)
        publishPaymentCompletedEvent(payment);
    }

    /**
     * 驗證 POS 簽章
     */
    private boolean verifySignature(PosCallbackRequest request) {
        String expectedSignature = calculateSignature(
            request.getPosOrderNo(),
            request.getAmount(),
            request.getReceiptNo()
        );
        return expectedSignature.equals(request.getSignature());
    }
}
```

### 3.2 Task 2: POS Integration (1 週)

```java
// PosClient.java (SOAP Client)
@Service
@Slf4j
public class PosClient {

    @Autowired
    private WebServiceTemplate webServiceTemplate;

    /**
     * 下載訂單到 POS
     */
    public PosDownloadResponse downloadOrder(Order order) {
        PosDownloadRequest request = new PosDownloadRequest();
        request.setOrderId(order.getOrderId());
        request.setMemberCardId(order.getMemberCardId());
        request.setTotalAmount(order.getFinalTotal());

        // 組裝商品明細
        List<PosOrderItem> items = order.getItems().stream()
            .map(item -> {
                PosOrderItem posItem = new PosOrderItem();
                posItem.setSkuNo(item.getSkuNo());
                posItem.setQuantity(item.getQuantity());
                posItem.setAmount(item.getFinalAmount());
                return posItem;
            })
            .collect(Collectors.toList());
        request.setItems(items);

        // 呼叫 POS SOAP API
        PosDownloadResponse response = (PosDownloadResponse) webServiceTemplate
            .marshalSendAndReceive(
                "http://pos.system.com/service",
                request
            );

        log.info("Order downloaded to POS: {}, POS Order No: {}",
            order.getOrderId(), response.getPosOrderNo());

        return response;
    }
}

// PosCallbackController.java
@RestController
@RequestMapping("/api/v1/payments/pos")
@Slf4j
public class PosCallbackController {

    @Autowired
    private PaymentService paymentService;

    /**
     * POS 付款回調
     */
    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<Void>> handleCallback(
        @RequestBody PosCallbackRequest request
    ) {
        log.info("Received POS callback: {}", request);

        try {
            paymentService.handlePosCallback(request);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (SecurityException e) {
            log.error("Invalid POS callback signature", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("INVALID_SIGNATURE", "Invalid signature"));
        }
    }
}
```

### 3.3 Task 3: Inventory Service (1 週)

```java
// InventoryService.java
@Service
@Slf4j
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    /**
     * 檢查庫存
     */
    public boolean checkAvailability(List<SkuRequest> skus) {
        for (SkuRequest sku : skus) {
            Inventory inventory = inventoryRepository.findBySkuNo(sku.getSkuNo())
                .orElseThrow(() -> new InventoryNotFoundException(sku.getSkuNo()));

            int available = inventory.getQuantity() - inventory.getReserved();
            if (available < sku.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 預留庫存 (訂單確認時)
     */
    @Transactional
    public void reserveInventory(String orderId, List<SkuRequest> skus) {
        for (SkuRequest sku : skus) {
            Inventory inventory = inventoryRepository.findBySkuNo(sku.getSkuNo())
                .orElseThrow(() -> new InventoryNotFoundException(sku.getSkuNo()));

            int available = inventory.getQuantity() - inventory.getReserved();
            if (available < sku.getQuantity()) {
                throw new InsufficientInventoryException(sku.getSkuNo());
            }

            // 建立預留記錄
            InventoryReservation reservation = new InventoryReservation();
            reservation.setOrderId(orderId);
            reservation.setSkuNo(sku.getSkuNo());
            reservation.setQuantity(sku.getQuantity());
            reservation.setExpiresAt(LocalDateTime.now().plusHours(24));
            inventoryReservationRepository.save(reservation);

            // 更新預留數量
            inventory.setReserved(inventory.getReserved() + sku.getQuantity());
            inventoryRepository.save(inventory);
        }

        log.info("Inventory reserved for order: {}", orderId);
    }

    /**
     * 釋放庫存 (訂單取消時)
     */
    @Transactional
    public void releaseInventory(String orderId) {
        List<InventoryReservation> reservations =
            inventoryReservationRepository.findByOrderId(orderId);

        for (InventoryReservation reservation : reservations) {
            Inventory inventory = inventoryRepository.findBySkuNo(reservation.getSkuNo())
                .orElseThrow(() -> new InventoryNotFoundException(reservation.getSkuNo()));

            // 更新預留數量
            inventory.setReserved(inventory.getReserved() - reservation.getQuantity());
            inventoryRepository.save(inventory);

            // 刪除預留記錄
            inventoryReservationRepository.delete(reservation);
        }

        log.info("Inventory released for order: {}", orderId);
    }

    /**
     * 提交庫存 (付款完成時)
     */
    @Transactional
    public void commitInventory(String orderId) {
        List<InventoryReservation> reservations =
            inventoryReservationRepository.findByOrderId(orderId);

        for (InventoryReservation reservation : reservations) {
            Inventory inventory = inventoryRepository.findBySkuNo(reservation.getSkuNo())
                .orElseThrow(() -> new InventoryNotFoundException(reservation.getSkuNo()));

            // 扣減實際庫存
            inventory.setQuantity(inventory.getQuantity() - reservation.getQuantity());
            inventory.setReserved(inventory.getReserved() - reservation.getQuantity());
            inventoryRepository.save(inventory);

            // 刪除預留記錄
            inventoryReservationRepository.delete(reservation);
        }

        log.info("Inventory committed for order: {}", orderId);
    }
}
```

### 3.4 Task 4: Saga Pattern 實作 (2 週)

```java
// OrderConfirmationSaga.java
@Component
@Slf4j
public class OrderConfirmationSaga {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PosClient posClient;

    /**
     * 訂單確認 Saga
     *
     * 步驟:
     * 1. 檢查庫存
     * 2. 預留庫存
     * 3. 下載訂單到 POS
     * 4. 更新訂單狀態 (草稿 → 有效)
     *
     * 補償:
     * - 釋放庫存
     * - 回復訂單狀態
     */
    @Transactional
    public SagaResult executeOrderConfirmation(String orderId) {
        SagaContext context = new SagaContext();

        try {
            // Step 1: 檢查庫存
            Order order = orderService.getOrderById(orderId);
            boolean available = inventoryService.checkAvailability(order.getItems());
            if (!available) {
                return SagaResult.failure("INSUFFICIENT_INVENTORY", "庫存不足");
            }

            // Step 2: 預留庫存
            inventoryService.reserveInventory(orderId, order.getItems());
            context.addCompensation(() -> inventoryService.releaseInventory(orderId));

            // Step 3: 下載訂單到 POS
            PosDownloadResponse posResponse = posClient.downloadOrder(order);
            context.setPosOrderNo(posResponse.getPosOrderNo());

            // Step 4: 更新訂單狀態
            orderService.confirmOrder(orderId);

            log.info("Order confirmation saga completed: {}", orderId);
            return SagaResult.success();

        } catch (Exception e) {
            log.error("Order confirmation saga failed: {}", orderId, e);

            // 執行補償交易
            context.executeCompensations();

            return SagaResult.failure("SAGA_FAILED", e.getMessage());
        }
    }
}

// PaymentProcessingSaga.java
@Component
@Slf4j
public class PaymentProcessingSaga {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderService orderService;

    /**
     * 付款處理 Saga
     *
     * 步驟:
     * 1. 處理付款
     * 2. 提交庫存
     * 3. 更新訂單狀態 (有效 → 已付款)
     *
     * 補償:
     * - 退款
     * - 釋放庫存
     * - 回復訂單狀態
     */
    @Transactional
    public SagaResult executePaymentProcessing(
        String orderId,
        String idempotencyKey,
        PaymentRequest request
    ) {
        SagaContext context = new SagaContext();

        try {
            // Step 1: 處理付款
            PaymentResponse paymentResponse = paymentService.processPayment(
                idempotencyKey, request
            );

            if (!"COMPLETED".equals(paymentResponse.getStatus())) {
                return SagaResult.failure("PAYMENT_FAILED", "付款失敗");
            }

            context.setPaymentId(paymentResponse.getPaymentId());
            context.addCompensation(() ->
                paymentService.refundPayment(paymentResponse.getPaymentId())
            );

            // Step 2: 提交庫存
            inventoryService.commitInventory(orderId);

            // Step 3: 更新訂單狀態
            orderService.markAsPaid(orderId, paymentResponse.getPaymentId());

            log.info("Payment processing saga completed: {}", orderId);
            return SagaResult.success();

        } catch (Exception e) {
            log.error("Payment processing saga failed: {}", orderId, e);

            // 執行補償交易
            context.executeCompensations();

            return SagaResult.failure("SAGA_FAILED", e.getMessage());
        }
    }
}
```

---

## 4. 時程規劃

### 4.1 Gantt Chart

```plaintext
Week 1 (S8)  Week 2       Week 3 (S9)  Week 4       Week 5 (S10) Week 6
│            │            │            │            │            │
├─ Task 1: Payment Service ──────────┤            │            │
│            │            │            │            │            │
│            ├─ Task 2: POS Integration ──────┤    │            │
│            │            │            │            │            │
│            │            ├─ Task 3: Inventory ────┤            │
│            │            │            │            │            │
│            │            │            ├─ Task 4: Saga ─────────┤
│            │            │            │            │            │
│            │            │            │            ├─ 整合測試 ┤
│            │            │            │            │            │
├────────────┼────────────┼────────────┼────────────┼────────────┤
Sprint 8                  Sprint 9                  Sprint 10
```

### 4.2 詳細時程

| 週次 | 任務 | 負責人 | 工時 (人天) |
|-----|------|-------|------------|
| W1-W2 | Task 1: Payment Service | Backend | 10 |
| W2-W3 | Task 2: POS Integration | Backend + Integration | 5 |
| W3-W4 | Task 3: Inventory Service | Backend | 5 |
| W4-W6 | Task 4: Saga Pattern | Backend | 10 |
| W5-W6 | 整合測試 | QA | 5 |

**總工時**: 35 人天

---

## 5. 驗收標準

### 5.1 功能驗收

| 編號 | 驗收項目 | 驗收標準 |
|-----|---------|---------|
| AC-1 | 付款處理 (現金) | 成功處理付款, 訂單狀態更新 |
| AC-2 | 冪等性 | 相同 Idempotency-Key 回傳相同結果 |
| AC-3 | POS 下載 | 訂單成功下載到 POS |
| AC-4 | POS 回調 | 付款完成後回調成功 |
| AC-5 | 庫存預留 | 訂單確認時預留庫存 |
| AC-6 | 庫存釋放 | 訂單取消時釋放庫存 |
| AC-7 | Saga 補償 | 失敗時正確執行補償交易 |

### 5.2 安全驗收

| 編號 | 驗收項目 | 驗收標準 |
|-----|---------|---------|
| S-1 | 簽章驗證 | POS 回調簽章驗證通過 |
| S-2 | 冪等性 | 防止重複扣款 |
| S-3 | 交易一致性 | Saga 補償正確執行 |

---

## 總結

### Phase 4 核心成果

1. ✅ **Payment Service**: 付款處理 + 冪等性設計
2. ✅ **POS Integration**: SOAP 整合 + 簽章驗證
3. ✅ **Inventory Service**: 庫存預留/釋放/提交
4. ✅ **Saga Pattern**: 分散式交易一致性保證

### 下一階段預告

**Phase 5: Testing & Launch (測試與上線)**
- 完整系統測試
- 效能測試與調優
- 安全性測試
- 生產環境部署

---

**參考文件**:
- `03-Order-Payment-Fulfillment-Flow.md`: 付款履約流程
- `14-API-Payment-Service.md`: 付款服務 API
- `18-Idempotency-Design.md`: 冪等性設計

---

**文件版本**: v1.0
**最後更新**: 2025-10-27
**作者**: AI Architecture Team
