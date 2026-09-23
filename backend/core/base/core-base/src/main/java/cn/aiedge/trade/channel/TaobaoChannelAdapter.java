package cn.aiedge.trade.channel;

import cn.aiedge.trade.dto.ExternalOrderDTO;
import cn.aiedge.trade.dto.InventoryQueryResult;
import cn.aiedge.trade.dto.ProductSyncResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 淘宝/天猫渠道适配器
 *
 * 实际实现需要：
 * 1. 集成淘宝开放平台SDK
 * 2. 实现OAuth2认证
 * 3. 调用淘宝API获取订单
 * 4. 推送库存/价格变更
 */
@Slf4j
@Component
public class TaobaoChannelAdapter implements ExternalChannelAdapter {

    @Override
    public String getChannelCode() {
        return "TAOBAO";
    }

    @Override
    public String getChannelName() {
        return "淘宝/天猫";
    }

    @Override
    public String getChannelType() {
        return "ECOMMERCE";
    }

    @Override
    public void initialize(Map<String, String> config) {
        // TODO: 初始化淘宝SDK，获取access_token
        // TaobaoClient client = new DefaultTaobaoClient(config.get("apiEndpoint"), config.get("appId"), config.get("appSecret"));
    }

    /**
     * 是否已与淘宝建立可用连接。
     *
     * <p>⚠️ 2026-09-23 由「无条件返回 true」改为 false：本类尚未接入淘宝 SDK，
     * {@link #initialize} 是空实现，**没有任何连接存在**。原先恒 true 会让调用方
     * （{@code ExternalOrderServiceImpl#pullOrders}）以为渠道已就绪而继续往下走。
     * 等真正接入 SDK 后，这里应改为「校验 access_token 有效性」。</p>
     */
    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void refreshToken() {
        // TODO: 使用refresh_token刷新access_token
    }

    /**
     * 拉取淘宝订单。
     *
     * <p>⚠️ 2026-09-23 移除原先的**硬编码假数据**（曾返回固定的一条
     * `TB_ORDER_001`／淘宝买家／张三／13800138000／金额 100.00）。那些「订单」一旦
     * 经 {@code ExternalOrderServiceImpl#pullOrders} 落库，就会变成 `external_order_raw`
     * 里的**假台账**，且带真实买家姓名手机号（看起来完全像真数据，极难事后识别）。
     * 未实现就返回**空列表**，让「同步成功但 0 条」如实反映事实。
     * 实现时应调用 `taobao.trades.sold.get` 并做字段映射。</p>
     */
    @Override
    public List<ExternalOrderDTO> pullOrders(String startTime, String endTime, Integer pageSize) {
        // TODO: 调用 taobao.trades.sold.get 接口（尚未实现，故不返回任何数据）
        log.warn("淘宝渠道订单拉取尚未实现，返回空列表: start={}, end={}, pageSize={}",
                startTime, endTime, pageSize);
        return Collections.emptyList();
    }

    @Override
    public ExternalOrderDTO pullOrderDetail(String externalOrderId) {
        // TODO: 调用taobao.trade.fullinfo.get接口
        return new ExternalOrderDTO();
    }

    @Override
    public void pushOrderStatus(String internalOrderId, String externalOrderId, String status, Map<String, String> extra) {
        // TODO: 调用taobao.logistics.offline.send接口发货
    }

    @Override
    public void pushLogistics(String externalOrderId, String logisticsCompany, String logisticsNo) {
        // TODO: 调用taobao.logistics.online.send接口
    }

    @Override
    public InventoryQueryResult queryInventory(String skuCode) {
        // 淘宝不提供库存查询API，只能推送
        InventoryQueryResult result = new InventoryQueryResult();
        result.setSkuCode(skuCode);
        result.setSuccess(false);
        result.setErrorMsg("平台不支持库存查询");
        return result;
    }

    @Override
    public Map<String, InventoryQueryResult> batchQueryInventory(List<String> skuCodes) {
        Map<String, InventoryQueryResult> results = new HashMap<>();
        for (String sku : skuCodes) {
            results.put(sku, queryInventory(sku));
        }
        return results;
    }

    @Override
    public ProductSyncResult pushInventory(String skuCode, Integer quantity) {
        // TODO: 调用taobao.item.quantity.update接口
        ProductSyncResult result = new ProductSyncResult();
        result.setSkuCode(skuCode);
        result.setStatus(1);
        result.setSyncTimestamp(System.currentTimeMillis());
        return result;
    }

    @Override
    public Map<String, ProductSyncResult> batchPushInventory(Map<String, Integer> skuQuantities) {
        Map<String, ProductSyncResult> results = new HashMap<>();
        for (Map.Entry<String, Integer> entry : skuQuantities.entrySet()) {
            results.put(entry.getKey(), pushInventory(entry.getKey(), entry.getValue()));
        }
        return results;
    }

    @Override
    public ProductSyncResult pushProduct(Map<String, Object> productInfo) {
        // TODO: 调用taobao.item.add接口
        return new ProductSyncResult();
    }

    @Override
    public ProductSyncResult updatePrice(String skuCode, BigDecimal price) {
        // TODO: 调用taobao.item.price.update接口
        ProductSyncResult result = new ProductSyncResult();
        result.setSkuCode(skuCode);
        result.setStatus(1);
        return result;
    }

    @Override
    public ProductSyncResult updateStatus(String skuCode, Integer status) {
        // TODO: 调用taobao.item.update.listing/delist接口
        return new ProductSyncResult();
    }

    @Override
    public ExternalOrderDTO handleOrderCallback(String callbackData) {
        // 解析订单回调 JSON（字段契约见 CallbackPayloadParser）；
        // TOP 专有消息格式（XML/签名包裹）接入时在此扩展，未识别键忽略而非报错
        ExternalOrderDTO dto = CallbackPayloadParser.apply(new ExternalOrderDTO(), callbackData);
        dto.setChannelCode(getChannelCode());
        return dto;
    }

    @Override
    public Map<String, Object> handleRefundCallback(String callbackData) {
        // 解析退款回调
        return new HashMap<>();
    }

    @Override
    public boolean verifyCallbackSignature(String callbackData, String signature) {
        // TODO: 验证TOP签名
        // 使用appSecret对回调数据进行签名验证
        return true;
    }
}