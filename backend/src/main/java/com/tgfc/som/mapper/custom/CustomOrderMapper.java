package com.tgfc.som.mapper.custom;

import com.tgfc.som.entity.OrderDetl;
import com.tgfc.som.entity.OrderMast;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 訂單自訂 Mapper
 *
 * 用於複雜查詢和效能敏感的操作
 * 遵循 Constitution VIII: MyBatis Generator Pattern
 */
@Mapper
public interface CustomOrderMapper {

    /**
     * 查詢訂單主表及明細（N+1 優化）
     */
    @Select("""
        SELECT m.ORDER_ID, m.PROJECT_ID, m.MEMBER_CARD_ID, m.MEMBER_NAME,
               m.INSTALL_ZIP, m.INSTALL_ADDR, m.STORE_ID, m.CHANNEL_ID,
               m.ORDER_STATUS, m.TOTAL_AMT, m.HANDLER_ID, m.SPECIALIST_ID,
               m.CREATED_AT, m.UPDATED_AT, m.CREATED_BY, m.UPDATED_BY
        FROM TBL_ORDER_MAST m
        WHERE m.ORDER_ID = #{orderId}
        """)
    @Results(id = "orderMastResultMap", value = {
        @Result(property = "orderId", column = "ORDER_ID", id = true),
        @Result(property = "projectId", column = "PROJECT_ID"),
        @Result(property = "memberCardId", column = "MEMBER_CARD_ID"),
        @Result(property = "memberName", column = "MEMBER_NAME"),
        @Result(property = "installZip", column = "INSTALL_ZIP"),
        @Result(property = "installAddr", column = "INSTALL_ADDR"),
        @Result(property = "storeId", column = "STORE_ID"),
        @Result(property = "channelId", column = "CHANNEL_ID"),
        @Result(property = "orderStatus", column = "ORDER_STATUS"),
        @Result(property = "totalAmt", column = "TOTAL_AMT"),
        @Result(property = "handlerId", column = "HANDLER_ID"),
        @Result(property = "specialistId", column = "SPECIALIST_ID"),
        @Result(property = "createdAt", column = "CREATED_AT"),
        @Result(property = "updatedAt", column = "UPDATED_AT"),
        @Result(property = "createdBy", column = "CREATED_BY"),
        @Result(property = "updatedBy", column = "UPDATED_BY")
    })
    OrderMast selectOrderWithDetails(String orderId);

    /**
     * 批次查詢訂單明細（用於避免 N+1 問題）
     */
    @Select("""
        SELECT d.LINE_ID, d.ORDER_ID, d.SERIAL_NO, d.SKU_NO, d.SKU_NAME,
               d.QUANTITY, d.POS_AMT, d.ACT_POS_AMT, d.TAX_TYPE,
               d.DELIVERY_FLAG, d.STOCK_METHOD, d.DELIVERY_DATE,
               d.MEMBER_DISC, d.BONUS_DISC, d.CREATED_AT, d.UPDATED_AT
        FROM TBL_ORDER_DETL d
        WHERE d.ORDER_ID IN (#{orderIds})
        ORDER BY d.ORDER_ID, d.SERIAL_NO
        """)
    @Results({
        @Result(property = "lineId", column = "LINE_ID"),
        @Result(property = "orderId", column = "ORDER_ID"),
        @Result(property = "serialNo", column = "SERIAL_NO"),
        @Result(property = "skuNo", column = "SKU_NO"),
        @Result(property = "skuName", column = "SKU_NAME"),
        @Result(property = "quantity", column = "QUANTITY"),
        @Result(property = "posAmt", column = "POS_AMT"),
        @Result(property = "actPosAmt", column = "ACT_POS_AMT"),
        @Result(property = "taxType", column = "TAX_TYPE"),
        @Result(property = "deliveryFlag", column = "DELIVERY_FLAG"),
        @Result(property = "stockMethod", column = "STOCK_METHOD"),
        @Result(property = "deliveryDate", column = "DELIVERY_DATE"),
        @Result(property = "memberDisc", column = "MEMBER_DISC"),
        @Result(property = "bonusDisc", column = "BONUS_DISC"),
        @Result(property = "createdAt", column = "CREATED_AT"),
        @Result(property = "updatedAt", column = "UPDATED_AT")
    })
    List<OrderDetl> selectDetailsByOrderIds(@Param("orderIds") List<String> orderIds);

    /**
     * 依店別查詢訂單（分頁用）
     */
    @Select("""
        SELECT m.ORDER_ID, m.PROJECT_ID, m.MEMBER_CARD_ID, m.MEMBER_NAME,
               m.STORE_ID, m.CHANNEL_ID, m.ORDER_STATUS, m.TOTAL_AMT,
               m.CREATED_AT, m.CREATED_BY
        FROM TBL_ORDER_MAST m
        WHERE m.STORE_ID = #{storeId}
        ORDER BY m.CREATED_AT DESC
        OFFSET #{offset} ROWS FETCH NEXT #{limit} ROWS ONLY
        """)
    @Results({
        @Result(property = "orderId", column = "ORDER_ID"),
        @Result(property = "projectId", column = "PROJECT_ID"),
        @Result(property = "memberCardId", column = "MEMBER_CARD_ID"),
        @Result(property = "memberName", column = "MEMBER_NAME"),
        @Result(property = "storeId", column = "STORE_ID"),
        @Result(property = "channelId", column = "CHANNEL_ID"),
        @Result(property = "orderStatus", column = "ORDER_STATUS"),
        @Result(property = "totalAmt", column = "TOTAL_AMT"),
        @Result(property = "createdAt", column = "CREATED_AT"),
        @Result(property = "createdBy", column = "CREATED_BY")
    })
    List<OrderMast> selectByStoreIdPaged(
            @Param("storeId") String storeId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 統計店別訂單數量
     */
    @Select("SELECT COUNT(*) FROM TBL_ORDER_MAST WHERE STORE_ID = #{storeId}")
    int countByStoreId(@Param("storeId") String storeId);

    /**
     * 依會員查詢訂單
     */
    @Select("""
        SELECT m.ORDER_ID, m.PROJECT_ID, m.MEMBER_CARD_ID, m.MEMBER_NAME,
               m.STORE_ID, m.CHANNEL_ID, m.ORDER_STATUS, m.TOTAL_AMT,
               m.CREATED_AT, m.CREATED_BY
        FROM TBL_ORDER_MAST m
        WHERE m.MEMBER_CARD_ID = #{memberCardId}
        ORDER BY m.CREATED_AT DESC
        """)
    @Results({
        @Result(property = "orderId", column = "ORDER_ID"),
        @Result(property = "projectId", column = "PROJECT_ID"),
        @Result(property = "memberCardId", column = "MEMBER_CARD_ID"),
        @Result(property = "memberName", column = "MEMBER_NAME"),
        @Result(property = "storeId", column = "STORE_ID"),
        @Result(property = "channelId", column = "CHANNEL_ID"),
        @Result(property = "orderStatus", column = "ORDER_STATUS"),
        @Result(property = "totalAmt", column = "TOTAL_AMT"),
        @Result(property = "createdAt", column = "CREATED_AT"),
        @Result(property = "createdBy", column = "CREATED_BY")
    })
    List<OrderMast> selectByMemberCardId(@Param("memberCardId") String memberCardId);

    /**
     * 計算訂單商品總數量
     */
    @Select("SELECT COALESCE(SUM(QUANTITY), 0) FROM TBL_ORDER_DETL WHERE ORDER_ID = #{orderId}")
    int sumQuantityByOrderId(@Param("orderId") String orderId);

    /**
     * 計算訂單商品總金額
     */
    @Select("SELECT COALESCE(SUM(ACT_POS_AMT * QUANTITY), 0) FROM TBL_ORDER_DETL WHERE ORDER_ID = #{orderId}")
    long sumAmountByOrderId(@Param("orderId") String orderId);
}
