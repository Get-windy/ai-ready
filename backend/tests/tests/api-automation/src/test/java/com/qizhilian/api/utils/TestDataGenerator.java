package com.qizhilian.api.utils;

import com.github.javafaker.Faker;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 测试数据生成器
 * 用于生成各种测试数据
 */
@Slf4j
public class TestDataGenerator {
    
    private final Faker faker;
    private final Random random;
    private final DateTimeFormatter dateTimeFormatter;
    
    public TestDataGenerator(String locale) {
        this.faker = new Faker(new Locale(locale));
        this.random = new Random();
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }
    
    public TestDataGenerator() {
        this("zh-CN");
    }
    
    // ==================== 用户相关数据 ====================
    
    /**
     * 生成随机用户名
     */
    public String generateUsername() {
        return faker.name().username() + random.nextInt(1000);
    }
    
    /**
     * 生成随机姓名
     */
    public String generateName() {
        return faker.name().fullName();
    }
    
    /**
     * 生成随机邮箱
     */
    public String generateEmail() {
        return faker.internet().emailAddress();
    }
    
    /**
     * 生成随机手机号（中国格式）
     */
    public String generatePhone() {
        String[] prefixes = {"138", "139", "137", "136", "135", "134", "159", "158", "157", "150", "151", "152", "188", "187", "182", "183", "184", "178", "130", "131", "132", "156", "155", "186", "185", "176", "133", "153", "189", "180", "181", "177"};
        String prefix = prefixes[random.nextInt(prefixes.length)];
        String suffix = String.format("%08d", random.nextInt(100000000));
        return prefix + suffix;
    }
    
    /**
     * 生成随机密码
     */
    public String generatePassword(int minLength, int maxLength) {
        int length = minLength + random.nextInt(maxLength - minLength + 1);
        StringBuilder password = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
    
    /**
     * 生成随机密码（默认8-16位）
     */
    public String generatePassword() {
        return generatePassword(8, 16);
    }
    
    // ==================== 企业相关数据 ====================
    
    /**
     * 生成随机公司名称
     */
    public String generateCompanyName() {
        return faker.company().name() + "有限公司";
    }
    
    /**
     * 生成随机部门名称
     */
    public String generateDepartmentName() {
        String[] departments = {"技术部", "销售部", "市场部", "人力资源部", "财务部", "运营部", "产品部", "客服部", "行政部", "采购部"};
        return departments[random.nextInt(departments.length)];
    }
    
    /**
     * 生成随机职位
     */
    public String generateJobTitle() {
        return faker.job().title();
    }
    
    /**
     * 生成统一社会信用代码（18位）
     */
    public String generateUnifiedCreditCode() {
        StringBuilder code = new StringBuilder();
        // 登记管理部门代码(1位) + 机构类别代码(1位) + 登记管理机关行政区划码(6位)
        code.append("91"); // 企业
        code.append(String.format("%06d", random.nextInt(1000000)));
        // 主体标识码(组织机构代码)(9位)
        for (int i = 0; i < 8; i++) {
            code.append(random.nextInt(10));
        }
        // 校验码(1位)
        code.append(random.nextInt(10));
        return code.toString();
    }
    
    // ==================== 地址相关数据 ====================
    
    /**
     * 生成随机地址
     */
    public String generateAddress() {
        return faker.address().fullAddress();
    }
    
    /**
     * 生成随机城市
     */
    public String generateCity() {
        return faker.address().city();
    }
    
    // ==================== ERP相关数据 ====================
    
    /**
     * 生成产品名称
     */
    public String generateProductName() {
        String[] products = {"笔记本电脑", "智能手机", "平板电脑", "显示器", "键盘", "鼠标", "耳机", "打印机", "扫描仪", "投影仪"};
        return products[random.nextInt(products.length)] + " " + faker.commerce().productName();
    }
    
    /**
     * 生成SKU编码
     */
    public String generateSku() {
        return "SKU-" + System.currentTimeMillis() + "-" + random.nextInt(1000);
    }
    
    /**
     * 生成订单号
     */
    public String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) 
            + String.format("%06d", random.nextInt(1000000));
    }
    
    /**
     * 生成采购单号
     */
    public String generatePurchaseNo() {
        return "PO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) 
            + String.format("%06d", random.nextInt(1000000));
    }
    
    /**
     * 生成随机金额
     */
    public double generateAmount(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }
    
    /**
     * 生成随机价格
     */
    public double generatePrice() {
        return generateAmount(1, 10000);
    }
    
    /**
     * 生成随机数量
     */
    public int generateQuantity(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
    
    // ==================== CRM相关数据 ====================
    
    /**
     * 生成客户名称
     */
    public String generateCustomerName() {
        return generateCompanyName();
    }
    
    /**
     * 生成线索来源
     */
    public String generateLeadSource() {
        String[] sources = {"官网", "电话咨询", "展会", "转介绍", "社交媒体", "广告投放", "邮件营销", "合作伙伴"};
        return sources[random.nextInt(sources.length)];
    }
    
    /**
     * 生成商机阶段
     */
    public String generateOpportunityStage() {
        String[] stages = {"初步接触", "需求确认", "方案报价", "商务谈判", "合同签订", "赢单", "输单"};
        return stages[random.nextInt(stages.length)];
    }
    
    /**
     * 生成客户等级
     */
    public String generateCustomerLevel() {
        String[] levels = {"A级", "B级", "C级", "D级"};
        return levels[random.nextInt(levels.length)];
    }
    
    // ==================== 通用数据 ====================
    
    /**
     * 生成UUID
     */
    public String generateUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成随机字符串
     */
    public String generateString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    /**
     * 生成当前时间字符串
     */
    public String generateCurrentTime() {
        return LocalDateTime.now().format(dateTimeFormatter);
    }
    
    /**
     * 生成随机日期时间
     */
    public String generateDateTime(int daysOffset) {
        return LocalDateTime.now().plusDays(daysOffset).format(dateTimeFormatter);
    }
    
    /**
     * 从列表中随机选择一项
     */
    public <T> T randomChoice(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(random.nextInt(list.size()));
    }
    
    /**
     * 从数组中随机选择一项
     */
    public <T> T randomChoice(T[] array) {
        if (array == null || array.length == 0) {
            return null;
        }
        return array[random.nextInt(array.length)];
    }
    
    /**
     * 生成随机布尔值
     */
    public boolean randomBoolean() {
        return random.nextBoolean();
    }
    
    /**
     * 生成指定范围内的随机整数
     */
    public int randomInt(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
}
