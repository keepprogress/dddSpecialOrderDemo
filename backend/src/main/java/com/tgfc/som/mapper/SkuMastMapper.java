package com.tgfc.som.mapper;

import com.tgfc.som.entity.SkuMast;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 商品主檔 Mapper (TBL_SKU_MAST)
 */
@Mapper
public interface SkuMastMapper {

    @Delete("DELETE FROM TBL_SKU_MAST WHERE SKU_NO = #{skuNo}")
    int deleteByPrimaryKey(String skuNo);

    @Insert("""
        INSERT INTO TBL_SKU_MAST (SKU_NO, SKU_NAME, CATEGORY, TAX_TYPE,
            MARKET_PRICE, REGISTERED_PRICE, POS_PRICE, COST,
            ALLOW_SALES, HOLD_ORDER, IS_SYSTEM_SKU, IS_NEGATIVE_SKU,
            FREE_DELIVERY, FREE_DELIVERY_SHIPPING, ALLOW_DIRECT_SHIPMENT, ALLOW_HOME_DELIVERY)
        VALUES (#{skuNo}, #{skuName}, #{category}, #{taxType},
            #{marketPrice}, #{registeredPrice}, #{posPrice}, #{cost},
            #{allowSales}, #{holdOrder}, #{isSystemSku}, #{isNegativeSku},
            #{freeDelivery}, #{freeDeliveryShipping}, #{allowDirectShipment}, #{allowHomeDelivery})
        """)
    int insert(SkuMast row);

    @Select("SELECT * FROM TBL_SKU_MAST WHERE SKU_NO = #{skuNo}")
    @Results({
        @Result(property = "skuNo", column = "SKU_NO"),
        @Result(property = "skuName", column = "SKU_NAME"),
        @Result(property = "category", column = "CATEGORY"),
        @Result(property = "taxType", column = "TAX_TYPE"),
        @Result(property = "marketPrice", column = "MARKET_PRICE"),
        @Result(property = "registeredPrice", column = "REGISTERED_PRICE"),
        @Result(property = "posPrice", column = "POS_PRICE"),
        @Result(property = "cost", column = "COST"),
        @Result(property = "allowSales", column = "ALLOW_SALES"),
        @Result(property = "holdOrder", column = "HOLD_ORDER"),
        @Result(property = "isSystemSku", column = "IS_SYSTEM_SKU"),
        @Result(property = "isNegativeSku", column = "IS_NEGATIVE_SKU"),
        @Result(property = "freeDelivery", column = "FREE_DELIVERY"),
        @Result(property = "freeDeliveryShipping", column = "FREE_DELIVERY_SHIPPING"),
        @Result(property = "allowDirectShipment", column = "ALLOW_DIRECT_SHIPMENT"),
        @Result(property = "allowHomeDelivery", column = "ALLOW_HOME_DELIVERY"),
        @Result(property = "createdAt", column = "CREATED_AT"),
        @Result(property = "updatedAt", column = "UPDATED_AT")
    })
    SkuMast selectByPrimaryKey(String skuNo);

    @Select("SELECT * FROM TBL_SKU_MAST ORDER BY SKU_NO")
    @Results({
        @Result(property = "skuNo", column = "SKU_NO"),
        @Result(property = "skuName", column = "SKU_NAME"),
        @Result(property = "category", column = "CATEGORY"),
        @Result(property = "taxType", column = "TAX_TYPE"),
        @Result(property = "marketPrice", column = "MARKET_PRICE"),
        @Result(property = "registeredPrice", column = "REGISTERED_PRICE"),
        @Result(property = "posPrice", column = "POS_PRICE"),
        @Result(property = "cost", column = "COST"),
        @Result(property = "allowSales", column = "ALLOW_SALES"),
        @Result(property = "holdOrder", column = "HOLD_ORDER"),
        @Result(property = "isSystemSku", column = "IS_SYSTEM_SKU"),
        @Result(property = "isNegativeSku", column = "IS_NEGATIVE_SKU"),
        @Result(property = "freeDelivery", column = "FREE_DELIVERY"),
        @Result(property = "freeDeliveryShipping", column = "FREE_DELIVERY_SHIPPING"),
        @Result(property = "allowDirectShipment", column = "ALLOW_DIRECT_SHIPMENT"),
        @Result(property = "allowHomeDelivery", column = "ALLOW_HOME_DELIVERY"),
        @Result(property = "createdAt", column = "CREATED_AT"),
        @Result(property = "updatedAt", column = "UPDATED_AT")
    })
    List<SkuMast> selectAll();

    @Update("""
        UPDATE TBL_SKU_MAST
        SET SKU_NAME = #{skuName}, CATEGORY = #{category}, TAX_TYPE = #{taxType},
            MARKET_PRICE = #{marketPrice}, REGISTERED_PRICE = #{registeredPrice},
            POS_PRICE = #{posPrice}, COST = #{cost},
            ALLOW_SALES = #{allowSales}, HOLD_ORDER = #{holdOrder},
            IS_SYSTEM_SKU = #{isSystemSku}, IS_NEGATIVE_SKU = #{isNegativeSku},
            FREE_DELIVERY = #{freeDelivery}, FREE_DELIVERY_SHIPPING = #{freeDeliveryShipping},
            ALLOW_DIRECT_SHIPMENT = #{allowDirectShipment}, ALLOW_HOME_DELIVERY = #{allowHomeDelivery},
            UPDATED_AT = CURRENT_TIMESTAMP
        WHERE SKU_NO = #{skuNo}
        """)
    int updateByPrimaryKey(SkuMast row);

    @Select("SELECT * FROM TBL_SKU_MAST WHERE ALLOW_SALES = 'Y' AND HOLD_ORDER = 'N' ORDER BY SKU_NO")
    @Results({
        @Result(property = "skuNo", column = "SKU_NO"),
        @Result(property = "skuName", column = "SKU_NAME"),
        @Result(property = "category", column = "CATEGORY"),
        @Result(property = "taxType", column = "TAX_TYPE"),
        @Result(property = "marketPrice", column = "MARKET_PRICE"),
        @Result(property = "registeredPrice", column = "REGISTERED_PRICE"),
        @Result(property = "posPrice", column = "POS_PRICE"),
        @Result(property = "cost", column = "COST"),
        @Result(property = "allowSales", column = "ALLOW_SALES"),
        @Result(property = "holdOrder", column = "HOLD_ORDER"),
        @Result(property = "isSystemSku", column = "IS_SYSTEM_SKU"),
        @Result(property = "isNegativeSku", column = "IS_NEGATIVE_SKU"),
        @Result(property = "freeDelivery", column = "FREE_DELIVERY"),
        @Result(property = "freeDeliveryShipping", column = "FREE_DELIVERY_SHIPPING"),
        @Result(property = "allowDirectShipment", column = "ALLOW_DIRECT_SHIPMENT"),
        @Result(property = "allowHomeDelivery", column = "ALLOW_HOME_DELIVERY"),
        @Result(property = "createdAt", column = "CREATED_AT"),
        @Result(property = "updatedAt", column = "UPDATED_AT")
    })
    List<SkuMast> selectSaleable();
}
