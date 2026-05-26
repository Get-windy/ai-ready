package cn.aiedge.finance.controller;

import cn.aiedge.finance.entity.Receivable;
import cn.aiedge.finance.enums.ReceivableStatus;
import cn.aiedge.finance.service.ReceivableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/finance/receivable")
@RequiredArgsConstructor
@Tag(name = "应收账款管理", description = "应收账款查询、收款、账龄分析等操作")
public class ReceivableController {

    private final ReceivableService receivableService;

    @GetMapping("/page")
    @Operation(summary = "分页查询应收账款")
    public Map<String, Object> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return convertPageToMap(receivableService.pageList(keyword, customerId, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取应收账款详情")
    public Receivable getById(@PathVariable Long id) {
        return receivableService.getById(id);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "获取客户应收账款列表")
    public List<Receivable> listByCustomerId(@PathVariable Long customerId) {
        return receivableService.listByCustomerId(customerId);
    }

    @GetMapping("/overdue")
    @Operation(summary = "获取逾期应收账款列表")
    public List<Receivable> listOverdue() {
        return receivableService.listOverdue();
    }

    @PostMapping("/from-order/{orderId}")
    @Operation(summary = "从订单创建应收账款")
    public Receivable createFromOrder(
            @PathVariable Long orderId,
            @RequestParam BigDecimal amount,
            @RequestParam LocalDate dueDate) {
        return receivableService.createFromOrder(orderId, amount, dueDate);
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "收款")
    public Receivable receive(
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        return receivableService.receive(id, amount);
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "关闭应收账款")
    public Receivable close(@PathVariable Long id) {
        return receivableService.close(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消应收账款")
    public Receivable cancel(@PathVariable Long id) {
        return receivableService.cancel(id);
    }

    @PostMapping("/update-overdue")
    @Operation(summary = "更新逾期状态")
    public void updateOverdueStatus() {
        receivableService.updateOverdueStatus();
    }

    @GetMapping("/statistics")
    @Operation(summary = "应收账款统计")
    public Map<String, Object> getStatistics() {
        return receivableService.getStatistics();
    }

    @GetMapping("/aging-analysis")
    @Operation(summary = "账龄分析")
    public Map<String, BigDecimal> getAgingAnalysis() {
        return receivableService.getAgingAnalysis();
    }

    private Map<String, Object> convertPageToMap(com.baomidou.mybatisplus.extension.plugins.pagination.Page<Receivable> page) {
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        for (Receivable receivable : page.getRecords()) {
            for (ReceivableStatus status : ReceivableStatus.values()) {
                if (status.getCode().equals(receivable.getStatus())) {
                    receivable.setRemark(status.getDesc());
                    break;
                }
            }
        }
        return result;
    }
}