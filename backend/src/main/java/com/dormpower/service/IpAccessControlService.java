package com.dormpower.service;

import com.dormpower.model.IpAccessControl;
import com.dormpower.repository.IpAccessControlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * IP访问控制服务
 */
@Service
public class IpAccessControlService {

    @Autowired
    private IpAccessControlRepository ipAccessControlRepository;

    @Autowired
    private SystemLogService systemLogService;

    /**
     * 添加IP到白名单
     */
    @CacheEvict(value = "ipWhitelist", allEntries = true)
    public IpAccessControl addToWhitelist(String ipAddress, String description, String createdBy) {
        return addIpControl(ipAddress, "WHITELIST", description, createdBy);
    }

    /**
     * 添加IP到黑名单
     */
    @CacheEvict(value = "ipBlacklist", allEntries = true)
    public IpAccessControl addToBlacklist(String ipAddress, String description, String createdBy) {
        return addIpControl(ipAddress, "BLACKLIST", description, createdBy);
    }

    private IpAccessControl addIpControl(String ipAddress, String type, String description, String createdBy) {
        if (ipAccessControlRepository.existsByIpAddress(ipAddress)) {
            throw new RuntimeException("IP already exists: " + ipAddress);
        }

        IpAccessControl control = new IpAccessControl(ipAddress, type);
        control.setDescription(description);
        control.setCreatedBy(createdBy);
        control.setEnabled(true);

        systemLogService.info("SECURITY",
            "IP " + type + " added: " + ipAddress,
            createdBy);

        return ipAccessControlRepository.save(control);
    }

    /**
     * 移除IP控制
     */
    @CacheEvict(value = {"ipWhitelist", "ipBlacklist"}, allEntries = true)
    public void removeIpControl(String ipAddress) {
        IpAccessControl control = ipAccessControlRepository.findByIpAddress(ipAddress)
                .orElseThrow(() -> new RuntimeException("IP not found: " + ipAddress));

        ipAccessControlRepository.delete(control);

        systemLogService.info("SECURITY", 
            "IP control removed: " + ipAddress, 
            "System");
    }

    /**
     * 更新IP控制
     */
    @CacheEvict(value = {"ipWhitelist", "ipBlacklist"}, allEntries = true)
    public IpAccessControl updateIpControl(String ipAddress, boolean enabled, long expiresAt) {
        IpAccessControl control = ipAccessControlRepository.findByIpAddress(ipAddress)
                .orElseThrow(() -> new RuntimeException("IP not found: " + ipAddress));

        control.setEnabled(enabled);
        control.setExpiresAt(expiresAt);
        control.setUpdatedAt(System.currentTimeMillis() / 1000);

        return ipAccessControlRepository.save(control);
    }

    /**
     * 检查IP是否允许访问
     */
    public boolean isIpAllowed(String ipAddress) {
        long now = System.currentTimeMillis() / 1000;

        List<IpAccessControl> activeWhitelist = ipAccessControlRepository.findActiveByType("WHITELIST", now);
        List<IpAccessControl> activeBlacklist = ipAccessControlRepository.findActiveByType("BLACKLIST", now);

        for (IpAccessControl control : activeBlacklist) {
            if (matchesIp(ipAddress, control.getIpAddress())) {
                return false;
            }
        }

        if (!activeWhitelist.isEmpty()) {
            for (IpAccessControl control : activeWhitelist) {
                if (matchesIp(ipAddress, control.getIpAddress())) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    /**
     * 检查IP是否在黑名单
     */
    public boolean isIpBlocked(String ipAddress) {
        long now = System.currentTimeMillis() / 1000;
        List<IpAccessControl> activeBlacklist = ipAccessControlRepository.findActiveByType("BLACKLIST", now);

        for (IpAccessControl control : activeBlacklist) {
            if (matchesIp(ipAddress, control.getIpAddress())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查IP是否在白名单
     */
    public boolean isIpWhitelisted(String ipAddress) {
        long now = System.currentTimeMillis() / 1000;
        List<IpAccessControl> activeWhitelist = ipAccessControlRepository.findActiveByType("WHITELIST", now);

        for (IpAccessControl control : activeWhitelist) {
            if (matchesIp(ipAddress, control.getIpAddress())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesIp(String clientIp, String controlIp) {
        if (controlIp.contains("/")) {
            return matchesCidr(clientIp, controlIp);
        }
        if (controlIp.endsWith("*")) {
            String prefix = controlIp.substring(0, controlIp.length() - 1);
            return clientIp.startsWith(prefix);
        }
        return clientIp.equals(controlIp);
    }

    private boolean matchesCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String network = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            long ipLong = ipToLong(ip);
            long networkLong = ipToLong(network);
            long mask = -1L << (32 - prefixLength);

            return (ipLong & mask) == (networkLong & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = result << 8 | (Long.parseLong(parts[i]) & 0xff);
        }
        return result;
    }

    /**
     * 获取所有白名单
     */
    @Cacheable(value = "ipWhitelist", key = "'all'")
    public List<IpAccessControl> getWhitelist() {
        return ipAccessControlRepository.findByTypeOrderByCreatedAtDesc("WHITELIST");
    }

    /**
     * 获取所有黑名单
     */
    @Cacheable(value = "ipBlacklist", key = "'all'")
    public List<IpAccessControl> getBlacklist() {
        return ipAccessControlRepository.findByTypeOrderByCreatedAtDesc("BLACKLIST");
    }

    /**
     * 获取所有活跃的IP控制
     */
    public List<IpAccessControl> getAllActive() {
        long now = System.currentTimeMillis() / 1000;
        return ipAccessControlRepository.findAllActive(now);
    }

    /**
     * 根据IP获取控制记录
     */
    public Optional<IpAccessControl> getByIpAddress(String ipAddress) {
        return ipAccessControlRepository.findByIpAddress(ipAddress);
    }

    /**
     * 清理过期记录
     */
    public void cleanupExpired() {
        long now = System.currentTimeMillis() / 1000;
        List<IpAccessControl> allControls = ipAccessControlRepository.findAll();

        for (IpAccessControl control : allControls) {
            if (control.getExpiresAt() > 0 && control.getExpiresAt() < now) {
                ipAccessControlRepository.delete(control);
                systemLogService.info("SECURITY", 
                    "Expired IP control removed: " + control.getIpAddress(), 
                    "System");
            }
        }
    }
}
