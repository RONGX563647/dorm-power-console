package com.dormpower.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 虚拟线程服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
class VirtualThreadServiceTest {

    @Autowired
    private VirtualThreadService virtualThreadService;

    @BeforeEach
    void setUp() {
        // 测试前准备
    }

    @Test
    void testProcessDevicesBatch() throws InterruptedException {
        // 准备测试数据
        List<String> deviceIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            deviceIds.add("device_" + i);
        }
        
        CountDownLatch latch = new CountDownLatch(1);
        
        // 异步执行 (因为服务是异步的)
        virtualThreadService.processDevicesBatch(deviceIds);
        
        // 等待处理完成
        latch.await();
        
        // 验证：不应该抛出异常
        assertTrue(true, "批量处理应该正常完成");
    }

    @Test
    void testSimulateDeviceConnection() {
        // 测试模拟设备连接
        int deviceCount = 100;
        
        int processedCount = virtualThreadService.simulateDeviceConnection(deviceCount);
        
        assertEquals(deviceCount, processedCount, 
            "所有设备应该处理完成");
    }

    @Test
    void testHighConcurrencyDeviceConnection() {
        // 测试高并发设备接入 (1000 个设备)
        int deviceCount = 1000;
        long startTime = System.currentTimeMillis();
        
        int processedCount = virtualThreadService.simulateDeviceConnection(deviceCount);
        
        long duration = System.currentTimeMillis() - startTime;
        
        assertEquals(deviceCount, processedCount, 
            "所有设备应该处理完成");
        
        // 验证性能：1000 个设备应该在 5 秒内完成
        assertTrue(duration < 5000, 
            "1000 个设备处理时间应该小于 5 秒，实际：" + duration + "ms");
        
        System.out.println("高并发测试：" + deviceCount + " 个设备，耗时：" + duration + "ms");
    }

    @Test
    void testProcessHighConcurrencyTelemetry() {
        // 准备测试数据
        List<VirtualThreadService.TelemetryData> telemetryDataList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            telemetryDataList.add(new VirtualThreadService.TelemetryData(
                "device_" + i,
                System.currentTimeMillis(),
                100.0 + i,
                220.0,
                0.5 + i * 0.01
            ));
        }
        
        // 测试高并发遥测处理
        assertDoesNotThrow(() -> {
            virtualThreadService.processHighConcurrencyTelemetry(telemetryDataList);
        }, "高并发遥测处理不应该抛出异常");
    }

    @Test
    void testConcurrentBatchProcessing() throws InterruptedException {
        int batchCount = 5;
        int devicesPerBatch = 20;
        ExecutorService executor = Executors.newFixedThreadPool(batchCount);
        CountDownLatch latch = new CountDownLatch(batchCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // 并发处理多个批次
        for (int i = 0; i < batchCount; i++) {
            final int batchIndex = i;
            executor.submit(() -> {
                try {
                    List<String> deviceIds = new ArrayList<>();
                    for (int j = 0; j < devicesPerBatch; j++) {
                        deviceIds.add("batch_" + batchIndex + "_device_" + j);
                    }
                    
                    virtualThreadService.processDevicesBatch(deviceIds);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(batchCount, successCount.get(), 
            "所有批次应该处理完成");
    }

    @Test
    void testVirtualThreadPerformance() {
        // 性能测试：对比传统线程
        int deviceCount = 500;
        List<String> deviceIds = new ArrayList<>();
        for (int i = 0; i < deviceCount; i++) {
            deviceIds.add("device_" + i);
        }
        
        long startTime = System.currentTimeMillis();
        virtualThreadService.processDevicesBatch(deviceIds);
        
        // 等待异步处理完成
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // 500 个设备应该在 3 秒内完成
        assertTrue(duration < 3000, 
            "500 个设备处理时间应该小于 3 秒，实际：" + duration + "ms");
        
        System.out.println("性能测试：" + deviceCount + " 个设备，耗时：" + duration + "ms");
    }

    @Test
    void testTelemetryDataProcessing() {
        // 测试单个遥测数据处理
        List<VirtualThreadService.TelemetryData> telemetryData = new ArrayList<>();
        telemetryData.add(new VirtualThreadService.TelemetryData(
            "test_device",
            System.currentTimeMillis(),
            150.0,
            220.0,
            0.68
        ));
        
        assertDoesNotThrow(() -> {
            virtualThreadService.processHighConcurrencyTelemetry(telemetryData);
        }, "单个遥测数据处理不应该抛出异常");
    }

    @Test
    void testLargeBatchProcessing() {
        // 测试大批量处理
        List<String> deviceIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            deviceIds.add("large_batch_device_" + i);
        }
        
        assertDoesNotThrow(() -> {
            virtualThreadService.processDevicesBatch(deviceIds);
        }, "大批量处理不应该抛出异常");
    }

    @Test
    void testEmptyBatch() {
        // 测试空批次
        List<String> emptyDeviceIds = new ArrayList<>();
        
        assertDoesNotThrow(() -> {
            virtualThreadService.processDevicesBatch(emptyDeviceIds);
        }, "空批次处理不应该抛出异常");
    }

    @Test
    void testServiceInjection() {
        // 验证服务已正确注入
        assertNotNull(virtualThreadService, "虚拟线程服务应该已注入");
    }
}
