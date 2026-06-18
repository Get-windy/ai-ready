package cn.aiedge.erp.payment.controller;

import cn.aiedge.erp.payment.dto.CapitalFlowDTO;
import cn.aiedge.erp.payment.entity.CapitalFlow;
import cn.aiedge.erp.payment.service.CapitalFlowService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/erp/capital-flow")
@RequiredArgsConstructor
@Tag(name = "资金流水台账", description = "资金流水查询、导出")
public class CapitalFlowController {

    private final CapitalFlowService capitalFlowService;

    @GetMapping("/page")
    @Operation(summary = "分页查询资金流水")
    public Page<CapitalFlowDTO> page(
            @Parameter(description = "流水类型") @RequestParam(required = false) String flowType,
            @Parameter(description = "方向: IN/OUT") @RequestParam(required = false) String direction,
            @Parameter(description = "对方类型") @RequestParam(required = false) String partyType,
            @Parameter(description = "对方ID") @RequestParam(required = false) Long partyId,
            @Parameter(description = "开始日期") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<CapitalFlow> page = capitalFlowService.pageList(flowType, direction, partyType, partyId,
                startDate, endDate, pageNum, pageSize);
        Page<CapitalFlowDTO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList()));
        return voPage;
    }

    @GetMapping("/export")
    @Operation(summary = "导出资金流水")
    public List<CapitalFlowDTO> export(
            @Parameter(description = "流水类型") @RequestParam(required = false) String flowType,
            @Parameter(description = "方向: IN/OUT") @RequestParam(required = false) String direction,
            @Parameter(description = "对方类型") @RequestParam(required = false) String partyType,
            @Parameter(description = "对方ID") @RequestParam(required = false) Long partyId,
            @Parameter(description = "开始日期") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) LocalDate endDate) {
        return capitalFlowService.exportList(flowType, direction, partyType, partyId, startDate, endDate)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @GetMapping("/statistics")
    @Operation(summary = "资金流水统计")
    public java.util.Map<String, Object> statistics(
            @Parameter(description = "开始日期") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) LocalDate endDate) {
        List<CapitalFlow> allFlows = capitalFlowService.list();
        java.math.BigDecimal totalIn = allFlows.stream()
                .filter(f -> "IN".equals(f.getDirection()))
                .map(CapitalFlow::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalOut = allFlows.stream()
                .filter(f -> "OUT".equals(f.getDirection()))
                .map(CapitalFlow::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalIn", totalIn);
        stats.put("totalOut", totalOut);
        stats.put("netFlow", totalIn.subtract(totalOut));
        stats.put("totalCount", allFlows.size());
        return stats;
    }

    private CapitalFlowDTO convertToDTO(CapitalFlow flow) {
        CapitalFlowDTO dto = new CapitalFlowDTO();
        BeanUtils.copyProperties(flow, dto);
        return dto;
    }
}
