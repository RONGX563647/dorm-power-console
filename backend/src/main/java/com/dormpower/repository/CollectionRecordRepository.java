package com.dormpower.repository;

import com.dormpower.model.CollectionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollectionRecordRepository extends JpaRepository<CollectionRecord, Long> {

    List<CollectionRecord> findByRoomIdOrderByCreatedAtDesc(String roomId);

    List<CollectionRecord> findByBillIdOrderByCreatedAtDesc(String billId);

    Page<CollectionRecord> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<CollectionRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT c FROM CollectionRecord c WHERE c.status = 'PENDING' AND c.scheduledTs <= :now ORDER BY c.scheduledTs ASC")
    List<CollectionRecord> findPendingRecords(long now);

    @Query("SELECT c FROM CollectionRecord c WHERE c.status = 'FAILED' AND c.retryCount < c.maxRetry ORDER BY c.createdAt ASC")
    List<CollectionRecord> findRetryableRecords();

    long countByBillIdAndStatus(String billId, String status);

    @Modifying
    @Query("UPDATE CollectionRecord c SET c.status = :status, c.updatedAt = :now WHERE c.id = :id")
    int updateStatus(Long id, String status, long now);

    long deleteByCreatedAtBefore(long ts);
}
