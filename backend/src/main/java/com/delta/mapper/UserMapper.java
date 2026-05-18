package com.delta.mapper;

import com.delta.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户数据访问层 — MyBatis 接口。
 * SQL 在 resources/mapper/UserMapper.xml 中。
 */
@Mapper
public interface UserMapper {

    User findById(Long id);

    /** 根据手机号查用户（登录、注册查重用） */
    User findByPhone(String phone);

    /** 插入用户，id 由数据库自增回填 */
    int insert(User user);

    int updateProfile(Long id, String nickname);

    /** 修改用户角色（player → booster） */
    int updateRole(Long id, String role);

    /** 查询所有打手（客服派单下拉选择用） */
    List<User> findBoosters();

    /** 所有用户列表（分页） */
    List<User> findAll(int offset, int limit);

    long count();
}
