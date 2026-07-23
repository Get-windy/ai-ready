package cn.aiedge.docquery.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.docquery.service.DocQueryService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 综合单据查询控制器（分析 > 综合单据）
 *
 * <p>跨单据类型聚合查询：经营历程、待审批单据、业务草稿。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/docquery")
@SaCheckLogin
@RequiredArgsConstructor
@Tag(name = "综合单据查询", description = "分析-综合单据：经营历程/待审批单据/业务草稿 跨单据聚合查询")
public class DocQueryController {

    private final DocQueryService docQueryService;

    /**
     * 经营历程分页查询（全部状态的各类单据，按业务日期倒序）
     */
    @GetMapping("/business-history/page")
    @Operation(summary = "经营历程分页查询", description = "UNION 聚合各类单据（销售/采购/收付款/库存/费用），按业务日期倒序分页")
    public Result<Map<String, Object>> businessHistory(
            @Parameter(description = "单据类型代码，如 SALE_ORDER/PURCHASE_ORDER/RECEIPT，空为全部")
            @RequestParam(required = false) String docType,
            @Parameter(description = "单据号模糊匹配") @RequestParam(required = false) String docNo,
            @Parameter(description = "往来单位模糊匹配") @RequestParam(required = false) String partnerName,
            @Parameter(description = "业务日期起 yyyy-MM-dd（含）") @RequestParam(required = false) String startDate,
            @Parameter(description = "业务日期止 yyyy-MM-dd（含）") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，最大100") @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> data = docQueryService.page(currentTenantId(), DocQueryService.StatusMode.ALL,
                docType, docNo, partnerName, startDate, endDate, page, size);
        return Result.ok(data);
    }

    /**
     * 待审批单据分页查询（各表 待审批 状态，附按类型计数汇总）
     */
    @GetMapping("/pending-docs/page")
    @Operation(summary = "待审批单据分页查询", description = "仅取各表待审批状态单据，summary 返回每类待审批数量")
    public Result<Map<String, Object>> pendingDocs(
            @Parameter(description = "单据类型代码，空为全部") @RequestParam(required = false) String docType,
            @Parameter(description = "单据号模糊匹配") @RequestParam(required = false) String docNo,
            @Parameter(description = "往来单位模糊匹配") @RequestParam(required = false) String partnerName,
            @Parameter(description = "业务日期起 yyyy-MM-dd（含）") @RequestParam(required = false) String startDate,
            @Parameter(description = "业务日期止 yyyy-MM-dd（含）") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，最大100") @RequestParam(defaultValue = "10") int size) {
        Long tenantId = currentTenantId();
        Map<String, Object> data = docQueryService.page(tenantId, DocQueryService.StatusMode.PENDING,
                docType, docNo, partnerName, startDate, endDate, page, size);
        List<Map<String, Object>> summary = docQueryService.pendingSummary(tenantId);
        data.put("summary", summary);
        return Result.ok(data);
    }

    /**
     * 业务草稿分页查询（各表 草稿 状态）
     */
    @GetMapping("/draft-docs/page")
    @Operation(summary = "业务草稿分页查询", description = "仅取各表草稿状态单据，按业务日期倒序分页")
    public Result<Map<String, Object>> draftDocs(
            @Parameter(description = "单据类型代码，空为全部") @RequestParam(required = false) String docType,
            @Parameter(description = "单据号模糊匹配") @RequestParam(required = false) String docNo,
            @Parameter(description = "往来单位模糊匹配") @RequestParam(required = false) String partnerName,
            @Parameter(description = "业务日期起 yyyy-MM-dd（含）") @RequestParam(required = false) String startDate,
            @Parameter(description = "业务日期止 yyyy-MM-dd（含）") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，最大100") @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> data = docQueryService.page(currentTenantId(), DocQueryService.StatusMode.DRAFT,
                docType, docNo, partnerName, startDate, endDate, page, size);
        return Result.ok(data);
    }

    /**
     * 获取当前租户ID；Sa-Token Session 中取不到时回退默认租户 1
     */
    private Long currentTenantId() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) {
            log.debug("Sa-Token Session 中无 tenantId，使用默认租户 1");
            return 1L;
        }
        return tenantId;
    }
}
