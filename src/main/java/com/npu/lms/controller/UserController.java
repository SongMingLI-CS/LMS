package com.npu.lms.controller; // 替换为您的包名

import com.npu.lms.entity.User;
import com.npu.lms.service.UserService;
import com.npu.lms.dto.UserDTO; // <-- NEW
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // MODIFIED: 返回 List<UserDTO>
    @GetMapping
    public List<UserDTO> getAllUsers() {
        return userService.findAllUsersDTO(); // <-- 调用新的 DTO 方法
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        user.setId(null);
        return userService.saveUser(user); // saveUser 方法会处理密码加密
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setName(userDetails.getName());
        user.setUsername(userDetails.getUsername());
        user.setRole(userDetails.getRole());

        // 检查前端是否传入了密码
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(userDetails.getPassword());
        } else {
            // 前端未传入密码 (留空)，保持原密码
            user.setPassword(null); // saveUser 逻辑会处理
        }

        return ResponseEntity.ok(userService.saveUser(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}