package com.delta.mapper;

import com.delta.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {

    User findById(Long id);

    User findByPhone(String phone);

    int insert(User user);

    int updateProfile(Long id, String nickname);

    int updateRole(Long id, String role);

    List<User> findBoosters();

    List<User> findAll(int offset, int limit);

    long count();
}
