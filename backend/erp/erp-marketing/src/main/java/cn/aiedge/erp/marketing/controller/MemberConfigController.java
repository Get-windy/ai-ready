package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.entity.MemberConfig;
import cn.aiedge.erp.marketing.service.MemberConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 会员设置（营销 → 会员中心 → 会员设置，菜单 80302）
 * 单行参数配置：GET 读取（无行自动建默认行）/ PUT 保存。
 */
@Slf4j
@Tag(name = "会员设置")
@RestController
@RequestMapping("/api/erp/marketing/member-config")
@RequiredArgsConstructor
public class MemberConfigController {

    private final MemberConfigService memberConfigService;

    @Operation(summary = "读取会员设置")
    @GetMapping
    public Result<MemberConfig> get() {
        return Result.ok(memberConfigService.getConfig());
    }

    @Operation(summary = "保存会员设置")
    @PutMapping
    public Result<MemberConfig> save(@RequestBody MemberConfig config) {
        validate(config);
        return Result.ok(memberConfigService.saveConfig(config));
    }

    /** 档位边界校验（仅拦自相矛盾的输入，不发明业务规则） */
    private void validate(MemberConfig config) {
        if (config.getMaxDeductPercent() != null
                && (config.getMaxDeductPercent().compareTo(BigDecimal.ZERO) < 0
                || config.getMaxDeductPercent().compareTo(new BigDecimal("100")) > 0)) {
            throw new IllegalArgumentException("单笔订单最高可抵扣的金额百分比须在 0~100 之间");
        }
        if (config.getPointsPerYuan() != null && config.getPointsPerYuan().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("抵现比例（N 积分=1 元）须大于 0");
        }
        if (config.getAmountPerPoint() != null && config.getAmountPerPoint().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("按销售金额积分（N 元=1 分）须大于 0");
        }
        if (config.getSigninEnabled() != null && config.getSigninEnabled() == 1) {
            if (config.getSigninFirstPoints() == null || config.getSigninIncrement() == null
                    || config.getSigninMaxPoints() == null) {
                throw new IllegalArgumentException("启用签到积分时，签到三项积分参数均为必填");
            }
            if (config.getSigninMaxPoints() < config.getSigninFirstPoints()) {
                throw new IllegalArgumentException("连续签到最大获得不得小于第一天签到积分");
            }
        }
    }
}
