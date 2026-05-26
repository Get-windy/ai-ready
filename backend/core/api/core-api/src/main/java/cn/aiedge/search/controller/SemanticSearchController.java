package cn.aiedge.search.controller;

import cn.aiedge.search.model.SearchResponse;
import cn.aiedge.search.service.SemanticSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 语义搜索控制器
 * 提供语义搜索相关的 REST API 接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/search/semantic")
public class SemanticSearchController {

    @Autowired
    private SemanticSearchService semanticSearchService;

    /**
     * 语义搜索
     */
    @GetMapping
    public SearchResponse semanticSearch(
            @RequestParam String query,
            @RequestParam Long tenantId,
            @RequestParam(defaultValue = "10") Integer limit) {
        return semanticSearchService.semanticSearch(query, tenantId, limit);
    }

    /**
     * 获取查询意图
     */
    @GetMapping("/intent")
    public String getIntent(@RequestParam String query) {
        return semanticSearchService.getIntent(query);
    }

    /**
     * 提取关键实体
     */
    @GetMapping("/entities")
    public List<String> extractEntities(@RequestParam String query) {
        return semanticSearchService.extractEntities(query);
    }

    /**
     * 查询扩展
     */
    @GetMapping("/expand")
    public List<String> expandQuery(@RequestParam String query) {
        return semanticSearchService.expandQuery(query);
    }

    /**
     * 相似度计算
     */
    @PostMapping("/similarity")
    public Map<String, Double> calculateSimilarity(@RequestBody Map<String, String> request) {
        String text1 = request.get("text1");
        String text2 = request.get("text2");
        double similarity = semanticSearchService.calculateSimilarity(text1, text2);
        return Map.of("similarity", similarity);
    }

    /**
     * 拼写纠错
     */
    @GetMapping("/correct")
    public Map<String, String> correctSpelling(@RequestParam String query) {
        String corrected = semanticSearchService.correctSpelling(query);
        return Map.of("original", query, "corrected", corrected);
    }
}
