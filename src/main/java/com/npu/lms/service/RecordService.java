package com.npu.lms.service;

import com.npu.lms.entity.Book;
import com.npu.lms.entity.BorrowRecord;
import com.npu.lms.entity.User;
import com.npu.lms.repository.BookRepository;
import com.npu.lms.repository.BorrowRecordRepository;
import com.npu.lms.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class RecordService {

    @Autowired
    private BorrowRecordRepository recordRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository; // 用于还书时查找用户

    private final int MAX_BORROW_LIMIT = 5; // 最大借阅数量

    // 获取记录 (根据用户角色)
    public List<BorrowRecord> getRecordsForUser(User user) {
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        if ("USER".equals(user.getRole())) {
            return recordRepository.findByUserId(user.getId());
        }
        // ADMIN 或 SUPERADMIN
        return recordRepository.findAll();
    }

    // 1. 借书
    @Transactional
    public BorrowRecord borrowBook(Long bookId, User user) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("未找到图书"));

        // 1. 检查库存
        if (book.getAvailable() <= 0) {
            throw new RuntimeException("借阅失败：图书已借完");
        }

        // 2. 检查用户借阅限制
        long currentBorrows = recordRepository.findByUserIdAndStatus(user.getId(), "borrowed").size();
        if (currentBorrows >= MAX_BORROW_LIMIT) {
            throw new RuntimeException("借阅失败：已达到最大借阅数量 (5本)");
        }

        // 3. 检查是否已借阅此书
        if (recordRepository.findByBookIdAndUserIdAndStatus(bookId, user.getId(), "borrowed").isPresent()) {
            throw new RuntimeException("借阅失败：您已借阅此书");
        }

        // 4. 更新库存
        book.setAvailable(book.getAvailable() - 1);
        bookRepository.save(book);

        // 5. 创建借阅记录
        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBook(book);
        record.setBorrowDate(LocalDate.now());
        record.setDueDate(LocalDate.now().plusDays(30)); // 默认30天
        record.setStatus("borrowed");

        return recordRepository.save(record);
    }

    // 2. 预约
    @Transactional
    public BorrowRecord reserveBook(Long bookId, User user) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("未找到图书"));

        // 1. 检查是否可借阅 (只有不可借阅时才预约)
        if (book.getAvailable() > 0) {
            throw new RuntimeException("预约失败：该书尚有库存，请直接借阅");
        }

        // 2. 检查是否已借阅或预约
        if (recordRepository.findByBookIdAndUserIdAndStatus(bookId, user.getId(), "borrowed").isPresent() ||
                recordRepository.findByBookIdAndUserIdAndStatus(bookId, user.getId(), "reserved").isPresent()) {
            throw new RuntimeException("预约失败：您已借阅或预约了此书");
        }

        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBook(book);
        record.setBorrowDate(LocalDate.now()); // 预约日期
        record.setStatus("reserved");

        return recordRepository.save(record);
    }

    // 3. 还书
    @Transactional
    public BorrowRecord returnBook(String bookIdentifier, String userId) {

        // (假设 bookIdentifier 是 bookId)
        Long bookId = Long.parseLong(bookIdentifier);

        BorrowRecord record;

        if (userId != null && !userId.isEmpty()) {
            // 管理员指定用户还书
            User user = userRepository.findByUsername(userId).orElse(null);
            if (user == null) {
                user = userRepository.findById(Long.parseLong(userId)).orElseThrow(() -> new RuntimeException("未找到指定用户"));
            }
            record = recordRepository.findByBookIdAndUserIdAndStatus(bookId, user.getId(), "borrowed")
                    .orElseThrow(() -> new RuntimeException("归还失败：未找到该用户的借阅记录"));
        } else {
            // 管理员扫码还书 (查找任意一个借阅记录)
            record = recordRepository.findByBookIdAndStatus(bookId, "borrowed").stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("归还失败：未找到该书的借阅记录"));
        }

        // 1. 更新记录
        record.setStatus("returned");
        record.setReturnDate(LocalDate.now());
        recordRepository.save(record);

        // 2. 更新库存
        Book book = record.getBook();
        book.setAvailable(book.getAvailable() + 1);
        bookRepository.save(book);

        // 3. (TODO) 检查并通知该书的预约者 (高级功能)

        return record;
    }

    // 4. 续借
    @Transactional
    public BorrowRecord renewBook(Long recordId, User user) {
        BorrowRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("未找到记录"));

        // 检查是否是本人的记录
        if (!record.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("权限不足，无法续借他人图书");
        }

        // (可以增加续借次数限制等逻辑)

        record.setDueDate(record.getDueDate().plusDays(30)); // 续30天
        return recordRepository.save(record);
    }

    // 5. (Admin) 处理预约
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

        // 1. 更新库存
        book.setAvailable(book.getAvailable() - 1);
        bookRepository.save(book);

        // 2. 更新记录
        reservation.setStatus("borrowed");
        reservation.setBorrowDate(LocalDate.now());
        reservation.setDueDate(LocalDate.now().plusDays(30)); // 借阅期30天

        return recordRepository.save(reservation);
    }
}