package com.ticket.system.mapper;

import com.ticket.system.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    int insert(User user);
    int update(User user);
    int delete(Long id);
    User selectById(Long id);
    User selectByUsername(String username);
    User selectByPhone(String phone);
    User selectByIdCard(String idCard);
    int updatePassword(@Param("id") Long id, @Param("password") String password);
}