-- 营销模块菜单新增：80303 营销自动化 / 80304 储值卡（本系统建模页，ql361 无对应页）
--
-- 依据：《系统菜单设计与管理/系统菜单开发文档-营销模块菜单新增.md》（V1.0，2026-09-18）
--       该两页是业界对标调研后确认「结构上无法补进现有 17 页」的唯二缺口：
--         · 营销自动化 = 多条规则 × 多个触发点 × 执行台账（有赞营销画布 / 微盟营销中心同类）
--         · 储值卡     = 独立账户 + 单据（开卡/充值/消费/退款/流水台账）
-- 幂等：INSERT ... ON CONFLICT (id) DO NOTHING（重复执行安全，不覆盖运维已改的名称与排序）
-- 授权：sys_role_menu / sys_tenant_menu 无营销菜单记录（超管全量下发），无需补授权
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component,
                      route_name, redirect, icon, sort,
                      is_external, is_cache, visible, status,
                      client_type, remark, biz_flow_tag, display_group, link_icon,
                      display_mode, list_path, tag_label, menu_level)
VALUES
    (80303, 0, 60801, 0, now(), now(),
     '营销自动化', 'mkt:auto-campaign', 1, 'marketing/auto-campaign', 'views/marketing/auto-campaign/index.vue',
     NULL, NULL, NULL, 4,
     0, 1, 1, 1,
     'tenant-admin', '本系统建模：会员生命周期自动化触达（ql361 无对应页）', NULL, 0, NULL,
     0, NULL, NULL, 3),
    (80304, 0, 60801, 0, now(), now(),
     '储值卡', 'mkt:stored-card', 1, 'marketing/stored-card', 'views/marketing/stored-card/index.vue',
     NULL, NULL, NULL, 5,
     0, 1, 1, 1,
     'tenant-admin', '本系统建模：储值卡档案与收支流水（ql361 无对应页）', NULL, 0, NULL,
     0, NULL, NULL, 3)
ON CONFLICT (id) DO NOTHING;
