package com.dormpower.repository;

import com.dormpower.model.DeviceFirmware;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceFirmwareRepository extends JpaRepository<DeviceFirmware, Long> {

    List<DeviceFirmware> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    Optional<DeviceFirmware> findFirstByDeviceIdOrderByCreatedAtDesc(String deviceId);

    List<DeviceFirmware> findByStatusOrderByCreatedAtDesc(String status);

    Optional<DeviceFirmware> findByDeviceIdAndStatus(String deviceId, String status);

    long countByDeviceIdAndStatus(String deviceId, String status);
}
