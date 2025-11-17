package com.npu.lms.controller;

import com.npu.lms.entity.BorrowRecord;
import com.npu.lms.entity.User;
import com.npu.lms.service.RecordService;
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

    // 获取当前用户 (或全部) 的记录
    @GetMapping
    public ResponseEntity<?> getMyRecords(@AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(recordService.getRecordsForUser(user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
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
}