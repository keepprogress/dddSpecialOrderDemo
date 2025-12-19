package com.tgfc.som.auth.service;

import com.tgfc.som.auth.domain.UserDomainService;
import com.tgfc.som.auth.dto.ChannelResponse;
import com.tgfc.som.entity.Channel;
import com.tgfc.som.entity.User;
import com.tgfc.som.mapper.ChannelMapper;
import com.tgfc.som.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 通路/系統別服務
 * 處理系統別查詢和選擇邏輯
 */
@Service
public class ChannelService {

    private static final Logger logger = LoggerFactory.getLogger(ChannelService.class);

    private final UserMapper userMapper;
    private final ChannelMapper channelMapper;
    private final UserDomainService userDomainService;

    public ChannelService(
            UserMapper userMapper,
            ChannelMapper channelMapper,
            UserDomainService userDomainService) {
        this.userMapper = userMapper;
        this.channelMapper = channelMapper;
        this.userDomainService = userDomainService;
    }

    /**
     * 取得使用者可用的系統別清單
     *
     * @param empId 員工ID
     * @return 系統別清單
     */
    public List<ChannelResponse> getAvailableChannels(String empId) {
        // 查詢使用者 (UAT 主鍵為 EMP_ID)
        User user = userMapper.selectByPrimaryKey(empId);
        if (user == null || user.getSystemFlag() == null) {
            logger.warn("User {} not found or has no system flags", empId);
            return new ArrayList<>();
        }

        // 解析 SYSTEM_FLAG
        String[] flags = userDomainService.parseSystemFlags(user.getSystemFlag());
        if (flags.length == 0) {
            return new ArrayList<>();
        }

        // 查詢所有系統別並過濾
        List<String> channelIds = Arrays.asList(flags);
        List<Channel> allChannels = channelMapper.selectByExample(null);

        return allChannels.stream()
            .filter(channel -> channelIds.contains(channel.getChannelId()))
            .map(channel -> new ChannelResponse(
                channel.getChannelId(),
                channel.getChannelName()
            ))
            .toList();
    }

    /**
     * 記錄系統別選擇 (目前僅記錄日誌，可擴展為存入資料庫)
     *
     * @param userId    使用者ID
     * @param channelId 系統別ID
     */
    public void recordChannelSelection(String userId, String channelId) {
        logger.info("Recording channel selection for user {}: {}", userId, channelId);

        // TODO: 可在此存入 session 或資料庫記錄使用者的選擇
    }
}
