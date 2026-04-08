package cn.aiedge.search.index;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Elasticsearch 索引管理服务
 * 
 * 提供索引的创建、删除、更新等管理功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(ElasticsearchClient.class)
public class IndexManagementService {

    private final ElasticsearchClient client;
    private final String indexPrefix = "ai_ready";

    // ==================== 索引名称常量 ====================

    /** 客户索引 */
    public static final String INDEX_CUSTOMER = "customer";

    /** 产品索引 */
    public static final String INDEX_PRODUCT = "product";

    /** 订单索引 */
    public static final String INDEX_ORDER = "order";

    /** 供应商索引 */
    public static final String INDEX_SUPPLIER = "supplier";

    /** 合同索引 */
    public static final String INDEX_CONTRACT = "contract";

    /** 知识库索引 */
    public static final String INDEX_KNOWLEDGE = "knowledge";

    // ==================== 索引创建方法 ====================

    /**
     * 创建所有业务索引
     */
    public void createAllIndices(Long tenantId) throws IOException {
        log.info("创建所有业务索引, tenantId={}", tenantId);
        
        createCustomerIndex(tenantId);
        createProductIndex(tenantId);
        createOrderIndex(tenantId);
        createSupplierIndex(tenantId);
        createContractIndex(tenantId);
        createKnowledgeIndex(tenantId);
        
        log.info("所有业务索引创建完成");
    }

    /**
     * 创建客户索引
     */
    public boolean createCustomerIndex(Long tenantId) throws IOException {
        String indexName = getFullIndexName(INDEX_CUSTOMER, tenantId);
        
        if (indexExists(indexName)) {
            log.info("客户索引已存在: {}", indexName);
            return false;
        }

        CreateIndexRequest request = CreateIndexRequest.of(builder ->
                builder.index(indexName)
                        .settings(s -> s
                                .numberOfShards("3")
                                .numberOfReplicas("1")
                                .analysis(a -> a
                                        .analyzer("ik_smart_analyzer", an -> an
                                                .custom(c -> c
                                                        .tokenizer("ik_smart")))
                                        .analyzer("ik_max_word_analyzer", an -> an
                                                .custom(c -> c
                                                        .tokenizer("ik_max_word")))
                                        .analyzer("pinyin_analyzer", an -> an
                                                .custom(c -> c
                                                        .tokenizer("pinyin")))))
                        .mappings(m -> m
                                .properties("id", p -> p.long_(l -> l))
                                .properties("tenantId", p -> p.long_(l -> l))
                                .properties("name", p -> p.text(t -> t
                                        .analyzer("ik_max_word_analyzer")
                                        .searchAnalyzer("ik_smart_analyzer")
                                        .fields("keyword", f -> f.keyword(k -> k))
                                        .fields("pinyin", f -> f.text(tx -> tx.analyzer("pinyin_analyzer")))))
                                .properties("code", p -> p.keyword(k -> k))
                                .properties("contact", p -> p.text(t -> t.analyzer("ik_max_word_analyzer")))
                                .properties("phone", p -> p.keyword(k -> k))
                                .properties("email", p -> p.keyword(k -> k))
                                .properties("address", p -> p.text(t -> t.analyzer("ik_max_word_analyzer")))
                                .properties("creditCode", p -> p.keyword(k -> k))
                                .properties("status", p -> p.keyword(k -> k))
                                .properties("createTime", p -> p.date(d -> d))
                                .properties("updateTime", p -> p.date(d -> d)))
        );

        CreateIndexResponse response = client.indices().create(request);
        log.info("创建客户索引: {}, acknowledged={}", indexName, response.acknowledged());
        return response.acknowledged();
    }

    /**
     * 创建产品索引
     */
    public boolean createProductIndex(Long tenantId) throws IOException {
        String indexName = getFullIndexName(INDEX_PRODUCT, tenantId);
        
        if (indexExists(indexName)) {
            log.info("产品索引已存在: {}", indexName);
            return false;
        }

        CreateIndexRequest request = CreateIndexRequest.of(builder ->
                builder.index(indexName)
                        .settings(s -> s
                                .numberOfShards("3")
                                .numberOfReplicas("1")
                                .analysis(a -> a
                                        .analyzer("ik_smart_analyzer", an -> an
                                                .custom(c -> c
                                                        .tokenizer("ik_smart")))
                                        .analyzer("ik_max_word_analyzer", an -> an
                                                .custom(c -> c
                                                        .tokenizer("ik_max_word")))))
                        .mappings(m -> m
                                .properties("id", p -> p.long_(l -> l))
                                .properties("tenantId", p -> p.long_(l -> l))
                                .properties("name", p -> p.text(t -> t
                                        .analyzer("ik_max_word_analyzer")
                                        .searchAnalyzer("ik_smart_analyzer")
                                        .fields("keyword", f -> f.keyword(k -> k))))
                                .properties("code", p -> p.keyword(k -> k))
                                .properties("barcode", p -> p.keyword(k -> k))
                                .properties("category", p -> p.keyword(k -> k))
                                .properties("brand", p -> p.keyword(k -> k))
                                .properties("specification", p -> p.text(t -> t.analyzer("ik_smart_analyzer")))
                                .properties("description", p -> p.text(t -> t.analyzer("ik_max_word_analyzer")))
                                .properties("unit", p -> p.keyword(k -> k))
                                .properties("price", p -> p.scaledFloat(sf -> sf.scalingFactor(100)))
                                .properties("status", p -> p.keyword(k -> k))
                                .properties("createTime", p -> p.date(d -> d))
                                .properties("updateTime", p -> p.date(d -> d)))
        );

        CreateIndexResponse response = client.indices().create(request);
        log.info("创建产品索引: {}, acknowledged={}", indexName, response.acknowledged());
        return response.acknowledged();
    }

    /**
     * 创建订单索引
     */
    public boolean createOrderIndex(Long tenantId) throws IOException {
        String indexName = getFullIndexName(INDEX_ORDER, tenantId);
        
        if (indexExists(indexName)) {
            log.info("订单索引已存在: {}", indexName);
            return false;
        }

        CreateIndexRequest request = CreateIndexRequest.of(builder ->
                builder.index(indexName)
                        .settings(s -> s
                                .numberOfShards("5")
                                .numberOfReplicas("1")
                                .analysis(a -> a
                                        .analyzer("ik_smart_analyzer", an -> an
                                                .custom(c -> c
                                                        .tokenizer("ik_smart")))))
                        .mappings(m -> m
                                .properties("id", p -> p.long_(l -> l))
                                .properties("tenantId", p -> p.long_(l -> l))
                                .properties("orderNo", p -> p.keyword(k -> k))
                                .properties("orderType", p -> p.keyword(k -> k))
                                .properties("customerId", p -> p.long_(l -> l))
                                .properties("customerName", p -> p.text(t -> t
                                        .analyzer("ik_smart_analyzer")
                                        .fields("keyword", f -> f.keyword(k -> k))))
                                .properties("salesId", p -> p.long_(l -> l))
                                .properties("salesName", p -> p.keyword(k -> k))
                                .properties("totalAmount", p -> p.scaledFloat(sf -> sf.scalingFactor(100)))
                                .properties("status", p -> p.keyword(k -> k))
                                .properties("orderDate", p -> p.date(d -> d))
                                .properties("createTime", p -> p.date(d -> d))
                                .properties("updateTime", p -> p.date(d -> d)))
        );

        CreateIndexResponse response = client.indices().create(request);
        log.info("创建订单索引: {}, acknowledged={}", indexName, response.acknowledged());
        return response.acknowledged();
    }

    /**
     * 创建供应商索引
     */
    public boolean createSupplierIndex(Long tenantId) throws IOException {
        String indexName = getFullIndexName(INDEX_SUPPLIER, tenantId);
        
        if (indexExists(indexName)) {
            log.info("供应商索引已存在: {}", indexName);
            return false;
        }

        CreateIndexRequest request = CreateIndexRequest.of(builder ->
                builder.index(indexName)
                        .settings(s -> s
                                .numberOfShards("2")
                                .numberOfReplicas("1")
                                .analysis(a -> a
                                        .analyzer("ik_max_word_analyzer", an -> an
                                                .custom(c -> c
                                                        .tokenizer("ik_max_word")))))
                        .mappings(m -> m
                                .properties("id", p -> p.long_(l -> l))
                                .properties("tenantId", p -> p.long_(l -> l))
                                .properties("name", p -> p.text(t -> t
                                        .analyzer("ik_max_word_analyzer")
                                        .fields("keyword", f -> f.keyword(k -> k))))
                                .properties("code", p -> p.keyword(k -> k))
                                .properties("contact", p -> p.text(t -> t.analyzer("ik_smart_analyzer")))
                                .properties("phone", p -> p.keyword(k -> k))
                                .properties("status", p -> p.keyword(k -> k))
                                .properties("createTime", p -> p.date(d -> d)))
        );

        CreateIndexResponse response = client.indices().create(request);
        log.info("创建供应商索引: {}, acknowledged={}", indexName, response.acknowledged());
        return response.acknowledged();
    }

    /**
     * 创建合同索引
     */
    public boolean createContractIndex(Long tenantId) throws IOException {
        String indexName = getFullIndexName(INDEX_CONTRACT, tenantId);
        
        if (indexExists(indexName)) {
            log.info("合同索引已存在: {}", indexName);
            return false;
        }

        CreateIndexRequest request = CreateIndexRequest.of(builder ->
                builder.index(indexName)
                        .settings(s -> s
                                .numberOfShards("2")
                                .numberOfReplicas("1"))
                        .mappings(m -> m
                                .properties("id", p -> p.long_(l -> l))
                                .properties("tenantId", p -> p.long_(l -> l))
                                .properties("contractNo", p -> p.keyword(k -> k))
                                .properties("title", p -> p.text(t -> t.analyzer("ik_max_word_analyzer")))
                                .properties("partyA", p -> p.keyword(k -> k))
                                .properties("partyB", p -> p.keyword(k -> k))
                                .properties("amount", p -> p.scaledFloat(sf -> sf.scalingFactor(100)))
                                .properties("status", p -> p.keyword(k -> k))
                                .properties("startDate", p -> p.date(d -> d))
                                .properties("endDate", p -> p.date(d -> d))
                                .properties("createTime", p -> p.date(d -> d)))
        );

        CreateIndexResponse response = client.indices().create(request);
        log.info("创建合同索引: {}, acknowledged={}", indexName, response.acknowledged());
        return response.acknowledged();
    }

    /**
     * 创建知识库索引
     */
    public boolean createKnowledgeIndex(Long tenantId) throws IOException {
        String indexName = getFullIndexName(INDEX_KNOWLEDGE, tenantId);
        
        if (indexExists(indexName)) {
            log.info("知识库索引已存在: {}", indexName);
            return false;
        }

        CreateIndexRequest request = CreateIndexRequest.of(builder ->
                builder.index(indexName)
                        .settings(s -> s
                                .numberOfShards("3")
                                .numberOfReplicas("1")
                                .analysis(a -> a
                                        .analyzer("ik_max_word_analyzer", an -> an
                                                .custom(c -> c
                                                        .tokenizer("ik_max_word")))))
                        .mappings(m -> m
                                .properties("id", p -> p.long_(l -> l))
                                .properties("tenantId", p -> p.long_(l -> l))
                                .properties("title", p -> p.text(t -> t.analyzer("ik_max_word_analyzer")))
                                .properties("content", p -> p.text(t -> t.analyzer("ik_max_word_analyzer")))
                                .properties("category", p -> p.keyword(k -> k))
                                .properties("tags", p -> p.keyword(k -> k))
                                .properties("author", p -> p.keyword(k -> k))
                                .properties("createTime", p -> p.date(d -> d))
                                .properties("updateTime", p -> p.date(d -> d)))
        );

        CreateIndexResponse response = client.indices().create(request);
        log.info("创建知识库索引: {}, acknowledged={}", indexName, response.acknowledged());
        return response.acknowledged();
    }

    // ==================== 索引删除方法 ====================

    /**
     * 删除索引
     */
    public boolean deleteIndex(String indexType, Long tenantId) throws IOException {
        String indexName = getFullIndexName(indexType, tenantId);
        
        if (!indexExists(indexName)) {
            log.info("索引不存在: {}", indexName);
            return false;
        }

        DeleteIndexRequest request = DeleteIndexRequest.of(builder -> builder.index(indexName));
        DeleteIndexResponse response = client.indices().delete(request);
        
        log.info("删除索引: {}, acknowledged={}", indexName, response.acknowledged());
        return response.acknowledged();
    }

    /**
     * 删除租户所有索引
     */
    public void deleteAllIndices(Long tenantId) throws IOException {
        log.info("删除租户所有索引, tenantId={}", tenantId);
        
        deleteIndex(INDEX_CUSTOMER, tenantId);
        deleteIndex(INDEX_PRODUCT, tenantId);
        deleteIndex(INDEX_ORDER, tenantId);
        deleteIndex(INDEX_SUPPLIER, tenantId);
        deleteIndex(INDEX_CONTRACT, tenantId);
        deleteIndex(INDEX_KNOWLEDGE, tenantId);
    }

    // ==================== 索引更新方法 ====================

    /**
     * 更新索引映射
     */
    public boolean updateMapping(String indexType, Long tenantId, Map<String, Object> newFields) throws IOException {
        String indexName = getFullIndexName(indexType, tenantId);
        
        if (!indexExists(indexName)) {
            log.warn("索引不存在，无法更新映射: {}", indexName);
            return false;
        }

        PutMappingRequest request = PutMappingRequest.of(builder ->
                builder.index(indexName)
                        .properties(newFields.entrySet().stream()
                                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll)));

        PutMappingResponse response = client.indices().putMapping(request);
        log.info("更新索引映射: {}, acknowledged={}", indexName, response.acknowledged());
        return response.acknowledged();
    }

    // ==================== 索引查询方法 ====================

    /**
     * 检查索引是否存在
     */
    public boolean indexExists(String indexName) throws IOException {
        GetIndexRequest request = GetIndexRequest.of(builder -> builder.index(indexName));
        try {
            GetIndexResponse response = client.indices().get(request);
            return response.result() != null && !response.result().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取索引列表
     */
    public Set<String> getIndexList() throws IOException {
        GetIndexRequest request = GetIndexRequest.of(builder -> builder.index(indexPrefix + "_*"));
        GetIndexResponse response = client.indices().get(request);
        return response.result() != null ? response.result().keySet() : Set.of();
    }

    // ==================== 工具方法 ====================

    /**
     * 获取完整索引名称
     * 格式：{prefix}_{indexType}_{tenantId}
     */
    public String getFullIndexName(String indexType, Long tenantId) {
        if (tenantId == null) {
            return String.format("%s_%s", indexPrefix, indexType);
        }
        return String.format("%s_%s_%d", indexPrefix, indexType, tenantId);
    }

    /**
     * 获取索引别名
     */
    public String getIndexAlias(String indexType) {
        return String.format("%s_%s_alias", indexPrefix, indexType);
    }
}
