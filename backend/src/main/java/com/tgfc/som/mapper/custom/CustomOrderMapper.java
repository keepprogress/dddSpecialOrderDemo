package com.tgfc.som.mapper.custom;

import com.tgfc.som.entity.TblOrder;
import com.tgfc.som.entity.TblOrderDetl;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 訂單自訂 Mapper
 *
 * 用於複雜查詢和效能敏感的操作
 * 遵循 Constitution VIII: MyBatis Generator Pattern
 *
 * 注意：表名和欄位名依據 docs/tables/*.html 實際定義
 * - TBL_ORDER (非 TBL_ORDER_MAST)
 * - TBL_ORDER_DETL
 */
@Mapper
public interface CustomOrderMapper {

    /**
     * 查詢訂單主表
     */
    @Select("""
        SELECT ORDER_ID, PROJECT_ID, MEMBER_CARD_ID, MEMBER_NAME,
               INSTALL_ADDR_ZIP, INSTALL_ADDR, STORE_ID, CHANNEL_ID,
               ORDER_STATUS_ID, HANDLE_EMP_ID, SPECIALIST_EMP_ID,
               CREATE_DATE, UPDATE_DATE, CREATE_EMP_ID, UPDATE_EMP_ID
        FROM TBL_ORDER
        WHERE ORDER_ID = #{orderId}
        """)
    @Results(id = "tblOrderResultMap", value = {
        @Result(property = "orderId", column = "ORDER_ID", id = true),
        @Result(property = "projectId", column = "PROJECT_ID"),
        @Result(property = "memberCardId", column = "MEMBER_CARD_ID"),
        @Result(property = "memberName", column = "MEMBER_NAME"),
        @Result(property = "installAddrZip", column = "INSTALL_ADDR_ZIP"),
        @Result(property = "installAddr", column = "INSTALL_ADDR"),
        @Result(property = "storeId", column = "STORE_ID"),
        @Result(property = "channelId", column = "CHANNEL_ID"),
        @Result(property = "orderStatusId", column = "ORDER_STATUS_ID"),
        @Result(property = "handleEmpId", column = "HANDLE_EMP_ID"),
        @Result(property = "specialistEmpId", column = "SPECIALIST_EMP_ID"),
        @Result(property = "createDate", column = "CREATE_DATE"),
        @Result(property = "updateDate", column = "UPDATE_DATE"),
        @Result(property = "createEmpId", column = "CREATE_EMP_ID"),
        @Result(property = "updateEmpId", column = "UPDATE_EMP_ID")
    })
    TblOrder selectOrderById(String orderId);

    /**
     * 批次查詢訂單明細（用於避免 N+1 問題）
     *
     * 注意：PK 為 ORDER_ID + DETL_SEQ_ID
     */
    @Select("""
        <script>
        SELECT DETL_SEQ_ID, ORDER_ID, STORE_ID, SKU_NO, SKU_NAME,
               QUANTITY, POS_AMT, ACT_POS_AMT, TAX_TYPE,
               DELIVERY_FLAG, WORKTYPE_ID, DELIVERY_DATE,
               DISCOUNT_AMT, CREATE_DATE, UPDATE_DATE
        FROM TBL_ORDER_DETL
        WHERE ORDER_ID IN
        <foreach item="orderId" collection="orderIds" open="(" separator="," close=")">
            #{orderId}
        </foreach>
        ORDER BY ORDER_ID, DETL_SEQ_ID
        </script>
        """)
    @Results({
        @Result(property = "detlSeqId", column = "DETL_SEQ_ID"),
        @Result(property = "orderId", column = "ORDER_ID"),
        @Result(property = "storeId", column = "STORE_ID"),
        @Result(property = "skuNo", column = "SKU_NO"),
        @Result(property = "skuName", column = "SKU_NAME"),
        @Result(property = "quantity", column = "QUANTITY"),
        @Result(property = "posAmt", column = "POS_AMT"),
        @Result(property = "actPosAmt", column = "ACT_POS_AMT"),
        @Result(property = "taxType", column = "TAX_TYPE"),
        @Result(property = "deliveryFlag", column = "DELIVERY_FLAG"),
        @Result(property = "worktypeId", column = "WORKTYPE_ID"),
        @Result(property = "deliveryDate", column = "DELIVERY_DATE"),
        @Result(property = "discountAmt", column = "DISCOUNT_AMT"),
        @Result(property = "createDate", column = "CREATE_DATE"),
        @Result(property = "updateDate", column = "UPDATE_DATE")
    })
    List<TblOrderDetl> selectDetailsByOrderIds(@Param("orderIds") List<String> orderIds);

    /**
     * 查詢單一訂單的所有明細
     */
    @Select("""
        SELECT DETL_SEQ_ID, ORDER_ID, STORE_ID, SKU_NO, SKU_NAME,
               QUANTITY, POS_AMT, ACT_POS_AMT, TAX_TYPE,
               DELIVERY_FLAG, WORKTYPE_ID, DELIVERY_DATE,
               DISCOUNT_AMT, CREATE_DATE, UPDATE_DATE
        FROM TBL_ORDER_DETL
        WHERE ORDER_ID = #{orderId}
        ORDER BY DETL_SEQ_ID
        """)
    @Results({
        @Result(property = "detlSeqId", column = "DETL_SEQ_ID"),
        @Result(property = "orderId", column = "ORDER_ID"),
        @Result(property = "storeId", column = "STORE_ID"),
        @Result(property = "skuNo", column = "SKU_NO"),
        @Result(property = "skuName", column = "SKU_NAME"),
        @Result(property = "quantity", column = "QUANTITY"),
        @Result(property = "posAmt", column = "POS_AMT"),
        @Result(property = "actPosAmt", column = "ACT_POS_AMT"),
        @Result(property = "taxType", column = "TAX_TYPE"),
        @Result(property = "deliveryFlag", column = "DELIVERY_FLAG"),
        @Result(property = "worktypeId", column = "WORKTYPE_ID"),
        @Result(property = "deliveryDate", column = "DELIVERY_DATE"),
        @Result(property = "discountAmt", column = "DISCOUNT_AMT"),
        @Result(property = "createDate", column = "CREATE_DATE"),
        @Result(property = "updateDate", column = "UPDATE_DATE")
    })
    List<TblOrderDetl> selectDetailsByOrderId(@Param("orderId") String orderId);

    /**
     * 依店別查詢訂單（分頁用）
     */
    @Select("""
        SELECT ORDER_ID, PROJECT_ID, MEMBER_CARD_ID, MEMBER_NAME,
               STORE_ID, CHANNEL_ID, ORDER_STATUS_ID,
               CREATE_DATE, CREATE_EMP_ID
        FROM TBL_ORDER
        WHERE STORE_ID = #{storeId}
        ORDER BY CREATE_DATE DESC
        OFFSET #{offset} ROWS FETCH NEXT #{limit} ROWS ONLY
        """)
    @Results({
        @Result(property = "orderId", column = "ORDER_ID"),
        @Result(property = "projectId", column = "PROJECT_ID"),
        @Result(property = "memberCardId", column = "MEMBER_CARD_ID"),
        @Result(property = "memberName", column = "MEMBER_NAME"),
        @Result(property = "storeId", column = "STORE_ID"),
        @Result(property = "channelId", column = "CHANNEL_ID"),
        @Result(property = "orderStatusId", column = "ORDER_STATUS_ID"),
        @Result(property = "createDate", column = "CREATE_DATE"),
        @Result(property = "createEmpId", column = "CREATE_EMP_ID")
    })
    List<TblOrder> selectByStoreIdPaged(
            @Param("storeId") String storeId,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 統計店別訂單數量
     */
    @Select("SELECT COUNT(*) FROM TBL_ORDER WHERE STORE_ID = #{storeId}")
    int countByStoreId(@Param("storeId") String storeId);

    /**
     * 依會員查詢訂單
     */
    @Select("""
        SELECT ORDER_ID, PROJECT_ID, MEMBER_CARD_ID, MEMBER_NAME,
               STORE_ID, CHANNEL_ID, ORDER_STATUS_ID,
               CREATE_DATE, CREATE_EMP_ID
        FROM TBL_ORDER
        WHERE MEMBER_CARD_ID = #{memberCardId}
        ORDER BY CREATE_DATE DESC
        """)
    @Results({
        @Result(property = "orderId", column = "ORDER_ID"),
        @Result(property = "projectId", column = "PROJECT_ID"),
        @Result(property = "memberCardId", column = "MEMBER_CARD_ID"),
        @Result(property = "memberName", column = "MEMBER_NAME"),
        @Result(property = "storeId", column = "STORE_ID"),
        @Result(property = "channelId", column = "CHANNEL_ID"),
        @Result(property = "orderStatusId", column = "ORDER_STATUS_ID"),
        @Result(property = "createDate", column = "CREATE_DATE"),
        @Result(property = "createEmpId", column = "CREATE_EMP_ID")
    })
    List<TblOrder> selectByMemberCardId(@Param("memberCardId") String memberCardId);

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
