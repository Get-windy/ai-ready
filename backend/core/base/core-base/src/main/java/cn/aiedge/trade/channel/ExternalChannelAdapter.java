package cn.aiedge.trade.channel;

import cn.aiedge.trade.dto.ExternalOrderDTO;
import cn.aiedge.trade.dto.InventoryQueryResult;
import cn.aiedge.trade.dto.ProductSyncResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 外部渠道适配器接口
 * 每个外部平台（淘宝、京东、拼多多等）需要实现此接口
 */
public interface ExternalChannelAdapter {

    /** 获取渠道编码 */
    String getChannelCode();

    /** 获取渠道名称 */
    String getChannelName();

    /** 获取渠道类型 */
    String getChannelType();

    /** 初始化连接（获取token等） */
    void initialize(Map<String, String> config);

    /** 检查连接状态 */
    boolean isConnected();

    /** 刷新访问令牌 */
    void refreshToken();

    // ========== 订单相关 ==========

    /** 拉取外部订单列表 */
    List<ExternalOrderDTO> pullOrders(String startTime, String endTime, Integer pageSize);

    /** 拉取单个订单详情 */
    ExternalOrderDTO pullOrderDetail(String externalOrderId);

    /** 推送订单状态变更（发货、取消等） */
    void pushOrderStatus(String internalOrderId, String externalOrderId, String status, Map<String, String> extra);

    /** 推送物流信息 */
    void pushLogistics(String externalOrderId, String logisticsCompany, String logisticsNo);

    // ========== 库存相关 ==========

    /** 查询平台库存 */
    InventoryQueryResult queryInventory(String skuCode);

    /** 批量查询平台库存 */
    Map<String, InventoryQueryResult> batchQueryInventory(List<String> skuCodes);

    /** 推送库存变更到平台 */
    ProductSyncResult pushInventory(String skuCode, Integer quantity);

    /** 批量推送库存 */
    Map<String, ProductSyncResult> batchPushInventory(Map<String, Integer> skuQuantities);

    // ========== 商品相关 ==========

    /** 推送商品信息 */
    ProductSyncResult pushProduct(Map<String, Object> productInfo);

    /** 更新商品价格 */
    ProductSyncResult updatePrice(String skuCode, BigDecimal price);

    /** 更新商品状态（上架/下架） */
    ProductSyncResult updateStatus(String skuCode, Integer status);

    // ========== 回调处理 ==========

    /** 处理订单回调 */
    ExternalOrderDTO handleOrderCallback(String callbackData);

    /** 处理退款回调 */
    Map<String, Object> handleRefundCallback(String callbackData);

    /** 验证回调签名 */
    boolean verifyCallbackSignature(String callbackData, String signature);
}