package cn.aiedge.search.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * 搜索响应
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "搜索响应")
public class SearchResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 搜索关键词
     */
    @Schema(description = "搜索关键词")
    private String keyword;

    /**
     * 搜索类型
     */
    @Schema(description = "搜索类型")
    private String type;

    /**
     * 结果列表
     */
    @Schema(description = "结果列表")
    private List<SearchResult> results;

    /**
     * 总数
     */
    @Schema(description = "总数")
    private Long total;

    /**
     * 当前页
     */
    @Schema(description = "当前页")
    private int page;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小")
    private int pageSize;

    /**
     * 总页数
     */
    @Schema(description = "总页数")
    private int totalPages;

    /**
     * 搜索耗时（毫秒）
     */
    @Schema(description = "搜索耗时（毫秒）")
    private Long took;

    /**
     * 是否有更多结果
     */
    @Schema(description = "是否有更多结果")
    private boolean hasMore;

    /**
     * 搜索建议
     */
    @Schema(description = "搜索建议")
    private List<SearchSuggestion> suggestions;

    /**
     * 热门搜索词
     */
    @Schema(description = "热门搜索词")
    private List<String> hotSearches;

    // Getters and Setters
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<SearchResult> getResults() { return results; }
    public void setResults(List<SearchResult> results) { this.results = results; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public Long getTook() { return took; }
    public void setTook(Long took) { this.took = took; }
    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
    public List<SearchSuggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<SearchSuggestion> suggestions) { this.suggestions = suggestions; }
    public List<String> getHotSearches() { return hotSearches; }
    public void setHotSearches(List<String> hotSearches) { this.hotSearches = hotSearches; }

    /**
     * 计算总页数
     */
    public static int calculateTotalPages(long total, int pageSize) {
        if (pageSize <= 0) return 0;
        return (int) Math.ceil((double) total / pageSize);
    }

    /**
     * 创建空响应
     */
    public static SearchResponse empty(String keyword, String type, int page, int pageSize) {
        SearchResponse response = new SearchResponse();
        response.setKeyword(keyword);
        response.setType(type);
        response.setResults(List.of());
        response.setTotal(0L);
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotalPages(0);
        response.setTook(0L);
        response.setHasMore(false);
        return response;
    }
}
