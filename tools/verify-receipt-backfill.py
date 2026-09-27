# -*- coding: utf-8 -*-
"""
D1 验收：收货确认 → 采购订单已收数量回写（进程内事件链路）。

背景（修复前）：`ReceiptServiceImpl` 用裸 HttpClient 调
`POST /api/erp/purchase/order/received`，内部调用不带 Authorization
⇒ 被 SaInterceptor 拦成 401 ⇒ 抛 WmsBusinessException ⇒ `confirmReceipt`
的 @Transactional 整体回滚 ⇒ **关联采购订单的收货单永远无法确认完成**。

本脚本端到端验证修复后的行为：
  1. 造一张关联采购订单的收货任务（+ 1 行明细）
  2. 调 /start → /confirm
  3. 断言：confirm 成功（修复前必然 500）
  4. 断言：该采购订单明细的 received_quantity 增加（回写生效）
  5. 无论成败都清理自造数据（含回退已收数量）

用法: python tools/verify-receipt-backfill.py
"""
import io
import re
import sys
import json
import base64
import urllib.request
import urllib.parse

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

BASE = 'http://127.0.0.1:%s' % (sys.argv[1] if len(sys.argv) > 1 else '5655')
USER, PWD, TENANT = 'admin', 'admin123', '系统租户'
TASK_NO = 'E2E-D1-VERIFY'

CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')

results = []


def check(name, ok, detail=''):
    results.append(ok)
    print('%s  %s    %s' % ('  PASS' if ok else '  FAIL', name, detail))


def http(method, path, body=None, token=None):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            txt = r.read().decode('utf-8', 'replace')
            return r.status, (json.loads(txt) if txt.strip().startswith(('{', '[')) else txt)
    except urllib.error.HTTPError as e:
        txt = e.read().decode('utf-8', 'replace')
        try:
            return e.code, json.loads(txt)
        except Exception:
            return e.code, txt


def login():
    _, cap = http('GET', '/api/auth/captcha')
    svg = base64.b64decode(cap['data']['img'].split(',')[1]).decode('utf-8', 'replace')
    code = ''.join(re.findall(r'<text[^>]*>([^<]+)</text>', svg))
    _, res = http('POST', '/api/auth/login', {
        'username': USER, 'password': PWD, 'tenantName': TENANT,
        'captcha': code, 'captchaKey': cap['data']['uuid'],
    })
    token = (res.get('data') or {}).get('token') or (res.get('data') or {}).get('accessToken')
    if not token:
        raise RuntimeError('登录失败: %s' % json.dumps(res, ensure_ascii=False)[:300])
    return token


def main():
    import psycopg2
    import psycopg2.extras
    conn = psycopg2.connect(**CONN)
    conn.autocommit = True
    cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    task_id = None
    po_item_id = None
    before_qty = None
    inv_before = {}

    print('\n== D1 收货回写验收 @ %s ==\n' % BASE)
    token = login()
    print('[1] 登录成功\n')

    try:
        # 选一张有明细的采购订单
        cur.execute("""SELECT i.id, i.order_id, i.product_id, i.received_quantity
                       FROM erp_purchase_order_item i
                       JOIN erp_purchase_order o ON o.id = i.order_id
                       WHERE o.deleted = 0 AND i.product_id IS NOT NULL
                       ORDER BY i.id LIMIT 1""")
        po = cur.fetchone()
        if not po:
            check('准备数据：找到可用采购订单明细', False, '无可用数据，跳过')
            return
        po_item_id, po_order_id, product_id = po['id'], po['order_id'], po['product_id']
        before_qty = po['received_quantity'] or 0
        print('[2] 目标采购订单明细: itemId=%s orderId=%s productId=%s 已收=%s\n'
              % (po_item_id, po_order_id, product_id, before_qty))

        # 记录库存前值（明细 + 合计，合计用于断言增量）
        cur.execute("SELECT id, quantity FROM wms_inventory WHERE product_id=%s AND warehouse_id=1", (product_id,))
        for r in cur.fetchall():
            inv_before[r['id']] = r['quantity']
        inv_qty_before = sum(float(v or 0) for v in inv_before.values())

        # 造收货任务 + 明细
        cur.execute("""INSERT INTO wms_receipt_task
                         (tenant_id, task_no, source_type, source_order_id, source_order_no,
                          warehouse_id, warehouse_name, status, total_items, total_quantity, received_quantity)
                       VALUES (1, %s, 0, %s, 'PO-TEST-001', 1, '主仓库', 0, 1, 10, 0)
                       RETURNING id""", (TASK_NO, po_order_id))
        task_id = cur.fetchone()['id']
        cur.execute("""INSERT INTO wms_receipt_detail
                         (tenant_id, task_id, line_no, product_id, product_code, product_name,
                          expected_quantity, received_quantity, status)
                       VALUES (1, %s, 1, %s, 'E2E-P', 'E2E验收商品', 10, 0, 0)""", (task_id, product_id))
        print('[3] 已造收货任务 id=%s（单号 %s），明细 1 行\n' % (task_id, TASK_NO))

        # start -> confirm
        st, r1 = http('POST', '/api/wms/receipt/start?taskId=%s&userId=1&userName=admin' % task_id, None, token)
        check('① 开始收货成功', st == 200, 'HTTP %s %s' % (st, str(r1)[:120]))

        cur.execute("UPDATE wms_receipt_detail SET received_quantity=10 WHERE task_id=%s", (task_id,))
        st2, r2 = http('POST', '/api/wms/receipt/confirm?taskId=%s&userId=1&userName=admin' % task_id, None, token)
        check('② 确认收货成功（修复前必然 500 回滚）', st2 == 200, 'HTTP %s %s' % (st2, str(r2)[:160]))

        # 断言回写
        cur.execute("SELECT received_quantity FROM erp_purchase_order_item WHERE id=%s", (po_item_id,))
        after_qty = cur.fetchone()['received_quantity'] or 0
        check('③ 采购订单「已收数量」已回写', float(after_qty) > float(before_qty),
              '%s → %s（本次实收 10）' % (before_qty, after_qty))

        # 断言库存已过账
        cur.execute("SELECT COALESCE(SUM(quantity),0) AS q FROM wms_inventory WHERE product_id=%s AND warehouse_id=1", (product_id,))
        qty_after = float(cur.fetchone()['q'])
        check('④ WMS 库存已过账（本次 +10）', qty_after - inv_qty_before >= 10,
              '%s → %s' % (inv_qty_before, qty_after))

    finally:
        print('\n[4] 清理自造数据')
        try:
            if po_item_id is not None and before_qty is not None:
                cur.execute("UPDATE erp_purchase_order_item SET received_quantity=%s WHERE id=%s", (before_qty, po_item_id))
            if task_id is not None:
                cur.execute("DELETE FROM wms_receipt_detail WHERE task_id=%s", (task_id,))
                cur.execute("DELETE FROM wms_receipt_task WHERE id=%s", (task_id,))
            # 回退库存（本次 increase 的量）
            cur.execute("""DELETE FROM wms_inventory WHERE product_id=%s AND warehouse_id=1 AND id <> ALL(%s)""",
                        (product_id, list(inv_before.keys()) or [0]))
            for iid, q in inv_before.items():
                cur.execute("UPDATE wms_inventory SET quantity=%s, available_quantity=%s WHERE id=%s", (q, q, iid))
            cur.execute("DELETE FROM wms_receipt_task WHERE task_no=%s", (TASK_NO,))
            print('  已清理（收货单/明细/库存回退/PO 已收数回退）')
        except Exception as e:
            print('  清理警告:', str(e)[:200])

    failed = sum(1 for x in results if not x)
    print('\n== 合计 %d 项，通过 %d，失败 %d ==\n' % (len(results), len(results) - failed, failed))


if __name__ == '__main__':
    main()
