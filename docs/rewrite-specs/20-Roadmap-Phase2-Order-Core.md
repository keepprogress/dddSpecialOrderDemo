# 20. Roadmap Phase 2 - Order Core Implementation

## 目錄

- [1. 階段概述](#1-階段概述)
- [2. 目標與交付成果](#2-目標與交付成果)
- [3. 技術任務](#3-技術任務)
- [4. 時程規劃](#4-時程規劃)
- [5. 驗收標準](#5-驗收標準)

---

## 1. 階段概述

### 1.1 階段定位

**Phase 2: Order Core Implementation (訂單核心功能)**

```plaintext
目標: 實作訂單核心功能 (CRUD + 狀態管理)

時程: 4 週 (Sprint 3-4)

關鍵成果:
├── 訂單 CRUD API 完成
├── 訂單狀態管理實作
├── 前端訂單頁面開發
├── 整合計價服務
└── 單元測試與整合測試

風險等級: 🟡 中
- 開始影響業務邏輯
- 需與計價服務整合
- 前後端協作
```

### 1.2 業務範圍

```plaintext
功能範圍:
1. 訂單建立 (Create Order)
   ├── 輸入會員資訊
   ├── 選擇商品與數量
   ├── 選擇工種 (安裝/測量)
   ├── 呼叫計價服務
   └── 儲存訂單

2. 訂單查詢 (Read Order)
   ├── 訂單詳情查詢
   ├── 訂單清單 (分頁)
   ├── 條件搜尋 (會員、狀態、日期)
   └── 匯出 Excel

3. 訂單修改 (Update Order)
   ├── 修改商品明細
   ├── 修改配送資訊
   └── 重新計價

4. 訂單狀態管理
   ├── 確認訂單 (草稿 → 有效)
   ├── 取消訂單 (有效 → 作廢)
   └── 狀態歷程查詢

不包含 (後續階段):
❌ 付款功能 (Phase 4)
❌ POS 整合 (Phase 4)
❌ 庫存預留 (Phase 4)
```

---

## 2. 目標與交付成果

### 2.1 主要目標

| 目標 | 說明 | 優先級 |
|-----|------|-------|
| G1 | 訂單 CRUD API 實作完成 | P0 |
| G2 | 訂單狀態管理實作完成 | P0 |
| G3 | 前端訂單建立頁面完成 | P0 |
| G4 | 前端訂單查詢頁面完成 | P0 |
| G5 | 整合計價服務 API | P0 |
| G6 | 單元測試覆蓋率 ≥ 80% | P1 |
| G7 | 整合測試完成 | P1 |

### 2.2 交付成果

```plaintext
1. Backend API (Order Service)
   ├── POST   /api/v1/orders              # 建立訂單
   ├── GET    /api/v1/orders/{orderId}    # 查詢訂單
   ├── PUT    /api/v1/orders/{orderId}    # 更新訂單
   ├── DELETE /api/v1/orders/{orderId}    # 刪除訂單
   ├── GET    /api/v1/orders              # 訂單清單 (分頁)
   ├── POST   /api/v1/orders/{orderId}/confirm  # 確認訂單
   ├── POST   /api/v1/orders/{orderId}/cancel   # 取消訂單
   └── GET    /api/v1/orders/{orderId}/history  # 狀態歷程

2. Frontend Pages (Angular 8)
   ├── order-create.component.ts         # 訂單建立頁面
   ├── order-list.component.ts           # 訂單清單頁面
   ├── order-detail.component.ts         # 訂單詳情頁面
   └── order-edit.component.ts           # 訂單編輯頁面

3. Database Migration
   ├── V1.0.0__create_orders_table.sql
   ├── V1.0.1__create_order_items_table.sql
   └── V1.0.2__create_order_status_history_table.sql

4. Tests
   ├── Unit Tests (JUnit 5)
   ├── Integration Tests (Spring Boot Test)
   └── E2E Tests (Cypress)

5. Documentation
   ├── API Documentation (Swagger)
   └── User Guide.md
```

---

## 3. 技術任務

### 3.1 Task 1: Backend - Order CRUD API (2 週)

#### 3.1.1 Entity 定義

```java
// Order.java
@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @Column(name = "order_id", length = 20)
    private String orderId;              // SO20251027001

    @Column(name = "member_card_id", length = 20)
    private String memberCardId;

    @Column(name = "channel_id", length = 10, nullable = false)
    private String channelId;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "status_id", length = 2, nullable = false)
    private String statusId;             // 1:草稿 2:報價 4:有效 3:已付款 5:已結案 6:作廢

    @Column(name = "status_name", length = 20)
    private String statusName;

    @Column(name = "pricing_request_id", length = 36)
    private String pricingRequestId;     // 計價請求 ID

    @Column(name = "original_total", precision = 10, scale = 2)
    private BigDecimal originalTotal;

    @Column(name = "discount_total", precision = 10, scale = 2)
    private BigDecimal discountTotal;

    @Column(name = "final_total", precision = 10, scale = 2)
    private BigDecimal finalTotal;

    @Column(name = "is_deleted", length = 1)
    private String isDeleted = "N";      // 軟刪除

    @Column(name = "version")
    @Version
    private Integer version = 1;         // 樂觀鎖

    @Column(name = "created_by", length = 20, nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 20)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}

// OrderItem.java
@Data
@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_items_seq")
    @SequenceGenerator(name = "order_items_seq", sequenceName = "seq_order_items_id", allocationSize = 1)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "sku_no", length = 20, nullable = false)
    private String skuNo;

    @Column(name = "sku_name", length = 100)
    private String skuName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "original_amount", precision = 10, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "is_deleted", length = 1)
    private String isDeleted = "N";

    @Column(name = "created_by", length = 20, nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
```

#### 3.1.2 Repository

```java
// OrderRepository.java
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    /**
     * 查詢會員訂單 (分頁)
     */
    Page<Order> findByMemberCardIdAndIsDeleted(
        String memberCardId,
        String isDeleted,
        Pageable pageable
    );

    /**
     * 查詢指定狀態訂單
     */
    List<Order> findByStatusIdAndIsDeleted(String statusId, String isDeleted);

    /**
     * 條件查詢 (使用 Specification)
     */
    Page<Order> findAll(Specification<Order> spec, Pageable pageable);
}
```

#### 3.1.3 Service

```java
// OrderService.java
@Service
@Slf4j
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PricingServiceClient pricingServiceClient;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 建立訂單
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for member: {}", request.getMemberCardId());

        // 1. 呼叫計價服務
        PricingRequest pricingRequest = PricingRequest.builder()
            .memberCardId(request.getMemberCardId())
            .skus(request.getItems())
            .channelId(request.getChannelId())
            .build();

        PricingResponse pricingResult = pricingServiceClient.calculatePrice(pricingRequest);

        // 2. 生成訂單編號
        String orderId = generateOrderId();

        // 3. 建立訂單實體
        Order order = new Order();
        order.setOrderId(orderId);
        order.setMemberCardId(request.getMemberCardId());
        order.setChannelId(request.getChannelId());
        order.setOrderDate(LocalDateTime.now());
        order.setStatusId("1");  // 草稿
        order.setStatusName("草稿");

        // 4. 儲存計價結果
        order.setPricingRequestId(pricingResult.getRequestId());
        order.setOriginalTotal(pricingResult.getSummary().getOriginalTotal());
        order.setDiscountTotal(pricingResult.getSummary().getDiscountTotal());
        order.setFinalTotal(pricingResult.getSummary().getFinalTotal());

        // 5. 建立訂單明細
        List<OrderItem> items = request.getItems().stream()
            .map(itemReq -> {
                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setSkuNo(itemReq.getSkuNo());
                item.setQuantity(itemReq.getQuantity());
                // ... 設定其他欄位
                return item;
            })
            .collect(Collectors.toList());
        order.setItems(items);

        // 6. 儲存訂單
        order.setCreatedBy(getCurrentUsername());
        order.setCreatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        // 7. 記錄狀態歷程
        createStatusHistory(savedOrder, null, "1", "訂單建立");

        log.info("Order created successfully: {}", orderId);
        return orderMapper.toResponse(savedOrder);
    }

    /**
     * 查詢訂單詳情
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if ("Y".equals(order.getIsDeleted())) {
            throw new OrderNotFoundException("Order has been deleted: " + orderId);
        }

        return orderMapper.toResponse(order);
    }

    /**
     * 更新訂單
     */
    @Transactional
    public OrderResponse updateOrder(String orderId, OrderRequest request) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        // 檢查訂單狀態 (只有草稿狀態可修改)
        if (!"1".equals(order.getStatusId())) {
            throw new IllegalStateException("Only draft orders can be updated");
        }

        // 重新計價
        PricingRequest pricingRequest = PricingRequest.builder()
            .memberCardId(request.getMemberCardId())
            .skus(request.getItems())
            .channelId(request.getChannelId())
            .build();

        PricingResponse pricingResult = pricingServiceClient.calculatePrice(pricingRequest);

        // 更新訂單
        order.setPricingRequestId(pricingResult.getRequestId());
        order.setOriginalTotal(pricingResult.getSummary().getOriginalTotal());
        order.setDiscountTotal(pricingResult.getSummary().getDiscountTotal());
        order.setFinalTotal(pricingResult.getSummary().getFinalTotal());
        order.setUpdatedBy(getCurrentUsername());
        order.setUpdatedAt(LocalDateTime.now());

        // 更新明細 (先刪除舊明細)
        order.getItems().clear();
        List<OrderItem> newItems = request.getItems().stream()
            .map(itemReq -> {
                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setSkuNo(itemReq.getSkuNo());
                item.setQuantity(itemReq.getQuantity());
                // ... 設定其他欄位
                return item;
            })
            .collect(Collectors.toList());
        order.getItems().addAll(newItems);

        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toResponse(updatedOrder);
    }

    /**
     * 刪除訂單 (軟刪除)
     */
    @Transactional
    public void deleteOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        // 檢查訂單狀態 (只有草稿狀態可刪除)
        if (!"1".equals(order.getStatusId())) {
            throw new IllegalStateException("Only draft orders can be deleted");
        }

        // 軟刪除
        order.setIsDeleted("Y");
        order.setDeletedBy(getCurrentUsername());
        order.setDeletedAt(LocalDateTime.now());

        orderRepository.save(order);
        log.info("Order deleted: {}", orderId);
    }

    /**
     * 訂單清單查詢 (分頁)
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> listOrders(OrderSearchRequest request) {
        Pageable pageable = PageRequest.of(
            request.getPage(),
            request.getSize(),
            Sort.by("orderDate").descending()
        );

        // 使用 Specification 動態查詢
        Specification<Order> spec = OrderSpecification.build(request);

        Page<Order> orders = orderRepository.findAll(spec, pageable);

        return PageResponse.from(orders, orderMapper::toSummary);
    }

    /**
     * 生成訂單編號: SO + YYYYMMDD + 流水號
     */
    private String generateOrderId() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // TODO: 實作流水號邏輯 (Redis INCR)
        String seq = "001";
        return "SO" + date + seq;
    }

    private String getCurrentUsername() {
        // TODO: 從 Security Context 取得當前用戶
        return "system";
    }
}
```

#### 3.1.4 Controller

```java
// OrderController.java
@RestController
@RequestMapping("/api/v1/orders")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 建立訂單
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
        @RequestBody @Valid OrderRequest request
    ) {
        log.info("Received create order request for member: {}", request.getMemberCardId());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }

    /**
     * 查詢訂單詳情
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
        @PathVariable String orderId
    ) {
        OrderResponse response = orderService.getOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 更新訂單
     */
    @PutMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
        @PathVariable String orderId,
        @RequestBody @Valid OrderRequest request
    ) {
        OrderResponse response = orderService.updateOrder(orderId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 刪除訂單 (軟刪除)
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
        @PathVariable String orderId
    ) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 訂單清單查詢 (分頁)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderSummary>>> listOrders(
        @Valid OrderSearchRequest request
    ) {
        PageResponse<OrderSummary> response = orderService.listOrders(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### 3.2 Task 2: Backend - Order Status Management (1 週)

```java
// OrderStatusService.java
@Service
@Slf4j
public class OrderStatusService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderStatusHistoryRepository statusHistoryRepository;

    /**
     * 確認訂單 (草稿 → 有效)
     */
    @Transactional
    public OrderResponse confirmOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        // 驗證狀態轉換
        validateStatusTransition(order.getStatusId(), "4");

        // 更新狀態
        String oldStatus = order.getStatusId();
        order.setStatusId("4");
        order.setStatusName("有效");
        order.setStatusUpdatedAt(LocalDateTime.now());
        order.setStatusUpdatedBy(getCurrentUsername());

        Order updatedOrder = orderRepository.save(order);

        // 記錄狀態歷程
        createStatusHistory(order, oldStatus, "4", "訂單確認");

        // TODO: 發送事件 (OrderConfirmedEvent)

        log.info("Order confirmed: {}", orderId);
        return orderMapper.toResponse(updatedOrder);
    }

    /**
     * 取消訂單 (有效 → 作廢)
     */
    @Transactional
    public OrderResponse cancelOrder(String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        // 驗證狀態轉換
        validateStatusTransition(order.getStatusId(), "6");

        // 更新狀態
        String oldStatus = order.getStatusId();
        order.setStatusId("6");
        order.setStatusName("作廢");
        order.setStatusUpdatedAt(LocalDateTime.now());
        order.setStatusUpdatedBy(getCurrentUsername());

        Order updatedOrder = orderRepository.save(order);

        // 記錄狀態歷程
        createStatusHistory(order, oldStatus, "6", "訂單取消: " + reason);

        // TODO: 發送事件 (OrderCancelledEvent)

        log.info("Order cancelled: {}", orderId);
        return orderMapper.toResponse(updatedOrder);
    }

    /**
     * 驗證狀態轉換
     */
    private void validateStatusTransition(String fromStatus, String toStatus) {
        // 狀態轉換矩陣 (參考 01-Order-Status-Lifecycle.md)
        Map<String, Set<String>> allowedTransitions = Map.of(
            "1", Set.of("2", "4", "6"),  // 草稿 → 報價/有效/作廢
            "2", Set.of("4", "6"),       // 報價 → 有效/作廢
            "4", Set.of("3", "6"),       // 有效 → 已付款/作廢
            "3", Set.of("5")             // 已付款 → 已結案
        );

        if (!allowedTransitions.getOrDefault(fromStatus, Set.of()).contains(toStatus)) {
            throw new IllegalStateException(
                String.format("Invalid status transition: %s → %s", fromStatus, toStatus)
            );
        }
    }

    /**
     * 建立狀態歷程記錄
     */
    private void createStatusHistory(Order order, String fromStatus, String toStatus, String reason) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setFromStatusId(fromStatus);
        history.setFromStatusName(getStatusName(fromStatus));
        history.setToStatusId(toStatus);
        history.setToStatusName(getStatusName(toStatus));
        history.setReason(reason);
        history.setChangedBy(getCurrentUsername());
        history.setChangedAt(LocalDateTime.now());

        statusHistoryRepository.save(history);
    }

    private String getStatusName(String statusId) {
        return switch (statusId) {
            case "1" -> "草稿";
            case "2" -> "報價";
            case "3" -> "已付款";
            case "4" -> "有效";
            case "5" -> "已結案";
            case "6" -> "作廢";
            default -> "未知";
        };
    }
}
```

### 3.3 Task 3: Frontend - Order Pages (2 週)

#### 3.3.1 訂單建立頁面

```typescript
// order-create.component.ts
@Component({
  selector: 'app-order-create',
  templateUrl: './order-create.component.html',
  styleUrls: ['./order-create.component.scss']
})
export class OrderCreateComponent implements OnInit {
  orderForm: FormGroup;
  pricingResult: PricingResponse | null = null;
  isCalculating = false;
  isSubmitting = false;

  constructor(
    private fb: FormBuilder,
    private orderService: OrderService,
    private pricingService: PricingService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.orderForm = this.fb.group({
      memberCardId: ['', [Validators.required, Validators.pattern(/^[A-Z]\d{9}$/)]],
      channelId: ['', Validators.required],
      items: this.fb.array([], Validators.minLength(1)),
      deliveryAddress: [''],
      deliveryContact: [''],
      deliveryPhone: [''],
      remarks: ['']
    });
  }

  ngOnInit(): void {}

  get items(): FormArray {
    return this.orderForm.get('items') as FormArray;
  }

  /**
   * 新增商品明細
   */
  addItem(): void {
    const itemGroup = this.fb.group({
      skuNo: ['', Validators.required],
      skuName: [''],
      quantity: [1, [Validators.required, Validators.min(1)]],
      workTypeId: [''],
      workTypeName: ['']
    });

    this.items.push(itemGroup);
  }

  /**
   * 移除商品明細
   */
  removeItem(index: number): void {
    this.items.removeAt(index);
  }

  /**
   * 計算價格
   */
  calculatePrice(): void {
    if (this.orderForm.invalid) {
      this.snackBar.open('請填寫必填欄位', '關閉', { duration: 3000 });
      return;
    }

    this.isCalculating = true;

    const request: PricingRequest = {
      memberCardId: this.orderForm.value.memberCardId,
      skus: this.orderForm.value.items,
      channelId: this.orderForm.value.channelId
    };

    this.pricingService.calculatePrice(request)
      .pipe(finalize(() => this.isCalculating = false))
      .subscribe(
        response => {
          this.pricingResult = response;
          this.snackBar.open('計價成功', '關閉', { duration: 2000 });
        },
        error => {
          this.snackBar.open('計價失敗: ' + error.message, '關閉', { duration: 3000 });
        }
      );
  }

  /**
   * 送出訂單
   */
  submitOrder(): void {
    if (this.orderForm.invalid || !this.pricingResult) {
      this.snackBar.open('請先計算價格', '關閉', { duration: 3000 });
      return;
    }

    this.isSubmitting = true;

    const request: OrderRequest = {
      ...this.orderForm.value,
      pricingRequestId: this.pricingResult.requestId
    };

    this.orderService.createOrder(request)
      .pipe(finalize(() => this.isSubmitting = false))
      .subscribe(
        response => {
          this.snackBar.open('訂單建立成功: ' + response.orderId, '關閉', { duration: 3000 });
          this.router.navigate(['/orders', response.orderId]);
        },
        error => {
          this.snackBar.open('訂單建立失敗: ' + error.message, '關閉', { duration: 3000 });
        }
      );
  }
}
```

---

## 4. 時程規劃

### 4.1 Gantt Chart

```plaintext
Week 1 (Sprint 3)   Week 2              Week 3 (Sprint 4)   Week 4
│                   │                   │                   │
├─ Task 1: Order CRUD API ───────────────┤
│  ├─ Entity & Repository  │             │
│  ├─ Service Layer        │             │
│  └─ Controller           │             │
│                          │             │
│  ├─ Task 2: Status Management ────┤    │
│                          │         │    │
│  ├─ Task 3: Frontend Pages ───────────────────┤
│                          │         │           │
│                          │         ├─ Testing ─┤
│                          │         │           │
├──────────────────────────┼─────────┼───────────┤
Sprint 3                   Sprint 4
```

### 4.2 詳細時程

| 週次 | 任務 | 負責人 | 工時 (人天) | 狀態 |
|-----|------|-------|------------|------|
| W1 | Task 1.1: Entity & Repository | Backend | 2 | 🟡 待開始 |
| W1 | Task 1.2: Service Layer | Backend | 3 | 🟡 待開始 |
| W2 | Task 1.3: Controller | Backend | 2 | 🟡 待開始 |
| W2 | Task 2: Status Management | Backend | 3 | 🟡 待開始 |
| W2-W3 | Task 3.1: 訂單建立頁面 | Frontend | 5 | 🟡 待開始 |
| W3 | Task 3.2: 訂單清單頁面 | Frontend | 3 | 🟡 待開始 |
| W3 | Task 3.3: 訂單詳情頁面 | Frontend | 2 | 🟡 待開始 |
| W4 | 單元測試 | Backend + Frontend | 3 | 🟡 待開始 |
| W4 | 整合測試 | QA | 2 | 🟡 待開始 |

**總工時**: 25 人天

---

## 5. 驗收標準

### 5.1 功能驗收

| 編號 | 驗收項目 | 驗收標準 | 驗收方式 |
|-----|---------|---------|---------|
| AC-1 | 訂單建立 API | 成功建立訂單, 回傳 201 + 訂單 ID | Postman 測試 |
| AC-2 | 訂單查詢 API | 查詢訂單詳情, 回傳完整資料 | Postman 測試 |
| AC-3 | 訂單更新 API | 更新訂單明細, 重新計價成功 | Postman 測試 |
| AC-4 | 訂單刪除 API | 軟刪除訂單, 狀態標記為已刪除 | Postman 測試 |
| AC-5 | 訂單確認 API | 草稿 → 有效, 記錄狀態歷程 | Postman 測試 |
| AC-6 | 訂單取消 API | 有效 → 作廢, 記錄取消原因 | Postman 測試 |
| AC-7 | 前端建立頁面 | 填寫表單、計價、送出成功 | 手動測試 |
| AC-8 | 前端清單頁面 | 顯示訂單清單、分頁、搜尋 | 手動測試 |

### 5.2 測試覆蓋率

| 類型 | 目標 | 實際 | 狀態 |
|-----|------|------|------|
| 單元測試覆蓋率 (Backend) | ≥ 80% | - | 🟡 待測試 |
| 單元測試覆蓋率 (Frontend) | ≥ 70% | - | 🟡 待測試 |
| 整合測試通過率 | 100% | - | 🟡 待測試 |
| E2E 測試通過率 | 100% | - | 🟡 待測試 |

---

## 總結

### Phase 2 核心成果

1. ✅ **Backend API**: 訂單 CRUD + 狀態管理完成
2. ✅ **Frontend Pages**: 訂單建立、查詢、編輯頁面完成
3. ✅ **Integration**: 整合計價服務 API
4. ✅ **Testing**: 單元測試 + 整合測試完成

### 下一階段預告

**Phase 3: Pricing Service Refactor (計價服務重構)**
- 實作 12 步驟計價邏輯
- Redis 快取優化
- 會員折扣計算
- 促銷規則引擎

---

**參考文件**:
- `01-Order-Status-Lifecycle.md`: 訂單狀態生命週期
- `02-Order-Creation-Flow.md`: 訂單建立流程
- `12-API-Order-Management.md`: 訂單管理 API
- `16-Database-Design.md`: 資料庫設計

---

**文件版本**: v1.0
**最後更新**: 2025-10-27
**作者**: AI Architecture Team
