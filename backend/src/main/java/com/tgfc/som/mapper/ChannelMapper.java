package com.tgfc.som.mapper;

import com.tgfc.som.entity.Channel;
import java.util.List;

public interface ChannelMapper {
    int deleteByPrimaryKey(String channelId);

    int insert(Channel row);

    Channel selectByPrimaryKey(String channelId);

    List<Channel> selectAll();

    int updateByPrimaryKey(Channel row);
}