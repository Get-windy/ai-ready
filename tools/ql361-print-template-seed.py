# -*- coding: utf-8 -*-
"""
把转换好的模板写进 sys_print_template（打印模板表）。

字段口径对齐后端 PrintTemplateV2Controller#create：
  tenant_id / page_code / template_name / template_json / paper_size /
  paper_width / paper_height / margin_* / status / is_default / version
  · 唯一性 = (tenant_id, page_code, template_name)，重复会插入失败（脚本先查再插）
  · template_json 是 jsonb：直接传 JSON 字符串，PG 会校验

用法：
  python tools/ql361-print-template-seed.py <转换后JSON> <page_code> [--name 模板名] [--apply]
    默认 dry-run；加 --apply 才落库；加 --update 则改写同名模板而非新增。
    例：python tools/ql361-print-template-seed.py \
          tool-results/.../_转换_28237.json sale --name "H皇朝销售订单" --apply
        python tools/ql361-print-template-seed.py \
          tool-results/print-template-v2/sale.json sale --name "销售订单（标准 v2）" --apply --update

⚠️ page_code 必须与前端 PrintDialog 的 page-code 属性一致，否则页面取不到模板。
   已确认的前端 page-code：sale / sale-exchange / sale-return-doc /
                          purchase / purchase-receive / ship-query / dispatch-query / dispatch-task
"""
import io
import json
import os
import sys

import psycopg2

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
DSN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')
TENANT_ID = 1          # 与库中既有业务数据同租户；打印模板按租户各自维护


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    apply_ = '--apply' in sys.argv
    update_ = '--update' in sys.argv
    # --template-id N：精确改写某一行。比按名字匹配可靠 ——
    # 名字里带空格时，命令行一不小心被拆词就会「匹配不到 → 插一条新的」，
    # 库里凭空多出 `销售订单（标准` 这种半截名字的行（本脚本 2026-09-26 真踩过）。
    template_id = None
    if '--template-id' in sys.argv:
        template_id = int(sys.argv[sys.argv.index('--template-id') + 1])
    name = None
    if '--name' in sys.argv:
        name = sys.argv[sys.argv.index('--name') + 1]
    if len(args) < 2:
        print(__doc__)
        sys.exit(1)
    src, page_code = args[0], args[1]

    tpl = json.load(open(src, encoding='utf-8'))
    tmpl_name = name or tpl.get('templateName') or os.path.basename(src)
    # v1 = components（绝对坐标，ql361 转换产物）
    # v2 = sections（分区流式，手建模板）
    comps = tpl.get('components') or []
    sections = tpl.get('sections') or {}
    if not comps and not sections:
        print('✗ 模板里既没有 components（v1）也没有 sections（v2），拒绝落库')
        sys.exit(2)

    conn = psycopg2.connect(**DSN)
    cur = conn.cursor()
    cur.execute("SELECT template_id, template_name FROM sys_print_template "
                "WHERE tenant_id=%s AND page_code=%s AND deleted=0", (TENANT_ID, page_code))
    exist = cur.fetchall()
    print(f"目标 page_code = {page_code}（tenant={TENANT_ID}）")
    print(f"  该页已有模板: {[(i, n) for i, n in exist] or '无'}")
    shape = (f"v2 sections={list(sections)}" if sections
             else f"v1 components={len(comps)} 个")
    print(f"  将写入: 「{tmpl_name}」  {shape}")

    # --update：同名模板已存在就改写正文，而不是再插一条。
    # --template-id：直接指定要改写的行（最可靠，推荐）。
    if template_id is not None:
        cur.execute("SELECT page_code, template_name FROM sys_print_template "
                    "WHERE template_id=%s AND tenant_id=%s AND deleted=0", (template_id, TENANT_ID))
        row = cur.fetchone()
        if not row:
            print(f"✗ 没有 template_id={template_id} 的模板")
            sys.exit(2)
        if row[0] != page_code:
            print(f"✗ template_id={template_id} 属于 page_code={row[0]}，"
                  f"与本次要写的 {page_code} 不符 —— 拒绝改写（防止把模板写到别的页面）")
            sys.exit(2)
        same = [template_id]
        tmpl_name = name or row[1]
        print(f"  [--template-id] 将改写 template_id={template_id}「{tmpl_name}」的正文")
    else:
        same = [tid for tid, tname in exist if tname == tmpl_name] if update_ else []
        if update_ and same:
            print(f"  [--update] 将改写已存在的 template_id={same[0]} 的正文")
        elif update_ and not same:
            print(f"  [--update] 该页没有名为「{tmpl_name}」的模板，改为新增")

    if not apply_:
        conn.rollback()
        print("\n[DRY-RUN] 未落库（加 --apply 执行）")
        cur.close()
        conn.close()
        return

    from psycopg2.extras import Json
    payload = {k: v for k, v in tpl.items() if k != 'templateName'}

    if same:
        cur.execute("""
            UPDATE sys_print_template
               SET template_json=%s, paper_size=%s, paper_width=%s, paper_height=%s,
                   updated_at=now()
             WHERE template_id=%s
        """, (Json(payload), 'A4', tpl.get('pageWidth') or None,
              tpl.get('pageHeight') or None, same[0]))
        conn.commit()
        print(f"\n✓ 已更新 template_id={same[0]}，page_code={page_code}")
        cur.execute("SELECT template_id, page_code, template_name, is_default, status, "
                    "length(template_json::text) FROM sys_print_template "
                    "WHERE tenant_id=%s AND deleted=0 ORDER BY template_id", (TENANT_ID,))
        for r in cur.fetchall():
            print('   ·', r)
        cur.close()
        conn.close()
        return
    # status 是 varchar 枚举，取值只有 DRAFT / PUBLISHED / DISABLED。
    # 写 'active' 之类的自造值，PrintDialog（只取 PUBLISHED）就再也看不到这条模板。
    cur.execute("""
        INSERT INTO sys_print_template
          (tenant_id, page_code, template_name, template_json, paper_size,
           paper_width, paper_height, status, is_default, version, created_at, updated_at, deleted)
        VALUES (%s,%s,%s,%s,%s,%s,%s,'PUBLISHED',%s,1, now(), now(), 0)
        RETURNING template_id
    """, (
        TENANT_ID, page_code, tmpl_name, Json(payload),
        'A4',
        tpl.get('pageWidth') or None,
        tpl.get('pageHeight') or None,
        len(exist) == 0,          # 该页第一个模板置为默认
    ))
    new_id = cur.fetchone()[0]
    conn.commit()
    print(f"\n✓ 已写入 template_id={new_id}，page_code={page_code}，is_default={len(exist) == 0}")
    cur.execute("SELECT template_id, page_code, template_name, is_default, status FROM sys_print_template "
                "WHERE tenant_id=%s AND deleted=0 ORDER BY template_id", (TENANT_ID,))
    for r in cur.fetchall():
        print('   ·', r)
    cur.close()
    conn.close()


if __name__ == '__main__':
    main()
