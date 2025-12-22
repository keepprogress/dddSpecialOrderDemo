package com.tgfc.som.pricing.service;

import com.tgfc.som.pricing.domain.ApportionmentResult;
import com.tgfc.som.pricing.domain.ApportionmentResult.ChangeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 工種變價分攤服務
 *
 * 用於 12-Step 計價流程的 Step 2: apportionmentDiscount()
 *
 * 當安裝/運送工種有變價授權時，將變價差額分攤到該工種下的各商品
 *
 * 觸發條件:
 * - installAuthEmpId IS NOT NULL AND installPrice != actInstallPrice
 * - deliveryAuthEmpId IS NOT NULL AND deliveryPrice != actDeliveryPrice
 *
 * 分攤算法 (餘數處理):
 * 1. 基礎分攤 = 變價總額 / 商品數量 (整數除法)
 * 2. 餘數 = 變價總額 % 商品數量
 * 3. 按 detlSeq 順序逐一分配餘數 ($1/筆)
 *
 * @see <a href="../../../specs/002-create-order/pricing-calculation-spec.md">Section 21.5</a>
 */
@Service
public class ApportionmentService {

    private static final Logger log = LoggerFactory.getLogger(ApportionmentService.class);

    /**
     * 明細行資料 (用於分攤計算)
     */
    public record LineData(
        String lineId,
        int serialNo,
        BigDecimal subtotal
    ) implements Comparable<LineData> {
        @Override
        public int compareTo(LineData other) {
            return Integer.compare(this.serialNo, other.serialNo);
        }
    }

    /**
     * 計算安裝變價分攤
     *
     * @param lines 商品明細 (屬於同一工種)
     * @param originalPrice 原始安裝費
     * @param actualPrice 實際安裝費 (變價後)
     * @param authEmployeeId 授權員工編號
     * @return 分攤結果
     */
    public ApportionmentResult calculateInstallApportionment(
        List<LineData> lines,
        BigDecimal originalPrice,
        BigDecimal actualPrice,
        String authEmployeeId
    ) {
        return calculate(lines, originalPrice, actualPrice, authEmployeeId, ChangeType.INSTALL);
    }

    /**
     * 計算運送變價分攤
     *
     * @param lines 商品明細 (屬於同一工種)
     * @param originalPrice 原始運送費
     * @param actualPrice 實際運送費 (變價後)
     * @param authEmployeeId 授權員工編號
     * @return 分攤結果
     */
    public ApportionmentResult calculateDeliveryApportionment(
        List<LineData> lines,
        BigDecimal originalPrice,
        BigDecimal actualPrice,
        String authEmployeeId
    ) {
        return calculate(lines, originalPrice, actualPrice, authEmployeeId, ChangeType.DELIVERY);
    }

    /**
     * 內部分攤計算
     */
    private ApportionmentResult calculate(
        List<LineData> lines,
        BigDecimal originalPrice,
        BigDecimal actualPrice,
        String authEmployeeId,
        ChangeType changeType
    ) {
        // 檢查是否需要分攤
        if (authEmployeeId == null || authEmployeeId.isEmpty()) {
            log.debug("{}: 無授權員工，跳過分攤", changeType.getName());
            return ApportionmentResult.empty(changeType);
        }

        if (originalPrice == null || actualPrice == null) {
            log.debug("{}: 價格為空，跳過分攤", changeType.getName());
            return ApportionmentResult.empty(changeType);
        }

        BigDecimal changeAmount = originalPrice.subtract(actualPrice);
        if (changeAmount.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("{}: 無變價差額，跳過分攤", changeType.getName());
            return ApportionmentResult.empty(changeType);
        }

        if (lines == null || lines.isEmpty()) {
            log.warn("{}: 無商品明細可分攤", changeType.getName());
            return ApportionmentResult.empty(changeType);
        }

        // 執行分攤計算
        Map<String, BigDecimal> apportionments = apportionBySerialNo(lines, changeAmount);

        log.info("{}: 變價金額={}, 分攤筆數={}, 授權員工={}",
            changeType.getName(), changeAmount, lines.size(), authEmployeeId);

        return new ApportionmentResult.Builder()
            .changeType(changeType)
            .totalChangeAmount(changeAmount)
            .lineApportionments(apportionments)
            .authEmployeeId(authEmployeeId)
            .build();
    }

    /**
     * 按 detlSeq 順序分攤 (餘數處理優化算法)
     *
     * 算法:
     * 1. 基礎分攤 = 變價總額 / 商品數量 (整數除法)
     * 2. 餘數 = 變價總額 % 商品數量
     * 3. 按 serialNo 順序逐一分配餘數 ($1/筆)
     *
     * 範例:
     * - 變價總額: 100 元，3 筆商品
     * - 基礎分攤: 33, 33, 33 (小計 99)
     * - 餘數: 1 元 → 分給 serialNo 最小的商品
     * - 最終: [34, 33, 33] = 100 元
     */
    private Map<String, BigDecimal> apportionBySerialNo(
        List<LineData> lines,
        BigDecimal changeAmount
    ) {
        Map<String, BigDecimal> result = new HashMap<>();

        // 處理負數變價 (減價)
        int total = changeAmount.abs().intValue();
        int sign = changeAmount.signum();
        int count = lines.size();

        if (count == 0 || total == 0) {
            return result;
        }

        int base = total / count;      // 基礎分攤
        int remainder = total % count; // 餘數

        // 按 serialNo 排序
        List<LineData> sortedLines = new ArrayList<>(lines);
        Collections.sort(sortedLines);

        // 分攤計算
        for (int i = 0; i < count; i++) {
            LineData line = sortedLines.get(i);
            int amount = base;

            // 餘數逐一分配
            if (i < remainder) {
                amount += 1;
            }

            // 套用符號 (正數=降價分攤，負數=漲價分攤)
            BigDecimal apportionment = BigDecimal.valueOf(amount * sign);
            result.put(line.lineId(), apportionment);

            log.debug("分攤: lineId={}, serialNo={}, amount={}",
                line.lineId(), line.serialNo(), apportionment);
        }

        // 驗證總額
        BigDecimal totalApportioned = result.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalApportioned.compareTo(changeAmount) != 0) {
            log.error("分攤總額不一致: expected={}, actual={}",
                changeAmount, totalApportioned);
        }

        return result;
    }

    /**
     * 按比例分攤 (Legacy 算法)
     *
     * 根據商品小計佔工種總額的比例分攤
     * 最後一筆承擔餘數誤差
     *
     * @deprecated 建議使用 apportionBySerialNo，結果更可預測
     */
    @Deprecated
    public Map<String, BigDecimal> apportionByRatio(
        List<LineData> lines,
        BigDecimal changeAmount
    ) {
        Map<String, BigDecimal> result = new HashMap<>();

        if (lines == null || lines.isEmpty()) {
            return result;
        }

        // 計算工種總額
        BigDecimal workTypeTotal = lines.stream()
            .map(LineData::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (workTypeTotal.compareTo(BigDecimal.ZERO) == 0) {
            // 無法按比例分攤，改用平均分攤
            return apportionBySerialNo(lines, changeAmount);
        }

        // 按 serialNo 排序
        List<LineData> sortedLines = new ArrayList<>(lines);
        Collections.sort(sortedLines);

        BigDecimal totalApportioned = BigDecimal.ZERO;

        for (int i = 0; i < sortedLines.size(); i++) {
            LineData line = sortedLines.get(i);
            BigDecimal apportionment;

            if (i == sortedLines.size() - 1) {
                // 最後一筆: 承擔餘數
                apportionment = changeAmount.subtract(totalApportioned);
            } else {
                // 其他筆: 按比例計算
                BigDecimal ratio = line.subtotal().divide(workTypeTotal, 10, java.math.RoundingMode.HALF_UP);
                apportionment = changeAmount.multiply(ratio).setScale(0, java.math.RoundingMode.HALF_UP);
            }

            result.put(line.lineId(), apportionment);
            totalApportioned = totalApportioned.add(apportionment);
        }

        return result;
    }
}
