package cn.aiedge.agent.capability;

import cn.aiedge.agent.annotation.AgentCapability;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@AgentCapability(code = "erp.purchase", name = "采购模块能力", tags = {"erp", "purchase"})
public class ErpPurchaseCapabilityProvider {

    @AgentCapability(code = "purchase.order.create", name = "创建采购订单", tags = {"purchase", "order"}, timeout = 60)
    public Map<String, Object> createPurchaseOrder(Map<String, Object> args) {
        return Map.of("status", "success", "orderNo", "PO-" + System.currentTimeMillis());
    }

    @AgentCapability(code = "purchase.order.query", name = "查询采购订单", tags = {"purchase", "order"})
    public Map<String, Object> queryPurchaseOrder(Map<String, Object> args) {
        return Map.of("count", 0, "list", List.of());
    }

    @AgentCapability(code = "purchase.inquiry.create", name = "创建询价单", tags = {"purchase", "inquiry"})
    public Map<String, Object> createInquiry(Map<String, Object> args) {
        return Map.of("status", "created", "inquiryNo", "IQ-" + System.currentTimeMillis());
    }

    @AgentCapability(code = "purchase.quote.query", name = "查询报价", tags = {"purchase", "quote"})
    public Map<String, Object> queryQuote(Map<String, Object> args) {
        return Map.of("count", 0, "quotes", List.of());
    }

    @AgentCapability(code = "purchase.supplier.query", name = "查询供应商", tags = {"purchase", "supplier"})
    public Map<String, Object> querySuppliers(Map<String, Object> args) {
        return Map.of("count", 0, "suppliers", List.of());
    }
}
