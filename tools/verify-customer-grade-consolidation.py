# -*- coding: utf-8 -*-
"""
客户级别收敛验证（2026-09-26）—— 真机跑通「子标签建的级别能出现在下拉里」

验证点：
  1. 旧端点 /api/erp/customer/level/* 已 404（表与控制器已下线）
  2. 新端点 /api/erp/partner/grades/page 可用（本次新增）
  3. 走子标签口径创建级别 → 出现在「下拉口径」的 GET /erp/partner/grades 里（收敛前这两处是两套数据源）
  4. 编码唯一校验生效
  5. 清理自造数据

用法: python tools/verify-customer-grade-consolidation.py
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
MARK = 'E2E收敛验证级'


def call(path, method='GET', token=None, body=None, limit=4000):
    url = API + path
    data = json.dumps(body).encode('utf-8') if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', 'Bearer %s' % token)
        req.add_header('X-Tenant-Id', str(TENANT_ID))
        req.add_header('tenantId', str(TENANT_ID))
    try:
        with urllib.request.urlopen(req, timeout=25) as r:
            return r.status, r.read().decode('utf-8', 'replace')[:limit]
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', 'replace')[:limit]
    except Exception as e:
        return -1, str(e)[:200]


def login(username='e2e_product', password='admin123', tenant='系统租户'):
    st, raw = call('/api/auth/captcha', limit=5_000_000)
    cap = json.loads(raw)
    img = cap['data']['img']
    svg = base64.b64decode(img.split(',', 1)[1]).decode('utf-8', 'replace')
    code = ''.join(re.findall(r'<text[^>]*>([^<]+)</text>', svg))
    st, raw = call('/api/auth/login', 'POST', limit=20000, body={
        'username': username, 'password': password, 'tenantName': tenant,
        'captcha': code, 'captchaKey': cap['data']['uuid']})
    return (json.loads(raw).get('data') or {}).get('token')


results = []


def check(name, ok, detail=''):
    results.append((name, ok, detail))
    print('  [%s] %-52s %s' % ('PASS' if ok else 'FAIL', name, detail))


def main():
    token = login()
    if not token:
        print('登录失败')
        return
    print('登录成功\n')

    # 1. 旧端点应 404
    st, raw = call('/api/erp/customer/level/page?pageNum=1&pageSize=5', token=token)
    check('旧端点 /erp/customer/level/page 已下线', st == 404, 'HTTP %s' % st)

    st, raw = call('/api/erp/customer/level/list', token=token)
    check('旧端点 /erp/customer/level/list 已下线', st == 404, 'HTTP %s' % st)

    # 2. 新分页端点
    st, raw = call('/api/erp/partner/grades/page?pageNum=1&pageSize=5', token=token)
    check('/erp/partner/grades/page 可用', st == 200, 'HTTP %s' % st)
    page_ok = st == 200

    # 3. 创建级别（子标签口径：gradeName/gradeCode/discountRate/status）
    st, raw = call('/api/erp/partner/grades', 'POST', token,
                   {'gradeName': MARK, 'gradeCode': 'E2E_GRADE_VERIFY', 'discountRate': 95, 'status': 1})
    created = st == 200 and '"data":true' in raw.replace(' ', '')
    check('子标签口径可创建级别', created, raw[:120])

    # 4. 下拉口径能读到（收敛的核心断言）
    st, raw = call('/api/erp/partner/grades', token=token)
    in_dropdown = MARK in raw
    check('新建级别出现在下拉口径里（收敛前读不到）', in_dropdown, 'HTTP %s' % st)

    # 5. 分页关键字也能查到
    if page_ok:
        st, raw = call('/api/erp/partner/grades/page?pageNum=1&pageSize=20&keyword=E2E', token=token)
        check('分页端点关键字查询命中', st == 200 and MARK in raw, 'HTTP %s' % st)

    # 6. 编码唯一
    st, raw = call('/api/erp/partner/grades', 'POST', token,
                   {'gradeName': MARK + '2', 'gradeCode': 'E2E_GRADE_VERIFY', 'discountRate': 95, 'status': 1})
    check('重复编码被拒绝', '已存在' in raw, raw[:140])

    # 7. 清理自造数据
    # 注意：本仓为规避 JS 大整数精度问题，后端把 id 序列化成**字符串**（"id":"1"），
    # 故正则必须带引号，否则抓不到 id（首版就是这么漏清理的）。
    st, raw = call('/api/erp/partner/grades/page?pageNum=1&pageSize=50&keyword=E2E_GRADE_VERIFY', token=token)
    ids = re.findall(r'"id":"(\d+)"', raw)
    cleaned = 0
    for gid in set(ids):
        st, raw = call('/api/erp/partner/grades/%s' % gid, 'DELETE', token)
        if st == 200 and '"data":true' in raw.replace(' ', ''):
            cleaned += 1
    check('清理自造级别', cleaned >= 1, '删除 %d 条' % cleaned)

    st, raw = call('/api/erp/partner/grades', token=token)
    check('清理后下拉不再含验证级', MARK not in raw, '')

    print()
    passed = sum(1 for _, ok, _ in results if ok)
    print('== %d/%d 通过 ==' % (passed, len(results)))
    return passed == len(results)


if __name__ == '__main__':
    sys.exit(0 if main() else 1)
