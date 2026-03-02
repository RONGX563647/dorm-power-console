package com.dormpower.service;

import com.dormpower.model.DataBackup;
import com.dormpower.repository.DataBackupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 数据备份服务类
 */
@Service
public class DataBackupService {

    @Autowired
    private DataBackupRepository dataBackupRepository;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${backup.path:./backups}")
    private String backupPath;

    private final ObjectMapper objectMapper;

    public DataBackupService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 创建数据库备份
     */
    public DataBackup createDatabaseBackup(String description, String createdBy) {
        DataBackup backup = new DataBackup();
        backup.setName("db_backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        backup.setType("DATABASE");
        backup.setDescription(description);
        backup.setCreatedBy(createdBy);
        backup.setFilePath(backupPath + "/" + backup.getName() + ".sql");

        dataBackupRepository.save(backup);

        try {
            // 执行数据库备份
            performDatabaseBackup(backup.getFilePath());

            File file = new File(backup.getFilePath());
            backup.setFileSize(file.length());
            backup.setStatus("COMPLETED");
            backup.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            backup.setStatus("FAILED");
        }

        return dataBackupRepository.save(backup);
    }

    /**
     * 创建数据导出备份
     */
    public DataBackup createDataExportBackup(String description, String createdBy) {
        DataBackup backup = new DataBackup();
        backup.setName("export_backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        backup.setType("EXPORT");
        backup.setDescription(description);
        backup.setCreatedBy(createdBy);
        backup.setFilePath(backupPath + "/" + backup.getName() + ".zip");

        dataBackupRepository.save(backup);

        try {
            // 创建导出数据备份
            performDataExport(backup.getFilePath());

            File file = new File(backup.getFilePath());
            backup.setFileSize(file.length());
            backup.setStatus("COMPLETED");
            backup.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            backup.setStatus("FAILED");
        }

        return dataBackupRepository.save(backup);
    }

    /**
     * 获取所有备份
     */
    public List<DataBackup> getAllBackups() {
        return dataBackupRepository.findAll();
    }

    /**
     * 获取最近的备份
     */
    public List<DataBackup> getRecentBackups() {
        return dataBackupRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * 删除备份
     */
    public void deleteBackup(Long id) {
        DataBackup backup = dataBackupRepository.findById(id).orElse(null);
        if (backup != null) {
            // 删除文件
            try {
                Files.deleteIfExists(Paths.get(backup.getFilePath()));
            } catch (IOException e) {
                // 忽略文件删除错误
            }
            dataBackupRepository.delete(backup);
        }
    }

    /**
     * 清理过期备份
     */
    public void cleanupOldBackups(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<DataBackup> oldBackups = dataBackupRepository.findByCreatedAtBefore(cutoffDate);

        for (DataBackup backup : oldBackups) {
            deleteBackup(backup.getId());
        }
    }

    /**
     * 执行数据库备份
     */
    private void performDatabaseBackup(String filePath) throws Exception {
        // 确保备份目录存在
        Files.createDirectories(Paths.get(backupPath));

        // 解析数据库连接信息
        String dbName = extractDatabaseName();

        // 构建pg_dump命令
        String[] command = {
                "pg_dump",
                "-h", "localhost",
                "-U", "postgres",
                "-d", dbName,
                "-f", filePath,
                "-F", "p"
        };

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("PGPASSWORD", "postgres");
        pb.redirectErrorStream(true);

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Database backup failed with exit code: " + exitCode);
        }
    }

    /**
     * 执行数据导出
     */
    private void performDataExport(String filePath) throws Exception {
        // 确保备份目录存在
        Files.createDirectories(Paths.get(backupPath));

        // 创建临时目录
        String tempDir = backupPath + "/temp_" + System.currentTimeMillis();
        Files.createDirectories(Paths.get(tempDir));

        try {
            // 导出数据到JSON文件
            exportTableData(tempDir);

            // 压缩为zip文件
            zipDirectory(tempDir, filePath);
        } finally {
            // 清理临时目录
            deleteDirectory(Paths.get(tempDir));
        }
    }

    /**
     * 导出表数据
     */
    private void exportTableData(String tempDir) throws Exception {
        // 这里应该导出各个表的数据
        // 简化实现，实际应该注入各个Repository
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("exportTime", LocalDateTime.now().toString());
        exportData.put("version", "1.0.0");

        String jsonFile = tempDir + "/data.json";
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(jsonFile), exportData);
    }

    /**
     * 压缩目录
     */
    private void zipDirectory(String sourceDir, String zipFile) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            Path sourcePath = Paths.get(sourceDir);
            Files.walk(sourcePath).forEach(path -> {
                try {
                    String zipEntryName = sourcePath.relativize(path).toString();
                    if (Files.isDirectory(path)) {
                        if (!zipEntryName.isEmpty()) {
                            zos.putNextEntry(new ZipEntry(zipEntryName + "/"));
                            zos.closeEntry();
                        }
                    } else {
                        zos.putNextEntry(new ZipEntry(zipEntryName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to zip file: " + path, e);
                }
            });
        }
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // 忽略删除错误
                        }
                    });
        }
    }

    /**
     * 从JDBC URL提取数据库名
     */
    private String extractDatabaseName() {
        // 解析 jdbc:postgresql://localhost:5432/dorm_power
        String url = datasourceUrl;
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash != -1) {
            String dbName = url.substring(lastSlash + 1);
            int questionMark = dbName.indexOf('?');
            if (questionMark != -1) {
                dbName = dbName.substring(0, questionMark);
            }
            return dbName;
        }
        return "dorm_power";
    }

    /**
     * 获取备份统计
     */
    public Map<String, Object> getBackupStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<DataBackup> allBackups = dataBackupRepository.findAll();

        long totalSize = allBackups.stream()
                .filter(b -> b.getFileSize() != null)
                .mapToLong(DataBackup::getFileSize)
                .sum();

        long completedCount = allBackups.stream()
                .filter(b -> "COMPLETED".equals(b.getStatus()))
                .count();

        long failedCount = allBackups.stream()
                .filter(b -> "FAILED".equals(b.getStatus()))
                .count();

        stats.put("totalBackups", allBackups.size());
        stats.put("completedBackups", completedCount);
        stats.put("failedBackups", failedCount);
        stats.put("totalSize", totalSize);
        stats.put("totalSizeMB", totalSize / (1024 * 1024));

        return stats;
    }
}
