package com.dormpower.service;

import com.dormpower.model.*;
import com.dormpower.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 催缴服务
 */
@Service
public class CollectionService {

    @Autowired
    private CollectionRecordRepository collectionRecordRepository;

    @Autowired
    private ElectricityBillRepository billRepository;

    @Autowired
    private RoomBalanceRepository roomBalanceRepository;

    @Autowired
    private DormRoomRepository dormRoomRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SystemLogService systemLogService;

    @Autowired
    private MessageTemplateRepository messageTemplateRepository;

    private static final double LOW_BALANCE_THRESHOLD = 50.0;
    private static final double CRITICAL_BALANCE_THRESHOLD = 10.0;

    /**
     * 创建催缴记录
     */
    public CollectionRecord createCollectionRecord(String billId, String type, String channel) {
        ElectricityBill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found: " + billId));

        CollectionRecord record = new CollectionRecord();
        record.setRoomId(bill.getRoomId());
        record.setBillId(billId);
        record.setType(type);
        record.setChannel(channel);
        record.setStatus("PENDING");
        record.setScheduledTs(System.currentTimeMillis() / 1000);

        DormRoom room = dormRoomRepository.findById(bill.getRoomId()).orElse(null);
        if (room != null) {
            List<Student> students = studentRepository.findByRoomIdAndStatus(room.getId(), "ACTIVE");
            if (!students.isEmpty()) {
                record.setStudentId(students.get(0).getId());
            }
        }

        return collectionRecordRepository.save(record);
    }

    /**
     * 发送催缴通知
     */
    @Transactional
    public void sendCollection(CollectionRecord record) {
        try {
            ElectricityBill bill = billRepository.findById(record.getBillId()).orElse(null);
            if (bill == null) {
                record.setStatus("FAILED");
                record.setErrorMessage("Bill not found");
                collectionRecordRepository.save(record);
                return;
            }

            DormRoom room = dormRoomRepository.findById(record.getRoomId()).orElse(null);
            String roomName = room != null ? room.getRoomNumber() : record.getRoomId();

            String content = generateCollectionContent(bill, roomName, record.getType());
            record.setContent(content);

            boolean sent = false;
            switch (record.getChannel().toUpperCase()) {
                case "EMAIL":
                    sent = sendEmailCollection(record, bill, roomName, content);
                    break;
                case "SYSTEM":
                    sent = sendSystemCollection(record, bill, roomName, content);
                    break;
                case "SMS":
                    sent = sendSmsCollection(record, bill, roomName, content);
                    break;
            }

            if (sent) {
                record.setStatus("SENT");
                record.setSentTs(System.currentTimeMillis() / 1000);
            } else {
                record.setStatus("FAILED");
                record.setRetryCount(record.getRetryCount() + 1);
            }

            collectionRecordRepository.save(record);
        } catch (Exception e) {
            record.setStatus("FAILED");
            record.setErrorMessage(e.getMessage());
            record.setRetryCount(record.getRetryCount() + 1);
            collectionRecordRepository.save(record);
        }
    }

    private boolean sendEmailCollection(CollectionRecord record, ElectricityBill bill, 
                                         String roomName, String content) {
        DormRoom room = dormRoomRepository.findById(record.getRoomId()).orElse(null);
        if (room == null) return false;

        List<Student> students = studentRepository.findByRoomIdAndStatus(room.getId(), "ACTIVE");
        if (students.isEmpty()) return false;

        Student student = students.get(0);
        if (student.getEmail() == null || student.getEmail().isEmpty()) {
            record.setRecipient("N/A");
            return false;
        }

        record.setRecipient(student.getEmail());
        String subject = "电费催缴通知 - " + roomName;
        try {
            emailService.sendSimpleEmail(student.getEmail(), subject, content);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean sendSystemCollection(CollectionRecord record, ElectricityBill bill,
                                          String roomName, String content) {
        DormRoom room = dormRoomRepository.findById(record.getRoomId()).orElse(null);
        if (room == null) return false;

        List<Student> students = studentRepository.findByRoomIdAndStatus(room.getId(), "ACTIVE");
        if (students.isEmpty()) return false;

        record.setRecipient(students.get(0).getStudentNumber());
        notificationService.createSystemNotification(
            "电费催缴通知",
            content,
            "HIGH"
        );
        return true;
    }

    private boolean sendSmsCollection(CollectionRecord record, ElectricityBill bill,
                                       String roomName, String content) {
        return false;
    }

    private String generateCollectionContent(ElectricityBill bill, String roomName, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("尊敬的用户：\n\n");
        sb.append("您的房间 ").append(roomName).append(" 电费账单已逾期，请及时缴费。\n\n");
        sb.append("账单详情：\n");
        sb.append("- 账单周期：").append(bill.getPeriod()).append("\n");
        sb.append("- 用电量：").append(String.format("%.2f", bill.getTotalConsumption())).append(" 度\n");
        sb.append("- 应缴金额：").append(String.format("%.2f", bill.getTotalAmount())).append(" 元\n");
        sb.append("- 账单状态：").append(bill.getStatus()).append("\n\n");

        if ("URGENT".equals(type)) {
            sb.append("【紧急提醒】您的账户余额已不足，请尽快缴费，否则将影响正常用电。\n\n");
        }

        sb.append("请登录系统查看详情并完成缴费。\n");
        return sb.toString();
    }

    /**
     * 定时检查并发送催缴通知
     */
    @Scheduled(fixedRate = 300000)
    public void processPendingCollections() {
        long now = System.currentTimeMillis() / 1000;
        List<CollectionRecord> pendingRecords = collectionRecordRepository.findPendingRecords(now);

        for (CollectionRecord record : pendingRecords) {
            sendCollection(record);
        }

        List<CollectionRecord> retryRecords = collectionRecordRepository.findRetryableRecords();
        for (CollectionRecord record : retryRecords) {
            sendCollection(record);
        }
    }

    /**
     * 检查余额并发送预警
     */
    @Scheduled(fixedRate = 600000)
    public void checkBalanceAndAlert() {
        List<RoomBalance> lowBalances = roomBalanceRepository.findByBalanceLessThan(LOW_BALANCE_THRESHOLD);

        for (RoomBalance balance : lowBalances) {
            boolean isCritical = balance.getBalance() < CRITICAL_BALANCE_THRESHOLD;

            long recentCount = collectionRecordRepository.countByBillIdAndStatus(
                balance.getRoomId(), "SENT");

            if (recentCount > 0) continue;

            CollectionRecord record = new CollectionRecord();
            record.setRoomId(balance.getRoomId());
            record.setBillId("BALANCE_ALERT_" + balance.getRoomId());
            record.setType(isCritical ? "URGENT" : "REMINDER");
            record.setChannel("SYSTEM");
            record.setStatus("PENDING");
            record.setScheduledTs(System.currentTimeMillis() / 1000);
            collectionRecordRepository.save(record);

            if (isCritical) {
                systemLogService.warn("BILLING", 
                    "Low balance alert: Room " + balance.getRoomId() + " balance: " + balance.getBalance(),
                    "CollectionService");
            }
        }
    }

    /**
     * 获取催缴记录
     */
    public Page<CollectionRecord> getCollectionRecords(Pageable pageable) {
        return collectionRecordRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * 获取房间的催缴记录
     */
    public List<CollectionRecord> getRoomCollectionRecords(String roomId) {
        return collectionRecordRepository.findByRoomIdOrderByCreatedAtDesc(roomId);
    }

    /**
     * 获取账单的催缴记录
     */
    public List<CollectionRecord> getBillCollectionRecords(String billId) {
        return collectionRecordRepository.findByBillIdOrderByCreatedAtDesc(billId);
    }

    /**
     * 清理过期记录
     */
    public long cleanupOldRecords(int retentionDays) {
        long cutoff = System.currentTimeMillis() / 1000 - (retentionDays * 86400L);
        return collectionRecordRepository.deleteByCreatedAtBefore(cutoff);
    }
}
