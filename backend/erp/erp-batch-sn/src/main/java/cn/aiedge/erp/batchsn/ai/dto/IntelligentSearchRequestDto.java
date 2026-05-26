package cn.aiedge.erp.batchsn.ai.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 智能搜索请求DTO
 */
@Data
public class IntelligentSearchRequestDto {
    
    private String query; // 搜索查询
    private String queryType; // 查询类型: keyword, semantic, hybrid
    private List<String> filters; // 过滤器
    private Map<String, Object> filterValues; // 过滤值
    
    // 搜索选项
    private Boolean enableFuzzySearch = true; // 启用模糊搜索
    private Boolean enableSemanticExpansion = true; // 启用语义扩展
    private Boolean enableSynonyms = true; // 启用同义词
    
    // 分页参数
    private Integer page = 1;
    private Integer pageSize = 20;
    private String sortBy; // 排序字段
    private String sortOrder = "desc"; // 排序顺序: asc, desc
    
    // 搜索范围
    private List<Long> productIds; // 产品ID范围
    private List<String> batchStatuses; // 批次状态范围
    private DateRange productionDateRange; // 生产日期范围
    private DateRange inspectionDateRange; // 检验日期范围
    
    // 高级选项
    private Double minSimilarityScore = 0.5; // 最小相似度分数
    private Integer maxResults = 100; // 最大结果数
    private Boolean includeSimilarBatches = false; // 是否包含相似批次
    private Integer similarBatchLimit = 5; // 相似批次限制
    
    /**
     * 日期范围
     */
    @Data
    public static class DateRange {
        private String startDate;
        private String endDate;
        
        public DateRange() {}
        
        public DateRange(String startDate, String endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
    
    /**
     * 验证请求参数
     */
    public boolean isValid() {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        
        if (page != null && page < 1) {
            return false;
        }
        
        if (pageSize != null && (pageSize < 1 || pageSize > 100)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取查询类型描述
     */
    public String getQueryTypeDescription() {
        if (queryType == null) return "混合搜索";
        
        switch (queryType.toLowerCase()) {
            case "keyword": return "关键词搜索";
            case "semantic": return "语义搜索";
            case "hybrid": return "混合搜索";
            default: return "未知搜索类型";
        }
    }
    
    /**
     * 获取搜索模式描述
     */
    public String getSearchModeDescription() {
        StringBuilder mode = new StringBuilder();
        
        if (enableFuzzySearch != null && enableFuzzySearch) {
            mode.append("模糊搜索");
        }
        
        if (enableSemanticExpansion != null && enableSemanticExpansion) {
            if (mode.length() > 0) mode.append(" + ");
            mode.append("语义扩展");
        }
        
        if (enableSynonyms != null && enableSynonyms) {
            if (mode.length() > 0) mode.append(" + ");
            mode.append("同义词扩展");
        }
        
        return mode.length() > 0 ? mode.toString() : "基础搜索";
    }
}