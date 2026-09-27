# -*- coding: utf-8 -*-
"""
D6 验收：成本调价单记账 → 回写 ERP 轨库存成本（erp_stock.unit_price）。

背景（修复前）：`StockCostAdjustServiceImpl.execute` 只更新单据/明细的状态与金额，
库存成本单价原封不动 ⇒ 调价后出库成本、毛利仍按旧价计算。

验证：
  1. 造一张已审核（status=2）的成本调价单 + 1 行明细（调后成本 12.34）
  2. 调 POST /api/erp/stock/cost-adjust/{id}/execute
  3. 断言 erp_stock.unit_price 变为 12.34
  4. 清理（删单据/明细、unit_price 回退原值）

用法: python tools/verify-cost-adjust-backfill.py
"""
import io
import re
import sys
import json
import base64
import urllib.request

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

BASE = 'http://127.0.0.1:5655'
USER, PWD, TENANT = 'admin', 'admin123', '系统租户'
NO = 'E2E-CBTJD-VERIFY'
NEW_COST = 12.34
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')

results = []


def check(name, ok, detail=''):
    results.append(ok)
    print('%s  %s    %s' % ('  PASS' if ok else '  FAIL', name, detail))


def http(method, path, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
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
        raise RuntimeError('登录失败: %s' % json.dumps(res, ensure_ascii=False)[:250])
    return token


def main():
    import psycopg2
    import psycopg2.extras
    conn = psycopg2.connect(**CONN)
    conn.autocommit = True
    cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    adjust_id = None
    stock_id = None
    price_before = None

    print('\n== D6 成本回写验收 @ %s ==\n' % BASE)
    token = login()
    print('[1] 登录成功\n')

    try:
        cur.execute("""SELECT id, product_id, warehouse_id, unit_price, quantity
                       FROM erp_stock WHERE deleted=0 AND quantity > 0
                       ORDER BY id LIMIT 1""")
        st = cur.fetchone()
        if not st:
            check('准备数据：找到有库存的 erp_stock 行', False, '无可用数据')
            return
        stock_id, product_id, wh_id = st['id'], st['product_id'], st['warehouse_id']
        price_before = st['unit_price']
        print('[2] 目标库存行: id=%s product=%s warehouse=%s 原成本=%s 数量=%s\n'
              % (stock_id, product_id, wh_id, price_before, st['quantity']))

        cur.execute("""INSERT INTO erp_stock_cost_adjust
                         (tenant_id, adjust_no, warehouse_id, status, total_adjust_amount)
                       VALUES (1, %s, %s, 2, 0) RETURNING id""", (NO, wh_id))
        adjust_id = cur.fetchone()['id']
        # ⚠️ 该明细表的 id 列 NOT NULL 且**无自增默认值**（主表有 nextval，明细没有），
        # 必须显式给主键，否则 NotNullViolation。
        cur.execute("SELECT COALESCE(max(id), 0) + 1 AS nid FROM erp_stock_cost_adjust_item")
        item_id = cur.fetchone()['nid']
        cur.execute("""INSERT INTO erp_stock_cost_adjust_item
                         (id, tenant_id, adjust_id, product_id, warehouse_id, current_quantity,
                          old_cost, new_cost, old_amount, new_amount, diff_amount)
                       VALUES (%s, 1, %s, %s, %s, %s, %s, %s, 0, 0, 0)""",
                    (item_id, adjust_id, product_id, wh_id, st['quantity'], price_before or 0, NEW_COST))
        print('[3] 已造成本调价单 id=%s（状态=2 已审核，调后成本 %s）\n' % (adjust_id, NEW_COST))

        code, res = http('POST', '/api/erp/stock/cost-adjust/%s/execute' % adjust_id, None, token)
        check('① 记账执行成功', code == 200, 'HTTP %s %s' % (code, str(res)[:140]))

        cur.execute("SELECT unit_price FROM erp_stock WHERE id=%s", (stock_id,))
        after = cur.fetchone()['unit_price']
        check('② erp_stock.unit_price 已回写', after is not None and abs(float(after) - NEW_COST) < 0.001,
              '%s → %s' % (price_before, after))

    finally:
        print('\n[4] 清理自造数据')
        try:
            if adjust_id is not None:
                cur.execute("DELETE FROM erp_stock_cost_adjust_item WHERE adjust_id=%s", (adjust_id,))
                cur.execute("DELETE FROM erp_stock_cost_adjust WHERE id=%s", (adjust_id,))
            if stock_id is not None:
                cur.execute("UPDATE erp_stock SET unit_price=%s WHERE id=%s", (price_before, stock_id))
            cur.execute("DELETE FROM erp_stock_cost_adjust WHERE adjust_no=%s", (NO,))
            print('  已清理（单据/明细/unit_price 回退）')
        except Exception as e:
            print('  清理警告:', str(e)[:200])

    failed = sum(1 for x in results if not x)
    print('\n== 合计 %d 项，通过 %d，失败 %d ==\n' % (len(results), len(results) - failed, failed))


if __name__ == '__main__':
    main()
