package cn.aiedge.base.controller;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.entity.SysMenu;
import cn.aiedge.base.event.PermissionChangeEvent;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.service.MenuVisibilityService;
import cn.aiedge.base.service.SysMenuService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 菜单配置（设置 → 系统配置 → 菜单配置，菜单 80620）控制器 —— <b>租户级</b>。
 *
 * <p>本页的定位是「<b>我这个租户</b>改<b>自己</b>菜单的显隐」，
 * 与系统模块的平台级「菜单管理」（{@link SysMenuController}，定义全局菜单树 + 权限码 + 路由/组件）
 * 是两件事，因此：
 * <ul>
 *   <li>只读全局菜单定义（{@code sys_menu}，平台资产），<b>不提供任何增删改菜单定义的端点</b>；</li>
 *   <li>写操作只落在<b>本租户</b>的配置行上（{@code sys_project_config.tenant_id = 当前会话租户}），
 *       租户 ID 一律取自登录会话，不接受前端传入，杜绝跨租户影响；</li>
 *   <li>复用仓库既有的租户自助配置口径（{@code UserPageConfigController} 的 {@code @SaCheckLogin}）——
 *       {@code sys_permission} 中不存在 {@code set:*} 权限码，凭空新增权限码而不授予任何角色
 *       会复现设置模块文档 §5.5 记录的「注解齐全但无权限码 → 非超管全 403」同症。</li>
 * </ul>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "菜单配置（租户级）", description = "租户改自己已授权菜单的显隐")
@RestController
@RequestMapping("/api/set/menu-config")
@RequiredArgsConstructor
public class SetMenuConfigController {

    private final SysMenuService menuService;
    private final MenuVisibilityService menuVisibilityService;
    private final ApplicationEventPublisher eventPublisher;

    /** 租户端菜单（设置模块属于 tenant-admin 侧） */
    private static final String CLIENT_TYPE_TENANT_ADMIN = "tenant-admin";

    /** 菜单类型：目录 */
    private static final int MENU_TYPE_DIR = 0;

    /** 菜单类型：页面（菜单） */
    private static final int MENU_TYPE_PAGE = 1;

    /**
     * 「菜单配置」页自身：允许关闭会把自己锁在门外（关闭后本页就从导航消失），
     * 因此前端置灰 + 后端兜底拒绝。
     */
    private static final String SELF_MENU_CODE = "set:menu-config";

    /** 菜单层级防护（父级链路异常成环时不至于死循环） */
    private static final int MAX_DEPTH_GUARD = 20;

    /**
     * 本租户可配置的页面级菜单清单（含当前已隐藏的菜单，否则关掉就再也打不开了）。
     */
    @Operation(summary = "本租户可配置的菜单清单")
    @GetMapping("/list")
    @SaCheckLogin
    public Result<List<MenuConfigItemVO>> list() {
        Long tenantId = requireTenantId();

        // 菜单定义是全局资产：sys_menu 不参与租户隔离（IGNORE_TENANT_TABLES），
        // 取租户端「目录 + 页面」，用于分组与列表展示；按钮（menu_type = 2）不在此页管辖范围
        List<SysMenu> all = menuService.list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getClientType, CLIENT_TYPE_TENANT_ADMIN)
                .in(SysMenu::getMenuType, MENU_TYPE_DIR, MENU_TYPE_PAGE)
                .orderByAsc(SysMenu::getSort));

        Map<Long, SysMenu> byId = new HashMap<>();
        for (SysMenu menu : all) {
            byId.put(menu.getId(), menu);
        }

        Set<Long> hiddenMenuIds = menuVisibilityService.getHiddenMenuIds(tenantId);

        List<MenuConfigItemVO> rows = new ArrayList<>();
        for (SysMenu menu : all) {
            // 只列「页面级」菜单：对标形态是每个页面一行开关；目录承担分组语义（左侧分组 + 所属分组列）
            if (menu.getMenuType() == null || menu.getMenuType() != MENU_TYPE_PAGE) {
                continue;
            }
            MenuConfigItemVO vo = new MenuConfigItemVO();
            vo.setId(menu.getId());
            vo.setParentId(menu.getParentId());
            vo.setMenuName(menu.getMenuName());
            vo.setMenuCode(menu.getMenuCode());
            vo.setPath(menu.getPath());
            vo.setComponent(menu.getComponent());
            vo.setIcon(menu.getIcon());
            vo.setSort(menu.getSort());
            // 全局菜单状态：sys_menu.status = 1 表示启用（后端 getUserMegaMenus 以 status = 1 过滤）
            vo.setStatus(menu.getStatus());
            vo.setVisible(!hiddenMenuIds.contains(menu.getId()));
            vo.setLocked(SELF_MENU_CODE.equals(menu.getMenuCode()));

            SysMenu parent = byId.get(menu.getParentId());
            SysMenu domain = resolveDomain(parent, byId);
            // 挂不到一级域的页面菜单**不进本页**：`6130701 菜单管理` 是一行 `client_type='tenant-admin'`
            // 却挂在 **system-admin** 域（61307 系统）下的异类菜单，属平台侧，不在本租户的菜单树里
            // （`getUserMegaMenus` 也到不了租户导航）。列出来只会得到 domainId=null 的孤儿行 +
            // 左侧分组面板多一个空组（2026-09-18 实机踩到）。
            if (domain == null) {
                continue;
            }
            vo.setGroupName(parent != null ? parent.getMenuName() : "");
            vo.setDomainId(domain.getId());
            vo.setDomainName(domain.getMenuName());
            // 一级域排序值：左侧分组面板按它排序（页面自身的 sort 只在组内有序，跨组会交错）
            vo.setDomainSort(domain.getSort());
            rows.add(vo);
        }
        return Result.ok(rows);
    }

    /**
     * 设置某个菜单在本租户的显隐（开 / 关）。
     *
     * @param menuId  菜单 ID
     * @param visible true = 显示；false = 隐藏
     */
    @Operation(summary = "设置本租户菜单显隐")
    @PutMapping("/visible")
    @SaCheckLogin
    @OperationLog(module = "菜单配置", type = "UPDATE", desc = "租户级菜单显隐")
    public Result<Void> updateVisible(@RequestParam Long menuId, @RequestParam Boolean visible) {
        Long tenantId = requireTenantId();

        SysMenu menu = menuService.getById(menuId);
        if (menu == null) {
            return Result.fail("菜单不存在");
        }
        if (!CLIENT_TYPE_TENANT_ADMIN.equals(menu.getClientType())) {
            return Result.fail("只能配置租户端菜单");
        }
        if (menu.getMenuType() == null || menu.getMenuType() != MENU_TYPE_PAGE) {
            return Result.fail("只能配置页面级菜单");
        }
        if (SELF_MENU_CODE.equals(menu.getMenuCode()) && !Boolean.TRUE.equals(visible)) {
            return Result.fail("「菜单配置」页自身不可隐藏，否则将无法再进入本页");
        }

        menuVisibilityService.updateVisibility(tenantId, menuId, Boolean.TRUE.equals(visible));
        // 与 SysMenuController 同口径：菜单可见性变化后广播权限/菜单变更事件
        eventPublisher.publishEvent(PermissionChangeEvent.broadcast(this,
                PermissionChangeEvent.ChangeType.MENU_UPDATED, tenantId, "租户菜单显隐: menuId=" + menuId));
        return Result.ok("设置成功", null);
    }

    /**
     * 取当前会话租户；拿不到直接抛错（本页所有读写都必须落在明确租户上）。
     */
    private Long requireTenantId() {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        if (tenantId == null) {
            throw new RuntimeException("无法解析当前会话租户，请重新登录后再试");
        }
        return tenantId;
    }

    /**
     * 由页面菜单的父目录上溯到一级域（parent_id = 0 的目录），用于左侧「菜单分组」面板。
     */
    private SysMenu resolveDomain(SysMenu start, Map<Long, SysMenu> byId) {
        SysMenu current = start;
        int guard = 0;
        while (current != null
                && current.getParentId() != null
                && current.getParentId() != 0L
                && guard++ < MAX_DEPTH_GUARD) {
            current = byId.get(current.getParentId());
        }
        return current;
    }

    /**
     * 菜单配置行（页面级菜单 + 本租户显隐状态）。
     */
    @Data
    public static class MenuConfigItemVO {

        /** 菜单 ID（雪花，前端按字符串处理，避免 JS 精度丢失） */
        private Long id;

        /** 直接父目录 ID（= 所属分组） */
        private Long parentId;

        /** 所属分组名（直接父目录名） */
        private String groupName;

        /** 一级域 ID（左侧分组面板节点） */
        private Long domainId;

        /** 一级域名 */
        private String domainName;

        /** 一级域排序（左侧分组面板排序用） */
        private Integer domainSort;

        /** 菜单名称（页面名） */
        private String menuName;

        /** 菜单编码，即页面上的「权限标识」列（sys_menu.menu_code，表中没有 perms 列） */
        private String menuCode;

        /** 路由路径 */
        private String path;

        /** 组件路径 */
        private String component;

        /** 图标名 */
        private String icon;

        /** 排序 */
        private Integer sort;

        /** 全局菜单状态：1 = 启用，0 = 停用 */
        private Integer status;

        /** 本租户是否显示该菜单（开关值） */
        private Boolean visible;

        /** 是否禁止关闭（「菜单配置」页自身：自锁保护） */
        private Boolean locked;
    }
}
