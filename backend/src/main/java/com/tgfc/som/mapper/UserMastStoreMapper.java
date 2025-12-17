package com.tgfc.som.mapper;

import com.tgfc.som.entity.UserMastStore;
import java.util.List;

public interface UserMastStoreMapper {
    int insert(UserMastStore row);

    List<UserMastStore> selectAll();
}