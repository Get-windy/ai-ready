package cn.aiedge.crm.customer.controller;

import cn.aiedge.crm.customer.entity.CustomerPool;
import cn.aiedge.crm.customer.enums.PoolReason;
import cn.aiedge.crm.customer.enums.PoolStatus;
import cn.aiedge.crm.customer.service.CustomerPoolService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/crm/customer-pool")
@Tag(name = "客户公海池管理", description = "公海池客户领取、退回、自动回收等操作")
@RequiredArgsConstructor
public class CustomerPoolController {

    private final CustomerPoolService customerPoolService;

    @Operation(summary = "分页查询公海池")
    @GetMapping("/page")
    public Page<CustomerPool> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "池类型") @RequestParam(required = false) Integer poolType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        Page<CustomerPool> page = customerPoolService.pageList(keyword, poolType, status, pageNum, pageSize);
        for (CustomerPool pool : page.getRecords()) {
            for (PoolStatus ps : PoolStatus.values()) {
                if (ps.getCode().equals(pool.getStatus())) {
                    pool.setStatusDesc(ps.getDesc());
                    break;
                }
            }
        }
        return page;
    }

    @Operation(summary = "获取可领取客户列表")
    @GetMapping("/available")
    public List<CustomerPool> listAvailable() {
        return customerPoolService.listAvailable();
    }

    @Operation(summary = "获取我领取的客户")
    @GetMapping("/my-claimed")
    public List<CustomerPool> listMyClaimed() {
        Long salesPersonId = StpUtil.getLoginIdAsLong();
        return customerPoolService.listByClaimSalesPerson(salesPersonId);
    }

    @Operation(summary = "获取我放入公海的客户")
    @GetMapping("/my-returned")
    public List<CustomerPool> listMyReturned() {
        Long salesPersonId = StpUtil.getLoginIdAsLong();
        return customerPoolService.listByOriginalSalesPerson(salesPersonId);
    }

    @Operation(summary = "放入公海池")
    @PostMapping("/put/{customerId}")
    public CustomerPool putToPool(
            @PathVariable Long customerId,
            @RequestParam Integer poolReason,
            @RequestParam(required = false) String remark) {
        return customerPoolService.putToPool(customerId, poolReason, remark);
    }

    @Operation(summary = "从公海池领取客户")
    @PostMapping("/claim/{poolId}")
    public CustomerPool claimFromPool(@PathVariable Long poolId) {
        Long salesPersonId = StpUtil.getLoginIdAsLong();
        return customerPoolService.claimFromPool(poolId, salesPersonId);
    }

    @Operation(summary = "退回公海池")
    @PostMapping("/return/{poolId}")
    public CustomerPool returnToPool(
            @PathVariable Long poolId,
            @RequestParam(required = false) String remark) {
        return customerPoolService.returnToPool(poolId, remark);
    }

    @Operation(summary = "执行自动回收")
    @PostMapping("/auto-recovery")
    public void autoRecovery(@RequestParam(defaultValue = "30") Integer noFollowUpDays) {
        customerPoolService.autoRecovery(noFollowUpDays);
    }

    @Operation(summary = "检查过期客户")
    @PostMapping("/check-expired")
    public void checkExpired() {
        customerPoolService.checkExpired();
    }

    @Operation(summary = "公海池统计")
    @GetMapping("/statistics")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("availableCount", customerPoolService.getAvailableCount());
        stats.put("myClaimCount", customerPoolService.getClaimCountBySalesPerson(StpUtil.getLoginIdAsLong()));
        stats.put("poolReasons", PoolReason.values());
        stats.put("poolStatuses", PoolStatus.values());
        return stats;
    }
}