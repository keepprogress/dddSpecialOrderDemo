package com.tgfc.som.member.service;

import com.tgfc.som.common.config.ExternalServiceConfig;
import com.tgfc.som.member.dto.MemberResponse;
import com.tgfc.som.member.dto.TempMemberRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 會員服務
 *
 * 負責會員查詢與臨時卡建立
 * 支援 CRM API timeout/fallback：
 * - 2 秒 timeout
 * - timeout 時使用快取資料
 */
@Service
public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    private final ExternalServiceConfig config;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // 會員快取（用於 CRM timeout fallback）
    // key: memberId, value: CachedMember(response, cachedAt)
    private final Map<String, CachedMember> memberCache = new ConcurrentHashMap<>();
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    public MemberService(ExternalServiceConfig config) {
        this.config = config;
    }

    /**
     * 快取資料記錄
     */
    private record CachedMember(MemberResponse response, Instant cachedAt) {
        boolean isExpired() {
            return Instant.now().isAfter(cachedAt.plus(CACHE_TTL));
        }
    }

    // Mock 會員資料
    private static final Map<String, MemberResponse> MOCK_MEMBERS = Map.of(
        // K00123 測試會員 - Type 0 (Discounting 折價)
        "K00123", new MemberResponse(
            "K00123", "0", "SIT測試人員", null, null, "0912345678",
            null, null, "0", "折價 (Discounting)", new BigDecimal("0.95"), null, false
        ),
        // Type 1 (Down Margin) 測試會員
        "M00001", new MemberResponse(
            "M00001", "0", "下降測試會員", null, null, "0911111111",
            null, null, "1", "下降 (Down Margin)", null, null, false
        ),
        // Type 2 (Cost Markup) 測試會員
        "M00002", new MemberResponse(
            "M00002", "1", "成本加成會員", null, null, "0922222222",
            null, null, "2", "成本加成 (Cost Markup)", null, new BigDecimal("1.05"), false
        ),
        // Special 特殊會員
        "S00001", new MemberResponse(
            "S00001", "1", "特殊VIP會員", null, null, "0933333333",
            null, null, "SPECIAL", "特殊會員", null, null, false
        )
    );

    // 臨時卡儲存（記憶體）
    private final Map<String, MemberResponse> tempMemberStore = new ConcurrentHashMap<>();
    private final AtomicLong tempMemberSequence = new AtomicLong(1L);

    /**
     * 查詢會員（含 CRM timeout/fallback 處理）
     *
     * @param memberId 會員卡號
     * @return 會員資料，若查無資料則返回空
     */
    public Optional<MemberResponse> getMember(String memberId) {
        log.info("查詢會員: memberId={}", memberId);

        if (memberId == null || memberId.isBlank()) {
            return Optional.empty();
        }

        // 先查臨時卡
        MemberResponse tempMember = tempMemberStore.get(memberId);
        if (tempMember != null) {
            log.info("找到臨時卡會員: memberId={}", memberId);
            return Optional.of(tempMember);
        }

        // 呼叫 CRM API（含 timeout 處理）
        return callCrmWithTimeout(memberId);
    }

    /**
     * 呼叫 CRM API（含 timeout/fallback 處理）
     *
     * @param memberId 會員卡號
     * @return 會員資料
     */
    private Optional<MemberResponse> callCrmWithTimeout(String memberId) {
        long timeoutMillis = config.getCrm().getTimeoutMillis();

        Future<Optional<MemberResponse>> future = executor.submit(() -> callCrmApi(memberId));

        try {
            Optional<MemberResponse> result = future.get(timeoutMillis, TimeUnit.MILLISECONDS);

            // 成功取得資料，更新快取
            result.ifPresent(member -> updateCache(memberId, member));

            return result;

        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("CRM API timeout: memberId={}, timeout={}ms，嘗試使用快取", memberId, timeoutMillis);
            return getCachedMember(memberId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("CRM API 呼叫被中斷: memberId={}", memberId);
            return getCachedMember(memberId);

        } catch (ExecutionException e) {
            log.error("CRM API 呼叫失敗: memberId={}, error={}", memberId, e.getCause().getMessage());
            return getCachedMember(memberId);
        }
    }

    /**
     * 呼叫 CRM API（目前使用 Mock 資料）
     *
     * TODO: 整合實際 CRM API
     */
    private Optional<MemberResponse> callCrmApi(String memberId) {
        // 目前使用 Mock 資料
        MemberResponse member = MOCK_MEMBERS.get(memberId.toUpperCase());
        if (member != null) {
            log.info("CRM 回應: memberId={}, discType={}", memberId, member.discType());
            return Optional.of(member);
        }

        log.info("CRM 查無會員: memberId={}", memberId);
        return Optional.empty();
    }

    /**
     * 更新會員快取
     */
    private void updateCache(String memberId, MemberResponse member) {
        memberCache.put(memberId.toUpperCase(), new CachedMember(member, Instant.now()));
        log.debug("會員快取已更新: memberId={}", memberId);
    }

    /**
     * 取得快取的會員資料（fallback 用）
     */
    private Optional<MemberResponse> getCachedMember(String memberId) {
        CachedMember cached = memberCache.get(memberId.toUpperCase());

        if (cached == null) {
            log.warn("無快取資料可用: memberId={}", memberId);
            return Optional.empty();
        }

        if (cached.isExpired()) {
            log.warn("快取已過期，但仍使用 fallback: memberId={}, cachedAt={}", memberId, cached.cachedAt());
        } else {
            log.info("使用快取資料: memberId={}, cachedAt={}", memberId, cached.cachedAt());
        }

        return Optional.of(cached.response());
    }

    /**
     * 建立臨時卡
     *
     * @param request 臨時卡資料
     * @return 臨時卡會員資料
     */
    public MemberResponse createTempMember(TempMemberRequest request) {
        log.info("建立臨時卡: name={}, cellPhone={}", request.name(), request.cellPhone());

        // 產生臨時卡號
        long seq = tempMemberSequence.getAndIncrement();
        String tempMemberId = String.format("TEMP%06d", seq);

        MemberResponse tempMember = MemberResponse.ofTempCard(
            tempMemberId,
            request.name(),
            request.cellPhone(),
            request.address(),
            request.zipCode()
        );

        // 儲存臨時卡
        tempMemberStore.put(tempMemberId, tempMember);

        log.info("臨時卡建立成功: tempMemberId={}", tempMemberId);

        return tempMember;
    }

    /**
     * 驗證會員卡號格式
     */
    public boolean isValidMemberId(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            return false;
        }
        // 一般會員卡號格式: 英文字母開頭 + 數字，至少 5 碼
        return memberId.matches("^[A-Za-z]\\d{4,}$");
    }

    /**
     * 驗證手機號碼格式
     */
    public boolean isValidCellPhone(String cellPhone) {
        if (cellPhone == null || cellPhone.isBlank()) {
            return false;
        }
        // 台灣手機號碼格式: 09 開頭，共 10 碼
        return cellPhone.matches("^09\\d{8}$");
    }

    /**
     * 驗證郵遞區號格式
     */
    public boolean isValidZipCode(String zipCode) {
        if (zipCode == null || zipCode.isBlank()) {
            return false;
        }
        // 3-5 碼數字
        return zipCode.matches("^\\d{3,5}$");
    }
}
