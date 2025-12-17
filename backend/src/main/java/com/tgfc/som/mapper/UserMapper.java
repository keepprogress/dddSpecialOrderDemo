package com.tgfc.som.mapper;

import com.tgfc.som.entity.User;
import java.util.List;

public interface UserMapper {
    int deleteByPrimaryKey(String empId);

    int insert(User row);

    User selectByPrimaryKey(String empId);

    List<User> selectAll();

    int updateByPrimaryKey(User row);
}