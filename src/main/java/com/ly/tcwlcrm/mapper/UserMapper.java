package com.ly.tcwlcrm.mapper;

import com.ly.tcwlcrm.pojo.User;

import java.util.List;

/**
 * @author gpd
 * @date 2019/12/11
 */
public interface UserMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(User record);

    int insertSelective(User record);

    User selectByPrimaryKey(Integer id);

    int updateByPrimaryKey(User record);

    List<User> select();
}