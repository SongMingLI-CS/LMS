package com.npu.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async; // 引入异步注解
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // 从 application.properties 中读取“发件人”邮箱地址
    @Value("${spring.mail.username}")
    private String fromEmailAddress;

    /**
     * 发送一封简单的文本邮件
     * * @param toEmail 目标邮箱地址
     * @param subject 邮件主题
     * @param body    邮件正文 (包含验证码)
     */
    @Async // 【重要】使用 @Async 使邮件发送在后台线程执行，避免阻塞注册 API
    public void sendSimpleEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmailAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
        } catch (Exception e) {
            // 在生产环境中，这里应该有更健壮的错误处理
            System.err.println("发送邮件失败: " + e.getMessage());
        }
    }
}