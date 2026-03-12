package com.dormpower.repository;

import com.dormpower.model.StripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 插座状态仓库接口
 */
@Repository
public interface StripStatusRepository extends JpaRepository<StripStatus, String> {

    StripStatus findByDeviceId(String deviceId);

}
