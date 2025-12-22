package com.tgfc.som.order.controller;

import com.tgfc.som.catalog.dto.InstallationService;
import com.tgfc.som.catalog.dto.InstallationServiceResponse;
import com.tgfc.som.order.dto.AddOrderLineRequest;
import com.tgfc.som.order.dto.ApplyCouponRequest;
import com.tgfc.som.order.dto.CalculationResponse;
import com.tgfc.som.order.dto.CreateOrderRequest;
import com.tgfc.som.order.dto.OrderLineResponse;
import com.tgfc.som.order.dto.OrderResponse;
import com.tgfc.som.order.dto.RedeemBonusRequest;
import com.tgfc.som.order.dto.UpdateOrderLineRequest;
import com.tgfc.som.order.service.OrderService;
import com.tgfc.som.pricing.dto.BonusRedemption;
import com.tgfc.som.pricing.dto.CouponValidation;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 訂單控制器
 *
 * 提供訂單的建立、查詢、行項管理、試算與提交功能
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order", description = "訂單管理 API")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "建立訂單", description = "建立新的訂單，需要提供冪等鍵")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "訂單建立成功"),
        @ApiResponse(responseCode = "400", description = "請求參數錯誤"),
        @ApiResponse(responseCode = "409", description = "重複提交（冪等鍵已使用）")
    })
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("X-Idempotency-Key")
            @Parameter(description = "冪等鍵，用於防止重複提交", required = true)
            String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = extractUserId(jwt);
        log.info("建立訂單請求: userId={}, idempotencyKey={}", userId, idempotencyKey);

        OrderResponse response = orderService.createOrder(request, idempotencyKey, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "取得訂單", description = "根據訂單編號取得訂單資料")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功取得訂單"),
        @ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable
            @Parameter(description = "訂單編號", required = true)
            String orderId
    ) {
        log.info("取得訂單請求: orderId={}", orderId);

        OrderResponse response = orderService.getOrder(orderId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/lines")
    @Operation(summary = "新增訂單行項", description = "新增商品到訂單")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "行項新增成功"),
        @ApiResponse(responseCode = "400", description = "商品不符合銷售資格"),
        @ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    public ResponseEntity<OrderLineResponse> addLine(
            @PathVariable
            @Parameter(description = "訂單編號", required = true)
            String orderId,
            @Valid @RequestBody AddOrderLineRequest request
    ) {
        log.info("新增訂單行項請求: orderId={}, skuNo={}", orderId, request.skuNo());

        OrderLineResponse response = orderService.addLine(orderId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{orderId}/lines/{lineId}")
    @Operation(summary = "更新訂單行項", description = "更新訂單行項的數量或運送方式")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "行項更新成功"),
        @ApiResponse(responseCode = "404", description = "訂單或行項不存在")
    })
    public ResponseEntity<OrderLineResponse> updateLine(
            @PathVariable
            @Parameter(description = "訂單編號", required = true)
            String orderId,
            @PathVariable
            @Parameter(description = "行項編號", required = true)
            String lineId,
            @Valid @RequestBody AddOrderLineRequest request
    ) {
        log.info("更新訂單行項請求: orderId={}, lineId={}", orderId, lineId);

        OrderLineResponse response = orderService.updateLine(orderId, lineId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{orderId}/lines/{lineId}")
    @Operation(summary = "刪除訂單行項", description = "從訂單中刪除商品")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "行項刪除成功"),
        @ApiResponse(responseCode = "404", description = "訂單或行項不存在")
    })
    public ResponseEntity<Void> removeLine(
            @PathVariable
            @Parameter(description = "訂單編號", required = true)
            String orderId,
            @PathVariable
            @Parameter(description = "行項編號", required = true)
            String lineId
    ) {
        log.info("刪除訂單行項請求: orderId={}, lineId={}", orderId, lineId);

        orderService.removeLine(orderId, lineId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{orderId}/lines/{lineId}/installation")
    @Operation(summary = "設定安裝服務", description = "為訂單行項設定安裝服務配置")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "安裝服務設定成功"),
        @ApiResponse(responseCode = "400", description = "工種不存在或服務不可用"),
        @ApiResponse(responseCode = "404", description = "訂單或行項不存在")
    })
    public ResponseEntity<OrderLineResponse> attachInstallation(
            @PathVariable
            @Parameter(description = "訂單編號", required = true)
            String orderId,
            @PathVariable
            @Parameter(description = "行項編號", required = true)
            String lineId,
            @Valid @RequestBody UpdateOrderLineRequest request
    ) {
        log.info("設定安裝服務請求: orderId={}, lineId={}, workTypeId={}",
                orderId, lineId, request.workTypeId());

        OrderLineResponse response = orderService.attachInstallation(
                orderId, lineId,
                request.workTypeId(),
                request.serviceTypes() != null ? request.serviceTypes() : List.of()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{orderId}/lines/{lineId}/delivery")
    @Operation(summary = "設定運送服務", description = "為訂單行項設定運送服務配置")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "運送服務設定成功"),
        @ApiResponse(responseCode = "400", description = "運送方式不合法"),
        @ApiResponse(responseCode = "404", description = "訂單或行項不存在")
    })
    public ResponseEntity<OrderLineResponse> attachDelivery(
            @PathVariable
            @Parameter(description = "訂單編號", required = true)
            String orderId,
            @PathVariable
            @Parameter(description = "行項編號", required = true)
            String lineId,
            @Valid @RequestBody UpdateOrderLineRequest request
    ) {
        log.info("設定運送服務請求: orderId={}, lineId={}, stockMethod={}, deliveryMethod={}",
                orderId, lineId, request.stockMethod(), request.deliveryMethod());

        OrderLineResponse response = orderService.attachDelivery(
                orderId, lineId,
                request.stockMethod(),
                request.deliveryMethod(),
                request.workTypeId(),
                request.receiverName(),
                request.receiverPhone(),
                request.deliveryAddress(),
                request.deliveryZipCode()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}/lines/{lineId}/available-services")
    @Operation(summary = "取得可用安裝服務", description = "查詢行項商品可用的安裝服務清單")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功取得服務清單"),
        @ApiResponse(responseCode = "404", description = "訂單或行項不存在")
    })
    public ResponseEntity<List<InstallationServiceResponse>> getAvailableServices(
            @PathVariable
            @Parameter(description = "訂單編號", required = true)
            String orderId,
            @PathVariable
            @Parameter(description = "行項編號", required = true)
            String lineId
    ) {
        log.info("取得可用安裝服務請求: orderId={}, lineId={}", orderId, lineId);

        // 取得訂單行項以獲取 skuNo
        OrderResponse order = orderService.getOrder(orderId);
        String skuNo = order.lines().stream()
                .filter(l -> l.lineId().equals(lineId))
                .findFirst()
                .map(OrderLineResponse::skuNo)
                .orElseThrow(() -> new IllegalArgumentException("找不到行項: " + lineId));

        List<InstallationService> services = orderService.getAvailableInstallationServices(skuNo);

        // 轉換為回應 DTO
        List<InstallationServiceResponse> responses = services.stream()
                .map(InstallationServiceResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{orderId}/calculate")
    @Operation(summary = "執行價格試算", description = "計算訂單的總金額和折扣")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "試算成功"),
        @ApiResponse(responseCode = "400", description = "訂單沒有商品"),
        @ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    public ResponseEntity<CalculationResponse> calculate(
            @PathVariable
            @Parameter(description = "訂單編號", required = true)
            String orderId
    ) {
        log.info("價格試算請求: orderId={}", orderId);

        CalculationResponse response = orderService.calculate(orderId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/submit")
    @Operation(summary = "提交訂單", description = "提交訂單進行後續處理")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "訂單提交成功"),
        @ApiResponse(responseCode = "400", description = "訂單狀態不允許提交"),
        @ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    public ResponseEntity<OrderResponse> submit(
            @PathVariable
            @Parameter(description = "訂單編號", required = true)
            String orderId
    ) {
        log.info("提交訂單請求: orderId={}", orderId);

        OrderResponse response = orderService.submit(orderId);

        return ResponseEntity.ok(response);
    }

    // === 優惠券相關 API ===

    @PostMapping("/{orderId}/coupons")
    @Operation(summary = "套用優惠券", description = "為訂單套用優惠券折扣")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "優惠券套用成功"),
        @ApiResponse(responseCode = "400", description = "優惠券無效或不適用"),
        @ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    public ResponseEntity<CouponValidation> applyCoupon(
            @PathVariable
            @Parameter(description = "訂單編號", required = true)
            String orderId,
            @Valid @RequestBody ApplyCouponRequest request
    ) {
        log.info("套用優惠券請求: orderId={}, couponId={}", orderId, request.couponId());

        CouponValidation validation = orderService.applyCoupon(orderId, request.couponId());

        if (!validation.valid()) {
            return ResponseEntity.badRequest().body(validation);
        }

        return ResponseEntity.ok(validation);
    }

    @GetMapping("/{orderId}/coupons/validate")
    @Operation(summary = "驗證優惠券", description = "驗證優惠券是否可套用於訂單")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "驗證完成"),
        @ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    public ResponseEntity<CouponValidation> validateCoupon(
            @PathVariable
            @Parameter(description = "訂單編號", required = true)
            String orderId,
            @Parameter(description = "優惠券編號", required = true)
            String couponId
    ) {
        log.info("驗證優惠券請求: orderId={}, couponId={}", orderId, couponId);

        CouponValidation validation = orderService.validateCoupon(orderId, couponId);

        return ResponseEntity.ok(validation);
    }

    @DeleteMapping("/{orderId}/coupons")
    @Operation(summary = "移除優惠券", description = "從訂單中移除已套用的優惠券")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "優惠券移除成功"),
        @ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    public ResponseEntity<Void> removeCoupon(
            @PathVariable
            @Parameter(description = "訂單編號", required = true)
            String orderId
    ) {
        log.info("移除優惠券請求: orderId={}", orderId);

        orderService.removeCoupon(orderId);

        return ResponseEntity.noContent().build();
    }

    // === 紅利點數相關 API ===

    @GetMapping("/{orderId}/bonus/available")
    @Operation(summary = "查詢可用紅利", description = "查詢會員可用的紅利點數")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查詢成功"),
        @ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    public ResponseEntity<Integer> getAvailableBonusPoints(
            @PathVariable
            @Parameter(description = "訂單編號", required = true)
            String orderId
    ) {
        log.info("查詢可用紅利請求: orderId={}", orderId);

        int availablePoints = orderService.getAvailableBonusPoints(orderId);

        return ResponseEntity.ok(availablePoints);
    }

    @PostMapping("/{orderId}/bonus")
    @Operation(summary = "紅利折抵", description = "使用紅利點數折抵商品金額")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "紅利折抵成功"),
        @ApiResponse(responseCode = "400", description = "紅利點數不足或不可使用"),
        @ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    public ResponseEntity<BonusRedemption> redeemBonusPoints(
            @PathVariable
            @Parameter(description = "訂單編號", required = true)
            String orderId,
            @Valid @RequestBody RedeemBonusRequest request
    ) {
        log.info("紅利折抵請求: orderId={}, skuNo={}, points={}",
                orderId, request.skuNo(), request.points());

        BonusRedemption redemption = orderService.redeemBonusPoints(
                orderId, request.skuNo(), request.points());

        return ResponseEntity.ok(redemption);
    }

    @DeleteMapping("/{orderId}/bonus/{skuNo}")
    @Operation(summary = "取消紅利折抵", description = "取消商品的紅利點數折抵")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "紅利折抵取消成功"),
        @ApiResponse(responseCode = "400", description = "此商品沒有紅利折抵"),
        @ApiResponse(responseCode = "404", description = "訂單不存在")
    })
    public ResponseEntity<Void> cancelBonusRedemption(
            @PathVariable
            @Parameter(description = "訂單編號", required = true)
            String orderId,
            @PathVariable
            @Parameter(description = "商品編號", required = true)
            String skuNo
    ) {
        log.info("取消紅利折抵請求: orderId={}, skuNo={}", orderId, skuNo);

        orderService.cancelBonusRedemption(orderId, skuNo);

        return ResponseEntity.noContent().build();
    }

    private String extractUserId(Jwt jwt) {
        if (jwt == null) {
            return "anonymous";
        }
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        return preferredUsername != null ? preferredUsername : jwt.getSubject();
    }
}
