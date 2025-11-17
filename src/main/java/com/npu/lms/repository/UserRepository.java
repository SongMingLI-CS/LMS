package com.npu.lms.repository;

import com.npu.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 【修复】添加 V2 注册所需的 findByEmail
    Optional<User> findByEmail(String email);

    // 【修复】添加 Spring Security 登录所需的 findByUsername
    Optional<User> findByUsername(String username);
}