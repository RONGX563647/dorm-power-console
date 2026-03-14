package com.dormpower.service;

import com.dormpower.exception.BusinessException;
import com.dormpower.exception.ResourceNotFoundException;
import com.dormpower.model.*;
import com.dormpower.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 计费管理服务
 */
@Service
public class BillingService {

    @Autowired
    private ElectricityPriceRuleRepository priceRuleRepository;

    @Autowired
    private ElectricityBillRepository billRepository;

    @Autowired
    private RechargeRecordRepository rechargeRecordRepository;

    @Autowired
    private RoomBalanceRepository roomBalanceRepository;

    @Autowired
    private DormRoomRepository dormRoomRepository;

    @Autowired
    private TelemetryRepository telemetryRepository;

    /**
     * 创建电价规则
     */
    @CacheEvict(value = "priceRules", allEntries = true)
    public ElectricityPriceRule createPriceRule(ElectricityPriceRule rule) {
        rule.setId("rule_" + UUID.randomUUID().toString().substring(0, 8));
        rule.setCreatedAt(System.currentTimeMillis() / 1000);
        rule.setEnabled(true);
        return priceRuleRepository.save(rule);
    }

    /**
     * 获取所有电价规则
     */
    public List<ElectricityPriceRule> getAllPriceRules() {
        return priceRuleRepository.findAll();
    }

    /**
     * 获取启用的电价规则
     */
    @Cacheable(value = "priceRules", key = "'enabled'")
    public List<ElectricityPriceRule> getEnabledPriceRules() {
        return priceRuleRepository.findByEnabled(true);
    }

    /**
     * 更新电价规则
     */
    @CacheEvict(value = "priceRules", allEntries = true)
    public ElectricityPriceRule updatePriceRule(String id, ElectricityPriceRule rule) {
        ElectricityPriceRule existing = priceRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Price rule not found"));

        existing.setName(rule.getName());
        existing.setType(rule.getType());
        existing.setDescription(rule.getDescription());
        existing.setBasePrice(rule.getBasePrice());
        existing.setTier1Price(rule.getTier1Price());
        existing.setTier1Limit(rule.getTier1Limit());
        existing.setTier2Price(rule.getTier2Price());
        existing.setTier2Limit(rule.getTier2Limit());
        existing.setTier3Price(rule.getTier3Price());
        existing.setPeakPrice(rule.getPeakPrice());
        existing.setValleyPrice(rule.getValleyPrice());
        existing.setFlatPrice(rule.getFlatPrice());
        existing.setPeakStartHour(rule.getPeakStartHour());
        existing.setPeakEndHour(rule.getPeakEndHour());
        existing.setValleyStartHour(rule.getValleyStartHour());
        existing.setValleyEndHour(rule.getValleyEndHour());
        existing.setEnabled(rule.isEnabled());
        existing.setUpdatedAt(System.currentTimeMillis() / 1000);

        return priceRuleRepository.save(existing);
    }

    /**
     * 删除电价规则
     */
    @CacheEvict(value = "priceRules", allEntries = true)
    public void deletePriceRule(String id) {
        priceRuleRepository.deleteById(id);
    }

    /**
     * 计算电费
     */
    public double calculateElectricityCost(double consumption, ElectricityPriceRule rule) {
        if (consumption <= 0) {
            return 0;
        }

        double cost = 0;

        switch (rule.getType()) {
            case "TIER":
                cost = calculateTierPrice(consumption, rule);
                break;
            case "TIME":
                cost = consumption * rule.getBasePrice();
                break;
            case "MIXED":
                cost = calculateTierPrice(consumption, rule);
                break;
            default:
                cost = consumption * rule.getBasePrice();
        }

        return Math.round(cost * 100) / 100.0;
    }

    private double calculateTierPrice(double consumption, ElectricityPriceRule rule) {
        double cost = 0;
        double remaining = consumption;

        if (rule.getTier1Limit() > 0 && remaining > 0) {
            double tier1Consumption = Math.min(remaining, rule.getTier1Limit());
            cost += tier1Consumption * rule.getTier1Price();
            remaining -= tier1Consumption;
        }

        if (rule.getTier2Limit() > 0 && remaining > 0) {
            double tier2Consumption = Math.min(remaining, rule.getTier2Limit() - rule.getTier1Limit());
            cost += tier2Consumption * rule.getTier2Price();
            remaining -= tier2Consumption;
        }

        if (remaining > 0) {
            cost += remaining * rule.getTier3Price();
        }

        return cost;
    }

    /**
     * 生成月度账单
     */
    @Transactional
    public ElectricityBill generateMonthlyBill(String roomId, String period) {
        DormRoom room = dormRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        String deviceId = room.getDeviceId();
        if (deviceId == null) {
            throw new RuntimeException("Room has no associated device");
        }

        // 解析周期
        String[] parts = period.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1, 0, 0, 0);
        long startTime = cal.getTimeInMillis() / 1000;
        cal.set(year, month, 1, 0, 0, 0);
        long endTime = cal.getTimeInMillis() / 1000;

        // 获取用电量
        List<Telemetry> telemetryList = telemetryRepository.findByDeviceIdAndTsBetween(deviceId, startTime, endTime);
        double totalConsumption = telemetryList.stream().mapToDouble(Telemetry::getPowerW).sum() / 1000; // 转换为度

        // 获取电价规则
        ElectricityPriceRule priceRule = priceRuleRepository.findById(room.getPriceRuleId())
                .orElse(priceRuleRepository.findFirstByEnabledTrueOrderByCreatedAtAsc()
                        .orElseThrow(() -> new RuntimeException("No price rule found")));

        // 计算电费
        double totalAmount = calculateElectricityCost(totalConsumption, priceRule);

        // 创建账单
        ElectricityBill bill = new ElectricityBill();
        bill.setId("bill_" + UUID.randomUUID().toString().substring(0, 8));
        bill.setRoomId(roomId);
        bill.setPeriod(period);
        bill.setTotalConsumption(Math.round(totalConsumption * 100) / 100.0);
        bill.setTotalAmount(totalAmount);
        bill.setStatus("PENDING");
        bill.setStartDate(startTime);
        bill.setEndDate(endTime);
        bill.setCreatedAt(System.currentTimeMillis() / 1000);

        return billRepository.save(bill);
    }

    /**
     * 获取房间账单列表
     */
    public Page<ElectricityBill> getRoomBills(String roomId, Pageable pageable) {
        return billRepository.findByRoomIdOrderByPeriodDesc(roomId, pageable);
    }

    /**
     * 获取所有待缴费账单（带缓存）
     */
    @Cacheable(value = "pendingBills", key = "'all'")
    public List<ElectricityBill> getPendingBills() {
        return billRepository.findByStatusOrderByPeriodDesc("PENDING");
    }
    
    /**
     * 缴费（清除待缴费账单缓存）
     */
    @Transactional
    @CacheEvict(value = "pendingBills", allEntries = true)
    public ElectricityBill payBill(String billId, String paymentMethod, String operator) {
        ElectricityBill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        if (!"PENDING".equals(bill.getStatus())) {
            throw new RuntimeException("Bill is not pending");
        }

        bill.setStatus("PAID");
        bill.setPaymentMethod(paymentMethod);
        bill.setPaidAt(System.currentTimeMillis() / 1000);
        bill.setTransactionId("TXN" + System.currentTimeMillis());
        bill.setUpdatedAt(System.currentTimeMillis() / 1000);

        return billRepository.save(bill);
    }

    /**
     * 充值金额最小值（元）
     */
    private static final double MIN_RECHARGE_AMOUNT = 1.0;

    /**
     * 充值金额最大值（元）
     */
    private static final double MAX_RECHARGE_AMOUNT = 10000.0;

    /**
     * 充值（清除余额缓存）
     */
    @Transactional
    @CacheEvict(value = "roomBalance", key = "#roomId")
    public RechargeRecord recharge(String roomId, double amount, String paymentMethod, String operator) {
        DormRoom room = dormRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        // 验证充值金额
        if (amount < MIN_RECHARGE_AMOUNT) {
            throw new BusinessException("充值金额不能小于" + MIN_RECHARGE_AMOUNT + "元");
        }
        if (amount > MAX_RECHARGE_AMOUNT) {
            throw new BusinessException("充值金额不能超过" + MAX_RECHARGE_AMOUNT + "元");
        }

        // 获取或创建余额记录
        RoomBalance balance = roomBalanceRepository.findByRoomId(roomId)
                .orElseGet(() -> createRoomBalance(roomId));

        double balanceBefore = balance.getBalance();
        double balanceAfter = balanceBefore + amount;

        // 更新余额
        balance.setBalance(balanceAfter);
        balance.setTotalRecharged(balance.getTotalRecharged() + amount);
        balance.setLastRechargeAt(System.currentTimeMillis() / 1000);
        balance.setUpdatedAt(System.currentTimeMillis() / 1000);
        balance.setWarningSent(false);
        roomBalanceRepository.save(balance);

        // 创建充值记录
        RechargeRecord record = new RechargeRecord();
        record.setId("rec_" + UUID.randomUUID().toString().substring(0, 8));
        record.setRoomId(roomId);
        record.setAmount(amount);
        record.setBalanceBefore(balanceBefore);
        record.setBalanceAfter(balanceAfter);
        record.setPaymentMethod(paymentMethod);
        record.setStatus("SUCCESS");
        record.setOperator(operator);
        record.setTransactionId("TXN" + System.currentTimeMillis());
        record.setCreatedAt(System.currentTimeMillis() / 1000);

        return rechargeRecordRepository.save(record);
    }

    private RoomBalance createRoomBalance(String roomId) {
        RoomBalance balance = new RoomBalance();
        balance.setId("bal_" + UUID.randomUUID().toString().substring(0, 8));
        balance.setRoomId(roomId);
        balance.setBalance(0);
        balance.setTotalRecharged(0);
        balance.setTotalConsumed(0);
        balance.setWarningThreshold(10);
        balance.setWarningSent(false);
        balance.setAutoCutoff(true);
        balance.setLastRechargeAt(System.currentTimeMillis() / 1000);
        balance.setLastConsumptionAt(System.currentTimeMillis() / 1000);
        balance.setCreatedAt(System.currentTimeMillis() / 1000);
        return roomBalanceRepository.save(balance);
    }

    /**
     * 获取房间余额（带缓存）
     */
    @Cacheable(value = "roomBalance", key = "#roomId")
    public RoomBalance getRoomBalance(String roomId) {
        return roomBalanceRepository.findByRoomId(roomId)
                .orElseGet(() -> createRoomBalance(roomId));
    }

    /**
     * 获取充值记录
     */
    public Page<RechargeRecord> getRechargeRecords(String roomId, Pageable pageable) {
        return rechargeRecordRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
    }

    /**
     * 扣除电费（清除余额缓存）
     */
    @Transactional
    @CacheEvict(value = "roomBalance", key = "#roomId")
    public boolean deductElectricityFee(String roomId, double amount) {
        RoomBalance balance = roomBalanceRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Room balance not found"));

        if (balance.getBalance() < amount) {
            return false; // 余额不足
        }

        balance.setBalance(balance.getBalance() - amount);
        balance.setTotalConsumed(balance.getTotalConsumed() + amount);
        balance.setLastConsumptionAt(System.currentTimeMillis() / 1000);
        balance.setUpdatedAt(System.currentTimeMillis() / 1000);

        roomBalanceRepository.save(balance);
        return true;
    }

    /**
     * 获取余额不足的房间
     */
    public List<RoomBalance> getLowBalanceRooms(double threshold) {
        return roomBalanceRepository.findByBalanceLessThan(threshold);
    }
}
