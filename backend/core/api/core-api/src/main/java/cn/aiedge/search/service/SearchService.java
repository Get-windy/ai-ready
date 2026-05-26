package cn.aiedge.search.service;

import cn.aiedge.search.model.*;

import java.util.List;

/**
 * 搜索服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SearchService {

    /**
     * 全局搜索
     *
     * @param request 搜索请求
     * @return 搜索响应
     */
    SearchResponse search(SearchRequest request);

    /**
     * 搜索客户
     *
     * @param keyword  关键词
     * @param page     页码
     * @param pageSize 每页大小
     * @param tenantId 租户ID
     * @return 搜索结果列表
     */
    List<SearchResult> searchCustomer(String keyword, int page, int pageSize, Long tenantId);

    /**
     * 搜索产品
     *
     * @param keyword  关键词
     * @param page     页码
     * @param pageSize 每页大小
     * @param tenantId 租户ID
     * @return 搜索结果列表
     */
    List<SearchResult> searchProduct(String keyword, int page, int pageSize, Long tenantId);

    /**
     * 搜索订单
     *
     * @param keyword  关键词
     * @param page     页码
     * @param pageSize 每页大小
     * @param tenantId 租户ID
     * @return 搜索结果列表
     */
    List<SearchResult> searchOrder(String keyword, int page, int pageSize, Long tenantId);

    /**
     * 获取搜索建议
     *
     * @param prefix   前缀
     * @param limit    数量限制
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 搜索建议列表
     */
    List<SearchSuggestion> getSuggestions(String prefix, int limit, Long userId, Long tenantId);

    /**
     * 获取热门搜索词
     *
     * @param limit    数量限制
     * @param tenantId 租户ID
     * @return 热门搜索词列表
     */
    List<String> getHotSearches(int limit, Long tenantId);

    /**
     * 获取搜索历史
     *
     * @param userId   用户ID
     * @param limit    数量限制
     * @param tenantId 租户ID
     * @return 搜索历史列表
     */
    List<SearchHistory> getSearchHistory(Long userId, int limit, Long tenantId);

    /**
     * 清空搜索历史
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 是否成功
     */
    boolean clearSearchHistory(Long userId, Long tenantId);

    /**
     * 删除单条搜索历史
     *
     * @param historyId 历史ID
     * @param userId    用户ID
     * @param tenantId  租户ID
     * @return 是否成功
     */
    boolean deleteSearchHistory(Long historyId, Long userId, Long tenantId);

    /**
     * 记录搜索历史
     *
     * @param userId      用户ID
     * @param keyword     关键词
     * @param searchType  搜索类型
     * @param resultCount 结果数量
     * @param tenantId    租户ID
     */
    void recordSearchHistory(Long userId, String keyword, String searchType, Integer resultCount, Long tenantId);

    /**
     * 索引客户数据
     *
     * @param customerId 客户ID
     * @param data       客户数据
     * @param tenantId   租户ID
     */
    void indexCustomer(Long customerId, Object data, Long tenantId);

    /**
     * 索引产品数据
     *
     * @param productId 产品ID
     * @param data      产品数据
     * @param tenantId  租户ID
     */
    void indexProduct(Long productId, Object data, Long tenantId);

    /**
     * 索引订单数据
     *
     * @param orderId 订单ID
     * @param data    订单数据
     * @param tenantId 租户ID
     */
    void indexOrder(Long orderId, Object data, Long tenantId);

    /**
     * 删除客户索引
     *
     * @param customerId 客户ID
     * @param tenantId   租户ID
     */
    void deleteCustomerIndex(Long customerId, Long tenantId);

    /**
     * 删除产品索引
     *
     * @param productId 产品ID
     * @param tenantId  租户ID
     */
    void deleteProductIndex(Long productId, Long tenantId);

    /**
     * 删除订单索引
     *
     * @param orderId  订单ID
     * @param tenantId 租户ID
     */
    void deleteOrderIndex(Long orderId, Long tenantId);

    /**
     * 重建索引
     *
     * @param type     索引类型
     * @param tenantId 租户ID
     * @return 重建数量
     */
    long rebuildIndex(String type, Long tenantId);
}
