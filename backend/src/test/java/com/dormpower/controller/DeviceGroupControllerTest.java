package com.dormpower.controller;

import com.dormpower.config.TestSecurityConfig;
import com.dormpower.model.DeviceGroup;
import com.dormpower.model.DeviceGroupMapping;
import com.dormpower.repository.DeviceGroupMappingRepository;
import com.dormpower.repository.DeviceGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 设备分组单元测试
 *
 * 测试用例覆盖：
 * - TC-GROUP-001: 创建分组
 * - TC-GROUP-002: 添加设备到分组
 * - TC-GROUP-003: 重复添加设备
 * - TC-GROUP-004: 删除分组
 * - TC-GROUP-005: 查询分组设备
 *
 * @author dormpower team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class DeviceGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceGroupRepository deviceGroupRepository;

    @MockBean
    private DeviceGroupMappingRepository deviceGroupMappingRepository;

    // 测试数据常量
    private static final String GROUP_ID_1 = "group_001";
    private static final String GROUP_NAME_1 = "一楼宿舍";
    private static final String GROUP_NAME_2 = "二楼宿舍";
    private static final String GROUP_TYPE_ROOM = "room";
    private static final String DEVICE_ID_1 = "device_001";
    private static final String DEVICE_ID_2 = "device_002";
    private static final String DEVICE_ID_3 = "device_003";
    private static final String PARENT_ID_ROOT = "root";

    /**
     * 设备分组操作测试
     */
    @Nested
    @DisplayName("设备分组操作测试")
    class DeviceGroupOperationTests {

        /**
         * TC-GROUP-001: 创建分组
         *
         * 测试场景：创建分组
         * 输入：有效分组信息
         * 预期输出：返回200，分组创建成功
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-GROUP-001: 创建分组-返回200，分组创建成功")
        void testCreateGroup_Success() throws Exception {
            // Arrange
            DeviceGroup savedGroup = createGroup(GROUP_ID_1, GROUP_NAME_1, GROUP_TYPE_ROOM, PARENT_ID_ROOT);
            when(deviceGroupRepository.save(any(DeviceGroup.class))).thenReturn(savedGroup);

            // Act & Assert
            mockMvc.perform(post("/api/groups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + GROUP_NAME_1 + "\",\"type\":\"" + GROUP_TYPE_ROOM + "\",\"parentId\":\"" + PARENT_ID_ROOT + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(GROUP_NAME_1))
                    .andExpect(jsonPath("$.type").value(GROUP_TYPE_ROOM))
                    .andExpect(jsonPath("$.parentId").value(PARENT_ID_ROOT));

            verify(deviceGroupRepository).save(any(DeviceGroup.class));
        }

        /**
         * TC-GROUP-002: 添加设备到分组
         *
         * 测试场景：添加设备到分组
         * 输入：有效设备ID列表
         * 预期输出：返回200，设备添加成功
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-GROUP-002: 添加设备到分组-返回200，设备添加成功")
        void testAddDeviceToGroup_Success() throws Exception {
            // Arrange
            DeviceGroup group = createGroup(GROUP_ID_1, GROUP_NAME_1, GROUP_TYPE_ROOM, PARENT_ID_ROOT);
            when(deviceGroupRepository.findById(GROUP_ID_1)).thenReturn(Optional.of(group));
            when(deviceGroupMappingRepository.findByDeviceId(anyString())).thenReturn(new ArrayList<>());
            when(deviceGroupMappingRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            // Act & Assert
            mockMvc.perform(post("/api/groups/{groupId}/devices", GROUP_ID_1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[\"" + DEVICE_ID_1 + "\", \"" + DEVICE_ID_2 + "\"]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Devices added to group successfully"))
                    .andExpect(jsonPath("$.count").value(2));

            verify(deviceGroupMappingRepository).saveAll(anyList());
        }

        /**
         * TC-GROUP-003: 重复添加设备
         *
         * 测试场景：重复添加设备
         * 输入：已存在的设备ID
         * 预期输出：忽略重复，返回成功
         * 优先级：P1
         */
        @Test
        @DisplayName("TC-GROUP-003: 重复添加设备-忽略重复，返回成功")
        void testAddDuplicateDevice_IgnoreDuplicate() throws Exception {
            // Arrange: 模拟设备1已经在分组中
            DeviceGroup group = createGroup(GROUP_ID_1, GROUP_NAME_1, GROUP_TYPE_ROOM, PARENT_ID_ROOT);
            when(deviceGroupRepository.findById(GROUP_ID_1)).thenReturn(Optional.of(group));

            // 设备1已存在于分组中
            DeviceGroupMapping existingMapping = createMapping("mapping_1", DEVICE_ID_1, GROUP_ID_1);
            when(deviceGroupMappingRepository.findByDeviceId(DEVICE_ID_1))
                    .thenReturn(List.of(existingMapping));
            when(deviceGroupMappingRepository.findByDeviceId(DEVICE_ID_2))
                    .thenReturn(new ArrayList<>());
            when(deviceGroupMappingRepository.findByDeviceId(DEVICE_ID_3))
                    .thenReturn(new ArrayList<>());
            when(deviceGroupMappingRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

            // Act & Assert: 添加3个设备，其中设备1已存在
            mockMvc.perform(post("/api/groups/{groupId}/devices", GROUP_ID_1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[\"" + DEVICE_ID_1 + "\", \"" + DEVICE_ID_2 + "\", \"" + DEVICE_ID_3 + "\"]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Devices added to group successfully"))
                    .andExpect(jsonPath("$.count").value(2)); // 只有2个新设备被添加
        }

        /**
         * TC-GROUP-004: 删除分组
         *
         * 测试场景：删除分组
         * 输入：有效分组ID
         * 预期输出：分组和关联关系删除成功
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-GROUP-004: 删除分组-分组和关联关系删除成功")
        void testDeleteGroup_Success() throws Exception {
            // Arrange
            DeviceGroup group = createGroup(GROUP_ID_1, GROUP_NAME_1, GROUP_TYPE_ROOM, PARENT_ID_ROOT);
            when(deviceGroupRepository.findById(GROUP_ID_1)).thenReturn(Optional.of(group));
            doNothing().when(deviceGroupMappingRepository).deleteByGroupId(GROUP_ID_1);
            doNothing().when(deviceGroupRepository).delete(group);

            // Act & Assert
            mockMvc.perform(delete("/api/groups/{groupId}", GROUP_ID_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Group deleted successfully"));

            verify(deviceGroupMappingRepository).deleteByGroupId(GROUP_ID_1);
            verify(deviceGroupRepository).delete(group);
        }

        /**
         * TC-GROUP-005: 查询分组设备
         *
         * 测试场景：查询分组设备
         * 输入：有效分组ID
         * 预期输出：返回设备ID列表
         * 优先级：P0
         */
        @Test
        @DisplayName("TC-GROUP-005: 查询分组设备-返回设备ID列表")
        void testGetGroupDevices_Success() throws Exception {
            // Arrange
            List<DeviceGroupMapping> mappings = Arrays.asList(
                    createMapping("mapping_1", DEVICE_ID_1, GROUP_ID_1),
                    createMapping("mapping_2", DEVICE_ID_2, GROUP_ID_1),
                    createMapping("mapping_3", DEVICE_ID_3, GROUP_ID_1)
            );
            when(deviceGroupMappingRepository.findByGroupId(GROUP_ID_1)).thenReturn(mappings);

            // Act & Assert
            mockMvc.perform(get("/api/groups/{groupId}/devices", GROUP_ID_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deviceIds").isArray())
                    .andExpect(jsonPath("$.deviceIds[0]").value(DEVICE_ID_1))
                    .andExpect(jsonPath("$.deviceIds[1]").value(DEVICE_ID_2))
                    .andExpect(jsonPath("$.deviceIds[2]").value(DEVICE_ID_3))
                    .andExpect(jsonPath("$.count").value(3));
        }
    }

    /**
     * 边界条件和异常场景测试
     */
    @Nested
    @DisplayName("边界条件和异常场景测试")
    class EdgeCaseTests {

        /**
         * 测试删除不存在的分组
         */
        @Test
        @DisplayName("删除不存在的分组-返回404")
        void testDeleteNonExistentGroup_NotFound() throws Exception {
            // Arrange
            when(deviceGroupRepository.findById("non_existent_group")).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(delete("/api/groups/{groupId}", "non_existent_group"))
                    .andExpect(status().isNotFound());
        }

        /**
         * 测试查询不存在分组的设备
         */
        @Test
        @DisplayName("查询不存在分组的设备-返回空列表")
        void testGetDevicesFromNonExistentGroup_EmptyList() throws Exception {
            // Arrange
            when(deviceGroupMappingRepository.findByGroupId("non_existent_group")).thenReturn(new ArrayList<>());

            // Act & Assert
            mockMvc.perform(get("/api/groups/{groupId}/devices", "non_existent_group"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0));
        }

        /**
         * 测试获取分组列表
         */
        @Test
        @DisplayName("获取分组列表-返回所有分组")
        void testGetGroups_Success() throws Exception {
            // Arrange
            List<DeviceGroup> groups = Arrays.asList(
                    createGroup(GROUP_ID_1, GROUP_NAME_1, GROUP_TYPE_ROOM, PARENT_ID_ROOT)
            );
            when(deviceGroupRepository.findAll()).thenReturn(groups);

            // Act & Assert
            mockMvc.perform(get("/api/groups"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        /**
         * 测试更新分组
         */
        @Test
        @DisplayName("更新分组-返回成功")
        void testUpdateGroup_Success() throws Exception {
            // Arrange
            DeviceGroup existingGroup = createGroup(GROUP_ID_1, GROUP_NAME_1, GROUP_TYPE_ROOM, PARENT_ID_ROOT);
            when(deviceGroupRepository.findById(GROUP_ID_1)).thenReturn(Optional.of(existingGroup));
            when(deviceGroupRepository.save(any(DeviceGroup.class))).thenReturn(existingGroup);

            // Act & Assert
            mockMvc.perform(put("/api/groups/{groupId}", GROUP_ID_1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + GROUP_NAME_2 + "\",\"type\":\"floor\",\"parentId\":\"parent_001\"}"))
                    .andExpect(status().isOk());
        }
    }

    // ==================== 辅助方法：创建测试数据 ====================

    /**
     * 创建设备分组实体
     */
    private DeviceGroup createGroup(String id, String name, String type, String parentId) {
        DeviceGroup group = new DeviceGroup();
        group.setId(id);
        group.setName(name);
        group.setType(type);
        group.setParentId(parentId);
        group.setCreatedAt(System.currentTimeMillis() / 1000);
        return group;
    }

    /**
     * 创建设备分组映射实体
     */
    private DeviceGroupMapping createMapping(String id, String deviceId, String groupId) {
        DeviceGroupMapping mapping = new DeviceGroupMapping();
        mapping.setId(id);
        mapping.setDeviceId(deviceId);
        mapping.setGroupId(groupId);
        mapping.setCreatedAt(System.currentTimeMillis() / 1000);
        return mapping;
    }
}