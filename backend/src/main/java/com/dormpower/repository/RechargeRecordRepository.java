package com.dormpower.repository;

import com.dormpower.model.RechargeRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 充值记录Repository
 */
@Repository
public interface RechargeRecordRepository extends JpaRepository<RechargeRecord, String> {

    /**
     * 根据房间ID查询充值记录
     */
    Page<RechargeRecord> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);

    /**
     * 根据状态查询充值记录
     */
    List<RechargeRecord> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * 根据交易号查询
     */
    RechargeRecord findByTransactionId(String transactionId);
}
