package com.qizhilian.datadriven;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 数据驱动测试数据提供者
 * 支持Excel、JSON、CSV格式的测试数据
 */
@Slf4j
public class DataProvider {
    
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    
    private static final String DATA_DIR = "src/test/resources/data/";
    
    /**
     * 从Excel文件读取测试数据
     */
    public static List<Map<String, Object>> readExcelData(String fileName, String sheetName) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        String filePath = DATA_DIR + fileName;
        
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = sheetName != null ? 
                    workbook.getSheet(sheetName) : workbook.getSheetAt(0);
            
            if (sheet == null) {
                log.error("Sheet not found: {}", sheetName);
                return dataList;
            }
            
            // 读取表头
            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValue(cell).toString());
            }
            
            // 读取数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Map<String, Object> rowData = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    rowData.put(headers.get(j), getCellValue(cell));
                }
                dataList.add(rowData);
            }
            
            log.info("从Excel加载数据: {} 条记录", dataList.size());
            
        } catch (IOException e) {
            log.error("读取Excel文件失败: {}", e.getMessage());
        }
        
        return dataList;
    }
    
    /**
     * 从JSON文件读取测试数据
     */
    public static <T> List<T> readJsonData(String fileName, Class<T> clazz) {
        String filePath = DATA_DIR + fileName;
        
        try {
            return objectMapper.readValue(new File(filePath), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            log.error("读取JSON文件失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 从JSON文件读取为Map列表
     */
    public static List<Map<String, Object>> readJsonDataAsMap(String fileName) {
        String filePath = DATA_DIR + fileName;
        
        try {
            return objectMapper.readValue(new File(filePath), 
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (IOException e) {
            log.error("读取JSON文件失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 从CSV文件读取测试数据
     */
    public static List<Map<String, Object>> readCsvData(String fileName) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        String filePath = DATA_DIR + fileName;
        
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            
            String headerLine = br.readLine();
            if (headerLine == null) return dataList;
            
            String[] headers = headerLine.split(",");
            
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                Map<String, Object> rowData = new LinkedHashMap<>();
                
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    rowData.put(headers[i].trim(), parseValue(values[i].trim()));
                }
                dataList.add(rowData);
            }
            
            log.info("从CSV加载数据: {} 条记录", dataList.size());
            
        } catch (IOException e) {
            log.error("读取CSV文件失败: {}", e.getMessage());
        }
        
        return dataList;
    }
    
    /**
     * 获取单元格值
     */
    private static Object getCellValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    /**
     * 解析CSV值
     */
    private static Object parseValue(String value) {
        // 尝试解析为数字
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            // 尝试解析为布尔值
            if (value.equalsIgnoreCase("true")) return true;
            if (value.equalsIgnoreCase("false")) return false;
            // 返回字符串
            return value;
        }
    }
    
    /**
     * 生成测试数据文件模板
     */
    public static void generateExcelTemplate(String fileName, String sheetName, 
                                              List<String> headers) {
        String filePath = DATA_DIR + fileName;
        
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(filePath)) {
            
            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
            }
            
            workbook.write(fos);
            log.info("生成Excel模板: {}", filePath);
            
        } catch (IOException e) {
            log.error("生成Excel模板失败: {}", e.getMessage());
        }
    }
    
    /**
     * 生成JSON数据模板
     */
    public static void generateJsonTemplate(String fileName, List<Map<String, Object>> sampleData) {
        String filePath = DATA_DIR + fileName;
        
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(filePath), sampleData);
            log.info("生成JSON模板: {}", filePath);
        } catch (IOException e) {
            log.error("生成JSON模板失败: {}", e.getMessage());
        }
    }
}
