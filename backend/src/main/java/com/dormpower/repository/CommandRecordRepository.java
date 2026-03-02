package com.dormpower.repository;

import com.dormpower.model.CommandRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 命令记录仓库接口
 */
@Repository
public interface CommandRecordRepository extends JpaRepository<CommandRecord, String> {

    /**
     * 根据设备ID查询命令记录
     * @param deviceId 设备ID
     * @return 该设备的所有命令记录
     */
    List<CommandRecord> findByDeviceId(String deviceId);

    /**
     * 根据设备ID查询命令记录（按创建时间倒序）
     * @param deviceId 设备ID
     * @return 该设备的命令记录列表，最新的在前
     */
    List<CommandRecord> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    /**
     * 根据设备ID和状态查询命令记录
     * 用于查询待处理的命令
     * @param deviceId 设备ID
     * @param state 命令状态（pending、success、failed、timeout）
     * @return 符合条件的命令记录列表
     */
    List<CommandRecord> findByDeviceIdAndState(String deviceId, String state);

    /**
     * 根据状态查询命令记录
     * 用于查询所有待处理的命令
     * @param state 命令状态
     * @return 符合条件的命令记录列表
     */
    List<CommandRecord> findByState(String state);

}
