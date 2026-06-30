package cn.aiedge.erp.b2b.controller;

import cn.aiedge.erp.b2b.dto.ApiResponse;
import cn.aiedge.erp.b2b.dto.IdentityLinkDTO;
import cn.aiedge.erp.b2b.dto.PartyLinkApplyRequest;
import cn.aiedge.erp.b2b.service.MallPartyLinkService;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mall/party-link")
@Tag(name = "企业身份关联", description = "用户申请/审批/撤销企业身份关联")
@RequiredArgsConstructor
public class MallPartyLinkController {

    private final MallPartyLinkService mallPartyLinkService;

    // ── 用户侧 ──

    @Operation(summary = "申请关联企业身份", description = "用户提交申请，需企业管理员和租户先后审批")
    @PostMapping("/apply")
    public ApiResponse<Void> apply(@RequestBody PartyLinkApplyRequest request) {
        mallPartyLinkService.apply(request);
        return ApiResponse.success("申请已提交，等待企业管理员审批", null);
    }

    @Operation(summary = "撤回申请", description = "用户撤回尚未审批的申请")
    @PostMapping("/{linkId}/withdraw")
    public ApiResponse<Void> withdraw(@PathVariable Long linkId) {
        mallPartyLinkService.withdraw(linkId);
        return ApiResponse.success("已撤回", null);
    }

    @Operation(summary = "我的关联列表", description = "查询当前用户的所有企业身份关联（含审批进度）")
    @GetMapping("/my")
    public ApiResponse<List<IdentityLinkDTO>> listMyLinks() {
        return ApiResponse.success("获取成功", mallPartyLinkService.listMyLinks());
    }

    // ── 企业客户管理员侧 ──

    @Operation(summary = "企业待审批列表", description = "企业管理员查看本企业的待审批申请")
    @GetMapping("/enterprise/pending")
    public ApiResponse<List<IdentityLinkDTO>> listEnterprisePending(@RequestParam Long partyId) {
        return ApiResponse.success("获取成功",
                mallPartyLinkService.listByParty(partyId, 0));  // status=0 待企业审批
    }

    @Operation(summary = "企业管理员审批通过", description = "第一步审批，通过后转租户审批")
    @PostMapping("/{linkId}/enterprise-approve")
    public ApiResponse<Void> enterpriseApprove(
            @PathVariable Long linkId,
            @RequestParam(required = false) String remark) {
        Long approverId = StpUtil.getLoginIdAsLong();
        mallPartyLinkService.enterpriseApprove(linkId, approverId, remark);
        return ApiResponse.success("已通过，等待平台审批", null);
    }

    @Operation(summary = "企业管理员驳回")
    @PostMapping("/{linkId}/enterprise-reject")
    public ApiResponse<Void> enterpriseReject(
            @PathVariable Long linkId,
            @RequestParam(required = false) String reason) {
        Long approverId = StpUtil.getLoginIdAsLong();
        mallPartyLinkService.enterpriseReject(linkId, approverId, reason);
        return ApiResponse.success("已驳回", null);
    }

    @Operation(summary = "企业管理员撤销已通过的关联", description = "员工离职时，企业管理员可撤销其身份关联")
    @PostMapping("/{linkId}/enterprise-revoke")
    public ApiResponse<Void> enterpriseRevoke(
            @PathVariable Long linkId,
            @RequestParam(required = false) String reason) {
        Long revokerId = StpUtil.getLoginIdAsLong();
        mallPartyLinkService.revoke(linkId, revokerId, "ENTERPRISE", reason);
        return ApiResponse.success("已撤销，该用户无法再以本企业身份下单", null);
    }

    // ── 租户管理员侧（后台调用，通过内部鉴权） ──

    @Operation(summary = "租户待审批列表")
    @GetMapping("/tenant/pending")
    public ApiResponse<List<IdentityLinkDTO>> listTenantPending() {
        // 查 status=1（待租户审批）的所有记录
        return ApiResponse.success("获取成功",
                mallPartyLinkService.listByParty(null, 1));
    }

    @Operation(summary = "租户管理员审批通过", description = "第二步审批，最终通过，关联正式生效")
    @PostMapping("/{linkId}/tenant-approve")
    public ApiResponse<Void> tenantApprove(
            @PathVariable Long linkId,
            @RequestParam(required = false) String remark) {
        // 租户管理员 ID 从当前登录用户获取（实际生产中应区分后台管理员和商城用户）
        Long tenantUserId = StpUtil.getLoginIdAsLong();
        mallPartyLinkService.tenantApprove(linkId, tenantUserId, remark);
        return ApiResponse.success("审批通过，关联已生效", null);
    }

    @Operation(summary = "租户管理员驳回")
    @PostMapping("/{linkId}/tenant-reject")
    public ApiResponse<Void> tenantReject(
            @PathVariable Long linkId,
            @RequestParam(required = false) String reason) {
        Long tenantUserId = StpUtil.getLoginIdAsLong();
        mallPartyLinkService.tenantReject(linkId, tenantUserId, reason);
        return ApiResponse.success("已驳回", null);
    }

    @Operation(summary = "租户管理员撤销已通过的关联")
    @PostMapping("/{linkId}/tenant-revoke")
    public ApiResponse<Void> tenantRevoke(
            @PathVariable Long linkId,
            @RequestParam(required = false) String reason) {
        Long revokerId = StpUtil.getLoginIdAsLong();
        mallPartyLinkService.revoke(linkId, revokerId, "TENANT", reason);
        return ApiResponse.success("已撤销", null);
    }
}
