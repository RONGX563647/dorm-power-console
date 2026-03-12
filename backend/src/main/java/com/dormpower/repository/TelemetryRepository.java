package com.dormpower.repository;

import com.dormpower.model.Telemetry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * 分页查询设备遥测数据
     * @param deviceId 设备ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<Telemetry> findByDeviceIdOrderByTsDesc(String deviceId, Pageable pageable);

    /**
     * 分页查询时间范围内的遥测数据
     * @param deviceId 设备ID
     * @param startTs 开始时间戳
     * @param endTs 结束时间戳
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<Telemetry> findByDeviceIdAndTsBetweenOrderByTsAsc(String deviceId, long startTs, long endTs, Pageable pageable);

    /**
     * 删除指定时间戳之前的遥测数据（用于数据归档）
     * @param ts 时间戳阈值
     * @return 删除的记录数
     */
    int deleteByTsLessThan(long ts);

    /**
     * 统计指定时间戳之前的遥测数据数量
     * @param ts 时间戳阈值
     * @return 记录数
     */
    long countByTsLessThan(long ts);

}
