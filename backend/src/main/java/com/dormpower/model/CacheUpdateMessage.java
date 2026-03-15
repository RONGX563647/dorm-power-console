package com.dormpower.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 缓存更新消息
 *
 * 用于Kafka消息队列异步更新缓存
 * 支持多节点部署下的缓存一致性
 *
 * @author dormpower team
 * @version 2.0
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class CacheUpdateMessage {

    private String cacheName;
    private String key;
    private Object value;
    private String operation;
    private long timestamp = System.currentTimeMillis();

    private String nodeId;
    private boolean broadcast = true;
}