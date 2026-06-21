package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.PartnerRole;
import cn.aiedge.erp.stock.service.PartnerRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "往来单位角色管理")
@RestController
@RequestMapping("/api/erp/partner/roles")
@RequiredArgsConstructor
public class PartnerRoleController {

    private final PartnerRoleService partnerRoleService;

    @Operation(summary = "查询单位角色列表")
    @GetMapping("/{partnerId}")
    public Result<List<PartnerRole>> getRoles(@PathVariable Long partnerId) {
        return Result.ok(partnerRoleService.getByPartnerId(partnerId));
    }

    @Operation(summary = "添加角色")
    @PostMapping
    public Result<Boolean> addRole(@RequestBody PartnerRole role) {
        return Result.ok(partnerRoleService.addRole(
                role.getPartnerId(),
                role.getRoleType(),
                role.getIsPrimary() != null && role.getIsPrimary() == 1
        ));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Boolean> removeRole(@PathVariable Long id) {
        return Result.ok(partnerRoleService.removeRole(id));
    }

    @Operation(summary = "检查是否有指定角色")
    @GetMapping("/has")
    public Result<Boolean> hasRole(@RequestParam Long partnerId, @RequestParam String roleType) {
        return Result.ok(partnerRoleService.hasRole(partnerId, roleType));
    }
}
