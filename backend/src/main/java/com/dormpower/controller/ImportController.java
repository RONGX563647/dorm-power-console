package com.dormpower.controller;

import com.dormpower.model.Device;
import com.dormpower.model.DormRoom;
import com.dormpower.model.Student;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.DormRoomRepository;
import com.dormpower.repository.StudentRepository;
import com.dormpower.service.DataDictService;
import com.dormpower.service.MessageTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * 数据导入控制器
 */
@RestController
@RequestMapping("/api/import")
@Tag(name = "数据导入", description = "批量数据导入接口")
public class ImportController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private DormRoomRepository dormRoomRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DataDictService dataDictService;

    @Autowired
    private MessageTemplateService messageTemplateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Operation(summary = "导入学生数据", description = "批量导入学生数据(CSV格式)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "导入成功")
    })
    @PostMapping("/students")
    public ResponseEntity<?> importStudents(
            @Parameter(description = "CSV文件", required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            List<String> errors = new ArrayList<>();
            int successCount = 0;
            int skipCount = 0;

            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] fields = line.split(",");
                if (fields.length < 5) {
                    errors.add("Invalid line format: " + line);
                    continue;
                }

                try {
                    String studentNumber = fields[0].trim();
                    String name = fields[1].trim();
                    String gender = fields[2].trim();
                    String department = fields[3].trim();
                    String className = fields[4].trim();

                    if (studentRepository.findByStudentNumber(studentNumber).isPresent()) {
                        skipCount++;
                        continue;
                    }

                    Student student = new Student();
                    student.setId("stu_" + UUID.randomUUID().toString().substring(0, 8));
                    student.setStudentNumber(studentNumber);
                    student.setName(name);
                    student.setGender(gender);
                    student.setDepartment(department);
                    student.setClassName(className);
                    student.setStatus("ACTIVE");
                    student.setEnabled(true);
                    student.setCreatedAt(System.currentTimeMillis() / 1000);

                    if (fields.length > 5) {
                        student.setPhone(fields[5].trim());
                    }
                    if (fields.length > 6) {
                        student.setEmail(fields[6].trim());
                    }
                    if (fields.length > 7) {
                        student.setRoomId(fields[7].trim());
                    }

                    studentRepository.save(student);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Error processing line: " + line + " - " + e.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("successCount", successCount);
            result.put("skipCount", skipCount);
            result.put("errorCount", errors.size());
            result.put("errors", errors);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "导入房间数据", description = "批量导入房间数据(CSV格式)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "导入成功")
    })
    @PostMapping("/rooms")
    public ResponseEntity<?> importRooms(
            @Parameter(description = "CSV文件", required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            List<String> errors = new ArrayList<>();
            int successCount = 0;
            int skipCount = 0;

            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] fields = line.split(",");
                if (fields.length < 4) {
                    errors.add("Invalid line format: " + line);
                    continue;
                }

                try {
                    String roomNumber = fields[0].trim();
                    String buildingId = fields[1].trim();
                    String floor = fields[2].trim();
                    String capacity = fields[3].trim();

                    if (dormRoomRepository.findByRoomNumber(roomNumber).isPresent()) {
                        skipCount++;
                        continue;
                    }

                    DormRoom room = new DormRoom();
                    room.setId("room_" + UUID.randomUUID().toString().substring(0, 8));
                    room.setRoomNumber(roomNumber);
                    room.setBuildingId(buildingId);
                    room.setFloor(Integer.parseInt(floor));
                    room.setCapacity(Integer.parseInt(capacity));
                    room.setStatus("AVAILABLE");
                    room.setEnabled(true);
                    room.setCreatedAt(System.currentTimeMillis() / 1000);

                    if (fields.length > 4) {
                        room.setDeviceId(fields[4].trim());
                    }

                    dormRoomRepository.save(room);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Error processing line: " + line + " - " + e.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("successCount", successCount);
            result.put("skipCount", skipCount);
            result.put("errorCount", errors.size());
            result.put("errors", errors);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "导入设备数据", description = "批量导入设备数据(CSV格式)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "导入成功")
    })
    @PostMapping("/devices")
    public ResponseEntity<?> importDevices(
            @Parameter(description = "CSV文件", required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            List<String> errors = new ArrayList<>();
            int successCount = 0;
            int skipCount = 0;

            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] fields = line.split(",");
                if (fields.length < 3) {
                    errors.add("Invalid line format: " + line);
                    continue;
                }

                try {
                    String deviceId = fields[0].trim();
                    String name = fields[1].trim();
                    String room = fields[2].trim();

                    if (deviceRepository.findById(deviceId).isPresent()) {
                        skipCount++;
                        continue;
                    }

                    Device device = new Device();
                    device.setId(deviceId);
                    device.setName(name);
                    device.setRoom(room);
                    device.setOnline(false);
                    device.setCreatedAt(System.currentTimeMillis() / 1000);

                    deviceRepository.save(device);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Error processing line: " + line + " - " + e.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("successCount", successCount);
            result.put("skipCount", skipCount);
            result.put("errorCount", errors.size());
            result.put("errors", errors);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "导入JSON数据", description = "批量导入JSON格式数据", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "导入成功")
    })
    @PostMapping("/json")
    public ResponseEntity<?> importJsonData(
            @Parameter(description = "JSON文件", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "数据类型", required = true, example = "students")
            @RequestParam String type) {
        try {
            Map<String, Object> data = objectMapper.readValue(file.getInputStream(), Map.class);
            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

            int successCount = 0;
            int skipCount = 0;
            List<String> errors = new ArrayList<>();

            switch (type.toLowerCase()) {
                case "students":
                    for (Map<String, Object> item : items) {
                        try {
                            String studentNumber = (String) item.get("studentNumber");
                            if (studentRepository.findByStudentNumber(studentNumber).isPresent()) {
                                skipCount++;
                                continue;
                            }

                            Student student = new Student();
                            student.setId("stu_" + UUID.randomUUID().toString().substring(0, 8));
                            student.setStudentNumber(studentNumber);
                            student.setName((String) item.get("name"));
                            student.setGender((String) item.get("gender"));
                            student.setDepartment((String) item.get("department"));
                            student.setClassName((String) item.get("className"));
                            student.setStatus("ACTIVE");
                            student.setEnabled(true);
                            student.setCreatedAt(System.currentTimeMillis() / 1000);

                            studentRepository.save(student);
                            successCount++;
                        } catch (Exception e) {
                            errors.add("Error: " + e.getMessage());
                        }
                    }
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of("message", "Unknown type: " + type));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("successCount", successCount);
            result.put("skipCount", skipCount);
            result.put("errorCount", errors.size());
            result.put("errors", errors);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "获取导入模板", description = "获取CSV导入模板", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/template/{type}")
    public ResponseEntity<Map<String, Object>> getImportTemplate(
            @Parameter(description = "数据类型", required = true, example = "students")
            @PathVariable String type) {
        Map<String, Object> template = new HashMap<>();

        switch (type.toLowerCase()) {
            case "students":
                template.put("headers", Arrays.asList("studentNumber", "name", "gender", "department", "className", "phone", "email", "roomId"));
                template.put("example", "2024001,张三,男,计算机学院,软件工程1班,13800138000,zhangsan@example.com,room_001");
                break;
            case "rooms":
                template.put("headers", Arrays.asList("roomNumber", "buildingId", "floor", "capacity", "deviceId"));
                template.put("example", "A101,building_001,1,4,strip01");
                break;
            case "devices":
                template.put("headers", Arrays.asList("deviceId", "name", "room"));
                template.put("example", "strip01,智能插座1号,A101");
                break;
            default:
                template.put("message", "Unknown type: " + type);
        }

        return ResponseEntity.ok(template);
    }
}
