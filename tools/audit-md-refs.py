# -*- coding: utf-8 -*-
"""
资料模块主数据「被引用完整性」检查（只读）：
  对资料模块的 5 类核心主数据（往来单位 / 商品 / 仓库 / 会计科目 / 支付方式），
  逐条检查业务单据中的外键 ID 能否命中共主数据表 —— 命中率低或为 0 说明
  存在「孤儿引用」（删了主数据、单据还在）或「口径不一致」（ID 指向了别的表）。
输出: tool-results/md-audit/refs-audit.json
用法: python tools/audit-md-refs.py
"""
import io
import json
import os
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONN = "host=localhost port=5432 dbname=devdb user=devuser password=devuser123"

# (标签, 子表, 子表列, 主子表, 主键, 子表是否逻辑删)
CHECKS = [
    ('商品 · 库存', 'erp_stock', 'product_id', 'erp_product', 'id', True),
    ('商品 · 销售明细', 'erp_sale_order_item', 'product_id', 'erp_product', 'id', False),
    ('商品 · 采购明细', 'erp_purchase_order_item', 'product_id', 'erp_product', 'id', False),
    ('商品 · 条码', 'erp_product_barcode', 'product_id', 'erp_product', 'id', True),
    ('商品 · 单位', 'erp_product_unit', 'product_id', 'erp_product', 'id', True),
    ('商品 · 图片', 'erp_product_image', 'product_id', 'erp_product', 'id', True),
    ('商品 · 推荐位', 'erp_product_shield', 'product_id', 'erp_product', 'id', True),
    ('往来单位 · 销售单', 'erp_sale_order', 'customer_id', 'biz_party', 'id', True),
    ('往来单位 · 销售单(party表)', 'erp_sale_order', 'customer_id', 'party', 'id', True),
    ('往来单位 · 采购单', 'erp_purchase_order', 'supplier_id', 'biz_party', 'id', True),
    ('往来单位 · 联系人', 'biz_party_contact', 'party_id', 'biz_party', 'id', True),
    ('仓库 · 库存', 'erp_stock', 'warehouse_id', 'erp_warehouse', 'id', True),
    ('仓库 · 货位', 'wms_location', 'warehouse_id', 'erp_warehouse', 'id', True),
    ('仓库 · 销售明细', 'erp_sale_order_item', 'warehouse_id', 'erp_warehouse', 'id', False),
    # 交叉验证：ID 是否其实指向 WMS 那一套仓库表（双源问题）
    ('仓库 · 销售明细→wms', 'erp_sale_order_item', 'warehouse_id', 'wms_warehouse', 'id', False),
    ('仓库 · 库存→wms', 'erp_stock', 'warehouse_id', 'wms_warehouse', 'id', True),
    ('商品 · 货位绑定', 'erp_product_location', 'product_id', 'erp_product', 'id', True),
    ('商品 · 货位绑定-仓库', 'erp_product_location', 'warehouse_id', 'erp_warehouse', 'id', True),
]


def main():
    import psycopg2
    out = []
    with psycopg2.connect(CONN) as conn:
        with conn.cursor() as cur:
            for label, child, ccol, master, mcol, softdel in CHECKS:
                where = " WHERE c.deleted = 0" if softdel else ""
                sql = ("SELECT count(*) AS total, count(m.{m}) AS hit FROM {c} c "
                       "LEFT JOIN {mm} m ON m.{m}=c.{cc}{w}").format(
                    m=mcol, c=child, mm=master, cc=ccol, w=where)
                try:
                    cur.execute(sql)
                    total, hit = cur.fetchone()
                except Exception as e:
                    conn.rollback()
                    out.append({'label': label, 'error': str(e)[:120]})
                    print('  %-28s ERROR %s' % (label, str(e)[:80]))
                    continue
                rate = (hit / total * 100) if total else 0
                out.append({'label': label, 'child': child, 'child_col': ccol,
                            'master': master, 'total': total, 'hit': hit, 'rate': round(rate, 1)})
                flag = 'OK' if total == 0 or hit == total else ('!!' if hit == 0 else '! ')
                print('  [%s] %-28s %-24s → %-16s %5s/%-5s (%5.1f%%)' % (
                    flag, label, '%s.%s' % (child, ccol), master, hit, total, rate))
    os.makedirs(os.path.join(ROOT, 'tool-results/md-audit'), exist_ok=True)
    json.dump(out, open(os.path.join(ROOT, 'tool-results/md-audit/refs-audit.json'), 'w',
                        encoding='utf-8'), ensure_ascii=False, indent=1)
    print()
    print('说明: [OK]=全命中或子表为空, [! ]=部分孤儿引用, [!!]=全部孤儿引用')
    print('输出: tool-results/md-audit/refs-audit.json')


if __name__ == '__main__':
    main()
