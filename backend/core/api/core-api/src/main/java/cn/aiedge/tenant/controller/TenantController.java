package cn.aiedge.tenant.controller;

import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.entity.SysTenantProfile;
import cn.aiedge.base.mapper.TenantMapper;
import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.base.vo.Result;
import cn.aiedge.tenant.dto.CompanyProfileDTO;
import cn.aiedge.tenant.dto.CompanyProfileVO;
import cn.aiedge.tenant.service.TenantProfileService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 租户管理控制器
 * 提供租户的 CRUD、分页查询、状态管理等功能
 *
 * <p>本控制器的端点分两类，切勿混用：
 * <ul>
 *   <li><b>平台侧</b>（系统模块「租户管理」）：{@code /page}、{@code POST}、{@code DELETE}、{@code /{id}/status} 等，
 *       入参带 id、可跨租户操作，靠 {@code system:tenant:*} 权限码收口。</li>
 *   <li><b>租户侧</b>（设置模块「企业信息」，菜单 80624 / set:company-info）：{@code GET/PUT /current}，
 *       <b>租户 ID 一律取会话（SecurityContext），不信任路径 / 请求体</b>。
 *       原因：{@code sys_tenant} 表无 tenant_id 列，且在 {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES} 中
 *       → 多租户插件不会注入租户条件；若继续沿用 {@code PUT /{id}}（前端 id 来自 localStorage），
 *       任意租户管理员都能改到别的租户行。
 *       <b>2026-09-18 拆表后</b>：企业档案字段在 1:1 子表 {@code sys_tenant_profile}
 *       （有 tenant_id、不在忽略清单中），读写统一走
 *       {@link cn.aiedge.tenant.service.TenantProfileService}，租户条件在服务层显式带上。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/tenant")
@SaCheckLogin
@RequiredArgsConstructor
@Tag(name = "租户管理", description = "租户CRUD、分页查询、状态管理")
public class TenantController {

    private final TenantMapper tenantMapper;

    private final SecurityContext securityContext;

    /** 企业档案（1:1 子表）读写；两张表的写入在同一事务内完成 */
    private final TenantProfileService tenantProfileService;

    /**
     * 分页查询租户
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询租户")
    public Result<Map<String, Object>> getPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String tenantName,
            @RequestParam(required = false) String tenantCode,
            @RequestParam(required = false) Integer status) {

        Page<SysTenant> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<SysTenant>()
                .like(tenantName != null && !tenantName.isEmpty(), SysTenant::getTenantName, tenantName)
                .like(tenantCode != null && !tenantCode.isEmpty(), SysTenant::getTenantCode, tenantCode)
                .eq(status != null, SysTenant::getStatus, status)
                .orderByDesc(SysTenant::getCreateTime);

        Page<SysTenant> result = tenantMapper.selectPage(page, wrapper);

        Map<String, Object> data = Map.of(
            "records", result.getRecords(),
            "total", result.getTotal(),
            "current", result.getCurrent(),
            "size", result.getSize(),
            "pages", result.getPages()
        );

        return Result.ok(data);
    }

    /**
     * 获取租户详情（平台侧，按 id 直取；租户侧请用 {@code GET /current}）
     *
     * <p>路径变量限定为纯数字：与租户侧的 {@code /current} 区分开，
     * 避免「非数字 id」走到本方法时抛参数转换异常（历史上 {@code /tenant/pending} 即被本方法吞掉并返回 400）。
     */
    @GetMapping("/{id:\\d+}")
    @Operation(summary = "获取租户详情")
    public Result<SysTenant> getById(@PathVariable Long id) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null || tenant.getDeleted() == 1) {
            return Result.fail(404, "租户不存在");
        }
        return Result.ok(tenant);
    }

    /**
     * 创建租户
     */
    @PostMapping
    @Operation(summary = "创建租户")
    @SaCheckPermission("system:tenant:create")
    public Result<Boolean> create(@RequestBody SysTenant tenant) {
        tenant.setDeleted(0);
        if (tenant.getStatus() == null) {
            tenant.setStatus(1);
        }
        int rows = tenantMapper.insert(tenant);
        return Result.ok(rows > 0);
    }

    /**
     * 更新租户（平台侧，按 id 直改；租户侧请用 {@code PUT /current}）
     */
    @PutMapping("/{id:\\d+}")
    @Operation(summary = "更新租户")
    @SaCheckPermission("system:tenant:update")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SysTenant tenant) {
        tenant.setId(id);
        int rows = tenantMapper.updateById(tenant);
        return Result.ok(rows > 0);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 企业信息（设置 → 系统配置 → 企业信息，菜单 80624 / set:company-info）
    // 租户只能读写**自己**的档案：租户 ID 全部取会话，路径/请求体里没有 id。
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 获取当前租户的企业档案（Tab① 企业信息 + Tab② 纳税人信息 的全部字段）
     *
     * <p>响应体由**两张表**拼成：{@code sys_tenant}（身份/生命周期，只读展示）+
     * {@code sys_tenant_profile}（企业档案 15 列 + LOGO，1:1）。返回类型从拆表前的
     * {@code SysTenant} 换成 {@link CompanyProfileVO}，但**字段名与类型逐字保持原样**
     * （VO 类注释已登记该契约），故前端 {@code api/tenant.ts} 与 E2E 断言无需改动。</p>
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前租户的企业档案")
    public Result<CompanyProfileVO> getCurrentProfile() {
        Long tenantId = securityContext.getCurrentTenantId();
        if (tenantId == null) {
            return Result.fail(401, "无法确定当前租户，请重新登录");
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || Integer.valueOf(1).equals(tenant.getDeleted())) {
            return Result.fail(404, "租户不存在");
        }
        // 档案行允许不存在（新注册租户尚未填过）→ 由 VO.of 补 null，不报错也不"读时建行"
        SysTenantProfile profile = tenantProfileService.getByTenantId(tenantId);
        return Result.ok(CompanyProfileVO.of(tenant, profile));
    }

    /**
     * 保存当前租户的企业档案
     *
     * <p>两张表在同一事务内写（拆表后必需）：{@code sys_tenant} 的既有列（tenant_name / 联系人 /
     * 联系电话 / 企业邮箱 / 企业地址）由 {@code TenantProfileService} 更新，其余档案列与 LOGO 写
     * {@code sys_tenant_profile}（不存在则插入）。</p>
     *
     * <p>逐列显式 {@code set} 而不是 {@code updateById(entity)}：后者会**忽略 null 字段**，
     * 导致用户清空某个输入框后保存无效（本页历史缺陷 P2-⑮）；空白串统一归一为 null 落库。
     * 只允许改档案列；tenant_code / level / expire_time / status / admin_user_id 属平台维护，
     * 由 {@link CompanyProfileDTO.Save} 白名单挡在门外。</p>
     */
    @PutMapping("/current")
    @Operation(summary = "保存当前租户的企业档案")
    @SaCheckPermission("set:company-info:save")
    public Result<Boolean> saveCurrentProfile(@Valid @RequestBody CompanyProfileDTO.Save dto) {
        Long tenantId = securityContext.getCurrentTenantId();
        if (tenantId == null) {
            return Result.fail(401, "无法确定当前租户，请重新登录");
        }
        SysTenant exists = tenantMapper.selectById(tenantId);
        if (exists == null || Integer.valueOf(1).equals(exists.getDeleted())) {
            return Result.fail(404, "租户不存在");
        }
        tenantProfileService.saveProfile(tenantId, dto);
        // 保存接口的返回值语义保持与拆表前一致：主表更新成功即视为成功（同事务，子表失败会整体回滚）
        return Result.ok(true);
    }

    /**
     * 删除租户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除租户")
    @SaCheckPermission("system:tenant:delete")
    public Result<Boolean> delete(@PathVariable Long id) {
        int rows = tenantMapper.deleteById(id);
        return Result.ok(rows > 0);
    }

    /**
     * 批量删除租户
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除租户")
    @SaCheckPermission("system:tenant:delete")
    public Result<Boolean> batchDelete(@RequestBody List<Long> ids) {
        int rows = tenantMapper.deleteBatchIds(ids);
        return Result.ok(rows > 0);
    }

    /**
     * 更新租户状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "更新租户状态")
    @SaCheckPermission("system:tenant:update")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        SysTenant tenant = new SysTenant();
        tenant.setId(id);
        tenant.setStatus(status);
        int rows = tenantMapper.updateById(tenant);
        return Result.ok(rows > 0);
    }

    /**
     * 获取租户配置（占位，返回空配置）
     */
    @GetMapping("/{id}/config")
    @Operation(summary = "获取租户配置")
    public Result<Map<String, Object>> getConfig(@PathVariable Long id) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null || tenant.getDeleted() == 1) {
            return Result.fail(404, "租户不存在");
        }
        Map<String, Object> config = Map.of(
            "id", 0,
            "tenantId", id,
            "logo", "",
            "themeColor", "#1890ff",
            "features", "",
            "maxUsers", 100,
            "expireDate", ""
        );
        return Result.ok(config);
    }

    /**
     * 更新租户配置（占位）
     */
    @PutMapping("/{id}/config")
    @Operation(summary = "更新租户配置")
    public Result<Boolean> updateConfig(@PathVariable Long id, @RequestBody Map<String, Object> config) {
        // 配置存储暂未实现，返回成功占位
        return Result.ok(true);
    }
}
