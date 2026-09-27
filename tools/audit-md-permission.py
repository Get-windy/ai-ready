#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
资料模块权限审计（只读）：
  1. 以「被资料页面真实调用的 216 个端点」为种子，反查其所在 controller
  2. 统计这些 controller 的方法鉴权注解覆盖率（@SaCheckPermission / @RequirePermission /
     @SaCheckRole / @SaCheckLogin / @SaIgnore / @PreAuthorize / 自定义）
  3. 抽取用到的权限码，与 sys_permission / sys_role_permission 对账：
     哪些码库里没有（补注解前必须先补种子）、哪些码只授给超管
输出: tool-results/md-audit/permission-audit.json
用法: 先跑 tools/audit-md-contract.py 与 tools/audit-md-api-usage.py，再跑本脚本
"""
import os
import re
import io
import sys
import json
import collections

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BE_ROOT = os.path.join(ROOT, 'backend')
OUT_DIR = os.path.join(ROOT, 'tool-results/md-audit')
USAGE_JSON = os.path.join(OUT_DIR, 'api-usage.json')
CONN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')

ANN = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\s*(?:\(\s*(?:value\s*=\s*)?(?:["\']([^"\']*)["\'])?|$)')
CLASS_MAP = re.compile(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']')
AUTH_ANN = re.compile(r'@(SaCheckPermission|RequiresPermission|RequirePermission|SaCheckRole|SaCheckLogin|'
                      r'SaIgnore|PreAuthorize|SaCheckSafe|SaCheckDisable)\b')
PERM_CODE = re.compile(r'@(?:SaCheckPermission|RequiresPermission|RequirePermission)\s*\(\s*(?:value\s*=\s*)?'
                       r'\{?\s*["\']([^"\']+)["\']')


def norm(p):
    p = p.split('?')[0].strip()
    p = re.sub(r'^/api(?=/|$)', '', p)
    p = re.sub(r'\$\{[^}]*\}', '{}', p)
    p = re.sub(r'\{[^}]*\}', '{}', p)
    p = re.sub(r'/+', '/', p)
    if not p.startswith('/'):
        p = '/' + p
    return p.rstrip('/').lower() or '/'


def load_seed_paths():
    d = json.load(open(USAGE_JSON, encoding='utf-8'))
    seed = {r['norm'] for r in d['rows'] if r.get('usage_md')}
    return seed


def scan_controllers():
    """返回 [{file, cls, prefix, methods:[{line, http, path, auths, perms, name}]}]"""
    out = []
    for dirpath, _, files in os.walk(BE_ROOT):
        if os.sep + 'target' + os.sep in dirpath + os.sep:
            continue
        for f in files:
            if not f.endswith('.java'):
                continue
            fp = os.path.join(dirpath, f)
            src = open(fp, encoding='utf-8', errors='ignore').read()
            if '@RestController' not in src and '@Controller' not in src:
                continue
            lines = src.split('\n')
            prefix = ''
            for ln in lines:
                m = CLASS_MAP.search(ln)
                if m and '@RequestMapping' in ln:
                    prefix = m.group(1)
                    break
            cls = ''
            for ln in lines:
                m = re.search(r'(?:public\s+)?class\s+(\w+)', ln)
                if m:
                    cls = m.group(1)
                    break
            methods = []
            for i, ln in enumerate(lines):
                m = ANN.search(ln)
                if not m:
                    continue
                sub = m.group(2) or ''
                full = (prefix.rstrip('/') + '/' + sub.lstrip('/')) if prefix else sub
                # 注解在方法上方若干行内（含方法签名的后续行不适合，取紧邻 12 行）
                win = '\n'.join(lines[max(0, i - 12):i + 6])
                auths = sorted(set(AUTH_ANN.findall(win)))
                perms = sorted(set(PERM_CODE.findall(win)))
                methods.append({'line': i + 1, 'http': m.group(1).upper(), 'path': full,
                                'norm': norm(full), 'auths': auths, 'perms': perms})
            out.append({'file': os.path.relpath(fp, ROOT).replace('\\', '/'),
                        'cls': cls, 'prefix': prefix, 'methods': methods})
    return out


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    seed = load_seed_paths()
    ctrls = scan_controllers()

    relevant = []
    for c in ctrls:
        hit = [m for m in c['methods'] if m['norm'] in seed]
        if hit:
            relevant.append({**c, 'hit': hit})

    total_m = sum(len(c['hit']) for c in relevant)
    noauth = [m for c in relevant for m in c['hit'] if not m['auths']]
    perms_used = sorted({p for c in relevant for m in c['hit'] for p in m['perms']})

    print('资料页面调用的端点落在 %d 个 controller 里，命中方法 %d 个' % (len(relevant), total_m))
    print('其中无任何鉴权注解: %d 个' % len(noauth))
    print()
    print('=== 无鉴权注解的端点 ===')
    for m in noauth:
        print('  %-60s %s  (%s)' % (m['norm'], m['http'], m['line']))
    print()
    print('=== 资料模块 controller 覆盖（命中方法 / 该类总方法 / 无注解数）===')
    for c in sorted(relevant, key=lambda x: -len(x['hit'])):
        na = sum(1 for m in c['methods'] if not m['auths'])
        print('  %-46s hit=%-4d total=%-4d noauth=%-4d prefix=%s' % (
            c['cls'], len(c['hit']), len(c['methods']), na, c['prefix']))

    # DB 对账
    import psycopg2
    with psycopg2.connect(**CONN) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT permission_code FROM sys_permission WHERE deleted=0")
            db_codes = {r[0] for r in cur.fetchall()}
            cur.execute("""SELECT p.permission_code, r.role_code, r.tenant_id
                           FROM sys_role_permission rp
                           JOIN sys_permission p ON p.id = rp.permission_id
                           JOIN sys_role r ON r.id = rp.role_id
                           WHERE p.deleted=0""")
            grant = collections.defaultdict(set)
            for code, role, tid in cur.fetchall():
                grant[code].add('%s(t%s)' % (role, tid))

    missing = [p for p in perms_used if p not in db_codes]
    super_only, partial, allok = [], [], []
    for p in perms_used:
        holders = grant.get(p, set())
        if not holders:
            super_only.append(p)
        elif all(h.startswith('SUPER_ADMIN') for h in holders):
            super_only.append(p)
        elif len(holders) == 1 and any(h.startswith('SUPER_ADMIN') for h in holders):
            super_only.append(p)
        else:
            partial.append(p)

    print()
    print('=== 代码里用到的权限码 %d 个 ===' % len(perms_used))
    print('  库里不存在（补种子前不能上线）: %d' % len(missing))
    for p in missing:
        print('     ✗ %s' % p)
    print('  仅超管持有 / 无任何角色持有: %d' % len(super_only))
    for p in super_only:
        print('     ! %-42s holders=%s' % (p, sorted(grant.get(p, set())) or '无'))
    print('  有非超管角色持有: %d' % len(partial))
    for p in partial:
        print('     ok %-42s holders=%s' % (p, sorted(grant.get(p, set()))))

    json.dump({'relevant': [{'cls': c['cls'], 'file': c['file'], 'prefix': c['prefix'],
                             'hit': c['hit'], 'methods': len(c['methods']),
                             'noauth': sum(1 for m in c['methods'] if not m['auths'])} for c in relevant],
               'noauth_endpoints': noauth, 'perms_used': perms_used,
               'perms_missing_in_db': missing, 'perms_super_only': super_only,
               'perms_partial': partial},
              open(os.path.join(OUT_DIR, 'permission-audit.json'), 'w', encoding='utf-8'),
              ensure_ascii=False, indent=1)
    print()
    print('输出: tool-results/md-audit/permission-audit.json')


if __name__ == '__main__':
    main()
