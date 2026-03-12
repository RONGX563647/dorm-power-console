package com.dormpower.repository;

import com.dormpower.model.DataBackup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据备份仓库接口
 */
@Repository
public interface DataBackupRepository extends JpaRepository<DataBackup, Long> {

    List<DataBackup> findByType(String type);

    List<DataBackup> findByStatus(String status);

    List<DataBackup> findByCreatedAtBefore(LocalDateTime date);

    List<DataBackup> findTop10ByOrderByCreatedAtDesc();
}
