# 企智连数据导入导出测试

## 概述

本测试套件用于验证企智连系统的数据导入导出模块功能，确保数据迁移的准确性和完整性。

## 测试范围

### 1. Excel导入测试
- 单表数据导入
- 多Sheet导入
- 大数据量导入性能
- 数据校验（必填项、格式、重复）
- 导入模板验证

### 2. Excel导出测试
- 列表数据导出
- 带筛选条件导出
- 大数据量导出（分页、异步）
- 导出字段自定义
- 导出格式（.xlsx/.csv）

### 3. JSON/XML导入导出测试
- API数据导入
- 系统配置导出
- 嵌套数据处理
- 批量导入

## 技术栈

- Java 11
- JUnit 5
- Apache POI 5.2.3
- Jackson 2.14.2

## 运行测试

```bash
# 编译并运行所有测试
mvn clean test

# 运行特定标签的测试
mvn test -Dgroups=import
mvn test -Dgroups=export
mvn test -Dgroups=performance

# 运行测试套件
mvn test -Dtest=DataImportExportTestSuite
```

## 测试用例清单

| 编号 | 名称 | 类型 | 标签 |
|------|------|------|------|
| TC-IE-001 | 单表数据导入测试 | 导入 | import |
| TC-IE-002 | 多Sheet导入测试 | 导入 | import |
| TC-IE-003 | 大数据量导入性能测试 | 导入 | import, performance |
| TC-IE-004 | 必填项验证测试 | 导入 | import, validation |
| TC-IE-005 | 数据格式验证测试 | 导入 | import, validation |
| TC-IE-006 | 重复数据检测测试 | 导入 | import, validation |
| TC-IE-007 | 导入模板下载测试 | 导入 | import |
| TC-IE-008 | 导入进度反馈测试 | 导入 | import |
| TC-IE-009 | 导入错误处理测试 | 导入 | import |
| TC-IE-010 | 导入事务回滚测试 | 导入 | import |
| TC-EE-001 | 列表数据导出测试 | 导出 | export |
| TC-EE-002 | 带筛选条件导出测试 | 导出 | export |
| TC-EE-003 | 大数据量导出分页测试 | 导出 | export, performance |
| TC-EE-004 | 导出字段自定义测试 | 导出 | export |
| TC-EE-005 | Excel格式导出测试 | 导出 | export |
| TC-EE-006 | CSV格式导出测试 | 导出 | export |
| TC-EE-007 | 异步导出测试 | 导出 | export |
| TC-EE-008 | 导出模板下载测试 | 导出 | export |
| TC-JX-001 | API数据JSON导入测试 | JSON | json, import |
| TC-JX-002 | 系统配置JSON导出测试 | JSON | json, export |
| TC-JX-003 | JSON数据格式验证测试 | JSON | json, validation |
| TC-JX-004 | XML数据导出测试 | XML | xml, export |
| TC-JX-005 | 嵌套JSON数据处理测试 | JSON | json, import |
| TC-JX-006 | JSON数组批量导入测试 | JSON | json, import |

## 目录结构

```
data_import_export/
├── pom.xml
├── README.md
└── src/test/java/com/qizhilian/importexport/
    ├── ExcelImportTest.java
    ├── ExcelExportTest.java
    ├── JsonXmlImportExportTest.java
    ├── DataImportExportTestSuite.java
    ├── config/
    │   ├── ImportConfig.java
    │   ├── ExportConfig.java
    │   └── ExportFilter.java
    └── model/
        ├── ImportResult.java
        ├── ExportResult.java
        └── ExportColumn.java
```

## 注意事项

1. 性能测试需要足够的内存和磁盘空间
2. 测试使用临时目录，不会污染实际数据
3. 部分测试使用Mock数据，不依赖真实数据库
