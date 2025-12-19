package com.tgfc.som.order.dto;

import com.tgfc.som.pricing.dto.ComputeTypeVO;
import com.tgfc.som.pricing.dto.MemberDiscVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 價格試算回應
 *
 * @param orderId 訂單編號
 * @param computeTypes 試算類型列表（6 種 ComputeType）
 * @param memberDiscounts 會員折扣明細
 * @param grandTotal 應付總額
 * @param taxAmount 稅額
 * @param promotionSkipped 是否跳過促銷計算
 * @param warnings 警告訊息列表
 * @param calculatedAt 試算時間
 */
public record CalculationResponse(
    String orderId,
    List<ComputeTypeVO> computeTypes,
    List<MemberDiscVO> memberDiscounts,
    int grandTotal,
    int taxAmount,
    boolean promotionSkipped,
    List<String> warnings,
    LocalDateTime calculatedAt
) {}
