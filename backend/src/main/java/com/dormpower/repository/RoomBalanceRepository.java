package com.dormpower.repository;

import com.dormpower.model.RoomBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 房间余额Repository
 */
@Repository
public interface RoomBalanceRepository extends JpaRepository<RoomBalance, String> {

    /**
     * 根据房间ID查询余额
     */
    Optional<RoomBalance> findByRoomId(String roomId);

    /**
     * 查询余额低于阈值的房间
     */
    List<RoomBalance> findByBalanceLessThan(double threshold);

    /**
     * 查询余额低于阈值且未发送预警的房间
     */
    List<RoomBalance> findByBalanceLessThanAndWarningSentFalse(double threshold);
}
