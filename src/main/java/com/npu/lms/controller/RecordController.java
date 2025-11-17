package com.npu.lms.controller;

import com.npu.lms.entity.User;
import com.npu.lms.service.RecordService;
// 1. 引入所有必需的 DTO
import com.npu.lms.dto.RecordDTO;
import com.npu.lms.dto.PopularBookDTO;
import com.npu.lms.dto.ActiveUserDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    @Autowired
    private RecordService recordService;

    // 2. 【修改】明确返回 List<RecordDTO>
    //    确保前端能收到包含书名和用户名的借阅记录
    @GetMapping
    public ResponseEntity<List<RecordDTO>> getMyRecords(@AuthenticationPrincipal User user) {
        // (您原来的 try-catch 逻辑可以保留，但 Service 返回类型已更改)
        try {
            return ResponseEntity.ok(recordService.getRecordsForUser(user));
        } catch (RuntimeException e) {
            // 如果 user 为 null 或发生错误，返回错误信息
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // --- (借书、预约、还书等方法保持不变) ---

    // 借书
    @PostMapping("/borrow")
    public ResponseEntity<?> borrowBook(@RequestBody Map<String, Long> payload, @AuthenticationPrincipal User user) {
        try {
            Long bookId = payload.get("bookId");
            return ResponseEntity.ok(recordService.borrowBook(bookId, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 预约
    @PostMapping("/reserve")
    public ResponseEntity<?> reserveBook(@RequestBody Map<String, Long> payload, @AuthenticationPrincipal User user) {
        try {
            Long bookId = payload.get("bookId");
            return ResponseEntity.ok(recordService.reserveBook(bookId, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 续借
    @PutMapping("/renew/{recordId}")
    public ResponseEntity<?> renewBook(@PathVariable Long recordId, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(recordService.renewBook(recordId, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 还书 (Admin)
    @PostMapping("/return")
    public ResponseEntity<?> returnBook(@RequestBody Map<String, String> payload) {
        try {
            String bookIdentifier = payload.get("bookIdentifier");
            String userId = payload.get("userId"); // 可选
            return ResponseEntity.ok(recordService.returnBook(bookIdentifier, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 处理预约 (Admin)
    @PostMapping("/process-reservation/{reservationId}")
    public ResponseEntity<?> processReservation(@PathVariable Long reservationId) {
        try {
            return ResponseEntity.ok(recordService.processReservation(reservationId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // --- 3. 【修改】数据分析接口 ---

    // 获取热门图书 Top 5 (返回 PopularBookDTO)
    @GetMapping("/analysis/popular-books")
    public ResponseEntity<List<PopularBookDTO>> getPopularBooks() {
        return ResponseEntity.ok(recordService.getTopPopularBooks());
    }

    // 获取活跃读者 Top 5 (返回 ActiveUserDTO)
    @GetMapping("/analysis/active-users")
    public ResponseEntity<List<ActiveUserDTO>> getActiveUsers() {
        return ResponseEntity.ok(recordService.getTopActiveUsers());
    }
}