package cn.aiedge.finance.controller;

import cn.aiedge.finance.entity.Payable;
import cn.aiedge.finance.enums.PayableStatus;
import cn.aiedge.finance.service.PayableService;
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
@RequestMapping("/api/finance/payable")
@RequiredArgsConstructor
@Tag(name = "应付账款管理", description = "应付账款查询、付款、账龄分析等操作")
public class PayableController {

    private final PayableService payableService;

    @GetMapping("/page")
    @Operation(summary = "分页查询应付账款")
    public Map<String, Object> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "供应商ID") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        return convertPageToMap(payableService.pageList(keyword, supplierId, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取应付账款详情")
    public Payable getById(@PathVariable Long id) {
        return payableService.getById(id);
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "获取供应商应付账款列表")
    public List<Payable> listBySupplierId(@PathVariable Long supplierId) {
        return payableService.listBySupplierId(supplierId);
    }

    @GetMapping("/overdue")
    @Operation(summary = "获取逾期应付账款列表")
    public List<Payable> listOverdue() {
        return payableService.listOverdue();
    }

    @PostMapping("/from-order/{orderId}")
    @Operation(summary = "从订单创建应付账款")
    public Payable createFromOrder(
            @PathVariable Long orderId,
            @RequestParam BigDecimal amount,
            @RequestParam LocalDate dueDate) {
        return payableService.createFromOrder(orderId, amount, dueDate);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "付款")
    public Payable pay(
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        return payableService.pay(id, amount);
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "关闭应付账款")
    public Payable close(@PathVariable Long id) {
        return payableService.close(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消应付账款")
    public Payable cancel(@PathVariable Long id) {
        return payableService.cancel(id);
    }

    @PostMapping("/update-overdue")
    @Operation(summary = "更新逾期状态")
    public void updateOverdueStatus() {
        payableService.updateOverdueStatus();
    }

    @GetMapping("/statistics")
    @Operation(summary = "应付账款统计")
    public Map<String, Object> getStatistics() {
        return payableService.getStatistics();
    }

    @GetMapping("/aging-analysis")
    @Operation(summary = "账龄分析")
    public Map<String, BigDecimal> getAgingAnalysis() {
        return payableService.getAgingAnalysis();
    }

    private Map<String, Object> convertPageToMap(com.baomidou.mybatisplus.extension.plugins.pagination.Page<Payable> page) {
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        for (Payable payable : page.getRecords()) {
            for (PayableStatus status : PayableStatus.values()) {
                if (status.getCode().equals(payable.getStatus())) {
                    payable.setRemark(status.getDesc());
                    break;
                }
            }
        }
        return result;
    }
}