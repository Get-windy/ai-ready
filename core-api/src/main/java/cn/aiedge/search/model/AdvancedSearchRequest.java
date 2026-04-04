package cn.aiedge.search.model;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 高级搜索请求
 * 
 * 支持多种查询类型和复杂过滤条件
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "高级搜索请求")
public class AdvancedSearchRequest {

    /**
     * 查询类型
     */
    public enum QueryType {
        /**
         * 词条查询（精确匹配）
         */
        TERM,
        /**
         * 匹配查询（分词匹配）
         */
        MATCH,
        /**
         * 短语查询（要求词序连续）
         */
        PHRASE,
        /**
         * 前缀查询
         */
        PREFIX,
        /**
         * 通配符查询（支持*和?）
         */
        WILDCARD,
        /**
         * 模糊查询（编辑距离匹配）
         */
        FUZZY,
        /**
         * 正则表达式查询
         */
        REGEX
    }

    /**
     * 搜索查询词
     */
    @Schema(description = "搜索查询词", example = "客户名称")
    @NotBlank(message = "查询词不能为空")
    private String query;

    /**
     * 查询类型
     */
    @Schema(description = "查询类型: TERM/MATCH/PHRASE/PREFIX/WILDCARD/FUZZY/REGEX", example = "MATCH")
    private QueryType queryType = QueryType.MATCH;

    /**
     * 搜索类型
     */
    @Schema(description = "搜索类型: customer/product/order/all", example = "all")
    private String type = "all";

    /**
     * 页码
     */
    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码不能小于1")
    private int page = 1;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", example = "10")
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private int pageSize = 10;

    /**
     * 排序字段
     */
    @Schema(description = "排序字段", example = "score")
    private String sortBy = "score";

    /**
     * 排序方向
     */
    @Schema(description = "排序方向: asc/desc", example = "desc")
    private String sortOrder = "desc";

    /**
     * 过滤条件
     * 
     * 支持格式：
     * - 精确匹配: {"status": "active"}
     * - 多值匹配: {"status": ["active", "pending"]}
     * - 范围过滤: {"createTime": {"gte": "2024-01-01", "lte": "2024-12-31"}}
     */
    @Schema(description = "过滤条件")
    private Map<String, Object> filters;

    /**
     * 布尔查询条件
     */
    @Schema(description = "布尔查询条件")
    private BoolQueryClause boolQuery;

    /**
     * 是否高亮显示
     */
    @Schema(description = "是否高亮显示关键词", example = "true")
    private boolean highlight = true;

    /**
     * 高亮前缀
     */
    @Schema(description = "高亮前缀", example = "<em>")
    private String highlightPre = "<em>";

    /**
     * 高亮后缀
     */
    @Schema(description = "高亮后缀", example = "</em>")
    private String highlightPost = "</em>";

    /**
     * 模糊查询最大编辑距离
     */
    @Schema(description = "模糊查询最大编辑距离", example = "2")
    private int fuzzyDistance = 2;

    /**
     * 最小匹配比例（用于匹配查询）
     */
    @Schema(description = "最小匹配比例", example = "75%")
    private String minimumShouldMatch;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    // Getters and Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public QueryType getQueryType() { return queryType; }
    public void setQueryType(QueryType queryType) { this.queryType = queryType; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public String getSortOrder() { return sortOrder; }
    public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
    public Map<String, Object> getFilters() { return filters; }
    public void setFilters(Map<String, Object> filters) { this.filters = filters; }
    public BoolQueryClause getBoolQuery() { return boolQuery; }
    public void setBoolQuery(BoolQueryClause boolQuery) { this.boolQuery = boolQuery; }
    public boolean isHighlight() { return highlight; }
    public void setHighlight(boolean highlight) { this.highlight = highlight; }
    public String getHighlightPre() { return highlightPre; }
    public void setHighlightPre(String highlightPre) { this.highlightPre = highlightPre; }
    public String getHighlightPost() { return highlightPost; }
    public void setHighlightPost(String highlightPost) { this.highlightPost = highlightPost; }
    public int getFuzzyDistance() { return fuzzyDistance; }
    public void setFuzzyDistance(int fuzzyDistance) { this.fuzzyDistance = fuzzyDistance; }
    public String getMinimumShouldMatch() { return minimumShouldMatch; }
    public void setMinimumShouldMatch(String minimumShouldMatch) { this.minimumShouldMatch = minimumShouldMatch; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public int getOffset() {
        return (page - 1) * pageSize;
    }

    public List<String> getSearchTypes() {
        if ("all".equals(type)) {
            return List.of("customer", "product", "order");
        }
        return List.of(type);
    }

    /**
     * 布尔查询条件
     */
    @Schema(description = "布尔查询条件")
    public static class BoolQueryClause {
        /**
         * 必须匹配的查询
         */
        @Schema(description = "必须匹配的查询")
        private List<AdvancedSearchRequest> must;

        /**
         * 应该匹配的查询（至少匹配一个）
         */
        @Schema(description = "应该匹配的查询")
        private List<AdvancedSearchRequest> should;

        /**
         * 必须不匹配的查询
         */
        @Schema(description = "必须不匹配的查询")
        private List<AdvancedSearchRequest> mustNot;

        /**
         * should查询最小匹配数
         */
        @Schema(description = "should查询最小匹配数")
        private int minimumShouldMatch = 1;

        // Getters and Setters
        public List<AdvancedSearchRequest> getMust() { return must; }
        public void setMust(List<AdvancedSearchRequest> must) { this.must = must; }
        public List<AdvancedSearchRequest> getShould() { return should; }
        public void setShould(List<AdvancedSearchRequest> should) { this.should = should; }
        public List<AdvancedSearchRequest> getMustNot() { return mustNot; }
        public void setMustNot(List<AdvancedSearchRequest> mustNot) { this.mustNot = mustNot; }
        public int getMinimumShouldMatch() { return minimumShouldMatch; }
        public void setMinimumShouldMatch(int minimumShouldMatch) { this.minimumShouldMatch = minimumShouldMatch; }
    }
}
