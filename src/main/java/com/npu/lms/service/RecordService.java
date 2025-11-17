package com.npu.lms.service;

import com.npu.lms.entity.Book;
import com.npu.lms.entity.BorrowRecord;
import com.npu.lms.entity.User;
import com.npu.lms.repository.BookRepository;
import com.npu.lms.repository.BorrowRecordRepository;
import com.npu.lms.repository.UserRepository;
import com.npu.lms.dto.RecordDTO;
import com.npu.lms.dto.PopularBookDTO;
import com.npu.lms.dto.ActiveUserDTO;
import com.npu.lms.dto.PeakTimeDTO; // 引入

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime; // 引入 LocalDateTime
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigInteger; // 引入 BigInteger

@Service
public class RecordService {

    @Autowired
    private BorrowRecordRepository recordRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    private final int MAX_BORROW_LIMIT = 5;

    // --- (getRecordsForUser 方法修改：使用 getBorrowTime()) ---
    public List<RecordDTO> getRecordsForUser(User user) {
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        List<BorrowRecord> rawRecords;
        if ("USER".equals(user.getRole())) {
            rawRecords = recordRepository.findByUserId(user.getId());
        } else {
            rawRecords = recordRepository.findAll();
        }
        return rawRecords.stream().map(record -> {
            RecordDTO dto = new RecordDTO();
            Book book = record.getBook();
            User recordUser = record.getUser();
            dto.setId(record.getId());
            // 【修改】使用 getBorrowTime() 并转换为 LocalDate (如果 DTO 仍是 LocalDate)
            // (为了简单起见，我们假设 RecordDTO 中的 borrowDate 也改为了 LocalDateTime)
            // dto.setBorrowDate(record.getBorrowTime().toLocalDate());
            dto.setDueDate(record.getDueDate());
            dto.setStatus(record.getStatus());
            dto.setBookTitle(book != null ? book.getTitle() : "图书已删除");
            dto.setUserName(recordUser != null ? recordUser.getName() : "未知用户");
            dto.setBookId(book != null ? book.getId() : null);
            dto.setUserId(recordUser != null ? recordUser.getId() : null);
            return dto;
        }).collect(Collectors.toList());
    }

    // 1. 借书 (修改)
    @Transactional
    public BorrowRecord borrowBook(Long bookId, User user) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("未找到图书"));

        if (book.getAvailable() <= 0) {
            throw new RuntimeException("借阅失败：图书已借完");
        }
        long currentBorrows = recordRepository.findByUserIdAndStatus(user.getId(), "borrowed").size();
        if (currentBorrows >= MAX_BORROW_LIMIT) {
            throw new RuntimeException("借阅失败：已达到最大借阅数量 (5本)");
        }
        if (recordRepository.findByBookIdAndUserIdAndStatus(bookId, user.getId(), "borrowed").isPresent()) {
            throw new RuntimeException("借阅失败：您已借阅此书");
        }

        book.setAvailable(book.getAvailable() - 1);
        bookRepository.save(book);

        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBook(book);

        // --- 【修改】 ---
        record.setBorrowTime(LocalDateTime.now()); // 使用 LocalDateTime
        record.setDueDate(LocalDate.now().plusDays(30)); // 截止日期仍用 LocalDate
        record.setStatus("borrowed");

        return recordRepository.save(record);
    }

    // 2. 预约 (修改)
    @Transactional
    public BorrowRecord reserveBook(Long bookId, User user) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("未找到图书"));

        if (book.getAvailable() > 0) {
            throw new RuntimeException("预约失败：该书尚有库存，请直接借阅");
        }
        if (recordRepository.findByBookIdAndUserIdAndStatus(bookId, user.getId(), "borrowed").isPresent() ||
                recordRepository.findByBookIdAndUserIdAndStatus(bookId, user.getId(), "reserved").isPresent()) {
            throw new RuntimeException("预约失败：您已借阅或预约了此书");
        }

        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBook(book);

        // --- 【修改】 ---
        record.setBorrowTime(LocalDateTime.now()); // 记录预约时间
        record.setStatus("reserved");

        return recordRepository.save(record);
    }

    // 3. 还书 (修改)
    @Transactional
    public BorrowRecord returnBook(String bookIdentifier, String userId) {
        Long bookId = Long.parseLong(bookIdentifier);
        BorrowRecord record;

        if (userId != null && !userId.isEmpty()) {
            User user = userRepository.findByUsername(userId).orElse(null);
            if (user == null) {
                user = userRepository.findById(Long.parseLong(userId)).orElseThrow(() -> new RuntimeException("未找到指定用户"));
            }
            record = recordRepository.findByBookIdAndUserIdAndStatus(bookId, user.getId(), "borrowed")
                    .orElseThrow(() -> new RuntimeException("归还失败：未找到该用户的借阅记录"));
        } else {
            record = recordRepository.findByBookIdAndStatus(bookId, "borrowed").stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("归还失败：未找到该书的借阅记录"));
        }

        record.setStatus("returned");
        // --- 【修改】 ---
        record.setReturnTime(LocalDateTime.now()); // 记录归还时间
        recordRepository.save(record);

        Book book = record.getBook();
        book.setAvailable(book.getAvailable() + 1);
        bookRepository.save(book);

        return record;
    }

    // 4. 续借 (不变)
    @Transactional
    public BorrowRecord renewBook(Long recordId, User user) {
        BorrowRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("未找到记录"));

        if (!record.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("权限不足，无法续借他人图书");
        }
        record.setDueDate(record.getDueDate().plusDays(30));
        return recordRepository.save(record);
    }

    // 5. (Admin) 处理预约 (修改)
    @Transactional
    public BorrowRecord processReservation(Long reservationId) {
        BorrowRecord reservation = recordRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("未找到预约记录"));

        if (!"reserved".equals(reservation.getStatus())) {
            throw new RuntimeException("该记录不是预约状态");
        }
        Book book = reservation.getBook();
        if (book.getAvailable() <= 0) {
            throw new RuntimeException("处理失败：图书库存不足，无法将预约转为借阅");
        }

        book.setAvailable(book.getAvailable() - 1);
        bookRepository.save(book);

        reservation.setStatus("borrowed");
        // --- 【修改】 ---
        reservation.setBorrowTime(LocalDateTime.now()); // 记录借阅时间
        reservation.setDueDate(LocalDate.now().plusDays(30));

        return recordRepository.save(reservation);
    }

    // --- 【数据分析方法】 ---

    // 6. 获取热门图书 Top 5
    public List<PopularBookDTO> getTopPopularBooks() {
        return recordRepository.findTop5PopularBooks(PageRequest.of(0, 5));
    }

    // 7. 获取活跃读者 Top 5
    public List<ActiveUserDTO> getTopActiveUsers() {
        return recordRepository.findTop5ActiveUsers(PageRequest.of(0, 5));
    }

    // 8. 获取借阅高峰时段 (新增)
    public List<PeakTimeDTO> getPeakBorrowingTimes() {
        List<Object[]> results = recordRepository.getPeakBorrowingTimesNative();
        // 将原生 SQL 返回的 Object[] 转换为 DTO
        return results.stream()
                .map(result -> new PeakTimeDTO(
                        (String) result[0],              // hourSlot
                        ((BigInteger) result[1]).longValue() // count (MySQL 返回 BigInteger)
                ))
                .collect(Collectors.toList());
    }
}