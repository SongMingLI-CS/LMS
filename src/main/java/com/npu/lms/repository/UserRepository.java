package com.npu.lms.repository;

import com.npu.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Collection; // 【新增】
import java.util.List;       // 【新增】

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 【修复】添加 V2 注册所需的 findByEmail
    Optional<User> findByEmail(String email);

    // 【新增】根据角色列表查找用户
    List<User> findByRoleIn(Collection<String> roles);

    // 【修复】添加 Spring Security 登录所需的 findByUsername
    Optional<User> findByUsername(String username);
}