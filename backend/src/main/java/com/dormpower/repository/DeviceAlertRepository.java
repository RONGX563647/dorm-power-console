package com.dormpower.repository;

import com.dormpower.model.DeviceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 设备告警仓库接口
 */
@Repository
public interface DeviceAlertRepository extends JpaRepository<DeviceAlert, String> {

    List<DeviceAlert> findByDeviceIdOrderByTsDesc(String deviceId);

    List<DeviceAlert> findByResolvedFalseOrderByTsDesc();

    List<DeviceAlert> findByDeviceIdAndResolvedFalseOrderByTsDesc(String deviceId);

    List<DeviceAlert> findByTsBetweenOrderByTsDesc(long startTs, long endTs);

}
