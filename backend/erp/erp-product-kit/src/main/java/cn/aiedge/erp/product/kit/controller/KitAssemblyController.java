package cn.aiedge.erp.product.kit.controller;

import cn.aiedge.erp.product.kit.dto.KitAssemblyCreateDTO;
import cn.aiedge.erp.product.kit.dto.KitAssemblyVO;
import cn.aiedge.erp.product.kit.entity.KitAssembly;
import cn.aiedge.erp.product.kit.entity.KitAssemblyItem;
import cn.aiedge.erp.product.kit.enums.AssemblyStatus;
import cn.aiedge.erp.product.kit.service.KitAssemblyService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/erp/kit-assembly")
@RequiredArgsConstructor
@Tag(name = "套装组装管理", description = "组装单创建、审批、执行、完成等操作")
public class KitAssemblyController {

    private final KitAssemblyService kitAssemblyService;

    @GetMapping("/page")
    @Operation(summary = "分页查询组装单")
    public Page<KitAssemblyVO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "套装ID") @RequestParam(required = false) Long kitId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<KitAssembly> page = kitAssemblyService.pageList(keyword, kitId, warehouseId, status, pageNum, pageSize);
        Page<KitAssemblyVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取组装单详情")
    public KitAssemblyVO getById(@PathVariable Long id) {
        KitAssembly assembly = kitAssemblyService.getById(id);
        if (assembly == null) {
            throw new RuntimeException("组装单不存在");
        }
        KitAssemblyVO vo = convertToVO(assembly);
        vo.setItems(kitAssemblyService.getItems(id));
        return vo;
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取组装明细")
    public List<KitAssemblyItem> getItems(@PathVariable Long id) {
        return kitAssemblyService.getItems(id);
    }

    @PostMapping
    @Operation(summary = "创建组装单")
    public KitAssemblyVO create(@RequestBody KitAssemblyCreateDTO dto) {
        KitAssembly assembly = new KitAssembly();
        BeanUtils.copyProperties(dto, assembly);
        assembly.setTenantId(1L);
        assembly.setCreateBy(StpUtil.getLoginIdAsLong());
        KitAssembly created = kitAssemblyService.createAssembly(assembly);
        return convertToVO(created);
    }

    @PostMapping("/from-kit/{kitId}")
    @Operation(summary = "从套装创建组装单")
    public KitAssemblyVO createFromKit(
            @PathVariable Long kitId,
            @RequestParam BigDecimal quantity,
            @RequestParam Long warehouseId) {
        KitAssembly assembly = kitAssemblyService.createFromKit(kitId, quantity, warehouseId);
        return convertToVO(assembly);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新组装单")
    public KitAssemblyVO update(@PathVariable Long id, @RequestBody KitAssemblyCreateDTO dto) {
        KitAssembly assembly = new KitAssembly();
        BeanUtils.copyProperties(dto, assembly);
        KitAssembly updated = kitAssemblyService.updateAssembly(id, assembly);
        return convertToVO(updated);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public KitAssemblyVO submitForApproval(@PathVariable Long id) {
        KitAssembly assembly = kitAssemblyService.submitForApproval(id);
        return convertToVO(assembly);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public KitAssemblyVO approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        KitAssembly assembly = kitAssemblyService.approve(id, approverId, note);
        return convertToVO(assembly);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public KitAssemblyVO reject(@PathVariable Long id, @RequestParam String reason) {
        KitAssembly assembly = kitAssemblyService.reject(id, reason);
        return convertToVO(assembly);
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行组装")
    public KitAssemblyVO execute(@PathVariable Long id) {
        Long executorId = StpUtil.getLoginIdAsLong();
        KitAssembly assembly = kitAssemblyService.execute(id, executorId);
        return convertToVO(assembly);
    }

    @PostMapping("/{id}/items/{itemId}/execute")
    @Operation(summary = "执行组装明细")
    public KitAssemblyItem executeItem(
            @PathVariable Long itemId,
            @RequestParam BigDecimal actualQuantity,
            @RequestParam(required = false) String batchNo) {
        return kitAssemblyService.executeItem(itemId, actualQuantity, batchNo);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成组装")
    public KitAssemblyVO complete(@PathVariable Long id) {
        KitAssembly assembly = kitAssemblyService.complete(id);
        return convertToVO(assembly);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消组装")
    public KitAssemblyVO cancel(@PathVariable Long id, @RequestParam String reason) {
        KitAssembly assembly = kitAssemblyService.cancel(id, reason);
        return convertToVO(assembly);
    }

    @GetMapping("/statistics")
    @Operation(summary = "组装统计")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new HashMap<>();
        for (AssemblyStatus status : AssemblyStatus.values()) {
            stats.put(status.getDesc(), kitAssemblyService.lambdaQuery()
                    .eq(KitAssembly::getStatus, status.getCode())
                    .eq(KitAssembly::getDeleted, 0)
                    .count());
        }
        stats.put("totalAssemblyCost", kitAssemblyService.baseMapper.sumAssemblyCost(1L));
        return stats;
    }

    private KitAssemblyVO convertToVO(KitAssembly assembly) {
        KitAssemblyVO vo = new KitAssemblyVO();
        BeanUtils.copyProperties(assembly, vo);
        for (AssemblyStatus status : AssemblyStatus.values()) {
            if (status.getCode().equals(assembly.getStatus())) {
                vo.setStatusDesc(status.getDesc());
                break;
            }
        }
        return vo;
    }
}