package cn.aiedge.erp.stock.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 商品图片文件存储服务
 *
 * <p>图片统一落盘到平台存储根目录（storage.local.base-path，与 /files/** 访问口径一致）下的
 * <code>product-image/{tenantId}/{yyyy}/{MM}/{dd}/{uuid}.ext</code>，不改动既有存储策略。</p>
 */
@Slf4j
@Service
public class ProductImageStorageService {

    /** 与 core-api FileAccessController / LocalStorageStrategy 共用同一存储根目录 */
    @Value("${storage.local.base-path:./uploads}")
    private String basePath;

    private static final String BIZ_DIR = "product-image";

    /**
     * 保存图片，返回相对路径
     */
    public String save(MultipartFile file, Long tenantId) throws IOException {
        String originalName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalName.substring(dot);
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relative = BIZ_DIR + "/" + (tenantId == null ? 0L : tenantId) + "/" + datePath + "/"
                + UUID.randomUUID().toString().replace("-", "") + ext;

        Path target = resolveSafe(relative);
        Files.createDirectories(target.getParent());
        file.transferTo(target.toFile());
        return relative;
    }

    /**
     * 读取图片内容
     */
    public byte[] read(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        Path path = resolveSafe(relativePath);
        if (!Files.exists(path)) {
            return null;
        }
        return Files.readAllBytes(path);
    }

    /**
     * 判断物理文件是否存在
     *
     * <p>图片记录仍在（未软删）但文件已被清理时，接口应视为不可用，避免前端 img 直接 404。</p>
     */
    public boolean exists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        try {
            return Files.exists(resolveSafe(relativePath));
        } catch (RuntimeException e) {
            log.warn("图片路径非法: path={}, err={}", relativePath, e.getMessage());
            return false;
        }
    }

    /**
     * 删除图片文件（物理文件不存在时静默忽略）
     */
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafe(relativePath));
        } catch (IOException e) {
            log.warn("删除图片文件失败: path={}, err={}", relativePath, e.getMessage());
        }
    }

    /**
     * 解析为存储根目录下的安全路径（防路径穿越）
     */
    private Path resolveSafe(String relativePath) {
        Path base = Paths.get(basePath).toAbsolutePath().normalize();
        Path target = base.resolve(relativePath).normalize();
        if (!target.startsWith(base)) {
            throw new IllegalArgumentException("非法的图片存储路径: " + relativePath);
        }
        return target;
    }
}
