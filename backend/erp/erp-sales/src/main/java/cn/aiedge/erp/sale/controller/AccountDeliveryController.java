package cn.aiedge.erp.sale.controller;

import cn.aiedge.erp.sale.dto.AccountDeliveryQueryDTO;
import cn.aiedge.erp.sale.dto.AccountDeliveryResult;
import cn.aiedge.erp.sale.service.AccountDeliveryService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 账款交账控制器
 * 对标：财务→收入支出→账款交账
 * 数据源：销售出库单（配送代收/业务员代收款项，待交账）
 * 路由前缀 erp/finance 保持财务菜单语义；代码位于 erp-sales 模块以复用销售单据数据
 */
@Tag(name = "账款交账")
@RestController
@RequestMapping("/api/erp/finance/account-delivery")
@SaCheckLogin
public class AccountDeliveryController {

    private final AccountDeliveryService accountDeliveryService;

    public AccountDeliveryController(AccountDeliveryService accountDeliveryService) {
        this.accountDeliveryService = accountDeliveryService;
    }

    @Operation(summary = "按单据视图：分页查询待交账单据 + 五档统计")
    @GetMapping("/doc-page")
    public AccountDeliveryResult docPage(AccountDeliveryQueryDTO query) {
        return accountDeliveryService.docPage(query);
    }

    @Operation(summary = "按职员视图：按交账职员分组汇总 + 五档统计")
    @GetMapping("/staff")
    public AccountDeliveryResult staff(AccountDeliveryQueryDTO query) {
        return accountDeliveryService.staffList(query);
    }

    @Operation(summary = "交账动作（去交账/配送退货）：回写结算状态为已结算")
    @PostMapping("/deliver")
    public Map<String, Object> deliver(@RequestBody AccountDeliveryQueryDTO query) {
        accountDeliveryService.deliver(query);
        return Map.of("success", true, "message", "交账成功");
    }
}
