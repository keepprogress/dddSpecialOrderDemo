package com.tgfc.som.pricing.service;

import com.tgfc.som.order.domain.Order;
import com.tgfc.som.order.domain.OrderLine;
import com.tgfc.som.order.domain.valueobject.Money;
import com.tgfc.som.pricing.dto.BonusRedemption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 紅利點數服務
 *
 * 處理會員紅利點數的查詢與折抵
 */
@Service
public class BonusService {

    private static final Logger log = LoggerFactory.getLogger(BonusService.class);

    /**
     * 紅利點數兌換比率（每點可折抵金額）
     * 例如：1 點 = 1 元
     */
    private static final int EXCHANGE_RATE = 1;

    /**
     * 最低折抵點數
     */
    private static final int MINIMUM_POINTS = 10;

    // 模擬的會員紅利資料（實際應從 CRM 或會員系統取得）
    private final Map<String, Integer> memberBonusPoints = new HashMap<>();

    public BonusService() {
        initializeSampleData();
    }

    /**
     * 初始化範例資料（開發測試用）
     */
    private void initializeSampleData() {
        memberBonusPoints.put("K00123", 5000);
        memberBonusPoints.put("M001", 1000);
        memberBonusPoints.put("M002", 500);
        log.info("已初始化 {} 位會員的紅利點數", memberBonusPoints.size());
    }

    /**
     * 查詢會員可用紅利點數
     *
     * @param memberId 會員編號
     * @return 可用點數
     */
    public int getAvailablePoints(String memberId) {
        // TODO: 實際應呼叫 CRM 或會員系統 API
        return memberBonusPoints.getOrDefault(memberId, 0);
    }

    /**
     * 檢查是否可使用紅利點數
     *
     * @param memberId 會員編號
     * @param order 訂單
     * @return 是否可使用
     */
    public boolean canUseBonusPoints(String memberId, Order order) {
        // 臨時卡不可使用紅利
        if (order.getCustomer().isTempCard()) {
            return false;
        }

        // 檢查是否有足夠點數
        int availablePoints = getAvailablePoints(memberId);
        return availablePoints >= MINIMUM_POINTS;
    }

    /**
     * 驗證紅利折抵
     *
     * @param memberId 會員編號
     * @param skuNo 折抵商品編號
     * @param points 使用點數
     * @param order 訂單
     * @return 驗證結果（成功返回 Optional 包含 BonusRedemption，失敗返回 empty）
     */
    public Optional<BonusRedemption> validateRedemption(
            String memberId,
            String skuNo,
            int points,
            Order order) {

        log.info("驗證紅利折抵: memberId={}, skuNo={}, points={}", memberId, skuNo, points);

        // 1. 檢查訂單會員
        if (order.getCustomer().isTempCard()) {
            log.warn("臨時卡會員不可使用紅利: memberId={}", memberId);
            return Optional.empty();
        }

        // 2. 檢查可用點數
        int availablePoints = getAvailablePoints(memberId);
        if (points > availablePoints) {
            log.warn("紅利點數不足: memberId={}, available={}, requested={}",
                    memberId, availablePoints, points);
            return Optional.empty();
        }

        // 3. 檢查最低點數
        if (points < MINIMUM_POINTS) {
            log.warn("紅利點數未達最低門檻: points={}, minimum={}", points, MINIMUM_POINTS);
            return Optional.empty();
        }

        // 4. 檢查商品是否存在於訂單中
        Optional<OrderLine> lineOpt = order.getLines().stream()
                .filter(line -> line.getSkuNo().equals(skuNo))
                .findFirst();

        if (lineOpt.isEmpty()) {
            log.warn("商品不在訂單中: skuNo={}", skuNo);
            return Optional.empty();
        }

        OrderLine line = lineOpt.get();

        // 5. 計算折抵金額
        Money discountAmount = BonusRedemption.calculateDiscountAmount(points, EXCHANGE_RATE);

        // 6. 折抵金額不能超過商品金額
        if (discountAmount.amount() > line.getSubtotal().amount()) {
            log.warn("折抵金額超過商品金額: discount={}, subtotal={}",
                    discountAmount.amount(), line.getSubtotal().amount());
            // 調整為商品金額
            int maxPoints = line.getSubtotal().amount() / EXCHANGE_RATE;
            points = maxPoints;
            discountAmount = BonusRedemption.calculateDiscountAmount(points, EXCHANGE_RATE);
            log.info("已調整折抵點數為: points={}", points);
        }

        int remainingPoints = availablePoints - points;

        BonusRedemption redemption = BonusRedemption.create(
                memberId,
                skuNo,
                line.getSkuName(),
                points,
                EXCHANGE_RATE,
                remainingPoints
        );

        log.info("紅利折抵驗證成功: memberId={}, points={}, discount={}",
                memberId, points, discountAmount.amount());

        return Optional.of(redemption);
    }

    /**
     * 執行紅利折抵
     *
     * @param memberId 會員編號
     * @param skuNo 折抵商品編號
     * @param points 使用點數
     * @param order 訂單
     * @return 折抵結果（成功返回 BonusRedemption，失敗返回 null）
     */
    public BonusRedemption redeemPoints(
            String memberId,
            String skuNo,
            int points,
            Order order) {

        Optional<BonusRedemption> validationResult = validateRedemption(memberId, skuNo, points, order);

        if (validationResult.isEmpty()) {
            return null;
        }

        BonusRedemption redemption = validationResult.get();

        // 更新訂單行項的紅利折扣
        for (OrderLine line : order.getLines()) {
            if (line.getSkuNo().equals(skuNo)) {
                line.setBonusDisc(redemption.discountAmount());
                break;
            }
        }

        // 扣除會員點數（實際應呼叫 CRM 系統）
        int currentPoints = memberBonusPoints.getOrDefault(memberId, 0);
        memberBonusPoints.put(memberId, currentPoints - points);

        log.info("紅利折抵完成: memberId={}, points={}, discount={}, remaining={}",
                memberId, points, redemption.discountAmount().amount(), redemption.remainingPoints());

        return redemption;
    }

    /**
     * 取消紅利折抵（退還點數）
     *
     * @param redemption 要取消的折抵記錄
     * @param order 訂單
     */
    public void cancelRedemption(BonusRedemption redemption, Order order) {
        // 清除訂單行項的紅利折扣
        for (OrderLine line : order.getLines()) {
            if (line.getSkuNo().equals(redemption.skuNo())) {
                line.setBonusDisc(Money.ZERO);
                break;
            }
        }

        // 退還會員點數
        int currentPoints = memberBonusPoints.getOrDefault(redemption.memberId(), 0);
        memberBonusPoints.put(redemption.memberId(), currentPoints + redemption.pointsUsed());

        log.info("紅利折抵已取消: memberId={}, pointsReturned={}",
                redemption.memberId(), redemption.pointsUsed());
    }

    /**
     * 取得兌換比率
     */
    public int getExchangeRate() {
        return EXCHANGE_RATE;
    }

    /**
     * 取得最低折抵點數
     */
    public int getMinimumPoints() {
        return MINIMUM_POINTS;
    }
}
