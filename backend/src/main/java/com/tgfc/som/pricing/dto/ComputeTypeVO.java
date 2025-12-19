package com.tgfc.som.pricing.dto;

/**
 * 試算類型 VO
 *
 * 對應 7 種 ComputeType：
 * 1. 商品小計
 * 2. 安裝小計
 * 3. 運送小計
 * 4. 會員卡折扣
 * 5. 直送費用小計
 * 6. 折價券折扣
 * 7. 紅利點數折扣
 *
 * @param computeType 試算類型代碼 (1-7)
 * @param computeName 類型名稱
 * @param totalPrice 原始價格
 * @param discount 折扣金額（負數）
 * @param actTotalPrice 實際價格
 */
public record ComputeTypeVO(
    String computeType,
    String computeName,
    int totalPrice,
    int discount,
    int actTotalPrice
) {

    /**
     * 建立 ComputeType 1: 商品小計
     */
    public static ComputeTypeVO productTotal(int total, int discount) {
        return new ComputeTypeVO("1", "商品小計", total, discount, total + discount);
    }

    /**
     * 建立 ComputeType 2: 安裝小計
     */
    public static ComputeTypeVO installationTotal(int total, int discount) {
        return new ComputeTypeVO("2", "安裝小計", total, discount, total + discount);
    }

    /**
     * 建立 ComputeType 3: 運送小計
     */
    public static ComputeTypeVO deliveryTotal(int total, int discount) {
        return new ComputeTypeVO("3", "運送小計", total, discount, total + discount);
    }

    /**
     * 建立 ComputeType 4: 會員卡折扣
     */
    public static ComputeTypeVO memberDiscount(int discount) {
        return new ComputeTypeVO("4", "會員卡折扣", 0, discount, discount);
    }

    /**
     * 建立 ComputeType 5: 直送費用小計
     */
    public static ComputeTypeVO directShipmentTotal(int total) {
        return new ComputeTypeVO("5", "直送費用小計", total, 0, total);
    }

    /**
     * 建立 ComputeType 6: 折價券折扣
     */
    public static ComputeTypeVO couponDiscount(int discount) {
        return new ComputeTypeVO("6", "折價券折扣", 0, discount, discount);
    }

    /**
     * 建立 ComputeType 7: 紅利點數折扣
     */
    public static ComputeTypeVO bonusDiscount(int discount) {
        return new ComputeTypeVO("7", "紅利點數折扣", 0, discount, discount);
    }

    /**
     * 建立通用的 ComputeTypeVO
     */
    public static ComputeTypeVO of(String type, String name, int total, int discount) {
        return new ComputeTypeVO(type, name, total, discount, total + discount);
    }
}
