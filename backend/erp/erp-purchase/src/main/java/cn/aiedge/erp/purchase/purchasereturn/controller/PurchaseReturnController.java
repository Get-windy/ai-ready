package cn.aiedge.erp.purchase.purchasereturn.controller;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.purchase.purchasereturn.dto.PurchaseReturnDTO;
import cn.aiedge.erp.purchase.purchasereturn.dto.PurchaseReturnItemDTO;
import cn.aiedge.erp.purchase.purchasereturn.dto.PurchaseReturnVO;
import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturn;
import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturnItem;
import cn.aiedge.erp.purchase.purchasereturn.enums.ReturnStatus;
import cn.aiedge.erp.purchase.purchasereturn.enums.ReturnType;
import cn.aiedge.erp.purchase.purchasereturn.mapper.PurchaseReturnMapper;
import cn.aiedge.erp.purchase.purchasereturn.service.PurchaseReturnService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@RestController
@RequestMapping("/api/erp/purchase/return")
@RequiredArgsConstructor
@Tag(name = "采购退货管理", description = "采购退货单创建、审批、出库完成、取消等操作")
public class PurchaseReturnController {

    private final PurchaseReturnService purchaseReturnService;
    private final PurchaseReturnMapper purchaseReturnMapper;

    @SaCheckPermission("purchase:return:list")
    @GetMapping("/page")
    @Operation(summary = "分页查询退货单")
    public Page<PurchaseReturnVO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "源订单ID") @RequestParam(required = false) Long purchaseOrderId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "结算状态") @RequestParam(required = false) Integer settleStatus,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<PurchaseReturn> page = purchaseReturnService.pageList(keyword, supplierId, purchaseOrderId, warehouseId, status, settleStatus, pageNum, pageSize);
        Page<PurchaseReturnVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @SaCheckPermission("purchase:return:list")
    @GetMapping("/next-no")
    @Operation(summary = "生成下一退货单号")
    public String nextNo() {
        return purchaseReturnService.generateReturnNo();
    }

    @SaCheckPermission("purchase:return:detail")
    @GetMapping("/{id}")
    @Operation(summary = "获取退货单详情")
    public PurchaseReturnVO getById(@PathVariable Long id) {
        PurchaseReturn ret = purchaseReturnService.getById(id);
        if (ret == null) {
            throw new RuntimeException("退货单不存在");
        }
        PurchaseReturnVO vo = convertToVO(ret);
        vo.setItems(purchaseReturnService.getItems(id));
        return vo;
    }

    @SaCheckPermission("purchase:return:list")
    @GetMapping("/{id}/items")
    @Operation(summary = "获取退货明细")
    public List<PurchaseReturnItem> getItems(@PathVariable Long id) {
        return purchaseReturnService.getItems(id);
    }

    @SaCheckPermission("purchase:return:detail")
    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "获取供应商的退货单列表")
    public List<PurchaseReturnVO> listBySupplierId(@PathVariable Long supplierId) {
        return purchaseReturnService.listBySupplierId(supplierId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @SaCheckPermission("purchase:return:detail")
    @GetMapping("/order/{orderId}")
    @Operation(summary = "获取源订单的退货单列表")
    public List<PurchaseReturnVO> listByOrderId(@PathVariable Long orderId) {
        return purchaseReturnService.listByOrderId(orderId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @SaCheckPermission("purchase:return:create")
    @PostMapping
    @Operation(summary = "创建退货单")
    public PurchaseReturnVO create(@RequestBody PurchaseReturnDTO dto) {
        PurchaseReturn ret = new PurchaseReturn();
        BeanUtils.copyProperties(dto, ret);
        // 租户取会话，不能写死：写死 1 会让租户 2 新建的单据落成租户 1（自己查不到、还污染别人）
        ret.setTenantId(MyBatisPlusConfig.getCurrentTenantIdValue());
        ret.setCreateBy(StpUtil.getLoginIdAsLong());
        List<PurchaseReturnItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                PurchaseReturnItem item = new PurchaseReturnItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
        PurchaseReturn created = purchaseReturnService.createReturn(ret, items);
        return convertToVO(created);
    }

    @SaCheckPermission("purchase:return:create")
    @PostMapping("/from-order/{orderId}")
    @Operation(summary = "从采购订单创建退货单")
    public PurchaseReturnVO createFromOrder(@PathVariable Long orderId) {
        PurchaseReturn ret = purchaseReturnService.createFromOrder(orderId);
        return convertToVO(ret);
    }

    @SaCheckPermission("purchase:return:update")
    @PutMapping("/{id}")
    @Operation(summary = "更新退货单")
    public PurchaseReturnVO update(@PathVariable Long id, @RequestBody PurchaseReturnDTO dto) {
        PurchaseReturn ret = new PurchaseReturn();
        BeanUtils.copyProperties(dto, ret);
        List<PurchaseReturnItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                PurchaseReturnItem item = new PurchaseReturnItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
        PurchaseReturn updated = purchaseReturnService.updateReturn(id, ret, items);
        return convertToVO(updated);
    }

    @SaCheckPermission("purchase:return:submit")
    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public PurchaseReturnVO submitForApproval(@PathVariable Long id) {
        PurchaseReturn ret = purchaseReturnService.submitForApproval(id);
        return convertToVO(ret);
    }

    @SaCheckPermission("purchase:return:approve")
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public PurchaseReturnVO approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        PurchaseReturn ret = purchaseReturnService.approve(id, approverId, note);
        return convertToVO(ret);
    }

    @SaCheckPermission("purchase:return:approve")
    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public PurchaseReturnVO reject(@PathVariable Long id, @RequestParam String reason) {
        PurchaseReturn ret = purchaseReturnService.reject(id, reason);
        return convertToVO(ret);
    }

    @SaCheckPermission("purchase:return:complete")
    @PostMapping("/{id}/complete")
    @Operation(summary = "完成退货出库")
    public PurchaseReturnVO complete(@PathVariable Long id) {
        PurchaseReturn ret = purchaseReturnService.complete(id);
        return convertToVO(ret);
    }

    @SaCheckPermission("purchase:return:cancel")
    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消退货单")
    public PurchaseReturnVO cancel(@PathVariable Long id, @RequestParam String reason) {
        PurchaseReturn ret = purchaseReturnService.cancel(id, reason);
        return convertToVO(ret);
    }

    @SaCheckPermission("purchase:return:delete")
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除退货单")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return purchaseReturnService.removeBatchByIds(ids);
    }

    @SaCheckPermission("purchase:return:export")
    @GetMapping("/export")
    @Operation(summary = "导出退货单列表")
    public List<PurchaseReturnVO> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "源订单ID") @RequestParam(required = false) Long purchaseOrderId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        return purchaseReturnService.exportList(keyword, supplierId, purchaseOrderId, warehouseId, status).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @SaCheckPermission("purchase:return:print")
    @PostMapping("/batch-print")
    @Operation(summary = "批量打印退货单")
    public ApiResponse<Void> batchPrint(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) params.get("ids");
        List<Long> ids = rawIds != null ? rawIds.stream().map(Number::longValue).toList() : List.of();
        String template = params.get("template") != null ? params.get("template").toString() : "default";
        purchaseReturnService.batchPrint(ids, template);
        return ApiResponse.ok("打印完成", null);
    }

    @SaCheckPermission("purchase:return:update")
    @PostMapping("/{id}/items")
    @Operation(summary = "添加退货明细")
    public PurchaseReturnItem addItem(@PathVariable Long id, @RequestBody PurchaseReturnItemDTO dto) {
        PurchaseReturnItem item = new PurchaseReturnItem();
        BeanUtils.copyProperties(dto, item);
        return purchaseReturnService.addItem(id, item);
    }

    @SaCheckPermission("purchase:return:update")
    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "更新退货明细")
    public PurchaseReturnItem updateItem(@PathVariable Long itemId, @RequestBody PurchaseReturnItemDTO dto) {
        PurchaseReturnItem item = new PurchaseReturnItem();
        BeanUtils.copyProperties(dto, item);
        return purchaseReturnService.updateItem(itemId, item);
    }

    @SaCheckPermission("purchase:return:delete")
    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除退货明细")
    public void removeItem(@PathVariable Long itemId) {
        purchaseReturnService.removeItem(itemId);
    }

    @SaCheckPermission("purchase:return:view")
    @GetMapping("/statistics")
    @Operation(summary = "退货统计")
    public ApiResponse<Map<String, Object>> statistics() {
        Map<String, Object> stats = new HashMap<>();
        for (ReturnStatus status : ReturnStatus.values()) {
            stats.put(status.getDesc(), purchaseReturnService.lambdaQuery()
                    .eq(PurchaseReturn::getStatus, status.getCode())
                    .eq(PurchaseReturn::getDeleted, 0)
                    .count());
        }
        stats.put("totalReturnAmount", purchaseReturnMapper.sumReturnAmount(1L));
        return ApiResponse.ok(stats);
    }

    private PurchaseReturnVO convertToVO(PurchaseReturn ret) {
        PurchaseReturnVO vo = new PurchaseReturnVO();
        BeanUtils.copyProperties(ret, vo);
        for (ReturnStatus status : ReturnStatus.values()) {
            if (status.getCode().equals(ret.getStatus())) {
                vo.setStatusDesc(status.getDesc());
                break;
            }
        }
        if (ret.getReturnType() != null) {
            for (ReturnType type : ReturnType.values()) {
                if (type.getCode().equals(ret.getReturnType())) {
                    vo.setReturnTypeDesc(type.getDesc());
                    break;
                }
            }
        }
        return vo;
    }
}
