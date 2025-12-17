package com.tgfc.som.mapper;

import com.tgfc.som.entity.UserStore;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserStoreMapper {
    int deleteByPrimaryKey(@Param("empId") String empId, @Param("storeId") String storeId);

    int insert(UserStore row);

    UserStore selectByPrimaryKey(@Param("empId") String empId, @Param("storeId") String storeId);

    List<UserStore> selectAll();

    int updateByPrimaryKey(UserStore row);
}