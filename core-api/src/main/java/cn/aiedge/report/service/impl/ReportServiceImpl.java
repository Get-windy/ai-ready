package cn.aiedge.report.service.impl;

import cn.aiedge.report.model.ReportData;
import cn.aiedge.report.model.ReportDefinition;
import cn.aiedge.report.model.ReportDefinition.*;
import cn.aiedge.report.service.ReportService;
import cn.aiedge.cache.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 报表服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final CacheService cacheService;

    // 缓存Key前缀
    private static final String REPORT_DEF_KEY = "report:definition:";
    private static final String REPORT_LIST_KEY = "report:list:";

    // 内置报表定义
    private static final Map<String, ReportDefinition> BUILTIN_REPORTS = new LinkedHashMap<>();

    static {
        // 销售汇总报表
        ReportDefinition salesSummary = createSalesSummaryReport();
        BUILTIN_REPORTS.put(salesSummary.getReportId(), salesSummary);

        // 客户分析报表
        ReportDefinition customerAnalysis = createCustomerAnalysisReport();
        BUILTIN_REPORTS.put(customerAnalysis.getReportId(), customerAnalysis);

        // 产品销售报表
        ReportDefinition productSales = createProductSalesReport();
        BUILTIN_REPORTS.put(productSales.getReportId(), productSales);

        // 订单明细报表
        ReportDefinition orderDetail = createOrderDetailReport();
        BUILTIN_REPORTS.put(orderDetail.getReportId(), orderDetail);
    }

    @Override
    public ReportDefinition getReportDefinition(String reportId) {
        // 先查内置报表
        ReportDefinition definition = BUILTIN_REPORTS.get(reportId);
        if (definition != null) {
            return definition;
        }
        
        // 从缓存获取
        return cacheService.get(REPORT_DEF_KEY + reportId, ReportDefinition.class);
    }

    @Override
    public List<ReportDefinition> getReportList(String category, Long tenantId) {
        List<ReportDefinition> result = new ArrayList<>();
        
        // 添加内置报表
        for (ReportDefinition def : BUILTIN_REPORTS.values()) {
            if (category == null || category.equals(def.getCategory())) {
                result.add(def);
            }
        }
        
        // TODO: 从数据库加载自定义报表
        
        return result;
    }

    @Override
    public ReportData generateReport(String reportId, Map<String, Object> parameters, Long tenantId) {
        long startTime = System.currentTimeMillis();
        
        ReportDefinition definition = getReportDefinition(reportId);
        if (definition == null) {
            throw new RuntimeException("报表不存在: " + reportId);
        }

        log.info("生成报表: reportId={}, reportName={}", reportId, definition.getReportName());

        ReportData reportData = new ReportData();
        reportData.setReportId(reportId);
        reportData.setReportName(definition.getReportName());
        reportData.setGeneratedAt(LocalDateTime.now());
        reportData.setParameters(parameters);
        reportData.setColumns(definition.getColumns());

        // 根据数据源类型获取数据
        List<Map<String, Object>> rows = fetchReportData(definition, parameters, tenantId);
        reportData.setRows(rows);
        reportData.setTotalRows(rows.size());

        // 计算汇总
        if ("summary".equals(definition.getReportType())) {
            reportData.setSummary(calculateSummary(rows, definition.getColumns()));
        }

        // 生成图表数据
        if ("chart".equals(definition.getReportType()) && definition.getChartConfig() != null) {
            reportData.setChartData(generateChartData(rows, definition.getChartConfig()));
        }

        reportData.setQueryTime(System.currentTimeMillis() - startTime);
        
        log.info("报表生成完成: rows={}, time={}ms", rows.size(), reportData.getQueryTime());
        return reportData;
    }

    @Override
    public void exportToExcel(String reportId, Map<String, Object> parameters, 
                              OutputStream outputStream, Long tenantId) {
        log.info("导出Excel: reportId={}", reportId);
        
        ReportData reportData = generateReport(reportId, parameters, tenantId);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(reportData.getReportName());
            
            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            List<ReportColumn> columns = reportData.getColumns();
            for (int i = 0; i < columns.size(); i++) {
                ReportColumn column = columns.get(i);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(column.getTitle());
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, column.getWidth() * 256);
            }
            
            // 填充数据
            int rowNum = 1;
            for (Map<String, Object> rowData : reportData.getRows()) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < columns.size(); i++) {
                    ReportColumn column = columns.get(i);
                    Cell cell = row.createCell(i);
                    Object value = rowData.get(column.getField());
                    setCellValue(cell, value, column.getDataType());
                }
            }
            
            // 添加汇总行
            if (reportData.getSummary() != null) {
                Row summaryRow = sheet.createRow(rowNum);
                CellStyle summaryStyle = workbook.createCellStyle();
                summaryStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                summaryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                summaryStyle.setFont(headerFont);
                
                for (int i = 0; i < columns.size(); i++) {
                    ReportColumn column = columns.get(i);
                    Cell cell = summaryRow.createCell(i);
                    if (i == 0) {
                        cell.setCellValue("合计");
                    } else if (column.isAggregate()) {
                        Object summaryValue = reportData.getSummary().get(column.getField());
                        if (summaryValue != null) {
                            cell.setCellValue(summaryValue.toString());
                        }
                    }
                    cell.setCellStyle(summaryStyle);
                }
            }
            
            workbook.write(outputStream);
        } catch (Exception e) {
            log.error("导出Excel失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public void exportToPdf(String reportId, Map<String, Object> parameters, 
                            OutputStream outputStream, Long tenantId) {
        log.info("导出PDF: reportId={}", reportId);
        // PDF导出需要iText或其他库，这里提供简化实现
        throw new UnsupportedOperationException("PDF导出功能待实现");
    }

    @Override
    public void exportToCsv(String reportId, Map<String, Object> parameters, 
                            OutputStream outputStream, Long tenantId) {
        log.info("导出CSV: reportId={}", reportId);
        
        ReportData reportData = generateReport(reportId, parameters, tenantId);
        
        try {
            StringBuilder csv = new StringBuilder();
            
            // 表头
            List<ReportColumn> columns = reportData.getColumns();
            List<String> headers = new ArrayList<>();
            for (ReportColumn column : columns) {
                headers.add(escapeCsvField(column.getTitle()));
            }
            csv.append(String.join(",", headers)).append("\n");
            
            // 数据行
            for (Map<String, Object> rowData : reportData.getRows()) {
                List<String> values = new ArrayList<>();
                for (ReportColumn column : columns) {
                    Object value = rowData.get(column.getField());
                    values.add(escapeCsvField(value != null ? value.toString() : ""));
                }
                csv.append(String.join(",", values)).append("\n");
            }
            
            outputStream.write(csv.toString().getBytes("UTF-8"));
        } catch (Exception e) {
            log.error("导出CSV失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public ReportDefinition saveReportDefinition(ReportDefinition definition, Long tenantId) {
        if (definition.getReportId() == null) {
            definition.setReportId(UUID.randomUUID().toString());
        }
        definition.setCreateTime(LocalDateTime.now());
        definition.setUpdateTime(LocalDateTime.now());
        
        // 保存到缓存
        cacheService.set(REPORT_DEF_KEY + definition.getReportId(), definition);
        
        log.info("保存报表定义: reportId={}", definition.getReportId());
        return definition;
    }

    @Override
    public boolean deleteReportDefinition(String reportId, Long tenantId) {
        // 内置报表不能删除
        if (BUILTIN_REPORTS.containsKey(reportId)) {
            log.warn("不能删除内置报表: {}", reportId);
            return false;
        }
        
        cacheService.delete(REPORT_DEF_KEY + reportId);
        log.info("删除报表定义: reportId={}", reportId);
        return true;
    }

    @Override
    public ReportDefinition copyReportDefinition(String reportId, String newName, Long tenantId) {
        ReportDefinition source = getReportDefinition(reportId);
        if (source == null) {
            throw new RuntimeException("源报表不存在: " + reportId);
        }
        
        ReportDefinition copy = new ReportDefinition();
        copy.setReportId(UUID.randomUUID().toString());
        copy.setReportName(newName);
        copy.setReportCode(source.getReportCode() + "_copy");
        copy.setReportType(source.getReportType());
        copy.setCategory(source.getCategory());
        copy.setDataSourceType(source.getDataSourceType());
        copy.setDataSourceConfig(new HashMap<>(source.getDataSourceConfig()));
        copy.setColumns(new ArrayList<>(source.getColumns()));
        copy.setParameters(new ArrayList<>(source.getParameters()));
        copy.setChartConfig(source.getChartConfig());
        copy.setEnabled(true);
        copy.setCreateTime(LocalDateTime.now());
        copy.setUpdateTime(LocalDateTime.now());
        
        return saveReportDefinition(copy, tenantId);
    }

    @Override
    public ReportData previewReport(String reportId, Map<String, Object> parameters, 
                                    int maxRows, Long tenantId) {
        ReportDefinition definition = getReportDefinition(reportId);
        if (definition == null) {
            throw new RuntimeException("报表不存在: " + reportId);
        }

        // 获取数据并限制行数
        List<Map<String, Object>> rows = fetchReportData(definition, parameters, tenantId);
        if (rows.size() > maxRows) {
            rows = rows.subList(0, maxRows);
        }

        ReportData reportData = new ReportData();
        reportData.setReportId(reportId);
        reportData.setReportName(definition.getReportName());
        reportData.setGeneratedAt(LocalDateTime.now());
        reportData.setColumns(definition.getColumns());
        reportData.setRows(rows);
        reportData.setTotalRows(rows.size());

        return reportData;
    }

    // ==================== 内置报表定义 ====================

    private static ReportDefinition createSalesSummaryReport() {
        ReportDefinition report = new ReportDefinition();
        report.setReportId("sales_summary");
        report.setReportName("销售汇总报表");
        report.setReportCode("SALES_SUMMARY");
        report.setReportType("summary");
        report.setCategory("sales");
        report.setDataSourceType("sql");
        report.setEnabled(true);
        report.setSortOrder(1);
        
        List<ReportColumn> columns = new ArrayList<>();
        columns.add(createColumn("date", "日期", "date", 15, true));
        columns.add(createColumn("totalOrders", "订单数", "number", 10, true, "sum"));
        columns.add(createColumn("totalAmount", "销售额", "number", 15, true, "sum", "#,##0.00"));
        columns.add(createColumn("totalQuantity", "销售数量", "number", 12, true, "sum"));
        columns.add(createColumn("avgOrderAmount", "客单价", "number", 12, true, "avg", "#,##0.00"));
        report.setColumns(columns);
        
        List<ReportParameter> parameters = new ArrayList<>();
        parameters.add(createDateParameter("startDate", "开始日期", true));
        parameters.add(createDateParameter("endDate", "结束日期", true));
        report.setParameters(parameters);
        
        return report;
    }

    private static ReportDefinition createCustomerAnalysisReport() {
        ReportDefinition report = new ReportDefinition();
        report.setReportId("customer_analysis");
        report.setReportName("客户分析报表");
        report.setReportCode("CUSTOMER_ANALYSIS");
        report.setReportType("chart");
        report.setCategory("customer");
        report.setDataSourceType("sql");
        report.setEnabled(true);
        report.setSortOrder(2);
        
        List<ReportColumn> columns = new ArrayList<>();
        columns.add(createColumn("customerName", "客户名称", "string", 25, true));
        columns.add(createColumn("orderCount", "订单数", "number", 10, true));
        columns.add(createColumn("totalAmount", "消费金额", "number", 15, true, "sum", "#,##0.00"));
        columns.add(createColumn("avgAmount", "平均订单金额", "number", 15, true, "avg", "#,##0.00"));
        columns.add(createColumn("lastOrderDate", "最近下单日期", "date", 15, true));
        report.setColumns(columns);
        
        ChartConfig chartConfig = new ChartConfig();
        chartConfig.setChartType("bar");
        chartConfig.setxField("customerName");
        chartConfig.setyFields(List.of("totalAmount"));
        chartConfig.setShowLegend(true);
        chartConfig.setChartTitle("客户消费金额排名");
        report.setChartConfig(chartConfig);
        
        return report;
    }

    private static ReportDefinition createProductSalesReport() {
        ReportDefinition report = new ReportDefinition();
        report.setReportId("product_sales");
        report.setReportName("产品销售报表");
        report.setReportCode("PRODUCT_SALES");
        report.setReportType("summary");
        report.setCategory("product");
        report.setDataSourceType("sql");
        report.setEnabled(true);
        report.setSortOrder(3);
        
        List<ReportColumn> columns = new ArrayList<>();
        columns.add(createColumn("productName", "产品名称", "string", 25, true));
        columns.add(createColumn("productCode", "产品编码", "string", 15, true));
        columns.add(createColumn("category", "分类", "string", 12, true));
        columns.add(createColumn("quantity", "销售数量", "number", 12, true, "sum"));
        columns.add(createColumn("amount", "销售金额", "number", 15, true, "sum", "#,##0.00"));
        columns.add(createColumn("avgPrice", "平均单价", "number", 12, true, "avg", "#,##0.00"));
        report.setColumns(columns);
        
        return report;
    }

    private static ReportDefinition createOrderDetailReport() {
        ReportDefinition report = new ReportDefinition();
        report.setReportId("order_detail");
        report.setReportName("订单明细报表");
        report.setReportCode("ORDER_DETAIL");
        report.setReportType("detail");
        report.setCategory("order");
        report.setDataSourceType("sql");
        report.setEnabled(true);
        report.setSortOrder(4);
        
        List<ReportColumn> columns = new ArrayList<>();
        columns.add(createColumn("orderNo", "订单号", "string", 20, true));
        columns.add(createColumn("customerName", "客户名称", "string", 20, true));
        columns.add(createColumn("productName", "产品名称", "string", 20, true));
        columns.add(createColumn("quantity", "数量", "number", 10, true));
        columns.add(createColumn("price", "单价", "number", 10, true, "#,##0.00"));
        columns.add(createColumn("amount", "金额", "number", 12, true, "#,##0.00"));
        columns.add(createColumn("orderDate", "下单日期", "date", 12, true));
        columns.add(createColumn("status", "状态", "string", 10, true));
        report.setColumns(columns);
        
        return report;
    }

    // ==================== 辅助方法 ====================

    private static ReportColumn createColumn(String field, String title, String dataType, 
                                             int width, boolean visible) {
        return createColumn(field, title, dataType, width, visible, null, null);
    }

    private static ReportColumn createColumn(String field, String title, String dataType, 
                                             int width, boolean visible, String aggregateType) {
        return createColumn(field, title, dataType, width, visible, aggregateType, null);
    }

    private static ReportColumn createColumn(String field, String title, String dataType, 
                                             int width, boolean visible, String aggregateType, String format) {
        ReportColumn column = new ReportColumn();
        column.setField(field);
        column.setTitle(title);
        column.setDataType(dataType);
        column.setWidth(width);
        column.setVisible(visible);
        column.setAggregate(aggregateType != null);
        column.setAggregateType(aggregateType);
        column.setFormat(format);
        column.setSortable(true);
        return column;
    }

    private static ReportParameter createDateParameter(String name, String title, boolean required) {
        ReportParameter param = new ReportParameter();
        param.setName(name);
        param.setTitle(title);
        param.setDataType("date");
        param.setRequired(required);
        return param;
    }

    /**
     * 获取报表数据
     */
    private List<Map<String, Object>> fetchReportData(ReportDefinition definition, 
                                                       Map<String, Object> parameters, Long tenantId) {
        // 模拟数据，实际应从数据库查询
        List<Map<String, Object>> rows = new ArrayList<>();
        
        String reportId = definition.getReportId();
        
        if ("sales_summary".equals(reportId)) {
            // 销售汇总模拟数据
            for (int i = 1; i <= 7; i++) {
                Map<String, Object> row = new HashMap<>();
                row.put("date", LocalDateTime.now().minusDays(i).format(DateTimeFormatter.ISO_DATE));
                row.put("totalOrders", 10 + (int)(Math.random() * 50));
                row.put("totalAmount", 5000 + (int)(Math.random() * 50000));
                row.put("totalQuantity", 20 + (int)(Math.random() * 100));
                row.put("avgOrderAmount", 200 + (int)(Math.random() * 800));
                rows.add(row);
            }
        } else if ("customer_analysis".equals(reportId)) {
            // 客户分析模拟数据
            String[] customers = {"张三公司", "李四科技", "王五集团", "赵六贸易", "钱七实业"};
            for (String customer : customers) {
                Map<String, Object> row = new HashMap<>();
                row.put("customerName", customer);
                row.put("orderCount", 10 + (int)(Math.random() * 100));
                row.put("totalAmount", 10000 + (int)(Math.random() * 100000));
                row.put("avgAmount", 500 + (int)(Math.random() * 2000));
                row.put("lastOrderDate", LocalDateTime.now().minusDays((int)(Math.random() * 30))
                        .format(DateTimeFormatter.ISO_DATE));
                rows.add(row);
            }
        } else if ("product_sales".equals(reportId)) {
            // 产品销售模拟数据
            String[] products = {"智能手机", "笔记本电脑", "平板电脑", "智能手表", "无线耳机"};
            for (String product : products) {
                Map<String, Object> row = new HashMap<>();
                row.put("productName", product);
                row.put("productCode", "P" + (1000 + rows.size()));
                row.put("category", "电子产品");
                row.put("quantity", 50 + (int)(Math.random() * 500));
                row.put("amount", 50000 + (int)(Math.random() * 500000));
                row.put("avgPrice", 1000 + (int)(Math.random() * 5000));
                rows.add(row);
            }
        } else if ("order_detail".equals(reportId)) {
            // 订单明细模拟数据
            String[] statuses = {"已完成", "处理中", "待付款", "已取消"};
            for (int i = 1; i <= 20; i++) {
                Map<String, Object> row = new HashMap<>();
                row.put("orderNo", "ORD" + System.currentTimeMillis() + i);
                row.put("customerName", "客户" + i);
                row.put("productName", "产品" + i);
                row.put("quantity", 1 + (int)(Math.random() * 10));
                row.put("price", 100 + (int)(Math.random() * 1000));
                row.put("amount", (int)row.get("quantity") * (int)row.get("price"));
                row.put("orderDate", LocalDateTime.now().minusDays((int)(Math.random() * 30))
                        .format(DateTimeFormatter.ISO_DATE));
                row.put("status", statuses[(int)(Math.random() * statuses.length)]);
                rows.add(row);
            }
        }
        
        return rows;
    }

    /**
     * 计算汇总数据
     */
    private Map<String, Object> calculateSummary(List<Map<String, Object>> rows, 
                                                  List<ReportColumn> columns) {
        Map<String, Object> summary = new HashMap<>();
        
        for (ReportColumn column : columns) {
            if (!column.isAggregate()) continue;
            
            String field = column.getField();
            String aggregateType = column.getAggregateType();
            
            if ("sum".equals(aggregateType)) {
                double sum = rows.stream()
                    .mapToDouble(r -> getDoubleValue(r.get(field)))
                    .sum();
                summary.put(field, sum);
            } else if ("avg".equals(aggregateType)) {
                double avg = rows.stream()
                    .mapToDouble(r -> getDoubleValue(r.get(field)))
                    .average()
                    .orElse(0);
                summary.put(field, avg);
            } else if ("count".equals(aggregateType)) {
                summary.put(field, rows.size());
            } else if ("max".equals(aggregateType)) {
                double max = rows.stream()
                    .mapToDouble(r -> getDoubleValue(r.get(field)))
                    .max()
                    .orElse(0);
                summary.put(field, max);
            } else if ("min".equals(aggregateType)) {
                double min = rows.stream()
                    .mapToDouble(r -> getDoubleValue(r.get(field)))
                    .min()
                    .orElse(0);
                summary.put(field, min);
            }
        }
        
        return summary;
    }

    /**
     * 生成图表数据
     */
    private ReportData.ChartData generateChartData(List<Map<String, Object>> rows, 
                                                    ChartConfig config) {
        ReportData.ChartData chartData = new ReportData.ChartData();
        
        // 提取标签
        List<String> labels = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object label = row.get(config.getxField());
            labels.add(label != null ? label.toString() : "");
        }
        chartData.setLabels(labels);
        
        // 提取数据集
        List<ReportData.ChartDataset> datasets = new ArrayList<>();
        for (String yField : config.getyFields()) {
            ReportData.ChartDataset dataset = new ReportData.ChartDataset();
            dataset.setLabel(yField);
            
            List<Double> data = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                data.add(getDoubleValue(row.get(yField)));
            }
            dataset.setData(data);
            
            // 设置颜色
            dataset.setBorderColor("#4BC0C0");
            dataset.setBackgroundColor(List.of("#4BC0C0", "#FF6384", "#36A2EB", "#FFCE56", "#9966FF"));
            dataset.setFill(false);
            
            datasets.add(dataset);
        }
        chartData.setDatasets(datasets);
        
        return chartData;
    }

    private double getDoubleValue(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void setCellValue(Cell cell, Object value, String dataType) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }
        
        switch (dataType) {
            case "number":
                cell.setCellValue(getDoubleValue(value));
                break;
            case "date":
            case "datetime":
                cell.setCellValue(value.toString());
                break;
            case "boolean":
                cell.setCellValue(Boolean.parseBoolean(value.toString()));
                break;
            default:
                cell.setCellValue(value.toString());
        }
    }

    private String escapeCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
