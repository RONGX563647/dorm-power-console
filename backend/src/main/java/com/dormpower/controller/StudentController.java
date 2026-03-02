package com.dormpower.controller;

import com.dormpower.annotation.AuditLog;
import com.dormpower.annotation.RateLimit;
import com.dormpower.model.Student;
import com.dormpower.model.StudentRoomHistory;
import com.dormpower.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生/住户管理控制器
 * 提供学生信息管理、入住退宿、批量处理等功能
 */
@RestController
@RequestMapping("/api/students")
@Tag(name = "学生管理", description = "学生/住户信息管理、入住退宿、批量处理接口")
public class StudentController {

    @Autowired
    private StudentService studentService;

    /**
     * 获取学生列表
     */
    @Operation(
            summary = "获取学生列表",
            description = "分页获取所有学生信息，支持按状态、院系筛选",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping
    public ResponseEntity<?> getStudents(
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态筛选：ACTIVE(在读)、GRADUATED(已毕业)、SUSPENDED(休学)")
            @RequestParam(required = false) String status,
            @Parameter(description = "院系筛选")
            @RequestParam(required = false) String department) {
        try {
            Page<Student> students;
            if (status != null && !status.isEmpty()) {
                students = studentService.searchStudents(null, department, status, PageRequest.of(page, size));
            } else {
                students = studentService.getAllStudents(PageRequest.of(page, size));
            }
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to get students: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 创建学生
     */
    @Operation(
            summary = "创建学生",
            description = "创建新的学生/住户信息，学号必须唯一",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Student.class))),
            @ApiResponse(responseCode = "400", description = "创建失败，学号已存在或参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 2.0, type = "create-student")
    @AuditLog(value = "创建学生", type = "STUDENT")
    @PostMapping
    public ResponseEntity<?> createStudent(
            @Parameter(description = "学生信息", required = true,
                    content = @Content(schema = @Schema(implementation = Student.class)))
            @Valid @RequestBody Student student) {
        try {
            Student created = studentService.createStudent(student);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to create student: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取学生详情
     */
    @Operation(
            summary = "获取学生详情",
            description = "根据学生ID获取详细信息",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Student.class))),
            @ApiResponse(responseCode = "404", description = "学生不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getStudent(
            @Parameter(description = "学生ID", required = true, example = "stu_abc123")
            @PathVariable String id) {
        try {
            Student student = studentService.getStudentById(id);
            return ResponseEntity.ok(student);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Student not found: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * 根据学号查询学生
     */
    @Operation(
            summary = "根据学号查询学生",
            description = "通过学号精确查询学生信息",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Student.class))),
            @ApiResponse(responseCode = "404", description = "学生不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/number/{studentNumber}")
    public ResponseEntity<?> getStudentByNumber(
            @Parameter(description = "学号", required = true, example = "2024001001")
            @PathVariable String studentNumber) {
        try {
            Student student = studentService.getStudentByStudentNumber(studentNumber);
            return ResponseEntity.ok(student);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Student not found: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * 更新学生信息
     */
    @Operation(
            summary = "更新学生信息",
            description = "更新指定学生的信息",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Student.class))),
            @ApiResponse(responseCode = "400", description = "更新失败"),
            @ApiResponse(responseCode = "404", description = "学生不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 2.0, type = "update-student")
    @AuditLog(value = "更新学生", type = "STUDENT")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(
            @Parameter(description = "学生ID", required = true, example = "stu_abc123")
            @PathVariable String id,
            @Parameter(description = "学生信息", required = true,
                    content = @Content(schema = @Schema(implementation = Student.class)))
            @Valid @RequestBody Student student) {
        try {
            Student updated = studentService.updateStudent(id, student);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to update student: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 删除学生
     */
    @Operation(
            summary = "删除学生",
            description = "删除学生信息，如学生当前有房间会自动退宿",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "学生不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 1.0, type = "delete-student")
    @AuditLog(value = "删除学生", type = "STUDENT")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(
            @Parameter(description = "学生ID", required = true, example = "stu_abc123")
            @PathVariable String id) {
        try {
            studentService.deleteStudent(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Student deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to delete student: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 学生入住
     */
    @Operation(
            summary = "学生入住",
            description = "为学生分配宿舍房间",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "入住成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Student.class))),
            @ApiResponse(responseCode = "400", description = "入住失败，房间已满或学生已有房间"),
            @ApiResponse(responseCode = "404", description = "学生或房间不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 2.0, type = "check-in")
    @AuditLog(value = "学生入住", type = "STUDENT")
    @PostMapping("/{studentId}/check-in")
    public ResponseEntity<?> checkInStudent(
            @Parameter(description = "学生ID", required = true, example = "stu_abc123")
            @PathVariable String studentId,
            @Parameter(description = "房间ID", required = true, example = "room_xyz789")
            @RequestParam String roomId,
            @Parameter(description = "入住原因")
            @RequestParam(required = false, defaultValue = "新生入住") String reason,
            @Parameter(description = "操作员")
            @RequestParam(required = false, defaultValue = "admin") String operator) {
        try {
            Student student = studentService.checkInStudent(studentId, roomId, reason, operator);
            return ResponseEntity.ok(student);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to check in student: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 学生退宿
     */
    @Operation(
            summary = "学生退宿",
            description = "为学生办理退宿手续",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "退宿成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Student.class))),
            @ApiResponse(responseCode = "400", description = "退宿失败，学生未入住"),
            @ApiResponse(responseCode = "404", description = "学生不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 2.0, type = "check-out")
    @AuditLog(value = "学生退宿", type = "STUDENT")
    @PostMapping("/{studentId}/check-out")
    public ResponseEntity<?> checkOutStudent(
            @Parameter(description = "学生ID", required = true, example = "stu_abc123")
            @PathVariable String studentId,
            @Parameter(description = "退宿原因")
            @RequestParam(required = false, defaultValue = "毕业退宿") String reason,
            @Parameter(description = "操作员")
            @RequestParam(required = false, defaultValue = "admin") String operator) {
        try {
            Student student = studentService.checkOutStudent(studentId, reason, operator);
            return ResponseEntity.ok(student);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to check out student: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 调换宿舍
     */
    @Operation(
            summary = "调换宿舍",
            description = "为学生调换到新的宿舍房间",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "调换成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Student.class))),
            @ApiResponse(responseCode = "400", description = "调换失败"),
            @ApiResponse(responseCode = "404", description = "学生或房间不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 2.0, type = "swap-room")
    @AuditLog(value = "调换宿舍", type = "STUDENT")
    @PostMapping("/{studentId}/swap-room")
    public ResponseEntity<?> swapRoom(
            @Parameter(description = "学生ID", required = true, example = "stu_abc123")
            @PathVariable String studentId,
            @Parameter(description = "新房间ID", required = true, example = "room_new456")
            @RequestParam String newRoomId,
            @Parameter(description = "调换原因")
            @RequestParam(required = false, defaultValue = "个人申请") String reason,
            @Parameter(description = "操作员")
            @RequestParam(required = false, defaultValue = "admin") String operator) {
        try {
            Student student = studentService.swapRoom(studentId, newRoomId, reason, operator);
            return ResponseEntity.ok(student);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to swap room: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取房间的学生列表
     */
    @Operation(
            summary = "获取房间的学生列表",
            description = "获取指定房间当前入住的所有学生",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getStudentsByRoom(
            @Parameter(description = "房间ID", required = true, example = "room_xyz789")
            @PathVariable String roomId) {
        try {
            List<Student> students = studentService.getStudentsByRoom(roomId);
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to get students: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取学生入住历史
     */
    @Operation(
            summary = "获取学生入住历史",
            description = "获取指定学生的所有入住历史记录",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "学生不存在"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/{studentId}/history")
    public ResponseEntity<?> getStudentHistory(
            @Parameter(description = "学生ID", required = true, example = "stu_abc123")
            @PathVariable String studentId,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<StudentRoomHistory> history = studentService.getStudentHistory(studentId, PageRequest.of(page, size));
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to get history: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 搜索学生
     */
    @Operation(
            summary = "搜索学生",
            description = "根据关键词（姓名或学号）、院系、状态搜索学生",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "搜索成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/search")
    public ResponseEntity<?> searchStudents(
            @Parameter(description = "搜索关键词（姓名或学号）")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "院系筛选")
            @RequestParam(required = false) String department,
            @Parameter(description = "状态筛选：ACTIVE(在读)、GRADUATED(已毕业)、SUSPENDED(休学)")
            @RequestParam(required = false) String status,
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Student> students = studentService.searchStudents(keyword, department, status, PageRequest.of(page, size));
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to search students: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取未分配房间的学生
     */
    @Operation(
            summary = "获取未分配房间的学生",
            description = "获取所有未分配宿舍的在读学生列表",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/unassigned")
    public ResponseEntity<?> getUnassignedStudents() {
        try {
            List<Student> students = studentService.getUnassignedStudents();
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to get unassigned students: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取学生统计
     */
    @Operation(
            summary = "获取学生统计",
            description = "获取学生数量、入住率等统计数据",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/statistics")
    public ResponseEntity<?> getStudentStatistics() {
        try {
            Map<String, Object> stats = studentService.getStudentStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to get statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 批量毕业处理
     */
    @Operation(
            summary = "批量毕业处理",
            description = "批量处理指定毕业年份的学生，自动办理退宿并标记为已毕业",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "处理成功"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @RateLimit(value = 1.0, type = "batch-graduate")
    @AuditLog(value = "批量毕业处理", type = "STUDENT")
    @PostMapping("/batch/graduate")
    public ResponseEntity<?> batchGraduate(
            @Parameter(description = "毕业年份", required = true, example = "2024")
            @RequestParam int graduationYear,
            @Parameter(description = "操作员")
            @RequestParam(required = false, defaultValue = "admin") String operator) {
        try {
            int count = studentService.batchGraduate(graduationYear, operator);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Batch graduate processed successfully");
            response.put("processedCount", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to process batch graduate: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
