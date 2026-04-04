# AI-Ready 文件存储模块文档

## 概述

AI-Ready 文件存储模块提供统一的文件管理能力，支持本地存储和云存储，包含文件上传、下载、预览和校验功能。

## 模块结构

```
cn.aiedge.storage
├── config/                    # 配置类
│   └── StorageProperties.java # 存储配置属性
├── controller/                # 控制器
│   └── FileStorageController.java
├── model/                     # 模型类
│   ├── StorageFile.java       # 存储文件信息
│   └── FileTypeCategory.java  # 文件类型分类
├── service/                   # 服务接口
│   ├── FileStorageService.java
│   └── impl/
│       └── LocalFileStorageService.java
├── validator/                 # 校验器
│   └── FileValidator.java     # 文件校验
└── preview/                   # 预览服务
    └── FilePreviewService.java
```

## 核心功能

### 1. 文件上传

```java
@Autowired
private FileStorageService storageService;

// 上传文件
StorageFile file = storageService.upload(
    inputStream,          // 文件流
    "document.pdf",       // 原始文件名
    "application/pdf",    // MIME类型
    "contract",           // 业务类型
    "12345"               // 业务ID
);
```

### 2. 文件下载

```java
// 下载文件
InputStream stream = storageService.download(fileId);

// 获取文件信息
StorageFile fileInfo = storageService.getFileInfo(fileId);
```

### 3. 文件校验

文件校验器自动检查：
- 文件大小限制（默认100MB）
- 允许/禁止的扩展名
- 文件魔数（文件头）验证

```java
@Autowired
private FileValidator fileValidator;

// 校验上传文件
ValidationResult result = fileValidator.validate(multipartFile);
if (!result.isValid()) {
    throw new IllegalArgumentException(result.getMessage());
}
```

### 4. 文件预览

```java
@Autowired
private FilePreviewService previewService;

// 获取预览信息
PreviewInfo preview = previewService.getPreviewInfo(file);

if (preview.isPreviewable()) {
    String previewUrl = preview.getPreviewUrl();
    String previewType = preview.getPreviewType(); // image/pdf/video/audio/code
}
```

## 支持的文件类型

| 类型 | 扩展名 | 预览支持 |
|------|--------|----------|
| 图片 | jpg, jpeg, png, gif, bmp, webp, svg | ✅ |
| 文档 | pdf, doc, docx, xls, xlsx, ppt, pptx, txt | ✅ |
| 视频 | mp4, avi, mov, wmv, flv, mkv, webm | ✅ |
| 音频 | mp3, wav, flac, aac, ogg, wma | ✅ |
| 压缩包 | zip, rar, 7z, tar, gz, bz2 | ❌ |
| 代码 | java, py, js, ts, html, css, json, xml, sql, md | ✅ |

## 配置说明

### application.yml

```yaml
storage:
  # 存储类型：local/oss/s3
  type: local
  
  # 本地存储配置
  local:
    base-path: ./uploads
    base-url: http://localhost:8080
  
  # 文件上传配置
  upload:
    # 最大文件大小（字节）
    max-file-size: 104857600  # 100MB
    # 最大请求大小
    max-request-size: 524288000  # 500MB
    # 允许的扩展名
    allowed-extensions: jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,zip,mp4,mp3
    # 禁止的扩展名
    denied-extensions: exe,bat,cmd,sh,js,vbs,php,asp,jsp
    # 是否检查文件内容
    check-content: true
```

## API 接口

### 上传文件

```
POST /api/storage/upload
Content-Type: multipart/form-data

参数：
- file: 文件
- bizType: 业务类型（可选）
- bizId: 业务ID（可选）

响应：StorageFile 对象
```

### 下载文件

```
GET /api/storage/download/{fileId}

响应：文件流
```

### 获取文件信息

```
GET /api/storage/info/{fileId}

响应：StorageFile 对象
```

### 获取预览信息

```
GET /api/storage/preview/{fileId}

响应：PreviewInfo 对象
```

### 删除文件

```
DELETE /api/storage/{fileId}

响应：204 No Content
```

### 批量删除

```
DELETE /api/storage/batch
Content-Type: application/json

请求体：["fileId1", "fileId2", ...]

响应：删除数量
```

## 最佳实践

### 1. 文件命名规范

```java
// 使用生成的文件ID作为存储名
String storageName = fileId + "." + extension;

// 按日期分目录存储
String path = "2024/01/15/" + storageName;
```

### 2. 安全考虑

- 始终校验文件扩展名
- 检查文件魔数防止伪装
- 限制文件大小
- 不要暴露服务器内部路径

### 3. 大文件处理

```java
// 使用流式处理，避免内存溢出
try (InputStream is = file.getInputStream();
     OutputStream os = new FileOutputStream(targetPath)) {
    byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = is.read(buffer)) != -1) {
        os.write(buffer, 0, bytesRead);
    }
}
```

### 4. 业务关联

```java
// 上传时关联业务信息
storageService.upload(
    inputStream,
    fileName,
    contentType,
    "contract",      // 业务类型
    contractId       // 业务ID
);

// 按业务查询文件
List<StorageFile> files = fileService.getByBizTypeAndBizId("contract", contractId);
```

## 单元测试

模块包含完整的单元测试：

- `FileValidatorTest` - 文件校验测试
- `FilePreviewServiceTest` - 预览服务测试
- `FileTypeCategoryTest` - 文件类型分类测试

运行测试：

```bash
mvn test -Dtest=FileValidatorTest,FilePreviewServiceTest,FileTypeCategoryTest
```

## 版本历史

- v1.0.0 (2026-04-03)
  - 初始版本
  - 支持本地文件存储
  - 文件类型校验（扩展名+魔数）
  - 多种文件预览支持
  - 完整单元测试覆盖
