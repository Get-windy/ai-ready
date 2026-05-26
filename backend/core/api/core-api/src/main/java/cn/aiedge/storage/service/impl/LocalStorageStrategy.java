package cn.aiedge.storage.service.impl;

import cn.aiedge.storage.model.FileInfo;
import cn.aiedge.storage.service.StorageStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 本地文件存储实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Component("localStorageStrategy")
public class LocalStorageStrategy implements StorageStrategy {

    private static final String TYPE = "LOCAL";

    @Value("${storage.local.base-path:./uploads}")
    private String basePath;

    @Value("${storage.local.base-url:http://localhost:8080/files}")
    private String baseUrl;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public FileInfo upload(InputStream inputStream, String fileName, String contentType) throws Exception {
        // 按日期生成存储路径
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return upload(inputStream, datePath, fileName, contentType);
    }

    @Override
    public FileInfo upload(InputStream inputStream, String filePath, String fileName, String contentType) throws Exception {
        // 生成唯一文件名
        String extension = getFileExtension(fileName);
        String storageName = UUID.randomUUID().toString() + extension;
        
        // 构建完整存储路径
        Path dirPath = Paths.get(basePath, filePath);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        
        Path targetPath = dirPath.resolve(storageName);
        
        // 保存文件
        try (OutputStream outputStream = new FileOutputStream(targetPath.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        
        // 构建文件信息
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(fileName);
        fileInfo.setStorageName(storageName);
        fileInfo.setFilePath(filePath + "/" + storageName);
        fileInfo.setFileSize(Files.size(targetPath));
        fileInfo.setFileType(contentType);
        fileInfo.setFileExtension(extension);
        fileInfo.setStorageType(TYPE);
        fileInfo.setBucketName("local");
        
        return fileInfo;
    }

    @Override
    public InputStream download(String filePath) throws Exception {
        Path path = Paths.get(basePath, filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        return new FileInputStream(path.toFile());
    }

    @Override
    public boolean delete(String filePath) throws Exception {
        Path path = Paths.get(basePath, filePath);
        if (Files.exists(path)) {
            Files.delete(path);
            return true;
        }
        return false;
    }

    @Override
    public int deleteBatch(List<String> filePaths) throws Exception {
        int count = 0;
        for (String filePath : filePaths) {
            if (delete(filePath)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String getAccessUrl(String filePath, int expireSeconds) {
        // 本地存储URL不过期
        return getPermanentUrl(filePath);
    }

    @Override
    public String getPermanentUrl(String filePath) {
        return baseUrl + "/" + filePath;
    }

    @Override
    public boolean exists(String filePath) {
        return Files.exists(Paths.get(basePath, filePath));
    }

    @Override
    public long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(basePath, filePath));
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public boolean copy(String sourcePath, String targetPath) throws Exception {
        Path source = Paths.get(basePath, sourcePath);
        Path target = Paths.get(basePath, targetPath);
        
        if (!Files.exists(source)) {
            return false;
        }
        
        Files.createDirectories(target.getParent());
        Files.copy(source, target);
        return true;
    }

    @Override
    public boolean move(String sourcePath, String targetPath) throws Exception {
        Path source = Paths.get(basePath, sourcePath);
        Path target = Paths.get(basePath, targetPath);
        
        if (!Files.exists(source)) {
            return false;
        }
        
        Files.createDirectories(target.getParent());
        Files.move(source, target);
        return true;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
