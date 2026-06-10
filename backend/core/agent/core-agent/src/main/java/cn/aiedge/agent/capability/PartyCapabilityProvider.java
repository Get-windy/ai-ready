package cn.aiedge.agent.capability;

import cn.aiedge.agent.annotation.AgentCapability;
import cn.aiedge.erp.party.entity.CustomerGrade;
import cn.aiedge.erp.party.entity.CustomerGradePrice;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.service.CustomerGradePriceService;
import cn.aiedge.erp.party.service.CustomerGradeService;
import cn.aiedge.erp.party.service.PartyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
@AgentCapability(code = "party", name = "往来单位", tags = {"party", "customer", "supplier"})
public class PartyCapabilityProvider {

    private final PartyService partyService;
    private final CustomerGradeService customerGradeService;
    private final CustomerGradePriceService customerGradePriceService;

    @Autowired
    public PartyCapabilityProvider(PartyService partyService,
                                   CustomerGradeService customerGradeService,
                                   CustomerGradePriceService customerGradePriceService) {
        this.partyService = partyService;
        this.customerGradeService = customerGradeService;
        this.customerGradePriceService = customerGradePriceService;
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
        // 校验必填参数
        if (args.get("partyId") == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "缺少 partyId 参数");
            return error;
        }

        Long partyId = ((Number) args.get("partyId")).longValue();
        Long productId = args.get("productId") != null ? ((Number) args.get("productId")).longValue() : null;

        // 查询往来单位信息
        Party party = partyService.getPartyDetailById(partyId);
        if (party == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "未找到该往来单位");
            return error;
        }

        String partyLevel = party.getPartyLevel();
        if (partyLevel == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "该往来单位未设置等级（partyLevel 为空）");
            return error;
        }
        log.info("查询客户等级价格：partyId={}, partyName={}, partyLevel={}, productId={}",
                partyId, party.getPartyName(), partyLevel, productId);

        // 根据 partyLevel 映射到客户等级配置
        CustomerGrade grade = customerGradeService.getByGradeCode(partyLevel);
        if (grade == null) {
            log.warn("未找到 partyLevel={} 对应的客户等级配置，使用默认定价逻辑", partyLevel);
            return buildFallbackPriceResult(party, productId);
        }
        log.info("匹配到等级配置：gradeId={}, gradeName={}, gradeCode={}, discountRate={}",
                grade.getId(), grade.getGradeName(), grade.getGradeCode(), grade.getDiscountRate());

        Map<String, Object> result = new HashMap<>();
        result.put("partyId", partyId);
        result.put("partyName", party.getPartyName());
        result.put("partyLevel", partyLevel);
        result.put("gradeId", grade.getId());
        result.put("gradeName", grade.getGradeName());
        result.put("discountRate", grade.getDiscountRate());

        if (productId != null) {
            // 查询指定产品的等级价格
            CustomerGradePrice gradePrice = customerGradePriceService.getByGradeAndProduct(grade.getId(), productId);
            if (gradePrice != null) {
                result.put("productId", productId);
                result.put("priceId", gradePrice.getId());
                result.put("priceType", gradePrice.getPriceType());
                result.put("standardPrice", gradePrice.getStandardPrice());
                result.put("gradePrice", gradePrice.getGradePrice());
                result.put("discountRate", gradePrice.getDiscountRate());
                result.put("minPrice", gradePrice.getMinPrice());
                result.put("maxPrice", gradePrice.getMaxPrice());
                log.debug("查找到产品等级价格：productId={}, gradePrice={}, discountRate={}",
                        productId, gradePrice.getGradePrice(), gradePrice.getDiscountRate());
            } else {
                result.put("productId", productId);
                result.put("message", "该产品未设置等级价格");
                log.info("产品未设置等级价格：productId={}", productId);
            }
        } else {
            // 查询该等级下所有产品的定价
            List<CustomerGradePrice> priceList = customerGradePriceService.listByGrade(grade.getId());
            List<Map<String, Object>> priceMaps = priceList.stream().map(p -> {
                Map<String, Object> m = new HashMap<>();
                m.put("priceId", p.getId());
                m.put("productId", p.getProductId());
                m.put("productName", p.getProductName());
                m.put("productSpec", p.getProductSpec());
                m.put("skuId", p.getSkuId());
                m.put("priceType", p.getPriceType());
                m.put("standardPrice", p.getStandardPrice());
                m.put("gradePrice", p.getGradePrice());
                m.put("discountRate", p.getDiscountRate());
                m.put("minPrice", p.getMinPrice());
                m.put("maxPrice", p.getMaxPrice());
                m.put("isDefault", p.getIsDefault());
                m.put("priority", p.getPriority());
                m.put("status", p.getStatus());
                return m;
            }).collect(Collectors.toList());
            result.put("total", priceList.size());
            result.put("prices", priceMaps);
            log.debug("查找到 {} 条等级价格记录", priceList.size());
        }

        return result;
    }

    /**
     * 当未找到等级配置时的兜底逻辑：基于 partyLevel (A/B/C/D) 返回示例定价
     */
    private Map<String, Object> buildFallbackPriceResult(Party party, Long productId) {
        Map<String, Object> result = new HashMap<>();
        result.put("partyId", party.getId());
        result.put("partyName", party.getPartyName());
        result.put("partyLevel", party.getPartyLevel());
        result.put("note", "未匹配到正式等级配置，以下为基于 partyLevel 的示例定价");

        if (productId != null) {
            result.put("productId", productId);
        }

        // 基于 partyLevel (A/B/C/D) 的降级定价策略
        Map<String, Object> tieredPricing = buildTieredPricing(party.getPartyLevel(), productId);
        result.put("tieredPricing", tieredPricing);
        return result;
    }

    /**
     * 构建基于等级 (A/B/C/D) 的阶梯定价
     */
    private Map<String, Object> buildTieredPricing(String partyLevel, Long productId) {
        Map<String, Object> pricing = new HashMap<>();
        pricing.put("rule", "基于往来单位等级的示例定价");
        pricing.put("level", partyLevel);

        // 等级对应的折扣率：A=90%, B=85%, C=80%, D=75%
        java.math.BigDecimal[] discountByLevel = {
                null, // 占位，index 0 不用
                new java.math.BigDecimal("90"),  // A
                new java.math.BigDecimal("85"),  // B
                new java.math.BigDecimal("80"),  // C
                new java.math.BigDecimal("75")   // D
        };

        int levelIndex = Math.max(0, Math.min(4, partyLevel.charAt(0) - 'A' + 1));
        java.math.BigDecimal discountRate = discountByLevel[levelIndex];
        pricing.put("defaultDiscountRate", discountRate + "%");

        if (productId != null) {
            pricing.put("productId", productId);
            pricing.put("estimatedPrice", "标准价格 × " + discountRate + "%");
        }

        pricing.put("levelDescriptions", Map.of(
                "A", "优质客户，折扣率 90%",
                "B", "良好客户，折扣率 85%",
                "C", "普通客户，折扣率 80%",
                "D", "待发展客户，折扣率 75%"
        ));

        log.warn("使用降级定价策略：partyLevel={}, discountRate={}", partyLevel, discountRate);
        return pricing;
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
