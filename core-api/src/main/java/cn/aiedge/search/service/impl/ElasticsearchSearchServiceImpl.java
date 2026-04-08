package cn.aiedge.search.service.impl;

import cn.aiedge.search.index.IndexManagementService;
import cn.aiedge.search.model.SearchRequest;
import cn.aiedge.search.model.SearchResponse;
import cn.aiedge.search.model.SearchResult;
import cn.aiedge.search.service.ElasticsearchSearchService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Elasticsearch 搜索服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(ElasticsearchClient.class)
public class ElasticsearchSearchServiceImpl implements ElasticsearchSearchService {

    private final ElasticsearchClient client;
    private final IndexManagementService indexManagementService;

    @Override
    public SearchResponse search(String indexType, SearchRequest request) {
        try {
            String indexName = indexManagementService.getFullIndexName(indexType, request.getTenantId());
            long startTime = System.currentTimeMillis();

            // 构建查询
            Query query = buildQuery(request);

            // 构建搜索请求
            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                    .index(indexName)
                    .query(query)
                    .from(request.getOffset())
                    .size(request.getPageSize())
                    .trackTotalHits(t -> t.enabled(request.isReturnTotal()));

            // 添加排序
            if (StringUtils.hasText(request.getSortBy())) {
                searchBuilder.sort(s -> s
                        .field(f -> f
                                .field(request.getSortBy())
                                .order("desc".equalsIgnoreCase(request.getSortOrder()) 
                                        ? co.elastic.clients.elasticsearch._types.SortOrder.Desc 
                                        : co.elastic.clients.elasticsearch._types.SortOrder.Asc)));
            }

            // 添加高亮
            if (request.isHighlight()) {
                searchBuilder.highlight(h -> h
                        .preTags(request.getHighlightPre())
                        .postTags(request.getHighlightPost())
                        .fields("name", f -> f)
                        .fields("title", f -> f)
                        .fields("content", f -> f));
            }

            // 执行搜索
            SearchResponse<Map> response = client.search(searchBuilder.build(), Map.class);

            // 解析结果
            List<SearchResult> results = parseSearchResults(response.hits().hits(), indexType);
            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            int totalPages = (int) Math.ceil((double) total / request.getPageSize());

            // 构建响应
            SearchResponse searchResponse = new SearchResponse();
            searchResponse.setKeyword(request.getKeyword());
            searchResponse.setType(indexType);
            searchResponse.setResults(results);
            searchResponse.setTotal(total);
            searchResponse.setPage(request.getPage());
            searchResponse.setPageSize(request.getPageSize());
            searchResponse.setTotalPages(totalPages);
            searchResponse.setTook(System.currentTimeMillis() - startTime);
            searchResponse.setHasMore(request.getPage() < totalPages);

            return searchResponse;
        } catch (IOException e) {
            log.error("搜索失败: indexType={}, keyword={}", indexType, request.getKeyword(), e);
            throw new RuntimeException("搜索失败: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchResponse multiSearch(List<String> indexTypes, SearchRequest request) {
        try {
            String[] indexNames = indexTypes.stream()
                    .map(type -> indexManagementService.getFullIndexName(type, request.getTenantId()))
                    .toArray(String[]::new);

            long startTime = System.currentTimeMillis();
            Query query = buildQuery(request);

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(Arrays.asList(indexNames))
                    .query(query)
                    .from(request.getOffset())
                    .size(request.getPageSize())
                    .trackTotalHits(t -> t.enabled(request.isReturnTotal())));

            SearchResponse<Map> response = client.search(searchRequest, Map.class);

            List<SearchResult> results = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                String index = hit.index();
                String indexType = extractIndexType(index);
                results.add(buildSearchResult(hit, indexType));
            }

            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            int totalPages = (int) Math.ceil((double) total / request.getPageSize());

            SearchResponse searchResponse = new SearchResponse();
            searchResponse.setKeyword(request.getKeyword());
            searchResponse.setType("multi");
            searchResponse.setResults(results);
            searchResponse.setTotal(total);
            searchResponse.setPage(request.getPage());
            searchResponse.setPageSize(request.getPageSize());
            searchResponse.setTotalPages(totalPages);
            searchResponse.setTook(System.currentTimeMillis() - startTime);
            searchResponse.setHasMore(request.getPage() < totalPages);

            return searchResponse;
        } catch (IOException e) {
            log.error("多索引搜索失败: indexTypes={}, keyword={}", indexTypes, request.getKeyword(), e);
            throw new RuntimeException("多索引搜索失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void indexDocument(String indexType, String id, Map<String, Object> document, Long tenantId) {
        try {
            String indexName = indexManagementService.getFullIndexName(indexType, tenantId);
            
            IndexRequest<Map> request = IndexRequest.of(i -> i
                    .index(indexName)
                    .id(id)
                    .document(document)
                    .refresh(Refresh.True));

            client.index(request);
            log.debug("索引文档成功: indexType={}, id={}", indexType, id);
        } catch (IOException e) {
            log.error("索引文档失败: indexType={}, id={}", indexType, id, e);
            throw new RuntimeException("索引文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void bulkIndex(String indexType, Map<String, Map<String, Object>> documents, Long tenantId) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        try {
            String indexName = indexManagementService.getFullIndexName(indexType, tenantId);

            List<BulkOperation> operations = documents.entrySet().stream()
                    .map(entry -> BulkOperation.of(b -> b
                            .index(i -> i
                                    .index(indexName)
                                    .id(entry.getKey())
                                    .document(entry.getValue()))))
                    .collect(Collectors.toList());

            BulkRequest request = BulkRequest.of(b -> b
                    .operations(operations)
                    .refresh(Refresh.True));

            BulkResponse response = client.bulk(request);

            if (response.errors()) {
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        log.error("批量索引失败: id={}, error={}", item.id(), item.error().reason());
                    }
                }
            }

            log.info("批量索引完成: indexType={}, count={}, errors={}", 
                    indexType, documents.size(), response.errors());
        } catch (IOException e) {
            log.error("批量索引失败: indexType={}", indexType, e);
            throw new RuntimeException("批量索引失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateDocument(String indexType, String id, Map<String, Object> document, Long tenantId) {
        try {
            String indexName = indexManagementService.getFullIndexName(indexType, tenantId);

            UpdateRequest<Map, Map> request = UpdateRequest.of(u -> u
                    .index(indexName)
                    .id(id)
                    .doc(document)
                    .docAsUpsert(true)
                    .refresh(Refresh.True));

            client.update(request, Map.class);
            log.debug("更新文档成功: indexType={}, id={}", indexType, id);
        } catch (IOException e) {
            log.error("更新文档失败: indexType={}, id={}", indexType, id, e);
            throw new RuntimeException("更新文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteDocument(String indexType, String id, Long tenantId) {
        try {
            String indexName = indexManagementService.getFullIndexName(indexType, tenantId);

            DeleteRequest request = DeleteRequest.of(d -> d
                    .index(indexName)
                    .id(id)
                    .refresh(Refresh.True));

            client.delete(request);
            log.debug("删除文档成功: indexType={}, id={}", indexType, id);
        } catch (IOException e) {
            log.error("删除文档失败: indexType={}, id={}", indexType, id, e);
            throw new RuntimeException("删除文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void bulkDelete(String indexType, List<String> ids, Long tenantId) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        try {
            String indexName = indexManagementService.getFullIndexName(indexType, tenantId);

            List<BulkOperation> operations = ids.stream()
                    .map(id -> BulkOperation.of(b -> b
                            .delete(d -> d
                                    .index(indexName)
                                    .id(id))))
                    .collect(Collectors.toList());

            BulkRequest request = BulkRequest.of(b -> b
                    .operations(operations)
                    .refresh(Refresh.True));

            client.bulk(request);
            log.info("批量删除完成: indexType={}, count={}", indexType, ids.size());
        } catch (IOException e) {
            log.error("批量删除失败: indexType={}", indexType, e);
            throw new RuntimeException("批量删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    public long deleteByQuery(String indexType, String field, Object value, Long tenantId) {
        try {
            String indexName = indexManagementService.getFullIndexName(indexType, tenantId);

            DeleteByQueryRequest request = DeleteByQueryRequest.of(d -> d
                    .index(indexName)
                    .query(q -> q
                            .term(TermQuery.of(t -> t
                                    .field(field)
                                    .value(FieldValue.of(value.toString())))))
                    .refresh(true));

            DeleteByQueryResponse response = client.deleteByQuery(request);
            log.info("查询删除完成: indexType={}, field={}, value={}, deleted={}", 
                    indexType, field, value, response.deleted());
            return response.deleted();
        } catch (IOException e) {
            log.error("查询删除失败: indexType={}, field={}, value={}", indexType, field, value, e);
            throw new RuntimeException("查询删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getDocument(String indexType, String id, Long tenantId) {
        try {
            String indexName = indexManagementService.getFullIndexName(indexType, tenantId);

            GetRequest request = GetRequest.of(g -> g
                    .index(indexName)
                    .id(id));

            GetResponse<Map> response = client.get(request, Map.class);
            return response.found() ? response.source() : null;
        } catch (IOException e) {
            log.error("获取文档失败: indexType={}, id={}", indexType, id, e);
            return null;
        }
    }

    @Override
    public List<Map<String, Object>> multiGet(String indexType, List<String> ids, Long tenantId) {
        try {
            String indexName = indexManagementService.getFullIndexName(indexType, tenantId);

            MgetRequest request = MgetRequest.of(m -> m
                    .index(indexName)
                    .ids(ids));

            MgetResponse<Map> response = client.mget(request, Map.class);
            
            return response.docs().stream()
                    .filter(d -> d.result().found())
                    .map(d -> d.result().source())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("批量获取文档失败: indexType={}", indexType, e);
            return Collections.emptyList();
        }
    }

    @Override
    public long count(String indexType, Long tenantId) {
        try {
            String indexName = indexManagementService.getFullIndexName(indexType, tenantId);

            CountRequest request = CountRequest.of(c -> c.index(indexName));
            CountResponse response = client.count(request);
            return response.count();
        } catch (IOException e) {
            log.error("统计文档数量失败: indexType={}", indexType, e);
            return 0;
        }
    }

    @Override
    public boolean exists(String indexType, String id, Long tenantId) {
        try {
            String indexName = indexManagementService.getFullIndexName(indexType, tenantId);

            ExistsRequest request = ExistsRequest.of(e -> e
                    .index(indexName)
                    .id(id));

            return client.exists(request).value();
        } catch (IOException e) {
            log.error("检查文档存在失败: indexType={}, id={}", indexType, id, e);
            return false;
        }
    }

    @Override
    public List<String> suggest(String indexType, String field, String prefix, int size, Long tenantId) {
        try {
            String indexName = indexManagementService.getFullIndexName(indexType, tenantId);

            SearchRequest request = SearchRequest.of(s -> s
                    .index(indexName)
                    .size(0)
                    .suggest(su -> su
                            .suggesters("suggest", sug -> sug
                                    .prefix(prefix)
                                    .completion(c -> c
                                            .field(field + ".suggest")
                                            .size(size)))));

            SearchResponse<Map> response = client.search(request, Map.class);
            
            return response.suggest().get("suggest").stream()
                    .flatMap(es -> es.completion().options().stream())
                    .map(opt -> opt.text())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("获取搜索建议失败: indexType={}, field={}", indexType, field, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Long> aggregate(String indexType, String field, int size, Long tenantId) {
        try {
            String indexName = indexManagementService.getFullIndexName(indexType, tenantId);

            SearchRequest request = SearchRequest.of(s -> s
                    .index(indexName)
                    .size(0)
                    .aggregations("agg", Aggregation.of(a -> a
                            .terms(TermsAggregation.of(t -> t
                                    .field(field)
                                    .size(size))))));

            SearchResponse<Map> response = client.search(request, Map.class);

            return response.aggregations().get("agg").sterms().buckets().array().stream()
                    .collect(Collectors.toMap(
                            b -> b.key().stringValue(),
                            b -> b.docCount(),
                            (v1, v2) -> v1,
                            LinkedHashMap::new));
        } catch (IOException e) {
            log.error("聚合查询失败: indexType={}, field={}", indexType, field, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public void refreshIndex(String indexType, Long tenantId) {
        try {
            String indexName = indexManagementService.getFullIndexName(indexType, tenantId);
            client.indices().refresh(r -> r.index(indexName));
            log.debug("刷新索引: {}", indexName);
        } catch (IOException e) {
            log.error("刷新索引失败: indexType={}", indexType, e);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 构建查询
     */
    private Query buildQuery(SearchRequest request) {
        String keyword = request.getKeyword();
        
        // 构建多字段匹配查询
        Query multiMatchQuery = Query.of(q -> q
                .multiMatch(m -> m
                        .query(keyword)
                        .fields(List.of("name^2", "title^2", "code^1.5", "content", "description"))
                        .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                        .fuzziness("AUTO")));

        // 构建布尔查询
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder()
                .must(multiMatchQuery);

        // 添加过滤条件
        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            for (Map.Entry<String, Object> filter : request.getFilters().entrySet()) {
                boolBuilder.filter(Query.of(q -> q
                        .term(TermQuery.of(t -> t
                                .field(filter.getKey())
                                .value(FieldValue.of(filter.getValue().toString()))))));
            }
        }

        // 添加租户过滤
        if (request.getTenantId() != null) {
            boolBuilder.filter(Query.of(q -> q
                    .term(TermQuery.of(t -> t
                            .field("tenantId")
                            .value(FieldValue.of(request.getTenantId()))))));
        }

        return Query.of(q -> q.bool(boolBuilder.build()));
    }

    /**
     * 解析搜索结果
     */
    private List<SearchResult> parseSearchResults(List<Hit<Map>> hits, String indexType) {
        return hits.stream()
                .map(hit -> buildSearchResult(hit, indexType))
                .collect(Collectors.toList());
    }

    /**
     * 构建搜索结果
     */
    @SuppressWarnings("unchecked")
    private SearchResult buildSearchResult(Hit<Map> hit, String indexType) {
        Map<String, Object> source = hit.source();
        if (source == null) {
            return null;
        }

        SearchResult result = new SearchResult();
        result.setId(Long.parseLong(hit.id()));
        result.setType(indexType);
        result.setScore(hit.score() != null ? hit.score() : 0.0);
        
        // 设置标题和描述
        Object name = source.get("name");
        if (name == null) {
            name = source.get("title");
        }
        result.setTitle(name != null ? name.toString() : "");
        
        Object desc = source.get("description");
        if (desc == null) {
            desc = source.get("content");
        }
        result.setDescription(desc != null ? desc.toString() : "");

        // 设置高亮
        if (hit.highlight() != null) {
            Map<String, List<String>> highlight = hit.highlight();
            if (highlight.containsKey("name")) {
                result.setHighlightedTitle(String.join("", highlight.get("name")));
            }
            if (highlight.containsKey("title")) {
                result.setHighlightedTitle(String.join("", highlight.get("title")));
            }
            if (highlight.containsKey("content")) {
                result.setHighlightededDescription(String.join("", highlight.get("content")));
            }
        }

        // 设置额外数据
        Map<String, Object> extra = new HashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (!List.of("id", "name", "title", "description", "content").contains(entry.getKey())) {
                extra.put(entry.getKey(), entry.getValue());
            }
        }
        result.setExtra(extra);

        return result;
    }

    /**
     * 从索引名提取索引类型
     */
    private String extractIndexType(String indexName) {
        String[] parts = indexName.split("_");
        if (parts.length >= 3) {
            return parts[1];
        }
        return indexName;
    }
}
