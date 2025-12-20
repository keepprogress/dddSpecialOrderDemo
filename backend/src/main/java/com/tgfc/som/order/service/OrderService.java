package com.tgfc.som.order.service;

import com.tgfc.som.catalog.dto.EligibilityResponse;
import com.tgfc.som.catalog.dto.InstallationService;
import com.tgfc.som.catalog.dto.ProductInfo;
import com.tgfc.som.catalog.service.ProductEligibilityService;
import com.tgfc.som.catalog.service.ProductServiceAssociationService;
import com.tgfc.som.fulfillment.dto.WorkType;
import com.tgfc.som.fulfillment.service.WorkTypeService;
import com.tgfc.som.common.exception.BusinessException;
import com.tgfc.som.common.exception.DuplicateSubmissionException;
import com.tgfc.som.common.service.IdempotencyService;
import com.tgfc.som.member.domain.MemberDiscountType;
import com.tgfc.som.order.domain.DeliveryMethod;
import com.tgfc.som.order.domain.Order;
import com.tgfc.som.order.domain.OrderLine;
import com.tgfc.som.order.domain.StockMethod;
import com.tgfc.som.order.domain.TaxType;
import com.tgfc.som.order.domain.valueobject.Customer;
import com.tgfc.som.order.domain.valueobject.DeliveryAddress;
import com.tgfc.som.order.domain.valueobject.DeliveryDetail;
import com.tgfc.som.order.domain.valueobject.InstallationDetail;
import com.tgfc.som.order.domain.valueobject.LineId;
import com.tgfc.som.order.domain.valueobject.Money;
import com.tgfc.som.order.domain.valueobject.OrderId;
import com.tgfc.som.order.domain.valueobject.PriceCalculation;
import com.tgfc.som.order.domain.valueobject.ProjectId;
import com.tgfc.som.order.dto.AddOrderLineRequest;
import com.tgfc.som.order.dto.CalculationResponse;
import com.tgfc.som.order.dto.CreateOrderRequest;
import com.tgfc.som.order.dto.OrderLineResponse;
import com.tgfc.som.order.dto.OrderResponse;
import com.tgfc.som.pricing.dto.BonusRedemption;
import com.tgfc.som.pricing.dto.ComputeTypeVO;
import com.tgfc.som.pricing.dto.CouponValidation;
import com.tgfc.som.pricing.service.BonusService;
import com.tgfc.som.pricing.service.CouponService;
import com.tgfc.som.pricing.service.PriceCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 訂單服務
 *
 * 負責訂單的建立、修改、試算與提交
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final IdempotencyService idempotencyService;
    private final ProductEligibilityService productEligibilityService;
    private final PriceCalculationService priceCalculationService;
    private final ProductServiceAssociationService productServiceAssociationService;
    private final WorkTypeService workTypeService;
    private final CouponService couponService;
    private final BonusService bonusService;

    // 暫時使用記憶體儲存（後續會改為 Repository）
    private final Map<String, Order> orderStore = new ConcurrentHashMap<>();
    private final AtomicLong orderSequence = new AtomicLong(3000000000L);
    private final AtomicLong projectSequence = new AtomicLong(1L);

    public OrderService(
            IdempotencyService idempotencyService,
            ProductEligibilityService productEligibilityService,
            PriceCalculationService priceCalculationService,
            ProductServiceAssociationService productServiceAssociationService,
            WorkTypeService workTypeService,
            CouponService couponService,
            BonusService bonusService
    ) {
        this.idempotencyService = idempotencyService;
        this.productEligibilityService = productEligibilityService;
        this.priceCalculationService = priceCalculationService;
        this.productServiceAssociationService = productServiceAssociationService;
        this.workTypeService = workTypeService;
        this.couponService = couponService;
        this.bonusService = bonusService;
    }

    /**
     * 建立訂單
     */
    public OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey, String userId) {
        log.info("建立訂單: idempotencyKey={}, storeId={}, channelId={}",
                idempotencyKey, request.storeId(), request.channelId());

        // 冪等性檢查
        try {
            idempotencyService.checkAndThrow(idempotencyKey);
        } catch (DuplicateSubmissionException e) {
            String existingOrderId = e.getExistingOrderId();
            if (existingOrderId != null) {
                Order existingOrder = orderStore.get(existingOrderId);
                if (existingOrder != null) {
                    return toOrderResponse(existingOrder);
                }
            }
            throw e;
        }

        // 建立訂單 ID 與專案代號
        OrderId orderId = generateOrderId();
        ProjectId projectId = generateProjectId(request.storeId());

        // 建立客戶資訊
        Customer customer = toCustomer(request.customer());

        // 建立地址
        DeliveryAddress address = new DeliveryAddress(
                request.address().zipCode(),
                request.address().fullAddress()
        );

        // 建立訂單
        Order order = new Order(
                orderId,
                projectId,
                customer,
                address,
                request.storeId(),
                request.channelId(),
                userId
        );
        order.setIdempotencyKey(idempotencyKey);
        order.setHandlerId(userId);

        // 儲存訂單
        orderStore.put(orderId.value(), order);

        // 記錄冪等鍵
        idempotencyService.record(idempotencyKey, orderId.value());

        log.info("訂單建立成功: orderId={}, projectId={}", orderId.value(), projectId.value());

        return toOrderResponse(order);
    }

    /**
     * 取得訂單
     */
    public OrderResponse getOrder(String orderId) {
        Order order = findOrderOrThrow(orderId);
        return toOrderResponse(order);
    }

    /**
     * 新增訂單行項
     */
    public OrderLineResponse addLine(String orderId, AddOrderLineRequest request) {
        log.info("新增訂單行項: orderId={}, skuNo={}, quantity={}",
                orderId, request.skuNo(), request.quantity());

        Order order = findOrderOrThrow(orderId);

        // 驗證商品銷售資格
        EligibilityResponse eligibility = productEligibilityService.checkEligibility(
                request.skuNo(), order.getChannelId(), order.getStoreId());

        if (!eligibility.eligible()) {
            throw new BusinessException("PRODUCT_NOT_ELIGIBLE", eligibility.failureReason());
        }

        ProductInfo product = eligibility.product();

        // 解析運送方式與備貨方式
        DeliveryMethod deliveryMethod = DeliveryMethod.fromCodeOrThrow(request.deliveryMethod());
        StockMethod stockMethod = StockMethod.fromCodeOrThrow(request.stockMethod());
        TaxType taxType = TaxType.fromCodeOrThrow(product.taxType());

        // 新增行項
        OrderLine line = order.addLine(
                product.skuNo(),
                product.skuName(),
                request.quantity(),
                Money.of(product.posPrice()),
                taxType,
                deliveryMethod,
                stockMethod
        );

        log.info("訂單行項新增成功: orderId={}, lineId={}", orderId, line.getId().value());

        return toOrderLineResponse(line);
    }

    /**
     * 更新訂單行項
     */
    public OrderLineResponse updateLine(String orderId, String lineId, AddOrderLineRequest request) {
        log.info("更新訂單行項: orderId={}, lineId={}", orderId, lineId);

        Order order = findOrderOrThrow(orderId);
        OrderLine line = order.getLineOrThrow(LineId.of(lineId));

        // 更新數量
        if (request.quantity() > 0) {
            line.updateQuantity(request.quantity());
        }

        // 解析運送方式與備貨方式
        DeliveryMethod deliveryMethod = request.deliveryMethod() != null
                ? DeliveryMethod.fromCodeOrThrow(request.deliveryMethod())
                : line.getDeliveryMethod();

        StockMethod stockMethod = request.stockMethod() != null
                ? StockMethod.fromCodeOrThrow(request.stockMethod())
                : line.getStockMethod();

        // EC-008: 驗證運送方式與備貨方式相容性
        stockMethod = validateAndCorrectCompatibility(deliveryMethod, stockMethod);

        // 更新運送方式
        if (request.deliveryMethod() != null) {
            line.updateDeliveryMethod(deliveryMethod);
        }

        // 更新備貨方式
        line.updateStockMethod(stockMethod);

        log.info("訂單行項更新成功: orderId={}, lineId={}", orderId, lineId);

        return toOrderLineResponse(line);
    }

    /**
     * 刪除訂單行項
     */
    public void removeLine(String orderId, String lineId) {
        log.info("刪除訂單行項: orderId={}, lineId={}", orderId, lineId);

        Order order = findOrderOrThrow(orderId);
        order.removeLine(LineId.of(lineId));

        log.info("訂單行項刪除成功: orderId={}, lineId={}", orderId, lineId);
    }

    /**
     * 設定安裝服務
     *
     * @param orderId 訂單編號
     * @param lineId 行項編號
     * @param workTypeId 工種代碼
     * @param serviceTypes 安裝服務類型清單
     * @return 更新後的訂單行項
     */
    public OrderLineResponse attachInstallation(
            String orderId,
            String lineId,
            String workTypeId,
            List<String> serviceTypes) {
        log.info("設定安裝服務: orderId={}, lineId={}, workTypeId={}, serviceTypes={}",
                orderId, lineId, workTypeId, serviceTypes);

        Order order = findOrderOrThrow(orderId);
        OrderLine line = order.getLineOrThrow(LineId.of(lineId));

        // 驗證工種是否存在
        WorkType workType = workTypeService.getWorkType(workTypeId)
                .orElseThrow(() -> new BusinessException("WORK_TYPE_NOT_FOUND", "找不到工種: " + workTypeId));

        // 取得可用的安裝服務
        List<InstallationService> availableServices = productServiceAssociationService
                .getAvailableServices(line.getSkuNo());

        // 驗證服務類型是否可用
        List<String> validServiceTypes = new ArrayList<>();
        Money totalInstallationCost = Money.ZERO;

        for (String serviceType : serviceTypes) {
            InstallationService service = availableServices.stream()
                    .filter(s -> s.serviceType().equals(serviceType))
                    .findFirst()
                    .orElse(null);

            if (service != null) {
                validServiceTypes.add(serviceType);
                Money cost = workType.calculateInstallationCost(
                        service.basePrice(),
                        !service.isExtraInstallation()
                );
                totalInstallationCost = totalInstallationCost.add(cost);
            }
        }

        // 建立安裝明細
        InstallationDetail installationDetail = new InstallationDetail(
                workType.workTypeId(),
                workType.workTypeName(),
                validServiceTypes,
                totalInstallationCost,
                Money.ZERO, // laborCost
                availableServices.stream().anyMatch(InstallationService::isMandatory),
                workType.basicDiscount()
        );

        line.setInstallationDetail(installationDetail);

        log.info("安裝服務設定成功: orderId={}, lineId={}, installationCost={}",
                orderId, lineId, totalInstallationCost.amount());

        return toOrderLineResponse(line);
    }

    /**
     * 設定運送服務
     *
     * @param orderId 訂單編號
     * @param lineId 行項編號
     * @param stockMethodCode 備貨方式代碼（X/Y）
     * @param deliveryMethodCode 運送方式代碼
     * @param workTypeId 運送工種代碼（可選）
     * @param receiverName 收件人姓名（直送/宅配用）
     * @param receiverPhone 收件人電話（直送/宅配用）
     * @param address 配送地址（直送/宅配用）
     * @param zipCode 郵遞區號（直送/宅配用）
     * @return 更新後的訂單行項
     */
    public OrderLineResponse attachDelivery(
            String orderId,
            String lineId,
            String stockMethodCode,
            String deliveryMethodCode,
            String workTypeId,
            String receiverName,
            String receiverPhone,
            String address,
            String zipCode) {
        log.info("設定運送服務: orderId={}, lineId={}, stockMethod={}, deliveryMethod={}, workTypeId={}",
                orderId, lineId, stockMethodCode, deliveryMethodCode, workTypeId);

        Order order = findOrderOrThrow(orderId);
        OrderLine line = order.getLineOrThrow(LineId.of(lineId));

        DeliveryMethod deliveryMethod = DeliveryMethod.fromCodeOrThrow(deliveryMethodCode);

        // 更新備貨方式（若有提供）並驗證相容性
        if (stockMethodCode != null && !stockMethodCode.isBlank()) {
            StockMethod stockMethod = StockMethod.fromCodeOrThrow(stockMethodCode);
            // EC-008: 驗證運送方式與備貨方式相容性
            stockMethod = validateAndCorrectCompatibility(deliveryMethod, stockMethod);
            line.updateStockMethod(stockMethod);
        }

        // 建立運送明細
        DeliveryDetail deliveryDetail;
        Money deliveryCost = Money.ZERO;

        switch (deliveryMethod) {
            case IMMEDIATE_PICKUP -> deliveryDetail = DeliveryDetail.immediatePickup();
            case LATER_PICKUP -> deliveryDetail = DeliveryDetail.laterPickup(null);
            case DIRECT_SHIPMENT -> deliveryDetail = DeliveryDetail.directShipment(
                    receiverName, receiverPhone, address, zipCode
            );
            case HOME_DELIVERY -> {
                // 查詢宅配工種費用
                if (workTypeId != null) {
                    WorkType workType = workTypeService.getWorkType(workTypeId).orElse(null);
                    if (workType != null && workType.isHomeDelivery()) {
                        deliveryCost = workType.calculateDeliveryCost(new Money(100)); // 基礎運費 100
                    }
                }
                deliveryDetail = DeliveryDetail.homeDelivery(
                        receiverName, receiverPhone, address, zipCode, deliveryCost
                );
            }
            default -> {
                // 代運或純運
                String wtId = workTypeId != null ? workTypeId : "0000";
                WorkType workType = workTypeService.getWorkType(wtId)
                        .orElse(WorkType.pureDelivery());

                deliveryDetail = new DeliveryDetail(
                        deliveryMethod,
                        workType.workTypeId(),
                        workType.workTypeName(),
                        null,
                        Money.ZERO,
                        receiverName,
                        receiverPhone,
                        address,
                        zipCode,
                        ""
                );
            }
        }

        line.setDeliveryDetail(deliveryDetail);

        log.info("運送服務設定成功: orderId={}, lineId={}, deliveryMethod={}, deliveryCost={}",
                orderId, lineId, deliveryMethod.getName(), deliveryCost.amount());

        return toOrderLineResponse(line);
    }

    /**
     * 取得商品可用的安裝服務
     */
    public List<InstallationService> getAvailableInstallationServices(String skuNo) {
        return productServiceAssociationService.getAvailableServices(skuNo);
    }

    /**
     * 取得可用的工種清單
     */
    public List<WorkType> getAvailableWorkTypes() {
        return workTypeService.getAllWorkTypes();
    }

    /**
     * 執行價格試算
     */
    public CalculationResponse calculate(String orderId) {
        log.info("執行價格試算: orderId={}", orderId);

        Order order = findOrderOrThrow(orderId);

        if (!order.hasLines()) {
            throw new BusinessException("ORDER_EMPTY", "訂單沒有任何商品");
        }

        // 呼叫試算服務
        PriceCalculation calculation = priceCalculationService.calculate(order);

        // 更新訂單試算結果
        order.setCalculation(calculation);

        log.info("價格試算完成: orderId={}, grandTotal={}",
                orderId, calculation.getGrandTotal().amount());

        return toCalculationResponse(orderId, calculation);
    }

    /**
     * 提交訂單
     */
    public OrderResponse submit(String orderId) {
        log.info("提交訂單: orderId={}", orderId);

        Order order = findOrderOrThrow(orderId);
        order.submit();

        log.info("訂單提交成功: orderId={}, status={}", orderId, order.getStatus().getName());

        return toOrderResponse(order);
    }

    /**
     * 驗證優惠券
     */
    public CouponValidation validateCoupon(String orderId, String couponId) {
        log.info("驗證優惠券: orderId={}, couponId={}", orderId, couponId);

        Order order = findOrderOrThrow(orderId);
        return couponService.validateCoupon(couponId, order);
    }

    /**
     * 套用優惠券
     */
    public CouponValidation applyCoupon(String orderId, String couponId) {
        log.info("套用優惠券: orderId={}, couponId={}", orderId, couponId);

        Order order = findOrderOrThrow(orderId);

        // 驗證優惠券
        CouponValidation validation = couponService.validateCoupon(couponId, order);
        if (!validation.valid()) {
            log.warn("優惠券驗證失敗: orderId={}, couponId={}, reason={}",
                    orderId, couponId, validation.failureReason());
            return validation;
        }

        // 套用優惠券
        couponService.applyCoupon(couponId, order);

        log.info("優惠券套用成功: orderId={}, couponId={}, discountAmount={}",
                orderId, couponId, validation.discountAmount().amount());

        return validation;
    }

    /**
     * 移除優惠券
     */
    public void removeCoupon(String orderId) {
        log.info("移除優惠券: orderId={}", orderId);

        Order order = findOrderOrThrow(orderId);
        couponService.removeCoupon(order);

        log.info("優惠券移除成功: orderId={}", orderId);
    }

    /**
     * 查詢會員可用紅利點數
     */
    public int getAvailableBonusPoints(String orderId) {
        Order order = findOrderOrThrow(orderId);
        String memberId = order.getCustomer().memberId();
        return bonusService.getAvailablePoints(memberId);
    }

    /**
     * 驗證紅利折抵
     */
    public Optional<BonusRedemption> validateBonusRedemption(
            String orderId, String skuNo, int points) {
        log.info("驗證紅利折抵: orderId={}, skuNo={}, points={}", orderId, skuNo, points);

        Order order = findOrderOrThrow(orderId);
        String memberId = order.getCustomer().memberId();

        return bonusService.validateRedemption(memberId, skuNo, points, order);
    }

    /**
     * 執行紅利折抵
     */
    public BonusRedemption redeemBonusPoints(String orderId, String skuNo, int points) {
        log.info("執行紅利折抵: orderId={}, skuNo={}, points={}", orderId, skuNo, points);

        Order order = findOrderOrThrow(orderId);
        String memberId = order.getCustomer().memberId();

        BonusRedemption redemption = bonusService.redeemPoints(memberId, skuNo, points, order);

        if (redemption == null) {
            throw new BusinessException("BONUS_REDEMPTION_FAILED", "紅利折抵失敗");
        }

        log.info("紅利折抵成功: orderId={}, skuNo={}, points={}, discountAmount={}",
                orderId, skuNo, points, redemption.discountAmount().amount());

        return redemption;
    }

    /**
     * 取消紅利折抵
     */
    public void cancelBonusRedemption(String orderId, String skuNo) {
        log.info("取消紅利折抵: orderId={}, skuNo={}", orderId, skuNo);

        Order order = findOrderOrThrow(orderId);

        // 找到對應的行項
        OrderLine line = order.getLines().stream()
                .filter(l -> l.getSkuNo().equals(skuNo))
                .findFirst()
                .orElseThrow(() -> new BusinessException("LINE_NOT_FOUND", "找不到商品: " + skuNo));

        // 檢查是否有紅利折扣
        if (line.getBonusDisc().isZero()) {
            throw new BusinessException("NO_BONUS_REDEMPTION", "此商品沒有紅利折抵");
        }

        // 建立折抵記錄用於退還
        String memberId = order.getCustomer().memberId();
        BonusRedemption redemption = BonusRedemption.create(
                memberId,
                skuNo,
                line.getSkuName(),
                line.getBonusDisc().amount(), // 使用折抵金額當作點數（1:1兌換）
                bonusService.getExchangeRate(),
                bonusService.getAvailablePoints(memberId)
        );

        bonusService.cancelRedemption(redemption, order);

        log.info("紅利折抵取消成功: orderId={}, skuNo={}", orderId, skuNo);
    }

    // === Private Methods ===

    /**
     * EC-008: 驗證運送方式與備貨方式相容性
     * - 直送(V) 僅限訂購(Y)
     * - 當場自取(C) 僅限現貨(X)
     *
     * @param deliveryMethod 運送方式
     * @param stockMethod 備貨方式
     * @return 修正後的備貨方式
     */
    private StockMethod validateAndCorrectCompatibility(DeliveryMethod deliveryMethod, StockMethod stockMethod) {
        if (deliveryMethod == DeliveryMethod.DIRECT_SHIPMENT && stockMethod == StockMethod.IN_STOCK) {
            log.warn("EC-008: 直送不相容現貨，自動更正為訂購");
            return StockMethod.PURCHASE_ORDER;
        }
        if (deliveryMethod == DeliveryMethod.IMMEDIATE_PICKUP && stockMethod == StockMethod.PURCHASE_ORDER) {
            log.warn("EC-008: 當場自取不相容訂購，自動更正為現貨");
            return StockMethod.IN_STOCK;
        }
        return stockMethod;
    }

    private Order findOrderOrThrow(String orderId) {
        return Optional.ofNullable(orderStore.get(orderId))
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "找不到訂單: " + orderId));
    }

    private OrderId generateOrderId() {
        long seq = orderSequence.getAndIncrement();
        return OrderId.of(String.valueOf(seq));
    }

    private ProjectId generateProjectId(String storeId) {
        LocalDate today = LocalDate.now();
        String year = String.format("%02d", today.getYear() % 100);
        String monthDay = today.format(DateTimeFormatter.ofPattern("MMdd"));
        long seq = projectSequence.getAndIncrement();
        String seqStr = String.format("%05d", seq);

        // 從 storeId 提取數字部分，若無數字則使用 "00000"
        String numericPart = storeId.replaceAll("[^0-9]", "");
        String paddedStoreId = String.format("%05d", numericPart.isEmpty() ? 0 : Long.parseLong(numericPart));

        return ProjectId.of(paddedStoreId + year + monthDay + seqStr);
    }

    private Customer toCustomer(CreateOrderRequest.CustomerInfo info) {
        MemberDiscountType discountType = null;
        if (info.discountType() != null && !info.discountType().isBlank()) {
            discountType = MemberDiscountType.fromCode(info.discountType()).orElse(null);
        }

        return new Customer(
                info.memberId(),
                info.cardType(),
                info.name(),
                info.gender(),
                info.phone(),
                info.cellPhone(),
                null, // birthday - 從字串轉換需要額外處理
                info.contactName(),
                info.contactPhone(),
                info.vipType(),
                discountType,
                info.isTempCard()
        );
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderLineResponse> lineResponses = order.getLines().stream()
                .map(this::toOrderLineResponse)
                .toList();

        PriceCalculation calc = order.getCalculation();
        OrderResponse.CalculationInfo calcInfo = new OrderResponse.CalculationInfo(
                calc.getProductTotal().amount(),
                calc.getInstallationTotal().amount(),
                calc.getDeliveryTotal().amount(),
                calc.getMemberDiscount().amount(),
                calc.getDirectShipmentTotal().amount(),
                calc.getCouponDiscount().amount(),
                calc.getTaxAmount().amount(),
                calc.getGrandTotal().amount(),
                calc.isPromotionSkipped(),
                calc.getWarnings(),
                calc.getCalculatedAt()
        );

        Customer customer = order.getCustomer();
        OrderResponse.CustomerInfo customerInfo = new OrderResponse.CustomerInfo(
                customer.memberId(),
                customer.cardType(),
                customer.name(),
                customer.gender(),
                customer.phone(),
                customer.cellPhone(),
                customer.birthday() != null ? customer.birthday().toString() : null,
                customer.contactName(),
                customer.contactPhone(),
                customer.vipType(),
                customer.discountType() != null ? customer.discountType().getCode() : null,
                customer.isTempCard()
        );

        DeliveryAddress address = order.getDeliveryAddress();
        OrderResponse.AddressInfo addressInfo = new OrderResponse.AddressInfo(
                address.zipCode(),
                address.fullAddress()
        );

        return new OrderResponse(
                order.getId().value(),
                order.getProjectId().value(),
                order.getStatus().getCode(),
                order.getStatus().getName(),
                customerInfo,
                addressInfo,
                order.getStoreId(),
                order.getChannelId(),
                lineResponses,
                calcInfo,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderLineResponse toOrderLineResponse(OrderLine line) {
        InstallationDetail installationDetail = line.getInstallationDetail();
        DeliveryDetail deliveryDetail = line.getDeliveryDetail();

        return new OrderLineResponse(
                line.getId().value(),
                line.getSerialNo(),
                line.getSkuNo(),
                line.getSkuName(),
                line.getQuantity(),
                line.getUnitPrice().amount(),
                line.getActualUnitPrice().amount(),
                line.getDeliveryMethod().getCode(),
                line.getDeliveryMethod().getName(),
                line.getStockMethod().getCode(),
                line.getStockMethod().getName(),
                line.getTaxType().getCode(),
                line.getTaxType().getName(),
                line.getSubtotal().amount(),
                line.getMemberDisc().amount(),
                line.getBonusDisc().amount(),
                line.getCouponDisc().amount(),
                // Installation & Delivery fields
                line.getWorkTypeId(),
                installationDetail != null ? installationDetail.workTypeName() : null,
                line.getInstallationServiceTypes(),
                line.hasInstallation(),
                line.getInstallationCost().amount(),
                line.getDeliveryCost().amount(),
                line.getDeliveryDate(),
                deliveryDetail != null ? deliveryDetail.receiverName() : null,
                deliveryDetail != null ? deliveryDetail.receiverPhone() : null,
                deliveryDetail != null ? deliveryDetail.deliveryAddress() : null
        );
    }

    private CalculationResponse toCalculationResponse(String orderId, PriceCalculation calc) {
        List<ComputeTypeVO> computeTypes = new ArrayList<>();
        computeTypes.add(ComputeTypeVO.productTotal(calc.getProductTotal().amount(), 0));
        computeTypes.add(ComputeTypeVO.installationTotal(calc.getInstallationTotal().amount(), 0));
        computeTypes.add(ComputeTypeVO.deliveryTotal(calc.getDeliveryTotal().amount(), 0));
        computeTypes.add(ComputeTypeVO.memberDiscount(calc.getMemberDiscount().amount()));
        computeTypes.add(ComputeTypeVO.directShipmentTotal(calc.getDirectShipmentTotal().amount()));
        computeTypes.add(ComputeTypeVO.couponDiscount(calc.getCouponDiscount().amount()));
        computeTypes.add(ComputeTypeVO.bonusDiscount(calc.getBonusDiscount().amount()));

        return new CalculationResponse(
                orderId,
                computeTypes,
                calc.getMemberDiscounts(),
                calc.getGrandTotal().amount(),
                calc.getTaxAmount().amount(),
                calc.isPromotionSkipped(),
                calc.getWarnings(),
                calc.getCalculatedAt()
        );
    }
}
