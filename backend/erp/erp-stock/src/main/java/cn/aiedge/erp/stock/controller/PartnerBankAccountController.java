package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.PartnerBankAccount;
import cn.aiedge.erp.stock.service.PartnerBankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "往来单位银行账户管理")
@RestController
@RequestMapping("/api/erp/partner/bank-accounts")
@RequiredArgsConstructor
public class PartnerBankAccountController {

    private final PartnerBankAccountService partnerBankAccountService;

    @Operation(summary = "查询银行账户列表")
    @GetMapping("/{partnerId}")
    public Result<List<PartnerBankAccount>> getByPartner(@PathVariable Long partnerId) {
        return Result.ok(partnerBankAccountService.getByPartnerId(partnerId));
    }

    @Operation(summary = "新增银行账户")
    @PostMapping
    public Result<Boolean> create(@RequestBody PartnerBankAccount account) {
        return Result.ok(partnerBankAccountService.save(account));
    }

    @Operation(summary = "更新银行账户")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody PartnerBankAccount account) {
        account.setId(id);
        return Result.ok(partnerBankAccountService.updateById(account));
    }

    @Operation(summary = "删除银行账户")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(partnerBankAccountService.removeById(id));
    }
}
