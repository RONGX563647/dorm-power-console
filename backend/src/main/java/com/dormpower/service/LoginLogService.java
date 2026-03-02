package com.dormpower.service;

import com.dormpower.model.LoginLog;
import com.dormpower.repository.LoginLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录日志服务
 */
@Service
public class LoginLogService {

    @Autowired
    private LoginLogRepository loginLogRepository;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 1800;

    /**
     * 记录登录成功
     */
    public LoginLog logLoginSuccess(String username, String ipAddress, String userAgent, 
                                     String browser, String os, String sessionId) {
        LoginLog log = new LoginLog(username, "SUCCESS");
        log.setLoginType("PASSWORD");
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setBrowser(browser);
        log.setOs(os);
        log.setSessionId(sessionId);
        log.setMessage("Login successful");
        return loginLogRepository.save(log);
    }

    /**
     * 记录登录失败
     */
    public LoginLog logLoginFailure(String username, String ipAddress, String userAgent, String reason) {
        LoginLog log = new LoginLog(username, "FAILED");
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setMessage(reason);
        return loginLogRepository.save(log);
    }

    /**
     * 记录登出
     */
    public void logLogout(String sessionId) {
        loginLogRepository.findAllByOrderByLoginTsDesc(PageRequest.of(0, 100))
                .stream()
                .filter(log -> sessionId != null && sessionId.equals(log.getSessionId()))
                .filter(log -> log.getLogoutTs() == null)
                .findFirst()
                .ifPresent(log -> {
                    log.setLogoutTs(System.currentTimeMillis() / 1000);
                    log.setDuration(log.getLogoutTs() - log.getLoginTs());
                    loginLogRepository.save(log);
                });
    }

    /**
     * 检查账户是否被锁定
     */
    public boolean isAccountLocked(String username) {
        long since = System.currentTimeMillis() / 1000 - LOCKOUT_DURATION;
        long failedAttempts = loginLogRepository.countFailedLoginsSince(username, since);
        return failedAttempts >= MAX_FAILED_ATTEMPTS;
    }

    /**
     * 检查IP是否被封禁
     */
    public boolean isIpBlocked(String ipAddress) {
        long since = System.currentTimeMillis() / 1000 - 3600;
        long failedAttempts = loginLogRepository.countFailedLoginsFromIpSince(ipAddress, since);
        return failedAttempts >= MAX_FAILED_ATTEMPTS * 3;
    }

    /**
     * 获取用户登录历史
     */
    public Page<LoginLog> getUserLoginHistory(String username, Pageable pageable) {
        return loginLogRepository.findByUsernameOrderByLoginTsDesc(username, pageable);
    }

    /**
     * 获取所有登录日志
     */
    public Page<LoginLog> getAllLoginLogs(Pageable pageable) {
        return loginLogRepository.findAllByOrderByLoginTsDesc(pageable);
    }

    /**
     * 获取时间范围内的登录日志
     */
    public Page<LoginLog> getLoginLogsByTimeRange(long startTime, long endTime, Pageable pageable) {
        return loginLogRepository.findByTimeRange(startTime, endTime, pageable);
    }

    /**
     * 获取登录统计
     */
    public Map<String, Object> getLoginStatistics(long since) {
        Map<String, Object> stats = new HashMap<>();

        long now = System.currentTimeMillis() / 1000;
        long dayAgo = now - 86400;
        long weekAgo = now - 604800;

        stats.put("totalLogins", loginLogRepository.count());
        stats.put("loginsToday", loginLogRepository.findByTimeRange(dayAgo, now, Pageable.unpaged()).getTotalElements());
        stats.put("loginsThisWeek", loginLogRepository.findByTimeRange(weekAgo, now, Pageable.unpaged()).getTotalElements());

        List<Object[]> topIps = loginLogRepository.findTopIpAddresses(since, PageRequest.of(0, 10));
        stats.put("topIpAddresses", topIps);

        List<Object[]> topUsers = loginLogRepository.findTopActiveUsers(since, PageRequest.of(0, 10));
        stats.put("topActiveUsers", topUsers);

        return stats;
    }

    /**
     * 清理过期日志
     */
    public long cleanupOldLogs(int retentionDays) {
        long cutoff = System.currentTimeMillis() / 1000 - (retentionDays * 86400L);
        return loginLogRepository.deleteByLoginTsBefore(cutoff);
    }

    /**
     * 获取最近登录记录
     */
    public LoginLog getLastLogin(String username) {
        return loginLogRepository.findByUsernameAndLoginTsAfterOrderByLoginTsDesc(
                username, 0)
                .stream()
                .filter(log -> "SUCCESS".equals(log.getStatus()))
                .skip(1)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取当前在线用户数
     */
    public long getActiveSessionCount() {
        long activeThreshold = System.currentTimeMillis() / 1000 - 1800;
        return loginLogRepository.findAllByOrderByLoginTsDesc(PageRequest.of(0, 1000))
                .stream()
                .filter(log -> "SUCCESS".equals(log.getStatus()))
                .filter(log -> log.getLogoutTs() == null)
                .filter(log -> log.getLoginTs() > activeThreshold)
                .count();
    }
}
