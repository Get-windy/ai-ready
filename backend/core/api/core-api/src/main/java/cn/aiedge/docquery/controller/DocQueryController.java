package cn.aiedge.docquery.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.docquery.dto.DocQueryParams;
import cn.aiedge.docquery.service.DocQueryService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 综合单据查询控制器（分析 &gt; 综合单据）
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
     * 经营历程分页查询（全部状态的各类单据，按单据日期倒序）
     */
    @GetMapping("/business-history/page")
    @Operation(summary = "经营历程分页查询", description = "UNION 聚合各类单据（销售/采购/收付款/库存/费用），按单据日期倒序分页；默认排除已取消单据（显示红冲）")
    public Result<Map<String, Object>> businessHistory(DocQueryParams params) {
        return Result.ok(docQueryService.page(currentTenantId(), DocQueryService.StatusMode.ALL, params));
    }

    /**
     * 待审批单据分页查询（各表 待审批 状态，附按类型计数汇总）
     */
    @GetMapping("/pending-docs/page")
    @Operation(summary = "待审批单据分页查询", description = "仅取各表待审批状态单据，summary 返回每类待审批数量")
    public Result<Map<String, Object>> pendingDocs(DocQueryParams params) {
        Long tenantId = currentTenantId();
        Map<String, Object> data = docQueryService.page(tenantId, DocQueryService.StatusMode.PENDING, params);
        List<Map<String, Object>> pendingSummary = docQueryService.pendingSummary(tenantId);
        data.put("pendingSummary", pendingSummary);
        return Result.ok(data);
    }

    /**
     * 业务草稿分页查询（各表 草稿 状态）
     */
    @GetMapping("/draft-docs/page")
    @Operation(summary = "业务草稿分页查询", description = "仅取各表草稿状态单据，按单据日期倒序分页")
    public Result<Map<String, Object>> draftDocs(DocQueryParams params) {
        return Result.ok(docQueryService.page(currentTenantId(), DocQueryService.StatusMode.DRAFT, params));
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
