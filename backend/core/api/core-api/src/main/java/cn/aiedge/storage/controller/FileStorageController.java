package cn.aiedge.storage.controller;

import cn.aiedge.storage.model.StorageFile;
import cn.aiedge.storage.preview.FilePreviewService;
import cn.aiedge.storage.service.FileStorageService;
import cn.aiedge.storage.validator.FileValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件上传控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@Tag(name = "文件存储", description = "文件上传、下载、预览接口")
public class FileStorageController {

    private final FileStorageService fileStorageService;
    private final FileValidator fileValidator;
    private final FilePreviewService filePreviewService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public ResponseEntity<StorageFile> upload(
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务类型") @RequestParam(value = "bizType", required = false) String bizType,
            @Parameter(description = "业务ID") @RequestParam(value = "bizId", required = false) String bizId) {

        // 校验文件
        FileValidator.ValidationResult validationResult = fileValidator.validate(file);
        if (!validationResult.isValid()) {
            throw new IllegalArgumentException(validationResult.getMessage());
        }

        try {
            StorageFile storageFile = fileStorageService.upload(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    bizType,
                    bizId
            );
            return ResponseEntity.ok(storageFile);
        } catch (Exception e) {
            log.error("File upload failed", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/download/{fileId}")
    @Operation(summary = "下载文件")
    public ResponseEntity<Resource> download(
            @Parameter(description = "文件ID") @PathVariable String fileId) {

        StorageFile fileInfo = fileStorageService.getFileInfo(fileId);
        if (fileInfo == null) {
            return ResponseEntity.notFound().build();
        }

        InputStream inputStream = fileStorageService.download(fileId);
        Resource resource = new InputStreamResource(inputStream);

        String encodedFilename = URLEncoder.encode(fileInfo.getOriginalName(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileInfo.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + encodedFilename + "\"")
                .contentLength(fileInfo.getFileSize())
                .body(resource);
    }

    @GetMapping("/info/{fileId}")
    @Operation(summary = "获取文件信息")
    public ResponseEntity<StorageFile> getFileInfo(
            @Parameter(description = "文件ID") @PathVariable String fileId) {

        StorageFile fileInfo = fileStorageService.getFileInfo(fileId);
        if (fileInfo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(fileInfo);
    }

    @GetMapping("/preview/{fileId}")
    @Operation(summary = "获取文件预览信息")
    public ResponseEntity<FilePreviewService.PreviewInfo> getPreview(
            @Parameter(description = "文件ID") @PathVariable String fileId) {

        StorageFile fileInfo = fileStorageService.getFileInfo(fileId);
        if (fileInfo == null) {
            return ResponseEntity.notFound().build();
        }

        FilePreviewService.PreviewInfo previewInfo = filePreviewService.getPreviewInfo(fileInfo);
        return ResponseEntity.ok(previewInfo);
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除文件")
    public ResponseEntity<Void> delete(
            @Parameter(description = "文件ID") @PathVariable String fileId) {

        boolean deleted = fileStorageService.delete(fileId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除文件")
    public ResponseEntity<Integer> deleteBatch(
            @Parameter(description = "文件ID列表") @RequestBody List<String> fileIds) {

        int count = fileStorageService.deleteBatch(fileIds);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/url/{fileId}")
    @Operation(summary = "获取文件访问URL")
    public ResponseEntity<String> getAccessUrl(
            @Parameter(description = "文件ID") @PathVariable String fileId) {

        String url = fileStorageService.getAccessUrl(fileId);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(url);
    }

    @GetMapping("/presigned-url/{fileId}")
    @Operation(summary = "获取临时访问URL")
    public ResponseEntity<String> getPresignedUrl(
            @Parameter(description = "文件ID") @PathVariable String fileId,
            @Parameter(description = "过期时间(秒)") @RequestParam(defaultValue = "3600") int expireSeconds) {

        String url = fileStorageService.getPresignedUrl(fileId, expireSeconds);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(url);
    }
}
