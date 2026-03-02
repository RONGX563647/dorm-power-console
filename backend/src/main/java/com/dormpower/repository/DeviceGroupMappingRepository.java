package com.dormpower.repository;

import com.dormpower.model.DeviceGroupMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 设备分组映射仓库接口
 */
@Repository
public interface DeviceGroupMappingRepository extends JpaRepository<DeviceGroupMapping, String> {

    List<DeviceGroupMapping> findByDeviceId(String deviceId);

    List<DeviceGroupMapping> findByGroupId(String groupId);

    void deleteByDeviceId(String deviceId);

    void deleteByGroupId(String groupId);

}
