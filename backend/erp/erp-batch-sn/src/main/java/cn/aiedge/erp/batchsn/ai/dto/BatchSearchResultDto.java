package cn.aiedge.erp.batchsn.ai.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 批次搜索结果DTO
 */
@Data
public class BatchSearchResultDto {
    
    private Long batchId;
    private String batchNumber;
    private Long productId;
    private String productName;
    private String productCode;
    
    // 搜索匹配信息
    private Double relevanceScore; // 相关度分数 0-1
    private List<MatchHighlight> matchHighlights; // 匹配高亮
    private String matchReason; // 匹配原因
    private Map<String, Double> featureScores; // 特征匹配分数
    
    // 批次基本信息
    private String status; // 批次状态
    private Integer quantity; // 数量
    private String unit; // 单位
    private Date productionDate; // 生产日期
    private Date expiryDate; // 过期日期
    private String storageLocation; // 存储位置
    
    // 质量信息
    private String qualityLevel; // 质量等级
    private Double qualityScore; // 质量分数
    private String inspectionResult; // 检验结果
    private Date lastInspectionDate; // 最后检验日期
    
    // 供应商信息
    private Long supplierId;
    private String supplierName;
    private String supplierCode;
    
    // 价格信息
    private Double unitPrice; // 单价
    private String currency; // 货币
    private Double totalValue; // 总价值
    
    // 搜索元数据
    private String searchAlgorithm; // 搜索算法
    private Long searchDurationMs; // 搜索耗时(毫秒)
    private Integer totalMatches; // 总匹配数
    private Integer rank; // 排名
    
    // 相似批次信息
    private List<SimilarBatchInfo> similarBatches;
    
    /**
     * 匹配高亮
     */
    @Data
    public static class MatchHighlight {
        private String field; // 匹配字段
        private String snippet; // 匹配片段
        private List<Integer> positions; // 匹配位置
        private Double score; // 匹配分数
        
        public MatchHighlight() {}
        
        public MatchHighlight(String field, String snippet) {
            this.field = field;
            this.snippet = snippet;
        }
    }
    
    /**
     * 相似批次信息
     */
    @Data
    public static class SimilarBatchInfo {
        private Long batchId;
        private String batchNumber;
        private Double similarityScore; // 相似度分数
        private String similarityReason; // 相似原因
        
        public SimilarBatchInfo() {}
        
        public SimilarBatchInfo(Long batchId, String batchNumber, Double similarityScore) {
            this.batchId = batchId;
            this.batchNumber = batchNumber;
            this.similarityScore = similarityScore;
        }
    }
    
    /**
     * 获取相关度等级
     */
    public String getRelevanceLevel() {
        if (relevanceScore == null) return "未知";
        
        if (relevanceScore >= 0.9) return "极高";
        else if (relevanceScore >= 0.8) return "高";
        else if (relevanceScore >= 0.6) return "中";
        else if (relevanceScore >= 0.4) return "低";
        else return "极低";
    }
    
    /**
     * 判断是否高质量匹配
     */
    public boolean isHighQualityMatch() {
        return relevanceScore != null && relevanceScore >= 0.7;
    }
    
    /**
     * 获取质量等级描述
     */
    public String getQualityLevelDescription() {
        if (qualityLevel == null) return "未知";
        
        switch (qualityLevel) {
            case "优": return "质量优秀";
            case "良": return "质量良好";
            case "中": return "质量中等";
            case "差": return "质量较差";
            default: return qualityLevel;
        }
    }
    
    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        if (status == null) return "未知";
        
        switch (status.toLowerCase()) {
            case "active": return "活跃";
            case "inactive": return "非活跃";
            case "expired": return "已过期";
            case "recalled": return "已召回";
            case "quarantined": return "隔离中";
            case "disposed": return "已处置";
            default: return status;
        }
    }
    
    /**
     * 获取格式化后的总价值
     */
    public String getFormattedTotalValue() {
        if (totalValue == null || currency == null) {
            return "N/A";
        }
        return String.format("%.2f %s", totalValue, currency);
    }
}