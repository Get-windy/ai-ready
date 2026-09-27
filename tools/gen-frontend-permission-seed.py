# -*- coding: utf-8 -*-
"""
为「前端在用、sys_permission 里没有」的权限码生成 Flyway 迁移（只生成 SQL，不执行、不连库）。

背景与归属（用户 2026-09-27 明确的授权三层模型）：
  系统模块=系统级、其余=租户级；**由系统给租户授予模块使用权**；租户内超管管自己员工角色。
  ⇒ 「权限点不存在」属**系统级定义缺失**，必须在系统层补码；补完才谈得上授给谁。

字段口径沿用本仓既有权限种子迁移（V11.394/407/417/419 与 V11.5xx）：
  tenant_id=0（=系统级定义，实测 1465 条走这个口径）
  parent_id=0 / deleted=0 / permission_type=3（按钮级）/ api_path=NULL / method=NULL
  visible=1 / status=0
id 号段：131000 起（实测 91000-99999 已用 284、<20 万 max=130005，131000+ 空闲）
sys_role_permission id 号段：9169200 起（9169100+ 实测空闲）
授权口径：只授 **SYSTEM_ADMIN**（租户内最高管理角色）。SUPER_ADMIN 走 `*` 通配无需授权行；
  其余角色按模型属租户超管自行分配，迁移不代替租户决策。

用法:
  python tools/gen-frontend-permission-seed.py            # 生成迁移文件
  python tools/gen-frontend-permission-seed.py --dry      # 只打印 SQL 不写文件
"""
import io
import json
import os
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PLAN = os.path.join(ROOT, 'tool-results', 'permission-code-fix-plan.json')
MIGRATION_DIR = os.path.join(ROOT, 'backend', 'core', 'api', 'core-api', 'src', 'main',
                             'resources', 'db', 'migration')
VERSION = 'V11.519.0'
FILENAME = f'{VERSION}__Seed_Frontend_Referenced_Permissions.sql'

ID_START = 131000          # sys_permission 主键起点
RP_ID_START = 9169200      # sys_role_permission 主键起点

# 权限点中文名（业务白话，与既有 permission_name 同构：`模块-动作`）
NAME_MAP = {
    'sale:return-doc:print': '销售退货单-打印',
    'finance:receivable:writeoff': '应收-核销',
    'finance:receivable:baddebt': '应收-坏账',
    'finance:payable:writeoff': '应付-核销',
    'finance:pre-payment:offset': '预付款-冲抵',
    'finance:pre-payment:recover': '预付款-收回',
    'finance:pre-payment:refund': '预付款-退款',
    'finance:payment:cancel': '付款单-取消',
    'finance:payment:complete': '付款单-完成',
    'finance:receipt:cancel': '收款单-取消',
    'finance:receipt:complete': '收款单-完成',
    'finance:offset:edit': '核销单-编辑',
    'finance:offset:delete': '核销单-删除',
    'finance:deposit:view': '押金-查看',
    'finance:writeoff:edit': '核销-编辑',
    'supplier:inquiry:acceptquotation': '供应商询价-接受报价',
    'supplier:inquiry:rejectquotation': '供应商询价-拒绝报价',
    'supplier:performance:evaluate': '供应商绩效-评分',
    'supplier:portal': '供应商门户-进入',
    'erp:serial:delete': '序列号-删除',
    'erp:serial:export': '序列号-导出',
    'erp:batch:delete': '批次-删除',
    'erp:product:inventory-mode': '商品-库存模式设置',
    'erp:partner:toggle-status': '往来单位-启用停用',
    'erp:fixed-asset:asset:depreciate': '固定资产-计提折旧',
    'erp:fixed-asset:depreciation:calculate': '折旧-计提计算',
    'erp:fixed-asset:purchase:accept': '固资采购-验收',
    'erp:fixed-asset:report:query': '固资报表-查询',
    'order:center:batchexport': '订单中心-批量导出',
    "erp:fixed-asset:asset:depreciate": "固定资产-计提折旧",
    "erp:fixed-asset:depreciation:calculate": "折旧-计提计算",
    "erp:fixed-asset:purchase:accept": "固资采购-验收",
    "erp:fixed-asset:report:query": "固资报表-查询",
    "erp:partner:toggle-status": "往来单位-启用停用",
    "erp:product:inventory-mode": "商品-库存模式设置",
    "erp:serial:delete": "序列号-删除",
    "erp:serial:export": "序列号-导出",
    "finance:deposit:view": "押金-查看",
    "finance:offset:delete": "核销单-删除",
    "finance:offset:edit": "核销单-编辑",
    "finance:payable:writeoff": "应付-核销",
    "finance:payment:cancel": "付款单-取消",
    "finance:payment:complete": "付款单-完成",
    "finance:pre-payment:offset": "预付款-冲抵",
    "finance:pre-payment:recover": "预付款-收回",
    "finance:pre-payment:refund": "预付款-退款",
    "finance:receipt:cancel": "收款单-取消",
    "finance:receipt:complete": "收款单-完成",
    "finance:receivable:baddebt": "应收-坏账",
    "finance:receivable:writeoff": "应收-核销",
    "finance:writeoff:edit": "核销-编辑",
    "order:center:batchexport": "订单中心-批量导出",
    "sale:return-doc:print": "销售退货单-打印",
    "supplier:inquiry:acceptquotation": "供应商询价-接受报价",
    "supplier:inquiry:rejectquotation": "供应商询价-拒绝报价",
    "supplier:performance:evaluate": "供应商绩效-评分",
    "supplier:portal": "供应商门户-进入",
}


def sql_quote(text):
    return "'" + str(text).replace("'", "''") + "'"


def main():
    plan = json.load(open(PLAN, encoding='utf-8'))
    codes = sorted(p['code'] for p in plan)
    missing_names = [c for c in codes if c not in NAME_MAP]
    if missing_names:
        print('✗ 以下码还没有中文名，先在 NAME_MAP 里补上：')
        for c in missing_names:
            print('   ', c)
        sys.exit(1)

    lines = []
    lines.append(f'-- {VERSION} 补齐「前端 v-permission 在用、库里没有」的权限点（共 {len(codes)} 个）')
    lines.append('--')
    lines.append('-- 背景：v-permission 校验不到就 removeChild（不是置灰）⇒ 码不存在 = 按钮对所有人')
    lines.append('--       隐藏且不报错（管理员也看不见）。后端侧已实测 missing_in_db=0，所以这些都是')
    lines.append('--       前端在用的功能点，属**系统级权限点定义缺失**：系统模块是系统级的，其余模块是')
    lines.append('--       租户级的，但模块使用权由系统授予，租户无权凭空造权限点。')
    lines.append('--')
    lines.append('-- 口径：tenant_id=0（系统级定义）/ permission_type=3（按钮级）/ visible=1 / status=0')
    lines.append(f'-- 授权：只授 SYSTEM_ADMIN（租户内最高管理角色）；SUPER_ADMIN 走 * 通配无需授权行；')
    lines.append('--       其余角色按「租户超管自行分配」的模型，不在本迁移里替租户决定。')
    lines.append('--')
    lines.append(f'-- 【回滚】DELETE FROM sys_role_permission WHERE id BETWEEN {RP_ID_START} AND {RP_ID_START + len(codes)};')
    lines.append(f'--         DELETE FROM sys_permission     WHERE id BETWEEN {ID_START} AND {ID_START + len(codes) - 1};')
    lines.append('')
    lines.append('-- ── ① 权限点定义（已存在则跳过，可重复执行）──')
    lines.append('INSERT INTO sys_permission')
    lines.append('  (id, tenant_id, parent_id, deleted, create_time, update_time, permission_name,')
    lines.append('   permission_code, permission_type, path, component, icon, api_path, method,')
    lines.append('   sort, visible, status, remark)')
    lines.append('VALUES')
    rows = []
    for idx, code in enumerate(codes):
        rows.append(
            f'  ({ID_START + idx}, 0, 0, 0, now(), now(), {sql_quote(NAME_MAP[code])},\n'
            f'   {sql_quote(code)}, 3, NULL, NULL, NULL, NULL, NULL,\n'
            f'   {2000 + idx}, 1, 0, {sql_quote("2026-09-27 前端 v-permission 漂移修复补齐")})'
        )
    lines.append(',\n'.join(rows))
    lines.append('ON CONFLICT (id) DO NOTHING;')
    lines.append('')
    lines.append('-- ── ② 授给系统管理员（租户内最高管理角色）──')
    lines.append('INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)')
    lines.append('SELECT')
    lines.append(f'  {RP_ID_START} + row_number() OVER (ORDER BY p.id),')
    lines.append('  r.id, p.id, 1, now()')
    lines.append('FROM sys_role r')
    lines.append('CROSS JOIN sys_permission p')
    lines.append("WHERE r.role_code = 'SYSTEM_ADMIN'")
    lines.append(f'  AND p.id BETWEEN {ID_START} AND {ID_START + len(codes) - 1}')
    lines.append('  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp')
    lines.append('                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);')
    lines.append('')
    sql = '\n'.join(lines)

    if '--dry' in sys.argv:
        print(sql)
        print(f'\n[{len(codes)} 个权限点] 未写文件（--dry）')
        return

    out = os.path.join(MIGRATION_DIR, FILENAME)
    with open(out, 'w', encoding='utf-8') as f:
        f.write(sql)
    print(f'✓ 已写出 {os.path.relpath(out, ROOT)}')
    print(f'  权限点 {len(codes)} 个，id {ID_START}–{ID_START + len(codes) - 1}')
    print(f'  授权行 id {RP_ID_START}–{RP_ID_START + len(codes)}（授 SYSTEM_ADMIN）')


if __name__ == '__main__':
    main()
