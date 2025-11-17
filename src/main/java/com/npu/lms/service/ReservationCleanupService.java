package com.npu.lms.service;

import com.npu.lms.entity.Book;
import com.npu.lms.entity.BorrowRecord;
import com.npu.lms.repository.BookRepository;
import com.npu.lms.repository.BorrowRecordRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ReservationCleanupService.class);

    @Autowired
    private BorrowRecordRepository recordRepository;

    @Autowired
    private BookRepository bookRepository;

    // 匹配 RecordService 中的常量
    private static final String STATUS_RESERVED = "reserved";
    private static final String STATUS_AWAITING_PICKUP = "awaiting_pickup";
    private static final String STATUS_EXPIRED = "expired";
    private final int RESERVATION_EXPIRY_DAYS = 3;

    /**
     * 预约过期清理任务
     * 每天凌晨 1:00 执行 (cron = "秒 分 时 日 月 周")
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void cleanupExpiredReservations() {
        log.info("【定时任务】开始执行：清理过期预约...");

        // 1. 查找所有已过期的“等待取书”记录
        List<BorrowRecord> expiredRecords = recordRepository.findByStatusAndReservationExpiryDateBefore(
                STATUS_AWAITING_PICKUP,
                LocalDate.now()
        );

        if (expiredRecords.isEmpty()) {
            log.info("【定时任务】未发现过期预约。");
            return;
        }

        log.info("【定时任务】发现 {} 条过期预约，正在处理...", expiredRecords.size());

        for (BorrowRecord record : expiredRecords) {

            // 2. 将过期的记录状态设为 EXPIRED
            record.setStatus(STATUS_EXPIRED);
            record.setReservationExpiryDate(null); // 清除到期日
            recordRepository.save(record);

            Book book = record.getBook();
            if (book == null) {
                continue; // 如果书被删了，跳过
            }

            // 3. 【V2 核心】处理队列中的下一个预约
            // 查找这本书是否还有其他人在 'reserved' 队列中
            List<BorrowRecord> nextReservations = recordRepository.findByBookIdAndStatusOrderByBorrowTimeAsc(
                    book.getId(),
                    STATUS_RESERVED
            );

            if (!nextReservations.isEmpty()) {
                // 3a. 如果有下一个人预约：激活该预约
                BorrowRecord nextInLine = nextReservations.get(0);

                nextInLine.setStatus(STATUS_AWAITING_PICKUP);
                nextInLine.setReservationExpiryDate(LocalDate.now().plusDays(RESERVATION_EXPIRY_DAYS));

                recordRepository.save(nextInLine);
                log.info("【定时任务】图书 ID: {} 已分配给下一个预约用户 ID: {}", book.getId(), nextInLine.getUser().getId());

            } else {
                // 3b. 如果没有下一个人预约：将图书库存 +1，使其变为可借阅
                book.setAvailable(book.getAvailable() + 1);
                bookRepository.save(book);
                log.info("【定时任务】图书 ID: {} 已无人预约，库存 +1", book.getId());
            }
        }

        log.info("【定时任务】清理完成。");
    }
}