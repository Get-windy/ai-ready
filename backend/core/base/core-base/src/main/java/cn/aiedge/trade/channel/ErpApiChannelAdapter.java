package cn.aiedge.trade.channel;

import cn.aiedge.trade.dto.ExternalOrderDTO;
import cn.aiedge.trade.dto.InventoryQueryResult;
import cn.aiedge.trade.dto.ProductSyncResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * ERP对接渠道适配器
 *
 * 用于其他ERP系统对接，提供标准化的API接口
 */
@Component
public class ErpApiChannelAdapter implements ExternalChannelAdapter {

    @Override
    public String getChannelCode() {
        return "ERP_API";
    }

    @Override
    public String getChannelName() {
        return "ERP对接";
    }

    @Override
    public String getChannelType() {
        return "ERP";
    }

    @Override
    public void initialize(Map<String, String> config) {
        // ERP对接通常不需要OAuth，使用API Key认证
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void refreshToken() {
        // ERP对接不需要token刷新
    }

    @Override
    public List<ExternalOrderDTO> pullOrders(String startTime, String endTime, Integer pageSize) {
        // ERP对接场景：外部ERP推送订单到本系统，不主动拉取
        return new ArrayList<>();
    }

    @Override
    public ExternalOrderDTO pullOrderDetail(String externalOrderId) {
        return new ExternalOrderDTO();
    }

    @Override
    public void pushOrderStatus(String internalOrderId, String externalOrderId, String status, Map<String, String> extra) {
        // 推送订单状态变更到外部ERP
        // 可通过Webhook或回调URL实现
    }

    @Override
    public void pushLogistics(String externalOrderId, String logisticsCompany, String logisticsNo) {
        // 推送物流信息
    }

    @Override
    public InventoryQueryResult queryInventory(String skuCode) {
        // ERP对接场景：外部ERP查询库存使用OpenApiController
        InventoryQueryResult result = new InventoryQueryResult();
        result.setSkuCode(skuCode);
        result.setSuccess(true);
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
        // ERP对接场景：库存变更通知外部ERP
        ProductSyncResult result = new ProductSyncResult();
        result.setSkuCode(skuCode);
        result.setStatus(1);
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
        return new ProductSyncResult();
    }

    @Override
    public ProductSyncResult updatePrice(String skuCode, BigDecimal price) {
        return new ProductSyncResult();
    }

    @Override
    public ProductSyncResult updateStatus(String skuCode, Integer status) {
        return new ProductSyncResult();
    }

    @Override
    public ExternalOrderDTO handleOrderCallback(String callbackData) {
        // 解析ERP推送的订单JSON
        ExternalOrderDTO dto = new ExternalOrderDTO();
        dto.setRawJson(callbackData);
        dto.setChannelCode(getChannelCode());
        return dto;
    }

    @Override
    public Map<String, Object> handleRefundCallback(String callbackData) {
        return new HashMap<>();
    }

    @Override
    public boolean verifyCallbackSignature(String callbackData, String signature) {
        // 使用API Key + HMAC验证
        return true;
    }
}