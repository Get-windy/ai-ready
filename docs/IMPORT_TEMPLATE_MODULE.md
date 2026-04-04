# AI-Ready 导入模板开发文档

## 版本信息
- 版本：v1.1.0
- 更新日期：2026-04-04
- 开发人员：team-member

## 一、概述

导入模板系统提供标准化的数据导入能力，支持：
- 模板定义与管理
- 带验证的Excel模板生成
- 数据校验与错误反馈
- 多数据类型支持

## 二、模块结构

```
cn.aiedge.export.template/
├── ImportTemplateDefinition.java   # 模板定义实体
├── ImportTemplateRegistry.java     # 模板注册表
├── ImportTemplateGenerator.java    # 模板生成器
└── ImportTemplateValidator.java    # 模板校验器
```

## 三、模板定义

### 3.1 字段类型

| 类型 | 说明 | 示例 |
|------|------|------|
| STRING | 字符串 | "张三" |
| INTEGER | 整数 | 100 |
| DECIMAL | 小数 | 99.99 |
| DATE | 日期 | 2026-04-04 |
| DATETIME | 日期时间 | 2026-04-04 10:30:00 |
| BOOLEAN | 布尔 | 是/否 |
| ENUM | 枚举 | 从下拉列表选择 |
| EMAIL | 邮箱 | test@example.com |
| PHONE | 手机号 | 13800138000 |
| ID_CARD | 身份证 | 110101199001011234 |
| URL | 网址 | https://example.com |
| REFERENCE | 引用 | 引用其他实体 |

### 3.2 校验规则

```java
TemplateField.builder()
    .fieldName("username")
    .fieldTitle("用户名")
    .fieldType(FieldType.STRING)
    .required(true)              // 必填
    .maxLength(50)               // 最大长度
    .unique(true)                // 唯一性
    .regexPattern("^[a-zA-Z][a-zA-Z0-9_]*$")  // 正则
    .build();
```

### 3.3 默认模板

系统预置四种导入模板：
- 用户导入（user）
- 客户导入（customer）
- 产品导入（product）
- 订单导入（order）

## 四、API接口

### 4.1 模板管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/import-templates | 获取所有模板 |
| GET | /api/import-templates/{templateId} | 获取模板详情 |
| GET | /api/import-templates/data-type/{dataType} | 按类型获取模板 |
| GET | /api/import-templates/data-types | 获取支持的数据类型 |
| POST | /api/import-templates | 注册新模板 |
| PUT | /api/import-templates/{templateId} | 更新模板 |
| DELETE | /api/import-templates/{templateId} | 删除模板 |

### 4.2 模板下载

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/import-templates/{templateId}/download | 下载模板文件 |
| GET | /api/import-templates/data-type/{dataType}/download | 按类型下载模板 |

### 4.3 数据校验

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/import-templates/{templateId}/validate | 校验导入文件 |
| POST | /api/import-templates/{templateId}/validate-row | 校验单行数据 |
| GET | /api/import-templates/{templateId}/preview | 预览模板结构 |
| GET | /api/import-templates/{templateId}/fields/{fieldName}/validation | 获取字段校验规则 |

## 五、使用示例

### 5.1 下载模板

```bash
# 下载用户导入模板
curl -O http://localhost:8080/api/import-templates/tpl_user_import/download

# 按数据类型下载
curl -O http://localhost:8080/api/import-templates/data-type/user/download
```

### 5.2 校验导入数据

```bash
# 上传文件校验
curl -X POST http://localhost:8080/api/import-templates/tpl_user_import/validate \
  -F "file=@users.xlsx"
```

### 5.3 校验单行数据

```bash
curl -X POST http://localhost:8080/api/import-templates/tpl_user_import/validate-row \
  -H "Content-Type: application/json" \
  -d '{
    "username": "zhangsan",
    "email": "invalid-email",
    "phone": "12345678901"
  }'
```

响应：
```json
{
  "valid": false,
  "errors": [
    {
      "rowNum": 1,
      "fieldName": "email",
      "message": "无效的邮箱格式"
    },
    {
      "rowNum": 1,
      "fieldName": "phone",
      "message": "无效的手机号格式"
    }
  ]
}
```

### 5.4 注册自定义模板

```bash
curl -X POST http://localhost:8080/api/import-templates \
  -H "Content-Type: application/json" \
  -d '{
    "templateId": "tpl_employee_import",
    "dataType": "employee",
    "templateName": "员工导入模板",
    "description": "批量导入员工数据",
    "version": "1.0.0",
    "fields": [
      {
        "fieldName": "employeeNo",
        "fieldTitle": "工号",
        "fieldType": "STRING",
        "required": true,
        "unique": true,
        "maxLength": 20
      },
      {
        "fieldName": "name",
        "fieldTitle": "姓名",
        "fieldType": "STRING",
        "required": true,
        "maxLength": 50
      },
      {
        "fieldName": "department",
        "fieldTitle": "部门",
        "fieldType": "ENUM",
        "required": true,
        "dropdownOptions": {
          "tech": "技术部",
          "sales": "销售部",
          "hr": "人事部"
        }
      }
    ],
    "maxImportRows": 5000,
    "strictValidation": true
  }'
```

## 六、模板生成特性

### 6.1 Excel模板结构

生成的Excel模板包含：
1. **标题行**：字段名称，必填字段标*
2. **说明行**：字段类型、限制说明
3. **示例行**：示例数据（浅黄色背景）
4. **数据验证**：下拉列表、数值范围、文本长度

### 6.2 数据验证类型

| 验证类型 | 实现方式 | 示例 |
|----------|----------|------|
| 下拉列表 | Excel数据验证-列表 | 部门选择 |
| 数值范围 | Excel数据验证-数值 | 价格0-999999 |
| 文本长度 | Excel数据验证-文本长度 | 名称≤100字符 |
| 日期格式 | 单元格格式 | yyyy-MM-dd |

### 6.3 说明Sheet

自动生成"填写说明"Sheet，包含：
- 模板名称与描述
- 字段说明表格
- 注意事项

## 七、校验规则详解

### 7.1 类型校验

```java
// INTEGER
Integer.parseInt(value);  // 必须能解析为整数

// DECIMAL
new BigDecimal(value);    // 必须能解析为数值

// DATE
支持格式：yyyy-MM-dd, yyyy/MM/dd, yyyy.MM.dd, yyyyMMdd

// DATETIME
支持格式：yyyy-MM-dd HH:mm:ss, yyyy/MM/dd HH:mm:ss 等
```

### 7.2 正则校验

内置正则：
- **邮箱**：`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`
- **手机号**：`^1[3-9]\d{9}$`
- **身份证**：18位身份证号格式
- **网址**：`^(https?://)?[\w.-]+\.[a-zA-Z]{2,}(/\S*)?$`

### 7.3 自定义校验

```java
TemplateField.builder()
    .fieldName("customField")
    .regexPattern("^[A-Z]{2}\\d{6}$")  // 自定义正则
    .customValidator("com.example.CustomValidator")  // 自定义校验器类
    .build();
```

## 八、扩展指南

### 8.1 添加新数据类型

```java
// 在 ImportTemplateRegistry 中添加新模板
private ImportTemplateDefinition createEmployeeTemplate() {
    return ImportTemplateDefinition.builder()
        .templateId("tpl_employee_import")
        .dataType("employee")
        .templateName("员工导入模板")
        .fields(Arrays.asList(
            // 字段定义...
        ))
        .build();
}

// 在 registerDefaultTemplates() 中注册
register(createEmployeeTemplate());
```

### 8.2 自定义字段类型

```java
// 1. 在 FieldType 枚举中添加新类型
public enum FieldType {
    // 现有类型...
    CUSTOM_TYPE
}

// 2. 在 ImportTemplateValidator 中添加校验逻辑
private FieldError validateSpecialType(TemplateField field, String value, int rowNum) {
    switch (field.getFieldType()) {
        case CUSTOM_TYPE:
            // 自定义校验逻辑
            break;
    }
}
```

## 九、性能建议

1. **批量导入**：使用流式读取大文件，避免内存溢出
2. **异步处理**：大数据量导入使用异步任务
3. **缓存模板**：模板定义已缓存，无需重复查询
4. **并行校验**：大数据量可分片并行校验

---
*文档更新时间：2026-04-04*
