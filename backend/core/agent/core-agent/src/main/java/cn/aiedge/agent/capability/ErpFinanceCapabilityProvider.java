package cn.aiedge.agent.capability;

import cn.aiedge.agent.annotation.AgentCapability;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@AgentCapability(code = "erp.finance", name = "财务模块能力", tags = {"erp", "finance"})
public class ErpFinanceCapabilityProvider {

    @AgentCapability(code = "finance.receivable.query", name = "查询应收账款", tags = {"finance", "receivable"})
    public Map<String, Object> queryReceivable(Map<String, Object> args) {
        return Map.of("count", 0, "list", List.of());
    }

    @AgentCapability(code = "finance.payable.query", name = "查询应付账款", tags = {"finance", "payable"})
    public Map<String, Object> queryPayable(Map<String, Object> args) {
        return Map.of("count", 0, "list", List.of());
    }

    @AgentCapability(code = "finance.voucher.create", name = "创建凭证", tags = {"finance", "voucher"})
    public Map<String, Object> createVoucher(Map<String, Object> args) {
        return Map.of("status", "success", "voucherNo", "VOU-" + System.currentTimeMillis());
    }

    @AgentCapability(code = "finance.report.query", name = "查询财务报表", tags = {"finance", "report"})
    public Map<String, Object> queryReport(Map<String, Object> args) {
        return Map.of("reportType", args.getOrDefault("type", "income_statement"), "data", Map.of());
    }
}
