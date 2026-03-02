package com.dormpower.repository;

import com.dormpower.model.ScheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 定时任务仓库接口
 */
@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, String> {

    List<ScheduledTask> findByDeviceIdOrderByScheduledTimeAsc(String deviceId);

    List<ScheduledTask> findByEnabledTrueAndScheduledTimeLessThanEqualOrderByScheduledTimeAsc(long timestamp);

    List<ScheduledTask> findByEnabledTrueOrderByScheduledTimeAsc();

}
