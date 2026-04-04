# AI-Ready 数据导出模块文档

## 概述

AI-Ready 数据导出模块提供统一的Excel、CSV和PDF处理能力，支持大数据量分批处理和数据校验。

## 模块结构

```
cn.aiedge.export
├── controller/                   # 控制器
│   ├── DataExportController.java
│   └── DataExportControllerExt.java
├── handler/                      # 处理器
│   ├── ExportConfig.java         # 导出配置
│   └── ImportContext.java        # 导入上下文
└── service/                      # 服务层
    ├── DataExportService.java
    ├── PdfExportService.java     # PDF导出
    └── impl/
        └── DataExportServiceImpl.java
```

## 核心功能

### 1. Excel导出

```java
@Autowired
private DataExportService exportService;

List<User> users = userRepository.findAll();
Map<String, String> headers = Map.of(
    "username", "用户名",
    "email", "邮箱",
    "phone", "手机号"
);
exportService.exportExcel(users, headers, response.getOutputStream());
```

### 2. CSV导出

```java
exportService.exportCsv(data, headers, outputStream);
```

### 3. PDF导出

```java
@Autowired
private PdfExportService pdfExportService;

// 导出PDF表格
pdfExportService.exportPdf(users, headers, "用户列表", outputStream);

// 导出PDF报告
pdfExportService.exportReport("报告内容...", "系统报告", outputStream);
```

### 4. 大数据量分批导出

```java
exportService.exportExcelBatch(
    (page, size) -> userRepository.findAll(PageRequest.of(page, size)).getContent(),
    headers,
    outputStream,
    1000  // 每批1000条
);
```

## API 接口

### 导出Excel

```
POST /api/export/excel/export
```

### 导出CSV

```
POST /api/export/csv/export
```

### 导出PDF

```
POST /api/export/pdf/export
{
    "filename": "用户列表",
    "title": "用户数据",
    "dataType": "user",
    "headers": {"username": "用户名", "email": "邮箱"}
}
```

### 导出PDF报告

```
POST /api/export/pdf/report
{
    "filename": "系统报告",
    "title": "月度报告",
    "content": "报告内容..."
}
```

## 支持格式

| 格式 | 导出 | 导入 | 特点 |
|------|------|------|------|
| Excel | ✅ | ✅ | 支持样式、多Sheet |
| CSV | ✅ | ✅ | 轻量、通用 |
| PDF | ✅ | - | 只读、打印友好 |

## 版本历史

- v1.0.0 (2026-04-03)
  - 支持Excel/CSV/PDF三种格式
  - 大数据量分批处理
  - 导入数据校验
