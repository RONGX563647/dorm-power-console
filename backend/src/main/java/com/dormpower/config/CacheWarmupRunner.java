package com.dormpower.config;

import com.dormpower.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 缓存预热运行器
 *
 * 在应用启动时预热缓存，提升系统启动后的响应速度
 * 预热场景：
 * 1. 系统配置 - 全局配置，变化极少
 * 2. 数据字典 - 下拉框、状态码转换
 * 3. IP黑白名单 - 安全关键路径
 * 4. 电价规则 - 计费核心
 * 5. 消息模板 - 通知发送
 * 6. 楼栋列表 - 前端下拉框
 * 7. 设备告警配置 - 遥测数据处理
 * 8. RBAC资源树 - 菜单渲染
 *
 * @author dormpower team
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "spring.data.redis.host")
public class CacheWarmupRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(CacheWarmupRunner.class);

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private DataDictService dataDictService;

    @Autowired
    private IpAccessControlService ipAccessControlService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private MessageTemplateService messageTemplateService;

    @Autowired
    private DormRoomService dormRoomService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private RbacService rbacService;

    @Override
    public void run(ApplicationArguments args) {
        long startTime = System.currentTimeMillis();
        logger.info("========== 开始缓存预热 ==========");

        int successCount = 0;
        int failCount = 0;

        // 1. 系统配置
        try {
            systemConfigService.getAllConfigs();
            logger.info("[1/8] 系统配置缓存预热完成");
            successCount++;
        } catch (Exception e) {
            logger.warn("[1/8] 系统配置缓存预热失败: {}", e.getMessage());
            failCount++;
        }

        // 2. 数据字典 - 预热常用字典类型
        try {
            dataDictService.getDictsByType("BILL_STATUS");
            dataDictService.getDictsByType("DEVICE_STATUS");
            dataDictService.getDictsByType("ALERT_LEVEL");
            dataDictService.getDictsByType("ALERT_STATUS");
            logger.info("[2/8] 数据字典缓存预热完成");
            successCount++;
        } catch (Exception e) {
            logger.warn("[2/8] 数据字典缓存预热失败: {}", e.getMessage());
            failCount++;
        }

        // 3. IP黑白名单
        try {
            ipAccessControlService.getWhitelist();
            ipAccessControlService.getBlacklist();
            logger.info("[3/8] IP黑白名单缓存预热完成");
            successCount++;
        } catch (Exception e) {
            logger.warn("[3/8] IP黑白名单缓存预热失败: {}", e.getMessage());
            failCount++;
        }

        // 4. 电价规则
        try {
            billingService.getEnabledPriceRules();
            logger.info("[4/8] 电价规则缓存预热完成");
            successCount++;
        } catch (Exception e) {
            logger.warn("[4/8] 电价规则缓存预热失败: {}", e.getMessage());
            failCount++;
        }

        // 5. 消息模板
        try {
            messageTemplateService.getAllEnabledTemplates();
            logger.info("[5/8] 消息模板缓存预热完成");
            successCount++;
        } catch (Exception e) {
            logger.warn("[5/8] 消息模板缓存预热失败: {}", e.getMessage());
            failCount++;
        }

        // 6. 楼栋列表
        try {
            dormRoomService.getEnabledBuildings();
            logger.info("[6/8] 楼栋列表缓存预热完成");
            successCount++;
        } catch (Exception e) {
            logger.warn("[6/8] 楼栋列表缓存预热失败: {}", e.getMessage());
            failCount++;
        }

        // 7. 设备告警配置
        try {
            alertService.getAllAlertConfigs();
            logger.info("[7/8] 设备告警配置缓存预热完成");
            successCount++;
        } catch (Exception e) {
            logger.warn("[7/8] 设备告警配置缓存预热失败: {}", e.getMessage());
            failCount++;
        }

        // 8. RBAC资源树
        try {
            rbacService.getResourceTree();
            logger.info("[8/8] RBAC资源树缓存预热完成");
            successCount++;
        } catch (Exception e) {
            logger.warn("[8/8] RBAC资源树缓存预热失败: {}", e.getMessage());
            failCount++;
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("========== 缓存预热完成: 成功={}, 失败={}, 耗时={}ms ==========",
                successCount, failCount, duration);
    }
}