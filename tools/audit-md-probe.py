# -*- coding: utf-8 -*-
"""
资料模块真机验证（只读，不写业务数据）：
  1. 用**非超管**账号登录（默认 e2e_hr_ta = 租户1 SYSTEM_ADMIN），逐个调用资料模块
     24 个页面依赖的代表性接口，记录 HTTP 状态码 —— 区分「真能用」与「403」。
  2. 用**租户2 非超管**账号（e2e_hr_t2）拉菜单，核对 menu_level=3 的菜单是否下发。
  3. 复核两个已判定断链的端点（客户建档用的角色/银行账户）确为 404。
用法: python tools/audit-md-probe.py
"""
import io
import sys
import json
import base64
import re
import urllib.request

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

API = 'http://127.0.0.1:5655'
TENANT_ID = 1


def call(path, method='GET', token=None, body=None, timeout=25, limit=400):
    url = API + path
    data = json.dumps(body).encode('utf-8') if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header('Content-Type', 'application/json')
    if token:
        # 与前端 utils/request.ts:121-133 保持一致
        req.add_header('Authorization', 'Bearer %s' % token)
        req.add_header('X-Tenant-Id', str(TENANT_ID))
        req.add_header('tenantId', str(TENANT_ID))
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            raw = r.read().decode('utf-8', 'replace')
            return r.status, raw[:limit]
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', 'replace')[:limit]
    except Exception as e:
        return -1, str(e)[:200]


def login(username, password='admin123', tenant='系统租户'):
    st, raw = call('/api/auth/captcha', limit=5_000_000)
    if st != 200:
        return None, 'captcha HTTP %s %s' % (st, raw)
    cap = json.loads(raw) if raw.strip().startswith('{') else {}
    uuid = cap.get('data', {}).get('uuid')
    img = cap.get('data', {}).get('img', '')
    code = ''
    if ',' in img:
        svg = base64.b64decode(img.split(',', 1)[1]).decode('utf-8', 'replace')
        code = ''.join(re.findall(r'<text[^>]*>([^<]+)</text>', svg))
    st, raw = call('/api/auth/login', 'POST', limit=20000, body={
        'username': username, 'password': password, 'tenantName': tenant,
        'captcha': code, 'captchaKey': uuid})
    try:
        obj = json.loads(raw)
    except Exception:
        return None, 'login 非 JSON: %s' % raw
    token = (obj.get('data') or {}).get('token')
    if not token:
        return None, 'login 失败: %s' % raw[:200]
    return token, None


# 资料模块 24 个页面各取 1~2 个代表性只读接口
PROBES = [
    ('70501 商品', 'GET', '/api/erp/product/page?pageNum=1&pageSize=5'),
    ('70502 商品条码', 'GET', '/api/erp/product/barcodes/page?pageNum=1&pageSize=5'),
    ('70503 商品价格管理', 'GET', '/api/erp/md/product-price/level-price/page?pageNum=1&pageSize=5'),
    ('70504 商品辅助资料', 'GET', '/api/erp/product-unit-group/page?pageNum=1&pageSize=5'),
    ('70505 图片管理', 'GET', '/api/erp/md/image/page?pageNum=1&pageSize=5'),
    ('70506 价格审批', 'GET', '/api/erp/pricing/approval/pending'),
    ('81008 价格等级', 'GET', '/api/erp/product-grade/list'),
    ('81009 库存管理模式', 'GET', '/api/erp/inventory-mode'),
    ('80510 客户', 'GET', '/api/erp/md/customer/page?pageNum=1&pageSize=5'),
    ('80511 供应商', 'GET', '/api/erp/md/customer/page?pageNum=1&pageSize=5&partyType=2'),
    ('80512 物流公司', 'GET', '/api/erp/partner/categories/tree'),
    ('80513 其他往来单位', 'GET', '/api/erp/md/customer/page?pageNum=1&pageSize=5&partyType=4'),
    ('70513 互联账号', 'GET', '/api/erp/md/linked-account/page?pageNum=1&pageSize=5'),
    ('80520 仓库规划', 'GET', '/api/erp/warehouse/page?pageNum=1&pageSize=5'),
    ('70520 商品货位设置', 'GET', '/api/erp/product-location/page?pageNum=1&pageSize=5'),
    ('81014 批次管理', 'GET', '/api/erp/batch-sn/batches/page?pageNum=1&pageSize=5'),
    ('70530 线路', 'GET', '/api/erp/md/route/page?pageNum=1&pageSize=5'),
    ('80550 支付方式', 'GET', '/api/erp/md/payment-method/page?pageNum=1&pageSize=5'),
    ('80551 支付渠道', 'GET', '/api/erp/md/payment-channel/page?pageNum=1&pageSize=5'),
    ('80552 支付账户', 'GET', '/api/erp/finance/account/page?pageNum=1&pageSize=5'),
    ('70540 银行账户', 'GET', '/api/erp/finance/bank-account/page?pageNum=1&pageSize=5'),
    ('70541 费用类型', 'GET', '/api/erp/md/expense-type/list'),
    ('70542 其他收入', 'GET', '/api/erp/md/other-income/list'),
    ('70543 会计科目', 'GET', '/api/erp/finance/subject/tree'),
]

BROKEN = [
    ('客户建档 · 绑角色（断链验证）', 'POST', '/api/erp/partner/roles/1/add', {'roleType': 'CUSTOMER'}),
    ('客户建档 · 银行账户（断链验证）', 'POST', '/api/erp/partner/bank-accounts',
     {'partnerId': 1, 'accountName': 'x', 'bankName': 'y', 'accountNo': '1'}),
    ('批次管理 · 删除（断链验证）', 'DELETE', '/api/erp/batch-sn/batches/999999', None),
]


def probe_all(tag, token):
    ok = fail = other = 0
    print('=' * 104)
    print('① %s' % tag)
    print('=' * 104)
    for name, method, path in PROBES:
        st, raw = call(path, method, token)
        mark = 'OK ' if st == 200 else ('403' if st == 403 else ('404' if st == 404 else str(st)))
        body = ''
        try:
            obj = json.loads(raw)
            body = 'code=%s' % obj.get('code')
            if obj.get('code') not in (0, 200, None):
                body += ' msg=%s' % str(obj.get('msg') or obj.get('message'))[:60]
        except Exception:
            body = raw[:60]
        print('  [%-4s] %-22s %-58s %s' % (mark, name, path.split('?')[0], body))
        if st == 200:
            ok += 1
        elif st == 403:
            fail += 1
        else:
            other += 1
    print('  --- 200: %d / 403: %d / 其它: %d ---' % (ok, fail, other))
    return ok, fail, other


def main():
    token, err = login('e2e_hr_ta')
    if not token:
        print('非超管登录失败：%s' % err)
    else:
        probe_all('① 非超管账号 e2e_hr_ta（租户1 / SYSTEM_ADMIN）调资料模块 24 页接口', token)
        print()
        print('=' * 104)
        print('② 断链端点在真机上的实际响应')
        print('=' * 104)
        for name, method, path, body in BROKEN:
            st, raw = call(path, method, token, body)
            print('  [%s] %-36s %-52s %s' % (st, name, path, raw[:90]))

    print()
    sup, err_s = login('e2e_product')
    if not sup:
        print('超管登录失败：%s' % err_s)
    else:
        probe_all('①b 对照：超管账号 e2e_product 调同一批接口', sup)

    print()
    print('=' * 104)
    print('③ 租户2 非超管（e2e_hr_t2）菜单下发核对 menu_level=3')
    print('=' * 104)
    t2, err2 = login('e2e_hr_t2', tenant='E2E验收租户2')
    if not t2:
        print('  登录失败：%s' % err2)
    else:
        for path in ('/api/menu/user/mega/tenant-admin',
                     '/api/menu/user/mega/tenant-admin?tenantId=2'):
            st, raw = call(path, 'GET', t2, limit=2000000)
            if st != 200:
                print('  [%s] %s' % (st, path))
                continue
            ids = set(re.findall(r'"id":\s*(\d+)', raw))
            names = re.findall(r'"menuName":\s*"([^"]+)"', raw)
            want = {'80520': '仓库规划', '80510': '客户', '80511': '供应商',
                    '80512': '物流公司', '80513': '其他往来单位'}
            print('  %s → 叶子名 %d 个' % (path, len(names)))
            for mid, nm in want.items():
                print('     menu_level=3 的 %s(%s): %s' % (mid, nm, '已下发' if mid in ids else '未下发'))
            print('     下发菜单名样本: %s' % ','.join(names[:12]))


if __name__ == '__main__':
    main()
