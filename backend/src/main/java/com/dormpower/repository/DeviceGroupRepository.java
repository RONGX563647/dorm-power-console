package com.dormpower.repository;

import com.dormpower.model.DeviceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 设备分组仓库接口
 */
@Repository
public interface DeviceGroupRepository extends JpaRepository<DeviceGroup, String> {

    List<DeviceGroup> findByType(String type);

    List<DeviceGroup> findByParentId(String parentId);

}
