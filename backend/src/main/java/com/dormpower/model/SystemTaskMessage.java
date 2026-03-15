package com.dormpower.model;

import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 系统任务消息模型
 *
 * 用于 Kafka 异步系统任务处理的传输对象
 *
 * @author dormpower team
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
public class SystemTaskMessage {

    /** 任务类型：BACKUP, CLEANUP, METRICS, etc. */
    private String taskType;

    /** 任务描述 */
    private String description;

    /** 任务来源 */
    private String source;

    /** 任务参数 */
    private Map<String, Object> parameters;

    /** 时间戳 */
    private Long timestamp = System.currentTimeMillis();

    /**
     * 便捷构造函数（3个参数）
     */
    public SystemTaskMessage(String taskType, String description, Map<String, Object> parameters) {
        this.taskType = taskType;
        this.description = description;
        this.parameters = parameters;
        this.timestamp = System.currentTimeMillis();
    }
}