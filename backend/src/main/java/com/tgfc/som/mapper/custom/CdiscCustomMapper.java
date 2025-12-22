package com.tgfc.som.mapper.custom;

import com.tgfc.som.entity.TblCdisc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * TBL_CDISC CustomMapper (會員折扣設定)
 *
 * 用於 12-Step 計價流程 Step 4-8: 會員折扣計算
 *
 * @see com.tgfc.som.entity.TblCdisc
 */
@Mapper
public interface CdiscCustomMapper {

    /**
     * 依折扣代號查詢所有設定
     *
     * @param discountId 折扣代號
     * @return 折扣設定列表
     */
    List<TblCdisc> selectByDiscountId(@Param("discountId") String discountId);

    /**
     * 依折扣代號和通路查詢
     *
     * @param discountId 折扣代號
     * @param channelId 通路代號
     * @return 折扣設定列表
     */
    List<TblCdisc> selectByDiscountIdAndChannel(
            @Param("discountId") String discountId,
            @Param("channelId") String channelId);

    /**
     * 查詢適用於指定商品的折扣設定
     *
     * @param discountId 折扣代號
     * @param channelId 通路代號
     * @param subDeptId 大類
     * @param classId 中類
     * @param subClassId 小類
     * @param skuNo 商品編號
     * @param checkDate 檢查日期
     * @return 適用的折扣設定列表 (按優先序排列)
     */
    List<TblCdisc> selectApplicableDiscounts(
            @Param("discountId") String discountId,
            @Param("channelId") String channelId,
            @Param("subDeptId") String subDeptId,
            @Param("classId") String classId,
            @Param("subClassId") String subClassId,
            @Param("skuNo") String skuNo,
            @Param("checkDate") LocalDate checkDate);

    /**
     * 查詢 Type 0 (Discounting) 折扣設定
     *
     * @param discountId 折扣代號
     * @param channelId 通路代號
     * @param checkDate 檢查日期
     * @return Type 0 折扣設定列表
     */
    List<TblCdisc> selectType0Discounts(
            @Param("discountId") String discountId,
            @Param("channelId") String channelId,
            @Param("checkDate") LocalDate checkDate);

    /**
     * 查詢 Type 1 (Down Margin) 折扣設定
     *
     * @param discountId 折扣代號
     * @param channelId 通路代號
     * @param checkDate 檢查日期
     * @return Type 1 折扣設定列表
     */
    List<TblCdisc> selectType1Discounts(
            @Param("discountId") String discountId,
            @Param("channelId") String channelId,
            @Param("checkDate") LocalDate checkDate);

    /**
     * 查詢 Type 2 (Cost Markup) 折扣設定
     *
     * @param discountId 折扣代號
     * @param channelId 通路代號
     * @param checkDate 檢查日期
     * @return Type 2 折扣設定列表
     */
    List<TblCdisc> selectType2Discounts(
            @Param("discountId") String discountId,
            @Param("channelId") String channelId,
            @Param("checkDate") LocalDate checkDate);

    /**
     * 查詢指定商品的最佳折扣
     * (優先順序: SKU > 小類 > 中類 > 大類 > 全部)
     *
     * @param discountId 折扣代號
     * @param channelId 通路代號
     * @param subDeptId 大類
     * @param classId 中類
     * @param subClassId 小類
     * @param skuNo 商品編號
     * @param checkDate 檢查日期
     * @return 最佳折扣設定 (可能為 null)
     */
    TblCdisc selectBestDiscountForSku(
            @Param("discountId") String discountId,
            @Param("channelId") String channelId,
            @Param("subDeptId") String subDeptId,
            @Param("classId") String classId,
            @Param("subClassId") String subClassId,
            @Param("skuNo") String skuNo,
            @Param("checkDate") LocalDate checkDate);
}
