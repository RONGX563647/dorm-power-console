package com.dormpower.repository;

import com.dormpower.model.StudentRoomHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 学生入住历史Repository
 */
@Repository
public interface StudentRoomHistoryRepository extends JpaRepository<StudentRoomHistory, String> {

    /**
     * 根据学生ID查询历史记录
     */
    Page<StudentRoomHistory> findByStudentIdOrderByCheckInDateDesc(String studentId, Pageable pageable);

    /**
     * 根据房间ID查询历史记录
     */
    Page<StudentRoomHistory> findByRoomIdOrderByCheckInDateDesc(String roomId, Pageable pageable);

    /**
     * 查询学生在住记录
     */
    StudentRoomHistory findByStudentIdAndStatus(String studentId, String status);

    /**
     * 查询房间当前在住学生
     */
    List<StudentRoomHistory> findByRoomIdAndStatus(String roomId, String status);

    /**
     * 查询某时间段内的入住记录
     */
    List<StudentRoomHistory> findByCheckInDateBetween(long startDate, long endDate);

    /**
     * 统计学生入住次数
     */
    long countByStudentId(String studentId);
}
