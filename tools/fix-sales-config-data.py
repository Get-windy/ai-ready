# -*- coding: utf-8 -*-
"""
销售模块配置数据订正（2026-09-23，用户批准后执行）。

对应 SALES_MODULE_AUDIT_20260922.md §八 的 A1/A2/A3/A5 四项。**只改数据，不改结构**。

  A1  删除 6 条永不生效的影子菜单（client_type='pc-admin'）：
      80091 销售退货申请 / 80092 销售退货申请表单 / 80601 新增换货单 /
      80602 编辑换货单 / 80603 新增退货单 / 80604 编辑退货单
      连带清理 sys_tenant_menu 里指向它们的 3 条授权行。
      前置：这些路径的路由已在前端 getRequiredRoutes() 补齐（见 verify-sales-routes.cjs 22/22），
            删除后功能不受影响；70011/70022/70023（tenant-admin 正主）保持不动。

  A2  删除 3 个僵尸权限码：sale:discount:edit / sale:manage / sale:settle:force
      （代码与前端全仓零引用、无端点挂载）。连带清理角色授权行。

  A3  把 sale:* 权限的中英混杂名改成业务白话：
      「销售exchange审批」→「销售换货单-审批」

  A5  会计科目 1403「库存商品」更名「原材料」——
      《企业会计准则》1403=原材料、1405=库存商品；此前两者同名，
      凭证两侧各用一个（出库/退货已统一按 1405 记账，历史凭证不动）。

用逻辑删（deleted=1），与系统「删除菜单/权限」接口（MyBatis-Plus @TableLogic）行为一致，可回滚。

用法：
  python tools/fix-sales-config-data.py --dry-run   # 只看将改什么，不落库
  python tools/fix-sales-config-data.py --apply     # 真正执行
"""
import io
import json
import os
import sys

import psycopg2

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

DSN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')

SHADOW_MENUS = [80091, 80092, 80601, 80602, 80603, 80604]
ZOMBIE_CODES = ['sale:discount:edit', 'sale:manage', 'sale:settle:force']

# 资源码 → 业务名（覆盖 sale:* 全部资源段）
RES = {
    'sale:order': '销售订单', 'sale:outbound': '销售出库单', 'sale:return-doc': '销售退货单',
    'sale:return': '销售退货申请', 'sale:exchange': '销售换货单', 'sale:retail': '零售单',
    'sale:retail-shift': '零售交班', 'sale:pre-order': '预订货单', 'sale:promotion': '促销活动',
    'sale:logistics': '销售物流', 'sale:price': '销售价格', 'sale:doc-query': '销售单据查询',
    'sale:detail-query': '销售明细查询', 'sale:analysis': '销售分析',
    'sale:analysis-promotion-funnel': '促销漏斗分析', 'sale:pre-order-analysis': '预订货单分析',
    'sale:out': '销售出库确认',
}
ACT = {
    'create': '新增', 'update': '编辑', 'delete': '删除', 'list': '查询', 'detail': '详情',
    'view': '查看', 'approve': '审批', 'submit': '提交', 'cancel': '取消', 'complete': '完成',
    'export': '导出', 'import': '导入', 'print': '打印', 'ship': '发货', 'payment': '收款',
    'edit': '修改', 'publish': '发布', 'close': '交班', 'confirm': '确认',
}


def cn_name(code: str):
    """sale:outbound:approve → 销售出库单-审批；解析不出则返回 None（不猜）"""
    parts = code.split(':')
    if len(parts) < 3:
        return None
    action = parts[-1]
    res = ':'.join(parts[:-1])
    if res not in RES or action not in ACT:
        return None
    return f"{RES[res]}-{ACT[action]}"


def main():
    apply = '--apply' in sys.argv
    backup_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                               'tool-results', 'fix-sales-config-backup.json')
    conn = psycopg2.connect(**DSN)
    cur = conn.cursor()

    # ── 备份（无论 dry-run 与否都先导出，便于回滚） ──
    backup = {}
    cur.execute("SELECT row_to_json(t) FROM (SELECT * FROM sys_menu WHERE id = ANY(%s)) t", (SHADOW_MENUS,))
    backup['sys_menu'] = [r[0] for r in cur.fetchall()]
    cur.execute("SELECT row_to_json(t) FROM (SELECT * FROM sys_tenant_menu WHERE menu_id = ANY(%s)) t", (SHADOW_MENUS,))
    backup['sys_tenant_menu'] = [r[0] for r in cur.fetchall()]
    cur.execute("SELECT row_to_json(t) FROM (SELECT * FROM sys_permission WHERE permission_code = ANY(%s)) t", (ZOMBIE_CODES,))
    backup['sys_permission'] = [r[0] for r in cur.fetchall()]
    cur.execute("SELECT row_to_json(t) FROM (SELECT * FROM sys_permission WHERE permission_code LIKE 'sale:%%') t")
    backup['sys_permission_sale_all'] = [r[0] for r in cur.fetchall()]
    cur.execute("SELECT row_to_json(t) FROM (SELECT * FROM finance_account_subject WHERE subject_code IN ('1403','1405')) t")
    backup['finance_account_subject'] = [r[0] for r in cur.fetchall()]
    os.makedirs(os.path.dirname(backup_path), exist_ok=True)
    with open(backup_path, 'w', encoding='utf-8') as f:
        json.dump(backup, f, ensure_ascii=False, indent=1, default=str)
    print(f"[备份] 已导出到 {backup_path}")
    print(f"       sys_menu {len(backup['sys_menu'])} 行 · sys_tenant_menu {len(backup['sys_tenant_menu'])} 行 · "
          f"sys_permission {len(backup['sys_permission'])} 僵尸行 · sale:码快照 {len(backup['sys_permission_sale_all'])} 行 · "
          f"科目 {len(backup['finance_account_subject'])} 行")

    # ── A1 删影子菜单 ──
    print("\n=== A1 删除 6 条影子菜单 ===")
    cur.execute("SELECT id, menu_name, path, client_type FROM sys_menu WHERE id = ANY(%s) AND deleted = 0 ORDER BY id",
                (SHADOW_MENUS,))
    for r in cur.fetchall():
        print(f"  · {r[0]}  {r[1]}  ({r[2]})  client_type={r[3]}")
    if apply:
        cur.execute("UPDATE sys_menu SET deleted = 1 WHERE id = ANY(%s) AND deleted = 0", (SHADOW_MENUS,))
        print(f"  → sys_menu 逻辑删除 {cur.rowcount} 行")
        cur.execute("UPDATE sys_tenant_menu SET deleted = 1 WHERE menu_id = ANY(%s) AND deleted = 0", (SHADOW_MENUS,))
        print(f"  → sys_tenant_menu 连带清理 {cur.rowcount} 行")

    # ── A2 删僵尸权限码 ──
    print("\n=== A2 删除 3 个僵尸权限码 ===")
    cur.execute("SELECT id, permission_code, permission_name FROM sys_permission WHERE permission_code = ANY(%s) AND deleted = 0",
                (ZOMBIE_CODES,))
    rows = cur.fetchall()
    for r in rows:
        cur.execute("SELECT count(*) FROM sys_role_permission WHERE permission_id = %s", (r[0],))
        print(f"  · {r[1]}  「{r[2]}」  角色授权 {cur.fetchone()[0]} 条")
    if apply and rows:
        ids = [r[0] for r in rows]
        cur.execute("DELETE FROM sys_role_permission WHERE permission_id = ANY(%s)", (ids,))
        print(f"  → 清理角色授权 {cur.rowcount} 行")
        cur.execute("UPDATE sys_permission SET deleted = 1 WHERE id = ANY(%s)", (ids,))
        print(f"  → sys_permission 逻辑删除 {cur.rowcount} 行")

    # ── A3 权限名业务白话 ──
    print("\n=== A3 权限名中英混杂 → 业务白话 ===")
    cur.execute("SELECT id, permission_code, permission_name FROM sys_permission "
                "WHERE permission_code LIKE 'sale:%%' AND deleted = 0 AND permission_name ~ '[A-Za-z]' ORDER BY permission_code")
    plans = []
    for pid, code, old in cur.fetchall():
        new = cn_name(code)
        if new and new != old:
            plans.append((pid, code, old, new))
    print(f"  命中 {len(plans)} 条；样例：")
    for pid, code, old, new in plans[:6]:
        print(f"  · {code:45s} 「{old}」 → 「{new}」")
    if len(plans) > 6:
        print(f"  · …（其余 {len(plans) - 6} 条同理）")
    unmapped = [c for pid, c, o, n in [] ]  # 占位
    cur.execute("SELECT permission_code, permission_name FROM sys_permission "
                "WHERE permission_code LIKE 'sale:%%' AND deleted = 0 AND permission_name ~ '[A-Za-z]'")
    for code, old in cur.fetchall():
        if cn_name(code) is None:
            print(f"  ! 无法解析资源/动作，跳过：{code} 「{old}」")
    if apply and plans:
        for pid, code, old, new in plans:
            cur.execute("UPDATE sys_permission SET permission_name = %s WHERE id = %s", (new, pid))
        print(f"  → 更新 {len(plans)} 行")

    # ── A5 科目更名 ──
    print("\n=== A5 科目 1403「库存商品」→「原材料」 ===")
    cur.execute("SELECT subject_code, subject_name FROM finance_account_subject "
                "WHERE subject_code IN ('1403','1405') AND deleted_flag = 0")
    for r in cur.fetchall():
        print(f"  · {r[0]}  当前名「{r[1]}」")
    if apply:
        cur.execute("UPDATE finance_account_subject SET subject_name = '原材料' "
                    "WHERE subject_code = '1403' AND subject_name = '库存商品' AND deleted_flag = 0")
        print(f"  → 改名 {cur.rowcount} 行（1405 仍为「库存商品」）")

    if apply:
        conn.commit()
        print("\n[已提交]")
    else:
        conn.rollback()
        print("\n[DRY-RUN] 未落库（加 --apply 执行）")
    cur.close()
    conn.close()


if __name__ == '__main__':
    main()
