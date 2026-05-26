# AI-Ready 文件存储模块文档

## 概述

本模块提供统一的文件存储管理能力，支持多种存储后端（本地、MinIO、OSS、S3等）。

## 模块结构

```
cn.aiedge.storage
├── config/
│   └── StorageConfig.java       # 存储配置
├── controller/
│   └── FileStorageController.java # 文件接口
├── mapper/
│   └── FileInfoMapper.java      # 数据访问层
├── model/
│   └── FileInfo.java            # 文件元数据实体
└── service/
    ├── FileStorageService.java  # 文件存储服务
    ├── StorageStrategy.java     # 存储策略接口
    └── impl/
        └── LocalStorageStrategy.java # 本地存储实现
```

## 支持的存储类型

| 类型 | 说明 | 配置前缀 |
|------|------|----------|
| LOCAL | 本地文件系统 | storage.local |
| MINIO | MinIO对象存储 | storage.minio |
| OSS | 阿里云OSS | storage.oss |
| S3 | AWS S3 | storage.s3 |

## 快速开始

### 1. 上传文件

```bash
# 单文件上传
POST /api/storage/upload
Content-Type: multipart/form-data

file: [文件]
bizType: product
bizId: 1001

# 批量上传
POST /api/storage/upload/batch
Content-Type: multipart/form-data

files: [文件1, 文件2, ...]
bizType: product
bizId: 1001
```

### 2. 下载文件

```bash
GET /api/storage/download/{fileId}
```

### 3. 获取访问URL

```bash
GET /api/storage/url/{fileId}?expire=3600
```

### 4. 删除文件

```bash
DELETE /api/storage/{fileId}
```

### 5. 查询文件列表

```bash
# 按业务查询
GET /api/storage/list?bizType=product&bizId=1001

# 分页查询
GET /api/storage/page?page=1&pageSize=10&bizType=product&fileName=test
```

## 代码示例

### 上传文件

```java
@Autowired
private FileStorageService fileStorageService;

// 上传文件
FileInfo fileInfo = fileStorageService.upload(file, "product", productId);

// 指定存储类型
FileInfo fileInfo = fileStorageService.upload(file, "product", productId, "OSS");
```

### 下载文件

```java
// 下载文件流
InputStream inputStream = fileStorageService.download(fileId);

// 获取访问URL
String url = fileStorageService.getAccessUrl(fileId, 3600); // 1小时有效
```

### 删除文件

```java
// 删除单个文件
boolean success = fileStorageService.deleteFile(fileId);

// 批量删除
int count = fileStorageService.deleteBatch(Arrays.asList(1L, 2L, 3L));
```

## 配置说明

### application.yml

```yaml
# 本地存储配置
storage:
  local:
    base-path: ./uploads        # 存储根目录
    base-url: http://localhost:8080/files  # 访问URL前缀
  
  # MinIO配置（可选）
  minio:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket-name: ai-ready
  
  # 阿里云OSS配置（可选）
  oss:
    endpoint: https://oss-cn-hangzhou.aliyuncs.com
    access-key-id: your-access-key-id
    access-key-secret: your-access-key-secret
    bucket-name: ai-ready
```

## 扩展存储策略

实现 `StorageStrategy` 接口：

```java
@Component("minioStorageStrategy")
public class MinioStorageStrategy implements StorageStrategy {
    
    @Override
    public String getType() {
        return "MINIO";
    }
    
    @Override
    public FileInfo upload(InputStream inputStream, String fileName, String contentType) {
        // 实现MinIO上传逻辑
    }
    
    // ... 其他方法实现
}
```

## 文件存储路径规则

```
{basePath}/{yyyy}/{MM}/{dd}/{uuid}.{extension}

示例：
./uploads/2026/04/03/abc123.jpg
```

## 安全特性

1. **访问权限控制**: 支持 PUBLIC、PRIVATE、PROTECTED 三种访问级别
2. **文件类型校验**: 支持配置允许上传的文件类型
3. **文件大小限制**: 支持配置最大文件大小
4. **病毒扫描**: 可集成病毒扫描服务

## 最佳实践

1. **大文件分片上传**: 对于大文件，使用分片上传提高成功率
2. **图片压缩**: 上传图片前进行压缩处理
3. **CDN加速**: 配合CDN提高文件访问速度
4. **定期清理**: 定期清理无用的临时文件