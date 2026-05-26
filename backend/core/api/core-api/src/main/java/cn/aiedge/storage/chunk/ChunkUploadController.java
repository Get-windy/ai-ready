package cn.aiedge.storage.chunk;

import cn.aiedge.storage.model.StorageFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 分片上传控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/storage/chunk")
@RequiredArgsConstructor
@Tag(name = "分片上传", description = "大文件分片上传接口")
public class ChunkUploadController {

    private final ChunkUploadService chunkUploadService;

    @PostMapping("/init")
    @Operation(summary = "初始化分片上传")
    public ResponseEntity<ChunkUploadInfo> initUpload(
            @Parameter(description = "文件名") @RequestParam String fileName,
            @Parameter(description = "文件大小") @RequestParam Long fileSize,
            @Parameter(description = "文件MD5") @RequestParam(required = false) String fileMd5,
            @Parameter(description = "分片大小") @RequestParam(required = false) Integer chunkSize,
            @Parameter(description = "业务类型") @RequestParam(required = false) String bizType,
            @Parameter(description = "业务ID") @RequestParam(required = false) String bizId) {

        ChunkUploadInfo uploadInfo = chunkUploadService.initUpload(
                fileName, fileSize, fileMd5, chunkSize, bizType, bizId);
        return ResponseEntity.ok(uploadInfo);
    }

    @PostMapping("/upload")
    @Operation(summary = "上传分片")
    public ResponseEntity<ChunkUploadInfo> uploadChunk(
            @Parameter(description = "上传ID") @RequestParam String uploadId,
            @Parameter(description = "分片序号") @RequestParam Integer chunkNumber,
            @Parameter(description = "分片MD5") @RequestParam(required = false) String chunkMd5,
            @Parameter(description = "分片数据") @RequestParam("chunk") MultipartFile chunk) {

        try {
            ChunkUploadInfo uploadInfo = chunkUploadService.uploadChunk(
                    uploadId, chunkNumber, chunkMd5, chunk.getInputStream());
            return ResponseEntity.ok(uploadInfo);
        } catch (Exception e) {
            log.error("Chunk upload failed", e);
            throw new RuntimeException("分片上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/merge/{uploadId}")
    @Operation(summary = "合并分片")
    public ResponseEntity<StorageFile> mergeChunks(
            @Parameter(description = "上传ID") @PathVariable String uploadId) {

        StorageFile storageFile = chunkUploadService.mergeChunks(uploadId);
        return ResponseEntity.ok(storageFile);
    }

    @GetMapping("/info/{uploadId}")
    @Operation(summary = "获取上传信息")
    public ResponseEntity<ChunkUploadInfo> getUploadInfo(
            @Parameter(description = "上传ID") @PathVariable String uploadId) {

        ChunkUploadInfo uploadInfo = chunkUploadService.getUploadInfo(uploadId);
        if (uploadInfo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(uploadInfo);
    }

    @DeleteMapping("/abort/{uploadId}")
    @Operation(summary = "取消上传")
    public ResponseEntity<Void> abortUpload(
            @Parameter(description = "上传ID") @PathVariable String uploadId) {

        boolean success = chunkUploadService.abortUpload(uploadId);
        if (success) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/pending")
    @Operation(summary = "获取待处理上传列表")
    public ResponseEntity<List<ChunkUploadInfo>> getPendingUploads(
            @Parameter(description = "业务类型") @RequestParam(required = false) String bizType,
            @Parameter(description = "业务ID") @RequestParam(required = false) String bizId) {

        List<ChunkUploadInfo> uploads = chunkUploadService.getPendingUploads(bizType, bizId);
        return ResponseEntity.ok(uploads);
    }

    @PostMapping("/check")
    @Operation(summary = "检查文件是否已存在（秒传）")
    public ResponseEntity<Map<String, Object>> checkFileExists(
            @Parameter(description = "文件MD5") @RequestParam String fileMd5) {

        StorageFile existingFile = chunkUploadService.checkFileExists(fileMd5);
        if (existingFile != null) {
            return ResponseEntity.ok(Map.of(
                    "exists", true,
                    "file", existingFile,
                    "message", "文件已存在，可使用秒传"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "exists", false,
                "message", "文件不存在，需要上传"
        ));
    }
}
