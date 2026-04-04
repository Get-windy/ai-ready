package cn.aiedge.search.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 搜索历史
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "搜索历史")
public class SearchHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 历史ID
     */
    @Schema(description = "历史ID")
    private Long id;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 搜索关键词
     */
    @Schema(description = "搜索关键词")
    private String keyword;

    /**
     * 搜索类型
     */
    @Schema(description = "搜索类型")
    private String searchType;

    /**
     * 结果数量
     */
    @Schema(description = "结果数量")
    private Integer resultCount;

    /**
     * 搜索时间
     */
    @Schema(description = "搜索时间")
    private LocalDateTime searchTime;

    /**
     * 租户ID
     */
    @Schema(description = "租户ID")
    private Long tenantId;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }
    public Integer getResultCount() { return resultCount; }
    public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }
    public LocalDateTime getSearchTime() { return searchTime; }
    public void setSearchTime(LocalDateTime searchTime) { this.searchTime = searchTime; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    /**
     * 创建搜索历史
     */
    public static SearchHistory of(Long userId, String keyword, String searchType, Integer resultCount, Long tenantId) {
        SearchHistory history = new SearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword);
        history.setSearchType(searchType);
        history.setResultCount(resultCount);
        history.setSearchTime(LocalDateTime.now());
        history.setTenantId(tenantId);
        return history;
    }
}
