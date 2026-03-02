package com.dormpower.repository;

import com.dormpower.model.DeviceStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 设备状态历史仓库接口
 */
@Repository
public interface DeviceStatusHistoryRepository extends JpaRepository<DeviceStatusHistory, String> {

    List<DeviceStatusHistory> findByDeviceIdOrderByTsDesc(String deviceId);

    List<DeviceStatusHistory> findByDeviceIdAndTsBetweenOrderByTsDesc(String deviceId, long startTs, long endTs);

}
