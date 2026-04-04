# AI-Ready 文件存储模块文档

## 版本信息
- 版本：v1.0.0
- 更新日期：2026-04-04
- 开发人员：team-member

## 一、模块架构

```
cn.aiedge.storage/
├── config/
│   ├── StorageConfig.java         # 存储配置
│   └── StorageProperties.java     # 配置属性
├── controller/
│   └── FileStorageController.java # 文件存储接口
├── mapper/
│   └── FileInfoMapper.java        # 数据访问
├── model/
│   ├── FileInfo.java              # 文件信息
│   ├── FileTypeCategory.java      # 文件分类
│   └── StorageFile.java           # 存储文件实体
├── preview/
│   └── FilePreviewService.java    # 文件预览服务
├── service/
│   ├── FileStorageService.java    # 存储服务接口
│   ├── StorageStrategy.java       # 存储策略接口
│   └── impl/
│       ├── LocalFileStorageService.java
│       ├── LocalStorageStrategy.java
│       ├── OssStorageStrategy.java      # 阿里云OSS
│       ├── S3StorageStrategy.java       # AWS S3
│       ├── MinioStorageStrategy.java    # MinIO
│       └── MultiStorageServiceImpl.java # 多存储实现
└── validator/
    └── FileValidator.java         # 文件校验器
```

## 二、支持的存储类型

| 存储类型 | 说明 | 配置值 |
|----------|------|--------|
| 本地存储 | 本地磁盘存储 | local |
| 阿里云OSS | 阿里云对象存储 | oss |
| AWS S3 | 亚马逊S3对象存储 | s3 |
| MinIO | 开源对象存储 | minio |

## 三、配置说明

### 3.1 本地存储配置

```yaml
storage:
  type: local
  local:
    base-path: ./uploads
    base-url: http://localhost:8080/files
```

### 3.2 阿里云OSS配置

```yaml
storage:
  type: oss
  oss:
    enabled: true
    endpoint: oss-cn-hangzhou.aliyuncs.com
    access-key-id: your-access-key
    access-key-secret: your-secret
    bucket-name: your-bucket
    domain: https://cdn.example.com
    path-prefix: ai-ready/
```

### 3.3 AWS S3配置

```yaml
storage:
  type: s3
  s3:
    enabled: true
    region: us-east-1
    endpoint: https://s3.amazonaws.com
    access-key-id: your-access-key
    secret-access-key: your-secret
    bucket-name: your-bucket
    path-prefix: ai-ready/
```

### 3.4 MinIO配置

```yaml
storage:
  type: minio
  s3:
    enabled: true
    endpoint: http://localhost:9000
    region: us-east-1
    access-key-id: minioadmin
    secret-access-key: minioadmin
    bucket-name: ai-ready
    path-prefix: ""
```

### 3.5 上传限制配置

```yaml
storage:
  upload:
    max-file-size: 104857600        # 100MB
    max-request-size: 524288000     # 500MB
    allowed-extensions: jpg,jpeg,png,gif,pdf,doc,docx
    denied-extensions: exe,bat,sh,php
    check-content: true
```

## 四、API接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/storage/upload | 上传文件 |
| POST | /api/storage/upload/batch | 批量上传 |
| GET | /api/storage/download/{fileId} | 下载文件 |
| GET | /api/storage/info/{fileId} | 获取文件信息 |
| DELETE | /api/storage/{fileId} | 删除文件 |
| GET | /api/storage/url/{fileId} | 获取访问URL |
| GET | /api/storage/url/{fileId}/presigned | 获取临时访问URL |
| POST | /api/storage/copy/{fileId} | 复制文件 |
| POST | /api/storage/move/{fileId} | 移动文件 |

## 五、使用示例

### 5.1 文件上传

```java
@Autowired
private FileStorageService fileStorageService;

// 上传文件
StorageFile file = fileStorageService.upload(
    inputStream,
    "document.pdf",
    "application/pdf",
    "contract",
    "CONTRACT-001"
);

String fileId = file.getFileId();
```

### 5.2 文件下载

```java
InputStream stream = fileStorageService.download(fileId);
```

### 5.3 获取访问URL

```java
// 永久URL
String url = fileStorageService.getAccessUrl(fileId);

// 临时URL（有效期1小时）
String presignedUrl = fileStorageService.getPresignedUrl(fileId, 3600);
```

### 5.4 文件操作

```java
// 复制文件
StorageFile copy = fileStorageService.copy(fileId, "backup", "BACKUP-001");

// 移动文件
StorageFile moved = fileStorageService.move(fileId, "archive", "ARCHIVE-001");

// 删除文件
boolean deleted = fileStorageService.delete(fileId);
```

## 六、存储策略接口

```java
public interface StorageStrategy {
    String getType();
    FileInfo upload(InputStream inputStream, String fileName, String contentType);
    InputStream download(String filePath);
    boolean delete(String filePath);
    String getAccessUrl(String filePath, int expireSeconds);
    boolean exists(String filePath);
    long getFileSize(String filePath);
    boolean copy(String sourcePath, String targetPath);
    boolean move(String sourcePath, String targetPath);
}
```

## 七、文件存储路径规则

```
{bizType}/{year}/{month}/{day}/{uuid}.{ext}

示例：
contract/2026/04/04/a1b2c3d4e5f6.pdf
avatar/2026/04/04/x1y2z3a4b5c6.jpg
```

## 八、文件校验

### 8.1 文件类型校验
- 扩展名白名单/黑名单
- MIME类型检查
- 文件魔数检测

### 8.2 文件大小校验
- 单文件大小限制
- 批量上传总大小限制

### 8.3 安全校验
- 文件名消毒
- 路径遍历防护
- 恶意文件检测

## 九、最佳实践

1. **选择合适的存储类型**：本地开发用local，生产环境用OSS/S3
2. **使用临时URL**：避免暴露存储凭证
3. **设置合理的过期时间**：临时URL有效期不超过1小时
4. **定期清理无用文件**：避免存储空间浪费
5. **备份重要文件**：关键数据应有多份副本
6. **监控存储使用量**：设置告警阈值

---
*文档更新时间：2026-04-04*
