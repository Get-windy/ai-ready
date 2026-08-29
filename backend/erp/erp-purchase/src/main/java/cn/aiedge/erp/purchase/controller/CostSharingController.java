package cn.aiedge.erp.purchase.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.purchase.dto.CostSharingCreateRequest;
import cn.aiedge.erp.purchase.dto.CostSharingDetailDTO;
import cn.aiedge.erp.purchase.dto.CostSharingPageDTO;
import cn.aiedge.erp.purchase.service.CostSharingService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 采购费用分摊单控制器
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "采购费用分摊管理", description = "采购费用分摊CRUD+记账/取消接口")
@RestController
@RequestMapping("/api/erp/purchase/cost-sharing")
@RequiredArgsConstructor
public class CostSharingController {

    private final CostSharingService costSharingService;

    /**
     * 分页查询分摊单
     */
    @Operation(summary = "分页查询分摊单")
    @GetMapping("/page")
    @SaCheckPermission("purchase:cost-sharing:list")
    public ApiResponse<Page<CostSharingPageDTO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String sharingNo,
            @RequestParam(required = false) String handlerName,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) String createByName,
            @RequestParam(required = false) String bookkeeperName,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<CostSharingPageDTO> result = costSharingService.pageList(pageNum, pageSize, sharingNo, handlerName,
                departmentName, createByName, bookkeeperName, summary, remark, status, startDate, endDate);
        return ApiResponse.ok(result);
    }

    /**
     * 获取分摊单详情（主表 + 费用单明细 + 入库单分摊明细）
     */
    @Operation(summary = "获取分摊单详情")
    @GetMapping("/{id}")
    @SaCheckPermission("purchase:cost-sharing:detail")
    public ApiResponse<CostSharingDetailDTO> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(costSharingService.getDetail(id));
    }

    /**
     * 获取下一个分摊单号
     */
    @Operation(summary = "获取下一个分摊单号")
    @GetMapping("/next-no")
    @SaCheckLogin
    public ApiResponse<String> nextNo() {
        return ApiResponse.ok(costSharingService.nextNo());
    }

    /**
     * 创建分摊单
     */
    @Operation(summary = "创建分摊单")
    @PostMapping
    @SaCheckPermission("purchase:cost-sharing:create")
    public ApiResponse<Long> create(@RequestBody CostSharingCreateRequest request) {
        Long sharingId = costSharingService.createCostSharing(request);
        return ApiResponse.ok("创建成功", sharingId);
    }

    /**
     * 更新分摊单（仅草稿状态）
     */
    @Operation(summary = "更新分摊单")
    @PutMapping("/{id}")
    @SaCheckPermission("purchase:cost-sharing:update")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody CostSharingCreateRequest request) {
        costSharingService.updateCostSharing(id, request);
        return ApiResponse.ok("更新成功", null);
    }

    /**
     * 完成（记账）分摊单
     */
    @Operation(summary = "记账分摊单")
    @PostMapping("/{id}/complete")
    @SaCheckPermission("purchase:cost-sharing:complete")
    public ApiResponse<Void> complete(@PathVariable Long id) {
        costSharingService.complete(id);
        return ApiResponse.ok("已记账", null);
    }

    /**
     * 取消分摊单
     */
    @Operation(summary = "取消分摊单")
    @PostMapping("/{id}/cancel")
    @SaCheckPermission("purchase:cost-sharing:cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        costSharingService.cancel(id);
        return ApiResponse.ok("已取消", null);
    }
}
