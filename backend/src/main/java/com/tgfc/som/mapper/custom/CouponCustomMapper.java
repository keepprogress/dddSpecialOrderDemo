package com.tgfc.som.mapper.custom;

import com.tgfc.som.entity.TblCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * TBL_COUPON CustomMapper
 *
 * 用於 12-Step 計價流程 Step 6: 折價券折扣計算
 *
 * @see com.tgfc.som.entity.TblCoupon
 */
@Mapper
public interface CouponCustomMapper {

    /**
     * 依折價券編號查詢
     *
     * @param dscSku 折價券編號
     * @return 折價券資料
     */
    TblCoupon selectByDscSku(@Param("dscSku") String dscSku);

    /**
     * 查詢有效折價券
     *
     * @param checkDate 檢查日期
     * @return 有效折價券列表
     */
    List<TblCoupon> selectValidCoupons(@Param("checkDate") LocalDate checkDate);

    /**
     * 查詢可用於 SO 的有效折價券
     *
     * @param checkDate 檢查日期
     * @return 可用折價券列表
     */
    List<TblCoupon> selectValidCouponsForSo(@Param("checkDate") LocalDate checkDate);

    /**
     * 更新使用數量
     *
     * @param dscSku 折價券編號
     * @param quantity 使用數量 (累加)
     * @return 更新筆數
     */
    int incrementUseQty(@Param("dscSku") String dscSku, @Param("quantity") int quantity);

    /**
     * 檢查折價券是否有效
     *
     * @param dscSku 折價券編號
     * @param checkDate 檢查日期
     * @return 是否有效
     */
    boolean isValidCoupon(@Param("dscSku") String dscSku, @Param("checkDate") LocalDate checkDate);
}
