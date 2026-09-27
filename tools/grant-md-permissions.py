# -*- coding: utf-8 -*-
"""
资料模块权限授权（一次性执行脚本，2026-09-26）。

矩阵：
  · SYSTEM_ADMIN  系统管理员   → 资料模块全部 161 个在用权限码
  · DEPT_ADMIN    部门管理员   → 其中只读子集 80 个（:list/:view/:detail/:query/:export）
  · SUPER_ADMIN   超级管理员   → 已有通配符 *（无需授权行）
  · E2E_T2_ADMIN  测试角色     → 不动

背景（2026-09-24 资料模块审计 P0-1）：资料模块 24 个页面用到的 161 个权限码里，
128 个只授给 SUPER_ADMIN ⇒ 真机实测非超管 21/24 个页面接口 403（超管同批 24/24 为 200）。

为什么用**精确码清单**而不是前缀：资料模块的码分散在 9 个命名空间
（md/product/party/wms/finance/erp/pricing/mall/stock），其中 wms:*、finance:*、mall:* 等
同时包含非资料模块的码，按前缀授权会越界把别人的码也授出去。

幂等：按 (role_id, permission_id) 判重，重复执行只补差集。
用法: python tools/grant-md-permissions.py
"""
import os
import json
import sys
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# 资料模块 24 页实际使用的权限码（实测清单，见 tools/audit-md-permission.py）
MD_PERMS = [
    'erp:batch:detail',
    'erp:batch:list',
    'erp:batch:update',
    'erp:product:create',
    'erp:product:delete',
    'erp:product:list',
    'erp:product:price-batch',
    'erp:product:status',
    'erp:product:update',
    'erp:product:view',
    'finance:account:create',
    'finance:account:delete',
    'finance:account:detail',
    'finance:account:export',
    'finance:account:list',
    'finance:account:update',
    'finance:account:view',
    'finance:bank-account:create',
    'finance:bank-account:delete',
    'finance:bank-account:detail',
    'finance:bank-account:export',
    'finance:bank-account:list',
    'finance:bank-account:status',
    'finance:bank-account:update',
    'finance:bank-account:view',
    'mall:tag:create',
    'mall:tag:delete',
    'mall:tag:export',
    'mall:tag:list',
    'mall:tag:update',
    'md:customer:create',
    'md:customer:delete',
    'md:customer:detail',
    'md:customer:export',
    'md:customer:list',
    'md:customer:update',
    'md:customer:view',
    'md:expense-type:create',
    'md:expense-type:delete',
    'md:expense-type:detail',
    'md:expense-type:export',
    'md:expense-type:list',
    'md:expense-type:update',
    'md:expense-type:view',
    'md:image:create',
    'md:image:delete',
    'md:image:list',
    'md:image:view',
    'md:linked-account:create',
    'md:linked-account:delete',
    'md:linked-account:detail',
    'md:linked-account:list',
    'md:linked-account:update',
    'md:linked-account:view',
    'md:other-income:create',
    'md:other-income:delete',
    'md:other-income:detail',
    'md:other-income:export',
    'md:other-income:list',
    'md:other-income:update',
    'md:other-income:view',
    'md:payment-channel:edit',
    'md:payment-channel:export',
    'md:payment-channel:view',
    'md:payment-method:create',
    'md:payment-method:delete',
    'md:payment-method:detail',
    'md:payment-method:export',
    'md:payment-method:list',
    'md:payment-method:status',
    'md:payment-method:update',
    'md:route-master:create',
    'md:route-master:delete',
    'md:route-master:detail',
    'md:route-master:export',
    'md:route-master:import',
    'md:route-master:list',
    'md:route-master:status',
    'md:route-master:update',
    'md:route-master:view',
    'party:attachments:create',
    'party:attachments:delete',
    'party:attachments:detail',
    'party:categories:create',
    'party:categories:delete',
    'party:categories:list',
    'party:categories:update',
    'party:categories:view',
    'party:contacts:create',
    'party:contacts:delete',
    'party:contacts:detail',
    'party:contacts:update',
    'party:customer-region:create',
    'party:customer-region:delete',
    'party:customer-region:detail',
    'party:customer-region:list',
    'party:customer-region:update',
    'party:grades:create',
    'party:grades:detail',
    'party:grades:view',
    'pricing:approval:approve',
    'pricing:approval:create',
    'pricing:approval:detail',
    'pricing:approval:list',
    'pricing:approval:reject',
    'pricing:approval:view',
    'product:barcodes:detail',
    'product:brand:create',
    'product:brand:delete',
    'product:brand:export',
    'product:brand:list',
    'product:brand:update',
    'product:category:create',
    'product:category:delete',
    'product:category:detail',
    'product:category:list',
    'product:category:update',
    'product:grade:create',
    'product:grade:delete',
    'product:grade:detail',
    'product:grade:list',
    'product:grade:update',
    'product:kit:list',
    'product:location:create',
    'product:location:export',
    'product:location:list',
    'product:unit-dict:create',
    'product:unit-dict:delete',
    'product:unit-dict:export',
    'product:unit-dict:list',
    'product:unit-dict:update',
    'product:unit-group:create',
    'product:unit-group:delete',
    'product:unit-group:detail',
    'product:unit-group:export',
    'product:unit-group:list',
    'product:unit-group:update',
    'stock:inventory-mode:list',
    'stock:inventory-mode:update',
    'stock:inventory-mode:view',
    'stock:view',
    'wms:category:create',
    'wms:category:delete',
    'wms:category:detail',
    'wms:category:list',
    'wms:category:update',
    'wms:create',
    'wms:delete',
    'wms:detail',
    'wms:export',
    'wms:list',
    'wms:location:create',
    'wms:location:delete',
    'wms:location:detail',
    'wms:location:export',
    'wms:location:generate',
    'wms:location:list',
    'wms:location:view',
    'wms:view',
    'wms:warehouse:delete',
    'wms:warehouse:view',
]

# 只读子集：查看类动作，供部门管理员按需分配
READONLY = [
    'erp:batch:detail',
    'erp:batch:list',
    'erp:product:list',
    'erp:product:view',
    'finance:account:detail',
    'finance:account:export',
    'finance:account:list',
    'finance:account:view',
    'finance:bank-account:detail',
    'finance:bank-account:export',
    'finance:bank-account:list',
    'finance:bank-account:view',
    'mall:tag:export',
    'mall:tag:list',
    'md:customer:detail',
    'md:customer:export',
    'md:customer:list',
    'md:customer:view',
    'md:expense-type:detail',
    'md:expense-type:export',
    'md:expense-type:list',
    'md:expense-type:view',
    'md:image:list',
    'md:image:view',
    'md:linked-account:detail',
    'md:linked-account:list',
    'md:linked-account:view',
    'md:other-income:detail',
    'md:other-income:export',
    'md:other-income:list',
    'md:other-income:view',
    'md:payment-channel:export',
    'md:payment-channel:view',
    'md:payment-method:detail',
    'md:payment-method:export',
    'md:payment-method:list',
    'md:route-master:detail',
    'md:route-master:export',
    'md:route-master:list',
    'md:route-master:view',
    'party:attachments:detail',
    'party:categories:list',
    'party:categories:view',
    'party:contacts:detail',
    'party:customer-region:detail',
    'party:customer-region:list',
    'party:grades:detail',
    'party:grades:view',
    'pricing:approval:detail',
    'pricing:approval:list',
    'pricing:approval:view',
    'product:barcodes:detail',
    'product:brand:export',
    'product:brand:list',
    'product:category:detail',
    'product:category:list',
    'product:grade:detail',
    'product:grade:list',
    'product:kit:list',
    'product:location:export',
    'product:location:list',
    'product:unit-dict:export',
    'product:unit-dict:list',
    'product:unit-group:detail',
    'product:unit-group:export',
    'product:unit-group:list',
    'stock:inventory-mode:list',
    'stock:inventory-mode:view',
    'stock:view',
    'wms:category:detail',
    'wms:category:list',
    'wms:detail',
    'wms:export',
    'wms:list',
    'wms:location:detail',
    'wms:location:export',
    'wms:location:list',
    'wms:location:view',
    'wms:view',
    'wms:warehouse:view',
]

GRANT_SQL = """
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time, create_by)
SELECT (SELECT COALESCE(max(id), 0) FROM sys_role_permission) + row_number() OVER (ORDER BY p.id),
       %(role)s, p.id, 0, now(), 1
FROM sys_permission p
WHERE p.deleted = 0
  AND p.permission_code = ANY(%(codes)s)
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = %(role)s AND rp.permission_id = p.id)
"""


def main():
    import psycopg2
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT id, role_code FROM sys_role WHERE deleted = 0 AND tenant_id = 1"
                        " AND role_code IN ('SYSTEM_ADMIN','DEPT_ADMIN')")
            roles = {code: rid for rid, code in cur.fetchall()}
            print('角色: %s' % roles)

            ids = list(roles.values())
            cur.execute("SELECT id, role_id, permission_id, tenant_id FROM sys_role_permission"
                        " WHERE role_id = ANY(%s) ORDER BY id", (ids,))
            cols = [c[0] for c in cur.description]
            before = [dict(zip(cols, map(str, r))) for r in cur.fetchall()]
            os.makedirs(os.path.join(ROOT, 'tool-results'), exist_ok=True)
            dest = os.path.join(ROOT, 'tool-results', 'role_permission_backup_before_MD_20260926.json')
            json.dump(before, open(dest, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
            print('已备份受影响角色现有授权 %d 行 → %s' % (len(before), os.path.basename(dest)))

            plans = [(roles.get('SYSTEM_ADMIN'), MD_PERMS, '系统管理员 → 资料模块全部'),
                     (roles.get('DEPT_ADMIN'), READONLY, '部门管理员 → 资料模块只读')]
            for rid, codes, label in plans:
                if rid is None:
                    print('  ! 角色缺失，跳过: %s' % label)
                    continue
                cur.execute(GRANT_SQL, {'role': rid, 'codes': codes})
                print('%-30s 新增 %d 条' % (label, cur.rowcount))

            cur.execute("SELECT r.role_code, count(*) FROM sys_role_permission rp"
                        " JOIN sys_permission p ON p.id = rp.permission_id"
                        " JOIN sys_role r ON r.id = rp.role_id"
                        " WHERE p.deleted = 0 AND p.permission_code = ANY(%s)"
                        " GROUP BY r.role_code ORDER BY r.role_code", (MD_PERMS,))
            print()
            print('=== 授权后各角色持有的资料模块权限码数（总数 %d）===' % len(MD_PERMS))
            for code, cnt in cur.fetchall():
                print('  %-16s %d' % (code, cnt))


if __name__ == '__main__':
    main()
