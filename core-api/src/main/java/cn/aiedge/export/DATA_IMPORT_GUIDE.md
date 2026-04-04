# AI-Ready 数据导入功能文档

## 概述

本模块提供数据导入功能，支持Excel和CSV格式的批量数据导入，包括数据预览、校验、错误报告等功能。

## 模块结构

```
cn.aiedge.export
├── controller/
│   ├── BatchImportController.java   # 批量导入控制器（原有）
│   └── DataImportController.java    # 数据导入控制器（新增）
├── service/
│   ├── BatchImportService.java      # 批量导入服务接口
│   └── DataImportService.java       # 数据导入服务接口（新增）
└── impl/
    ├── BatchImportServiceImpl.java  # 批量导入服务实现
    └── DataImportServiceImpl.java   # 数据导入服务实现（新增）
```

## 支持的数据类型

| 类型 | 名称 | 主要字段 |
|------|------|----------|
| customer | 客户数据 | 客户名称、编码、联系人、电话、邮箱、地址 |
| product | 产品数据 | 产品名称、编码、分类、价格、库存、状态 |
| order | 订单数据 | 订单号、客户名称、产品名称、数量、单价、日期 |
| user | 用户数据 | 用户名、真实姓名、邮箱、手机号、部门、角色 |

## API接口

### 1. 导入Excel

```bash
POST /api/import/v2/excel/{dataType}
Content-Type: multipart/form-data

file: [Excel文件]
```

响应示例：
```json
{
  "success": true,
  "totalCount": 100,
  "successCount": 98,
  "failureCount": 2,
  "message": "导入完成，成功98条，失败2条",
  "errors": [
    { "rowIndex": 5, "field": "phone", "value": "123", "errorMessage": "手机号格式不正确" }
  ]
}
```

### 2. 导入CSV

```bash
POST /api/import/v2/csv/{dataType}
Content-Type: multipart/form-data

file: [CSV文件]
```

### 3. 预览数据

```bash
POST /api/import/v2/preview/{dataType}?maxRows=10
Content-Type: multipart/form-data

file: [文件]
```

响应示例：
```json
{
  "headers": ["客户名称", "联系人", "电话", "邮箱"],
  "rows": [
    { "客户名称": "张三公司", "联系人": "张三", "电话": "13800138001", "邮箱": "zhangsan@test.com" }
  ],
  "totalRows": 100,
  "previewRows": 10
}
```

### 4. 校验数据

```bash
POST /api/import/v2/validate/{dataType}
Content-Type: multipart/form-data

file: [文件]
```

响应示例：
```json
{
  "valid": false,
  "totalRows": 100,
  "validRows": 95,
  "invalidRows": 5,
  "errors": [
    { "rowIndex": 3, "field": "phone", "value": "123", "rule": "pattern", "message": "手机号格式不正确" }
  ]
}
```

### 5. 获取字段定义

```bash
GET /api/import/v2/fields/{dataType}
```

响应示例：
```json
{
  "dataType": "customer",
  "fields": [
    {
      "field": "customerName",
      "label": "客户名称",
      "type": "string",
      "required": true,
      "maxLength": 100,
      "pattern": null,
      "description": "客户全称"
    }
  ],
  "fieldCount": 7
}
```

### 6. 下载导入模板

```bash
GET /api/import/v2/template/{dataType}
```

## 数据校验规则

### 必填校验
- `required: true` 的字段不能为空

### 长度校验
- `maxLength` 限制字符串最大长度

### 格式校验（正则表达式）
| 字段 | 正则规则 | 说明 |
|------|----------|------|
| 手机号 | `^1[3-9]\d{9}$` | 11位手机号 |
| 邮箱 | `^[\w.-]+@[\w.-]+\.\w+$` | 邮箱格式 |
| 编码 | `^[A-Z0-9]+$` | 大写字母和数字 |
| 价格 | `^\d+(\.\d{1,2})?$` | 数字，最多2位小数 |
| 日期 | `^\d{4}-\d{2}-\d{2}$` | YYYY-MM-DD格式 |

### 枚举值校验
- `options` 定义可选值列表
- 值必须匹配 `value` 或 `label`

## 模板格式

下载的导入模板包含：
1. **表头行** - 字段名称，必填字段带 `*` 标记
2. **说明行** - 字段说明（黄色背景）
3. **示例行** - 示例数据

## 使用示例

### Java代码调用

```java
@Service
public class CustomerImportService {
    
    @Autowired
    private DataImportService dataImportService;
    
    public void importCustomers(MultipartFile file, Long tenantId) throws IOException {
        // 1. 预览数据
        PreviewResult preview = dataImportService.preview(file.getInputStream(), "customer", 10);
        log.info("预览: 共{}行数据", preview.totalRows());
        
        // 2. 校验数据
        ValidateResult validate = dataImportService.validate(preview.rows(), "customer");
        if (!validate.valid()) {
            log.warn("校验失败: {}行数据有问题", validate.invalidRows());
            return;
        }
        
        // 3. 执行导入
        ImportResult result = dataImportService.importExcel(file.getInputStream(), "customer", tenantId);
        log.info("导入完成: 成功{}条, 失败{}条", result.successCount(), result.failureCount());
    }
}
```

### 前端调用示例

```javascript
// 上传Excel文件
const formData = new FormData();
formData.append('file', file);

const response = await fetch('/api/import/v2/excel/customer', {
  method: 'POST',
  body: formData,
  headers: {
    'X-Tenant-Id': tenantId
  }
});

const result = await response.json();
if (result.success) {
  console.log(`导入成功: ${result.successCount}条`);
}
```

## 错误处理

导入失败时，系统会返回详细的错误信息：

```json
{
  "success": false,
  "message": "导入失败",
  "errors": [
    {
      "rowIndex": 3,
      "field": "customerName",
      "value": "",
      "errorMessage": "客户名称不能为空"
    }
  ]
}
```

## 性能建议

1. **分批导入**: 大文件建议分割成多个小文件
2. **异步处理**: 使用 `BatchImportService` 进行异步导入
3. **预校验**: 先调用校验接口确认数据质量

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-04-04 | 初始版本，支持Excel/CSV导入 |

---

*AI-Ready Team © 2026*
