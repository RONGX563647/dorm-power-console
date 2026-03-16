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
 * 催缴记录实体
 */
@Entity
@Table(name = "collection_record", indexes = {
    @Index(name = "idx_collection_room", columnList = "roomId"),
    @Index(name = "idx_collection_bill", columnList = "billId"),
    @Index(name = "idx_collection_ts", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
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

    private long createdAt = System.currentTimeMillis() / 1000;

    private long updatedAt = System.currentTimeMillis() / 1000;
}