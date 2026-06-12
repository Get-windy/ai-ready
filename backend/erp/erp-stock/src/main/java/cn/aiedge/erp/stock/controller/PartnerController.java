package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.Partner;
import cn.aiedge.erp.stock.service.PartnerService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "往来单位管理")
@RestController
@RequestMapping("/api/erp/partner")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @Operation(summary = "分页查询往来单位")
    @GetMapping("/page")
    public Result<IPage<Partner>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String partnerType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(partnerService.getPartnerPage(keyword, partnerType, status, categoryId, pageNum, pageSize));
    }

    @Operation(summary = "查询往来单位详情")
    @GetMapping("/{id}")
    public Result<Partner> getById(@PathVariable Long id) {
        return Result.ok(partnerService.getPartnerDetail(id));
    }

    @Operation(summary = "搜索往来单位(下拉)")
    @GetMapping("/search")
    public Result<List<Partner>> search(@RequestParam String keyword,
                                         @RequestParam(required = false) String partnerType) {
        return Result.ok(partnerService.search(keyword, partnerType));
    }

    @Operation(summary = "获取往来单位列表(不分页)")
    @GetMapping("/list")
    public Result<List<Partner>> list(
            @RequestParam(required = false) String partnerType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "200") Integer pageSize) {
        return Result.ok(partnerService.getPartnerList(partnerType, status, pageSize));
    }

    @Operation(summary = "新增往来单位")
    @PostMapping
    public Result<Boolean> create(@RequestBody Partner partner) {
        return Result.ok(partnerService.createPartner(partner));
    }

    @Operation(summary = "更新往来单位")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Partner partner) {
        partner.setId(id);
        return Result.ok(partnerService.updatePartner(partner));
    }

    @Operation(summary = "启用/停用")
    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Partner partner = new Partner();
        partner.setId(id);
        partner.setStatus(status);
        return Result.ok(partnerService.updateById(partner));
    }

    @Operation(summary = "删除往来单位")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(partnerService.removeById(id));
    }
}
