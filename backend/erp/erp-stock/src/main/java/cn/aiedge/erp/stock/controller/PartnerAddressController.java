package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.PartnerAddress;
import cn.aiedge.erp.stock.service.PartnerAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "往来单位地址管理")
@RestController
@RequestMapping("/api/erp/partner/addresses")
@RequiredArgsConstructor
public class PartnerAddressController {

    private final PartnerAddressService partnerAddressService;

    @Operation(summary = "查询地址列表")
    @GetMapping("/{partnerId}")
    public Result<List<PartnerAddress>> getByPartner(@PathVariable Long partnerId) {
        return Result.ok(partnerAddressService.getByPartnerId(partnerId));
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public Result<Boolean> create(@RequestBody PartnerAddress address) {
        return Result.ok(partnerAddressService.save(address));
    }

    @Operation(summary = "更新地址")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody PartnerAddress address) {
        address.setId(id);
        return Result.ok(partnerAddressService.updateById(address));
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/{id}/set-default")
    public Result<Boolean> setDefault(@PathVariable Long id, @RequestParam Long partnerId) {
        return Result.ok(partnerAddressService.setDefault(id, partnerId));
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(partnerAddressService.removeById(id));
    }
}
