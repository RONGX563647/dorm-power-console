package com.dormpower.service;

import com.dormpower.model.MessageTemplate;
import com.dormpower.repository.MessageTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息模板服务
 */
@Service
public class MessageTemplateService {

    @Autowired
    private MessageTemplateRepository templateRepository;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    /**
     * 创建消息模板
     */
    public MessageTemplate createTemplate(MessageTemplate template) {
        if (templateRepository.existsByTemplateCode(template.getTemplateCode())) {
            throw new RuntimeException("Template code already exists: " + template.getTemplateCode());
        }

        template.setCreatedAt(System.currentTimeMillis() / 1000);
        template.setUpdatedAt(System.currentTimeMillis() / 1000);

        return templateRepository.save(template);
    }

    /**
     * 更新消息模板
     */
    public MessageTemplate updateTemplate(Long id, MessageTemplate template) {
        MessageTemplate existing = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));

        existing.setName(template.getName());
        existing.setSubject(template.getSubject());
        existing.setContent(template.getContent());
        existing.setHtmlContent(template.getHtmlContent());
        existing.setChannel(template.getChannel());
        existing.setVariables(template.getVariables());
        existing.setEnabled(template.isEnabled());
        existing.setUpdatedAt(System.currentTimeMillis() / 1000);

        return templateRepository.save(existing);
    }

    /**
     * 删除消息模板
     */
    public void deleteTemplate(Long id) {
        MessageTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));

        if (template.isSystem()) {
            throw new RuntimeException("Cannot delete system template: " + template.getTemplateCode());
        }

        templateRepository.deleteById(id);
    }

    /**
     * 根据编码获取模板
     */
    public Optional<MessageTemplate> getTemplateByCode(String templateCode) {
        return templateRepository.findByTemplateCodeAndEnabledTrue(templateCode);
    }

    /**
     * 根据类型获取模板列表
     */
    public List<MessageTemplate> getTemplatesByType(String type) {
        return templateRepository.findByTypeAndEnabledTrueOrderByCreatedAtDesc(type);
    }

    /**
     * 根据渠道获取模板列表
     */
    public List<MessageTemplate> getTemplatesByChannel(String channel) {
        return templateRepository.findByChannelOrderByCreatedAtDesc(channel);
    }

    /**
     * 获取所有启用的模板
     */
    public List<MessageTemplate> getAllEnabledTemplates() {
        return templateRepository.findByEnabledTrueOrderByCreatedAtDesc();
    }

    /**
     * 渲染模板内容
     */
    public String renderTemplate(String templateCode, Map<String, Object> variables) {
        MessageTemplate template = templateRepository.findByTemplateCodeAndEnabledTrue(templateCode)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateCode));

        return renderContent(template.getContent(), variables);
    }

    /**
     * 渲染模板主题
     */
    public String renderSubject(String templateCode, Map<String, Object> variables) {
        MessageTemplate template = templateRepository.findByTemplateCodeAndEnabledTrue(templateCode)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateCode));

        return renderContent(template.getSubject(), variables);
    }

    /**
     * 渲染HTML内容
     */
    public String renderHtmlContent(String templateCode, Map<String, Object> variables) {
        MessageTemplate template = templateRepository.findByTemplateCodeAndEnabledTrue(templateCode)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateCode));

        String htmlContent = template.getHtmlContent();
        if (htmlContent == null || htmlContent.isEmpty()) {
            return renderContent(template.getContent(), variables);
        }
        return renderContent(htmlContent, variables);
    }

    private String renderContent(String content, Map<String, Object> variables) {
        if (content == null) {
            return "";
        }

        if (variables == null) {
            variables = new HashMap<>();
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(content);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 提取模板变量
     */
    public List<String> extractVariables(String content) {
        List<String> variables = new java.util.ArrayList<>();
        if (content == null) {
            return variables;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (!variables.contains(variableName)) {
                variables.add(variableName);
            }
        }

        return variables;
    }

    /**
     * 初始化系统模板
     */
    public void initSystemTemplates() {
        if (templateRepository.count() > 0) {
            return;
        }

        createSystemTemplate("BILL_DUE_REMINDER", "BILLING", "账单到期提醒",
            "电费账单到期提醒 - ${roomNumber}",
            "尊敬的用户：\n\n您的房间 ${roomNumber} 电费账单将于 ${dueDate} 到期，请及时缴费。\n\n账单金额：${amount} 元\n用电量：${consumption} 度\n\n请登录系统查看详情。",
            null, "EMAIL,SYSTEM", "roomNumber,dueDate,amount,consumption");

        createSystemTemplate("LOW_BALANCE_ALERT", "BILLING", "余额不足预警",
            "电费余额不足预警 - ${roomNumber}",
            "尊敬的用户：\n\n您的房间 ${roomNumber} 电费余额已不足 ${balance} 元，请及时充值，以免影响正常用电。\n\n当前余额：${balance} 元",
            null, "EMAIL,SYSTEM,SMS", "roomNumber,balance");

        createSystemTemplate("DEVICE_OFFLINE_ALERT", "DEVICE", "设备离线告警",
            "设备离线告警 - ${deviceName}",
            "设备 ${deviceName} (${deviceId}) 已离线，请检查设备状态。\n\n离线时间：${offlineTime}",
            null, "EMAIL,SYSTEM", "deviceName,deviceId,offlineTime");

        createSystemTemplate("DEVICE_ALERT", "DEVICE", "设备异常告警",
            "设备异常告警 - ${deviceName}",
            "设备 ${deviceName} 检测到异常：\n\n告警类型：${alertType}\n告警级别：${alertLevel}\n告警信息：${message}\n\n请及时处理。",
            null, "EMAIL,SYSTEM", "deviceName,alertType,alertLevel,message");

        createSystemTemplate("PAYMENT_SUCCESS", "BILLING", "缴费成功通知",
            "电费缴费成功 - ${roomNumber}",
            "尊敬的用户：\n\n您已成功缴纳电费 ${amount} 元。\n\n房间：${roomNumber}\n缴费时间：${paymentTime}\n当前余额：${balance} 元\n\n感谢您的使用。",
            null, "EMAIL,SYSTEM,SMS", "roomNumber,amount,paymentTime,balance");

        createSystemTemplate("COLLECTION_NOTICE", "BILLING", "催缴通知",
            "电费催缴通知 - ${roomNumber}",
            "尊敬的用户：\n\n您的房间 ${roomNumber} 电费账单已逾期，请及时缴费。\n\n账单周期：${period}\n应缴金额：${amount} 元\n逾期天数：${overdueDays} 天\n\n请尽快缴费，以免影响正常用电。",
            null, "EMAIL,SYSTEM,SMS", "roomNumber,period,amount,overdueDays");

        createSystemTemplate("SYSTEM_ANNOUNCEMENT", "SYSTEM", "系统公告",
            "${title}",
            "${content}",
            null, "EMAIL,SYSTEM", "title,content");

        createSystemTemplate("PASSWORD_RESET", "AUTH", "密码重置",
            "密码重置验证码",
            "尊敬的用户：\n\n您的密码重置验证码为：${code}\n\n验证码有效期为 ${expireMinutes} 分钟，请勿泄露给他人。",
            null, "EMAIL,SMS", "code,expireMinutes");
    }

    private void createSystemTemplate(String code, String type, String name,
                                       String subject, String content, String htmlContent,
                                       String channel, String variables) {
        MessageTemplate template = new MessageTemplate(code, type, name);
        template.setSubject(subject);
        template.setContent(content);
        template.setHtmlContent(htmlContent);
        template.setChannel(channel);
        template.setVariables(variables);
        template.setSystem(true);
        template.setEnabled(true);
        templateRepository.save(template);
    }
}
