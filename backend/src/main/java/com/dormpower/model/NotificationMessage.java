package com.dormpower.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 通知消息模型
 *
 * 用于 Kafka 异步通知处理的传输对象
 *
 * @author dormpower team
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
public class NotificationMessage {

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 通知类型：SYSTEM, ALERT, EMAIL, BILLING, MAINTENANCE */
    private String type;

    /** 优先级：HIGH, NORMAL, LOW */
    private String priority;

    /** 目标用户名 */
    private String username;

    /** 来源 */
    private String source;

    /** 来源 ID */
    private String sourceId;

    /** 时间戳 */
    private Long timestamp = System.currentTimeMillis();

    /**
     * 转换为 Notification 实体
     */
    public Notification toEntity() {
        Notification notification = new Notification();
        notification.setTitle(this.title);
        notification.setContent(this.content);
        notification.setType(this.type);
        notification.setPriority(this.priority != null ? this.priority : "NORMAL");
        notification.setUsername(this.username);
        notification.setSource(this.source != null ? this.source : this.type);
        notification.setSourceId(this.sourceId);
        return notification;
    }
}