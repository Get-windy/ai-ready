package cn.aiedge.trade.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.trade.entity.ExternalOrderRaw;
import cn.aiedge.trade.service.ExternalOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 外部订单管理端控制器（交易模块 → 外部平台 → 外部订单）
 *
 * <p>此前管理端仅有 OpenApiController（`/api/open/order/*`，供外部平台回调调用），
 * 前端 `externalOrderApi.page`/`retry`/`countPending` 所调的 `/api/trade/external-order/*`
 * 无控制器映射（404）。本控制器补齐管理端装配，直接复用 {@link ExternalOrderService}
 * 已有能力（pageRawOrders / retry / countPending），**不新增实体字段**。</p>
 */
@Tag(name = "外部订单管理", description = "外部平台订单接入台账（管理端）")
@RestController
@RequestMapping("/api/trade/external-order")
@RequiredArgsConstructor
@Validated
public class ExternalOrderController {

    private final ExternalOrderService externalOrderService;

    @Operation(summary = "外部订单分页查询")
    @SaCheckPermission("trade:external-order:list")
    @GetMapping("/page")
    public Result<PageResult<ExternalOrderRaw>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "渠道编码") @RequestParam(required = false) String channelCode,
            @Parameter(description = "处理状态: 0待处理 1已转换 2已入库 3失败") @RequestParam(required = false) Integer status,
            @Parameter(description = "外部订单号（模糊）") @RequestParam(required = false) String externalOrderId,
            @Parameter(description = "接收时间起") @RequestParam(required = false) String startTime,
            @Parameter(description = "接收时间止") @RequestParam(required = false) String endTime) {
        return Result.success(externalOrderService.pageRawOrders(
                pageNum, pageSize, channelCode, status, externalOrderId, startTime, endTime));
    }

    @Operation(summary = "失败订单重试", description = "将失败订单重新加入待处理队列")
    @SaCheckPermission("trade:external-order:retry")
    @PostMapping("/{id}/retry")
    public Result<Boolean> retry(@PathVariable Long id) {
        return Result.success(externalOrderService.retry(id));
    }

    @Operation(summary = "外部订单处理状态统计",
            description = "真实聚合 SQL（COUNT + GROUP BY，租户过滤）：待处理 / 已处理 / 失败 笔数，非当前页口径")
    @SaCheckPermission("trade:external-order:view")
    @GetMapping("/stat")
    public Result<java.util.Map<String, Object>> stat() {
        return Result.success(externalOrderService.statExternalOrders());
    }

    @Operation(summary = "待处理订单数量")
    @SaCheckPermission("trade:external-order:view")
    @GetMapping("/pending-count")
    public Result<Integer> pendingCount(@Parameter(description = "渠道编码(可选)") @RequestParam(required = false) String channelCode) {
        return Result.success(externalOrderService.countPending(channelCode));
    }
}
