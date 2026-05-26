package cn.aiedge.agent.capability;

import cn.aiedge.agent.annotation.AgentCapability;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@AgentCapability(code = "erp.sales", name = "销售模块能力", tags = {"erp", "sales"})
public class ErpSalesCapabilityProvider {

    @AgentCapability(
            code = "sale.order.create",
            name = "创建销售订单",
            description = "创建销售订单",
            tags = {"sales", "order"},
            timeout = 60
    )
    public Map<String, Object> createOrder(Map<String, Object> args) {
        return Map.of(
                "orderId", UUID.randomUUID().toString(),
                "status", "created",
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        );
    }

    @AgentCapability(
            code = "sale.order.query",
            name = "查询销售订单",
            tags = {"sales", "order"},
            timeout = 30
    )
    public Map<String, Object> queryOrder(Map<String, Object> args) {
        return Map.of(
                "count", 25,
                "orders", List.of()
        );
    }
}
