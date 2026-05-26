package cn.aiedge.search.service;

import cn.aiedge.search.model.SearchRequest;
import cn.aiedge.search.model.SearchResponse;
import cn.aiedge.search.model.SearchResult;

import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 搜索服务接口
 * 
 * 提供基于 Elasticsearch 的全文搜索功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface ElasticsearchSearchService {

    /**
     * 搜索文档
     *
     * @param indexType 索引类型
     * @param request 搜索请求
     * @return 搜索响应
     */
    SearchResponse search(String indexType, SearchRequest request);

    /**
     * 多索引搜索
     *
     * @param indexTypes 索引类型列表
     * @param request 搜索请求
     * @return 搜索响应
     */
    SearchResponse multiSearch(List<String> indexTypes, SearchRequest request);

    /**
     * 索引文档
     *
     * @param indexType 索引类型
     * @param id 文档ID
     * @param document 文档数据
     * @param tenantId 租户ID
     */
    void indexDocument(String indexType, String id, Map<String, Object> document, Long tenantId);

    /**
     * 批量索引文档
     *
     * @param indexType 索引类型
     * @param documents 文档列表（key为ID，value为文档数据）
     * @param tenantId 租户ID
     */
    void bulkIndex(String indexType, Map<String, Map<String, Object>> documents, Long tenantId);

    /**
     * 更新文档
     *
     * @param indexType 索引类型
     * @param id 文档ID
     * @param document 文档数据
     * @param tenantId 租户ID
     */
    void updateDocument(String indexType, String id, Map<String, Object> document, Long tenantId);

    /**
     * 删除文档
     *
     * @param indexType 索引类型
     * @param id 文档ID
     * @param tenantId 租户ID
     */
    void deleteDocument(String indexType, String id, Long tenantId);

    /**
     * 批量删除文档
     *
     * @param indexType 索引类型
     * @param ids 文档ID列表
     * @param tenantId 租户ID
     */
    void bulkDelete(String indexType, List<String> ids, Long tenantId);

    /**
     * 根据查询删除文档
     *
     * @param indexType 索引类型
     * @param field 字段名
     * @param value 字段值
     * @param tenantId 租户ID
     */
    long deleteByQuery(String indexType, String field, Object value, Long tenantId);

    /**
     * 获取文档
     *
     * @param indexType 索引类型
     * @param id 文档ID
     * @param tenantId 租户ID
     * @return 文档数据
     */
    Map<String, Object> getDocument(String indexType, String id, Long tenantId);

    /**
     * 批量获取文档
     *
     * @param indexType 索引类型
     * @param ids 文档ID列表
     * @param tenantId 租户ID
     * @return 文档列表
     */
    List<Map<String, Object>> multiGet(String indexType, List<String> ids, Long tenantId);

    /**
     * 统计文档数量
     *
     * @param indexType 索引类型
     * @param tenantId 租户ID
     * @return 文档数量
     */
    long count(String indexType, Long tenantId);

    /**
     * 检查文档是否存在
     *
     * @param indexType 索引类型
     * @param id 文档ID
     * @param tenantId 租户ID
     * @return 是否存在
     */
    boolean exists(String indexType, String id, Long tenantId);

    /**
     * 获取搜索建议
     *
     * @param indexType 索引类型
     * @param field 字段名
     * @param prefix 前缀
     * @param size 返回数量
     * @param tenantId 租户ID
     * @return 建议列表
     */
    List<String> suggest(String indexType, String field, String prefix, int size, Long tenantId);

    /**
     * 聚合查询
     *
     * @param indexType 索引类型
     * @param field 聚合字段
     * @param size 返回数量
     * @param tenantId 租户ID
     * @return 聚合结果
     */
    Map<String, Long> aggregate(String indexType, String field, int size, Long tenantId);

    /**
     * 刷新索引
     *
     * @param indexType 索引类型
     * @param tenantId 租户ID
     */
    void refreshIndex(String indexType, Long tenantId);
}
