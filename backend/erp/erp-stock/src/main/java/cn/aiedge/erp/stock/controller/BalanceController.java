package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.BalanceLog;
import cn.aiedge.erp.stock.entity.UserBalance;
import cn.aiedge.erp.stock.entity.WithdrawRequest;
import cn.aiedge.erp.stock.mapper.BalanceLogMapper;
import cn.aiedge.erp.stock.mapper.WithdrawRequestMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.aiedge.erp.stock.service.UserBalanceService;
import cn.aiedge.erp.stock.service.WithdrawRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "用户余额/资金管理")
@RestController
@RequestMapping("/api/erp/marketing/balances")
@RequiredArgsConstructor
public class BalanceController {

    private final UserBalanceService balanceService;
    private final BalanceLogMapper balanceLogMapper;
    private final WithdrawRequestMapper withdrawMapper;
    private final WithdrawRequestService withdrawService;

    @Operation(summary = "查询用户余额")
    @GetMapping("/{partnerId}")
    public Result<UserBalance> getBalance(@PathVariable Long partnerId) {
        UserBalance balance = balanceService.lambdaQuery()
                .eq(UserBalance::getPartnerId, partnerId)
                .eq(UserBalance::getDeleted, 0)
                .one();
        return Result.ok(balance);
    }

    @Operation(summary = "分页查询余额流水")
    @GetMapping("/logs")
    public Result<IPage<BalanceLog>> logs(
            @RequestParam Long partnerId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(balanceLogMapper.selectPage(new Page<>(pageNum, pageSize),
                new QueryWrapper<BalanceLog>().eq("partner_id", partnerId)
                        .eq("deleted", 0).orderByDesc("create_time")));
    }

    @Operation(summary = "分页查询提现申请")
    @GetMapping("/withdraws")
    public Result<IPage<WithdrawRequest>> withdraws(
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        QueryWrapper<WithdrawRequest> wrapper = new QueryWrapper<WithdrawRequest>().eq("deleted", 0);
        if (partnerId != null) wrapper.eq("partner_id", partnerId);
        if (status != null) wrapper.eq("status", status);
        wrapper.orderByDesc("create_time");
        return Result.ok(withdrawService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @Operation(summary = "提交提现申请")
    @PostMapping("/withdraws")
    public Result<Boolean> createWithdraw(@RequestBody WithdrawRequest request) {
        return Result.ok(withdrawService.save(request));
    }

    @Operation(summary = "审核提现")
    @PutMapping("/withdraws/{id}/approve")
    public Result<Boolean> approveWithdraw(@PathVariable Long id, @RequestParam String status,
                                            @RequestParam(required = false) String remark) {
        WithdrawRequest w = new WithdrawRequest();
        w.setId(id);
        w.setStatus(status);
        w.setApproveRemark(remark);
        return Result.ok(withdrawService.updateById(w));
    }
}
