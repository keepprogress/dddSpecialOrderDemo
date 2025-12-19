package com.tgfc.som.order.domain.valueobject;

import com.tgfc.som.order.domain.DeliveryMethod;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 運送明細值物件
 *
 * 記錄訂單行項的運送配置
 */
public record DeliveryDetail(
    DeliveryMethod method,       // 運送方式
    String workTypeId,           // 運送工種代碼
    String workTypeName,         // 運送工種名稱
    LocalDate scheduledDate,     // 預定出貨日
    Money deliveryCost,          // 運送費用
    String receiverName,         // 收件人姓名
    String receiverPhone,        // 收件人電話
    String deliveryAddress,      // 配送地址
    String deliveryZipCode,      // 配送郵遞區號
    String deliveryNote          // 配送備註
) {
    public DeliveryDetail {
        Objects.requireNonNull(method, "運送方式不可為空");
        deliveryCost = deliveryCost != null ? deliveryCost : Money.ZERO;
        deliveryNote = deliveryNote != null ? deliveryNote : "";
    }

    /**
     * 建立預設運送明細（代運）
     */
    public static DeliveryDetail defaultDelivery() {
        return new DeliveryDetail(
            DeliveryMethod.MANAGED,
            null, null,
            null,
            Money.ZERO,
            null, null, null, null, ""
        );
    }

    /**
     * 建立當場自取明細
     */
    public static DeliveryDetail immediatePickup() {
        return new DeliveryDetail(
            DeliveryMethod.IMMEDIATE_PICKUP,
            null, null,
            LocalDate.now(),
            Money.ZERO,
            null, null, null, null, "當場自取"
        );
    }

    /**
     * 建立下次自取明細
     */
    public static DeliveryDetail laterPickup(LocalDate pickupDate) {
        return new DeliveryDetail(
            DeliveryMethod.LATER_PICKUP,
            null, null,
            pickupDate,
            Money.ZERO,
            null, null, null, null, "下次自取"
        );
    }

    /**
     * 建立直送明細
     */
    public static DeliveryDetail directShipment(
            String receiverName,
            String receiverPhone,
            String address,
            String zipCode) {
        return new DeliveryDetail(
            DeliveryMethod.DIRECT_SHIPMENT,
            null, null,
            null,
            Money.ZERO,
            receiverName, receiverPhone, address, zipCode, "直送"
        );
    }

    /**
     * 建立宅配明細
     */
    public static DeliveryDetail homeDelivery(
            String receiverName,
            String receiverPhone,
            String address,
            String zipCode,
            Money deliveryCost) {
        return new DeliveryDetail(
            DeliveryMethod.HOME_DELIVERY,
            null, null,
            null,
            deliveryCost,
            receiverName, receiverPhone, address, zipCode, "宅配"
        );
    }

    /**
     * 是否需要配送地址
     */
    public boolean requiresAddress() {
        return method == DeliveryMethod.DIRECT_SHIPMENT ||
               method == DeliveryMethod.HOME_DELIVERY ||
               method == DeliveryMethod.MANAGED;
    }

    /**
     * 是否為自取
     */
    public boolean isPickup() {
        return method == DeliveryMethod.IMMEDIATE_PICKUP ||
               method == DeliveryMethod.LATER_PICKUP;
    }

    /**
     * 是否需要指派工種
     */
    public boolean requiresWorkType() {
        return method == DeliveryMethod.MANAGED ||
               method == DeliveryMethod.PURE_DELIVERY;
    }

    /**
     * 是否為直送
     */
    public boolean isDirectShipment() {
        return method == DeliveryMethod.DIRECT_SHIPMENT;
    }

    /**
     * 是否免運
     */
    public boolean isFreeDelivery() {
        return deliveryCost.isZero();
    }

    /**
     * 複製並設定新的預定日期
     */
    public DeliveryDetail withScheduledDate(LocalDate newDate) {
        return new DeliveryDetail(
            method, workTypeId, workTypeName, newDate,
            deliveryCost, receiverName, receiverPhone,
            deliveryAddress, deliveryZipCode, deliveryNote
        );
    }

    /**
     * 複製並設定工種
     */
    public DeliveryDetail withWorkType(String newWorkTypeId, String newWorkTypeName) {
        return new DeliveryDetail(
            method, newWorkTypeId, newWorkTypeName, scheduledDate,
            deliveryCost, receiverName, receiverPhone,
            deliveryAddress, deliveryZipCode, deliveryNote
        );
    }

    /**
     * 複製並設定運送費用
     */
    public DeliveryDetail withDeliveryCost(Money newCost) {
        return new DeliveryDetail(
            method, workTypeId, workTypeName, scheduledDate,
            newCost, receiverName, receiverPhone,
            deliveryAddress, deliveryZipCode, deliveryNote
        );
    }
}
