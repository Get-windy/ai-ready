package com.qizhilian.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * 测试数据工具类
 * 提供测试数据加载和生成方法
 */
@Slf4j
public class TestDataUtil {
    
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    
    private static final String DATA_DIR = "src/test/resources/data/";
    
    /**
     * 从JSON文件加载对象
     */
    public static <T> T loadFromJson(String fileName, Class<T> clazz) {
        String filePath = DATA_DIR + fileName;
        try {
            return objectMapper.readValue(new File(filePath), clazz);
        } catch (IOException e) {
            log.error("加载JSON文件失败: {}, 错误: {}", filePath, e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 从JSON文件加载为Map
     */
    public static Map<String, Object> loadJsonAsMap(String fileName) {
        String filePath = DATA_DIR + fileName;
        try {
            return objectMapper.readValue(new File(filePath), 
                    new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            log.error("加载JSON文件失败: {}, 错误: {}", filePath, e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 生成随机用户名
     */
    public static String generateRandomUsername() {
        return "test_user_" + System.currentTimeMillis();
    }
    
    /**
     * 生成随机邮箱
     */
    public static String generateRandomEmail() {
        return "test_" + System.currentTimeMillis() + "@qizhilian.com";
    }
    
    /**
     * 生成随机手机号
     */
    public static String generateRandomPhone() {
        return "138" + String.format("%08d", (int)(Math.random() * 100000000));
    }
    
    /**
     * 生成批次号
     */
    public static String generateBatchNo() {
        return "BATCH_" + System.currentTimeMillis();
    }
    
    /**
     * 生成订单号
     */
    public static String generateOrderNo() {
        return "ORD_" + System.currentTimeMillis();
    }
    
    /**
     * 将对象转换为JSON字符串
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (IOException e) {
            log.error("对象转JSON失败: {}", e.getMessage());
            return "{}";
        }
    }
    
    /**
     * 从JSON字符串解析对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            log.error("JSON解析失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
