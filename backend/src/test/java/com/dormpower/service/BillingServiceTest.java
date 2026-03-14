package com.dormpower.service;

import com.dormpower.exception.BusinessException;
import com.dormpower.exception.ResourceNotFoundException;
import com.dormpower.model.*;
import com.dormpower.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 计费服务单元测试
 *
 * 测试用例覆盖：
 * - TC-PAY-001: 正常缴费
 * - TC-PAY-002: 账单不存在
 * - TC-PAY-003: 重复缴费
 * - TC-RECHARGE-001: 正常充值
 * - TC-RECHARGE-002: 充值金额验证
 * - TC-BILL-001: 账单生成
 *
 * @author dormpower team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private ElectricityBillRepository billRepository;

    @Mock
    private ElectricityPriceRuleRepository priceRuleRepository;

    @Mock
    private RechargeRecordRepository rechargeRecordRepository;

    @Mock
    private RoomBalanceRepository roomBalanceRepository;

    @Mock
    private DormRoomRepository dormRoomRepository;

    @Mock
    private TelemetryRepository telemetryRepository;

    @InjectMocks
    private BillingService billingService;

    private static final String BILL_ID = "bill_001";
    private static final String ROOM_ID = "room_001";
    private static final String PAYMENT_METHOD = "WECHAT";

    // ==================== TC-PAY-001: 正常缴费 ====================

    @Nested
    @DisplayName("TC-PAY-001: 正常缴费")
    class PayBillSuccessTests {

        @Test
        @DisplayName("TC-PAY-001-01: 有效账单ID，缴费成功")
        void testPayBill_Success() {
            // Given
            ElectricityBill bill = createPendingBill(BILL_ID, ROOM_ID);

            when(billRepository.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(billRepository.save(any(ElectricityBill.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ElectricityBill result = billingService.payBill(BILL_ID, PAYMENT_METHOD, "admin");

            // Then
            assertEquals("PAID", result.getStatus(), "账单状态应更新为PAID");
            assertEquals(PAYMENT_METHOD, result.getPaymentMethod(), "支付方式应正确设置");
            assertTrue(result.getPaidAt() > 0, "缴费时间应被设置");
            assertNotNull(result.getTransactionId(), "交易流水号应被生成");
        }

        @Test
        @DisplayName("TC-PAY-001-02: 缴费后transactionId格式正确")
        void testPayBill_TransactionIdFormat() {
            // Given
            ElectricityBill bill = createPendingBill(BILL_ID, ROOM_ID);

            when(billRepository.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(billRepository.save(any(ElectricityBill.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ElectricityBill result = billingService.payBill(BILL_ID, PAYMENT_METHOD, "admin");

            // Then
            assertTrue(result.getTransactionId().startsWith("TXN"), "交易流水号应以TXN开头");
        }

        @Test
        @DisplayName("TC-PAY-001-03: 缴费时间戳正确")
        void testPayBill_PaidAtTimestamp() {
            // Given
            ElectricityBill bill = createPendingBill(BILL_ID, ROOM_ID);

            when(billRepository.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(billRepository.save(any(ElectricityBill.class))).thenAnswer(inv -> inv.getArgument(0));

            long beforeTime = System.currentTimeMillis() / 1000;

            // When
            ElectricityBill result = billingService.payBill(BILL_ID, PAYMENT_METHOD, "admin");

            long afterTime = System.currentTimeMillis() / 1000;

            // Then
            assertTrue(result.getPaidAt() >= beforeTime && result.getPaidAt() <= afterTime,
                    "缴费时间应在调用时间范围内");
        }

        @Test
        @DisplayName("TC-PAY-001-04: 不同支付方式都能正常缴费")
        void testPayBill_DifferentPaymentMethods() {
            // Given
            String[] paymentMethods = {"WECHAT", "ALIPAY", "CASH"};

            for (String method : paymentMethods) {
                ElectricityBill bill = createPendingBill("bill_" + method, ROOM_ID);

                when(billRepository.findById("bill_" + method)).thenReturn(Optional.of(bill));
                when(billRepository.save(any(ElectricityBill.class))).thenAnswer(inv -> inv.getArgument(0));

                // When
                ElectricityBill result = billingService.payBill("bill_" + method, method, "admin");

                // Then
                assertEquals("PAID", result.getStatus());
                assertEquals(method, result.getPaymentMethod());
            }
        }
    }

    // ==================== TC-PAY-002: 账单不存在 ====================

    @Nested
    @DisplayName("TC-PAY-002: 账单不存在")
    class PayBillNotFoundTests {

        @Test
        @DisplayName("TC-PAY-002-01: 无效账单ID，抛出异常")
        void testPayBill_BillNotFound_ThrowsException() {
            // Given
            String invalidBillId = "nonexistent_bill";

            when(billRepository.findById(invalidBillId)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                billingService.payBill(invalidBillId, PAYMENT_METHOD, "admin");
            });

            assertTrue(exception.getMessage().contains("Bill not found"));
        }

        @Test
        @DisplayName("TC-PAY-002-02: 空账单ID处理")
        void testPayBill_EmptyBillId() {
            // Given
            when(billRepository.findById("")).thenReturn(Optional.empty());

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                billingService.payBill("", PAYMENT_METHOD, "admin");
            });
        }

        @Test
        @DisplayName("TC-PAY-002-03: null账单ID处理")
        void testPayBill_NullBillId() {
            // Given
            when(billRepository.findById(null)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                billingService.payBill(null, PAYMENT_METHOD, "admin");
            });
        }
    }

    // ==================== TC-PAY-003: 重复缴费 ====================

    @Nested
    @DisplayName("TC-PAY-003: 重复缴费")
    class PayBillAlreadyPaidTests {

        @Test
        @DisplayName("TC-PAY-003-01: 已缴费账单，抛出异常")
        void testPayBill_AlreadyPaid_ThrowsException() {
            // Given
            ElectricityBill paidBill = createPaidBill(BILL_ID, ROOM_ID);

            when(billRepository.findById(BILL_ID)).thenReturn(Optional.of(paidBill));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                billingService.payBill(BILL_ID, PAYMENT_METHOD, "admin");
            });

            assertTrue(exception.getMessage().contains("not pending"));
        }

        @Test
        @DisplayName("TC-PAY-003-02: 已缴费账单不执行保存操作")
        void testPayBill_AlreadyPaid_NoSave() {
            // Given
            ElectricityBill paidBill = createPaidBill(BILL_ID, ROOM_ID);

            when(billRepository.findById(BILL_ID)).thenReturn(Optional.of(paidBill));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                billingService.payBill(BILL_ID, PAYMENT_METHOD, "admin");
            });

            verify(billRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-PAY-003-03: 其他状态账单（OVERDUE）也不能缴费")
        void testPayBill_OverdueBill_ThrowsException() {
            // Given
            ElectricityBill overdueBill = createPendingBill(BILL_ID, ROOM_ID);
            overdueBill.setStatus("OVERDUE");

            when(billRepository.findById(BILL_ID)).thenReturn(Optional.of(overdueBill));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                billingService.payBill(BILL_ID, PAYMENT_METHOD, "admin");
            });
        }
    }

    // ==================== TC-RECHARGE-001: 正常充值 ====================

    @Nested
    @DisplayName("TC-RECHARGE-001: 正常充值")
    class RechargeSuccessTests {

        @Test
        @DisplayName("TC-RECHARGE-001-01: 正常充值成功")
        void testRecharge_Success() {
            // Given
            DormRoom room = createDormRoom(ROOM_ID);
            RoomBalance balance = createRoomBalance(ROOM_ID, 50.0);

            when(dormRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(balance));
            when(roomBalanceRepository.save(any(RoomBalance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(rechargeRecordRepository.save(any(RechargeRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            RechargeRecord result = billingService.recharge(ROOM_ID, 100.0, PAYMENT_METHOD, "admin");

            // Then
            assertEquals(ROOM_ID, result.getRoomId());
            assertEquals(100.0, result.getAmount());
            assertEquals(50.0, result.getBalanceBefore());
            assertEquals(150.0, result.getBalanceAfter());
            assertEquals("SUCCESS", result.getStatus());
        }

        @Test
        @DisplayName("TC-RECHARGE-001-02: 房间无余额记录时自动创建")
        void testRecharge_CreateNewBalance() {
            // Given
            DormRoom room = createDormRoom(ROOM_ID);

            when(dormRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
            when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.empty());
            when(roomBalanceRepository.save(any(RoomBalance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(rechargeRecordRepository.save(any(RechargeRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            RechargeRecord result = billingService.recharge(ROOM_ID, 50.0, PAYMENT_METHOD, "admin");

            // Then
            assertNotNull(result);
            verify(roomBalanceRepository, times(2)).save(any(RoomBalance.class)); // 创建 + 更新
        }
    }

    // ==================== TC-RECHARGE-002: 充值金额验证 ====================

    @Nested
    @DisplayName("TC-RECHARGE-002: 充值金额验证")
    class RechargeValidationTests {

        @Test
        @DisplayName("TC-RECHARGE-002-01: 充值金额小于最小值，抛出BusinessException")
        void testRecharge_AmountTooSmall_ThrowsException() {
            // Given
            DormRoom room = createDormRoom(ROOM_ID);

            when(dormRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                billingService.recharge(ROOM_ID, 0.5, PAYMENT_METHOD, "admin");
            });

            assertTrue(exception.getMessage().contains("不能小于"));
        }

        @Test
        @DisplayName("TC-RECHARGE-002-02: 充值金额超过最大值，抛出BusinessException")
        void testRecharge_AmountTooLarge_ThrowsException() {
            // Given
            DormRoom room = createDormRoom(ROOM_ID);

            when(dormRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () -> {
                billingService.recharge(ROOM_ID, 15000.0, PAYMENT_METHOD, "admin");
            });

            assertTrue(exception.getMessage().contains("不能超过"));
        }

        @Test
        @DisplayName("TC-RECHARGE-002-03: 房间不存在，抛出ResourceNotFoundException")
        void testRecharge_RoomNotFound_ThrowsException() {
            // Given
            when(dormRoomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () -> {
                billingService.recharge(ROOM_ID, 100.0, PAYMENT_METHOD, "admin");
            });
        }
    }

    // ==================== TC-BILL-001: 获取账单 ====================

    @Nested
    @DisplayName("TC-BILL-001: 获取账单")
    class GetBillTests {

        @Test
        @DisplayName("TC-BILL-001-01: 获取房间账单列表")
        void testGetRoomBills_Success() {
            // Given
            List<ElectricityBill> bills = List.of(
                    createPendingBill("bill_001", ROOM_ID),
                    createPaidBill("bill_002", ROOM_ID)
            );
            Page<ElectricityBill> billPage = new PageImpl<>(bills);

            when(billRepository.findByRoomIdOrderByPeriodDesc(eq(ROOM_ID), any(Pageable.class)))
                    .thenReturn(billPage);

            // When
            Page<ElectricityBill> result = billingService.getRoomBills(ROOM_ID, Pageable.unpaged());

            // Then
            assertEquals(2, result.getTotalElements());
        }

        @Test
        @DisplayName("TC-BILL-001-02: 获取待缴费账单")
        void testGetPendingBills_Success() {
            // Given
            List<ElectricityBill> pendingBills = List.of(
                    createPendingBill("bill_001", ROOM_ID),
                    createPendingBill("bill_002", ROOM_ID)
            );

            when(billRepository.findByStatusOrderByPeriodDesc("PENDING")).thenReturn(pendingBills);

            // When
            List<ElectricityBill> result = billingService.getPendingBills();

            // Then
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(b -> "PENDING".equals(b.getStatus())));
        }
    }

    // ==================== TC-BALANCE-001: 余额管理 ====================

    @Nested
    @DisplayName("TC-BALANCE-001: 余额管理")
    class BalanceTests {

        @Test
        @DisplayName("TC-BALANCE-001-01: 获取房间余额")
        void testGetRoomBalance_Success() {
            // Given
            RoomBalance balance = createRoomBalance(ROOM_ID, 100.0);

            when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(balance));

            // When
            RoomBalance result = billingService.getRoomBalance(ROOM_ID);

            // Then
            assertEquals(ROOM_ID, result.getRoomId());
            assertEquals(100.0, result.getBalance());
        }

        @Test
        @DisplayName("TC-BALANCE-001-02: 扣除电费成功")
        void testDeductElectricityFee_Success() {
            // Given
            RoomBalance balance = createRoomBalance(ROOM_ID, 100.0);

            when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(balance));
            when(roomBalanceRepository.save(any(RoomBalance.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            boolean result = billingService.deductElectricityFee(ROOM_ID, 30.0);

            // Then
            assertTrue(result);
            ArgumentCaptor<RoomBalance> captor = ArgumentCaptor.forClass(RoomBalance.class);
            verify(roomBalanceRepository).save(captor.capture());
            assertEquals(70.0, captor.getValue().getBalance());
        }

        @Test
        @DisplayName("TC-BALANCE-001-03: 余额不足扣除失败")
        void testDeductElectricityFee_InsufficientBalance() {
            // Given
            RoomBalance balance = createRoomBalance(ROOM_ID, 20.0);

            when(roomBalanceRepository.findByRoomId(ROOM_ID)).thenReturn(Optional.of(balance));

            // When
            boolean result = billingService.deductElectricityFee(ROOM_ID, 50.0);

            // Then
            assertFalse(result);
            verify(roomBalanceRepository, never()).save(any());
        }
    }

    // ==================== 辅助方法 ====================

    private ElectricityBill createPendingBill(String billId, String roomId) {
        ElectricityBill bill = new ElectricityBill();
        bill.setId(billId);
        bill.setRoomId(roomId);
        bill.setPeriod("2024-01");
        bill.setTotalConsumption(100.0);
        bill.setTotalAmount(60.0);
        bill.setStatus("PENDING");
        bill.setStartDate(System.currentTimeMillis() / 1000 - 2592000);
        bill.setEndDate(System.currentTimeMillis() / 1000);
        bill.setCreatedAt(System.currentTimeMillis() / 1000 - 86400);
        return bill;
    }

    private ElectricityBill createPaidBill(String billId, String roomId) {
        ElectricityBill bill = createPendingBill(billId, roomId);
        bill.setStatus("PAID");
        bill.setPaymentMethod(PAYMENT_METHOD);
        bill.setPaidAt(System.currentTimeMillis() / 1000 - 3600);
        bill.setTransactionId("TXN" + System.currentTimeMillis());
        return bill;
    }

    private DormRoom createDormRoom(String roomId) {
        DormRoom room = new DormRoom();
        room.setId(roomId);
        room.setBuildingId("building_001");
        room.setFloor(3);
        room.setRoomNumber("301");
        room.setDeviceId("device_001");
        room.setPriceRuleId("rule_001");
        room.setStatus("OCCUPIED");
        room.setEnabled(true);
        return room;
    }

    private RoomBalance createRoomBalance(String roomId, double balance) {
        RoomBalance rb = new RoomBalance();
        rb.setId("bal_" + roomId);
        rb.setRoomId(roomId);
        rb.setBalance(balance);
        rb.setTotalRecharged(500.0);
        rb.setTotalConsumed(400.0);
        rb.setWarningThreshold(10.0);
        rb.setWarningSent(false);
        rb.setAutoCutoff(true);
        rb.setLastRechargeAt(System.currentTimeMillis() / 1000 - 86400);
        rb.setLastConsumptionAt(System.currentTimeMillis() / 1000 - 3600);
        rb.setCreatedAt(System.currentTimeMillis() / 1000 - 2592000);
        return rb;
    }
}