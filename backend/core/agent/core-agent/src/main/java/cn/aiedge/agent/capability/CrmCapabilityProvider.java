package cn.aiedge.agent.capability;

import cn.aiedge.agent.annotation.AgentCapability;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@AgentCapability(code = "crm", name = "CRM模块能力", tags = {"crm"})
public class CrmCapabilityProvider {

    @AgentCapability(code = "crm.customer.create", name = "创建客户", tags = {"crm", "customer"})
    public Map<String, Object> createCustomer(Map<String, Object> args) {
        return Map.of("status", "success", "customerId", "CUST-" + System.currentTimeMillis());
    }

    @AgentCapability(code = "crm.customer.query", name = "查询客户", tags = {"crm", "customer"})
    public Map<String, Object> queryCustomer(Map<String, Object> args) {
        return Map.of("count", 0, "customers", List.of());
    }

    @AgentCapability(code = "crm.lead.create", name = "创建线索", tags = {"crm", "lead"})
    public Map<String, Object> createLead(Map<String, Object> args) {
        return Map.of("status", "success", "leadId", "LEAD-" + System.currentTimeMillis());
    }

    @AgentCapability(code = "crm.opportunity.create", name = "创建商机", tags = {"crm", "opportunity"})
    public Map<String, Object> createOpportunity(Map<String, Object> args) {
        return Map.of("status", "success", "oppId", "OPP-" + System.currentTimeMillis());
    }

    @AgentCapability(code = "crm.activity.create", name = "创建活动记录", tags = {"crm", "activity"})
    public Map<String, Object> createActivity(Map<String, Object> args) {
        return Map.of("status", "success");
    }

    @AgentCapability(code = "crm.contract.create", name = "创建合同", tags = {"crm", "contract"})
    public Map<String, Object> createContract(Map<String, Object> args) {
        return Map.of("status", "success", "contractId", "CON-" + System.currentTimeMillis());
    }
}
