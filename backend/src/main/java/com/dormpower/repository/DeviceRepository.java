package com.dormpower.repository;

import com.dormpower.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 设备仓库接口
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {

    List<Device> findByRoom(String room);

}
