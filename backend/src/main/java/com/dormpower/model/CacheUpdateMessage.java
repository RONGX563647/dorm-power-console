package com.dormpower.model;

/**
 * 缓存更新消息
 * 
 * 用于Kafka消息队列异步更新缓存
 * 
 * @author dormpower team
 * @version 1.0
 */
public class CacheUpdateMessage {

    private String cacheName;
    private String key;
    private Object value;
    private String operation;
    private long timestamp;

    public CacheUpdateMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "CacheUpdateMessage{" +
                "cacheName='" + cacheName + '\'' +
                ", key='" + key + '\'' +
                ", operation='" + operation + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
