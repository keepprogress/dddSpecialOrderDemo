package com.tgfc.som.member.dto;

import java.math.BigDecimal;

/**
 * 會員查詢回應
 *
 * @param memberId 會員卡號
 * @param cardType 卡別（0: 一般卡, 1: 商務卡, T: 臨時卡）
 * @param name 姓名
 * @param birthday 生日（格式：yyyy-MM-dd）
 * @param gender 性別（M/F）
 * @param cellPhone 手機號碼
 * @param address 地址
 * @param zipCode 郵遞區號
 * @param discType 折扣類型代碼（0/1/2/SPECIAL）
 * @param discTypeName 折扣類型名稱
 * @param discRate 折扣率（Type 0 用，如 0.95 表示 95 折）
 * @param markupRate 加成比例（Type 2 用，如 1.05 表示成本加成 5%）
 * @param isTempCard 是否為臨時卡
 */
public record MemberResponse(
    String memberId,
    String cardType,
    String name,
    String birthday,
    String gender,
    String cellPhone,
    String address,
    String zipCode,
    String discType,
    String discTypeName,
    BigDecimal discRate,
    BigDecimal markupRate,
    boolean isTempCard
) {

    /**
     * 建立一般會員回應
     */
    public static MemberResponse ofMember(
            String memberId,
            String cardType,
            String name,
            String cellPhone,
            String discType,
            String discTypeName,
            BigDecimal discRate,
            BigDecimal markupRate
    ) {
        return new MemberResponse(
            memberId, cardType, name, null, null, cellPhone,
            null, null, discType, discTypeName, discRate, markupRate, false
        );
    }

    /**
     * 建立臨時卡會員回應
     */
    public static MemberResponse ofTempCard(
            String tempMemberId,
            String name,
            String cellPhone,
            String address,
            String zipCode
    ) {
        return new MemberResponse(
            tempMemberId, "T", name, null, null, cellPhone,
            address, zipCode, null, null, null, null, true
        );
    }

    /**
     * 是否有會員折扣
     */
    public boolean hasDiscount() {
        return discType != null && !isTempCard;
    }
}
