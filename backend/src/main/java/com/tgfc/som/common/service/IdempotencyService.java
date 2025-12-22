package com.tgfc.som.common.service;

import com.tgfc.som.common.exception.DuplicateSubmissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 冪等鍵服務
 *
 * 使用 ConcurrentHashMap 記錄 5 秒內已處理的冪等鍵
 * ScheduledExecutor 定期清理過期 key
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final long TTL_SECONDS = 5;
    private static final long CLEANUP_INTERVAL_SECONDS = 10;

    private final ConcurrentHashMap<String, IdempotencyRecord> keyStore = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;

    public IdempotencyService() {
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "idempotency-cleanup");
            t.setDaemon(true);
            return t;
        });

        // 定期清理過期的 key
        this.cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredKeys,
                CLEANUP_INTERVAL_SECONDS,
                CLEANUP_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        log.info("IdempotencyService 已啟動，TTL={}秒，清理間隔={}秒",
                TTL_SECONDS, CLEANUP_INTERVAL_SECONDS);
    }

    /**
     * 檢查冪等鍵是否重複
     *
     * @param idempotencyKey 冪等鍵
     * @return 若重複，返回原始訂單 ID；若不重複，返回 null
     */
    public String checkDuplicate(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        IdempotencyRecord record = keyStore.get(idempotencyKey);
        if (record != null && !record.isExpired()) {
            log.warn("偵測到重複提交: idempotencyKey={}, existingOrderId={}",
                    idempotencyKey, record.orderId());
            return record.orderId();
        }

        return null;
    }

    /**
     * 檢查並拋出重複提交異常
     *
     * @param idempotencyKey 冪等鍵
     * @throws DuplicateSubmissionException 若冪等鍵重複
     */
    public void checkAndThrow(String idempotencyKey) {
        String existingOrderId = checkDuplicate(idempotencyKey);
        if (existingOrderId != null) {
            throw new DuplicateSubmissionException(idempotencyKey, existingOrderId);
        }
    }

    /**
     * 記錄冪等鍵
     *
     * @param idempotencyKey 冪等鍵
     * @param orderId        關聯的訂單 ID
     */
    public void record(String idempotencyKey, String orderId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        IdempotencyRecord record = new IdempotencyRecord(
                orderId,
                Instant.now().plusSeconds(TTL_SECONDS)
        );

        keyStore.put(idempotencyKey, record);
        log.debug("記錄冪等鍵: idempotencyKey={}, orderId={}", idempotencyKey, orderId);
    }

    /**
     * 清理過期的 key
     */
    private void cleanupExpiredKeys() {
        int beforeSize = keyStore.size();
        keyStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int afterSize = keyStore.size();

        if (beforeSize != afterSize) {
            log.debug("清理過期冪等鍵: before={}, after={}", beforeSize, afterSize);
        }
    }

    /**
     * 取得目前儲存的 key 數量（用於監控）
     */
    public int getKeyCount() {
        return keyStore.size();
    }

    /**
     * 冪等鍵記錄
     */
    private record IdempotencyRecord(String orderId, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
