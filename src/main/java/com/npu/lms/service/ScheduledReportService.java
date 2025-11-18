package com.npu.lms.service;

import com.npu.lms.entity.User;
import com.npu.lms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource; // 【重要】
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduledReportService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledReportService.class);

    @Autowired
    private AnalysisExportService analysisExportService; // 你上一步创建的

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    // Excel MIME 类型
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * 【V3】月度报表自动生成与发送
     * 每月 1 号凌晨 5:00 执行
     */
    @Scheduled(cron = "0 0 5 1 * ?")
    public void generateAndSendMonthlyReports() {
        log.info("【定时任务】开始执行：生成月度报表...");

        // 1. 查找所有管理员
        List<User> admins = userRepository.findByRoleIn(List.of("ROLE_ADMIN", "ROLE_SUPERADMIN"));
        List<String> adminEmails = admins.stream()
                .map(User::getEmail)
                .collect(Collectors.toList());

        if (adminEmails.isEmpty()) {
            log.warn("【定时任务】未找到任何管理员邮箱，任务终止。");
            return;
        }

        // 2. 准备附件列表
        List<EmailAttachment> attachments = new ArrayList<>();
        try {
            // 生成所有 6 个报表，并转换为 InputStreamSource
            attachments.add(createAttachment("popular_books.xlsx", analysisExportService.exportPopularBooks()));
            attachments.add(createAttachment("active_users.xlsx", analysisExportService.exportActiveUsers()));
            attachments.add(createAttachment("peak_times.xlsx", analysisExportService.exportPeakTimes()));
            attachments.add(createAttachment("categories.xlsx", analysisExportService.exportCategoryStats()));
            attachments.add(createAttachment("overdue_users.xlsx", analysisExportService.exportOverdueUsers()));
            attachments.add(createAttachment("stagnant_books.xlsx", analysisExportService.exportStagnantBooks()));

        } catch (IOException e) {
            log.error("【定时任务】生成报表附件失败", e);
            // 失败时也发送邮件，通知管理员
            sendFailureNotification(adminEmails, e.getMessage());
            return;
        }

        // 3. 准备邮件内容
        String monthName = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String subject = String.format("【LMS 系统】%s 月度数据分析报表", monthName);
        String body = String.format("""
            <html>
            <body>
                <h3>LMS 月度报表</h3>
                <p>您好，</p>
                <p>附件为 LMS 系统 <b>%s</b> 的月度数据分析报表，共 6 份，请查收。</p>
                <p style="color: gray;">此邮件为系统自动发送，请勿回复。</p>
            </body>
            </html>
            """, monthName);

        // 4. 发送邮件
        emailService.sendEmailWithAttachments(adminEmails, subject, body, attachments);
        log.info("【定时任务】月度报表已成功发送至 {} 位管理员。", adminEmails.size());
    }

    /**
     * 辅助方法：将 ByteArrayInputStream 转换为可用于附件的 InputStreamSource
     */
    private EmailAttachment createAttachment(String filename, ByteArrayInputStream bis) throws IOException {
        // ByteArrayResource 是一个完美的 InputStreamSource 实现
        return new EmailAttachment(filename, new ByteArrayResource(bis.readAllBytes()), EXCEL_CONTENT_TYPE);
    }

    /**
     * 辅助方法：在报表生成失败时发送通知
     */
    private void sendFailureNotification(List<String> adminEmails, String errorMessage) {
        String subject = "【LMS 系统】月度报表生成失败";
        String body = String.format("""
            <html>
            <body>
                <h3 style="color: red;">LMS 月度报表生成失败</h3>
                <p>您好，</NUL>
                <p>系统在 %s 尝试自动生成月度报表时遇到错误，未能成功生成附件。</p>
                <p><b>错误详情:</b> %s</p>
                <p>请联系技术支持检查系统日志。</p>
            </body>
            </html>
            """, LocalDate.now(), errorMessage);

        // 注意：这里最后一个参数传 null 或空列表，因为没有附件
        emailService.sendEmailWithAttachments(adminEmails, subject, body, null);
    }
}