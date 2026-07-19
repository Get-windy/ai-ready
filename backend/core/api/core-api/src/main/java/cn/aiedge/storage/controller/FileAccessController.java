package cn.aiedge.storage.controller;

import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.platform.model.StorageConfig;
import cn.aiedge.platform.service.StorageConfigService;
import cn.aiedge.storage.config.StorageProperties;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件访问控制器（带租户隔离校验）
 *
 * 替代原来的静态资源映射，对 /files/** 请求进行：
 * 1. 租户隔离（URL 中的 tenantId 必须匹配）
 * 2. 路径穿越防护（禁止 ../ 等攻击）
 *
 * 注意：文件访问接口不强制要求登录（支持 <img> 等无 Cookie 场景），
 * 安全性依赖 URL 中的 tenantId 路径隔离 + 不可猜测的文件路径。
 *
 * URL 格式：/files/{tenantId}/{bizType}/{yyyy/MM/dd}/{fileName}
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class FileAccessController {

    private final StorageProperties storageProperties;
    private final SecurityContext securityContext;
    private final StorageConfigService storageConfigService;

    /**
     * 获取存储基础路径（优先从 DB 读取）
     * @param urlTenantId 从 URL 路径中提取的租户ID（用于未认证的 img 请求）
     */
    private String getBasePath(Long urlTenantId) {
        try {
            Long tenantId = securityContext.getCurrentTenantId();
            if (tenantId == null) {
                tenantId = urlTenantId;
            }
            if (tenantId != null) {
                StorageConfig dbConfig = storageConfigService.getConfig(tenantId);
                if (dbConfig != null && dbConfig.getLocalPath() != null && !dbConfig.getLocalPath().isEmpty()) {
                    return dbConfig.getLocalPath();
                }
            }
        } catch (Exception e) {
            log.warn("[getBasePath] 从DB读取存储路径失败: {}", e.getMessage());
        }
        return storageProperties.getLocal().getBasePath();
    }

    /**
     * 文件访问（预览/内联）
     * URL 格式：/files/{tenantId}/{bizType}/{yyyy/MM/dd}/{fileName}
     */
    @GetMapping("/files/**")
    public ResponseEntity<byte[]> accessFile(HttpServletRequest request) {
        String fullPath = extractPath(request, "/files/");
        if (fullPath == null) return ResponseEntity.badRequest().build();
        return serveFile(fullPath, false);
    }

    private String extractPath(HttpServletRequest request, String prefix) {
        String uri = request.getRequestURI();
        int idx = uri.indexOf(prefix);
        if (idx < 0) return null;
        return uri.substring(idx + prefix.length());
    }

    private ResponseEntity<byte[]> serveFile(String path, boolean asAttachment) {
        try {
            // 1. 路径穿越防护
            if (isUnsafePath(path)) {
                return ResponseEntity.badRequest().build();
            }

            // 2. 解析租户ID（URL 路径第一层）
            String tenantDir = path.contains("/") ? path.substring(0, path.indexOf('/')) : path;

            // 3. 租户隔离校验（仅当用户已登录时校验，未登录用户如 <img> 标签不强制校验）
            if (StpUtil.isLogin()) {
                Long currentTenantId = securityContext.getCurrentTenantId();
                if (currentTenantId != null && !String.valueOf(currentTenantId).equals(tenantDir)) {
                    log.warn("租户越权访问：当前租户={}, 请求路径租户={}, path={}",
                            currentTenantId, tenantDir, path);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }

            // 4. 定位文件
            String relativePath = path.substring(tenantDir.length() + 1);
            Long urlTenantId = null;
            try { urlTenantId = Long.parseLong(tenantDir); } catch (NumberFormatException ignored) {}
            String basePath = getBasePath(urlTenantId);
            Path filePath = Paths.get(basePath, path).normalize();

            // 二次校验：normalize 后的路径必须在 basePath 下
            if (!filePath.startsWith(Paths.get(basePath).normalize())) {
                return ResponseEntity.badRequest().build();
            }

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // 5. 读取文件内容
            byte[] content = Files.readAllBytes(filePath);

            // 6. 探测真实 Content-Type（优先用文件魔数，回退到文件名猜测）
            String contentType = probeContentType(filePath, relativePath);

            // 7. 构建响应
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(content.length);
            headers.setCacheControl("max-age=86400, private");

            if (asAttachment) {
                String filename = filePath.getFileName().toString();
                String encoded = URLEncoder.encode(relativePath.contains("/")
                        ? relativePath.substring(relativePath.lastIndexOf('/') + 1)
                        : filename, StandardCharsets.UTF_8);
                headers.setContentDispositionFormData("attachment", encoded);
            }

            return ResponseEntity.ok().headers(headers).body(content);

        } catch (IOException e) {
            log.error("文件访问失败: {}", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 路径穿越检测：禁止 ..、反斜杠、空字节
     */
    private boolean isUnsafePath(String path) {
        if (path == null || path.isEmpty()) return true;
        if (path.contains("..") || path.contains("\\") || path.contains("\0")) return true;
        if (path.contains("//")) return true;
        return false;
    }

    /**
     * 探测文件真实 Content-Type（优先用文件魔数检测）
     */
    private String probeContentType(Path filePath, String fallbackName) {
        try {
            String detected = Files.probeContentType(filePath);
            if (detected != null) return detected;
        } catch (IOException ignored) {
            // probeContentType 在某些系统上不支持，忽略
        }
        // 回退：按扩展名猜测
        try {
            String detected = Files.probeContentType(Paths.get(fallbackName));
            if (detected != null) return detected;
        } catch (IOException ignored) {}
        return "application/octet-stream";
    }
}
