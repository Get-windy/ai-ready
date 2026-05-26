package cn.aiedge.search.service.impl;

import cn.aiedge.cache.service.CacheService;
import cn.aiedge.search.config.SearchConfig;
import cn.aiedge.search.model.*;
import cn.aiedge.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 搜索服务实现
 * 
 * 基于Redis实现搜索功能：
 * - 使用ZSet存储搜索词和热度
 * - 使用Set存储用户搜索历史
 * - 使用Hash存储搜索索引
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final CacheService cacheService;
    private final SearchConfig searchConfig;

    // Redis Key 前缀
    private static final String SEARCH_INDEX_PREFIX = "search:index:";
    private static final String SEARCH_HISTORY_PREFIX = "search:history:";
    private static final String SEARCH_HOT_PREFIX = "search:hot:";
    private static final String SEARCH_SUGGESTION_PREFIX = "search:suggest:";

    @Override
    public SearchResponse search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        String keyword = request.getKeyword().trim().toLowerCase();
        String type = request.getType();

        log.debug("搜索请求: keyword={}, type={}, page={}, pageSize={}", 
                keyword, type, request.getPage(), request.getPageSize());

        List<SearchResult> allResults = new ArrayList<>();

        // 根据类型搜索
        for (String searchType : request.getSearchTypes()) {
            List<SearchResult> typeResults = searchByType(searchType, keyword, 
                    request.getPageSize() * 2, request.getTenantId());
            allResults.addAll(typeResults);
        }

        // 按分数排序
        allResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // 分页
        int total = allResults.size();
        int totalPages = (int) Math.ceil((double) total / request.getPageSize());
        int fromIndex = request.getOffset();
        int toIndex = Math.min(fromIndex + request.getPageSize(), total);

        List<SearchResult> pagedResults = fromIndex < total 
                ? allResults.subList(fromIndex, toIndex) 
                : Collections.emptyList();

        // 高亮处理
        if (request.isHighlight()) {
            pagedResults = highlightResults(pagedResults, keyword, 
                    request.getHighlightPre(), request.getHighlightPost());
        }

        // 记录搜索历史
        if (request.getUserId() != null) {
            recordSearchHistory(request.getUserId(), keyword, type, total, request.getTenantId());
        }

        // 构建响应
        SearchResponse response = new SearchResponse();
        response.setKeyword(keyword);
        response.setType(type);
        response.setResults(pagedResults);
        response.setTotal((long) total);
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());
        response.setTotalPages(totalPages);
        response.setTook(System.currentTimeMillis() - startTime);
        response.setHasMore(request.getPage() < totalPages);

        return response;
    }

    @Override
    public List<SearchResult> searchCustomer(String keyword, int page, int pageSize, Long tenantId) {
        return searchByType("customer", keyword, pageSize, tenantId);
    }

    @Override
    public List<SearchResult> searchProduct(String keyword, int page, int pageSize, Long tenantId) {
        return searchByType("product", keyword, pageSize, tenantId);
    }

    @Override
    public List<SearchResult> searchOrder(String keyword, int page, int pageSize, Long tenantId) {
        return searchByType("order", keyword, pageSize, tenantId);
    }

    @Override
    public List<SearchSuggestion> getSuggestions(String prefix, int limit, Long userId, Long tenantId) {
        if (!StringUtils.hasText(prefix) || prefix.length() < searchConfig.getSuggestionMinChars()) {
            return Collections.emptyList();
        }

        List<SearchSuggestion> suggestions = new ArrayList<>();
        String lowerPrefix = prefix.toLowerCase().trim();

        // 1. 从搜索历史获取建议
        if (userId != null) {
            List<SearchHistory> histories = getSearchHistory(userId, limit, tenantId);
            for (SearchHistory history : histories) {
                if (history.getKeyword().toLowerCase().startsWith(lowerPrefix)) {
                    suggestions.add(SearchSuggestion.history(history.getKeyword()));
                }
            }
        }

        // 2. 从热门搜索获取建议
        List<String> hotSearches = getHotSearches(limit * 2, tenantId);
        for (String hot : hotSearches) {
            if (hot.toLowerCase().startsWith(lowerPrefix) && 
                suggestions.stream().noneMatch(s -> s.getText().equalsIgnoreCase(hot))) {
                suggestions.add(SearchSuggestion.hot(hot, null));
            }
        }

        // 3. 从建议索引获取
        String suggestKey = SEARCH_SUGGESTION_PREFIX + lowerPrefix.charAt(0);
        Set<Object> matched = cacheService.sMembers(suggestKey);
        if (matched != null) {
            for (Object obj : matched) {
                String word = obj.toString();
                if (word.toLowerCase().startsWith(lowerPrefix) &&
                    suggestions.stream().noneMatch(s -> s.getText().equalsIgnoreCase(word))) {
                    suggestions.add(SearchSuggestion.keyword(word, 0.5));
                }
            }
        }

        // 排序并限制数量
        return suggestions.stream()
                .limit(limit > 0 ? limit : searchConfig.getSuggestionMaxCount())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getHotSearches(int limit, Long tenantId) {
        String hotKey = SEARCH_HOT_PREFIX + getTodayKey();
        
        // 从ZSet获取热门搜索（按分数降序）
        Set<Object> hotSet = cacheService.zReverseRange(hotKey, 0, limit - 1);
        if (hotSet == null || hotSet.isEmpty()) {
            return Collections.emptyList();
        }

        return hotSet.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    @Override
    public List<SearchHistory> getSearchHistory(Long userId, int limit, Long tenantId) {
        String historyKey = SEARCH_HISTORY_PREFIX + userId;
        
        // 从List获取搜索历史（最近的在前）
        List<Object> historyList = cacheService.lRange(historyKey, 0, limit - 1);
        if (historyList == null || historyList.isEmpty()) {
            return Collections.emptyList();
        }

        List<SearchHistory> histories = new ArrayList<>();
        long id = 1;
        for (Object obj : historyList) {
            String[] parts = obj.toString().split("\\|");
            if (parts.length >= 3) {
                SearchHistory history = new SearchHistory();
                history.setId(id++);
                history.setUserId(userId);
                history.setKeyword(parts[0]);
                history.setSearchType(parts[1]);
                history.setResultCount(Integer.parseInt(parts[2]));
                history.setSearchTime(LocalDateTime.parse(parts[parts.length - 1], 
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                history.setTenantId(tenantId);
                histories.add(history);
            }
        }

        return histories;
    }

    @Override
    public boolean clearSearchHistory(Long userId, Long tenantId) {
        String historyKey = SEARCH_HISTORY_PREFIX + userId;
        cacheService.delete(historyKey);
        log.info("清空用户搜索历史: userId={}", userId);
        return true;
    }

    @Override
    public boolean deleteSearchHistory(Long historyId, Long userId, Long tenantId) {
        // 简化实现：删除指定位置的历史记录
        String historyKey = SEARCH_HISTORY_PREFIX + userId;
        List<Object> historyList = cacheService.lRange(historyKey, 0, -1);
        if (historyList != null && historyId > 0 && historyId <= historyList.size()) {
            // 重建列表（跳过删除项）
            List<Object> newList = new ArrayList<>();
            for (int i = 0; i < historyList.size(); i++) {
                if (i != historyId - 1) {
                    newList.add(historyList.get(i));
                }
            }
            cacheService.delete(historyKey);
            for (Object item : newList) {
                cacheService.rPush(historyKey, item);
            }
            return true;
        }
        return false;
    }

    @Override
    public void recordSearchHistory(Long userId, String keyword, String searchType, 
            Integer resultCount, Long tenantId) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }

        // 记录到用户历史
        String historyKey = SEARCH_HISTORY_PREFIX + userId;
        String historyValue = String.format("%s|%s|%d|%s", 
                keyword, searchType, resultCount, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        cacheService.lPush(historyKey, historyValue);
        cacheService.expire(historyKey, searchConfig.getHistoryRetentionDays(), TimeUnit.DAYS);

        // 更新热门搜索
        String hotKey = SEARCH_HOT_PREFIX + getTodayKey();
        cacheService.zAdd(hotKey, keyword.toLowerCase(), 1);
        cacheService.expire(hotKey, searchConfig.getHotSearchPeriod() + 1, TimeUnit.DAYS);

        // 更新建议索引
        String suggestKey = SEARCH_SUGGESTION_PREFIX + keyword.toLowerCase().charAt(0);
        cacheService.sAdd(suggestKey, keyword.toLowerCase());
    }

    @Override
    public void indexCustomer(Long customerId, Object data, Long tenantId) {
        String indexKey = SEARCH_INDEX_PREFIX + "customer:" + tenantId;
        Map<String, Object> customerMap = convertToMap(data);
        if (customerMap != null) {
            cacheService.hSet(indexKey, customerId.toString(), customerMap);
        }
    }

    @Override
    public void indexProduct(Long productId, Object data, Long tenantId) {
        String indexKey = SEARCH_INDEX_PREFIX + "product:" + tenantId;
        Map<String, Object> productMap = convertToMap(data);
        if (productMap != null) {
            cacheService.hSet(indexKey, productId.toString(), productMap);
        }
    }

    @Override
    public void indexOrder(Long orderId, Object data, Long tenantId) {
        String indexKey = SEARCH_INDEX_PREFIX + "order:" + tenantId;
        Map<String, Object> orderMap = convertToMap(data);
        if (orderMap != null) {
            cacheService.hSet(indexKey, orderId.toString(), orderMap);
        }
    }

    @Override
    public void deleteCustomerIndex(Long customerId, Long tenantId) {
        String indexKey = SEARCH_INDEX_PREFIX + "customer:" + tenantId;
        cacheService.hDelete(indexKey, customerId.toString());
    }

    @Override
    public void deleteProductIndex(Long productId, Long tenantId) {
        String indexKey = SEARCH_INDEX_PREFIX + "product:" + tenantId;
        cacheService.hDelete(indexKey, productId.toString());
    }

    @Override
    public void deleteOrderIndex(Long orderId, Long tenantId) {
        String indexKey = SEARCH_INDEX_PREFIX + "order:" + tenantId;
        cacheService.hDelete(indexKey, orderId.toString());
    }

    @Override
    public long rebuildIndex(String type, Long tenantId) {
        log.info("重建索引: type={}, tenantId={}", type, tenantId);
        // 实际实现需要从数据库加载数据并重建索引
        // 这里返回0表示需要子类实现
        return 0;
    }

    // ==================== 私有方法 ====================

    /**
     * 根据类型搜索
     */
    private List<SearchResult> searchByType(String type, String keyword, int limit, Long tenantId) {
        String indexKey = SEARCH_INDEX_PREFIX + type + ":" + tenantId;
        Map<Object, Object> indexData = cacheService.hGetAll(indexKey);
        
        if (indexData == null || indexData.isEmpty()) {
            return Collections.emptyList();
        }

        List<SearchResult> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        for (Map.Entry<Object, Object> entry : indexData.entrySet()) {
            try {
                Long id = Long.parseLong(entry.getKey().toString());
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) entry.getValue();
                
                double score = calculateMatchScore(data, lowerKeyword, type);
                if (score > 0) {
                    SearchResult result = buildSearchResult(type, id, data, score);
                    if (result != null) {
                        results.add(result);
                    }
                }
            } catch (Exception e) {
                log.warn("解析搜索结果失败: {}", entry.getKey(), e);
            }
        }

        // 排序并限制数量
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return results.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 计算匹配分数
     */
    private double calculateMatchScore(Map<String, Object> data, String keyword, String type) {
        double maxScore = 0;
        
        // 获取字段权重配置
        SearchConfig.IndexConfig indexConfig = searchConfig.getIndices().get(type);
        Map<String, Double> fieldWeights = indexConfig != null ? indexConfig.getFieldWeights() : Collections.emptyMap();

        for (Map.Entry<String, Object> field : data.entrySet()) {
            String fieldName = field.getKey();
            Object value = field.getValue();
            
            if (value == null) continue;

            String strValue = value.toString().toLowerCase();
            double weight = fieldWeights.getOrDefault(fieldName, 1.0);

            // 完全匹配
            if (strValue.equals(keyword)) {
                maxScore = Math.max(maxScore, 3.0 * weight);
            }
            // 前缀匹配
            else if (strValue.startsWith(keyword)) {
                maxScore = Math.max(maxScore, 2.0 * weight);
            }
            // 包含匹配
            else if (strValue.contains(keyword)) {
                maxScore = Math.max(maxScore, 1.0 * weight);
            }
        }

        return maxScore;
    }

    /**
     * 构建搜索结果
     */
    private SearchResult buildSearchResult(String type, Long id, Map<String, Object> data, double score) {
        switch (type) {
            case "customer":
                return SearchResult.fromCustomer(
                        id,
                        getString(data, "name", ""),
                        getString(data, "code", ""),
                        getString(data, "contact", ""),
                        score
                );
            case "product":
                return SearchResult.fromProduct(
                        id,
                        getString(data, "name", ""),
                        getString(data, "code", ""),
                        getString(data, "category", ""),
                        score
                );
            case "order":
                return SearchResult.fromOrder(
                        id,
                        getString(data, "orderNo", ""),
                        getString(data, "customerName", ""),
                        getString(data, "status", ""),
                        score
                );
            default:
                return null;
        }
    }

    /**
     * 高亮处理
     */
    private List<SearchResult> highlightResults(List<SearchResult> results, String keyword,
            String highlightPre, String highlightPost) {
        for (SearchResult result : results) {
            if (result.getTitle() != null && result.getTitle().toLowerCase().contains(keyword)) {
                result.setHighlightedTitle(highlightText(result.getTitle(), keyword, highlightPre, highlightPost));
            }
            if (result.getDescription() != null && result.getDescription().toLowerCase().contains(keyword)) {
                result.setHighlightededDescription(highlightText(result.getDescription(), keyword, highlightPre, highlightPost));
            }
        }
        return results;
    }

    /**
     * 高亮文本
     */
    private String highlightText(String text, String keyword, String pre, String post) {
        if (text == null) return null;
        return text.replaceAll("(?i)(" + escapeRegex(keyword) + ")", pre + "$1" + post);
    }

    /**
     * 转义正则特殊字符
     */
    private String escapeRegex(String str) {
        return str.replaceAll("([\\[\\]\\(\\)\\{\\}\\*\\+\\?\\.\\\\\\^\\$\\|])", "\\\\$1");
    }

    /**
     * 获取今日Key
     */
    private String getTodayKey() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 转换为Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object data) {
        if (data == null) return null;
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        // 其他类型需要通过反射或其他方式转换
        return null;
    }

    /**
     * 从Map获取字符串值
     */
    private String getString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
