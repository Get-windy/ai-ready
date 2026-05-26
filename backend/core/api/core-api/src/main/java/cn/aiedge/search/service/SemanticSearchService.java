package cn.aiedge.search.service;

import cn.aiedge.search.model.*;

import java.util.List;

/**
 * 语义搜索服务接口
 * 提供基于语义理解的智能搜索功能
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SemanticSearchService {

    /**
     * 语义搜索
     * 理解用户意图并进行智能搜索
     *
     * @param query    搜索查询
     * @param tenantId 租户 ID
     * @param limit    结果数量限制
     * @return 搜索结果
     */
    SearchResponse semanticSearch(String query, Long tenantId, Integer limit);

    /**
     * 获取查询意图
     * 分析用户查询的意图类型
     *
     * @param query 用户查询
     * @return 意图类型
     */
    String getIntent(String query);

    /**
     * 提取关键实体
     * 从查询中提取关键实体信息
     *
     * @param query 用户查询
     * @return 实体列表
     */
    List<String> extractEntities(String query);

    /**
     * 查询扩展
     * 对用户查询进行语义扩展
     *
     * @param query 原始查询
     * @return 扩展后的查询列表
     */
    List<String> expandQuery(String query);

    /**
     * 相似度计算
     * 计算两个文本的语义相似度
     *
     * @param text1 文本1
     * @param text2 文本2
     * @return 相似度分数 (0-1)
     */
    double calculateSimilarity(String text1, String text2);

    /**
     * 智能纠错
     * 对查询进行拼写纠错
     *
     * @param query 原始查询
     * @return 纠错后的查询
     */
    String correctSpelling(String query);
}
