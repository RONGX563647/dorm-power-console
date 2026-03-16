package com.dormpower.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 消息模板实体
 */
@Entity
@Table(name = "message_template", indexes = {
    @Index(name = "idx_template_code", columnList = "templateCode", unique = true),
    @Index(name = "idx_template_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
public class MessageTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String templateCode;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String type;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String htmlContent;

    @Column(length = 50)
    private String channel;

    @Column(length = 500)
    private String variables;

    private boolean enabled = true;

    private boolean isSystem = false;

    private long createdAt = System.currentTimeMillis() / 1000;

    private long updatedAt = System.currentTimeMillis() / 1000;

    /**
     * 便捷构造函数
     */
    public MessageTemplate(String templateCode, String type, String name) {
        this.templateCode = templateCode;
        this.type = type;
        this.name = name;
    }
}