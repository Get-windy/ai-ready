package cn.aiedge.erp.finance.otherincome.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.finance.otherincome.dto.OtherIncomeCreateDTO;
import cn.aiedge.erp.finance.otherincome.dto.OtherIncomeItemDetailVO;
import cn.aiedge.erp.finance.otherincome.dto.OtherIncomeVO;
import cn.aiedge.erp.finance.otherincome.service.OtherIncomeDocService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 其他收入单控制器（金标准）
 * 非主营收入登记，与费用单对称。
 */
@Tag(name = "其他收入单管理", description = "其他收入单CRUD、记账、分页查询")
@RestController
@RequestMapping("/api/erp/finance/other-income-doc")
@RequiredArgsConstructor
public class OtherIncomeDocController {

    private final OtherIncomeDocService otherIncomeDocService;

    @Operation(summary = "分页查询其他收入单（按单据）")
    @GetMapping("/page")
    @SaCheckPermission("finance:other-income-doc:view")
    @OperationLog(module = "其他收入单", type = "QUERY", desc = "分页查询其他收入单")
    public ApiResponse<Page<OtherIncomeVO>> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "单据编号") @RequestParam(required = false) String docNo,
            @Parameter(description = "往来单位") @RequestParam(required = false) String partnerName,
            @Parameter(description = "经手人") @RequestParam(required = false) String handlerName,
            @Parameter(description = "部门") @RequestParam(required = false) String departmentName,
            @Parameter(description = "制单人") @RequestParam(required = false) String creatorName,
            @Parameter(description = "记账人") @RequestParam(required = false) String bookkeeperName,
            @Parameter(description = "单据状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "结算状态") @RequestParam(required = false) Integer settleStatus,
            @Parameter(description = "收入科目") @RequestParam(required = false) String incomeSubject,
            @Parameter(description = "摘要") @RequestParam(required = false) String summary,
            @Parameter(description = "单据备注") @RequestParam(required = false) String remark,
            @Parameter(description = "显示红冲") @RequestParam(required = false) Integer showRed,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<OtherIncomeVO> page = otherIncomeDocService.pageList(
                keyword, docNo, partnerName, handlerName, departmentName, creatorName, bookkeeperName,
                status, settleStatus, incomeSubject, summary, remark, showRed,
                parse(startDate), parse(endDate), pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @Operation(summary = "分页查询其他收入单（按明细）")
    @GetMapping("/page-detail")
    @SaCheckPermission("finance:other-income-doc:view")
    @OperationLog(module = "其他收入单", type = "QUERY", desc = "按明细分页查询其他收入单")
    public ApiResponse<Page<OtherIncomeItemDetailVO>> pageDetail(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "单据编号") @RequestParam(required = false) String docNo,
            @Parameter(description = "往来单位") @RequestParam(required = false) String partnerName,
            @Parameter(description = "经手人") @RequestParam(required = false) String handlerName,
            @Parameter(description = "部门") @RequestParam(required = false) String departmentName,
            @Parameter(description = "制单人") @RequestParam(required = false) String creatorName,
            @Parameter(description = "记账人") @RequestParam(required = false) String bookkeeperName,
            @Parameter(description = "单据状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "收入科目") @RequestParam(required = false) String incomeSubject,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<OtherIncomeItemDetailVO> page = otherIncomeDocService.pageDetail(
                keyword, docNo, partnerName, handlerName, departmentName, creatorName, bookkeeperName,
                status, incomeSubject, parse(startDate), parse(endDate), pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @Operation(summary = "生成下一个其他收入单号（QTSRD-）")
    @GetMapping("/next-no")
    @SaCheckPermission("finance:other-income-doc:create")
    @OperationLog(module = "其他收入单", type = "QUERY", desc = "生成下一个其他收入单号")
    public ApiResponse<String> nextNo() {
        return ApiResponse.success(otherIncomeDocService.nextNo());
    }

    @Operation(summary = "获取其他收入单详情")
    @GetMapping("/{id}")
    @SaCheckPermission("finance:other-income-doc:view")
    @OperationLog(module = "其他收入单", type = "QUERY", desc = "获取其他收入单详情")
    public ApiResponse<OtherIncomeVO> getById(@PathVariable Long id) {
        return ApiResponse.success(otherIncomeDocService.getDetail(id));
    }

    @Operation(summary = "保存草稿")
    @PostMapping("/save-draft")
    @SaCheckPermission("finance:other-income-doc:create")
    @OperationLog(module = "其他收入单", type = "CREATE", desc = "保存草稿")
    public ApiResponse<OtherIncomeVO> saveDraft(@RequestBody OtherIncomeCreateDTO dto) {
        return ApiResponse.success(otherIncomeDocService.saveDoc(null, dto, false));
    }

    @Operation(summary = "更新草稿")
    @PutMapping("/{id}")
    @SaCheckPermission("finance:other-income-doc:update")
    @OperationLog(module = "其他收入单", type = "UPDATE", desc = "更新草稿")
    public ApiResponse<OtherIncomeVO> update(@PathVariable Long id, @RequestBody OtherIncomeCreateDTO dto) {
        return ApiResponse.success(otherIncomeDocService.saveDoc(id, dto, false));
    }

    @Operation(summary = "记账（生成凭证，入总账/明细账）")
    @PostMapping("/{id}/confirm")
    @SaCheckPermission("finance:other-income-doc:update")
    @OperationLog(module = "其他收入单", type = "UPDATE", desc = "记账")
    public ApiResponse<OtherIncomeVO> confirm(@PathVariable Long id) {
        return ApiResponse.success(otherIncomeDocService.confirm(id));
    }

    @Operation(summary = "删除其他收入单")
    @DeleteMapping("/{id}")
    @SaCheckPermission("finance:other-income-doc:delete")
    @OperationLog(module = "其他收入单", type = "DELETE", desc = "删除其他收入单")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        otherIncomeDocService.removeDoc(id);
        return ApiResponse.success(true);
    }

    private LocalDate parse(String date) {
        if (date == null || date.isEmpty()) {
            return null;
        }
        return LocalDate.parse(date);
    }
}
