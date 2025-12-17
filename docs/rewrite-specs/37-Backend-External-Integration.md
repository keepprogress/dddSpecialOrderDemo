# 37. Backend - External Integration

## CRM Integration (SOAP)

```java
// CrmClient.java
@Service
public class CrmClient {

    private final WebServiceTemplate webServiceTemplate;

    public CrmClient(WebServiceTemplate webServiceTemplate) {
        this.webServiceTemplate = webServiceTemplate;
    }

    /**
     * 查詢會員資訊
     */
    public MemberInfo getMemberInfo(String memberCardId) {
        GetMemberRequest request = new GetMemberRequest();
        request.setMemberCardId(memberCardId);

        GetMemberResponse response = (GetMemberResponse) webServiceTemplate
            .marshalSendAndReceive(
                "http://crmjbtst.testritegroup.com/RFEP/service/MemberWebService",
                request
            );

        return convertToMemberInfo(response);
    }

    /**
     * 查詢會員折扣
     */
    public MemberDiscount getMemberDiscount(String memberCardId) {
        // 呼叫 CRM SOAP API
        GetMemberDiscountRequest request = new GetMemberDiscountRequest();
        request.setMemberCardId(memberCardId);

        GetMemberDiscountResponse response = (GetMemberDiscountResponse)
            webServiceTemplate.marshalSendAndReceive(
                "http://crmjbtst.testritegroup.com/RFEP/service/MemberWebService",
                request
            );

        return convertToMemberDiscount(response);
    }
}
```

## POS Integration (SOAP)

```java
// PosClient.java
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
            .map(this::convertToPosItem)
            .collect(Collectors.toList());
        request.setItems(items);

        // 呼叫 POS SOAP API
        PosDownloadResponse response = (PosDownloadResponse)
            webServiceTemplate.marshalSendAndReceive(
                "http://pos.system.com/service",
                request
            );

        log.info("Order downloaded to POS: {}, POS Order No: {}",
            order.getOrderId(), response.getPosOrderNo());

        return response;
    }
}
```

## POS Callback Handler

```java
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
            // 驗證簽章
            if (!verifySignature(request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("INVALID_SIGNATURE", "Invalid signature"));
            }

            // 處理回調
            paymentService.handlePosCallback(request);

            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("POS callback failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("CALLBACK_FAILED", e.getMessage()));
        }
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

    private String calculateSignature(String... params) {
        String data = String.join("|", params);
        return DigestUtils.md5Hex(data + SECRET_KEY);
    }
}
```

## Resilience4j 設定

```java
// Resilience4j Configuration
@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .build();
    }

    @Bean
    public RetryConfig retryConfig() {
        return RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(1))
            .build();
    }
}

// 使用範例
@Service
public class MemberService {

    @CircuitBreaker(name = "crm", fallbackMethod = "fallbackGetMember")
    @Retry(name = "crm", maxAttempts = 3)
    @TimeLimiter(name = "crm", timeout = Duration.ofSeconds(3))
    public MemberInfo getMemberInfo(String memberCardId) {
        return crmClient.getMemberInfo(memberCardId);
    }

    // Fallback 方法
    private MemberInfo fallbackGetMember(String memberCardId, Exception e) {
        log.warn("CRM unavailable, using cache for: {}", memberCardId);
        return memberCache.get(memberCardId)
            .orElseThrow(() -> new MemberNotFoundException(memberCardId));
    }
}
```

---

**參考文件**:
- `15-API-Member-Service.md`
- `22-Roadmap-Phase4-Payment-Fulfillment.md`

**文件版本**: v1.0
