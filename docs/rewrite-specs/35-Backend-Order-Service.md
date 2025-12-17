# 35. Backend - Order Service Implementation

## Order Service 核心邏輯

```java
// OrderService.java
@Service
@Slf4j
@Transactional
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
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for member: {}", request.getMemberCardId());

        // 1. 呼叫計價服務
        PricingRequest pricingRequest = buildPricingRequest(request);
        PricingResponse pricingResult = pricingServiceClient.calculatePrice(pricingRequest);

        // 2. 生成訂單編號
        String orderId = generateOrderId();

        // 3. 建立訂單實體
        Order order = Order.builder()
            .orderId(orderId)
            .memberCardId(request.getMemberCardId())
            .channelId(request.getChannelId())
            .statusId("1") // 草稿
            .pricingRequestId(pricingResult.getRequestId())
            .originalTotal(pricingResult.getSummary().getOriginalTotal())
            .finalTotal(pricingResult.getSummary().getFinalTotal())
            .build();

        // 4. 建立訂單明細
        List<OrderItem> items = createOrderItems(order, request.getItems());
        order.setItems(items);

        // 5. 儲存訂單
        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully: {}", orderId);
        return orderMapper.toResponse(savedOrder);
    }

    /**
     * 確認訂單 (草稿 → 有效)
     */
    public OrderResponse confirmOrder(String orderId) {
        Order order = getOrderById(orderId);

        // 驗證狀態轉換
        if (!"1".equals(order.getStatusId())) {
            throw new IllegalStateException("Only draft orders can be confirmed");
        }

        // 更新狀態
        order.setStatusId("4");
        order.setStatusName("有效");
        order.setStatusUpdatedAt(LocalDateTime.now());

        // 記錄狀態歷程
        createStatusHistory(order, "1", "4", "訂單確認");

        // 發送事件
        publishOrderConfirmedEvent(order);

        return orderMapper.toResponse(orderRepository.save(order));
    }

    private String generateOrderId() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 使用 Redis INCR 生成序號
        Long seq = redisTemplate.opsForValue().increment("order:seq:" + date);
        return "SO" + date + String.format("%03d", seq);
    }
}
```

## Order Repository (MyBatis)

```java
// OrderMapper.java (Interface)
@Mapper
public interface OrderMapper {
    void insert(Order order);
    Order selectById(String orderId);
    List<Order> selectByMemberCardId(String memberCardId);
    void update(Order order);
}
```

```xml
<!-- OrderMapper.xml -->
<mapper namespace="com.trihome.som.order.mapper.OrderMapper">

    <insert id="insert" parameterType="Order">
        INSERT INTO orders (
            order_id, member_card_id, channel_id, order_date,
            status_id, status_name, final_total, created_by, created_at
        ) VALUES (
            #{orderId}, #{memberCardId}, #{channelId}, #{orderDate},
            #{statusId}, #{statusName}, #{finalTotal}, #{createdBy}, #{createdAt}
        )
    </insert>

    <select id="selectById" resultType="Order">
        SELECT * FROM orders WHERE order_id = #{orderId} AND is_deleted = 'N'
    </select>

    <select id="selectByMemberCardId" resultType="Order">
        SELECT * FROM orders
        WHERE member_card_id = #{memberCardId}
        AND is_deleted = 'N'
        ORDER BY order_date DESC
    </select>

</mapper>
```

## Order Controller

```java
// OrderController.java
@RestController
@RequestMapping("/api/v1/orders")
@Validated
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
        @RequestBody @Valid OrderRequest request
    ) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
        @PathVariable String orderId
    ) {
        OrderResponse response = orderService.getOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(
        @PathVariable String orderId
    ) {
        OrderResponse response = orderService.confirmOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

---

**參考文件**:
- `02-Order-Creation-Flow.md`
- `12-API-Order-Management.md`

**文件版本**: v1.0
