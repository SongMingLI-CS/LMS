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
import com.npu.lms.dto.PeakTimeDTO;
import com.npu.lms.dto.OverdueUserDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional; // 引入 Optional
import java.util.stream.Collectors;
import java.math.BigInteger;

@Service
public class RecordService {

    @Autowired
    private BorrowRecordRepository recordRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    // --- 【核心修改：注入动态配置服务】 ---
    @Autowired
    private SystemConfigService configService;

    // --- (状态常量) ---
    private static final String STATUS_BORROWED = "borrowed";
    private static final String STATUS_RETURNED = "returned";
    private static final String STATUS_RESERVED = "reserved";
    private static final String STATUS_AWAITING_PICKUP = "awaiting_pickup";
    private static final String STATUS_EXPIRED = "expired";


    // --- 【方法 1：获取记录 (DTO 映射)】 ---
    public List<RecordDTO> getRecordsForUser(User user) {
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        List<BorrowRecord> rawRecords;
        // 根据角色获取记录
        if ("USER".equals(user.getRole())) {
            rawRecords = recordRepository.findByUserId(user.getId());
        } else {
            rawRecords = recordRepository.findAll();
        }

        // 将实体列表转换为 DTO 列表
        return rawRecords.stream().map(record -> {
            RecordDTO dto = new RecordDTO();
            Book book = record.getBook();
            User recordUser = record.getUser();

            dto.setId(record.getId());
            // 确保 borrowTime 在 DTO 映射时进行转换和非空检查
            if (record.getBorrowTime() != null) {
                dto.setBorrowDate(record.getBorrowTime().toLocalDate());
            }
            dto.setDueDate(record.getDueDate());
            dto.setStatus(record.getStatus());
            dto.setBookTitle(book != null ? book.getTitle() : "图书已删除");
            dto.setUserName(recordUser != null ? recordUser.getName() : "未知用户");
            dto.setBookId(book != null ? book.getId() : null);
            dto.setUserId(recordUser != null ? recordUser.getId() : null);

            return dto;
        }).collect(Collectors.toList());
    }

    // --- 【方法 2：借书 (使用动态配置)】 ---
    @Transactional
    public BorrowRecord borrowBook(Long bookId, User user) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("未找到图书"));

        // 1. 检查是否为“等待取书”状态
        Optional<BorrowRecord> pickupRecordOpt = recordRepository.findByBookIdAndUserIdAndStatus(bookId, user.getId(), STATUS_AWAITING_PICKUP);

        int maxBorrowLimit = configService.getMaxBorrowLimit(); // 动态获取

        if (pickupRecordOpt.isPresent()) {
            // 1a. 用户是来取预约的书
            BorrowRecord record = pickupRecordOpt.get();

            long currentBorrows = recordRepository.findByUserIdAndStatus(user.getId(), STATUS_BORROWED).size();
            if (currentBorrows >= maxBorrowLimit) {
                throw new RuntimeException("借阅失败：已达到最大借阅数量 (" + maxBorrowLimit + "本)");
            }

            record.setStatus(STATUS_BORROWED);
            record.setBorrowTime(LocalDateTime.now());
            record.setDueDate(LocalDate.now().plusDays(configService.getLoanPeriodDays()));
            record.setReservationExpiryDate(null);
            return recordRepository.save(record);
        }

        // 2. 正常借阅流程
        if (book.getAvailable() <= 0) {
            throw new RuntimeException("借阅失败：图书已借完 (或已被预约)");
        }

        long currentBorrows = recordRepository.findByUserIdAndStatus(user.getId(), STATUS_BORROWED).size();
        if (currentBorrows >= maxBorrowLimit) {
            throw new RuntimeException("借阅失败：已达到最大借阅数量 (" + maxBorrowLimit + "本)");
        }
        if (recordRepository.findByBookIdAndUserIdAndStatus(bookId, user.getId(), STATUS_BORROWED).isPresent()) {
            throw new RuntimeException("借阅失败：您已借阅此书");
        }

        book.setAvailable(book.getAvailable() - 1);
        bookRepository.save(book);
        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBook(book);
        record.setBorrowTime(LocalDateTime.now());
        record.setDueDate(LocalDate.now().plusDays(configService.getLoanPeriodDays()));
        record.setStatus(STATUS_BORROWED);
        return recordRepository.save(record);
    }

    // --- 【方法 3：预约】 ---
    @Transactional
    public BorrowRecord reserveBook(Long bookId, User user) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("未找到图书"));
        if (book.getAvailable() > 0) {
            throw new RuntimeException("预约失败：该书尚有库存，请直接借阅");
        }
        if (recordRepository.findByBookIdAndUserIdAndStatus(bookId, user.getId(), STATUS_BORROWED).isPresent() ||
                recordRepository.findByBookIdAndUserIdAndStatus(bookId, user.getId(), STATUS_RESERVED).isPresent()) {
            throw new RuntimeException("预约失败：您已借阅或预约了此书");
        }
        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBook(book);
        record.setBorrowTime(LocalDateTime.now()); // 记录预约时间
        record.setStatus(STATUS_RESERVED);
        return recordRepository.save(record);
    }

    // --- 【方法 4：还书 (使用动态配置)】 ---
    @Transactional
    public BorrowRecord returnBook(String bookIdentifier, String userId) {
        Long bookId = Long.parseLong(bookIdentifier);
        BorrowRecord record;

        if (userId != null && !userId.isEmpty()) {
            User user = userRepository.findByUsername(userId).orElse(null);
            if (user == null) {
                user = userRepository.findById(Long.parseLong(userId)).orElseThrow(() -> new RuntimeException("未找到指定用户"));
            }
            record = recordRepository.findByBookIdAndUserIdAndStatus(bookId, user.getId(), STATUS_BORROWED)
                    .orElseThrow(() -> new RuntimeException("归还失败：未找到该用户的借阅记录"));
        } else {
            record = recordRepository.findByBookIdAndStatus(bookId, STATUS_BORROWED).stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("归还失败：未找到该书的借阅记录"));
        }

        record.setStatus(STATUS_RETURNED);
        record.setReturnTime(LocalDateTime.now());
        recordRepository.save(record);
        Book book = record.getBook();

        // 检查预约队列
        List<BorrowRecord> pendingReservations = recordRepository.findByBookIdAndStatusOrderByBorrowTimeAsc(book.getId(), STATUS_RESERVED);

        if (!pendingReservations.isEmpty()) {
            BorrowRecord nextReservation = pendingReservations.get(0);
            nextReservation.setStatus(STATUS_AWAITING_PICKUP);

            int expiryDays = configService.getReservationExpiryDays(); // 动态获取
            nextReservation.setReservationExpiryDate(LocalDate.now().plusDays(expiryDays));

            recordRepository.save(nextReservation);
        } else {
            book.setAvailable(book.getAvailable() + 1);
            bookRepository.save(book);
        }
        return record;
    }

    // --- 【方法 5：续借 (使用动态配置)】 ---
    @Transactional
    public BorrowRecord renewBook(Long recordId, User user) {
        BorrowRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("未找到记录"));
        if (!record.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("权限不足，无法续借他人图书");
        }

        int maxRenewalLimit = configService.getMaxRenewalLimit(); // 动态获取

        if (record.getRenewalCount() >= maxRenewalLimit) {
            throw new RuntimeException("续借失败：已达到最大续借次数 (" + maxRenewalLimit + "次)");
        }
        if (record.getDueDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("续借失败：图书已逾期，请先归还");
        }

        record.setRenewalCount(record.getRenewalCount() + 1);
        record.setDueDate(record.getDueDate().plusDays(configService.getLoanPeriodDays()));
        return recordRepository.save(record);
    }

    // --- 【方法 6：处理预约】 ---
    @Transactional
    public BorrowRecord processReservation(Long reservationId) {
        BorrowRecord reservation = recordRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("未找到预约记录"));
        if (!STATUS_RESERVED.equals(reservation.getStatus())) {
            throw new RuntimeException("该记录不是预约状态");
        }
        Book book = reservation.getBook();
        if (book.getAvailable() <= 0) {
            throw new RuntimeException("处理失败：图书库存不足，无法将预约转为借阅");
        }
        book.setAvailable(book.getAvailable() - 1);
        bookRepository.save(book);
        reservation.setStatus(STATUS_BORROWED);
        reservation.setBorrowTime(LocalDateTime.now());
        reservation.setDueDate(LocalDate.now().plusDays(configService.getLoanPeriodDays()));
        return recordRepository.save(reservation);
    }

    // --- 【数据分析方法】 ---

    // 7. 热门图书 Top 5
    public List<PopularBookDTO> getTopPopularBooks() {
        return recordRepository.findTop5PopularBooks(PageRequest.of(0, 5));
    }

    // 8. 活跃读者 Top 5
    public List<ActiveUserDTO> getTopActiveUsers() {
        return recordRepository.findTop5ActiveUsers(PageRequest.of(0, 5));
    }

    // 9. 借阅高峰时段
    public List<PeakTimeDTO> getPeakBorrowingTimes() {
        List<Object[]> results = recordRepository.getPeakBorrowingTimesNative();

        // 核心修正：使用 Number 进行安全的类型转换
        return results.stream()
                .map(result -> new PeakTimeDTO(
                        (String) result[0],              // hourSlot
                        ((Number) result[1]).longValue() // count (避免 ClassCastException)
                ))
                .collect(Collectors.toList());
    }

    // 10. 逾期读者
    public List<OverdueUserDTO> getOverdueUsers() {
        return recordRepository.findOverdueUsers();
    }
}