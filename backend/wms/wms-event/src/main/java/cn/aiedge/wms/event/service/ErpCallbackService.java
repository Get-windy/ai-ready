package cn.aiedge.wms.event.service;

import cn.aiedge.base.vo.Result;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * ERP 回调服务 - 处理 WMS → ERP 的事件回调
 */
public interface ErpCallbackService {
    Result<String> handleReceiptComplete(JsonNode payload);
    Result<String> handleShipComplete(JsonNode payload);
    Result<String> handleInventoryChange(JsonNode payload);
    Result<String> handleCheckDiff(JsonNode payload);
}
