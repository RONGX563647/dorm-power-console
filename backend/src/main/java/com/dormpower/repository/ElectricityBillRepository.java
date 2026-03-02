package com.dormpower.repository;

import com.dormpower.model.ElectricityBill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 电费账单Repository
 */
@Repository
public interface ElectricityBillRepository extends JpaRepository<ElectricityBill, String> {

    /**
     * 根据房间ID查询账单
     */
    Page<ElectricityBill> findByRoomIdOrderByPeriodDesc(String roomId, Pageable pageable);

    /**
     * 根据房间ID和周期查询账单
     */
    Optional<ElectricityBill> findByRoomIdAndPeriod(String roomId, String period);

    /**
     * 根据状态查询账单
     */
    List<ElectricityBill> findByStatus(String status);

    /**
     * 查询房间的未缴费账单
     */
    List<ElectricityBill> findByRoomIdAndStatus(String roomId, String status);

    /**
     * 查询所有未缴费账单
     */
    List<ElectricityBill> findByStatusOrderByPeriodDesc(String status);
}
