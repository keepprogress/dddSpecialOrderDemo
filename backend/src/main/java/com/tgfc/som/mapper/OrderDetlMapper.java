package com.tgfc.som.mapper;

import com.tgfc.som.entity.OrderDetl;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 訂單明細 Mapper (TBL_ORDER_DETL)
 */
@Mapper
public interface OrderDetlMapper {

    @Delete("DELETE FROM TBL_ORDER_DETL WHERE LINE_ID = #{lineId}")
    int deleteByPrimaryKey(String lineId);

    @Delete("DELETE FROM TBL_ORDER_DETL WHERE ORDER_ID = #{orderId}")
    int deleteByOrderId(String orderId);

    @Insert("""
        INSERT INTO TBL_ORDER_DETL (LINE_ID, ORDER_ID, SERIAL_NO, SKU_NO, SKU_NAME,
            QUANTITY, POS_AMT, ACT_POS_AMT, TAX_TYPE, DELIVERY_FLAG, STOCK_METHOD,
            DELIVERY_DATE, MEMBER_DISC, BONUS_DISC)
        VALUES (#{lineId}, #{orderId}, #{serialNo}, #{skuNo}, #{skuName},
            #{quantity}, #{posAmt}, #{actPosAmt}, #{taxType}, #{deliveryFlag}, #{stockMethod},
            #{deliveryDate}, #{memberDisc}, #{bonusDisc})
        """)
    int insert(OrderDetl row);

    @Select("SELECT * FROM TBL_ORDER_DETL WHERE LINE_ID = #{lineId}")
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
    OrderDetl selectByPrimaryKey(String lineId);

    @Select("SELECT * FROM TBL_ORDER_DETL WHERE ORDER_ID = #{orderId} ORDER BY SERIAL_NO")
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
    List<OrderDetl> selectByOrderId(String orderId);

    @Update("""
        UPDATE TBL_ORDER_DETL
        SET SKU_NO = #{skuNo}, SKU_NAME = #{skuName}, QUANTITY = #{quantity},
            POS_AMT = #{posAmt}, ACT_POS_AMT = #{actPosAmt}, TAX_TYPE = #{taxType},
            DELIVERY_FLAG = #{deliveryFlag}, STOCK_METHOD = #{stockMethod},
            DELIVERY_DATE = #{deliveryDate}, MEMBER_DISC = #{memberDisc}, BONUS_DISC = #{bonusDisc},
            UPDATED_AT = CURRENT_TIMESTAMP
        WHERE LINE_ID = #{lineId}
        """)
    int updateByPrimaryKey(OrderDetl row);

    @Select("SELECT COUNT(*) FROM TBL_ORDER_DETL WHERE ORDER_ID = #{orderId}")
    int countByOrderId(String orderId);

    @Select("SELECT COALESCE(MAX(SERIAL_NO), 0) + 1 FROM TBL_ORDER_DETL WHERE ORDER_ID = #{orderId}")
    int getNextSerialNo(String orderId);
}
