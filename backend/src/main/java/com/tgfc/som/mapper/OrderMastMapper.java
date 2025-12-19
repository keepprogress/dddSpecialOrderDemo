package com.tgfc.som.mapper;

import com.tgfc.som.entity.OrderMast;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 訂單主表 Mapper (TBL_ORDER_MAST)
 */
@Mapper
public interface OrderMastMapper {

    @Delete("DELETE FROM TBL_ORDER_MAST WHERE ORDER_ID = #{orderId}")
    int deleteByPrimaryKey(String orderId);

    @Insert("""
        INSERT INTO TBL_ORDER_MAST (ORDER_ID, PROJECT_ID, MEMBER_CARD_ID, MEMBER_NAME,
            INSTALL_ZIP, INSTALL_ADDR, STORE_ID, CHANNEL_ID, ORDER_STATUS, TOTAL_AMT,
            HANDLER_ID, SPECIALIST_ID, CREATED_BY, UPDATED_BY)
        VALUES (#{orderId}, #{projectId}, #{memberCardId}, #{memberName},
            #{installZip}, #{installAddr}, #{storeId}, #{channelId}, #{orderStatus}, #{totalAmt},
            #{handlerId}, #{specialistId}, #{createdBy}, #{updatedBy})
        """)
    int insert(OrderMast row);

    @Select("SELECT * FROM TBL_ORDER_MAST WHERE ORDER_ID = #{orderId}")
    @Results({
        @Result(property = "orderId", column = "ORDER_ID"),
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
    OrderMast selectByPrimaryKey(String orderId);

    @Select("SELECT * FROM TBL_ORDER_MAST ORDER BY CREATED_AT DESC")
    @Results({
        @Result(property = "orderId", column = "ORDER_ID"),
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
    List<OrderMast> selectAll();

    @Update("""
        UPDATE TBL_ORDER_MAST
        SET PROJECT_ID = #{projectId}, MEMBER_CARD_ID = #{memberCardId}, MEMBER_NAME = #{memberName},
            INSTALL_ZIP = #{installZip}, INSTALL_ADDR = #{installAddr}, STORE_ID = #{storeId},
            CHANNEL_ID = #{channelId}, ORDER_STATUS = #{orderStatus}, TOTAL_AMT = #{totalAmt},
            HANDLER_ID = #{handlerId}, SPECIALIST_ID = #{specialistId},
            UPDATED_AT = CURRENT_TIMESTAMP, UPDATED_BY = #{updatedBy}
        WHERE ORDER_ID = #{orderId}
        """)
    int updateByPrimaryKey(OrderMast row);

    @Update("UPDATE TBL_ORDER_MAST SET ORDER_STATUS = #{status}, UPDATED_AT = CURRENT_TIMESTAMP WHERE ORDER_ID = #{orderId}")
    int updateStatus(@Param("orderId") String orderId, @Param("status") String status);

    @Select("SELECT NEXT VALUE FOR SEQ_ORDER_ID")
    Long getNextOrderId();
}
