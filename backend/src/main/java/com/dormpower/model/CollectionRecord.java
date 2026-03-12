package com.dormpower.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * 催缴记录实体
 */
@Entity
@Table(name = "collection_record", indexes = {
    @Index(name = "idx_collection_room", columnList = "roomId"),
    @Index(name = "idx_collection_bill", columnList = "billId"),
    @Index(name = "idx_collection_ts", columnList = "createdAt")
})
public class CollectionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String roomId;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String billId;

    @Column(length = 50)
    private String studentId;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String type;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String channel;

    @Column(length = 200)
    private String recipient;

    @Column(columnDefinition = "TEXT")
    private String content;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 500)
    private String errorMessage;

    private int retryCount = 0;

    private int maxRetry = 3;

    private long scheduledTs;

    private long sentTs;

    private long createdAt;

    private long updatedAt;

    public CollectionRecord() {
        long now = System.currentTimeMillis() / 1000;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public long getScheduledTs() {
        return scheduledTs;
    }

    public void setScheduledTs(long scheduledTs) {
        this.scheduledTs = scheduledTs;
    }

    public long getSentTs() {
        return sentTs;
    }

    public void setSentTs(long sentTs) {
        this.sentTs = sentTs;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
