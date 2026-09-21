package cn.aiedge.erp.party.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.party.dto.PartyRoleDTO;
import cn.aiedge.erp.party.entity.PartyRole;
import cn.aiedge.erp.party.service.IPartyRoleService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@Tag(name = "往来单位角色管理")
@RestController
@RequestMapping("/api/erp/partner/roles")
@RequiredArgsConstructor
public class PartyRoleController {

    private final IPartyRoleService partyRoleService;

    @Operation(summary = "分页查询角色")
    @SaCheckPermission("party:roles:list")
    @GetMapping("/page")
    public Result<IPage<PartyRoleDTO>> pageRoles(
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleCode,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        Page<PartyRole> page = new Page<>(pageNum, pageSize);
        QueryWrapper<PartyRole> wrapper = new QueryWrapper<>();
        wrapper.like(roleName != null && !roleName.isEmpty(), "role_name", roleName)
               .like(roleCode != null && !roleCode.isEmpty(), "role_code", roleCode)
               .eq("deleted", 0)
               .orderByAsc("sort_order");

        IPage<PartyRole> result = partyRoleService.page(page, wrapper);

        IPage<PartyRoleDTO> dtoPage = new Page<>();
        dtoPage.setRecords(result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
        dtoPage.setCurrent(result.getCurrent());
        dtoPage.setSize(result.getSize());
        dtoPage.setTotal(result.getTotal());

        return Result.ok(dtoPage);
    }

    @Operation(summary = "查询所有启用的角色")
    @SaCheckPermission("party:roles:list")
    @GetMapping("/list")
    public Result<List<PartyRoleDTO>> listRoles() {
        List<PartyRole> roles = partyRoleService.list(
                new QueryWrapper<PartyRole>()
                        .eq("status", 1)
                        .eq("deleted", 0)
                        .orderByAsc("sort_order"));
        return Result.ok(roles.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @Operation(summary = "获取角色详情")
    @SaCheckPermission("party:roles:detail")
    @GetMapping("/{id}")
    public Result<PartyRoleDTO> getRole(@PathVariable Long id) {
        PartyRole role = partyRoleService.getById(id);
        if (role == null || role.getDeleted() != 0) {
            return Result.fail("角色不存在");
        }
        return Result.ok(convertToDTO(role));
    }

    @Operation(summary = "新增角色")
    @SaCheckPermission("party:roles:create")
    @PostMapping
    public Result<Boolean> addRole(@RequestBody PartyRoleDTO dto) {
        // 检查角色代码是否已存在
        PartyRole existing = partyRoleService.getOne(
                new QueryWrapper<PartyRole>()
                        .eq("role_code", dto.getRoleCode())
                        .eq("deleted", 0));
        if (existing != null) {
            return Result.fail("角色代码已存在");
        }

        PartyRole role = new PartyRole();
        BeanUtils.copyProperties(dto, role);
        boolean result = partyRoleService.save(role);
        return Result.ok(result);
    }

    @Operation(summary = "更新角色")
    @SaCheckPermission("party:roles:update")
    @PutMapping("/{id}")
    public Result<Boolean> updateRole(@PathVariable Long id, @RequestBody PartyRoleDTO dto) {
        PartyRole role = partyRoleService.getById(id);
        if (role == null || role.getDeleted() != 0) {
            return Result.fail("角色不存在");
        }

        // 检查角色代码是否与其他角色冲突
        PartyRole existing = partyRoleService.getOne(
                new QueryWrapper<PartyRole>()
                        .eq("role_code", dto.getRoleCode())
                        .ne("id", id)
                        .eq("deleted", 0));
        if (existing != null) {
            return Result.fail("角色代码已存在");
        }

        BeanUtils.copyProperties(dto, role);
        role.setId(id);
        boolean result = partyRoleService.updateById(role);
        return Result.ok(result);
    }

    @Operation(summary = "删除角色")
    @SaCheckPermission("party:roles:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteRole(@PathVariable Long id) {
        PartyRole role = partyRoleService.getById(id);
        if (role == null || role.getDeleted() != 0) {
            return Result.fail("角色不存在");
        }

        // 逻辑删除
        role.setDeleted(1);
        boolean result = partyRoleService.updateById(role);
        return Result.ok(result);
    }

    @Operation(summary = "启用/禁用角色")
    @SaCheckPermission("party:roles:update")
    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        PartyRole role = partyRoleService.getById(id);
        if (role == null || role.getDeleted() != 0) {
            return Result.fail("角色不存在");
        }
        role.setStatus(status);
        boolean result = partyRoleService.updateById(role);
        return Result.ok(result);
    }

    private PartyRoleDTO convertToDTO(PartyRole role) {
        PartyRoleDTO dto = new PartyRoleDTO();
        BeanUtils.copyProperties(role, dto);
        return dto;
    }
}
