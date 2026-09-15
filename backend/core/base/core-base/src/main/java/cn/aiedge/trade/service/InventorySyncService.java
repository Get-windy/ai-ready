package cn.aiedge.trade.service;

import cn.aiedge.trade.dto.InventoryQueryResult;
import cn.aiedge.trade.dto.ProductSyncResult;
import cn.aiedge.trade.entity.InventorySyncRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 库存同步服务
 *
 * 核心职责：
 * 1. 接收外部库存查询请求（高频）
 * 2. 推送库存变更到外部平台
 * 3. 缓存优化查询性能
 * 4. 同步状态跟踪与告警
 */
public interface InventorySyncService {

    /**
     * 查询库存（对外API）
     * 支持缓存，毫秒级响应
     * @param skuCode SKU编码
     * @param warehouseId 仓库ID（可选）
     * @return 库存结果
     */
    InventoryQueryResult queryInventory(String skuCode, Long warehouseId);

    /**
     * 批量查询库存（对外API）
     * @param skuCodes SKU编码列表
     * @return 批量结果
     */
    Map<String, InventoryQueryResult> batchQueryInventory(List<String> skuCodes, Long warehouseId);

    /**
     * 推送库存到外部平台
     * @param channelCode 渠道编码
     * @param skuCode SKU编码
     * @param quantity 数量
     * @return 同步结果
     */
    ProductSyncResult pushToChannel(String channelCode, String skuCode, Integer quantity);

    /**
     * 仅调用渠道（**不写同步记录**）
     *
     * <p>供《API监控》「同步失败重试」复用：重试须回写**原记录**的重试次数与结果，
     * 不能再追加一条新记录（避免同一笔同步重复记账）。调用本身仍落网关调用日志（方向 OUT）。</p>
     *
     * @param channelCode 渠道编码
     * @param skuCode     SKU编码
     * @param quantity    数量
     * @return 渠道返回结果
     */
    ProductSyncResult invokeChannel(String channelCode, String skuCode, Integer quantity);

    /**
     * 批量推送库存
     */
    Map<String, ProductSyncResult> batchPushToChannel(String channelCode, Map<String, Integer> skuQuantities);

    /**
     * 全量同步所有SKU库存到指定渠道
     */
    int fullSyncToChannel(String channelCode);

    /**
     * 刷新缓存
     */
    void refreshCache(String skuCode);

    /**
     * 刷新所有缓存
     */
    void refreshAllCache();

    /**
     * 分页查询同步记录
     */
    cn.aiedge.common.result.PageResult<InventorySyncRecord> pageSyncRecords(Integer pageNum, Integer pageSize, String channelCode, String skuCode);

    /**
     * 查询同步失败的SKU
     */
    List<InventorySyncRecord> listFailedSyncs(String channelCode);

    /**
     * 更新商品价格到外部平台
     */
    ProductSyncResult updatePrice(String channelCode, String skuCode, BigDecimal price);
}