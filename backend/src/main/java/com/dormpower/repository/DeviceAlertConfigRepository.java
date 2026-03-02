package com.dormpower.repository;

import com.dormpower.model.DeviceAlertConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 设备告警配置仓库接口
 */
@Repository
public interface DeviceAlertConfigRepository extends JpaRepository<DeviceAlertConfig, String> {

    List<DeviceAlertConfig> findByDeviceId(String deviceId);

    DeviceAlertConfig findByDeviceIdAndType(String deviceId, String type);

    List<DeviceAlertConfig> findByEnabledTrue();

}
