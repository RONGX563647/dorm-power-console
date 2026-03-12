package com.dormpower.repository;

import com.dormpower.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户通知偏好设置数据访问层
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    
    /**
     * 根据用户名查找通知偏好设置
     */
    Optional<NotificationPreference> findByUsername(String username);
    
    /**
     * 检查用户是否存在偏好设置
     */
    boolean existsByUsername(String username);
    
    /**
     * 根据用户名删除偏好设置
     */
    void deleteByUsername(String username);
}
