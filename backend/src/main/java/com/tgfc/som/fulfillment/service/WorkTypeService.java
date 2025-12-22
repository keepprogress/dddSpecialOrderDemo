package com.tgfc.som.fulfillment.service;

import com.tgfc.som.fulfillment.domain.WorkCategory;
import com.tgfc.som.fulfillment.dto.WorkType;
import com.tgfc.som.order.domain.valueobject.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 工種服務
 *
 * 查詢可用的工種資訊
 * 目前使用 Mock 資料，後續會整合工種主檔 API
 */
@Service
public class WorkTypeService {

    private static final Logger log = LoggerFactory.getLogger(WorkTypeService.class);

    // Mock 工種資料
    private static final Map<String, WorkType> WORK_TYPES = Map.of(
        // 純運工種
        "0000", new WorkType(
            "0000", "純運",
            WorkCategory.PURE_DELIVERY,
            Money.ZERO,
            BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("0.95")
        ),
        // 一般安裝工種
        "0001", new WorkType(
            "0001", "一般安裝",
            WorkCategory.INSTALLATION,
            new Money(500),
            new BigDecimal("0.85"), new BigDecimal("0.90"), new BigDecimal("0.95")
        ),
        // 冷氣安裝工種
        "0002", new WorkType(
            "0002", "冷氣安裝",
            WorkCategory.INSTALLATION,
            new Money(1500),
            new BigDecimal("0.80"), new BigDecimal("0.85"), new BigDecimal("0.90")
        ),
        // 大型家電安裝
        "0003", new WorkType(
            "0003", "大型家電安裝",
            WorkCategory.INSTALLATION,
            new Money(1000),
            new BigDecimal("0.82"), new BigDecimal("0.88"), new BigDecimal("0.92")
        ),
        // 家具組裝
        "0004", new WorkType(
            "0004", "家具組裝",
            WorkCategory.INSTALLATION,
            new Money(300),
            new BigDecimal("0.85"), new BigDecimal("0.90"), new BigDecimal("0.95")
        ),
        // 宅配工種
        "9001", new WorkType(
            "9001", "宅配-常溫",
            WorkCategory.HOME_DELIVERY,
            Money.ZERO,
            BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("1.00")
        ),
        // 宅配低溫
        "9002", new WorkType(
            "9002", "宅配-低溫",
            WorkCategory.HOME_DELIVERY,
            Money.ZERO,
            BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("1.10")
        )
    );

    /**
     * 取得所有工種
     *
     * @return 所有工種清單
     */
    public List<WorkType> getAllWorkTypes() {
        log.info("查詢所有工種");
        return WORK_TYPES.values().stream().toList();
    }

    /**
     * 根據工種代碼查詢
     *
     * @param workTypeId 工種代碼
     * @return 工種資訊
     */
    public Optional<WorkType> getWorkType(String workTypeId) {
        log.info("查詢工種: workTypeId={}", workTypeId);
        return Optional.ofNullable(WORK_TYPES.get(workTypeId));
    }

    /**
     * 根據類別查詢工種
     *
     * @param category 工種類別
     * @return 該類別的工種清單
     */
    public List<WorkType> getWorkTypesByCategory(WorkCategory category) {
        log.info("查詢類別工種: category={}", category);
        return WORK_TYPES.values().stream()
                .filter(wt -> wt.category() == category)
                .toList();
    }

    /**
     * 查詢安裝工種
     *
     * @return 安裝類工種清單
     */
    public List<WorkType> getInstallationWorkTypes() {
        return getWorkTypesByCategory(WorkCategory.INSTALLATION);
    }

    /**
     * 查詢運送工種（含純運）
     *
     * @return 運送類工種清單
     */
    public List<WorkType> getDeliveryWorkTypes() {
        return WORK_TYPES.values().stream()
                .filter(wt -> wt.category().isDelivery())
                .toList();
    }

    /**
     * 查詢宅配工種
     *
     * @return 宅配類工種清單
     */
    public List<WorkType> getHomeDeliveryWorkTypes() {
        return getWorkTypesByCategory(WorkCategory.HOME_DELIVERY);
    }

    /**
     * 取得純運工種
     *
     * @return 純運工種
     */
    public WorkType getPureDeliveryWorkType() {
        return WORK_TYPES.get("0000");
    }

    /**
     * 驗證工種是否存在
     *
     * @param workTypeId 工種代碼
     * @return 是否存在
     */
    public boolean exists(String workTypeId) {
        return WORK_TYPES.containsKey(workTypeId);
    }

    /**
     * 根據商品類別推薦工種
     *
     * @param productCategory 商品類別
     * @return 推薦的工種
     */
    public WorkType recommendWorkType(String productCategory) {
        log.info("推薦工種: productCategory={}", productCategory);

        return switch (productCategory) {
            case "AC" -> WORK_TYPES.get("0002");           // 冷氣 → 冷氣安裝
            case "APPLIANCE" -> WORK_TYPES.get("0003");   // 家電 → 大型家電安裝
            case "FURNITURE" -> WORK_TYPES.get("0004");   // 家具 → 家具組裝
            case "3C" -> WORK_TYPES.get("0000");          // 3C → 純運
            default -> WORK_TYPES.get("0001");            // 預設 → 一般安裝
        };
    }
}
