package com.tgfc.som.mapper;

import com.tgfc.som.entity.OrderCompute;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 訂單試算 Mapper (TBL_ORDER_COMPUTE)
 */
@Mapper
public interface OrderComputeMapper {

    @Delete("DELETE FROM TBL_ORDER_COMPUTE WHERE ID = #{id}")
    int deleteByPrimaryKey(Long id);

    @Delete("DELETE FROM TBL_ORDER_COMPUTE WHERE ORDER_ID = #{orderId}")
    int deleteByOrderId(String orderId);

    @Insert("""
        INSERT INTO TBL_ORDER_COMPUTE (ORDER_ID, COMPUTE_TYPE, COMPUTE_NAME,
            TOTAL_PRICE, DISCOUNT, ACT_TOTAL_PRICE)
        VALUES (#{orderId}, #{computeType}, #{computeName},
            #{totalPrice}, #{discount}, #{actTotalPrice})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OrderCompute row);

    @Select("SELECT * FROM TBL_ORDER_COMPUTE WHERE ID = #{id}")
    @Results({
        @Result(property = "id", column = "ID"),
        @Result(property = "orderId", column = "ORDER_ID"),
        @Result(property = "computeType", column = "COMPUTE_TYPE"),
        @Result(property = "computeName", column = "COMPUTE_NAME"),
        @Result(property = "totalPrice", column = "TOTAL_PRICE"),
        @Result(property = "discount", column = "DISCOUNT"),
        @Result(property = "actTotalPrice", column = "ACT_TOTAL_PRICE"),
        @Result(property = "createdAt", column = "CREATED_AT")
    })
    OrderCompute selectByPrimaryKey(Long id);

    @Select("SELECT * FROM TBL_ORDER_COMPUTE WHERE ORDER_ID = #{orderId} ORDER BY COMPUTE_TYPE")
    @Results({
        @Result(property = "id", column = "ID"),
        @Result(property = "orderId", column = "ORDER_ID"),
        @Result(property = "computeType", column = "COMPUTE_TYPE"),
        @Result(property = "computeName", column = "COMPUTE_NAME"),
        @Result(property = "totalPrice", column = "TOTAL_PRICE"),
        @Result(property = "discount", column = "DISCOUNT"),
        @Result(property = "actTotalPrice", column = "ACT_TOTAL_PRICE"),
        @Result(property = "createdAt", column = "CREATED_AT")
    })
    List<OrderCompute> selectByOrderId(String orderId);

    @Update("""
        UPDATE TBL_ORDER_COMPUTE
        SET COMPUTE_TYPE = #{computeType}, COMPUTE_NAME = #{computeName},
            TOTAL_PRICE = #{totalPrice}, DISCOUNT = #{discount}, ACT_TOTAL_PRICE = #{actTotalPrice}
        WHERE ID = #{id}
        """)
    int updateByPrimaryKey(OrderCompute row);
}
