package com.npu.lms.repository;

import com.npu.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Security 登录需要
    Optional<User> findByUsername(String username);

    // 注册时检查学号是否重复
    Boolean existsByUsername(String username);
}