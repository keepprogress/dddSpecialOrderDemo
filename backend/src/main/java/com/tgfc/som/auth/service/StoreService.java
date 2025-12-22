package com.tgfc.som.auth.service;

import com.tgfc.som.auth.dto.MastStoreResponse;
import com.tgfc.som.auth.dto.StoreResponse;
import com.tgfc.som.auth.dto.StoreSelectionRequest;
import com.tgfc.som.entity.Store;
import com.tgfc.som.entity.UserMastStore;
import com.tgfc.som.entity.UserStore;
import com.tgfc.som.mapper.StoreMapper;
import com.tgfc.som.mapper.UserMastStoreMapper;
import com.tgfc.som.mapper.UserStoreMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 店別服務
 * 處理店別查詢和選擇邏輯
 */
@Service
public class StoreService {

    private static final Logger logger = LoggerFactory.getLogger(StoreService.class);

    private final UserMastStoreMapper userMastStoreMapper;
    private final UserStoreMapper userStoreMapper;
    private final StoreMapper storeMapper;

    public StoreService(
            UserMastStoreMapper userMastStoreMapper,
            UserStoreMapper userStoreMapper,
            StoreMapper storeMapper) {
        this.userMastStoreMapper = userMastStoreMapper;
        this.userStoreMapper = userStoreMapper;
        this.storeMapper = storeMapper;
    }

    /**
     * 取得使用者主店別
     *
     * @param empId 員工ID
     * @return 主店別回應 (storeId 為 null 表示全區)
     */
    public MastStoreResponse getMastStore(String empId) {
        // 查詢所有主店別並過濾 (UAT 使用 EMP_ID)
        UserMastStore userMastStore = userMastStoreMapper.selectByExample(null).stream()
            .filter(ums -> empId.equals(ums.getEmpId()))
            .findFirst()
            .orElse(null);

        if (userMastStore == null) {
            logger.warn("No mast store found for user: {}", empId);
            return null;
        }

        // storeId 為 null 表示全區
        if (userMastStore.getStoreId() == null) {
            return new MastStoreResponse(null, "全區");
        }

        // 查詢店別詳情
        Store store = storeMapper.selectByPrimaryKey(userMastStore.getStoreId());
        if (store == null) {
            logger.warn("Store not found: {}", userMastStore.getStoreId());
            return new MastStoreResponse(userMastStore.getStoreId(), "未知店別");
        }

        return new MastStoreResponse(store.getStoreId(), store.getStoreName());
    }

    /**
     * 取得使用者支援店別清單
     *
     * @param empId 員工ID
     * @return 支援店別清單
     */
    public List<StoreResponse> getSupportStores(String empId) {
        // 查詢所有支援店別並過濾 (UAT 使用 EMP_ID)
        List<UserStore> userStores = userStoreMapper.selectByExample(null).stream()
            .filter(us -> empId.equals(us.getEmpId()))
            .toList();

        if (userStores.isEmpty()) {
            return new ArrayList<>();
        }

        // 取得店別ID清單
        List<String> storeIds = userStores.stream()
            .map(UserStore::getStoreId)
            .toList();

        // 批量查詢店別詳情
        List<Store> allStores = storeMapper.selectByExample(null);

        return allStores.stream()
            .filter(store -> storeIds.contains(store.getStoreId()))
            .map(store -> new StoreResponse(store.getStoreId(), store.getStoreName()))
            .toList();
    }

    /**
     * 記錄店別選擇 (目前僅記錄日誌，可擴展為存入資料庫)
     *
     * @param userId  使用者ID
     * @param request 店別選擇請求
     */
    public void recordStoreSelection(String userId, StoreSelectionRequest request) {
        logger.info("Recording store selection for user {}: mast={}, support={}",
            userId, request.mastStoreId(), request.supportStoreIds());

        // TODO: 可在此存入 session 或資料庫記錄使用者的選擇
    }
}
