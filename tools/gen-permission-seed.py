# -*- coding: utf-8 -*-
"""
生成「补齐 90 个缺失权限码」的 Flyway 迁移。

背景：代码里 @SaCheckPermission 引用了 90 个权限码，但 sys_permission 中不存在。
鉴权取码走 `SysUserMapper.xml` 的 INNER JOIN sys_permission，码不存在 ⇒ 谁都拿不到
⇒ 这些接口对**非超管一律 403**（超管走 ["*"] 通配所以看不出来）。

本脚本只生成 SQL，不执行、不连库。

字段口径完全沿用 V11.419.0（同 V11.394.0 / V11.407.0 / V11.417.0）：
  tenant_id=1 / parent_id=0 / deleted=0 / permission_type=3 / api_path=NULL / method=NULL
  visible=1 / status=0
  id 号段 91603 起（91601/91602 已被 V11.419.0 占用，实测 91603~91692 空闲）
  sys_role_permission id 号段 9169100 起（实测空闲）
  角色口径：关联给 role_id = 1（超级管理员），与既有迁移一致

用法: python tools/gen-permission-seed.py
"""
import os
import sys
import io
import json

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'tools', 'audit-permission-codes.json')
DST = os.path.join(ROOT, 'backend', 'core', 'api', 'core-api', 'src', 'main',
                   'resources', 'db', 'migration',
                   'V11.423.0__Seed_Missing_Permission_Codes.sql')

MOD = {
    'dms': '配送', 'erp': 'ERP', 'system': '系统', 'platform': '平台',
    'finance': '财务', 'purchase': '采购', 'purchase_order': '采购订单',
    'sale': '销售', 'log': '日志', 'permission-template': '权限模板',
    'role-inheritance': '角色继承',
}
SEG = {
    'channel': '渠道', 'config': '配置', 'dispatch': '派单', 'event': '事件',
    'execution': '执行', 'rider': '骑手', 'sign': '签到', 'tracking': '轨迹',
    'vehicle': '车辆', 'product': '商品', 'mail': '邮件', 'sms': '短信',
    'storage': '存储', 'security': '安全', 'subject': '科目',
    'cost-sharing': '费用分摊', 'order': '订单', 'template': '模板',
    'inheritance': '继承', 'oper': '操作', 'policy': '策略', 'barcode': '条码',
}
ACT = {
    'create': '新增', 'update': '编辑', 'delete': '删除', 'list': '列表',
    'view': '查看', 'approve': '审批', 'audit': '审核', 'submit': '提交',
    'config': '配置', 'query': '查询', 'export': '导出', 'import': '导入',
    'assign': '指派', 'auto': '自动派单', 'candidates': '候选', 'fence': '围栏',
    'reassign': '改派', 'retry': '重试', 'operate': '操作', 'report': '上报',
    'price-batch': '批量改价', 'status': '状态变更', 'apply': '应用',
    'detail': '详情', 'update-status': '状态更新', 'test': '测试',
    'history': '历史', 'edit': '编辑', 'stats': '统计', 'reconcile': '对账',
    'close': '结账', 'cancel': '取消', 'complete': '完成', 'submit-approval': '提交审批',
}


def cn_name(code):
    parts = code.split(':')
    if len(parts) == 1:
        return code
    out = MOD.get(parts[0], parts[0])
    for mid in parts[1:-1]:
        out += SEG.get(mid, mid[:1].upper() + mid[1:])
    out += ACT.get(parts[-1], parts[-1])
    return out


def main():
    data = json.load(open(SRC, encoding='utf-8'))
    codes = sorted(data['missing_in_db'])
    n = len(codes)

    lines = []
    lines.append('-- =============================================================================')
    lines.append('-- 补齐「代码引用但库中缺失」的 %d 个权限码 + 关联超管角色' % n)
    lines.append('-- V11.423.0 · 2026-09-19（由 tools/gen-permission-seed.py 生成）')
    lines.append('--')
    lines.append('-- 【问题】鉴权取码 SQL（core/base/core-base/src/main/resources/mapper/SysUserMapper.xml:48-59）')
    lines.append('--   走 `INNER JOIN sys_permission`，**权限码必须在 sys_permission 里有行**才能被任何人拿到。')
    lines.append('--   实测（tools/audit-permission-codes.py）：代码引用 243 个码，库中 364 个，交叉命中仅 153 个')
    lines.append('--   ⇒ %d 个码所对应的接口对**非超管用户一律 403**。超管走 UnifiedPermissionCacheService' % n)
    lines.append('--   的 ["*"] 通配，所以只有超管不受影响——这也掩盖了问题。')
    lines.append('--')
    lines.append('--   典型：erp:product:list 被引用 24 处、erp:product:update 12 处，')
    lines.append('--   而库中 `erp:product%` 实测 0 行 ⇒ 商品管理整块对非超管不可用。')
    lines.append('--   同理 dms:* 整族 23 个（配送模块）、platform:* 11 个（平台配置）。')
    lines.append('--')
    lines.append('-- 【字段口径】完全沿用 V11.419.0（同 V11.394.0 / V11.407.0 / V11.417.0）：')
    lines.append('--   tenant_id=1 / parent_id=0 / deleted=0 / permission_type=3')
    lines.append('--   api_path=NULL / method=NULL / visible=1 / status=0')
    lines.append('--   sort 从 942 起递增（V11.419.0 用到 941）。')
    lines.append('--')
    lines.append('-- 【id 号段纪律】落地前实测（devdb，2026-09-19）：')
    lines.append('--   SELECT count(*) FROM sys_permission      WHERE id BETWEEN 91603 AND 91692   → 0')
    lines.append('--   SELECT count(*) FROM sys_role_permission WHERE id BETWEEN 9169100 AND 9169300 → 0')
    lines.append('--   `sys_permission.id` / `sys_role_permission.id` 均无序列默认值，必须显式给值。')
    lines.append('--   迁移版本号：实测该目录最大为 V11.422.0，故取 V11.423.0。')
    lines.append('--')
    lines.append('-- 【幂等】两张表均无 (业务列) 唯一约束 ⇒ 一律用 NOT EXISTS 守卫，重复执行安全。')
    lines.append('--')
    lines.append('-- 【本迁移不做的事】只登记权限码 + 关联超管。**不给普通角色授权**——')
    lines.append('--   普通角色（SYSTEM_ADMIN/DEPT_ADMIN 等）该不该有这些权限属业务决策，')
    lines.append('--   由租户在「权限配置」页自行勾选。这样本迁移不改变任何现有账号的实际权限，')
    lines.append('--   只把「无法授予」变为「可以授予」。')
    lines.append('--')
    lines.append('-- 【回滚】')
    lines.append('--   DELETE FROM sys_role_permission WHERE permission_id IN')
    lines.append('--     (SELECT id FROM sys_permission WHERE id BETWEEN 91603 AND %d);' % (91602 + n))
    lines.append('--   DELETE FROM sys_permission WHERE id BETWEEN 91603 AND %d;' % (91602 + n))
    lines.append('-- =============================================================================')
    lines.append('')
    lines.append('-- ── ① %d 个权限码（id 91603~%d，sort 942 起）──────────────────────────────' % (n, 91602 + n))
    lines.append('INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,')
    lines.append('                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)')
    rows = []
    for i, code in enumerate(codes):
        pid = 91603 + i
        name = cn_name(code).replace("'", "''")
        rows.append(
            "SELECT %d, 1, 0, 0, now(), now(), '%s', '%s', 3, NULL, NULL, %d, 1, 0\n"
            "WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = '%s')"
            % (pid, name, code, 942 + i, code))
    lines.append('\nUNION ALL\n'.join(rows) + ';')
    lines.append('')
    lines.append('-- ── ② 关联给超级管理员（role_id = 1），口径同 V11.394.0 / V11.407.0 / V11.417.0 / V11.419.0 ──')
    lines.append('INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)')
    lines.append('SELECT 9169100 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()')
    lines.append('FROM sys_permission p')
    lines.append('WHERE p.id BETWEEN 91603 AND %d' % (91602 + n))
    lines.append('  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp')
    lines.append('                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);')
    lines.append('')

    with open(DST, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))

    print('已生成 %s' % os.path.relpath(DST, ROOT))
    print('  权限码 %d 个（id 91603~%d）' % (n, 91602 + n))
    print('  角色关联 %d 条（id 9169100 起，role_id=1）' % n)
    print()
    print('=== 中文名抽样（前 12 个）===')
    for code in codes[:12]:
        print('  %-34s %s' % (code, cn_name(code)))


if __name__ == '__main__':
    main()
