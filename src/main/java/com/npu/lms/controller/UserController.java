package com.npu.lms.controller; // (请确保包名正确)

import com.npu.lms.dto.UserAdminDTO;
import com.npu.lms.dto.UserUpsertRequest;
import com.npu.lms.entity.User;
import com.npu.lms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 引入安全注解
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users") // 专用于用户管理的 Controller
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPERADMIN')") // 仅管理员可访问
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取所有用户列表。
     * P0 安全：返回 UserAdminDTO，绝不序列化口令哈希/验证码等实体字段。
     */
    @GetMapping
    public ResponseEntity<List<UserAdminDTO>> getAllUsers() {
        List<UserAdminDTO> users = userService.findAllUsers().stream()
                .map(UserAdminDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * 用户管理页服务端检索 + 分页。
     */
    @GetMapping("/search")
    public ResponseEntity<Page<UserAdminDTO>> searchUsers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        return ResponseEntity.ok(userService.searchUsers(q, page, size, sort).map(UserAdminDTO::from));
    }

    /**
     * 按 ID 获取单个用户
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserAdminDTO> getUserById(@PathVariable Long id) {
        User user = userService.findUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(UserAdminDTO.from(user));
    }

    /**
     * 新增用户 (仅 Superadmin)。前端「添加用户」依赖此接口。
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_SUPERADMIN')")
    public ResponseEntity<?> createUser(@RequestBody UserUpsertRequest request) {
        try {
            User created = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(UserAdminDTO.from(created));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 更新用户信息（角色白名单校验 + 服务端加密密码）
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserUpsertRequest request) {
        try {
            // 调用 Service 层的更新逻辑
            User updatedUser = userService.updateUser(id, request);
            return ResponseEntity.ok(UserAdminDTO.from(updatedUser));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 删除用户 (仅 Superadmin)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPERADMIN')") // 确保只有超级管理员能删除
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User user = userService.findUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteUser(id); // 调用 Service 层的删除逻辑
        return ResponseEntity.noContent().build();
    }
}
