package com.dormpower.service;

import com.dormpower.model.Notification;
import com.dormpower.model.NotificationPreference;
import com.dormpower.repository.NotificationPreferenceRepository;
import com.dormpower.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知服务类
 */
@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    /**
     * 创建通知（清除未读计数缓存）
     */
    @CacheEvict(value = "unreadCount", key = "#notification.username")
    public Notification createNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    /**
     * 创建系统通知
     */
    public Notification createSystemNotification(String title, String content, String priority) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType("SYSTEM");
        notification.setPriority(priority);
        notification.setSource("SYSTEM");
        return notificationRepository.save(notification);
    }

    /**
     * 创建告警通知（清除未读计数缓存）
     */
    @CacheEvict(value = "unreadCount", key = "#username")
    public Notification createAlertNotification(String title, String content, String username, String sourceId) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType("ALERT");
        notification.setPriority("HIGH");
        notification.setUsername(username);
        notification.setSource("ALERT");
        notification.setSourceId(sourceId);
        return notificationRepository.save(notification);
    }

    /**
     * 创建邮件通知（清除未读计数缓存）
     */
    @CacheEvict(value = "unreadCount", key = "#username")
    public Notification createEmailNotification(String title, String content, String username) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType("EMAIL");
        notification.setPriority("NORMAL");
        notification.setUsername(username);
        notification.setSource("EMAIL");
        return notificationRepository.save(notification);
    }

    /**
     * 获取用户通知列表
     */
    public Page<Notification> getUserNotifications(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return notificationRepository.findByUsernameOrderByCreatedAtDesc(username, pageable);
    }

    /**
     * 获取用户未读通知
     */
    public List<Notification> getUserUnreadNotifications(String username) {
        return notificationRepository.findByUsernameAndReadOrderByCreatedAtDesc(username, false);
    }

    /**
     * 获取用户未读通知数量（带缓存）
     */
    @Cacheable(value = "unreadCount", key = "#username")
    public long getUserUnreadCount(String username) {
        return notificationRepository.countByUsernameAndRead(username, false);
    }

    /**
     * 标记通知为已读（清除未读计数缓存）
     */
    @Transactional
    @CacheEvict(value = "unreadCount", allEntries = true)
    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId, LocalDateTime.now());
    }

    /**
     * 标记所有通知为已读（清除未读计数缓存）
     */
    @Transactional
    @CacheEvict(value = "unreadCount", key = "#username")
    public void markAllAsRead(String username) {
        notificationRepository.markAllAsRead(username, LocalDateTime.now());
    }

    /**
     * 删除通知
     */
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    /**
     * 清理过期通知
     */
    public void cleanupOldNotifications(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        notificationRepository.deleteByCreatedAtBefore(cutoffDate);
    }

    /**
     * 获取通知统计
     */
    public Map<String, Object> getNotificationStatistics(String username) {
        Map<String, Object> stats = new HashMap<>();

        long unreadCount = notificationRepository.countByUsernameAndRead(username, false);
        stats.put("unreadCount", unreadCount);

        List<Object[]> typeCounts = notificationRepository.countUnreadByType(username);
        Map<String, Long> typeStats = new HashMap<>();
        for (Object[] row : typeCounts) {
            typeStats.put((String) row[0], (Long) row[1]);
        }
        stats.put("unreadByType", typeStats);

        return stats;
    }
    
    // ==================== 通知偏好设置 ====================
    
    /**
     * 获取用户通知偏好设置
     */
    public NotificationPreference getUserPreference(String username) {
        return preferenceRepository.findByUsername(username)
                .orElseGet(() -> {
                    NotificationPreference pref = new NotificationPreference(username);
                    return preferenceRepository.save(pref);
                });
    }
    
    /**
     * 更新用户通知偏好设置
     */
    public NotificationPreference updateUserPreference(String username, NotificationPreference preference) {
        NotificationPreference existingPref = preferenceRepository.findByUsername(username)
                .orElseGet(() -> {
                    NotificationPreference newPref = new NotificationPreference(username);
                    return newPref;
                });
        
        if (preference.isEmailEnabled() != existingPref.isEmailEnabled()) {
            existingPref.setEmailEnabled(preference.isEmailEnabled());
        }
        if (preference.isSystemEnabled() != existingPref.isSystemEnabled()) {
            existingPref.setSystemEnabled(preference.isSystemEnabled());
        }
        if (preference.isAlertEnabled() != existingPref.isAlertEnabled()) {
            existingPref.setAlertEnabled(preference.isAlertEnabled());
        }
        if (preference.isBillingEnabled() != existingPref.isBillingEnabled()) {
            existingPref.setBillingEnabled(preference.isBillingEnabled());
        }
        if (preference.isMaintenanceEnabled() != existingPref.isMaintenanceEnabled()) {
            existingPref.setMaintenanceEnabled(preference.isMaintenanceEnabled());
        }
        if (preference.isQuietHoursEnabled() != existingPref.isQuietHoursEnabled()) {
            existingPref.setQuietHoursEnabled(preference.isQuietHoursEnabled());
        }
        if (preference.getQuietHoursStart() != null) {
            existingPref.setQuietHoursStart(preference.getQuietHoursStart());
        }
        if (preference.getQuietHoursEnd() != null) {
            existingPref.setQuietHoursEnd(preference.getQuietHoursEnd());
        }
        if (preference.getAlertLevel() != null) {
            existingPref.setAlertLevel(preference.getAlertLevel());
        }
        
        existingPref.setUpdatedAt(System.currentTimeMillis() / 1000);
        
        return preferenceRepository.save(existingPref);
    }
    
    /**
     * 重置用户通知偏好设置为默认值
     */
    @Transactional
    public NotificationPreference resetUserPreference(String username) {
        NotificationPreference pref = preferenceRepository.findByUsername(username)
                .orElseGet(() -> new NotificationPreference(username));
        
        pref.setEmailEnabled(true);
        pref.setSystemEnabled(true);
        pref.setAlertEnabled(true);
        pref.setBillingEnabled(true);
        pref.setMaintenanceEnabled(true);
        pref.setQuietHoursEnabled(false);
        pref.setQuietHoursStart("22:00");
        pref.setQuietHoursEnd("08:00");
        pref.setAlertLevel("warning");
        pref.setUpdatedAt(System.currentTimeMillis() / 1000);
        
        return preferenceRepository.save(pref);
    }
    
    /**
     * 检查用户是否启用了特定类型的通知
     */
    public boolean isNotificationEnabled(String username, String notificationType) {
        NotificationPreference pref = getUserPreference(username);
        
        switch (notificationType.toUpperCase()) {
            case "EMAIL":
                return pref.isEmailEnabled();
            case "SYSTEM":
                return pref.isSystemEnabled();
            case "ALERT":
                return pref.isAlertEnabled();
            case "BILLING":
                return pref.isBillingEnabled();
            case "MAINTENANCE":
                return pref.isMaintenanceEnabled();
            default:
                return true;
        }
    }
    
    /**
     * 检查当前是否在用户的免打扰时段内
     */
    public boolean isInQuietHours(String username) {
        NotificationPreference pref = getUserPreference(username);
        
        if (!pref.isQuietHoursEnabled()) {
            return false;
        }
        
        try {
            String[] startParts = pref.getQuietHoursStart().split(":");
            String[] endParts = pref.getQuietHoursEnd().split(":");
            
            int startHour = Integer.parseInt(startParts[0]);
            int startMinute = Integer.parseInt(startParts[1]);
            int endHour = Integer.parseInt(endParts[0]);
            int endMinute = Integer.parseInt(endParts[1]);
            
            java.time.LocalTime now = java.time.LocalTime.now();
            java.time.LocalTime startTime = java.time.LocalTime.of(startHour, startMinute);
            java.time.LocalTime endTime = java.time.LocalTime.of(endHour, endMinute);
            
            if (startTime.isAfter(endTime)) {
                return now.isAfter(startTime) || now.isBefore(endTime);
            } else {
                return now.isAfter(startTime) && now.isBefore(endTime);
            }
        } catch (Exception e) {
            return false;
        }
    }
}
