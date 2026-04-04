package cn.aiedge.search.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * 搜索建议
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "搜索建议")
public class SearchSuggestion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 建议文本
     */
    @Schema(description = "建议文本")
    private String text;

    /**
     * 建议类型
     */
    @Schema(description = "建议类型: keyword/history/hot")
    private String type;

    /**
     * 匹配分数
     */
    @Schema(description = "匹配分数")
    private double score;

    /**
     * 结果数量（预估）
     */
    @Schema(description = "结果数量（预估）")
    private Long count;

    /**
     * 关联结果
     */
    @Schema(description = "关联结果（如客户名称）")
    private List<SearchResult> results;

    // Getters and Setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
    public List<SearchResult> getResults() { return results; }
    public void setResults(List<SearchResult> results) { this.results = results; }

    /**
     * 创建关键词建议
     */
    public static SearchSuggestion keyword(String text, double score) {
        SearchSuggestion suggestion = new SearchSuggestion();
        suggestion.setText(text);
        suggestion.setType("keyword");
        suggestion.setScore(score);
        return suggestion;
    }

    /**
     * 创建历史建议
     */
    public static SearchSuggestion history(String text) {
        SearchSuggestion suggestion = new SearchSuggestion();
        suggestion.setText(text);
        suggestion.setType("history");
        suggestion.setScore(1.0);
        return suggestion;
    }

    /**
     * 创建热门建议
     */
    public static SearchSuggestion hot(String text, Long count) {
        SearchSuggestion suggestion = new SearchSuggestion();
        suggestion.setText(text);
        suggestion.setType("hot");
        suggestion.setCount(count);
        return suggestion;
    }
}
