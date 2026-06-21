package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.Partner;
import cn.aiedge.erp.stock.service.PartnerService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "往来单位管理")
@RestController
@RequestMapping("/api/erp/partner")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @Operation(summary = "分页查询往来单位")
    @GetMapping("/page")
    public Result<IPage<Partner>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String partnerType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(partnerService.getPartnerPage(keyword, partnerType, status, categoryId, pageNum, pageSize));
    }

    @Operation(summary = "查询往来单位详情")
    @GetMapping("/{id}")
    public Result<Partner> getById(@PathVariable Long id) {
        return Result.ok(partnerService.getPartnerDetail(id));
    }

    @Operation(summary = "搜索往来单位(下拉)")
    @GetMapping("/search")
    public Result<List<Partner>> search(@RequestParam String keyword,
                                         @RequestParam(required = false) String partnerType) {
        return Result.ok(partnerService.search(keyword, partnerType));
    }

    @Operation(summary = "获取往来单位列表(不分页)")
    @GetMapping("/list")
    public Result<List<Partner>> list(
            @RequestParam(required = false) String partnerType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "200") Integer pageSize) {
        return Result.ok(partnerService.getPartnerList(partnerType, status, pageSize));
    }

    @Operation(summary = "新增往来单位")
    @PostMapping
    public Result<Boolean> create(@RequestBody Partner partner) {
        return Result.ok(partnerService.createPartner(partner));
    }

    @Operation(summary = "新增往来单位(含角色)")
    @PostMapping("/with-roles")
    public Result<Boolean> createWithRoles(@RequestBody Map<String, Object> body) {
        Partner partner = new Partner();
        partner.setPartnerCode((String) body.get("partnerCode"));
        partner.setPartnerName((String) body.get("partnerName"));
        partner.setPartnerShortName((String) body.get("partnerShortName"));
        partner.setPartnerType((String) body.get("partnerType"));
        partner.setContactPerson((String) body.get("contactPerson"));
        partner.setContactPhone((String) body.get("contactPhone"));
        partner.setContactEmail((String) body.get("contactEmail"));
        partner.setDetailAddress((String) body.get("detailAddress"));
        partner.setUnifiedSocialCode((String) body.get("unifiedSocialCode"));
        partner.setTaxId((String) body.get("taxId"));
        partner.setLegalPerson((String) body.get("legalPerson"));
        partner.setCompanyPhone((String) body.get("companyPhone"));
        partner.setCompanyEmail((String) body.get("companyEmail"));
        partner.setCompanyWebsite((String) body.get("companyWebsite"));
        partner.setSettleType((String) body.get("settleType"));
        partner.setRemark((String) body.get("remark"));
        partner.setStatus((String) body.getOrDefault("status", "ENABLED"));
        if (body.get("taxRate") != null) {
            partner.setTaxRate(new java.math.BigDecimal(body.get("taxRate").toString()));
        }
        if (body.get("creditLimit") != null) {
            partner.setCreditLimit(new java.math.BigDecimal(body.get("creditLimit").toString()));
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) body.get("roles");
        if (roles == null || roles.isEmpty()) {
            roles = List.of(partner.getPartnerType());
        }
        return Result.ok(partnerService.createPartnerWithRoles(partner, roles));
    }

    @Operation(summary = "更新往来单位")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Partner partner) {
        partner.setId(id);
        return Result.ok(partnerService.updatePartner(partner));
    }

    @Operation(summary = "启用/停用")
    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Partner partner = new Partner();
        partner.setId(id);
        partner.setStatus(status);
        return Result.ok(partnerService.updateById(partner));
    }

    @Operation(summary = "删除往来单位")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(partnerService.removeById(id));
    }

    @Operation(summary = "获取下一个编号序号")
    @GetMapping("/next-seq")
    public Result<Map<String, Integer>> getNextSeq(@RequestParam String prefix) {
        Map<String, Integer> result = new HashMap<>();
        result.put("seq", partnerService.getNextSeq(prefix));
        return Result.ok(result);
    }
}
