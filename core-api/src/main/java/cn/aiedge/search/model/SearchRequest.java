package cn.aiedge.search.model;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 搜索请求
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "搜索请求")
public class SearchRequest {

    /**
     * 搜索关键词
     */
    @Schema(description = "搜索关键词", example = "客户名称")
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;

    /**
     * 搜索类型（可选）
     * customer: 客户
     * product: 产品
     * order: 订单
     * all: 全部（默认）
     */
    @Schema(description = "搜索类型: customer/product/order/all", example = "all")
    private String type = "all";

    /**
     * 页码（从1开始）
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
    @Schema(description = "排序字段", example = "createTime")
    private String sortBy;

    /**
     * 排序方向
     */
    @Schema(description = "排序方向: asc/desc", example = "desc")
    private String sortOrder = "desc";

    /**
     * 过滤条件
     */
    @Schema(description = "过滤条件")
    private Map<String, Object> filters;

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
     * 是否返回总数
     */
    @Schema(description = "是否返回总数", example = "true")
    private boolean returnTotal = true;

    /**
     * 租户ID（系统自动填充）
     */
    private Long tenantId;

    /**
     * 用户ID（系统自动填充）
     */
    private Long userId;

    // Getters and Setters
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
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
    public boolean isHighlight() { return highlight; }
    public void setHighlight(boolean highlight) { this.highlight = highlight; }
    public String getHighlightPre() { return highlightPre; }
    public void setHighlightPre(String highlightPre) { this.highlightPre = highlightPre; }
    public String getHighlightPost() { return highlightPost; }
    public void setHighlightPost(String highlightPost) { this.highlightPost = highlightPost; }
    public boolean isReturnTotal() { return returnTotal; }
    public void setReturnTotal(boolean returnTotal) { this.returnTotal = returnTotal; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    /**
     * 获取偏移量
     */
    public int getOffset() {
        return (page - 1) * pageSize;
    }

    /**
     * 获取有效的搜索类型列表
     */
    public List<String> getSearchTypes() {
        if ("all".equals(type)) {
            return List.of("customer", "product", "order");
        }
        return List.of(type);
    }
}
