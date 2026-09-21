package cn.aiedge.erp.purchase.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.purchase.dto.ContractStatisticsDTO;
import cn.aiedge.erp.purchase.dto.PurchaseContractQueryDTO;
import cn.aiedge.erp.purchase.entity.PurchaseContract;
import cn.aiedge.erp.purchase.service.PurchaseContractService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 采购合同Controller - RESTful API（菜单 81010「采购合同」）
 *
 * <p><b>2026-09-21 补齐</b>：该菜单此前是「菜单在、页面在、接口缺、表缺」的四重断点 ——
 * 前端调用的 {@code /page}、{@code POST /}、{@code PUT /{id}}、{@code DELETE /{id}}、
 * {@code /export} 五个端点后端**都不存在**，且 {@code purchase_contract} 表在库中也不存在。</p>
 *
 * <p>本轮按用户决议「保留 ERP 侧合同能力」补建表 + 补端点；
 * 权限码命名对齐同域既有约定 {@code purchase:<资源>:<动作>}（见 {@code purchase:order:*}）。</p>
 *
 * <p>⚠️ 返回类型统一改为 {@code ApiResponse<T>}：原实现直接返回裸实体
 * （{@code ResponseEntity<PurchaseContract>}），而前端
 * {@code api/purchase-contract.ts} 的类型是 {@code ApiResponse<PurchaseContract>}
 * ⇒ 即便端点存在，前端也取不到 {@code data}。</p>
 */
@Tag(name = "采购合同")
@RestController
@RequestMapping("/api/erp/purchase/contract")
@RequiredArgsConstructor
public class PurchaseContractController {

    private final PurchaseContractService contractService;

    /**
     * 分页查询合同（列表页主查询）
     */
    @Operation(summary = "分页查询采购合同")
    @GetMapping("/page")
    @SaCheckPermission("purchase:contract:list")
    public ApiResponse<Page<PurchaseContract>> page(PurchaseContractQueryDTO query) {
        return ApiResponse.ok(contractService.pageContracts(query));
    }

    /**
     * 获取合同详情
     */
    @Operation(summary = "获取采购合同详情")
    @GetMapping("/{id}")
    @SaCheckPermission("purchase:contract:detail")
    public ApiResponse<PurchaseContract> getContractById(@PathVariable Long id) {
        return ApiResponse.ok(contractService.getContractById(id));
    }

    /**
     * 根据合同号查询合同
     */
    @Operation(summary = "按合同号查询")
    @GetMapping("/by-no/{contractNo}")
    @SaCheckPermission("purchase:contract:detail")
    public ApiResponse<PurchaseContract> getContractByNo(@PathVariable String contractNo) {
        return ApiResponse.ok(contractService.getContractByNo(contractNo));
    }

    /**
     * 根据供应商查询合同
     */
    @Operation(summary = "按供应商查询合同")
    @GetMapping("/supplier/{supplierId}")
    @SaCheckPermission("purchase:contract:detail")
    public ApiResponse<List<PurchaseContract>> queryContractsBySupplier(@PathVariable Long supplierId) {
        return ApiResponse.ok(contractService.queryContractsBySupplier(supplierId));
    }

    /**
     * 新增合同
     */
    @Operation(summary = "新增采购合同")
    @PostMapping
    @SaCheckPermission("purchase:contract:create")
    public ApiResponse<PurchaseContract> createContract(@RequestBody PurchaseContract contract) {
        return ApiResponse.ok("创建成功", contractService.createContract(contract));
    }

    /**
     * 编辑合同
     */
    @Operation(summary = "编辑采购合同")
    @PutMapping("/{id}")
    @SaCheckPermission("purchase:contract:update")
    public ApiResponse<PurchaseContract> updateContract(@PathVariable Long id,
                                                        @RequestBody PurchaseContract contract) {
        return ApiResponse.ok("更新成功", contractService.updateContract(id, contract));
    }

    /**
     * 删除合同（仅草稿）
     */
    @Operation(summary = "删除采购合同")
    @DeleteMapping("/{id}")
    @SaCheckPermission("purchase:contract:delete")
    public ApiResponse<Void> deleteContract(@PathVariable Long id) {
        contractService.deleteContract(id);
        return ApiResponse.ok("删除成功", null);
    }

    /**
     * 提交审批
     */
    @Operation(summary = "提交审批")
    @PostMapping("/{id}/submit")
    @SaCheckPermission("purchase:contract:update")
    public ApiResponse<PurchaseContract> submitForApproval(@PathVariable Long id,
                                                           @RequestParam String reason) {
        return ApiResponse.ok("提交成功", contractService.submitForApproval(id, reason));
    }

    /**
     * 审批合同
     */
    @Operation(summary = "审批采购合同")
    @PostMapping("/{id}/approve")
    @SaCheckPermission("purchase:contract:approve")
    public ApiResponse<PurchaseContract> approveContract(@PathVariable Long id,
                                                         @RequestParam Long approverId,
                                                         @RequestParam String comment,
                                                         @RequestParam boolean approved) {
        return ApiResponse.ok(contractService.approveContract(id, approverId, comment, approved));
    }

    /**
     * 激活合同
     */
    @Operation(summary = "激活采购合同")
    @PostMapping("/{id}/activate")
    @SaCheckPermission("purchase:contract:approve")
    public ApiResponse<PurchaseContract> activateContract(@PathVariable Long id) {
        return ApiResponse.ok(contractService.activateContract(id));
    }

    /**
     * 终止合同
     */
    @Operation(summary = "终止采购合同")
    @PostMapping("/{id}/terminate")
    @SaCheckPermission("purchase:contract:approve")
    public ApiResponse<PurchaseContract> terminateContract(@PathVariable Long id,
                                                           @RequestParam String reason) {
        return ApiResponse.ok(contractService.terminateContract(id, reason));
    }

    /**
     * 归档合同
     */
    @Operation(summary = "归档采购合同")
    @PostMapping("/{id}/archive")
    @SaCheckPermission("purchase:contract:approve")
    public ApiResponse<PurchaseContract> archiveContract(@PathVariable Long id,
                                                         @RequestParam String archiveNo,
                                                         @RequestParam String reason) {
        return ApiResponse.ok(contractService.archiveContract(id, archiveNo, reason));
    }

    /**
     * 获取合同统计（列表页顶部卡片）
     */
    @Operation(summary = "采购合同统计")
    @GetMapping("/statistics")
    @SaCheckPermission("purchase:contract:list")
    public ApiResponse<ContractStatisticsDTO> generateContractStatistics() {
        return ApiResponse.ok(contractService.generateContractStatistics());
    }

    /**
     * 导出合同列表为 CSV（按当前查询条件，不分页）。
     *
     * <p>前端 {@code purchaseContractApi.export()} 以 blob 接收，故直接返回字节流，
     * 不套 {@code ApiResponse}。加了 BOM 以便 Excel 正确识别 UTF-8 中文。</p>
     */
    @Operation(summary = "导出采购合同")
    @GetMapping("/export")
    @SaCheckPermission("purchase:contract:export")
    public ResponseEntity<byte[]> export(PurchaseContractQueryDTO query) {
        // 复用分页查询：把页大小放到上限，拿到符合条件的全部行
        query.setCurrent(1L);
        query.setSize(10000L);
        List<PurchaseContract> rows = contractService.pageContracts(query).getRecords();

        StringBuilder sb = new StringBuilder();
        sb.append("合同编号,合同标题,供应商,合同类型,状态,合同金额,已执行金额,开始日期,结束日期,备注\n");
        for (PurchaseContract c : rows) {
            sb.append(csv(c.getContractNo())).append(',')
              .append(csv(c.getContractTitle())).append(',')
              .append(csv(c.getSupplierName())).append(',')
              .append(csv(c.getContractType())).append(',')
              .append(csv(c.getContractStatus() == null ? "" : c.getContractStatus().name())).append(',')
              .append(c.getTotalAmount() == null ? "" : c.getTotalAmount()).append(',')
              .append(c.getExecutedAmount() == null ? "" : c.getExecutedAmount()).append(',')
              .append(c.getStartDate() == null ? "" : c.getStartDate().toLocalDate()).append(',')
              .append(c.getEndDate() == null ? "" : c.getEndDate().toLocalDate()).append(',')
              .append(csv(c.getRemark())).append('\n');
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        out.writeBytes(sb.toString().getBytes(StandardCharsets.UTF_8));

        String filename = "purchase-contract.csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(out.toByteArray());
    }

    /** CSV 字段转义：含逗号/引号/换行时用双引号包裹并转义内部引号 */
    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
