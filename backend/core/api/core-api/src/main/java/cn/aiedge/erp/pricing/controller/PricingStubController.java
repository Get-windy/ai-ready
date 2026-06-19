package cn.aiedge.erp.pricing.controller;

import cn.aiedge.base.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 定价审批占位 Controller
 * 审批相关接口已由 erp-pricing 模块 PriceApprovalController 提供
 * 本 Controller 仅保留价格层级等尚未实现的占位接口
 */
@Tag(name = "定价审批（占位）")
@RestController
@RequestMapping("/api/erp/pricing")
public class PricingStubController {

    @Operation(summary = "价格层级列表（占位）")
    @GetMapping("/tiers/list")
    public Result<List<?>> tiersList() {
        return Result.ok(Collections.emptyList());
    }
}
