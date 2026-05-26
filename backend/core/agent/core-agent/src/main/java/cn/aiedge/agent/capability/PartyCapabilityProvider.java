package cn.aiedge.agent.capability;

import cn.aiedge.agent.annotation.AgentCapability;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.service.PartyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 往来单位能力提供者
 * 提供 AI Agent 查询、管理往来单位的能力
 * 往来单位包含：客户、供应商、物流商、配套商
 */
@Component
@AgentCapability(code = "party", name = "往来单位", tags = {"party", "customer", "supplier"})
public class PartyCapabilityProvider {

    private final PartyService partyService;

    @Autowired
    public PartyCapabilityProvider(PartyService partyService) {
        this.partyService = partyService;
    }

    /**
     * 查询往来单位列表
     * 参数：
     * - partyType: 单位类型（1：客户 2：供应商 3：物流商 4：配套商），可选
     * - partyLevel: 单位等级（A/B/C/D），可选
     * - keyword: 搜索关键词，可选
     * - pageNum: 页码，默认1
     * - pageSize: 每页数量，默认20
     */
    @AgentCapability(
            code = "party.list",
            name = "查询往来单位列表",
            tags = {"party", "list", "query"},
            timeout = 30
    )
    public Map<String, Object> listParties(Map<String, Object> args) {
        Integer pageNum = args.get("pageNum") != null ? ((Number) args.get("pageNum")).intValue() : 1;
        Integer pageSize = args.get("pageSize") != null ? ((Number) args.get("pageSize")).intValue() : 20;
        
        LambdaQueryWrapper<Party> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Party::getDeleted, 0);
        
        if (args.get("partyType") != null) {
            wrapper.eq(Party::getPartyType, ((Number) args.get("partyType")).intValue());
        }
        
        if (args.get("partyLevel") != null) {
            wrapper.eq(Party::getPartyLevel, (String) args.get("partyLevel"));
        }
        
        if (args.get("keyword") != null) {
            String keyword = (String) args.get("keyword");
            wrapper.and(w -> w.like(Party::getPartyName, keyword)
                    .or()
                    .like(Party::getPartyCode, keyword)
                    .or()
                    .like(Party::getShortName, keyword));
        }
        
        Page<Party> page = partyService.page(new Page<>(pageNum, pageSize), wrapper);
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", page.getTotal());
        result.put("list", page.getRecords().stream().map(this::toSimpleMap).collect(Collectors.toList()));
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return result;
    }

    /**
     * 查询往来单位详情
     * 参数：
     * - id: 往来单位ID，必填
     * - partyCode: 单位编码，和ID二选一
     */
    @AgentCapability(
            code = "party.detail",
            name = "查询往来单位详情",
            tags = {"party", "detail", "query"},
            timeout = 20
    )
    public Map<String, Object> getPartyDetail(Map<String, Object> args) {
        Party party = null;
        
        if (args.get("id") != null) {
            Long id = ((Number) args.get("id")).longValue();
            party = partyService.getPartyDetailById(id);
        } else if (args.get("partyCode") != null) {
            String partyCode = (String) args.get("partyCode");
            party = partyService.getByPartyCode(partyCode);
        }
        
        Map<String, Object> result = new HashMap<>();
        if (party != null) {
            result.put("found", true);
            result.put("data", toDetailMap(party));
        } else {
            result.put("found", false);
            result.put("message", "未找到对应的往来单位");
        }
        return result;
    }

    /**
     * 根据单位类型查询列表
     * 参数：
     * - partyType: 单位类型（1：客户 2：供应商 3：物流商 4：配套商），必填
     */
    @AgentCapability(
            code = "party.listByType",
            name = "按类型查询往来单位",
            tags = {"party", "type", "query"},
            timeout = 30
    )
    public Map<String, Object> listPartiesByType(Map<String, Object> args) {
        if (args.get("partyType") == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "缺少 partyType 参数");
            return error;
        }
        
        Integer partyType = ((Number) args.get("partyType")).intValue();
        List<Party> parties = partyService.listByPartyType(partyType);
        
        Map<String, Object> result = new HashMap<>();
        result.put("partyType", partyType);
        result.put("total", parties.size());
        result.put("list", parties.stream().map(this::toSimpleMap).collect(Collectors.toList()));
        return result;
    }

    /**
     * 查询客户等级价格
     * 参数：
     * - partyId: 往来单位ID，必填
     * - productId: 产品ID，可选（不传查询所有产品的等级价格）
     */
    @AgentCapability(
            code = "party.getGradePrice",
            name = "查询客户等级价格",
            tags = {"party", "price", "grade"},
            timeout = 30
    )
    public Map<String, Object> getGradePrice(Map<String, Object> args) {
        // TODO: 接入真实的 CustomerGradePriceService
        Map<String, Object> result = new HashMap<>();
        result.put("partyId", args.get("partyId"));
        result.put("productId", args.get("productId"));
        result.put("message", "客户等级价格功能开发中");
        return result;
    }

    private Map<String, Object> toSimpleMap(Party party) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", party.getId());
        map.put("partyCode", party.getPartyCode());
        map.put("partyName", party.getPartyName());
        map.put("shortName", party.getShortName());
        map.put("partyType", party.getPartyType());
        map.put("partyLevel", party.getPartyLevel());
        map.put("status", party.getStatus());
        return map;
    }

    private Map<String, Object> toDetailMap(Party party) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", party.getId());
        map.put("partyCode", party.getPartyCode());
        map.put("partyName", party.getPartyName());
        map.put("shortName", party.getShortName());
        map.put("partyType", party.getPartyType());
        map.put("partyLevel", party.getPartyLevel());
        map.put("creditLimit", party.getCreditLimit());
        map.put("currentDebt", party.getCurrentDebt());
        map.put("settlementType", party.getSettlementType());
        map.put("settlementDays", party.getSettlementDays());
        map.put("unifiedCode", party.getUnifiedCode());
        map.put("businessLicense", party.getBusinessLicense());
        map.put("taxNumber", party.getTaxNumber());
        map.put("bankName", party.getBankName());
        map.put("bankAccount", party.getBankAccount());
        map.put("registeredAddress", party.getRegisteredAddress());
        map.put("businessAddress", party.getBusinessAddress());
        map.put("phone", party.getPhone());
        map.put("fax", party.getFax());
        map.put("email", party.getEmail());
        map.put("legalPerson", party.getLegalPerson());
        map.put("businessContact", party.getBusinessContact());
        map.put("financeContact", party.getFinanceContact());
        map.put("firstTradeDate", party.getFirstTradeDate());
        map.put("lastTradeDate", party.getLastTradeDate());
        map.put("tradeCount", party.getTradeCount());
        map.put("tradeAmount", party.getTradeAmount());
        map.put("status", party.getStatus());
        return map;
    }
}
