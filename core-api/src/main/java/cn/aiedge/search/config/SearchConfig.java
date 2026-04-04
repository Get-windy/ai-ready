package cn.aiedge.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

/**
 * 搜索服务配置
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "search")
public class SearchConfig {

    /**
     * 是否启用搜索服务
     */
    private boolean enabled = true;

    /**
     * 搜索结果默认大小
     */
    private int defaultPageSize = 10;

    /**
     * 搜索结果最大大小
     */
    private int maxPageSize = 100;

    /**
     * 搜索历史保留天数
     */
    private int historyRetentionDays = 30;

    /**
     * 热门搜索词统计周期（天）
     */
    private int hotSearchPeriod = 7;

    /**
     * 热门搜索词显示数量
     */
    private int hotSearchCount = 10;

    /**
     * 搜索建议最小字符数
     */
    private int suggestionMinChars = 1;

    /**
     * 搜索建议最大数量
     */
    private int suggestionMaxCount = 10;

    /**
     * 搜索索引配置
     */
    private Map<String, IndexConfig> indices = new HashMap<>();

    /**
     * 索引配置类
     */
    public static class IndexConfig {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 索引名称
         */
        private String name;

        /**
         * 搜索字段列表
         */
        private List<String> searchFields = new ArrayList<>();

        /**
         * 权重配置
         */
        private Map<String, Double> fieldWeights = new HashMap<>();

        /**
         * 结果类型
         */
        private String resultType;

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getSearchFields() { return searchFields; }
        public void setSearchFields(List<String> searchFields) { this.searchFields = searchFields; }
        public Map<String, Double> getFieldWeights() { return fieldWeights; }
        public void setFieldWeights(Map<String, Double> fieldWeights) { this.fieldWeights = fieldWeights; }
        public String getResultType() { return resultType; }
        public void setResultType(String resultType) { this.resultType = resultType; }
    }

    /**
     * 默认索引配置
     */
    @Bean
    public Map<String, IndexConfig> defaultIndexConfigs() {
        Map<String, IndexConfig> configs = new HashMap<>();

        // 客户索引
        IndexConfig customerIndex = new IndexConfig();
        customerIndex.setName("customer");
        customerIndex.setEnabled(true);
        customerIndex.setSearchFields(Arrays.asList("name", "code", "contact", "phone", "email"));
        customerIndex.setFieldWeights(Map.of("name", 3.0, "code", 2.5, "contact", 1.5));
        customerIndex.setResultType("customer");
        configs.put("customer", customerIndex);

        // 产品索引
        IndexConfig productIndex = new IndexConfig();
        productIndex.setName("product");
        productIndex.setEnabled(true);
        productIndex.setSearchFields(Arrays.asList("name", "code", "description", "category"));
        productIndex.setFieldWeights(Map.of("name", 3.0, "code", 2.5, "category", 1.5));
        productIndex.setResultType("product");
        configs.put("product", productIndex);

        // 订单索引
        IndexConfig orderIndex = new IndexConfig();
        orderIndex.setName("order");
        orderIndex.setEnabled(true);
        orderIndex.setSearchFields(Arrays.asList("orderNo", "customerName", "productName", "status"));
        orderIndex.setFieldWeights(Map.of("orderNo", 3.0, "customerName", 2.0));
        orderIndex.setResultType("order");
        configs.put("order", orderIndex);

        return configs;
    }

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getDefaultPageSize() { return defaultPageSize; }
    public void setDefaultPageSize(int defaultPageSize) { this.defaultPageSize = defaultPageSize; }
    public int getMaxPageSize() { return maxPageSize; }
    public void setMaxPageSize(int maxPageSize) { this.maxPageSize = maxPageSize; }
    public int getHistoryRetentionDays() { return historyRetentionDays; }
    public void setHistoryRetentionDays(int historyRetentionDays) { this.historyRetentionDays = historyRetentionDays; }
    public int getHotSearchPeriod() { return hotSearchPeriod; }
    public void setHotSearchPeriod(int hotSearchPeriod) { this.hotSearchPeriod = hotSearchPeriod; }
    public int getHotSearchCount() { return hotSearchCount; }
    public void setHotSearchCount(int hotSearchCount) { this.hotSearchCount = hotSearchCount; }
    public int getSuggestionMinChars() { return suggestionMinChars; }
    public void setSuggestionMinChars(int suggestionMinChars) { this.suggestionMinChars = suggestionMinChars; }
    public int getSuggestionMaxCount() { return suggestionMaxCount; }
    public void setSuggestionMaxCount(int suggestionMaxCount) { this.suggestionMaxCount = suggestionMaxCount; }
    public Map<String, IndexConfig> getIndices() { return indices; }
    public void setIndices(Map<String, IndexConfig> indices) { this.indices = indices; }
}
