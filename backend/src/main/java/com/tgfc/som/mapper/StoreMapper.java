package com.tgfc.som.mapper;

import com.tgfc.som.entity.Store;
import java.util.List;

public interface StoreMapper {
    int deleteByPrimaryKey(String storeId);

    int insert(Store row);

    Store selectByPrimaryKey(String storeId);

    List<Store> selectAll();

    int updateByPrimaryKey(Store row);
}