package cn.aiedge.export.service.impl;

import cn.aiedge.export.service.DataExportService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 数据导出服务实现
 * 支持 Excel、CSV 格式的大数据量导出
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class DataExportServiceImpl implements DataExportService {

    // ==================== Excel 导出 ====================

    @Override
    public <T> void exportExcel(List<T> data, Map<String, String> headers, OutputStream out) {
        if (data == null || headers == null) {
            throw new IllegalArgumentException("数据和表头不能为空");
        }

        try (ExcelWriter writer = ExcelUtil.getWriter(true)) {
            // 设置表头
            List<String> fieldNames = new ArrayList<>(headers.keySet());
            for (int i = 0; i < fieldNames.size(); i++) {
                String field = fieldNames.get(i);
                writer.addHeaderAlias(field, headers.get(field));
            }

            // 写入数据
            writer.write(data, true);
            writer.flush(out);

            log.info("Excel导出完成，共{}条数据", data.size());

        } catch (Exception e) {
            log.error("Excel导出失败", e);
            throw new RuntimeException("Excel导出失败: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> void exportExcelBatch(DataProvider<T> dataProvider, Map<String, String> headers,
                                      OutputStream out, int batchSize) {
        if (dataProvider == null || headers == null) {
            throw new IllegalArgumentException("数据提供者和表头不能为空");
        }

        batchSize = batchSize <= 0 ? 1000 : batchSize;
        AtomicInteger totalCount = new AtomicInteger(0);
        int page = 1;

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("数据导出");

            // 创建表头样式
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 写入表头
            Row headerRow = sheet.createRow(0);
            List<String> fieldNames = new ArrayList<>(headers.keySet());
            for (int i = 0; i < fieldNames.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(fieldNames.get(i)));
                cell.setCellStyle(headerStyle);
            }

            // 数据样式
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.CENTER);

            // 分批写入数据
            int rowNum = 1;
            List<T> batch;
            while (!(batch = dataProvider.fetchData(page, batchSize)).isEmpty()) {
                for (T item : batch) {
                    Row dataRow = sheet.createRow(rowNum++);
                    Map<String, Object> map = BeanUtil.beanToMap(item);

                    for (int i = 0; i < fieldNames.size(); i++) {
                        Cell cell = dataRow.createCell(i);
                        Object value = map.get(fieldNames.get(i));
                        setCellValue(cell, value);
                        cell.setCellStyle(dataStyle);
                    }
                }
                totalCount.addAndGet(batch.size());
                page++;
                log.debug("已导出{}条数据", totalCount.get());
            }

            workbook.write(out);
            log.info("批量Excel导出完成，共{}条数据", totalCount.get());

        } catch (Exception e) {
            log.error("批量Excel导出失败", e);
            throw new RuntimeException("批量Excel导出失败: " + e.getMessage(), e);
        }
    }

    // ==================== CSV 导出 ====================

    @Override
    public <T> void exportCsv(List<T> data, Map<String, String> headers, OutputStream out) {
        if (data == null || headers == null) {
            throw new IllegalArgumentException("数据和表头不能为空");
        }

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // 写入BOM（解决Excel打开中文乱码）
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            // 写入表头
            List<String> fieldNames = new ArrayList<>(headers.keySet());
            String[] headerArray = fieldNames.stream()
                    .map(headers::get)
                    .toArray(String[]::new);
            writer.writeNext(headerArray);

            // 写入数据
            for (T item : data) {
                Map<String, Object> map = BeanUtil.beanToMap(item);
                String[] row = fieldNames.stream()
                        .map(field -> {
                            Object value = map.get(field);
                            return value != null ? String.valueOf(value) : "";
                        })
                        .toArray(String[]::new);
                writer.writeNext(row);
            }

            log.info("CSV导出完成，共{}条数据", data.size());

        } catch (Exception e) {
            log.error("CSV导出失败", e);
            throw new RuntimeException("CSV导出失败: " + e.getMessage(), e);
        }
    }

    // ==================== Excel 导入 ====================

    @Override
    public <T> List<T> importExcel(InputStream in, Map<String, String> headers, Class<T> rowClass) {
        if (in == null || headers == null || rowClass == null) {
            throw new IllegalArgumentException("输入流、表头和行类型不能为空");
        }

        try (ExcelReader reader = ExcelUtil.getReader(in)) {
            // 设置表头别名
            headers.forEach((field, displayName) -> reader.addHeaderAlias(displayName, field));

            // 读取所有数据
            List<T> data = reader.readAll(rowClass);

            log.info("Excel导入完成，共{}条数据", data.size());
            return data;

        } catch (Exception e) {
            log.error("Excel导入失败", e);
            throw new RuntimeException("Excel导入失败: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> List<T> importCsv(InputStream in, Map<String, String> headers, Class<T> rowClass) {
        if (in == null || headers == null || rowClass == null) {
            throw new IllegalArgumentException("输入流、表头和行类型不能为空");
        }

        try (CSVReader reader = new CSVReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<T> result = new ArrayList<>();
            String[] headerLine = reader.readNext();

            if (headerLine == null) {
                return result;
            }

            // 构建字段索引映射
            List<String> fieldNames = new ArrayList<>(headers.keySet());
            Map<String, Integer> headerIndexMap = new HashMap<>();
            for (int i = 0; i < headerLine.length; i++) {
                String header = headerLine[i].trim();
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    if (entry.getValue().equals(header)) {
                        headerIndexMap.put(entry.getKey(), i);
                        break;
                    }
                }
            }

            // 读取数据行
            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, Object> rowData = new HashMap<>();
                for (String field : fieldNames) {
                    Integer index = headerIndexMap.get(field);
                    if (index != null && index < line.length) {
                        rowData.put(field, line[index]);
                    }
                }
                T obj = BeanUtil.toBean(rowData, rowClass);
                result.add(obj);
            }

            log.info("CSV导入完成，共{}条数据", result.size());
            return result;

        } catch (Exception e) {
            log.error("CSV导入失败", e);
            throw new RuntimeException("CSV导入失败: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> ImportResult importExcelBatch(InputStream in, Map<String, String> headers,
                                              Class<T> rowClass, int batchSize,
                                              Consumer<List<T>> processor) {
        if (in == null || processor == null) {
            throw new IllegalArgumentException("输入流和处理器不能为空");
        }

        batchSize = batchSize <= 0 ? 1000 : batchSize;
        int totalCount = 0;
        int successCount = 0;
        int failureCount = 0;
        List<ImportError> errors = new ArrayList<>();

        try (ExcelReader reader = ExcelUtil.getReader(in)) {
            headers.forEach((field, displayName) -> reader.addHeaderAlias(displayName, field));

            List<T> allData = reader.readAll(rowClass);
            totalCount = allData.size();

            // 分批处理
            for (int i = 0; i < allData.size(); i += batchSize) {
                int end = Math.min(i + batchSize, allData.size());
                List<T> batch = allData.subList(i, end);

                try {
                    processor.accept(batch);
                    successCount += batch.size();
                } catch (Exception e) {
                    failureCount += batch.size();
                    for (int j = i; j < end; j++) {
                        errors.add(new ImportError(j + 2, "batch", e.getMessage()));
                    }
                    log.warn("批次处理失败: {}-{}", i, end, e);
                }
            }

            log.info("批量导入完成: 总数={}, 成功={}, 失败={}", totalCount, successCount, failureCount);
            return new ImportResult(totalCount, successCount, failureCount, errors);

        } catch (Exception e) {
            log.error("批量导入失败", e);
            throw new RuntimeException("批量导入失败: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> ImportResult importExcelWithValidation(InputStream in, Map<String, String> headers,
                                                       Class<T> rowClass, DataValidator<T> validator) {
        if (in == null || validator == null) {
            throw new IllegalArgumentException("输入流和校验器不能为空");
        }

        int totalCount = 0;
        int successCount = 0;
        int failureCount = 0;
        List<ImportError> errors = new ArrayList<>();
        List<T> validData = new ArrayList<>();

        try (ExcelReader reader = ExcelUtil.getReader(in)) {
            headers.forEach((field, displayName) -> reader.addHeaderAlias(displayName, field));

            List<T> allData = reader.readAll(rowClass);
            totalCount = allData.size();

            for (int i = 0; i < allData.size(); i++) {
                T item = allData.get(i);
                ValidationResult result = validator.validate(item, i + 2);

                if (result.valid()) {
                    validData.add(item);
                    successCount++;
                } else {
                    failureCount++;
                    errors.add(new ImportError(i + 2, "validation", result.message()));
                }
            }

            log.info("带校验导入完成: 总数={}, 成功={}, 失败={}", totalCount, successCount, failureCount);
            return new ImportResult(totalCount, successCount, failureCount, errors);

        } catch (Exception e) {
            log.error("带校验导入失败", e);
            throw new RuntimeException("带校验导入失败: " + e.getMessage(), e);
        }
    }

    // ==================== 辅助方法 ====================

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        return style;
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
