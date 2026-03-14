package com.dormpower.service;

import com.dormpower.model.DormRoom;
import com.dormpower.model.Student;
import com.dormpower.model.StudentRoomHistory;
import com.dormpower.repository.DormRoomRepository;
import com.dormpower.repository.StudentRepository;
import com.dormpower.repository.StudentRoomHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 学生/住户管理服务
 */
@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentRoomHistoryRepository historyRepository;

    @Autowired
    private DormRoomRepository dormRoomRepository;

    /**
     * 创建学生（清除统计缓存）
     */
    @CacheEvict(value = "studentStats", allEntries = true)
    public Student createStudent(Student student) {
        // 检查学号是否已存在
        if (studentRepository.findByStudentNumber(student.getStudentNumber()).isPresent()) {
            throw new RuntimeException("Student number already exists");
        }

        student.setId("stu_" + UUID.randomUUID().toString().substring(0, 8));
        student.setCreatedAt(System.currentTimeMillis() / 1000);
        student.setEnabled(true);
        student.setStatus("ACTIVE");

        return studentRepository.save(student);
    }

    /**
     * 获取所有学生
     */
    public Page<Student> getAllStudents(Pageable pageable) {
        return studentRepository.findAll(pageable);
    }

    /**
     * 根据ID获取学生
     */
    public Student getStudentById(String id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    /**
     * 根据学号获取学生
     */
    public Student getStudentByStudentNumber(String studentNumber) {
        return studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    /**
     * 更新学生信息（清除统计缓存）
     */
    @CacheEvict(value = "studentStats", allEntries = true)
    public Student updateStudent(String id, Student student) {
        Student existing = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // 如果修改了学号，检查新学号是否已存在
        if (!existing.getStudentNumber().equals(student.getStudentNumber())) {
            if (studentRepository.findByStudentNumber(student.getStudentNumber()).isPresent()) {
                throw new RuntimeException("Student number already exists");
            }
            existing.setStudentNumber(student.getStudentNumber());
        }

        existing.setName(student.getName());
        existing.setGender(student.getGender());
        existing.setDepartment(student.getDepartment());
        existing.setMajor(student.getMajor());
        existing.setClassName(student.getClassName());
        existing.setPhone(student.getPhone());
        existing.setEmail(student.getEmail());
        existing.setEmergencyContact(student.getEmergencyContact());
        existing.setEmergencyPhone(student.getEmergencyPhone());
        existing.setType(student.getType());
        existing.setEnrollmentYear(student.getEnrollmentYear());
        existing.setExpectedGraduationYear(student.getExpectedGraduationYear());
        existing.setPhotoUrl(student.getPhotoUrl());
        existing.setRemark(student.getRemark());
        existing.setEnabled(student.isEnabled());
        existing.setUpdatedAt(System.currentTimeMillis() / 1000);

        return studentRepository.save(existing);
    }

    /**
     * 删除学生（清除统计缓存）
     */
    @Transactional
    @CacheEvict(value = {"studentStats", "roomStats"}, allEntries = true)
    public void deleteStudent(String id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // 如果学生当前有房间，先退宿
        if (student.getRoomId() != null) {
            checkOutStudent(id, "学生信息删除", "system");
        }

        studentRepository.deleteById(id);
    }

    /**
     * 学生入住（清除统计缓存）
     */
    @Transactional
    @CacheEvict(value = {"studentStats", "roomStats"}, allEntries = true)
    public Student checkInStudent(String studentId, String roomId, String reason, String operator) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        DormRoom room = dormRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // 检查学生是否已在其他房间
        if (student.getRoomId() != null) {
            throw new RuntimeException("Student is already checked in to another room");
        }

        // 检查房间是否已满
        List<Student> currentResidents = studentRepository.findByRoomIdAndStatus(roomId, "ACTIVE");
        if (currentResidents.size() >= room.getCapacity()) {
            throw new RuntimeException("Room is full");
        }

        // 更新学生信息
        student.setRoomId(roomId);
        student.setUpdatedAt(System.currentTimeMillis() / 1000);
        studentRepository.save(student);

        // 更新房间入住人数
        room.setCurrentOccupants(currentResidents.size() + 1);
        room.setStatus("OCCUPIED");
        room.setUpdatedAt(System.currentTimeMillis() / 1000);
        dormRoomRepository.save(room);

        // 创建入住历史记录
        StudentRoomHistory history = new StudentRoomHistory();
        history.setId("hist_" + UUID.randomUUID().toString().substring(0, 8));
        history.setStudentId(studentId);
        history.setRoomId(roomId);
        history.setCheckInDate(System.currentTimeMillis() / 1000);
        history.setStatus("ACTIVE");
        history.setCheckInReason(reason);
        history.setOperator(operator);
        history.setCreatedAt(System.currentTimeMillis() / 1000);
        historyRepository.save(history);

        return student;
    }

    /**
     * 学生退宿（清除统计缓存）
     */
    @Transactional
    @CacheEvict(value = {"studentStats", "roomStats"}, allEntries = true)
    public Student checkOutStudent(String studentId, String reason, String operator) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getRoomId() == null) {
            throw new RuntimeException("Student is not checked in");
        }

        String roomId = student.getRoomId();

        // 更新学生信息
        student.setRoomId(null);
        student.setUpdatedAt(System.currentTimeMillis() / 1000);
        studentRepository.save(student);

        // 更新房间信息
        DormRoom room = dormRoomRepository.findById(roomId).orElse(null);
        if (room != null) {
            List<Student> currentResidents = studentRepository.findByRoomIdAndStatus(roomId, "ACTIVE");
            room.setCurrentOccupants(Math.max(0, currentResidents.size() - 1));
            if (room.getCurrentOccupants() == 0) {
                room.setStatus("VACANT");
            }
            room.setUpdatedAt(System.currentTimeMillis() / 1000);
            dormRoomRepository.save(room);
        }

        // 更新历史记录
        StudentRoomHistory history = historyRepository.findByStudentIdAndStatus(studentId, "ACTIVE");
        if (history != null) {
            history.setStatus("CHECKED_OUT");
            history.setCheckOutDate(System.currentTimeMillis() / 1000);
            history.setCheckOutReason(reason);
            history.setOperator(operator);
            historyRepository.save(history);
        }

        return student;
    }

    /**
     * 批量毕业处理（清除统计缓存）
     */
    @Transactional
    @CacheEvict(value = {"studentStats", "roomStats"}, allEntries = true)
    public int batchGraduate(int graduationYear, String operator) {
        List<Student> graduatingStudents = studentRepository.findByStatusAndEnabledTrue("ACTIVE");
        int count = 0;

        for (Student student : graduatingStudents) {
            if (student.getExpectedGraduationYear() == graduationYear) {
                // 如果有房间，先退宿
                if (student.getRoomId() != null) {
                    checkOutStudent(student.getId(), "毕业退宿", operator);
                }

                student.setStatus("GRADUATED");
                student.setUpdatedAt(System.currentTimeMillis() / 1000);
                studentRepository.save(student);
                count++;
            }
        }

        return count;
    }

    /**
     * 获取房间的学生列表
     */
    public List<Student> getStudentsByRoom(String roomId) {
        return studentRepository.findByRoomIdAndStatus(roomId, "ACTIVE");
    }

    /**
     * 获取学生的入住历史
     */
    public Page<StudentRoomHistory> getStudentHistory(String studentId, Pageable pageable) {
        return historyRepository.findByStudentIdOrderByCheckInDateDesc(studentId, pageable);
    }

    /**
     * 搜索学生
     */
    public Page<Student> searchStudents(String keyword, String department, String status, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            // 先尝试按学号搜索
            Page<Student> byStudentNumber = studentRepository.findByStudentNumberContaining(keyword, pageable);
            if (byStudentNumber.hasContent()) {
                return byStudentNumber;
            }
            // 再按姓名搜索
            return studentRepository.findByNameContaining(keyword, pageable);
        }

        if (department != null && !department.isEmpty()) {
            return studentRepository.findByDepartmentContaining(department, pageable);
        }

        if (status != null && !status.isEmpty()) {
            return studentRepository.findByStatus(status, pageable);
        }

        return studentRepository.findAll(pageable);
    }

    /**
     * 获取未分配房间的学生
     */
    public List<Student> getUnassignedStudents() {
        return studentRepository.findByRoomIdIsNullAndStatusAndEnabledTrue("ACTIVE");
    }

    /**
     * 获取学生统计（带缓存）
     */
    @Cacheable(value = "studentStats", key = "'stats'")
    public Map<String, Object> getStudentStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalStudents = studentRepository.count();
        long activeStudents = studentRepository.countByStatus("ACTIVE");
        long graduatedStudents = studentRepository.countByStatus("GRADUATED");
        long assignedStudents = studentRepository.countByRoomIdIsNotNullAndStatus("ACTIVE");

        stats.put("totalStudents", totalStudents);
        stats.put("activeStudents", activeStudents);
        stats.put("graduatedStudents", graduatedStudents);
        stats.put("assignedStudents", assignedStudents);
        stats.put("unassignedStudents", activeStudents - assignedStudents);
        stats.put("assignmentRate", activeStudents > 0 ? (double) assignedStudents / activeStudents * 100 : 0);

        return stats;
    }

    /**
     * 调换宿舍（清除统计缓存）
     */
    @Transactional
    @CacheEvict(value = {"studentStats", "roomStats"}, allEntries = true)
    public Student swapRoom(String studentId, String newRoomId, String reason, String operator) {
        // 先退宿
        checkOutStudent(studentId, "调换宿舍 - 退宿: " + reason, operator);

        // 再入住新房间
        return checkInStudent(studentId, newRoomId, "调换宿舍 - 入住: " + reason, operator);
    }
}
