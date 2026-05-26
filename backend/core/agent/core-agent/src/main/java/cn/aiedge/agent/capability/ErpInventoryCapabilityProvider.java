package cn.aiedge.agent.capability;

import cn.aiedge.agent.annotation.AgentCapability;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@AgentCapability(code = "erp.inventory", name = "库存模块能力", tags = {"erp", "inventory"})
public class ErpInventoryCapabilityProvider {

    @AgentCapability(code = "stock.query", name = "查询库存", tags = {"inventory"})
    public Map<String, Object> queryStock(Map<String, Object> args) {
        return Map.of("count", 0, "items", List.of());
    }

    @AgentCapability(code = "stock.inbound", name = "入库单", tags = {"inventory", "inbound"})
    public Map<String, Object> createInbound(Map<String, Object> args) {
        return Map.of("status", "success", "inboundNo", "IB-" + System.currentTimeMillis());
    }

    @AgentCapability(code = "stock.outbound", name = "出库单", tags = {"inventory", "outbound"})
    public Map<String, Object> createOutbound(Map<String, Object> args) {
        return Map.of("status", "success", "outboundNo", "OB-" + System.currentTimeMillis());
    }

    @AgentCapability(code = "stock.transfer", name = "库存调拨", tags = {"inventory", "transfer"})
    public Map<String, Object> transfer(Map<String, Object> args) {
        return Map.of("status", "success");
    }

    @AgentCapability(code = "stock.check", name = "库存盘点", tags = {"inventory", "check"})
    public Map<String, Object> check(Map<String, Object> args) {
        return Map.of("status", "success", "checkNo", "CK-" + System.currentTimeMillis());
    }

    @AgentCapability(code = "stock.batch.query", name = "查询批次", tags = {"inventory", "batch"})
    public Map<String, Object> queryBatch(Map<String, Object> args) {
        return Map.of("count", 0, "batches", List.of());
    }
}
