package cn.aiedge.erp.product.kit.controller;

import cn.aiedge.erp.product.kit.dto.KitDisassemblyCreateDTO;
import cn.aiedge.erp.product.kit.dto.KitDisassemblyVO;
import cn.aiedge.erp.product.kit.entity.KitDisassembly;
import cn.aiedge.erp.product.kit.entity.KitDisassemblyItem;
import cn.aiedge.erp.product.kit.enums.DisassemblyStatus;
import cn.aiedge.erp.product.kit.service.KitDisassemblyService;
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
@RequestMapping("/api/erp/kit-disassembly")
@RequiredArgsConstructor
@Tag(name = "套装拆分管理", description = "拆分单创建、审批、执行、完成等操作")
public class KitDisassemblyController {

    private final KitDisassemblyService kitDisassemblyService;

    @GetMapping("/page")
    @Operation(summary = "分页查询拆分单")
    public Page<KitDisassemblyVO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "套装ID") @RequestParam(required = false) Long kitId,
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<KitDisassembly> page = kitDisassemblyService.pageList(keyword, kitId, warehouseId, status, pageNum, pageSize);
        Page<KitDisassemblyVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取拆分单详情")
    public KitDisassemblyVO getById(@PathVariable Long id) {
        KitDisassembly disassembly = kitDisassemblyService.getById(id);
        if (disassembly == null) {
            throw new RuntimeException("拆分单不存在");
        }
        KitDisassemblyVO vo = convertToVO(disassembly);
        vo.setItems(kitDisassemblyService.getItems(id));
        return vo;
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取拆分明细")
    public List<KitDisassemblyItem> getItems(@PathVariable Long id) {
        return kitDisassemblyService.getItems(id);
    }

    @PostMapping
    @Operation(summary = "创建拆分单")
    public KitDisassemblyVO create(@RequestBody KitDisassemblyCreateDTO dto) {
        KitDisassembly disassembly = new KitDisassembly();
        BeanUtils.copyProperties(dto, disassembly);
        disassembly.setTenantId(1L);
        disassembly.setCreateBy(StpUtil.getLoginIdAsLong());
        KitDisassembly created = kitDisassemblyService.createDisassembly(disassembly);
        return convertToVO(created);
    }

    @PostMapping("/from-kit/{kitId}")
    @Operation(summary = "从套装创建拆分单")
    public KitDisassemblyVO createFromKit(
            @PathVariable Long kitId,
            @RequestParam BigDecimal quantity,
            @RequestParam Long warehouseId,
            @RequestParam(required = false) String batchNo) {
        KitDisassembly disassembly = kitDisassemblyService.createFromKit(kitId, quantity, warehouseId, batchNo);
        return convertToVO(disassembly);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新拆分单")
    public KitDisassemblyVO update(@PathVariable Long id, @RequestBody KitDisassemblyCreateDTO dto) {
        KitDisassembly disassembly = new KitDisassembly();
        BeanUtils.copyProperties(dto, disassembly);
        KitDisassembly updated = kitDisassemblyService.updateDisassembly(id, disassembly);
        return convertToVO(updated);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public KitDisassemblyVO submitForApproval(@PathVariable Long id) {
        KitDisassembly disassembly = kitDisassemblyService.submitForApproval(id);
        return convertToVO(disassembly);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public KitDisassemblyVO approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        KitDisassembly disassembly = kitDisassemblyService.approve(id, approverId, note);
        return convertToVO(disassembly);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public KitDisassemblyVO reject(@PathVariable Long id, @RequestParam String reason) {
        KitDisassembly disassembly = kitDisassemblyService.reject(id, reason);
        return convertToVO(disassembly);
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行拆分")
    public KitDisassemblyVO execute(@PathVariable Long id) {
        Long executorId = StpUtil.getLoginIdAsLong();
        KitDisassembly disassembly = kitDisassemblyService.execute(id, executorId);
        return convertToVO(disassembly);
    }

    @PostMapping("/{id}/items/{itemId}/execute")
    @Operation(summary = "执行拆分明细")
    public KitDisassemblyItem executeItem(
            @PathVariable Long itemId,
            @RequestParam BigDecimal actualQuantity,
            @RequestParam(required = false) String batchNo) {
        return kitDisassemblyService.executeItem(itemId, actualQuantity, batchNo);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成拆分")
    public KitDisassemblyVO complete(@PathVariable Long id) {
        KitDisassembly disassembly = kitDisassemblyService.complete(id);
        return convertToVO(disassembly);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消拆分")
    public KitDisassemblyVO cancel(@PathVariable Long id, @RequestParam String reason) {
        KitDisassembly disassembly = kitDisassemblyService.cancel(id, reason);
        return convertToVO(disassembly);
    }

    @GetMapping("/statistics")
    @Operation(summary = "拆分统计")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new HashMap<>();
        for (DisassemblyStatus status : DisassemblyStatus.values()) {
            stats.put(status.getDesc(), kitDisassemblyService.lambdaQuery()
                    .eq(KitDisassembly::getStatus, status.getCode())
                    .eq(KitDisassembly::getDeleted, 0)
                    .count());
        }
        stats.put("totalDisassemblyCost", kitDisassemblyService.baseMapper.sumDisassemblyCost(1L));
        return stats;
    }

    private KitDisassemblyVO convertToVO(KitDisassembly disassembly) {
        KitDisassemblyVO vo = new KitDisassemblyVO();
        BeanUtils.copyProperties(disassembly, vo);
        for (DisassemblyStatus status : DisassemblyStatus.values()) {
            if (status.getCode().equals(disassembly.getStatus())) {
                vo.setStatusDesc(status.getDesc());
                break;
            }
        }
        return vo;
    }
}