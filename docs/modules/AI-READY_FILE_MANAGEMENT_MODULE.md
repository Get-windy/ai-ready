# AI-Ready 文件管理模块

## 模块概述

文件管理模块提供完整的文件存储、预览、权限控制功能，支持多种存储后端和分片上传。

## 功能特性

### 1. 文件上传下载
- ✅ 单文件上传
- ✅ 批量文件上传
- ✅ 分片上传（大文件支持）
- ✅ 断点续传
- ✅ 秒传（MD5校验）
- ✅ 文件下载
- ✅ 临时访问URL

### 2. 文件预览
- ✅ 图片预览（jpg/png/gif/webp/svg）
- ✅ PDF预览
- ✅ Office文档预览（doc/docx/xls/xlsx/ppt/pptx）
- ✅ 视频预览（mp4/webm/mkv）
- ✅ 音频预览（mp3/wav/ogg）
- ✅ 代码预览（语法高亮）
- ✅ 缩略图生成
- ✅ 水印添加

### 3. 文件权限控制
- ✅ 五级权限：READ/WRITE/DELETE/SHARE/ADMIN
- ✅ 四种授权对象：用户/角色/部门/所有人
- ✅ 权限继承
- ✅ 权限过期
- ✅ 权限注解AOP

### 4. 存储支持
- ✅ 本地存储
- ✅ MinIO存储
- ✅ 阿里云OSS
- ✅ AWS S3
- ✅ 多存储策略

## 目录结构

```
storage/
├── config/          # 配置类
│   ├── StorageConfig.java
│   └── StorageProperties.java
├── controller/      # 控制器
│   └── FileStorageController.java
├── model/           # 模型类
│   ├── FileInfo.java
│   ├── StorageFile.java
│   └── FileTypeCategory.java
├── service/         # 服务接口
│   ├── FileStorageService.java
│   └── StorageStrategy.java
├── impl/            # 服务实现
│   ├── LocalFileStorageService.java
│   ├── MinioStorageStrategy.java
│   ├── OssStorageStrategy.java
│   └── S3StorageStrategy.java
├── preview/         # 预览服务
│   ├── FilePreviewService.java
│   └── EnhancedPreviewService.java
├── permission/      # 权限控制
│   ├── FilePermission.java
│   ├── FilePermissionService.java
│   ├── FilePermissionMapper.java
│   ├── FilePermissionAspect.java
│   └── RequireFilePermission.java
├── chunk/           # 分片上传
│   ├── ChunkUploadInfo.java
│   ├── ChunkUploadService.java
│   ├── ChunkUploadServiceImpl.java
│   ├── ChunkUploadController.java
│   └── ChunkStorageProperties.java
├── validator/       # 文件校验
│   └── FileValidator.java
└── mapper/          # 数据访问
    └── FileInfoMapper.java
```

## API 接口

### 文件上传下载

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/storage/upload | 上传文件 |
| GET | /api/storage/download/{fileId} | 下载文件 |
| GET | /api/storage/info/{fileId} | 获取文件信息 |
| DELETE | /api/storage/{fileId} | 删除文件 |
| DELETE | /api/storage/batch | 批量删除 |
| GET | /api/storage/url/{fileId} | 获取访问URL |
| GET | /api/storage/presigned-url/{fileId} | 获取临时URL |

### 文件预览

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/storage/preview/{fileId} | 获取预览信息 |

### 分片上传

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/storage/chunk/init | 初始化分片上传 |
| POST | /api/storage/chunk/upload | 上传分片 |
| POST | /api/storage/chunk/merge/{uploadId} | 合并分片 |
| GET | /api/storage/chunk/info/{uploadId} | 获取上传信息 |
| DELETE | /api/storage/chunk/abort/{uploadId} | 取消上传 |
| POST | /api/storage/chunk/check | 检查文件是否存在 |

## 使用示例

### 1. 文件上传

```java
@RestController
@RequestMapping("/api/example")
public class ExampleController {
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @PostMapping("/upload")
    public StorageFile upload(@RequestParam("file") MultipartFile file) throws IOException {
        return fileStorageService.upload(
            file.getInputStream(),
            file.getOriginalFilename(),
            file.getContentType(),
            "example", // bizType
            "123"      // bizId
        );
    }
}
```

### 2. 权限检查

```java
@GetMapping("/files/{fileId}")
@RequireFilePermission(value = PermissionType.READ)
public ResponseEntity<StorageFile> getFile(@PathVariable String fileId) {
    return ResponseEntity.ok(fileStorageService.getFileInfo(fileId));
}
```

### 3. 分片上传流程

```javascript
// 1. 初始化上传
const initResponse = await fetch('/api/storage/chunk/init', {
    method: 'POST',
    body: JSON.stringify({
        fileName: 'large_file.zip',
        fileSize: 100 * 1024 * 1024, // 100MB
        chunkSize: 5 * 1024 * 1024    // 5MB
    })
});
const { uploadId, totalChunks } = await initResponse.json();

// 2. 上传分片
for (let i = 1; i <= totalChunks; i++) {
    const chunk = file.slice(
        (i - 1) * chunkSize,
        Math.min(i * chunkSize, fileSize)
    );
    const formData = new FormData();
    formData.append('uploadId', uploadId);
    formData.append('chunkNumber', i);
    formData.append('chunk', chunk);
    await fetch('/api/storage/chunk/upload', {
        method: 'POST',
        body: formData
    });
}

// 3. 合并分片
const mergeResponse = await fetch(`/api/storage/chunk/merge/${uploadId}`, {
    method: 'POST'
});
const storageFile = await mergeResponse.json();
```

## 配置说明

```yaml
# application.yml
storage:
  type: local  # local, minio, oss, s3
  
  local:
    base-path: /data/files
    base-url: http://localhost:8080
  
  minio:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket-name: ai-ready
  
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.com
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    bucket-name: ai-ready
  
  upload:
    max-file-size: 104857600  # 100MB
    allowed-extensions: jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx
    denied-extensions: exe,bat,sh,cmd
    check-content: true
  
  chunk:
    temp-path: /tmp/file-chunks
    default-chunk-size: 5242880  # 5MB
    max-chunk-size: 104857600    # 100MB
    expire-hours: 24
    enable-instant-upload: true
```

## 数据库表

### sys_file - 文件信息表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 文件ID |
| file_name | VARCHAR(255) | 文件名 |
| storage_name | VARCHAR(255) | 存储文件名 |
| file_path | VARCHAR(500) | 文件路径 |
| file_size | BIGINT | 文件大小 |
| file_type | VARCHAR(100) | MIME类型 |
| storage_type | VARCHAR(50) | 存储类型 |
| file_md5 | VARCHAR(64) | MD5值 |
| biz_type | VARCHAR(100) | 业务类型 |
| biz_id | BIGINT | 业务ID |
| access_level | VARCHAR(20) | 访问权限 |

### sys_file_permission - 文件权限表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 权限ID |
| file_id | BIGINT | 文件ID |
| permission_type | VARCHAR(20) | 权限类型 |
| principal_type | VARCHAR(20) | 授权对象类型 |
| principal_id | BIGINT | 授权对象ID |
| expire_time | DATETIME | 过期时间 |

## 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 1.0.0 | 2026-04-04 | 初始版本，基础存储功能 |
| 1.1.0 | 2026-04-04 | 新增权限控制、分片上传 |

---

*AI-Ready Team*
