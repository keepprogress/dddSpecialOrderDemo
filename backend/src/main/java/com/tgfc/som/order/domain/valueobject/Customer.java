package com.tgfc.som.order.domain.valueobject;

import com.tgfc.som.member.domain.MemberDiscountType;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 客戶資訊值物件
 */
public record Customer(
        String memberId,
        String cardType,
        String name,
        String gender,
        String phone,
        String cellPhone,
        LocalDate birthday,
        String contactName,
        String contactPhone,
        String vipType,
        MemberDiscountType discountType,
        boolean isTempCard
) {

    public Customer {
        // 會員卡號、身分證、電話至少有一項
        if (!hasIdentification(memberId, phone, cellPhone)) {
            throw new IllegalArgumentException("會員卡號、身分證、電話至少須填寫一項");
        }
    }

    private static boolean hasIdentification(String memberId, String phone, String cellPhone) {
        return (memberId != null && !memberId.isBlank()) ||
               (phone != null && !phone.isBlank()) ||
               (cellPhone != null && !cellPhone.isBlank());
    }

    /**
     * 建立一般會員
     */
    public static Customer ofMember(
            String memberId,
            String cardType,
            String name,
            String cellPhone,
            MemberDiscountType discountType
    ) {
        return new Customer(
                memberId, cardType, name, null, null, cellPhone,
                null, name, cellPhone, null, discountType, false
        );
    }

    /**
     * 建立臨時卡客戶
     */
    public static Customer ofTempCard(
            String name,
            String cellPhone,
            String address
    ) {
        Objects.requireNonNull(name, "臨時卡姓名不可為空");
        Objects.requireNonNull(cellPhone, "臨時卡電話不可為空");

        return new Customer(
                null, "T", name, null, null, cellPhone,
                null, name, cellPhone, null, null, true
        );
    }

    /**
     * 是否有會員折扣
     */
    public boolean hasDiscount() {
        return discountType != null && !isTempCard;
    }

    /**
     * 取得顯示名稱
     */
    public String getDisplayName() {
        return name != null ? name : "未知客戶";
    }

    /**
     * 取得有效聯絡電話
     */
    public String getEffectivePhone() {
        return contactPhone != null ? contactPhone :
               (cellPhone != null ? cellPhone : phone);
    }
}
