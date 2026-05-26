package cn.aiedge.search.service.impl;

import cn.aiedge.search.model.*;
import cn.aiedge.search.service.SemanticSearchService;
import cn.aiedge.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 语义搜索服务实现
 * 提供基于语义理解的智能搜索功能
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
public class SemanticSearchServiceImpl implements SemanticSearchService {

    @Autowired
    private SearchService searchService;

    @Override
    public SearchResponse semanticSearch(String query, Long tenantId, Integer limit) {
        // 纠错查询
        String correctedQuery = correctSpelling(query);
        
        // 获取查询意图
        String intent = getIntent(correctedQuery);
        
        // 提取实体
        List<String> entities = extractEntities(correctedQuery);
        
        // 查询扩展
        List<String> expandedQueries = expandQuery(correctedQuery);
        
        // 构建搜索请求
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setKeyword(expandedQueries.get(0));
        searchRequest.setType("all");
        searchRequest.setPage(1);
        searchRequest.setPageSize(limit);
        searchRequest.setHighlight(true);
        
        // 执行搜索
        SearchResponse response = searchService.search(searchRequest);
        
        // 对结果进行相关性排序
        List<SearchResult> rerankedResults = rerankResults(response.getResults(), correctedQuery);
        response.setResults(rerankedResults);
        
        return response;
    }

    @Override
    public String getIntent(String query) {
        // 简单意图识别
        if (query.contains("搜索") || query.contains("找") || query.contains("查询")) {
            return "SEARCH";
        } else if (query.contains("推荐") || query.contains("建议")) {
            return "RECOMMEND";
        } else if (query.contains("购买") || query.contains("买")) {
            return "PURCHASE";
        } else {
            return "GENERAL";
        }
    }

    @Override
    public List<String> extractEntities(String query) {
        // 简化实现：提取中文词语
        List<String> entities = new ArrayList<>();
        
        // 移除常用停用词
        String cleanQuery = query.replaceAll("的|了|和|与|是|在|有|没有|要|想", "");
        
        // 分词（简化版，实际应使用分词器）
        for (int i = 0; i < cleanQuery.length() - 1; i++) {
            String word = cleanQuery.substring(i, i + 2);
            if (word.matches("[\u4e00-\u9fa5]{2}")) {
                entities.add(word);
            }
        }
        
        return entities.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public List<String> expandQuery(String query) {
        List<String> expansions = new ArrayList<>();
        expansions.add(query);
        
        // 同义词扩展（简化版）
        Map<String, String[]> synonyms = Map.of(
            "手机", new String[]{"电话", "智能手机", "移动设备"},
            "电脑", new String[]{"计算机", "笔记本", "台式机"},
            "客户", new String[]{"客户公司", "客户单位"}
        );
        
        for (Map.Entry<String, String[]> entry : synonyms.entrySet()) {
            if (query.contains(entry.getKey())) {
                for (String synonym : entry.getValue()) {
                    expansions.add(query.replace(entry.getKey(), synonym));
                }
            }
        }
        
        return expansions;
    }

    @Override
    public double calculateSimilarity(String text1, String text2) {
        // 使用余弦相似度计算（简化版）
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    @Override
    public String correctSpelling(String query) {
        // 简化的纠错实现
        // 实际项目中应使用拼写纠错算法或API
        return query;
    }
    
    /**
     * 结果重排序
     */
    private List<SearchResult> rerankResults(List<SearchResult> results, String query) {
        // 根据语义相关性重新排序
        results.forEach(result -> {
            double semanticScore = calculateSimilarity(result.getTitle(), query);
            result.setScore(result.getScore() + semanticScore * 0.3);
        });
        
        return results.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());
    }
}
