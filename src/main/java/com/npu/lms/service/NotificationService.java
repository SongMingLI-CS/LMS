package com.npu.lms.service;

import com.npu.lms.entity.Book;
import com.npu.lms.entity.User;
import com.npu.lms.repository.BookRepository;
import com.npu.lms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int LOW_STOCK_THRESHOLD = 5; // 低库存阈值

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    /**
     * 【新增 V3】低库存检查定时任务
     * 每天早上 8:00 执行
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkAndNotifyLowStock() {
        log.info("【定时任务】开始执行：检查低库存图书...");

        // 1. 查找所有低库存图书
        List<Book> lowStockBooks = bookRepository.findByAvailableLessThan(LOW_STOCK_THRESHOLD);

        if (lowStockBooks.isEmpty()) {
            log.info("【定时任务】未发现低库存图书。");
            return;
        }

        log.info("【定时任务】发现 {} 本低库存图书，准备发送通知...", lowStockBooks.size());

        // 2. 查找所有管理员和超管
        List<User> admins = userRepository.findByRoleIn(List.of("ROLE_ADMIN", "ROLE_SUPERADMIN"));
        List<String> adminEmails = admins.stream()
                .map(User::getEmail)
                .collect(Collectors.toList());

        if (adminEmails.isEmpty()) {
            log.warn("【定时任务】未找到任何管理员邮箱，无法发送通知。");
            return;
        }

        // 3. 构建邮件内容
        String subject = "【LMS 系统】低库存预警通知";
        StringBuilder body = new StringBuilder("LMS 系统提醒您，以下图书库存已低于 " + LOW_STOCK_THRESHOLD + " 本，请及时补充：\n\n");

        for (Book book : lowStockBooks) {
            body.append(String.format("- 《%s》 (ISBN: %s) - 剩余库存: %d\n",
                    book.getTitle(), book.getIsbn(), book.getAvailable()));
        }

        body.append("\n\n此邮件为系统自动发送，请勿回复。");

        // 4. 异步发送邮件
        emailService.sendSimpleEmail(adminEmails, subject, body.toString());
        log.info("【定时任务】低库存预警邮件已发送给 {} 位管理员。", adminEmails.size());
    }
}