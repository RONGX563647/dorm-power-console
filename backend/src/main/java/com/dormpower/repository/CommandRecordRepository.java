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

    List<CommandRecord> findByDeviceId(String deviceId);

    List<CommandRecord> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    List<CommandRecord> findByDeviceIdAndState(String deviceId, String state);

    List<CommandRecord> findByState(String state);

}
