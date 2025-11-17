package com.npu.lms.service;

import com.npu.lms.entity.Book;
import com.npu.lms.entity.BorrowRecord;
import com.npu.lms.entity.User;
import com.npu.lms.repository.BookRepository;
import com.npu.lms.repository.BorrowRecordRepository;
import com.npu.lms.repository.UserRepository;
// 1. 引入所有必需的 DTO
import com.npu.lms.dto.RecordDTO;
import com.npu.lms.dto.PopularBookDTO;
import com.npu.lms.dto.ActiveUserDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest; // 引入 PageRequest
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors; // 引入 Collectors

@Service
public class RecordService {

    @Autowired
    private BorrowRecordRepository recordRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    private final int MAX_BORROW_LIMIT = 5; // 最大借阅数量

    // --- 【修正：返回 DTO 列表】 ---
    // 获取记录 (根据用户角色)
    public List<RecordDTO> getRecordsForUser(User user) {
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }

        List<BorrowRecord> rawRecords;
        if ("USER".equals(user.getRole())) {
            rawRecords = recordRepository.findByUserId(user.getId());
        } else {
            // ADMIN 或 SUPERADMIN 返回所有记录
            rawRecords = recordRepository.findAll();
        }

        // --- 核心修复逻辑：将实体映射为包含名称的 DTO ---
        return rawRecords.stream().map(record -> {
            RecordDTO dto = new RecordDTO();

            // 确保关联对象不为空 (防止空指针异常)
            Book book = record.getBook();
            User recordUser = record.getUser();

            // 1. 填充 DTO 的 ID 和日期等原始字段
            dto.setId(record.getId());
            dto.setBorrowDate(record.getBorrowDate());
            dto.setDueDate(record.getDueDate());
            dto.setStatus(record.getStatus());

            // 2. 填充关联名称 (BookTitle 和 UserName)
            dto.setBookTitle(book != null ? book.getTitle() : "图书已删除");
            dto.setUserName(recordUser != null ? recordUser.getName() : "未知用户");

            // 3. 填充 ID (方便前端操作)
            dto.setBookId(book != null ? book.getId() : null);
            dto.setUserId(recordUser != null ? recordUser.getId() : null);

            return dto;
        }).collect(Collectors.toList());
    }

    // --- (以下是您原有的完整业务逻辑) ---

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

    // --- 【新增：数据分析方法】 ---

    // 6. 获取热门图书 Top 5
    public List<PopularBookDTO> getTopPopularBooks() {
        return recordRepository.findTop5PopularBooks(PageRequest.of(0, 5));
    }

    // 7. 获取活跃读者 Top 5
    public List<ActiveUserDTO> getTopActiveUsers() {
        return recordRepository.findTop5ActiveUsers(PageRequest.of(0, 5));
    }
}