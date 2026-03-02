package com.dormpower.repository;

import com.dormpower.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学生/住户Repository
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, String> {

    /**
     * 根据学号查询学生
     */
    Optional<Student> findByStudentNumber(String studentNumber);

    /**
     * 根据房间ID查询学生
     */
    List<Student> findByRoomIdAndStatus(String roomId, String status);

    /**
     * 根据状态查询学生
     */
    Page<Student> findByStatus(String status, Pageable pageable);

    /**
     * 根据院系查询学生
     */
    Page<Student> findByDepartmentContaining(String department, Pageable pageable);

    /**
     * 根据姓名模糊查询
     */
    Page<Student> findByNameContaining(String name, Pageable pageable);

    /**
     * 根据学号模糊查询
     */
    Page<Student> findByStudentNumberContaining(String studentNumber, Pageable pageable);

    /**
     * 查询所有在读学生
     */
    List<Student> findByStatusAndEnabledTrue(String status);

    /**
     * 查询未分配房间的学生
     */
    List<Student> findByRoomIdIsNullAndStatusAndEnabledTrue(String status);

    /**
     * 根据类型查询学生
     */
    Page<Student> findByType(String type, Pageable pageable);

    /**
     * 统计各状态学生数量
     */
    long countByStatus(String status);

    /**
     * 统计已分配房间的学生数量
     */
    long countByRoomIdIsNotNullAndStatus(String status);
}
