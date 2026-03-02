package com.dormpower.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * 邮件服务类
 */
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private SystemConfigService systemConfigService;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    /**
     * 发送简单邮件
     */
    public void sendSimpleEmail(String to, String subject, String content) {
        if (!isEmailEnabled() || mailSender == null) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);
    }

    /**
     * 发送HTML邮件
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        if (!isEmailEnabled() || mailSender == null) {
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * 发送告警邮件
     */
    public void sendAlertEmail(String to, String deviceName, String alertType, String message) {
        String subject = "【告警】宿舍电源管理系统 - " + deviceName;
        String content = String.format(
            "设备名称: %s\n告警类型: %s\n告警信息: %s\n\n请及时处理。",
            deviceName, alertType, message
        );
        sendSimpleEmail(to, subject, content);
    }

    /**
     * 发送系统通知邮件
     */
    public void sendSystemNotificationEmail(String to, String title, String content) {
        String subject = "【系统通知】宿舍电源管理系统 - " + title;
        sendSimpleEmail(to, subject, content);
    }

    /**
     * 检查邮件是否启用
     */
    private boolean isEmailEnabled() {
        String enabled = systemConfigService.getConfigValue("email.enabled", "false");
        return "true".equalsIgnoreCase(enabled);
    }
}
