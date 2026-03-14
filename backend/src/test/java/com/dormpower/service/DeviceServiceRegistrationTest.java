package com.dormpower.service;

import com.dormpower.dto.DeviceRegistrationRequest;
import com.dormpower.exception.BusinessException;
import com.dormpower.model.Device;
import com.dormpower.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 设备注册服务单元测试
 *
 * 测试用例覆盖：
 * - TC-REG-001: 正常注册新设备
 * - TC-REG-002: 注册已存在设备
 * - TC-REG-003: 设备ID格式错误（超过64字符）
 * - TC-REG-004: 缺少必填字段（缺少name字段）
 * - TC-REG-005: 房间格式错误
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class DeviceServiceRegistrationTest {

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceService deviceService;

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ==================== TC-REG-001: 正常注册新设备 ====================

    @Test
    @DisplayName("TC-REG-001: 正常注册新设备")
    void testRegisterDevice_Success() {
        // Given
        String deviceId = "device_001";
        String name = "A1-301智能插座";
        String room = "A1-301";

        when(deviceRepository.existsById(deviceId)).thenReturn(false);
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> {
            Device device = invocation.getArgument(0);
            return device;
        });

        // When
        Device result = deviceService.registerDevice(deviceId, name, room);

        // Then
        assertNotNull(result);
        assertEquals(deviceId, result.getId());
        assertEquals(name, result.getName());
        assertEquals(room, result.getRoom());
        assertFalse(result.isOnline());
        assertTrue(result.getCreatedAt() > 0);
        assertTrue(result.getLastSeenTs() > 0);

        verify(deviceRepository).existsById(deviceId);
        verify(deviceRepository).save(any(Device.class));
    }

    // ==================== TC-REG-002: 注册已存在设备 ====================

    @Test
    @DisplayName("TC-REG-002: 注册已存在设备")
    void testRegisterDevice_AlreadyExists() {
        // Given
        String deviceId = "device_001";
        String name = "A1-301智能插座";
        String room = "A1-301";

        when(deviceRepository.existsById(deviceId)).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> deviceService.registerDevice(deviceId, name, room)
        );

        assertTrue(exception.getMessage().contains("设备ID已存在"));
        verify(deviceRepository).existsById(deviceId);
        verify(deviceRepository, never()).save(any(Device.class));
    }

    // ==================== TC-REG-003: 设备ID格式错误（超过64字符） ====================

    @Test
    @DisplayName("TC-REG-003: 设备ID超过64字符，校验失败")
    void testRegisterDevice_InvalidDeviceId_TooLong() {
        // Given - 构造超过64字符的设备ID
        String tooLongId = "a".repeat(65);
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setId(tooLongId);
        request.setName("测试设备");
        request.setRoom("A1-301");

        // When
        Set<ConstraintViolation<DeviceRegistrationRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("设备ID只能包含字母、数字、下划线，长度不超过64字符")));
    }

    @Test
    @DisplayName("TC-REG-003: 设备ID包含非法字符，校验失败")
    void testRegisterDevice_InvalidDeviceId_InvalidChars() {
        // Given - 设备ID包含非法字符（中文）
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setId("设备001");
        request.setName("测试设备");
        request.setRoom("A1-301");

        // When
        Set<ConstraintViolation<DeviceRegistrationRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("设备ID只能包含字母、数字、下划线")));
    }

    // ==================== TC-REG-004: 缺少必填字段（缺少name字段） ====================

    @Test
    @DisplayName("TC-REG-004: 缺少name字段，校验失败")
    void testRegisterDevice_MissingName() {
        // Given
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setId("device_001");
        request.setName(null); // 缺少name
        request.setRoom("A1-301");

        // When
        Set<ConstraintViolation<DeviceRegistrationRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("设备名称不能为空")));
    }

    @Test
    @DisplayName("TC-REG-004: name为空字符串，校验失败")
    void testRegisterDevice_BlankName() {
        // Given
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setId("device_001");
        request.setName(""); // 空字符串
        request.setRoom("A1-301");

        // When
        Set<ConstraintViolation<DeviceRegistrationRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("设备名称不能为空")));
    }

    // ==================== TC-REG-005: 房间格式错误 ====================

    @Test
    @DisplayName("TC-REG-005: 无效的房间编号格式，校验失败")
    void testRegisterDevice_InvalidRoomFormat() {
        // Given - 房间格式不正确（缺少楼栋号）
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setId("device_001");
        request.setName("测试设备");
        request.setRoom("301"); // 缺少楼栋号前缀

        // When
        Set<ConstraintViolation<DeviceRegistrationRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("房间格式错误")));
    }

    @Test
    @DisplayName("TC-REG-005: 房间格式正确（标准格式），校验通过")
    void testRegisterDevice_ValidRoomFormat() {
        // Given - 正确的房间格式
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setId("device_001");
        request.setName("测试设备");
        request.setRoom("A1-301");

        // When
        Set<ConstraintViolation<DeviceRegistrationRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("TC-REG-005: 房间格式正确（带楼层前缀），校验通过")
    void testRegisterDevice_ValidRoomFormat_WithFloor() {
        // Given - 正确的房间格式（带楼层前缀）
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setId("device_001");
        request.setName("测试设备");
        request.setRoom("B12-1001");

        // When
        Set<ConstraintViolation<DeviceRegistrationRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    // ==================== 其他边界测试 ====================

    @Test
    @DisplayName("TC-REG-003: 设备ID恰好64字符，校验通过")
    void testRegisterDevice_DeviceIdExactly64Chars() {
        // Given - 恰好64字符的设备ID
        String validId = "a".repeat(64);
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setId(validId);
        request.setName("测试设备");
        request.setRoom("A1-301");

        // When
        Set<ConstraintViolation<DeviceRegistrationRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("TC-REG-004: 缺少房间号，校验失败")
    void testRegisterDevice_MissingRoom() {
        // Given
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setId("device_001");
        request.setName("测试设备");
        request.setRoom(null); // 缺少房间号

        // When
        Set<ConstraintViolation<DeviceRegistrationRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("房间号不能为空")));
    }

    @Test
    @DisplayName("TC-REG-004: 缺少设备ID，校验失败")
    void testRegisterDevice_MissingDeviceId() {
        // Given
        DeviceRegistrationRequest request = new DeviceRegistrationRequest();
        request.setId(null); // 缺少设备ID
        request.setName("测试设备");
        request.setRoom("A1-301");

        // When
        Set<ConstraintViolation<DeviceRegistrationRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("设备ID不能为空")));
    }

}