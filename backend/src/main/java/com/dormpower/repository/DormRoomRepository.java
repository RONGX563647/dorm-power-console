package com.dormpower.repository;

import com.dormpower.model.DormRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 宿舍房间Repository
 */
@Repository
public interface DormRoomRepository extends JpaRepository<DormRoom, String> {

    /**
     * 根据楼栋ID查询房间
     */
    List<DormRoom> findByBuildingIdOrderByFloorAscRoomNumberAsc(String buildingId);

    /**
     * 根据楼栋ID和楼层查询房间
     */
    List<DormRoom> findByBuildingIdAndFloorOrderByRoomNumberAsc(String buildingId, int floor);

    /**
     * 根据房间号查询
     */
    Optional<DormRoom> findByRoomNumber(String roomNumber);

    /**
     * 根据设备ID查询房间
     */
    Optional<DormRoom> findByDeviceId(String deviceId);

    /**
     * 根据状态查询房间
     */
    List<DormRoom> findByStatus(String status);

    /**
     * 查询启用的房间
     */
    List<DormRoom> findByEnabledTrue();

    /**
     * 根据楼栋ID和状态查询
     */
    List<DormRoom> findByBuildingIdAndStatus(String buildingId, String status);
}
