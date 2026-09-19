package cn.aiedge.base.service;

import java.util.Set;

/**
 * 租户级「菜单显隐」服务（设置 → 系统配置 → 菜单配置，菜单 80620）。
 *
 * <p><b>落位（配置中心 DB，通道 3）</b>：{@code sys_project_config}
 * —— 该表自带 {@code tenant_id}，且是本仓库既有的「租户级通用键值配置」载体
 * （{@code UserPageConfigController} 已用它存列配置 / 页面配置）。
 * 配置键 {@link #CONFIG_KEY}，配置值 = 本租户**隐藏**的菜单 id 的 JSON 数组
 * （存「隐藏集」而非「显示集」：默认全部显示，隐藏是增量覆盖，
 * 因此平台新增菜单后租户端自动可见，不会被老的快照挡住）。</p>
 *
 * <p><b>为什么不写 {@code sys_menu.visible}</b>：{@code sys_menu} 是**全局菜单定义**
 * （已列入 {@code MyBatisPlusConfig.IGNORE_TENANT_TABLES}），改它会影响所有租户，
 * 与设置模块「只改本租户数据」的红线冲突。</p>
 *
 * <p><b>为什么不写 {@code sys_tenant_menu}</b>：该表是**平台 → 租户的授权集**
 * （系统模块「模块授权」页维护，且系统租户 {@code tenant_id = 1} 在
 * {@code SysMenuServiceImpl.getUserMegaMenus} 中被显式跳过），
 * 它表达不了「租户在平台授权范围内自己关掉一部分」。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface MenuVisibilityService {

    /** 配置键（{@code sys_project_config.config_key}） */
    String CONFIG_KEY = "set:menu-config:hidden";

    /** 配置分组（{@code sys_project_config.config_group}），便于按组检索与清理 */
    String CONFIG_GROUP = "set_menu_config";

    /**
     * 读取本租户已隐藏（关闭）的菜单 id 集合。
     *
     * @param tenantId 租户 ID（取自登录会话，不接受前端传入）
     * @return 隐藏的菜单 id 集合；无配置或配置为空时返回空集合（= 全部显示）
     */
    Set<Long> getHiddenMenuIds(Long tenantId);

    /**
     * 设置某个菜单在本租户的显隐（开 = 从隐藏集移除，关 = 加入隐藏集）。
     *
     * @param tenantId 租户 ID（取自登录会话，不接受前端传入）
     * @param menuId   菜单 ID
     * @param visible  true = 显示；false = 隐藏
     */
    void updateVisibility(Long tenantId, Long menuId, boolean visible);
}
