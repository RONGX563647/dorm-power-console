package com.dormpower.service;

import com.dormpower.exception.BusinessException;
import com.dormpower.exception.ResourceNotFoundException;
import com.dormpower.model.DormRoom;
import com.dormpower.model.RechargeRecord;
import com.dormpower.model.RoomBalance;
import com.dormpower.repository.DormRoomRepository;
import com.dormpower.repository.ElectricityBillRepository;
import com.dormpower.repository.ElectricityPriceRuleRepository;
import com.dormpower.repository.RechargeRecordRepository;
import com.dormpower.repository.RoomBalanceRepository;
import com.dormpower.repository.TelemetryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 充值服务单元测试
 *
 * 测试用例覆盖：
 * - TC-RECH-001: 正常充值
 * - TC-RECH-002: 房间不存在
 * - TC-RECH-003: 金额过小
 * - TC-RECH-004: 金额过大
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class BillingServiceRechargeTest {

    @Mock
    private DormRoomRepository dormRoomRepository;

    @Mock
    private RoomBalanceRepository roomBalanceRepository;

    @Mock
    private RechargeRecordRepository rechargeRecordRepository;

    @Mock
    private ElectricityPriceRuleRepository priceRuleRepository;

    @Mock
    private ElectricityBillRepository billRepository;

    @Mock
    private TelemetryRepository telemetryRepository;

    @InjectMocks
    private BillingService billingService;

    // ==================== TC-RECH-001: 正常充值 ====================

    @Nested
    @DisplayName("TC-RECH-001: 正常充值")
    class RechargeSuccessTests {

        @Test
        @DisplayName("TC-RECH-001-01: 有效参数充值成功")
        void testRecharge_ValidParams_Success() {
            // Given
            String roomId = "room_001";
            double amount = 100.0;
            String paymentMethod = "WECHAT";
            String operator = "admin";

            DormRoom room = createDormRoom(roomId);
            RoomBalance balance = createRoomBalance(roomId, 50.0);

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
            when(roomBalanceRepository.findByRoomId(roomId)).thenReturn(Optional.of(balance));
            when(roomBalanceRepository.save(any(RoomBalance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(rechargeRecordRepository.save(any(RechargeRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            RechargeRecord record = billingService.recharge(roomId, amount, paymentMethod, operator);

            // Then
            assertNotNull(record, "充值记录不应为空");
            assertEquals(roomId, record.getRoomId(), "房间ID应匹配");
            assertEquals(amount, record.getAmount(), 0.01, "充值金额应匹配");
            assertEquals(50.0, record.getBalanceBefore(), 0.01, "充值前余额应为50元");
            assertEquals(150.0, record.getBalanceAfter(), 0.01, "充值后余额应为150元");
            assertEquals(paymentMethod, record.getPaymentMethod(), "支付方式应匹配");
            assertEquals("SUCCESS", record.getStatus(), "状态应为SUCCESS");
        }

        @Test
        @DisplayName("TC-RECH-001-02: 充值后余额正确更新")
        void testRecharge_BalanceUpdated() {
            // Given
            String roomId = "room_002";
            double amount = 50.0;
            RoomBalance balance = createRoomBalance(roomId, 20.0);

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(createDormRoom(roomId)));
            when(roomBalanceRepository.findByRoomId(roomId)).thenReturn(Optional.of(balance));
            when(roomBalanceRepository.save(any(RoomBalance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(rechargeRecordRepository.save(any(RechargeRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            billingService.recharge(roomId, amount, "CASH", "admin");

            // Then
            ArgumentCaptor<RoomBalance> captor = ArgumentCaptor.forClass(RoomBalance.class);
            verify(roomBalanceRepository).save(captor.capture());

            RoomBalance savedBalance = captor.getValue();
            assertEquals(70.0, savedBalance.getBalance(), 0.01, "余额应更新为70元");
            assertEquals(70.0, savedBalance.getTotalRecharged(), 0.01, "累计充值应为70元(20+50)");
            assertFalse(savedBalance.isWarningSent(), "预警标志应重置");
        }

        @Test
        @DisplayName("TC-RECH-001-03: 新房间首次充值创建余额记录")
        void testRecharge_NewRoom_CreatesBalance() {
            // Given
            String roomId = "room_new";
            double amount = 200.0;

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(createDormRoom(roomId)));
            when(roomBalanceRepository.findByRoomId(roomId)).thenReturn(Optional.empty());
            when(roomBalanceRepository.save(any(RoomBalance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(rechargeRecordRepository.save(any(RechargeRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            RechargeRecord record = billingService.recharge(roomId, amount, "ALIPAY", "admin");

            // Then
            assertNotNull(record, "充值记录不应为空");
            assertEquals(200.0, record.getBalanceAfter(), 0.01, "新房间充值后余额应为200元");
            // 验证创建了新的余额记录
            verify(roomBalanceRepository, times(2)).save(any(RoomBalance.class));
        }

        @Test
        @DisplayName("TC-RECH-001-04: 最小金额充值成功")
        void testRecharge_MinAmount_Success() {
            // Given
            String roomId = "room_003";
            double minAmount = 1.0;

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(createDormRoom(roomId)));
            when(roomBalanceRepository.findByRoomId(roomId)).thenReturn(Optional.of(createRoomBalance(roomId, 0)));
            when(roomBalanceRepository.save(any(RoomBalance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(rechargeRecordRepository.save(any(RechargeRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            RechargeRecord record = billingService.recharge(roomId, minAmount, "WECHAT", "admin");

            // Then
            assertNotNull(record, "最小金额充值应成功");
            assertEquals(1.0, record.getAmount(), 0.01, "充值金额应为1元");
        }

        @Test
        @DisplayName("TC-RECH-001-05: 最大金额充值成功")
        void testRecharge_MaxAmount_Success() {
            // Given
            String roomId = "room_004";
            double maxAmount = 10000.0;

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(createDormRoom(roomId)));
            when(roomBalanceRepository.findByRoomId(roomId)).thenReturn(Optional.of(createRoomBalance(roomId, 0)));
            when(roomBalanceRepository.save(any(RoomBalance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(rechargeRecordRepository.save(any(RechargeRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            RechargeRecord record = billingService.recharge(roomId, maxAmount, "ALIPAY", "admin");

            // Then
            assertNotNull(record, "最大金额充值应成功");
            assertEquals(10000.0, record.getAmount(), 0.01, "充值金额应为10000元");
        }

        @Test
        @DisplayName("TC-RECH-001-06: 充值记录包含交易流水号")
        void testRecharge_HasTransactionId() {
            // Given
            String roomId = "room_005";

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(createDormRoom(roomId)));
            when(roomBalanceRepository.findByRoomId(roomId)).thenReturn(Optional.of(createRoomBalance(roomId, 0)));
            when(roomBalanceRepository.save(any(RoomBalance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(rechargeRecordRepository.save(any(RechargeRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            RechargeRecord record = billingService.recharge(roomId, 100.0, "WECHAT", "admin");

            // Then
            assertNotNull(record.getTransactionId(), "交易流水号不应为空");
            assertTrue(record.getTransactionId().startsWith("TXN"), "交易流水号应以TXN开头");
        }
    }

    // ==================== TC-RECH-002: 房间不存在 ====================

    @Nested
    @DisplayName("TC-RECH-002: 房间不存在")
    class RechargeRoomNotFoundTests {

        @Test
        @DisplayName("TC-RECH-002-01: 无效房间ID抛出ResourceNotFoundException")
        void testRecharge_InvalidRoomId_ThrowsResourceNotFoundException() {
            // Given
            String invalidRoomId = "nonexistent_room";

            when(dormRoomRepository.findById(invalidRoomId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                billingService.recharge(invalidRoomId, 100.0, "WECHAT", "admin");
            }, "无效房间ID应抛出ResourceNotFoundException");

            // 验证未执行余额相关操作
            verify(roomBalanceRepository, never()).save(any());
            verify(rechargeRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-RECH-002-02: 空房间ID抛出ResourceNotFoundException")
        void testRecharge_EmptyRoomId_ThrowsResourceNotFoundException() {
            // Given
            String emptyRoomId = "";

            when(dormRoomRepository.findById(emptyRoomId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                billingService.recharge(emptyRoomId, 100.0, "WECHAT", "admin");
            }, "空房间ID应抛出ResourceNotFoundException");
        }

        @Test
        @DisplayName("TC-RECH-002-03: 异常消息包含房间ID")
        void testRecharge_InvalidRoomId_ErrorMessageContainsRoomId() {
            // Given
            String invalidRoomId = "room_invalid_123";

            when(dormRoomRepository.findById(invalidRoomId)).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                billingService.recharge(invalidRoomId, 100.0, "WECHAT", "admin");
            });

            assertTrue(exception.getMessage().contains(invalidRoomId),
                    "异常消息应包含房间ID");
        }
    }

    // ==================== TC-RECH-003: 金额过小 ====================

    @Nested
    @DisplayName("TC-RECH-003: 金额过小")
    class RechargeAmountTooSmallTests {

        @Test
        @DisplayName("TC-RECH-003-01: 金额0.5元抛出BusinessException")
        void testRecharge_AmountTooSmall_ThrowsBusinessException() {
            // Given
            String roomId = "room_001";
            double smallAmount = 0.5;

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(createDormRoom(roomId)));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                billingService.recharge(roomId, smallAmount, "WECHAT", "admin");
            }, "金额过小应抛出BusinessException");

            assertTrue(exception.getMessage().contains("不能小于"),
                    "异常消息应说明金额限制");
        }

        @Test
        @DisplayName("TC-RECH-003-02: 金额0元抛出BusinessException")
        void testRecharge_ZeroAmount_ThrowsBusinessException() {
            // Given
            String roomId = "room_002";
            double zeroAmount = 0.0;

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(createDormRoom(roomId)));

            // When & Then
            assertThrows(BusinessException.class, () -> {
                billingService.recharge(roomId, zeroAmount, "WECHAT", "admin");
            }, "0元充值应抛出BusinessException");
        }

        @Test
        @DisplayName("TC-RECH-003-03: 负数金额抛出BusinessException")
        void testRecharge_NegativeAmount_ThrowsBusinessException() {
            // Given
            String roomId = "room_003";
            double negativeAmount = -10.0;

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(createDormRoom(roomId)));

            // When & Then
            assertThrows(BusinessException.class, () -> {
                billingService.recharge(roomId, negativeAmount, "WECHAT", "admin");
            }, "负数金额应抛出BusinessException");
        }

        @Test
        @DisplayName("TC-RECH-003-04: 金额刚好小于最小值抛出异常")
        void testRecharge_AmountJustBelowMin_ThrowsBusinessException() {
            // Given
            String roomId = "room_004";
            double amountBelowMin = 0.99;

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(createDormRoom(roomId)));

            // When & Then
            assertThrows(BusinessException.class, () -> {
                billingService.recharge(roomId, amountBelowMin, "WECHAT", "admin");
            }, "金额0.99元应抛出BusinessException");
        }

        @Test
        @DisplayName("TC-RECH-003-05: 金额过小不创建充值记录")
        void testRecharge_AmountTooSmall_NoRecordCreated() {
            // Given
            String roomId = "room_005";

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(createDormRoom(roomId)));

            // When
            try {
                billingService.recharge(roomId, 0.5, "WECHAT", "admin");
            } catch (BusinessException e) {
                // 预期异常
            }

            // Then - 不应创建充值记录
            verify(rechargeRecordRepository, never()).save(any());
            verify(roomBalanceRepository, never()).save(any());
        }
    }

    // ==================== TC-RECH-004: 金额过大 ====================

    @Nested
    @DisplayName("TC-RECH-004: 金额过大")
    class RechargeAmountTooLargeTests {

        @Test
        @DisplayName("TC-RECH-004-01: 金额20000元抛出BusinessException")
        void testRecharge_AmountTooLarge_ThrowsBusinessException() {
            // Given
            String roomId = "room_001";
            double largeAmount = 20000.0;

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(createDormRoom(roomId)));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                billingService.recharge(roomId, largeAmount, "WECHAT", "admin");
            }, "金额过大应抛出BusinessException");

            assertTrue(exception.getMessage().contains("不能超过"),
                    "异常消息应说明金额限制");
        }

        @Test
        @DisplayName("TC-RECH-004-02: 金额刚好大于最大值抛出异常")
        void testRecharge_AmountJustAboveMax_ThrowsBusinessException() {
            // Given
            String roomId = "room_002";
            double amountAboveMax = 10000.01;

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(createDormRoom(roomId)));

            // When & Then
            assertThrows(BusinessException.class, () -> {
                billingService.recharge(roomId, amountAboveMax, "WECHAT", "admin");
            }, "金额10000.01元应抛出BusinessException");
        }

        @Test
        @DisplayName("TC-RECH-004-03: 超大金额抛出BusinessException")
        void testRecharge_ExtremelyLargeAmount_ThrowsBusinessException() {
            // Given
            String roomId = "room_003";
            double extremeAmount = 1000000.0;

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(createDormRoom(roomId)));

            // When & Then
            assertThrows(BusinessException.class, () -> {
                billingService.recharge(roomId, extremeAmount, "WECHAT", "admin");
            }, "超大金额应抛出BusinessException");
        }

        @Test
        @DisplayName("TC-RECH-004-04: 金额过大不创建充值记录")
        void testRecharge_AmountTooLarge_NoRecordCreated() {
            // Given
            String roomId = "room_004";

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(createDormRoom(roomId)));

            // When
            try {
                billingService.recharge(roomId, 20000.0, "WECHAT", "admin");
            } catch (BusinessException e) {
                // 预期异常
            }

            // Then - 不应创建充值记录
            verify(rechargeRecordRepository, never()).save(any());
            verify(roomBalanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-RECH-004-05: 边界值15000元抛出异常")
        void testRecharge_Amount15000_ThrowsBusinessException() {
            // Given
            String roomId = "room_005";
            double amount = 15000.0;

            when(dormRoomRepository.findById(roomId)).thenReturn(Optional.of(createDormRoom(roomId)));

            // When & Then
            assertThrows(BusinessException.class, () -> {
                billingService.recharge(roomId, amount, "ALIPAY", "admin");
            }, "金额15000元应抛出BusinessException");
        }
    }

    // ==================== 辅助方法 ====================

    private DormRoom createDormRoom(String roomId) {
        DormRoom room = new DormRoom();
        room.setId(roomId);
        room.setBuildingId("building_001");
        room.setFloor(1);
        room.setRoomNumber("101");
        room.setRoomType("DOUBLE");
        room.setCapacity(2);
        room.setCurrentOccupants(2);
        room.setStatus("OCCUPIED");
        room.setEnabled(true);
        room.setCreatedAt(System.currentTimeMillis() / 1000);
        return room;
    }

    private RoomBalance createRoomBalance(String roomId, double initialBalance) {
        RoomBalance balance = new RoomBalance();
        balance.setId("bal_" + roomId);
        balance.setRoomId(roomId);
        balance.setBalance(initialBalance);
        balance.setTotalRecharged(initialBalance);
        balance.setTotalConsumed(0);
        balance.setWarningThreshold(10);
        balance.setWarningSent(false);
        balance.setAutoCutoff(true);
        balance.setLastRechargeAt(System.currentTimeMillis() / 1000);
        balance.setLastConsumptionAt(System.currentTimeMillis() / 1000);
        balance.setCreatedAt(System.currentTimeMillis() / 1000);
        return balance;
    }
}