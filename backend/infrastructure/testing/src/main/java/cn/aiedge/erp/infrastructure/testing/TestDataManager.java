package cn.aiedge.erp.infrastructure.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 测试数据管理器
 * 提供测试数据的生成、管理和清理功能
 */
public class TestDataManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TestDataManager.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, List<Map<String, Object>>> testDataCache = new ConcurrentHashMap<>();
    private static final String TEST_DATA_DIR = "test-data";
    
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final String moduleName;
    
    public TestDataManager(JdbcTemplate jdbcTemplate, 
                          TransactionTemplate transactionTemplate,
                          String moduleName) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.moduleName = moduleName;
        loadTestData();
    }
    
    /**
     * 加载测试数据
     */
    private void loadTestData() {
        String dataPath = String.format("%s/%s", TEST_DATA_DIR, moduleName);
        Path dirPath = Paths.get(dataPath);
        
        if (!Files.exists(dirPath)) {
            logger.warn("测试数据目录不存在: {}", dirPath);
            return;
        }
        
        try {
            Files.list(dirPath)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(this::loadDataFile);
            
            logger.info("加载模块 {} 的测试数据完成，共 {} 个文件", 
                moduleName, testDataCache.size());
        } catch (IOException e) {
            logger.error("加载测试数据失败", e);
        }
    }
    
    /**
     * 加载单个数据文件
     */
    private void loadDataFile(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString();
            String dataType = fileName.replace(".json", "");
            String content = Files.readString(filePath);
            
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            List<Map<String, Object>> data = objectMapper.readValue(
                content, 
                typeFactory.constructCollectionType(List.class, Map.class)
            );
            
            testDataCache.put(dataType, data);
            logger.info("加载测试数据文件: {}，数据量: {}", fileName, data.size());
        } catch (IOException e) {
            logger.error("加载测试数据文件失败: {}", filePath, e);
        }
    }
    
    /**
     * 获取测试数据
     */
    public List<Map<String, Object>> getTestData(String dataType) {
        return testDataCache.getOrDefault(dataType, new ArrayList<>());
    }
    
    /**
     * 获取单个测试数据
     */
    public Map<String, Object> getSingleTestData(String dataType) {
        List<Map<String, Object>> dataList = getTestData(dataType);
        return dataList.isEmpty() ? new HashMap<>() : dataList.get(0);
    }
    
    /**
     * 生成随机测试数据
     */
    public Map<String, Object> generateRandomData(String entityName) {
        Map<String, Object> data = new HashMap<>();
        
        switch (entityName.toLowerCase()) {
            case "user":
                data.put("username", "test_user_" + System.currentTimeMillis());
                data.put("email", "test" + System.currentTimeMillis() + "@example.com");
                data.put("phone", "138" + String.format("%08d", new Random().nextInt(99999999)));
                data.put("status", "ACTIVE");
                break;
                
            case "product":
                data.put("productCode", "PROD_" + System.currentTimeMillis());
                data.put("productName", "测试产品" + new Random().nextInt(1000));
                data.put("price", new Random().nextDouble() * 1000);
                data.put("stockQuantity", new Random().nextInt(1000));
                data.put("category", "TEST_CATEGORY");
                break;
                
            case "order":
                data.put("orderNo", "ORD_" + System.currentTimeMillis());
                data.put("customerId", "CUST_" + new Random().nextInt(10000));
                data.put("totalAmount", new Random().nextDouble() * 10000);
                data.put("orderStatus", "PENDING");
                data.put("orderDate", new Date());
                break;
                
            case "batch":
                data.put("batchNo", "BATCH_" + System.currentTimeMillis());
                data.put("productId", "PROD_" + new Random().nextInt(10000));
                data.put("quantity", new Random().nextInt(1000));
                data.put("productionDate", new Date());
                data.put("expiryDate", new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000));
                data.put("qualityStatus", "PASSED");
                break;
                
            default:
                data.put("id", UUID.randomUUID().toString());
                data.put("name", "测试数据" + new Random().nextInt(1000));
                data.put("createdAt", new Date());
                break;
        }
        
        return data;
    }
    
    /**
     * 批量插入测试数据
     */
    public void insertTestData(String tableName, List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            logger.warn("没有测试数据需要插入");
            return;
        }
        
        transactionTemplate.execute(status -> {
            try {
                data.forEach(row -> {
                    String columns = String.join(", ", row.keySet());
                    String placeholders = row.keySet().stream()
                        .map(k -> "?")
                        .collect(Collectors.joining(", "));
                    String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", 
                        tableName, columns, placeholders);
                    
                    jdbcTemplate.update(sql, row.values().toArray());
                });
                
                logger.info("成功插入 {} 条测试数据到表 {}", data.size(), tableName);
                return data.size();
            } catch (Exception e) {
                status.setRollbackOnly();
                logger.error("插入测试数据失败", e);
                throw new RuntimeException("插入测试数据失败", e);
            }
        });
    }
    
    /**
     * 清理测试数据
     */
    public void cleanupTestData(String tableName, String condition) {
        String sql = condition == null || condition.trim().isEmpty() 
            ? String.format("DELETE FROM %s", tableName)
            : String.format("DELETE FROM %s WHERE %s", tableName, condition);
        
        int deletedRows = jdbcTemplate.update(sql);
        logger.info("清理表 {} 的测试数据，删除 {} 行", tableName, deletedRows);
    }
    
    /**
     * 清理所有测试数据
     */
    public void cleanupAllTestData() {
        List<String> tables = Arrays.asList(
            "batch_management", "order_details", "product_inventory",
            "user_accounts", "quality_records", "transaction_logs"
        );
        
        tables.forEach(table -> {
            try {
                jdbcTemplate.update("DELETE FROM " + table);
                logger.info("清理表 {} 的测试数据", table);
            } catch (Exception e) {
                logger.warn("清理表 {} 失败: {}", table, e.getMessage());
            }
        });
        
        logger.info("所有测试数据清理完成");
    }
    
    /**
     * 保存测试数据到文件
     */
    public void saveTestDataToFile(String dataType, List<Map<String, Object>> data) {
        try {
            Path dirPath = Paths.get(TEST_DATA_DIR, moduleName);
            Files.createDirectories(dirPath);
            
            Path filePath = dirPath.resolve(dataType + ".json");
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(data);
            
            Files.writeString(filePath, jsonContent);
            logger.info("保存测试数据到文件: {}，数据量: {}", filePath, data.size());
        } catch (IOException e) {
            logger.error("保存测试数据到文件失败", e);
        }
    }
    
    /**
     * 生成测试数据报告
     */
    public String generateTestDataReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== 测试数据报告 ===\n");
        report.append("模块名称: ").append(moduleName).append("\n");
        report.append("测试数据类型数量: ").append(testDataCache.size()).append("\n");
        
        testDataCache.forEach((dataType, dataList) -> {
            report.append(String.format("  %s: %d 条记录\n", dataType, dataList.size()));
        });
        
        report.append("===================");
        return report.toString();
    }
}