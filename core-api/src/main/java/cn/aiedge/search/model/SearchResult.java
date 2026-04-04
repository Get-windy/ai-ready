package cn.aiedge.search.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 搜索结果
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "搜索结果")
public class SearchResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 结果ID
     */
    @Schema(description = "结果ID")
    private Long id;

    /**
     * 结果类型
     */
    @Schema(description = "结果类型: customer/product/order")
    private String type;

    /**
     * 结果标题
     */
    @Schema(description = "结果标题")
    private String title;

    /**
     * 结果描述
     */
    @Schema(description = "结果描述")
    private String description;

    /**
     * 高亮标题
     */
    @Schema(description = "高亮标题")
    private String highlightedTitle;

    /**
     * 高亮描述
     */
    @Schema(description = "高亮描述")
    private String highlightedDescription;

    /**
     * 匹配分数
     */
    @Schema(description = "匹配分数")
    private double score;

    /**
     * 匹配的字段
     */
    @Schema(description = "匹配的字段")
    private List<String> matchedFields;

    /**
     * 扩展数据
     */
    @Schema(description = "扩展数据")
    private Map<String, Object> data;

    /**
     * 跳转URL
     */
    @Schema(description = "跳转URL")
    private String url;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Long createTime;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getHighlightedTitle() { return highlightedTitle; }
    public void setHighlightedTitle(String highlightedTitle) { this.highlightedTitle = highlightedTitle; }
    public String getHighlightededDescription() { return highlightedDescription; }
    public void setHighlightededDescription(String highlightedDescription) { this.highlightedDescription = highlightedDescription; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public List<String> getMatchedFields() { return matchedFields; }
    public void setMatchedFields(List<String> matchedFields) { this.matchedFields = matchedFields; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Long getCreateTime() { return createTime; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }

    /**
     * 构建客户搜索结果
     */
    public static SearchResult fromCustomer(Long id, String name, String code, String contact, double score) {
        SearchResult result = new SearchResult();
        result.setId(id);
        result.setType("customer");
        result.setTitle(name);
        result.setDescription(String.format("客户编码: %s, 联系人: %s", code, contact));
        result.setScore(score);
        result.setUrl("/customer/" + id);
        return result;
    }

    /**
     * 构建产品搜索结果
     */
    public static SearchResult fromProduct(Long id, String name, String code, String category, double score) {
        SearchResult result = new SearchResult();
        result.setId(id);
        result.setType("product");
        result.setTitle(name);
        result.setDescription(String.format("产品编码: %s, 分类: %s", code, category));
        result.setScore(score);
        result.setUrl("/product/" + id);
        return result;
    }

    /**
     * 构建订单搜索结果
     */
    public static SearchResult fromOrder(Long id, String orderNo, String customerName, String status, double score) {
        SearchResult result = new SearchResult();
        result.setId(id);
        result.setType("order");
        result.setTitle(orderNo);
        result.setDescription(String.format("客户: %s, 状态: %s", customerName, status));
        result.setScore(score);
        result.setUrl("/order/" + id);
        return result;
    }
}
