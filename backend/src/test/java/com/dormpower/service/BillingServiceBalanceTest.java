package com.dormpower.service;

import com.dormpower.model.RoomBalance;
import com.dormpower.repository.RoomBalanceRepository;
import org.junit.jupiter.api.DisplayName;
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
 * 房间余额单元测试
 *
 * 测试用例覆盖：
 * - TC-BAL-001: 查询余额
 * - TC-BAL-002: 扣除电费
 * - TC-BAL-003: 余额不足
 * - TC-BAL-004: 新房间余额
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class BillingServiceBalanceTest {

    @Mock
    private RoomBalanceRepository roomBalanceRepository;

    @InjectMocks
    private BillingService billingService;

    private static final String ROOM_ID = "A1-301";
    private static final double INITIAL_BALANCE = 100.0;
    private static final double DEDUCT_AMOUNT = 25.0;

    // ==================== TC-BAL-001: 查询余额 ====================

    @Test
    @DisplayName("TC-BAL-001: 查询余额 - 有效房间ID，返回余额信息")
    void testGetRoomBalance_ExistingRoom_ReturnsBalanceInfo() {
        // Given
        RoomBalance existingBalance = createRoomBalance(ROOM_ID, INITIAL_BALANCE);
        when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(existingBalance));

        // When
        RoomBalance result = billingService.getRoomBalance(ROOM_ID);

        // Then
        assertNotNull(result, "应返回余额信息");
        assertEquals(ROOM_ID, result.getRoomId());
        assertEquals(INITIAL_BALANCE, result.getBalance());
        assertEquals(INITIAL_BALANCE, result.getTotalRecharged());
        assertFalse(result.isWarningSent());
        assertTrue(result.isAutoCutoff());

        // 验证没有创建新记录
        verify(roomBalanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-BAL-001: 查询余额 - 验证余额详细信息正确")
    void testGetRoomBalance_VerifyBalanceDetails() {
        // Given
        RoomBalance existingBalance = createRoomBalance(ROOM_ID, 50.0);
        existingBalance.setTotalConsumed(30.0);
        existingBalance.setWarningThreshold(10.0);
        existingBalance.setWarningSent(true);

        when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(existingBalance));

        // When
        RoomBalance result = billingService.getRoomBalance(ROOM_ID);

        // Then
        assertEquals(50.0, result.getBalance());
        assertEquals(50.0, result.getTotalRecharged());
        assertEquals(30.0, result.getTotalConsumed());
        assertEquals(10.0, result.getWarningThreshold());
        assertTrue(result.isWarningSent());
    }

    // ==================== TC-BAL-002: 扣除电费 ====================

    @Test
    @DisplayName("TC-BAL-002: 扣除电费 - 余额充足，扣费成功")
    void testDeductElectricityFee_SufficientBalance_DeductionSuccess() {
        // Given
        RoomBalance balance = createRoomBalance(ROOM_ID, INITIAL_BALANCE);
        when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(balance));
        when(roomBalanceRepository.save(any(RoomBalance.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        boolean result = billingService.deductElectricityFee(ROOM_ID, DEDUCT_AMOUNT);

        // Then
        assertTrue(result, "余额充足时应扣费成功");

        // 验证余额更新
        ArgumentCaptor<RoomBalance> captor = ArgumentCaptor.forClass(RoomBalance.class);
        verify(roomBalanceRepository).save(captor.capture());

        RoomBalance savedBalance = captor.getValue();
        assertEquals(INITIAL_BALANCE - DEDUCT_AMOUNT, savedBalance.getBalance(), 0.001, "余额应减少");
        assertEquals(DEDUCT_AMOUNT, savedBalance.getTotalConsumed(), 0.001, "累计消费应增加");
        assertNotNull(savedBalance.getLastConsumptionAt(), "最后消费时间应更新");
        assertNotNull(savedBalance.getUpdatedAt(), "更新时间应设置");
    }

    @Test
    @DisplayName("TC-BAL-002: 扣除电费 - 扣费后余额刚好为零")
    void testDeductElectricityFee_DeductToZero_Success() {
        // Given
        RoomBalance balance = createRoomBalance(ROOM_ID, DEDUCT_AMOUNT);
        when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(balance));
        when(roomBalanceRepository.save(any(RoomBalance.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        boolean result = billingService.deductElectricityFee(ROOM_ID, DEDUCT_AMOUNT);

        // Then
        assertTrue(result);

        ArgumentCaptor<RoomBalance> captor = ArgumentCaptor.forClass(RoomBalance.class);
        verify(roomBalanceRepository).save(captor.capture());

        RoomBalance savedBalance = captor.getValue();
        assertEquals(0.0, savedBalance.getBalance(), 0.001, "余额应为零");
    }

    @Test
    @DisplayName("TC-BAL-002: 扣除电费 - 多次扣费累计正确")
    void testDeductElectricityFee_MultipleDeductions_CumulativeCorrect() {
        // Given
        RoomBalance balance = createRoomBalance(ROOM_ID, 100.0);
        balance.setTotalConsumed(20.0); // 已有消费

        when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(balance));
        when(roomBalanceRepository.save(any(RoomBalance.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        boolean result = billingService.deductElectricityFee(ROOM_ID, 30.0);

        // Then
        assertTrue(result);

        ArgumentCaptor<RoomBalance> captor = ArgumentCaptor.forClass(RoomBalance.class);
        verify(roomBalanceRepository).save(captor.capture());

        RoomBalance savedBalance = captor.getValue();
        assertEquals(70.0, savedBalance.getBalance(), 0.001, "余额应为 100-30=70");
        assertEquals(50.0, savedBalance.getTotalConsumed(), 0.001, "累计消费应为 20+30=50");
    }

    // ==================== TC-BAL-003: 余额不足 ====================

    @Test
    @DisplayName("TC-BAL-003: 余额不足 - 扣费失败，返回false")
    void testDeductElectricityFee_InsufficientBalance_DeductionFails() {
        // Given
        RoomBalance balance = createRoomBalance(ROOM_ID, 10.0); // 余额只有 10
        when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(balance));

        // When
        boolean result = billingService.deductElectricityFee(ROOM_ID, DEDUCT_AMOUNT); // 尝试扣除 25

        // Then
        assertFalse(result, "余额不足时应扣费失败");

        // 验证没有保存操作
        verify(roomBalanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-BAL-003: 余额不足 - 零余额扣费失败")
    void testDeductElectricityFee_ZeroBalance_DeductionFails() {
        // Given
        RoomBalance balance = createRoomBalance(ROOM_ID, 0.0);
        when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(balance));

        // When
        boolean result = billingService.deductElectricityFee(ROOM_ID, 1.0);

        // Then
        assertFalse(result, "零余额时应扣费失败");
        verify(roomBalanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-BAL-003: 余额不足 - 扣费金额超过余额")
    void testDeductElectricityFee_AmountExceedsBalance_DeductionFails() {
        // Given
        RoomBalance balance = createRoomBalance(ROOM_ID, 50.0);
        when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(balance));

        // When
        boolean result = billingService.deductElectricityFee(ROOM_ID, 100.0);

        // Then
        assertFalse(result, "扣费金额超过余额时应失败");
    }

    @Test
    @DisplayName("TC-BAL-003: 余额不足 - 无余额记录抛出异常")
    void testDeductElectricityFee_NoBalanceRecord_ThrowsException() {
        // Given
        when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            billingService.deductElectricityFee(ROOM_ID, DEDUCT_AMOUNT);
        }, "无余额记录时应抛出异常");

        verify(roomBalanceRepository, never()).save(any());
    }

    // ==================== TC-BAL-004: 新房间余额 ====================

    @Test
    @DisplayName("TC-BAL-004: 新房间余额 - 无余额记录，自动创建余额记录")
    void testGetRoomBalance_NewRoom_AutoCreateBalanceRecord() {
        // Given
        when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.empty());
        when(roomBalanceRepository.save(any(RoomBalance.class))).thenAnswer(inv -> {
            RoomBalance saved = inv.getArgument(0);
            saved.setId("bal_test123");
            return saved;
        });

        // When
        RoomBalance result = billingService.getRoomBalance(ROOM_ID);

        // Then
        assertNotNull(result, "应自动创建余额记录");
        assertEquals(ROOM_ID, result.getRoomId());
        assertEquals(0.0, result.getBalance(), "新房间初始余额应为0");
        assertEquals(0.0, result.getTotalRecharged(), "累计充值应为0");
        assertEquals(0.0, result.getTotalConsumed(), "累计消费应为0");
        assertEquals(10.0, result.getWarningThreshold(), "默认预警阈值应为10");
        assertFalse(result.isWarningSent(), "预警发送状态应为false");
        assertTrue(result.isAutoCutoff(), "默认开启自动断电");

        // 验证保存操作
        verify(roomBalanceRepository).save(any(RoomBalance.class));
    }

    @Test
    @DisplayName("TC-BAL-004: 新房间余额 - 验证自动创建记录的默认值")
    void testGetRoomBalance_NewRoom_VerifyDefaultValues() {
        // Given
        when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.empty());
        when(roomBalanceRepository.save(any(RoomBalance.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        billingService.getRoomBalance(ROOM_ID);

        // Then
        ArgumentCaptor<RoomBalance> captor = ArgumentCaptor.forClass(RoomBalance.class);
        verify(roomBalanceRepository).save(captor.capture());

        RoomBalance savedBalance = captor.getValue();
        assertNotNull(savedBalance.getId(), "应生成ID");
        assertTrue(savedBalance.getId().startsWith("bal_"), "ID应以'bal_'开头");
        assertNotNull(savedBalance.getLastRechargeAt(), "最后充值时间应设置");
        assertNotNull(savedBalance.getLastConsumptionAt(), "最后消费时间应设置");
        assertNotNull(savedBalance.getCreatedAt(), "创建时间应设置");
    }

    @Test
    @DisplayName("TC-BAL-004: 新房间余额 - 不同房间创建独立记录")
    void testGetRoomBalance_DifferentRooms_SeparateRecords() {
        // Given
        String room1 = "A1-301";
        String room2 = "A1-302";

        when(roomBalanceRepository.findByRoomId(room1)).thenReturn(Optional.empty());
        when(roomBalanceRepository.findByRoomId(room2)).thenReturn(Optional.empty());
        when(roomBalanceRepository.save(any(RoomBalance.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        RoomBalance result1 = billingService.getRoomBalance(room1);
        RoomBalance result2 = billingService.getRoomBalance(room2);

        // Then
        assertEquals(room1, result1.getRoomId());
        assertEquals(room2, result2.getRoomId());
        assertNotSame(result1, result2, "不同房间应有独立的余额记录");
    }

    // ==================== 辅助方法 ====================

    private RoomBalance createRoomBalance(String roomId, double balance) {
        RoomBalance roomBalance = new RoomBalance();
        roomBalance.setId("bal_" + roomId.replace("-", ""));
        roomBalance.setRoomId(roomId);
        roomBalance.setBalance(balance);
        roomBalance.setTotalRecharged(balance);
        roomBalance.setTotalConsumed(0);
        roomBalance.setWarningThreshold(10);
        roomBalance.setWarningSent(false);
        roomBalance.setAutoCutoff(true);
        roomBalance.setLastRechargeAt(System.currentTimeMillis() / 1000);
        roomBalance.setLastConsumptionAt(System.currentTimeMillis() / 1000);
        roomBalance.setCreatedAt(System.currentTimeMillis() / 1000 - 86400);
        return roomBalance;
    }

}