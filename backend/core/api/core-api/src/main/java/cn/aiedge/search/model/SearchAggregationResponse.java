package cn.aiedge.search.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 搜索聚合响应
 * 
 * 包含搜索结果和聚合统计信息
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "搜索聚合响应")
public class SearchAggregationResponse {

    /**
     * 搜索响应
     */
    @Schema(description = "搜索响应")
    private SearchResponse searchResponse;

    /**
     * 类型聚合统计
     */
    @Schema(description = "按类型统计数量", example = "{\"customer\": 10, \"product\": 25, \"order\": 15}")
    private Map<String, Long> typeAggregation;

    /**
     * 字段聚合统计
     */
    @Schema(description = "按字段统计")
    private Map<String, Map<String, Long>> fieldAggregation;

    /**
     * 时间分布统计
     */
    @Schema(description = "按时间分布统计")
    private Map<String, Long> timeAggregation;

    /**
     * 热门关键词
     */
    @Schema(description = "热门关键词列表")
    private List<String> hotKeywords;

    /**
     * 相关关键词推荐
     */
    @Schema(description = "相关关键词推荐")
    private List<String> relatedKeywords;

    /**
     * 搜索建议
     */
    @Schema(description = "搜索建议")
    private List<SearchSuggestion> suggestions;

    /**
     * 搜索统计
     */
    @Schema(description = "搜索统计信息")
    private SearchStatistics statistics;

    // Getters and Setters
    public SearchResponse getSearchResponse() { return searchResponse; }
    public void setSearchResponse(SearchResponse searchResponse) { this.searchResponse = searchResponse; }
    public Map<String, Long> getTypeAggregation() { return typeAggregation; }
    public void setTypeAggregation(Map<String, Long> typeAggregation) { this.typeAggregation = typeAggregation; }
    public Map<String, Map<String, Long>> getFieldAggregation() { return fieldAggregation; }
    public void setFieldAggregation(Map<String, Map<String, Long>> fieldAggregation) { this.fieldAggregation = fieldAggregation; }
    public Map<String, Long> getTimeAggregation() { return timeAggregation; }
    public void setTimeAggregation(Map<String, Long> timeAggregation) { this.timeAggregation = timeAggregation; }
    public List<String> getHotKeywords() { return hotKeywords; }
    public void setHotKeywords(List<String> hotKeywords) { this.hotKeywords = hotKeywords; }
    public List<String> getRelatedKeywords() { return relatedKeywords; }
    public void setRelatedKeywords(List<String> relatedKeywords) { this.relatedKeywords = relatedKeywords; }
    public List<SearchSuggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<SearchSuggestion> suggestions) { this.suggestions = suggestions; }
    public SearchStatistics getStatistics() { return statistics; }
    public void setStatistics(SearchStatistics statistics) { this.statistics = statistics; }

    /**
     * 搜索统计信息
     */
    @Schema(description = "搜索统计信息")
    public static class SearchStatistics {

        /**
         * 总搜索次数
         */
        @Schema(description = "总搜索次数")
        private Long totalSearches;

        /**
         * 平均响应时间（毫秒）
         */
        @Schema(description = "平均响应时间（毫秒）")
        private Double avgResponseTime;

        /**
         * 平均结果数
         */
        @Schema(description = "平均结果数")
        private Double avgResultCount;

        /**
         * 无结果搜索占比
         */
        @Schema(description = "无结果搜索占比（百分比）")
        private Double zeroResultRate;

        /**
         * 搜索词平均长度
         */
        @Schema(description = "搜索词平均长度")
        private Double avgQueryLength;

        /**
         * 独立搜索词数
         */
        @Schema(description = "独立搜索词数")
        private Long uniqueQueries;

        /**
         * 独立搜索用户数
         */
        @Schema(description = "独立搜索用户数")
        private Long uniqueUsers;

        // Getters and Setters
        public Long getTotalSearches() { return totalSearches; }
        public void setTotalSearches(Long totalSearches) { this.totalSearches = totalSearches; }
        public Double getAvgResponseTime() { return avgResponseTime; }
        public void setAvgResponseTime(Double avgResponseTime) { this.avgResponseTime = avgResponseTime; }
        public Double getAvgResultCount() { return avgResultCount; }
        public void setAvgResultCount(Double avgResultCount) { this.avgResultCount = avgResultCount; }
        public Double getZeroResultRate() { return zeroResultRate; }
        public void setZeroResultRate(Double zeroResultRate) { this.zeroResultRate = zeroResultRate; }
        public Double getAvgQueryLength() { return avgQueryLength; }
        public void setAvgQueryLength(Double avgQueryLength) { this.avgQueryLength = avgQueryLength; }
        public Long getUniqueQueries() { return uniqueQueries; }
        public void setUniqueQueries(Long uniqueQueries) { this.uniqueQueries = uniqueQueries; }
        public Long getUniqueUsers() { return uniqueUsers; }
        public void setUniqueUsers(Long uniqueUsers) { this.uniqueUsers = uniqueUsers; }
    }
}
