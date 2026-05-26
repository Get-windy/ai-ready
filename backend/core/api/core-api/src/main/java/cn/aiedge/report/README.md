# AI-Ready 报表模块文档

## 概述

本模块提供报表生成、导出和管理功能，支持多种报表类型（汇总表、明细表、图表）和多格式导出（Excel、CSV、PDF）。

## 模块结构

```
cn.aiedge.report
├── controller/
│   └── ReportController.java    # 报表接口
├── model/
│   ├── ReportDefinition.java    # 报表定义
│   └── ReportData.java          # 报表数据
└── service/
    ├── ReportService.java       # 报表服务接口
    └── impl/
        └── ReportServiceImpl.java # 报表服务实现
```

## 内置报表

| 报表ID | 名称 | 类型 | 分类 |
|--------|------|------|------|
| sales_summary | 销售汇总报表 | summary | sales |
| customer_analysis | 客户分析报表 | chart | customer |
| product_sales | 产品销售报表 | summary | product |
| order_detail | 订单明细报表 | detail | order |

## 报表类型

| 类型 | 说明 | 特点 |
|------|------|------|
| summary | 汇总报表 | 支持聚合计算、汇总行 |
| detail | 明细报表 | 原始数据展示 |
| chart | 图表报表 | 支持图表可视化 |
| pivot | 透视报表 | 支持数据透视（规划中） |

## API接口

### 1. 获取报表列表

```bash
GET /api/report/list?category=sales
```

响应示例：
```json
{
  "reports": [
    {
      "reportId": "sales_summary",
      "reportName": "销售汇总报表",
      "reportType": "summary",
      "category": "sales"
    }
  ],
  "total": 1
}
```

### 2. 获取报表定义

```bash
GET /api/report/{reportId}
```

### 3. 生成报表

```bash
POST /api/report/{reportId}/generate
Content-Type: application/json

{
  "startDate": "2026-01-01",
  "endDate": "2026-03-31"
}
```

响应示例：
```json
{
  "reportId": "sales_summary",
  "reportName": "销售汇总报表",
  "generatedAt": "2026-04-04T05:00:00",
  "columns": [...],
  "rows": [...],
  "totalRows": 30,
  "summary": {
    "totalAmount": 150000.00,
    "totalOrders": 350
  },
  "queryTime": 125
}
```

### 4. 预览报表

```bash
POST /api/report/{reportId}/preview?maxRows=10
```

### 5. 导出Excel

```bash
POST /api/report/{reportId}/export/excel
Content-Type: application/json

{
  "startDate": "2026-01-01",
  "endDate": "2026-03-31"
}
```

### 6. 导出CSV

```bash
POST /api/report/{reportId}/export/csv
```

### 7. 导出PDF

```bash
POST /api/report/{reportId}/export/pdf
```

### 8. 创建自定义报表

```bash
POST /api/report
Content-Type: application/json

{
  "reportName": "自定义销售报表",
  "reportCode": "CUSTOM_SALES_001",
  "reportType": "summary",
  "category": "sales",
  "dataSourceType": "sql",
  "columns": [
    { "field": "date", "title": "日期", "dataType": "date" },
    { "field": "amount", "title": "金额", "dataType": "number", "aggregate": true, "aggregateType": "sum" }
  ],
  "parameters": [
    { "name": "startDate", "title": "开始日期", "dataType": "date", "required": true }
  ]
}
```

## 报表定义结构

### 列定义 (ReportColumn)

| 属性 | 类型 | 说明 |
|------|------|------|
| field | String | 字段名 |
| title | String | 列标题 |
| dataType | String | 数据类型: string/number/date/datetime/boolean |
| width | int | 列宽（字符数） |
| format | String | 格式化模式（如 #,##0.00） |
| aggregate | boolean | 是否聚合 |
| aggregateType | String | 聚合方式: sum/avg/count/max/min |
| sortable | boolean | 是否可排序 |
| visible | boolean | 是否可见 |

### 参数定义 (ReportParameter)

| 属性 | 类型 | 说明 |
|------|------|------|
| name | String | 参数名 |
| title | String | 参数标题 |
| dataType | String | 数据类型: string/number/date/select/multiSelect |
| required | boolean | 是否必填 |
| defaultValue | Object | 默认值 |
| options | List | 可选项（select/multiSelect类型） |

### 图表配置 (ChartConfig)

| 属性 | 类型 | 说明 |
|------|------|------|
| chartType | String | 图表类型: line/bar/pie/doughnut/radar/polarArea |
| xField | String | X轴字段 |
| yFields | List | Y轴字段列表 |
| groupField | String | 分组字段 |
| stacked | boolean | 是否堆叠 |
| showLegend | boolean | 是否显示图例 |
| chartTitle | String | 图表标题 |

## 使用示例

### Java代码调用

```java
@Service
public class SalesReportService {
    
    @Autowired
    private ReportService reportService;
    
    public ReportData getMonthlySalesReport(int year, int month) {
        Map<String, Object> params = new HashMap<>();
        params.put("startDate", year + "-" + month + "-01");
        params.put("endDate", year + "-" + month + "-31");
        
        return reportService.generateReport("sales_summary", params, tenantId);
    }
    
    public void exportSalesReport(OutputStream out, int year, int month) {
        Map<String, Object> params = new HashMap<>();
        params.put("startDate", year + "-" + month + "-01");
        params.put("endDate", year + "-" + month + "-31");
        
        reportService.exportToExcel("sales_summary", params, out, tenantId);
    }
}
```

### 前端集成

```javascript
// 生成报表
const generateReport = async (reportId, params) => {
  const response = await fetch(`/api/report/${reportId}/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params)
  });
  return await response.json();
};

// 导出Excel
const exportReport = async (reportId, params) => {
  const response = await fetch(`/api/report/${reportId}/export/excel`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params)
  });
  
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'report.xlsx';
  a.click();
};
```

## 扩展说明

### 添加新报表

1. 在 `ReportServiceImpl` 中添加报表定义
2. 实现 `fetchReportData` 方法中的数据获取逻辑
3. 配置列定义和参数定义

### 自定义数据源

支持三种数据源类型：

| 类型 | 说明 |
|------|------|
| sql | SQL查询（需配置SQL语句） |
| api | API调用（需配置接口地址） |
| custom | 自定义实现（需编写Java代码） |

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2026-04-04 | 初始版本，支持基础报表功能 |

---

*AI-Ready Team © 2026*
