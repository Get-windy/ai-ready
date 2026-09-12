package cn.aiedge.common.file;

import cn.aiedge.common.result.ApiResponse;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 通用文件上传/访问（基础资料统一契约）
 *
 * <p>前端多个组件（证件信息 CertUploadList、附件上传 AttachmentUpload、商品附件、员工照片、
 * 银行账户图片等）统一调用 <code>POST /api/file/upload</code> 上传并读取返回的 <code>url</code>，
 * 本控制器为该契约的唯一实现：文件落盘到 <code>storage.local.base-path</code>（默认 ./uploads）下的
 * <code>file/{yyyy}/{MM}/{dd}/{uuid}.{ext}</code>，访问地址为 <code>/api/file/view/...</code>
 * （免登录，供 <code>&lt;img src&gt;</code> 直接引用，安全性依赖 uuid 路径不可猜测 + 路径穿越防护）。</p>
 *
 * <p>注意：与 <code>cn.aiedge.storage</code>（FileStorageService/FileStorageController）为不同用途，
 * 后者是带分片/多后端/元数据的完整存储模块，当前未纳入组件扫描（存在双实现注入歧义等未接线问题），
 * 本控制器不依赖它，避免为上传一个图片引入整套未验证链路。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Tag(name = "通用文件", description = "基础资料文件上传与访问（统一契约）")
public class FileUploadController {

    /** 与商品图片等模块共用同一存储根目录 */
    @Value("${storage.local.base-path:./uploads}")
    private String basePath;

    /** 业务子目录（与 erp_product_image 的 product-image 目录并列） */
    private static final String BIZ_DIR = "file";

    /** 单文件上限 20MB（spring.servlet.multipart.max-file-size 亦需 ≥ 此值） */
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXT = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "zip", "rar", "mp4", "mp3");

    private static final Set<String> DENIED_EXT = Set.of(
            "exe", "bat", "cmd", "sh", "js", "vbs", "php", "asp", "aspx", "jsp", "jspx", "html", "htm");

    /** 访问路径白名单形态：yyyy/MM/dd/{32位uuid}.{ext} */
    private static final Pattern SAFE_RELATIVE = Pattern.compile("^\\d{4}/\\d{2}/\\d{2}/[a-f0-9]{32}\\.[a-z0-9]{1,8}$");

    private static final Map<String, MediaType> MEDIA_TYPES = Map.ofEntries(
            Map.entry("jpg", MediaType.IMAGE_JPEG), Map.entry("jpeg", MediaType.IMAGE_JPEG),
            Map.entry("png", MediaType.IMAGE_PNG), Map.entry("gif", MediaType.IMAGE_GIF),
            Map.entry("bmp", MediaType.parseMediaType("image/bmp")),
            Map.entry("webp", MediaType.parseMediaType("image/webp")),
            Map.entry("svg", MediaType.parseMediaType("image/svg+xml")),
            Map.entry("pdf", MediaType.APPLICATION_PDF));

    @PostMapping("/upload")
    @SaCheckLogin
    @Operation(summary = "上传文件（返回可直接引用的 url）")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制（最大 20MB）");
        }
        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "file";
        String ext = extensionOf(originalName);
        if (!StringUtils.hasText(ext) || DENIED_EXT.contains(ext) || !ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("不允许上传此类型的文件: " + (StringUtils.hasText(ext) ? ext : originalName));
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relative = datePath + "/" + fileName;

        Path target = resolve(relative);
        Files.createDirectories(target.getParent());
        file.transferTo(target.toFile());

        Map<String, Object> data = new HashMap<>();
        data.put("url", "/api/file/view/" + relative);
        data.put("fileId", fileName);
        data.put("name", originalName);
        data.put("size", file.getSize());
        data.put("type", file.getContentType());
        log.info("[file/upload] name={} size={} url=/api/file/view/{}", originalName, file.getSize(), relative);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/view/**")
    @Operation(summary = "访问文件内容（免登录，供 <img> 直接引用）")
    public ResponseEntity<byte[]> view(@RequestParam(required = false) String unused,
                                       jakarta.servlet.http.HttpServletRequest request) throws IOException {
        String uri = request.getRequestURI();
        int idx = uri.indexOf("/api/file/view/");
        if (idx < 0) return ResponseEntity.badRequest().build();
        String relative = uri.substring(idx + "/api/file/view/".length());
        if (!SAFE_RELATIVE.matcher(relative).matches()) {
            return ResponseEntity.badRequest().build();
        }
        Path path = resolve(relative);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = Files.readAllBytes(path);
        MediaType mediaType = MEDIA_TYPES.getOrDefault(extensionOf(relative), MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePublic())
                .body(bytes);
    }

    // ── 内部工具 ──

    private String extensionOf(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** 解析为存储根目录下的安全路径（防路径穿越） */
    private Path resolve(String relative) {
        Path base = Paths.get(basePath).toAbsolutePath().normalize();
        Path target = base.resolve(BIZ_DIR).resolve(relative).normalize();
        if (!target.startsWith(base)) {
            throw new IllegalArgumentException("非法文件路径: " + relative);
        }
        return target;
    }
}
