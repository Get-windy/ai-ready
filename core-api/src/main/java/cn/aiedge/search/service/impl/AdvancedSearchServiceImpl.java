package cn.aiedge.search.service.impl;

import cn.aiedge.cache.service.CacheService;
import cn.aiedge.search.analyzer.SearchAnalyzer;
import cn.aiedge.search.config.SearchConfig;
import cn.aiedge.search.index.InvertedIndex;
import cn.aiedge.search.index.SearchQueryBuilder;
import cn.aiedge.search.model.*;
import cn.aiedge.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 高级搜索服务实现
 * 
 * 优化特性：
 * 1. 倒排索引 - O(1)查找复杂度
 * 2. 多种查询类型 - 布尔/范围/前缀/通配符/模糊/短语
 * 3. 分词支持 - 中英文智能分词
 * 4. 拼音搜索 - 支持拼音首字母和全拼
 * 5. 同义词扩展 - 搜索结果更全面
 * 6. 结果缓存 - 热门搜索缓存
 * 7. 聚合统计 - 搜索结果分析
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service("advancedSearchService")
@RequiredArgsConstructor
public class AdvancedSearchServiceImpl implements SearchService {

    private final CacheService cacheService;
    private final SearchConfig searchConfig;
    private final SearchAnalyzer searchAnalyzer;

    // 索引管理器
    private final Map<String, InvertedIndex> indexManager = new ConcurrentHashMap<>();

    // Redis Key 前缀
    private static final String SEARCH_INDEX_PREFIX = "search:index:";
    private static final String SEARCH_HISTORY_PREFIX = "search:history:";
    private static final String SEARCH_HOT_PREFIX = "search:hot:";
    private static final String SEARCH_SUGGESTION_PREFIX = "search:suggest:";
    private static final String SEARCH_CACHE_PREFIX = "search:cache:";
    private static final String SEARCH_PINYIN_PREFIX = "search:pinyin:";

    /**
     * 获取或创建索引
     */
    private InvertedIndex getOrCreateIndex(String type, Long tenantId) {
        String indexKey = type + ":" + tenantId;
        return indexManager.computeIfAbsent(indexKey, k -> {
            InvertedIndex index = new InvertedIndex(type);
            SearchConfig.IndexConfig config = searchConfig.getIndices().get(type);
            if (config != null) {
                index.setFieldWeights(config.getFieldWeights());
            }
            return index;
        });
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        String keyword = request.getKeyword().trim();
        String type = request.getType();

        log.debug("高级搜索请求: keyword={}, type={}, page={}, pageSize={}",
                keyword, type, request.getPage(), request.getPageSize());

        // 检查缓存
        String cacheKey = buildCacheKey(request);
        SearchResponse cachedResponse = getCachedResponse(cacheKey);
        if (cachedResponse != null) {
            log.debug("命中搜索缓存: key={}", cacheKey);
            return cachedResponse;
        }

        // 分析搜索词
        SearchAnalyzer.AnalysisResult analysis = searchAnalyzer.analyze(
                keyword, SearchAnalyzer.AnalyzeOptions.FULL);

        // 构建查询
        List<SearchResult> allResults = new ArrayList<>();

        for (String searchType : request.getSearchTypes()) {
            InvertedIndex index = getOrCreateIndex(searchType, request.getTenantId());

            // 构建布尔查询：原始词 OR 拼音 OR 同义词
            SearchQueryBuilder.BoolQuery boolQuery = SearchQueryBuilder.bool();

            // 原始词匹配
            boolQuery.should(SearchQueryBuilder.match("_all", keyword).boost(2.0));

            // 拼音匹配
            if (analysis.getPinyin() != null && !analysis.getPinyin().isEmpty()) {
                for (String pinyin : analysis.getPinyin().values()) {
                    boolQuery.should(SearchQueryBuilder.match("_all", pinyin).boost(0.8));
                }
            }

            // 同义词匹配
            if (analysis.getSynonyms() != null && !analysis.getSynonyms().isEmpty()) {
                for (List<String> synonyms : analysis.getSynonyms().values()) {
                    for (String synonym : synonyms) {
                        boolQuery.should(SearchQueryBuilder.match("_all", synonym).boost(0.5));
                    }
                }
            }

            // 执行查询
            Map<Long, Double> searchResults = boolQuery.execute(index);

            // 构建结果
            for (Map.Entry<Long, Double> entry : searchResults.entrySet()) {
                Map<String, Object> doc = index.getDocument(entry.getKey());
                if (doc != null) {
                    SearchResult result = buildSearchResult(searchType, entry.getKey(), doc, entry.getValue());
                    if (result != null) {
                        allResults.add(result);
                    }
                }
            }
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

        // 缓存结果
        cacheResponse(cacheKey, response);

        log.info("搜索完成: keyword={}, results={}, took={}ms",
                keyword, total, response.getTook());

        return response;
    }

    /**
     * 高级搜索（支持复杂查询）
     */
    public SearchResponse advancedSearch(AdvancedSearchRequest request) {
        long startTime = System.currentTimeMillis();

        log.debug("高级搜索: query={}", request.getQuery());

        List<SearchResult> allResults = new ArrayList<>();

        for (String searchType : request.getSearchTypes()) {
            InvertedIndex index = getOrCreateIndex(searchType, request.getTenantId());

            // 根据查询类型构建查询
            SearchQueryBuilder.Query query = buildQuery(request);

            // 执行查询
            Map<Long, Double> searchResults = query.execute(index);

            // 应用过滤条件
            if (request.getFilters() != null && !request.getFilters().isEmpty()) {
                searchResults = applyFilters(searchResults, index, request.getFilters());
            }

            // 构建结果
            for (Map.Entry<Long, Double> entry : searchResults.entrySet()) {
                Map<String, Object> doc = index.getDocument(entry.getKey());
                if (doc != null) {
                    SearchResult result = buildSearchResult(searchType, entry.getKey(), doc, entry.getValue());
                    if (result != null) {
                        allResults.add(result);
                    }
                }
            }
        }

        // 排序
        applySort(allResults, request.getSortBy(), request.getSortOrder());

        // 分页
        int total = allResults.size();
        int totalPages = (int) Math.ceil((double) total / request.getPageSize());

        List<SearchResult> pagedResults = paginate(allResults,
                request.getOffset(), request.getPageSize());

        // 构建响应
        SearchResponse response = new SearchResponse();
        response.setKeyword(request.getQuery());
        response.setType(request.getType());
        response.setResults(pagedResults);
        response.setTotal((long) total);
        response.setPage(request.getPage());
        response.setPageSize(request.getPageSize());
        response.setTotalPages(totalPages);
        response.setTook(System.currentTimeMillis() - startTime);
        response.setHasMore(request.getPage() < totalPages);

        return response;
    }

    /**
     * 聚合搜索
     */
    public SearchAggregationResponse searchWithAggregation(SearchRequest request) {
        SearchResponse searchResponse = search(request);

        SearchAggregationResponse response = new SearchAggregationResponse();
        response.setSearchResponse(searchResponse);

        // 按类型聚合
        Map<String, Long> typeCounts = new HashMap<>();
        for (SearchResult result : searchResponse.getResults()) {
            typeCounts.merge(result.getType(), 1L, Long::sum);
        }
        response.setTypeAggregation(typeCounts);

        // 热门搜索词
        response.setHotKeywords(getHotSearches(10, request.getTenantId()));

        // 相关推荐（基于同义词）
        SearchAnalyzer.AnalysisResult analysis = searchAnalyzer.analyze(
                request.getKeyword(), SearchAnalyzer.AnalyzeOptions.DEFAULT);
        if (analysis.getSynonyms() != null && !analysis.getSynonyms().isEmpty()) {
            List<String> relatedKeywords = analysis.getSynonyms().values().stream()
                    .flatMap(List::stream)
                    .distinct()
                    .limit(5)
                    .collect(Collectors.toList());
            response.setRelatedKeywords(relatedKeywords);
        }

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

    private List<SearchResult> searchByType(String type, String keyword, int pageSize, Long tenantId) {
        InvertedIndex index = getOrCreateIndex(type, tenantId);

        SearchQueryBuilder.MatchQuery query = SearchQueryBuilder.match("_all", keyword);
        Map<Long, Double> results = query.execute(index);

        return results.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(pageSize)
                .map(entry -> {
                    Map<String, Object> doc = index.getDocument(entry.getKey());
                    return doc != null ? buildSearchResult(type, entry.getKey(), doc, entry.getValue()) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<SearchSuggestion> getSuggestions(String prefix, int limit, Long userId, Long tenantId) {
        if (!StringUtils.hasText(prefix) || prefix.length() < searchConfig.getSuggestionMinChars()) {
            return Collections.emptyList();
        }

        List<SearchSuggestion> suggestions = new ArrayList<>();

        // 1. 从搜索历史获取
        if (userId != null) {
            List<SearchHistory> histories = getSearchHistory(userId, limit, tenantId);
            for (SearchHistory history : histories) {
                if (history.getKeyword().toLowerCase().startsWith(prefix.toLowerCase())) {
                    suggestions.add(SearchSuggestion.history(history.getKeyword()));
                }
            }
        }

        // 2. 从热门搜索获取
        List<String> hotSearches = getHotSearches(limit * 2, tenantId);
        for (String hot : hotSearches) {
            if (hot.toLowerCase().startsWith(prefix.toLowerCase()) &&
                    suggestions.stream().noneMatch(s -> s.getText().equalsIgnoreCase(hot))) {
                suggestions.add(SearchSuggestion.hot(hot, null));
            }
        }

        // 3. 前缀搜索（从索引）
        for (String type : Arrays.asList("customer", "product", "order")) {
            InvertedIndex index = getOrCreateIndex(type, tenantId);
            Map<Long, Double> prefixResults = index.searchPrefix(prefix.toLowerCase());

            // 从结果中提取词条
            for (Long docId : prefixResults.keySet()) {
                Map<String, Object> doc = index.getDocument(docId);
                if (doc != null) {
                    for (Object value : doc.values()) {
                        if (value != null && value.toString().toLowerCase().startsWith(prefix.toLowerCase())) {
                            String text = value.toString();
                            if (suggestions.stream().noneMatch(s -> s.getText().equalsIgnoreCase(text))) {
                                suggestions.add(SearchSuggestion.keyword(text, 0.5));
                            }
                        }
                    }
                }
            }
        }

        // 4. 拼音搜索建议
        SearchAnalyzer.PinyinConverter pinyinConverter = new SearchAnalyzer.PinyinConverter();
        String pinyinPrefix = pinyinConverter.toPinyinFirstLetter(prefix);
        if (!pinyinPrefix.isEmpty()) {
            // 从拼音索引获取
            String pinyinKey = SEARCH_PINYIN_PREFIX + tenantId + ":" + pinyinPrefix.charAt(0);
            Set<Object> pinyinSet = cacheService.sMembers(pinyinKey);
            if (pinyinSet != null) {
                for (Object obj : pinyinSet) {
                    String word = obj.toString();
                    if (word.toLowerCase().startsWith(prefix.toLowerCase()) &&
                            suggestions.stream().noneMatch(s -> s.getText().equalsIgnoreCase(word))) {
                        suggestions.add(SearchSuggestion.keyword(word, 0.3));
                    }
                }
            }
        }

        return suggestions.stream()
                .limit(limit > 0 ? limit : searchConfig.getSuggestionMaxCount())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getHotSearches(int limit, Long tenantId) {
        String hotKey = SEARCH_HOT_PREFIX + getTodayKey();

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
        String historyKey = SEARCH_HISTORY_PREFIX + userId;
        List<Object> historyList = cacheService.lRange(historyKey, 0, -1);
        if (historyList != null && historyId > 0 && historyId <= historyList.size()) {
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

        // 更新拼音索引
        SearchAnalyzer.PinyinConverter pinyinConverter = new SearchAnalyzer.PinyinConverter();
        String pinyin = pinyinConverter.toPinyin(keyword);
        if (!pinyin.isEmpty()) {
            String pinyinKey = SEARCH_PINYIN_PREFIX + tenantId + ":" + keyword.toLowerCase().charAt(0);
            cacheService.sAdd(pinyinKey, keyword.toLowerCase());
        }
    }

    @Override
    public void indexCustomer(Long customerId, Object data, Long tenantId) {
        indexDocument("customer", customerId, data, tenantId);
    }

    @Override
    public void indexProduct(Long productId, Object data, Long tenantId) {
        indexDocument("product", productId, data, tenantId);
    }

    @Override
    public void indexOrder(Long orderId, Object data, Long tenantId) {
        indexDocument("order", orderId, data, tenantId);
    }

    @SuppressWarnings("unchecked")
    private void indexDocument(String type, Long id, Object data, Long tenantId) {
        InvertedIndex index = getOrCreateIndex(type, tenantId);

        Map<String, Object> document = new HashMap<>();
        if (data instanceof Map) {
            document = (Map<String, Object>) data;
        }

        SearchConfig.IndexConfig config = searchConfig.getIndices().get(type);
        List<String> fieldsToIndex = config != null ? config.getSearchFields() : Collections.emptyList();

        index.addDocument(id, document, fieldsToIndex);
        log.debug("索引文档: type={}, id={}", type, id);
    }

    @Override
    public void deleteCustomerIndex(Long customerId, Long tenantId) {
        deleteDocument("customer", customerId, tenantId);
    }

    @Override
    public void deleteProductIndex(Long productId, Long tenantId) {
        deleteDocument("product", productId, tenantId);
    }

    @Override
    public void deleteOrderIndex(Long orderId, Long tenantId) {
        deleteDocument("order", orderId, tenantId);
    }

    private void deleteDocument(String type, Long id, Long tenantId) {
        InvertedIndex index = getOrCreateIndex(type, tenantId);
        index.removeDocument(id);
        log.debug("删除索引: type={}, id={}", type, id);
    }

    @Override
    public long rebuildIndex(String type, Long tenantId) {
        log.info("重建索引: type={}, tenantId={}", type, tenantId);
        InvertedIndex index = getOrCreateIndex(type, tenantId);
        index.clear();
        // 实际实现需要从数据库加载数据
        return 0;
    }

    /**
     * 获取索引统计信息
     */
    public Map<String, Object> getIndexStats(Long tenantId) {
        Map<String, Object> stats = new HashMap<>();

        for (String type : Arrays.asList("customer", "product", "order")) {
            InvertedIndex index = getOrCreateIndex(type, tenantId);
            stats.put(type, index.getStats());
        }

        return stats;
    }

    // ==================== 私有方法 ====================

    private String buildCacheKey(SearchRequest request) {
        return SEARCH_CACHE_PREFIX + request.getKeyword().hashCode() + ":" +
                request.getType() + ":" + request.getPage() + ":" + request.getPageSize();
    }

    private SearchResponse getCachedResponse(String cacheKey) {
        try {
            Object cached = cacheService.get(cacheKey);
            if (cached instanceof SearchResponse) {
                return (SearchResponse) cached;
            }
        } catch (Exception e) {
            log.debug("获取缓存失败: {}", e.getMessage());
        }
        return null;
    }

    private void cacheResponse(String cacheKey, SearchResponse response) {
        try {
            cacheService.set(cacheKey, response, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.debug("缓存结果失败: {}", e.getMessage());
        }
    }

    private SearchQueryBuilder.Query buildQuery(AdvancedSearchRequest request) {
        if (request.getQueryType() == null) {
            return SearchQueryBuilder.match("_all", request.getQuery());
        }

        return switch (request.getQueryType()) {
            case TERM -> SearchQueryBuilder.term("_all", request.getQuery());
            case PHRASE -> SearchQueryBuilder.phrase("_all", request.getQuery());
            case PREFIX -> SearchQueryBuilder.prefix("_all", request.getQuery());
            case WILDCARD -> SearchQueryBuilder.wildcard("_all", request.getQuery());
            case FUZZY -> SearchQueryBuilder.fuzzy("_all", request.getQuery());
            default -> SearchQueryBuilder.match("_all", request.getQuery());
        };
    }

    private Map<Long, Double> applyFilters(Map<Long, Double> results, InvertedIndex index,
                                            Map<String, Object> filters) {
        Map<Long, Double> filtered = new HashMap<>();

        for (Map.Entry<Long, Double> entry : results.entrySet()) {
            Map<String, Object> doc = index.getDocument(entry.getKey());
            if (doc != null && matchesFilters(doc, filters)) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }

        return filtered;
    }

    private boolean matchesFilters(Map<String, Object> doc, Map<String, Object> filters) {
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            Object value = doc.get(filter.getKey());
            Object expected = filter.getValue();

            if (value == null) return false;

            if (expected instanceof Map) {
                // 范围过滤
                Map<String, Object> range = (Map<String, Object>) expected;
                if (range.containsKey("gte") && compareValues(value, range.get("gte")) < 0) return false;
                if (range.containsKey("gt") && compareValues(value, range.get("gt")) <= 0) return false;
                if (range.containsKey("lte") && compareValues(value, range.get("lte")) > 0) return false;
                if (range.containsKey("lt") && compareValues(value, range.get("lt")) >= 0) return false;
            } else if (expected instanceof Collection) {
                // 多值过滤
                Collection<?> expectedValues = (Collection<?>) expected;
                if (!expectedValues.contains(value)) return false;
            } else {
                // 精确匹配
                if (!expected.equals(value)) return false;
            }
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compareValues(Object v1, Object v2) {
        if (v1 instanceof Comparable && v2 instanceof Comparable) {
            return ((Comparable) v1).compareTo(v2);
        }
        return v1.toString().compareTo(v2.toString());
    }

    private void applySort(List<SearchResult> results, String sortBy, String sortOrder) {
        if (sortBy == null || sortBy.isEmpty()) {
            return;
        }

        Comparator<SearchResult> comparator = switch (sortBy) {
            case "score" -> Comparator.comparingDouble(SearchResult::getScore);
            case "createTime" -> Comparator.comparing(SearchResult::getCreateTime,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparingDouble(SearchResult::getScore);
        };

        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }

        results.sort(comparator);
    }

    private List<SearchResult> paginate(List<SearchResult> results, int offset, int pageSize) {
        int fromIndex = offset;
        int toIndex = Math.min(fromIndex + pageSize, results.size());

        return fromIndex < results.size()
                ? results.subList(fromIndex, toIndex)
                : Collections.emptyList();
    }

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

    private List<SearchResult> highlightResults(List<SearchResult> results, String keyword,
                                                String highlightPre, String highlightPost) {
        for (SearchResult result : results) {
            if (result.getTitle() != null && result.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
                result.setHighlightedTitle(highlightText(result.getTitle(), keyword, highlightPre, highlightPost));
            }
            if (result.getDescription() != null && result.getDescription().toLowerCase().contains(keyword.toLowerCase())) {
                result.setHighlightededDescription(highlightText(result.getDescription(), keyword, highlightPre, highlightPost));
            }
        }
        return results;
    }

    private String highlightText(String text, String keyword, String pre, String post) {
        if (text == null) return null;
        return text.replaceAll("(?i)(" + escapeRegex(keyword) + ")", pre + "$1" + post);
    }

    private String escapeRegex(String str) {
        return str.replaceAll("([\\[\\]\\(\\)\\{\\}\\*\\+\\?\\.\\\\\\^\\$\\|])", "\\\\$1");
    }

    private String getTodayKey() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private String getString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
