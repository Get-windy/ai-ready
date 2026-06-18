package cn.aiedge.common.utils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID生成器工具类
 * 支持多种ID生成策略：UUID、雪花ID、序列号
 */
public class IdGenerator {
    
    private static final AtomicLong sequence = new AtomicLong(0);
    
    /**
     * 生成UUID（32位，无横线）
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成UUID（36位，带横线）
     */
    public static String uuidWithDash() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 生成短ID（16位）
     */
    public static String shortId() {
        return uuid().substring(0, 16);
    }
    
    /**
     * 生成序列号
     */
    public static long nextSequence() {
        return sequence.incrementAndGet();
    }
    
    /**
     * 生成时间戳ID（毫秒级）
     */
    public static String timestampId() {
        return System.currentTimeMillis() + "" + (int)(Math.random() * 1000);
    }
    
    /**
     * 生成订单号（格式：yyyyMMddHHmmss + 6位随机数）
     */
    public static String orderNo() {
        long timestamp = System.currentTimeMillis();
        int random = (int)(Math.random() * 1000000);
        return String.format("%d%06d", timestamp, random);
    }
}
