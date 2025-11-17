package com.npu.lms.controller;

import com.npu.lms.entity.User;
import com.npu.lms.service.RecordService;
// 1. 引入所有必需的 DTO
import com.npu.lms.dto.RecordDTO;
import com.npu.lms.dto.PopularBookDTO;
import com.npu.lms.dto.ActiveUserDTO;
import com.npu.lms.dto.PeakTimeDTO;
import com.npu.lms.dto.OverdueUserDTO; // 引入新 DTO

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

    // --- (业务接口) ---

    // 明确返回 List<RecordDTO>
    @GetMapping
    public ResponseEntity<List<RecordDTO>> getMyRecords(@AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(recordService.getRecordsForUser(user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

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

    // --- 【数据分析接口 (4 个)】 ---

    // 热门图书 Top 5
    @GetMapping("/analysis/popular-books")
    public ResponseEntity<List<PopularBookDTO>> getPopularBooks() {
        return ResponseEntity.ok(recordService.getTopPopularBooks());
    }

    // 活跃读者 Top 5
    @GetMapping("/analysis/active-users")
    public ResponseEntity<List<ActiveUserDTO>> getActiveUsers() {
        return ResponseEntity.ok(recordService.getTopActiveUsers());
    }

    // 高峰时段
    @GetMapping("/analysis/peak-times")
    public ResponseEntity<List<PeakTimeDTO>> getPeakTimes() {
        return ResponseEntity.ok(recordService.getPeakBorrowingTimes());
    }

    // 【新增】逾期读者
    @GetMapping("/analysis/overdue-users")
    public ResponseEntity<List<OverdueUserDTO>> getOverdueUsers() {
        return ResponseEntity.ok(recordService.getOverdueUsers());
    }
}