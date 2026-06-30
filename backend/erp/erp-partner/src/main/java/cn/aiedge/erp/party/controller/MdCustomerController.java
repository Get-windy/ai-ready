package cn.aiedge.erp.party.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.party.dto.MdCustomerVO;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.entity.PartyFollow;
import cn.aiedge.erp.party.entity.PartyCategory;
import cn.aiedge.erp.party.service.IPartyCategoryService;
import cn.aiedge.erp.party.service.PartyFollowService;
import cn.aiedge.erp.party.service.PartyService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MD客户管理——往来单位（POS/ERP成交客户）
 * <p>
 * 使用 biz_party 表，接管原 PartnerController(erp_partner) 全部功能。
 * 字段名向前端映射：partyCode→partnerCode, partyName→partnerName, partyType=1→partnerType='customer'
 * </p>
 */
@Tag(name = "MD客户管理", description = "MD客户CRUD（biz_party），替换旧版 PartnerController(erp_partner)")
@RestController
@RequestMapping("/api/erp/md/customer")
@RequiredArgsConstructor
public class MdCustomerController {

    private final PartyService partyService;
    private final PartyFollowService partyFollowService;
    private final IPartyCategoryService partyCategoryService;

    // ── 类型映射 ──
    private static final Map<String, Integer> PARTNER_TYPE_TO_PARTY = Map.of(
            "customer", 1,
            "supplier", 2,
            "logistics", 3,
            "other", 4
    );

    private static final Map<Integer, String> PARTY_TYPE_TO_PARTNER = Map.of(
            1, "customer",
            2, "supplier",
            3, "logistics",
            4, "other"
    );

    // ==== 分页查询 ====

    @Operation(summary = "分页查询MD客户")
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResult<MdCustomerVO>>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String partnerType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String settleType,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String handler,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String createTimeStart,
            @RequestParam(required = false) String createTimeEnd,
            @RequestParam(required = false) String lastTradeStart,
            @RequestParam(required = false) String lastTradeEnd,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        Integer partyTypeVal = partnerType != null ? PARTNER_TYPE_TO_PARTY.get(partnerType.toLowerCase()) : null;
        Integer statusVal;
        if ("DISABLED".equalsIgnoreCase(status)) {
            statusVal = 0;
        } else if ("ENABLED".equalsIgnoreCase(status)) {
            statusVal = 1;
        } else {
            statusVal = null;
        }
        LocalDate ctStart = createTimeStart != null ? LocalDate.parse(createTimeStart) : null;
        LocalDate ctEnd = createTimeEnd != null ? LocalDate.parse(createTimeEnd) : null;
        LocalDate ltStart = lastTradeStart != null ? LocalDate.parse(lastTradeStart) : null;
        LocalDate ltEnd = lastTradeEnd != null ? LocalDate.parse(lastTradeEnd) : null;

        IPage<Party> page = partyService.getPartyPage(keyword, partyTypeVal, statusVal,
                categoryId, settleType, region, handler, address,
                ctStart, ctEnd, ltStart, ltEnd, pageNum, pageSize);

        List<MdCustomerVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        PageResult<MdCustomerVO> pageResult = new PageResult<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(page.getTotal());
        pageResult.setPageNum(page.getCurrent());
        pageResult.setPageSize(page.getSize());

        return ResponseEntity.ok(ApiResponse.ok(pageResult));
    }

    // ==== 查询详情 ====

    @Operation(summary = "查询MD客户详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MdCustomerVO>> getById(@PathVariable Long id) {
        Party party = partyService.getPartyDetailById(id);
        if (party == null) {
            return ResponseEntity.ok(ApiResponse.ok(null));
        }
        return ResponseEntity.ok(ApiResponse.ok(toVO(party)));
    }

    // ==== 搜索(下拉) ====

    @Operation(summary = "搜索MD客户(下拉)")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MdCustomerVO>>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) String partnerType) {
        Integer partyTypeVal = partnerType != null ? PARTNER_TYPE_TO_PARTY.get(partnerType.toLowerCase()) : null;
        List<Party> list = partyService.search(keyword, partyTypeVal);
        List<MdCustomerVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(voList));
    }

    // ==== 获取列表(不分页) ====

    @Operation(summary = "获取MD客户列表(不分页)")
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<MdCustomerVO>>> list(
            @RequestParam(required = false) String partnerType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "200") Integer pageSize) {
        Integer partyTypeVal = partnerType != null ? PARTNER_TYPE_TO_PARTY.get(partnerType.toLowerCase()) : null;
        Integer statusVal;
        if ("DISABLED".equalsIgnoreCase(status)) {
            statusVal = 0;
        } else if ("ENABLED".equalsIgnoreCase(status)) {
            statusVal = 1;
        } else {
            statusVal = null;
        }
        List<Party> list = partyService.getPartyList(partyTypeVal, statusVal, pageSize);
        List<MdCustomerVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(voList));
    }

    // ==== 新增 ====

    @Operation(summary = "新增MD客户")
    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> create(@RequestBody Map<String, Object> body) {
        Party party = fromBody(body, null);
        boolean success = partyService.save(party);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    // ==== 更新 ====

    @Operation(summary = "更新MD客户")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Party party = fromBody(body, id);
        boolean success = partyService.updateById(party);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    // ==== 状态更新 ====

    @Operation(summary = "启用/停用MD客户")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Boolean>> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Integer statusVal = "DISABLED".equalsIgnoreCase(status) ? 0 : 1;
        boolean success = partyService.updatePartyStatus(id, statusVal);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    // ==== 删除 ====

    @Operation(summary = "删除MD客户")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> delete(@PathVariable Long id) {
        boolean success = partyService.removeById(id);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    // ==== 下一个编号 ====

    @Operation(summary = "获取下一个编号序号")
    @GetMapping("/next-seq")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getNextSeq(@RequestParam String prefix) {
        Map<String, Integer> result = new HashMap<>();
        result.put("seq", partyService.getNextSeq(prefix));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ==== 跟进记录 ====

    @Operation(summary = "添加跟进记录")
    @PostMapping("/{id}/follow")
    public ResponseEntity<ApiResponse<Boolean>> addFollow(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        PartyFollow follow = new PartyFollow();
        follow.setPartyId(id);
        if (body.get("followType") instanceof Number) {
            follow.setFollowType(((Number) body.get("followType")).intValue());
        }
        follow.setContent((String) body.get("content"));
        if (body.get("result") instanceof Number) {
            follow.setFollowResult(((Number) body.get("result")).intValue());
        }
        boolean success = partyFollowService.save(follow);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    // ==== 导入 ====

    @Operation(summary = "导入MD客户(CSV)")
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<Boolean>> importCustomers(@RequestBody List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(false));
        }
        List<Party> parties = records.stream()
                .map(r -> fromBody(r, null))
                .collect(Collectors.toList());
        boolean success = partyService.saveBatch(parties);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    // ============================================================
    // 内部转换方法
    // ============================================================

    private MdCustomerVO toVO(Party party) {
        if (party == null) return null;
        MdCustomerVO vo = new MdCustomerVO();
        vo.setId(party.getId());
        vo.setPartnerCode(party.getPartyCode());
        vo.setPartnerName(party.getPartyName());
        vo.setPartnerShortName(party.getShortName());
        vo.setPartnerType(PARTY_TYPE_TO_PARTNER.getOrDefault(party.getPartyType(), "customer"));
        vo.setCategoryId(party.getCategoryId());
        if (party.getCategoryId() != null) {
            PartyCategory category = partyCategoryService.getById(party.getCategoryId());
            vo.setCategoryName(category != null ? category.getCategoryName() : null);
        }
        vo.setGradeName(party.getPartyLevel());
        // settlementType Integer → settleType String
        vo.setSettleType(party.getSettlementType() != null && party.getSettlementType() == 1 ? "挂账" : "现结");
        vo.setPhone(party.getPhone());
        vo.setFax(party.getFax());
        vo.setEmail(party.getEmail());
        vo.setWebsite(party.getWebsite());
        vo.setLegalPerson(party.getLegalPerson());
        vo.setTaxNumber(party.getTaxNumber());
        vo.setBankName(party.getBankName());
        vo.setBankAccount(party.getBankAccount());
        vo.setCreditLimit(party.getCreditLimit());
        vo.setRemark(party.getRemark());
        // status Integer → String (兼容前端 ENABLED/DISABLED)
        vo.setStatus(party.getStatus() != null && party.getStatus() == 1 ? "ENABLED" : "DISABLED");
        vo.setStatusDesc(party.getStatus() != null && party.getStatus() == 1 ? "已启用" : "已停用");
        vo.setCreateTime(party.getCreateTime());
        vo.setUpdateTime(party.getUpdateTime());
        vo.setDeleted(party.getDeleted());
        return vo;
    }

    @SuppressWarnings("unchecked")
    private Party fromBody(Map<String, Object> body, Long existingId) {
        Party party = existingId != null ? new Party() : new Party();
        if (existingId != null) {
            party.setId(existingId);
        }
        // 基础字段
        party.setPartyCode((String) body.getOrDefault("partnerCode", body.get("partyCode")));
        party.setPartyName((String) body.getOrDefault("partnerName", body.get("partyName")));
        party.setShortName((String) body.getOrDefault("partnerShortName", body.get("shortName")));

        // 类型映射
        String partnerType = (String) body.getOrDefault("partnerType", "customer");
        party.setPartyType(PARTNER_TYPE_TO_PARTY.getOrDefault(partnerType.toLowerCase(), 1));

        // 分类
        if (body.get("categoryId") instanceof Number) {
            party.setCategoryId(((Number) body.get("categoryId")).longValue());
        }
        if (body.get("partnerCategoryId") instanceof Number) {
            party.setCategoryId(((Number) body.get("partnerCategoryId")).longValue());
        }

        // 联系人
        String email = (String) body.getOrDefault("contactEmail", body.get("email"));
        if (email != null) party.setEmail(email);

        // 结算方式
        String settleType = (String) body.getOrDefault("settleType", "现结");
        party.setSettlementType("挂账".equals(settleType) ? 1 : 0);

        // 级别
        String gradeName = (String) body.getOrDefault("gradeName", body.get("partyLevel"));
        if (gradeName != null) party.setPartyLevel(gradeName);

        // 其他字段
        String phone = (String) body.getOrDefault("phone", null);
        if (phone != null) party.setPhone(phone);
        String fax = (String) body.getOrDefault("fax", null);
        if (fax != null) party.setFax(fax);
        String website = (String) body.getOrDefault("website", null);
        if (website != null) party.setWebsite(website);
        String legalPerson = (String) body.getOrDefault("legalPerson", null);
        if (legalPerson != null) party.setLegalPerson(legalPerson);
        String taxNumber = (String) body.getOrDefault("taxNumber", null);
        if (taxNumber != null) party.setTaxNumber(taxNumber);
        String bankName = (String) body.getOrDefault("bankName", null);
        if (bankName != null) party.setBankName(bankName);
        String bankAccount = (String) body.getOrDefault("bankAccount", null);
        if (bankAccount != null) party.setBankAccount(bankAccount);
        String remark = (String) body.getOrDefault("remark", null);
        if (remark != null) party.setRemark(remark);

        // 额度
        if (body.get("creditLimit") instanceof Number) {
            party.setCreditLimit(new BigDecimal(body.get("creditLimit").toString()));
        }

        // 状态映射（兼容 Integer 和 String 两种格式）
        Object statusObj = body.get("status");
        if (statusObj instanceof Number) {
            party.setStatus(((Number) statusObj).intValue() == 1 ? 1 : 0);
        } else {
            String statusStr = statusObj != null ? statusObj.toString() : "ENABLED";
            party.setStatus("DISABLED".equalsIgnoreCase(statusStr) ? 0 : 1);
        }

        return party;
    }
}
