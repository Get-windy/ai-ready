package cn.aiedge.erp.purchase.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.purchase.dto.CostSharingCreateRequest;
import cn.aiedge.erp.purchase.dto.CostSharingPageDTO;
import cn.aiedge.erp.purchase.entity.CostSharing;
import cn.aiedge.erp.purchase.entity.CostSharingItem;
import cn.aiedge.erp.purchase.mapper.CostSharingItemMapper;
import cn.aiedge.erp.purchase.service.CostSharingService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采购费用分摊单控制器
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "采购费用分摊管理", description = "采购费用分摊CRUD+完成/取消接口")
@RestController
@RequestMapping("/api/erp/purchase/cost-sharing")
@RequiredArgsConstructor
public class CostSharingController {

    private final CostSharingService costSharingService;
    private final CostSharingItemMapper costSharingItemMapper;

    /**
     * 分页查询分摊单
     */
    @Operation(summary = "分页查询分摊单")
    @GetMapping("/page")
    @SaCheckPermission("purchase:cost-sharing:list")
    public ApiResponse<Page<CostSharingPageDTO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sharingNo,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<CostSharingPageDTO> result = costSharingService.pageList(page, size, sharingNo, supplierName, startDate, endDate);
        return ApiResponse.ok(result);
    }

    /**
     * 根据ID获取分摊单详情
     */
    @Operation(summary = "获取分摊单详情")
    @GetMapping("/{id}")
    @SaCheckPermission("purchase:cost-sharing:detail")
    public ApiResponse<CostSharing> getById(@PathVariable Long id) {
        CostSharing sharing = costSharingService.getById(id);
        if (sharing == null) {
            return ApiResponse.notFound("分摊单不存在");
        }
        return ApiResponse.ok(sharing);
    }

    /**
     * 获取分摊单明细列表
     */
    @Operation(summary = "获取分摊单明细")
    @GetMapping("/{id}/items")
    @SaCheckPermission("purchase:cost-sharing:detail")
    public ApiResponse<List<CostSharingItem>> getItems(@PathVariable Long id) {
        LambdaQueryWrapper<CostSharingItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CostSharingItem::getCostSharingId, id);
        List<CostSharingItem> items = costSharingItemMapper.selectList(wrapper);
        return ApiResponse.ok(items);
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
     * 更新分摊单
     */
    @Operation(summary = "更新分摊单")
    @PutMapping("/{id}")
    @SaCheckPermission("purchase:cost-sharing:update")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody CostSharing sharing) {
        sharing.setId(id);
        costSharingService.updateById(sharing);
        return ApiResponse.ok("更新成功", null);
    }

    /**
     * 完成分摊单
     */
    @Operation(summary = "完成分摊单")
    @PostMapping("/{id}/complete")
    @SaCheckPermission("purchase:cost-sharing:complete")
    public ApiResponse<Void> complete(@PathVariable Long id) {
        costSharingService.complete(id);
        return ApiResponse.ok("已完成", null);
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
