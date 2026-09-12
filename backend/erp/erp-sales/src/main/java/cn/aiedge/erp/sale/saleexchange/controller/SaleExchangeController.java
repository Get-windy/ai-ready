package cn.aiedge.erp.sale.saleexchange.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.sale.saleexchange.dto.SaleExchangeQuery;
import cn.aiedge.erp.sale.saleexchange.entity.ExchangeApprovalRecord;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchange;
import cn.aiedge.erp.sale.saleexchange.entity.SaleExchangeItem;
import cn.aiedge.erp.sale.saleexchange.service.SaleExchangeService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 销售换货单接口。
 *
 * <p>主表/明细均通过实体 + Jackson 全字段绑定，避免手工 Map 映射丢字段。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/erp/sale/exchange")
@RequiredArgsConstructor
@Tag(name = "销售换货管理", description = "销售换货单的创建、审批、执行等操作")
public class SaleExchangeController {

    private final SaleExchangeService saleExchangeService;

    @GetMapping("/page")
    @Operation(summary = "分页查询换货单（按单据）")
    public Page<SaleExchange> page(SaleExchangeQuery query) {
        return saleExchangeService.pageListExtended(query);
    }

    @GetMapping("/next-no")
    @Operation(summary = "生成下一换货单号（号段 XSHHD-yyyyMMdd-NNNN）")
    public ApiResponse<String> nextNo() {
        return ApiResponse.ok(saleExchangeService.generateExchangeNo());
    }

    @GetMapping("/{id:\\d+}")
    @Operation(summary = "获取换货单详情（含明细）")
    public SaleExchange getById(@PathVariable Long id) {
        SaleExchange exchange = saleExchangeService.getById(id);
        if (exchange != null) {
            exchange.setItems(saleExchangeService.getItems(id));
        }
        return exchange;
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "获取换货单明细")
    public List<SaleExchangeItem> getItems(@PathVariable Long id) {
        return saleExchangeService.getItems(id);
    }

    @GetMapping("/{id}/items/{warehouseType}")
    @Operation(summary = "按仓库类型获取明细（1=换入, 2=换出）")
    public List<SaleExchangeItem> getItemsByWarehouseType(
            @PathVariable Long id,
            @PathVariable Integer warehouseType) {
        return saleExchangeService.getItemsByWarehouseType(id, warehouseType);
    }

    @PostMapping
    @Operation(summary = "创建换货单")
    public SaleExchange create(@RequestBody SaleExchange exchange) {
        return saleExchangeService.createExchange(exchange);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新换货单")
    public SaleExchange update(@PathVariable Long id, @RequestBody SaleExchange exchange) {
        return saleExchangeService.updateExchange(id, exchange);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除换货单")
    public boolean delete(@PathVariable Long id) {
        return saleExchangeService.deleteExchange(id);
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除换货单")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        boolean allOk = true;
        for (Long id : ids) {
            allOk &= saleExchangeService.deleteExchange(id);
        }
        return allOk;
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public SaleExchange submit(@PathVariable Long id) {
        return saleExchangeService.submitForApproval(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批换货单（通过即过账库存）")
    public SaleExchange approve(
            @PathVariable Long id,
            @Parameter(description = "审批意见") @RequestParam(required = false) String remark) {
        // 审批人姓名由 Service 统一解析（SysUser 昵称），避免各处重复实现
        return saleExchangeService.approve(id, StpUtil.getLoginIdAsLong(), null, remark);
    }

    @PostMapping("/batch-approve")
    @Operation(summary = "批量审核")
    public int batchApprove(@RequestBody List<Long> ids) {
        return saleExchangeService.batchApprove(ids, StpUtil.getLoginIdAsLong(), null);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "拒绝换货单")
    public SaleExchange reject(
            @PathVariable Long id,
            @Parameter(description = "拒绝原因") @RequestParam String remark) {
        return saleExchangeService.reject(id, remark);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消换货单（回滚库存）")
    public SaleExchange cancel(
            @PathVariable Long id,
            @Parameter(description = "取消原因") @RequestParam(required = false) String reason) {
        return saleExchangeService.cancel(id, reason);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成换货单")
    public SaleExchange complete(@PathVariable Long id) {
        return saleExchangeService.complete(id);
    }

    @PostMapping("/{id}/print")
    @Operation(summary = "打印（打印次数+1）")
    public void print(@PathVariable Long id) {
        saleExchangeService.print(id);
    }

    @PostMapping("/batch-print")
    @Operation(summary = "批量打印（打印次数+1）")
    public void batchPrint(@RequestBody List<Long> ids) {
        saleExchangeService.batchPrint(ids);
    }

    @GetMapping("/{id}/approval-records")
    @Operation(summary = "获取审批记录")
    public List<ExchangeApprovalRecord> getApprovalRecords(@PathVariable Long id) {
        return saleExchangeService.getApprovalRecords(id);
    }

    @GetMapping("/{id}/tracking")
    @Operation(summary = "获取换货单跟踪信息")
    public Map<String, Object> getTracking(@PathVariable Long id) {
        return saleExchangeService.getTracking(id);
    }

    @GetMapping("/export")
    @Operation(summary = "导出换货单列表（xlsx）")
    public ResponseEntity<byte[]> export(SaleExchangeQuery query) {
        byte[] bytes = saleExchangeService.exportExcel(query);
        String filename = "销售换货单_" + LocalDate.now() + ".xlsx";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .body(bytes);
    }

    private String currentUserName() {
        try {
            return StpUtil.getLoginIdAsString();
        } catch (Exception e) {
            return "system";
        }
    }
}
