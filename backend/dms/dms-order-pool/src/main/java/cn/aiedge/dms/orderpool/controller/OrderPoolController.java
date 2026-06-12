package cn.aiedge.dms.orderpool.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.orderpool.dto.BidRequest;
import cn.aiedge.dms.orderpool.dto.CancelBidRequest;
import cn.aiedge.dms.orderpool.dto.GrabRequest;
import cn.aiedge.dms.orderpool.entity.DmsBid;
import cn.aiedge.dms.orderpool.entity.DmsOrderPool;
import cn.aiedge.dms.orderpool.service.BidService;
import cn.aiedge.dms.orderpool.service.OrderPoolService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单大厅管理控制器
 */
@Tag(name = "订单大厅管理")
@RestController
@RequestMapping("/api/dms/order-pool")
@RequiredArgsConstructor
@SaCheckLogin
public class OrderPoolController {

    private final OrderPoolService orderPoolService;
    private final BidService bidService;

    @Operation(summary = "分页查询订单大厅")
    @GetMapping("/page")
    public ApiResponse<IPage<DmsOrderPool>> page(@Parameter(description = "当前页") @RequestParam(defaultValue = "1") long current,
                                                  @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long size,
                                                  @Parameter(description = "查询条件") DmsOrderPool query) {
        Page<DmsOrderPool> page = new Page<>(current, size);
        LambdaQueryWrapper<DmsOrderPool> wrapper = new LambdaQueryWrapper<>();
        if (query.getPoolStatus() != null) {
            wrapper.eq(DmsOrderPool::getPoolStatus, query.getPoolStatus());
        }
        if (query.getTenantId() != null) {
            wrapper.eq(DmsOrderPool::getTenantId, query.getTenantId());
        }
        wrapper.orderByDesc(DmsOrderPool::getCreateTime);
        return ApiResponse.ok(orderPoolService.page(page, wrapper));
    }

    @Operation(summary = "获取订单大厅详情")
    @GetMapping("/{id}")
    public ApiResponse<DmsOrderPool> getById(@Parameter(description = "订单池ID") @PathVariable Long id) {
        return ApiResponse.ok(orderPoolService.getById(id));
    }

    @Operation(summary = "抢单")
    @PostMapping("/{id}/grab")
    @SaCheckPermission("dms:pool:operate")
    public ApiResponse<Void> grab(@Parameter(description = "订单池ID") @PathVariable Long id, @Parameter(description = "抢单请求") @Valid @RequestBody GrabRequest request) {
        orderPoolService.grab(id, request.getRiderId(), request.getRiderName());
        return ApiResponse.ok(null);
    }

    @Operation(summary = "竞价")
    @PostMapping("/{id}/bid")
    @SaCheckPermission("dms:pool:operate")
    public ApiResponse<DmsBid> bid(@Parameter(description = "订单池ID") @PathVariable Long id, @Parameter(description = "竞价请求") @Valid @RequestBody BidRequest request) {
        return ApiResponse.ok(bidService.createBid(id, request.getRiderId(), request.getRiderName(), request.getPrice()));
    }

    @Operation(summary = "获取竞价列表")
    @GetMapping("/{id}/bid-list")
    public ApiResponse<List<DmsBid>> getBidList(@Parameter(description = "订单池ID") @PathVariable Long id) {
        orderPoolService.getById(id);
        return ApiResponse.ok(bidService.getBidsByPoolId(id));
    }

    @Operation(summary = "取消竞价")
    @PostMapping("/{id}/cancel-bid")
    @SaCheckPermission("dms:pool:operate")
    public ApiResponse<Void> cancelBid(@Parameter(description = "订单池ID") @PathVariable Long id, @Parameter(description = "取消竞价请求") @Valid @RequestBody CancelBidRequest request) {
        bidService.cancelBid(request.getBidId(), request.getRiderId());
        return ApiResponse.ok(null);
    }
}
