package com.dormpower.repository;

import com.dormpower.model.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 遥测数据仓库接口
 */
@Repository
public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {

    List<Telemetry> findByDeviceIdAndTsBetween(String deviceId, long startTs, long endTs);

    List<Telemetry> findByDeviceIdAndTsBetweenOrderByTsAsc(String deviceId, long startTs, long endTs);

    Telemetry findFirstByDeviceIdAndTsLessThanOrderByTsDesc(String deviceId, long ts);

    List<Telemetry> findByDeviceIdOrderByTsDesc(String deviceId);

}
