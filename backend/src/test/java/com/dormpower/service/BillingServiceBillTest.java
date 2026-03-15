package com.dormpower.service;

import com.dormpower.model.DormRoom;
import com.dormpower.model.ElectricityBill;
import com.dormpower.model.ElectricityPriceRule;
import com.dormpower.model.Telemetry;
import com.dormpower.repository.DormRoomRepository;
import com.dormpower.repository.ElectricityBillRepository;
import com.dormpower.repository.ElectricityPriceRuleRepository;
import com.dormpower.repository.TelemetryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * 账单生成和查询单元测试
 *
 * 测试用例覆盖：
 * - TC-BILL-001: 生成账单
 * - TC-BILL-002: 房间不存在
 * - TC-BILL-003: 无设备房间
 * - TC-BILL-004: 查询账单
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class BillingServiceBillTest {

    @Mock
    private ElectricityBillRepository billRepository;

    @Mock
    private DormRoomRepository dormRoomRepository;

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private ElectricityPriceRuleRepository priceRuleRepository;

    @InjectMocks
    private BillingService billingService;

    private static final String ROOM_ID = "room_001";
    private static final String DEVICE_ID = "device_001";
    private static final String PERIOD = "2024-01";
    private static final String PRICE_RULE_ID = "rule_001";

    // ==================== TC-BILL-001: 生成账单 ====================

    @Nested
    @DisplayName("TC-BILL-001: 生成账单")
    class GenerateBillTests {

        @Test
        @DisplayName("TC-BILL-001: 有效房间和周期，账单生成成功")
        void testGenerateMonthlyBill_ValidRoomAndPeriod_BillGeneratedSuccessfully() {
            // Given
            DormRoom room = createRoomWithDevice();
            ElectricityPriceRule priceRule = createPriceRule();
            List<Telemetry> telemetryList = createMockTelemetry(DEVICE_ID, 100);

            when(dormRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            when(telemetryRepository.findByDeviceIdAndTsBetween(eq(DEVICE_ID), anyLong(), anyLong()))
                    .thenReturn(telemetryList);
            when(priceRuleRepository.findById(any())).thenReturn(Optional.of(priceRule));
            when(priceRuleRepository.findFirstByEnabledTrueOrderByCreatedAtAsc()).thenReturn(Optional.of(priceRule));
            when(billRepository.save(any(ElectricityBill.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ElectricityBill bill = billingService.generateMonthlyBill(ROOM_ID, PERIOD);

            // Then
            assertNotNull(bill);
            assertEquals(ROOM_ID, bill.getRoomId());
            assertEquals(PERIOD, bill.getPeriod());
            assertEquals("PENDING", bill.getStatus());
            assertTrue(bill.getTotalConsumption() >= 0);
            assertTrue(bill.getTotalAmount() >= 0);

            verify(billRepository).save(any(ElectricityBill.class));
        }

        @Test
        @DisplayName("TC-BILL-001: 账单包含正确的用电量和金额")
        void testGenerateMonthlyBill_ContainsCorrectConsumptionAndAmount() {
            // Given
            DormRoom room = createRoomWithDevice();
            ElectricityPriceRule priceRule = createPriceRule();
            // 创建100条功率为1000W的遥测数据
            List<Telemetry> telemetryList = createMockTelemetryWithPower(DEVICE_ID, 100, 1000.0);

            when(dormRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            when(telemetryRepository.findByDeviceIdAndTsBetween(eq(DEVICE_ID), anyLong(), anyLong()))
                    .thenReturn(telemetryList);
            when(priceRuleRepository.findById(any())).thenReturn(Optional.of(priceRule));
            when(priceRuleRepository.findFirstByEnabledTrueOrderByCreatedAtAsc()).thenReturn(Optional.of(priceRule));
            when(billRepository.save(any(ElectricityBill.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ElectricityBill bill = billingService.generateMonthlyBill(ROOM_ID, PERIOD);

            // Then
            // 总功率 = 100 * 1000W = 100000W = 100kWh (度)
            // 电费 = 100 * 0.5 = 50元
            assertEquals(100.0, bill.getTotalConsumption(), 0.1);
            assertEquals(50.0, bill.getTotalAmount(), 0.1);
        }

        @Test
        @DisplayName("TC-BILL-001: 无遥测数据时生成0度账单")
        void testGenerateMonthlyBill_NoTelemetry_ZeroConsumption() {
            // Given
            DormRoom room = createRoomWithDevice();
            ElectricityPriceRule priceRule = createPriceRule();

            when(dormRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            when(telemetryRepository.findByDeviceIdAndTsBetween(eq(DEVICE_ID), anyLong(), anyLong()))
                    .thenReturn(Collections.emptyList());
            when(priceRuleRepository.findById(any())).thenReturn(Optional.of(priceRule));
            when(priceRuleRepository.findFirstByEnabledTrueOrderByCreatedAtAsc()).thenReturn(Optional.of(priceRule));
            when(billRepository.save(any(ElectricityBill.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ElectricityBill bill = billingService.generateMonthlyBill(ROOM_ID, PERIOD);

            // Then
            assertEquals(0.0, bill.getTotalConsumption(), 0.01);
            assertEquals(0.0, bill.getTotalAmount(), 0.01);
        }

        @Test
        @DisplayName("TC-BILL-001: 使用房间指定的电价规则")
        void testGenerateMonthlyBill_UsesRoomPriceRule() {
            // Given
            DormRoom room = createRoomWithDevice();
            String customRuleId = "rule_custom";
            room.setPriceRuleId(customRuleId);

            ElectricityPriceRule customRule = createPriceRule();
            customRule.setId(customRuleId);
            customRule.setTier1Price(0.6); // 自定义电价（阶梯电价第一档）

            List<Telemetry> telemetryList = createMockTelemetryWithPower(DEVICE_ID, 10, 1000.0);

            when(dormRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            when(telemetryRepository.findByDeviceIdAndTsBetween(eq(DEVICE_ID), anyLong(), anyLong()))
                    .thenReturn(telemetryList);
            when(priceRuleRepository.findById(any())).thenReturn(Optional.of(customRule));
            when(priceRuleRepository.findFirstByEnabledTrueOrderByCreatedAtAsc()).thenReturn(Optional.of(customRule));
            when(billRepository.save(any(ElectricityBill.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ElectricityBill bill = billingService.generateMonthlyBill(ROOM_ID, PERIOD);

            // Then
            // 10kWh * 0.6 = 6元
            assertEquals(6.0, bill.getTotalAmount(), 0.1);
            verify(priceRuleRepository).findById(customRuleId);
        }

        @Test
        @DisplayName("TC-BILL-001: 房间无电价规则时使用默认规则")
        void testGenerateMonthlyBill_NoRoomPriceRule_UsesDefault() {
            // Given
            DormRoom room = createRoomWithDevice();
            room.setPriceRuleId(null);

            ElectricityPriceRule defaultRule = createPriceRule();
            List<Telemetry> telemetryList = createMockTelemetryWithPower(DEVICE_ID, 10, 1000.0);

            when(dormRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            when(telemetryRepository.findByDeviceIdAndTsBetween(eq(DEVICE_ID), anyLong(), anyLong()))
                    .thenReturn(telemetryList);
            // 当 priceRuleId 为 null 时，findById 返回 empty
            when(priceRuleRepository.findById(isNull())).thenReturn(Optional.empty());
            when(priceRuleRepository.findFirstByEnabledTrueOrderByCreatedAtAsc())
                    .thenReturn(Optional.of(defaultRule));
            when(billRepository.save(any(ElectricityBill.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ElectricityBill bill = billingService.generateMonthlyBill(ROOM_ID, PERIOD);

            // Then
            assertNotNull(bill);
            verify(priceRuleRepository).findFirstByEnabledTrueOrderByCreatedAtAsc();
        }
    }

    // ==================== TC-BILL-002: 房间不存在 ====================

    @Nested
    @DisplayName("TC-BILL-002: 房间不存在")
    class RoomNotFoundTests {

        @Test
        @DisplayName("TC-BILL-002: 无效房间ID，抛出RuntimeException")
        void testGenerateMonthlyBill_InvalidRoomId_ThrowsException() {
            // Given
            String invalidRoomId = "room_invalid";
            when(dormRoomRepository.findById(invalidRoomId)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> billingService.generateMonthlyBill(invalidRoomId, PERIOD)
            );

            assertTrue(exception.getMessage().contains("Room not found"));
            verify(billRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-BILL-002: 空房间ID，抛出RuntimeException")
        void testGenerateMonthlyBill_EmptyRoomId_ThrowsException() {
            // Given
            when(dormRoomRepository.findById("")).thenReturn(Optional.empty());

            // When & Then
            assertThrows(
                    RuntimeException.class,
                    () -> billingService.generateMonthlyBill("", PERIOD)
            );
        }
    }

    // ==================== TC-BILL-003: 无设备房间 ====================

    @Nested
    @DisplayName("TC-BILL-003: 无设备房间")
    class NoDeviceRoomTests {

        @Test
        @DisplayName("TC-BILL-003: 无关联设备，抛出RuntimeException")
        void testGenerateMonthlyBill_NoDevice_ThrowsException() {
            // Given
            DormRoom roomWithoutDevice = createRoomWithoutDevice();

            when(dormRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(roomWithoutDevice));

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> billingService.generateMonthlyBill(ROOM_ID, PERIOD)
            );

            assertTrue(exception.getMessage().contains("no associated device"));
            verify(telemetryRepository, never()).findByDeviceIdAndTsBetween(anyString(), anyLong(), anyLong());
            verify(billRepository, never()).save(any());
        }
    }

    // ==================== TC-BILL-004: 查询账单 ====================

    @Nested
    @DisplayName("TC-BILL-004: 查询账单")
    class QueryBillTests {

        @Test
        @DisplayName("TC-BILL-004: 有效房间ID，返回账单列表")
        void testGetRoomBills_ValidRoomId_ReturnsBillList() {
            // Given
            List<ElectricityBill> mockBills = createMockBills(ROOM_ID, 3);
            Page<ElectricityBill> mockPage = new PageImpl<>(mockBills);

            when(billRepository.findByRoomIdOrderByPeriodDesc(eq(ROOM_ID), any(Pageable.class)))
                    .thenReturn(mockPage);

            // When
            Page<ElectricityBill> result = billingService.getRoomBills(ROOM_ID, Pageable.unpaged());

            // Then
            assertNotNull(result);
            assertEquals(3, result.getContent().size());

            // 验证所有账单属于正确的房间
            for (ElectricityBill bill : result.getContent()) {
                assertEquals(ROOM_ID, bill.getRoomId());
            }

            verify(billRepository).findByRoomIdOrderByPeriodDesc(eq(ROOM_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("TC-BILL-004: 账单按周期降序排列")
        void testGetRoomBills_SortedByPeriodDesc() {
            // Given
            List<ElectricityBill> mockBills = new ArrayList<>();
            mockBills.add(createBill(ROOM_ID, "2024-03"));
            mockBills.add(createBill(ROOM_ID, "2024-02"));
            mockBills.add(createBill(ROOM_ID, "2024-01"));
            Page<ElectricityBill> mockPage = new PageImpl<>(mockBills);

            when(billRepository.findByRoomIdOrderByPeriodDesc(eq(ROOM_ID), any(Pageable.class)))
                    .thenReturn(mockPage);

            // When
            Page<ElectricityBill> result = billingService.getRoomBills(ROOM_ID, Pageable.unpaged());

            // Then
            assertEquals("2024-03", result.getContent().get(0).getPeriod());
            assertEquals("2024-02", result.getContent().get(1).getPeriod());
            assertEquals("2024-01", result.getContent().get(2).getPeriod());
        }

        @Test
        @DisplayName("TC-BILL-004: 无账单房间返回空列表")
        void testGetRoomBills_NoBills_ReturnsEmptyList() {
            // Given
            Page<ElectricityBill> emptyPage = new PageImpl<>(Collections.emptyList());

            when(billRepository.findByRoomIdOrderByPeriodDesc(eq(ROOM_ID), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When
            Page<ElectricityBill> result = billingService.getRoomBills(ROOM_ID, Pageable.unpaged());

            // Then
            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("TC-BILL-004: 账单包含完整信息")
        void testGetRoomBills_ContainsCompleteInfo() {
            // Given
            List<ElectricityBill> mockBills = Collections.singletonList(createCompleteBill());
            Page<ElectricityBill> mockPage = new PageImpl<>(mockBills);

            when(billRepository.findByRoomIdOrderByPeriodDesc(eq(ROOM_ID), any(Pageable.class)))
                    .thenReturn(mockPage);

            // When
            Page<ElectricityBill> result = billingService.getRoomBills(ROOM_ID, Pageable.unpaged());

            // Then
            ElectricityBill bill = result.getContent().get(0);
            assertNotNull(bill.getId());
            assertNotNull(bill.getRoomId());
            assertNotNull(bill.getPeriod());
            assertNotNull(bill.getStatus());
            assertTrue(bill.getTotalConsumption() >= 0);
            assertTrue(bill.getTotalAmount() >= 0);
            assertTrue(bill.getStartDate() > 0);
            assertTrue(bill.getEndDate() > 0);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建带设备的房间
     */
    private DormRoom createRoomWithDevice() {
        DormRoom room = new DormRoom();
        room.setId(ROOM_ID);
        room.setBuildingId("building_001");
        room.setFloor(1);
        room.setRoomNumber("101");
        room.setDeviceId(DEVICE_ID);
        room.setPriceRuleId(PRICE_RULE_ID);
        room.setStatus("OCCUPIED");
        room.setEnabled(true);
        room.setCreatedAt(System.currentTimeMillis() / 1000);
        return room;
    }

    /**
     * 创建无设备的房间
     */
    private DormRoom createRoomWithoutDevice() {
        DormRoom room = new DormRoom();
        room.setId(ROOM_ID);
        room.setBuildingId("building_001");
        room.setFloor(1);
        room.setRoomNumber("102");
        room.setDeviceId(null);
        room.setStatus("VACANT");
        room.setEnabled(true);
        room.setCreatedAt(System.currentTimeMillis() / 1000);
        return room;
    }

    /**
     * 创建电价规则
     */
    private ElectricityPriceRule createPriceRule() {
        ElectricityPriceRule rule = new ElectricityPriceRule();
        rule.setId(PRICE_RULE_ID);
        rule.setName("基础电价");
        rule.setType("TIER");
        rule.setBasePrice(0.5);
        rule.setTier1Price(0.5);
        rule.setTier1Limit(200);
        rule.setTier2Price(0.6);
        rule.setTier2Limit(400);
        rule.setTier3Price(0.8);
        rule.setEnabled(true);
        rule.setCreatedAt(System.currentTimeMillis() / 1000);
        return rule;
    }

    /**
     * 创建模拟遥测数据
     */
    private List<Telemetry> createMockTelemetry(String deviceId, int count) {
        return createMockTelemetryWithPower(deviceId, count, 500.0);
    }

    /**
     * 创建指定功率的模拟遥测数据
     */
    private List<Telemetry> createMockTelemetryWithPower(String deviceId, int count, double powerW) {
        List<Telemetry> list = new ArrayList<>(count);
        long baseTs = System.currentTimeMillis() / 1000 - 86400;

        for (int i = 0; i < count; i++) {
            Telemetry t = new Telemetry();
            t.setId((long) i + 1);
            t.setDeviceId(deviceId);
            t.setTs(baseTs + i * 3600);
            t.setPowerW(powerW);
            t.setVoltageV(220.0);
            t.setCurrentA(powerW / 220.0);
            list.add(t);
        }
        return list;
    }

    /**
     * 创建模拟账单列表
     */
    private List<ElectricityBill> createMockBills(String roomId, int count) {
        List<ElectricityBill> list = new ArrayList<>(count);
        String[] periods = {"2024-01", "2024-02", "2024-03"};

        for (int i = 0; i < count; i++) {
            list.add(createBill(roomId, periods[i % periods.length]));
        }
        return list;
    }

    /**
     * 创建单个账单
     */
    private ElectricityBill createBill(String roomId, String period) {
        ElectricityBill bill = new ElectricityBill();
        bill.setId("bill_" + period);
        bill.setRoomId(roomId);
        bill.setPeriod(period);
        bill.setTotalConsumption(100.0 + Math.random() * 50);
        bill.setTotalAmount(50.0 + Math.random() * 25);
        bill.setStatus("PENDING");
        bill.setStartDate(System.currentTimeMillis() / 1000 - 2592000);
        bill.setEndDate(System.currentTimeMillis() / 1000);
        bill.setCreatedAt(System.currentTimeMillis() / 1000);
        return bill;
    }

    /**
     * 创建完整的账单
     */
    private ElectricityBill createCompleteBill() {
        ElectricityBill bill = new ElectricityBill();
        bill.setId("bill_complete");
        bill.setRoomId(ROOM_ID);
        bill.setPeriod("2024-01");
        bill.setTotalConsumption(150.5);
        bill.setTotalAmount(75.25);
        bill.setStatus("PENDING");
        bill.setStartDate(1704067200L); // 2024-01-01
        bill.setEndDate(1706745600L);   // 2024-02-01
        bill.setCreatedAt(1706745600L);
        return bill;
    }
}