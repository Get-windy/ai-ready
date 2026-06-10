package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.PartnerContact;
import cn.aiedge.erp.stock.service.PartnerContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "往来单位联系人管理")
@RestController
@RequestMapping("/api/erp/partner/contacts")
@RequiredArgsConstructor
public class PartnerContactController {

    private final PartnerContactService partnerContactService;

    @Operation(summary = "查询联系人列表")
    @GetMapping("/{partnerId}")
    public Result<List<PartnerContact>> getByPartner(@PathVariable Long partnerId) {
        return Result.ok(partnerContactService.getByPartnerId(partnerId));
    }

    @Operation(summary = "新增联系人")
    @PostMapping
    public Result<Boolean> create(@RequestBody PartnerContact contact) {
        return Result.ok(partnerContactService.save(contact));
    }

    @Operation(summary = "更新联系人")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody PartnerContact contact) {
        contact.setId(id);
        return Result.ok(partnerContactService.updateById(contact));
    }

    @Operation(summary = "删除联系人")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(partnerContactService.removeById(id));
    }
}
