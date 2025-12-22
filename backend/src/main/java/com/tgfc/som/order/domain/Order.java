package com.tgfc.som.order.domain;

import com.tgfc.som.common.exception.BusinessException;
import com.tgfc.som.order.domain.valueobject.Customer;
import com.tgfc.som.order.domain.valueobject.DeliveryAddress;
import com.tgfc.som.order.domain.valueobject.LineId;
import com.tgfc.som.order.domain.valueobject.Money;
import com.tgfc.som.order.domain.valueobject.OrderId;
import com.tgfc.som.order.domain.valueobject.PriceCalculation;
import com.tgfc.som.order.domain.valueobject.ProjectId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 訂單聚合根
 *
 * 職責：
 * - 管理訂單生命週期（Draft → Quotation → Active → Paid → Closed）
 * - 協調訂單行項的新增、修改、刪除
 * - 執行訂單層級的業務規則驗證
 *
 * 不變量：
 * - 訂單至少包含一項商品（提交時）
 * - 商品數量不超過 999 項
 * - 提交前必須完成價格試算
 */
public class Order {

    // 最大行項數量
    public static final int MAX_LINES = 999;

    // === Identity ===
    private final OrderId id;
    private final ProjectId projectId;

    // === Customer Information ===
    private Customer customer;
    private DeliveryAddress deliveryAddress;

    // === Order Context ===
    private final String storeId;
    private final String channelId;
    private OrderStatus status;
    private String orderSource;

    // === Personnel ===
    private String handlerId;
    private String specialistId;

    // === Order Lines ===
    private final List<OrderLine> lines;

    // === Calculation Result ===
    private PriceCalculation calculation;

    // === Idempotency ===
    private String idempotencyKey;

    // === Audit ===
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    /**
     * 建立新訂單
     */
    public Order(
            OrderId id,
            ProjectId projectId,
            Customer customer,
            DeliveryAddress deliveryAddress,
            String storeId,
            String channelId,
            String createdBy
    ) {
        this.id = Objects.requireNonNull(id, "訂單編號不可為空");
        this.projectId = Objects.requireNonNull(projectId, "專案代號不可為空");
        this.customer = Objects.requireNonNull(customer, "客戶資訊不可為空");
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress, "安運地址不可為空");
        this.storeId = Objects.requireNonNull(storeId, "出貨店不可為空");
        this.channelId = Objects.requireNonNull(channelId, "通路代號不可為空");

        this.status = OrderStatus.DRAFT;
        this.lines = new ArrayList<>();
        this.calculation = PriceCalculation.empty();

        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    // === Business Methods ===

    /**
     * 新增訂單行項
     *
     * @return 新增的行項
     */
    public OrderLine addLine(
            String skuNo,
            String skuName,
            int quantity,
            Money unitPrice,
            TaxType taxType,
            DeliveryMethod deliveryMethod,
            StockMethod stockMethod
    ) {
        validateCanModify();

        if (lines.size() >= MAX_LINES) {
            throw new BusinessException("ORDER_LINE_LIMIT", "訂單已達商品上限（" + MAX_LINES + "項）");
        }

        LineId lineId = LineId.generate();
        int serialNo = lines.size() + 1;

        OrderLine line = new OrderLine(
                lineId,
                serialNo,
                skuNo,
                skuName,
                quantity,
                unitPrice,
                taxType,
                deliveryMethod,
                stockMethod
        );

        lines.add(line);
        markUpdated();

        return line;
    }

    /**
     * 移除訂單行項
     */
    public void removeLine(LineId lineId) {
        validateCanModify();

        boolean removed = lines.removeIf(line -> line.getId().equals(lineId));
        if (!removed) {
            throw new BusinessException("LINE_NOT_FOUND", "找不到訂單行項: " + lineId);
        }

        // 重新編號
        for (int i = 0; i < lines.size(); i++) {
            lines.get(i).setSerialNo(i + 1);
        }

        markUpdated();
    }

    /**
     * 取得訂單行項
     */
    public Optional<OrderLine> getLine(LineId lineId) {
        return lines.stream()
                .filter(line -> line.getId().equals(lineId))
                .findFirst();
    }

    /**
     * 取得訂單行項（若不存在則拋出例外）
     */
    public OrderLine getLineOrThrow(LineId lineId) {
        return getLine(lineId)
                .orElseThrow(() -> new BusinessException("LINE_NOT_FOUND", "找不到訂單行項: " + lineId));
    }

    /**
     * 更新訂單行項數量
     */
    public void updateLineQuantity(LineId lineId, int quantity) {
        validateCanModify();

        OrderLine line = getLineOrThrow(lineId);
        line.updateQuantity(quantity);

        markUpdated();
    }

    /**
     * 更新訂單行項運送方式
     */
    public void updateLineDeliveryMethod(LineId lineId, DeliveryMethod deliveryMethod) {
        validateCanModify();

        OrderLine line = getLineOrThrow(lineId);
        line.updateDeliveryMethod(deliveryMethod);

        markUpdated();
    }

    /**
     * 設定價格試算結果
     */
    public void setCalculation(PriceCalculation calculation) {
        this.calculation = Objects.requireNonNull(calculation, "價格試算結果不可為空");
        markUpdated();
    }

    /**
     * 提交訂單
     */
    public void submit() {
        validateCanSubmit();

        // 驗證至少有一項商品
        if (lines.isEmpty()) {
            throw new BusinessException("ORDER_EMPTY", "訂單必須包含至少一項商品");
        }

        // 驗證已完成價格試算
        if (calculation == null || calculation.getGrandTotal().isZero() && !lines.isEmpty()) {
            throw new BusinessException("CALCULATION_REQUIRED", "請先執行價格試算");
        }

        // 狀態轉換
        this.status = OrderStatus.QUOTATION;
        markUpdated();
    }

    /**
     * 取消訂單
     */
    public void cancel(String reason) {
        if (!status.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                    "無法從狀態 " + status.getName() + " 取消訂單");
        }

        this.status = OrderStatus.CANCELLED;
        markUpdated();
    }

    /**
     * 設為有效訂單
     */
    public void activate() {
        if (!status.canTransitionTo(OrderStatus.ACTIVE)) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                    "無法從狀態 " + status.getName() + " 轉換為有效");
        }

        this.status = OrderStatus.ACTIVE;
        markUpdated();
    }

    // === Validation Methods ===

    private void validateCanModify() {
        if (!status.canModify()) {
            throw new BusinessException("ORDER_NOT_MODIFIABLE",
                    "訂單狀態為 " + status.getName() + "，無法修改");
        }
    }

    private void validateCanSubmit() {
        if (!status.canTransitionTo(OrderStatus.QUOTATION)) {
            throw new BusinessException("INVALID_STATUS_TRANSITION",
                    "無法從狀態 " + status.getName() + " 提交訂單");
        }
    }

    // === Calculation Methods ===

    /**
     * 計算商品小計（不含折扣）
     */
    public Money getProductTotal() {
        return lines.stream()
                .map(OrderLine::getOriginalSubtotal)
                .reduce(Money.ZERO, Money::add);
    }

    /**
     * 計算實際商品小計（含折扣）
     */
    public Money getActualProductTotal() {
        return lines.stream()
                .map(OrderLine::getSubtotal)
                .reduce(Money.ZERO, Money::add);
    }

    /**
     * 計算總稅額
     */
    public Money getTotalTax() {
        return lines.stream()
                .map(OrderLine::getTaxAmount)
                .reduce(Money.ZERO, Money::add);
    }

    /**
     * 取得行項數量
     */
    public int getLineCount() {
        return lines.size();
    }

    /**
     * 是否有行項
     */
    public boolean hasLines() {
        return !lines.isEmpty();
    }

    /**
     * 是否可以新增更多行項
     */
    public boolean canAddMoreLines() {
        return lines.size() < MAX_LINES;
    }

    // === Private Methods ===

    private void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    // === Getters ===

    public OrderId getId() {
        return id;
    }

    public ProjectId getProjectId() {
        return projectId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        validateCanModify();
        this.customer = Objects.requireNonNull(customer, "客戶資訊不可為空");
        markUpdated();
    }

    public DeliveryAddress getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(DeliveryAddress deliveryAddress) {
        validateCanModify();
        this.deliveryAddress = Objects.requireNonNull(deliveryAddress, "安運地址不可為空");
        markUpdated();
    }

    public String getStoreId() {
        return storeId;
    }

    public String getChannelId() {
        return channelId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getOrderSource() {
        return orderSource;
    }

    public void setOrderSource(String orderSource) {
        this.orderSource = orderSource;
    }

    public String getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(String handlerId) {
        this.handlerId = handlerId;
    }

    public String getSpecialistId() {
        return specialistId;
    }

    public void setSpecialistId(String specialistId) {
        this.specialistId = specialistId;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public PriceCalculation getCalculation() {
        return calculation;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{" +
               "id=" + id +
               ", projectId=" + projectId +
               ", status=" + status +
               ", storeId='" + storeId + '\'' +
               ", channelId='" + channelId + '\'' +
               ", lineCount=" + lines.size() +
               ", grandTotal=" + (calculation != null ? calculation.getGrandTotal() : "N/A") +
               '}';
    }
}
