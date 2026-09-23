# -*- coding: utf-8 -*-
"""
交易模块（商城 / 支付结算 / 外部平台）专项审计脚本 —— 只读，不改任何文件。

覆盖后端三处：
  · backend/erp/erp-mall                        （商城管理端 + 商城前台端）
  · backend/core/base/core-base/.../trade       （外部平台：渠道/外部订单/库存同步）
  · backend/core/payment/core-payment           （支付请求/记录/退款/每日对账）

审计四件事：
  ① 前端调用 → 后端是否存在（断链检查）
  ② 后端端点 → 前端是否调用（冗余候选；导出/回调/定时任务不算）
  ③ 端点权限注解覆盖（@SaCheckPermission / @SaCheckLogin / @SaIgnore ...）
  ④ 权限码：注解里出现但库里没有的（会造成非超管 403）

用法:
  python tools/audit-trade-module.py [--links|--unused|--perms|--codes|--all]
"""
import os
import re
import io
import sys
import json

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND_DIRS = [
    os.path.join(ROOT, 'backend', 'erp', 'erp-mall', 'src', 'main', 'java'),
    os.path.join(ROOT, 'backend', 'core', 'base', 'core-base', 'src', 'main', 'java', 'cn', 'aiedge', 'trade'),
    os.path.join(ROOT, 'backend', 'core', 'payment', 'core-payment', 'src', 'main', 'java'),
]
FRONTEND_SRC = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src')
FRONT_DIRS = [
    os.path.join(FRONTEND_SRC, 'views', 'trade'),
    os.path.join(FRONTEND_SRC, 'views', 'mall'),
    os.path.join(FRONTEND_SRC, 'views', 'payment'),
]
FRONT_FILES = [
    os.path.join(FRONTEND_SRC, 'api', 'erp', 'mall.ts'),
    os.path.join(FRONTEND_SRC, 'api', 'trade', 'index.ts'),
    os.path.join(FRONTEND_SRC, 'api', 'payment', 'index.ts'),
]

# C 端商城（mobile-mall）用 axios baseURL='/api/v1/mall'，路径字面量是**相对**的，
# 不加前缀会被误判成「后端无人调用」。这里显式登记 baseURL 前缀。
MOBILE_MALL_SRC = os.path.join(ROOT, 'frontend', 'apps', 'mobile-mall', 'src')
MOBILE_MALL_PREFIX = '/api/v1/mall'

VERB_RE = re.compile(
    r'@(Get|Post|Put|Delete|Patch)Mapping\s*(?:\(\s*(?:value\s*=\s*)?(["\'])([^"\']*)\2\s*\))?')
BASE_RE = re.compile(r'@RequestMapping\(\s*(?:value\s*=\s*)?["\']([^"\']+)["\']')
PERM_RE = re.compile(r'@(SaCheckPermission|PreAuthorize|SaCheckRole|SaCheckLogin|SaIgnore)\b')
CODE_RE = re.compile(r'@SaCheckPermission\(\s*(?:value\s*=\s*)?\{?\s*["\']([^"\']+)["\']')


def collect_backend_endpoints():
    eps = []
    for base_dir in BACKEND_DIRS:
        if not os.path.isdir(base_dir):
            print(f'[WARN] 后端目录不存在: {base_dir}', file=sys.stderr)
            continue
        for dirpath, _dirnames, filenames in os.walk(base_dir):
            for fn in filenames:
                if not fn.endswith('.java'):
                    continue
                full = os.path.join(dirpath, fn)
                src = open(full, encoding='utf-8', errors='replace').read()
                base_m = BASE_RE.search(src)
                base = base_m.group(1) if base_m else ''
                rel = os.path.relpath(full, ROOT).replace('\\', '/')
                for m in VERB_RE.finditer(src):
                    verb = m.group(1).upper()
                    sub = m.group(3) or ''
                    path = (base.rstrip('/') + '/' + sub.lstrip('/')).rstrip('/')
                    if not path:
                        path = base
                    line = src[:m.start()].count('\n') + 1
                    fwd = src[m.end():m.end() + 1200]
                    cut = re.search(r'\n\s*(?:public|private|protected)\s', fwd)
                    anns = PERM_RE.findall(src[max(0, m.start() - 900):m.start()]) + \
                        PERM_RE.findall(fwd[:cut.start()] if cut else fwd)
                    codes = CODE_RE.findall(src[max(0, m.start() - 900):m.start()]) + \
                        CODE_RE.findall(fwd[:cut.start()] if cut else fwd)
                    eps.append({
                        'verb': verb, 'path': path, 'file': rel, 'line': line,
                        'anns': sorted(set(anns)), 'codes': sorted(set(codes)),
                    })
    return eps


def collect_frontend_calls():
    calls = []
    files = []
    for d in FRONT_DIRS:
        for dirpath, dirnames, filenames in os.walk(d):
            dirnames[:] = [x for x in dirnames if x not in ('.atcode', 'node_modules')]
            for fn in filenames:
                if fn.endswith(('.vue', '.ts', '.js')):
                    files.append(os.path.join(dirpath, fn))
    for f in FRONT_FILES:
        if os.path.exists(f):
            files.append(f)
    # C 端商城（mobile-mall）：相对路径调用，统一套 baseURL 前缀
    if os.path.isdir(MOBILE_MALL_SRC):
        for dirpath, dirnames, filenames in os.walk(MOBILE_MALL_SRC):
            dirnames[:] = [x for x in dirnames if x not in ('.atcode', 'node_modules', 'dist')]
            for fn in filenames:
                if fn.endswith(('.vue', '.ts', '.js')):
                    files.append(os.path.join(dirpath, fn))

    call_re = re.compile(r'request\.(get|post|put|delete|patch)\s*[<(]')
    obj_re = re.compile(r'url\s*:\s*[`\'"]([^`\'"]+)[`\'"]')
    str_re = re.compile(r'[`\'"](/[^`\'"]*)[`\'"]')
    for f in files:
        src = open(f, encoding='utf-8', errors='replace').read()
        rel = os.path.relpath(f, ROOT).replace('\\', '/')
        is_mobile = rel.replace('\\', '/').startswith('frontend/apps/mobile-mall/')
        for m in call_re.finditer(src):
            verb = m.group(1).upper()
            seg = src[m.end():m.end() + 400]
            sm = str_re.search(seg)
            if not sm:
                continue
            p = sm.group(1)
            if is_mobile and not p.startswith(MOBILE_MALL_PREFIX):
                p = MOBILE_MALL_PREFIX + ('/' + p.lstrip('/'))
            calls.append({'verb': verb, 'path': p,
                          'file': rel, 'line': src[:m.start()].count('\n') + 1})
        for m in obj_re.finditer(src):
            calls.append({'verb': 'ANY', 'path': m.group(1),
                          'file': rel, 'line': src[:m.start()].count('\n') + 1})
    return calls


def collect_frontend_path_literals():
    apps = os.path.join(ROOT, 'frontend', 'apps')
    lits = set()
    pat = re.compile(r'[`\'"](/[A-Za-z0-9_\-/{}.$:\[\]]{2,120})[`\'"]')
    for dirpath, dirnames, filenames in os.walk(apps):
        dirnames[:] = [d for d in dirnames
                       if d not in ('node_modules', 'dist', '.atcode', 'test-results')]
        for fn in filenames:
            if not fn.endswith(('.vue', '.ts', '.js')):
                continue
            try:
                src = open(os.path.join(dirpath, fn), encoding='utf-8', errors='replace').read()
            except OSError:
                continue
            for m in pat.finditer(src):
                lits.add(m.group(1))
                # C 端商城：相对字面量也要以 baseURL 前缀入池
                if 'mobile-mall' in dirpath.replace('\\', '/') \
                        and not m.group(1).startswith(MOBILE_MALL_PREFIX):
                    lits.add(MOBILE_MALL_PREFIX + m.group(1))
    return lits


def norm(p):
    p = p.split('?')[0]
    p = re.sub(r'^/api', '', p)
    p = re.sub(r'\$\{[^}]*\}', '{x}', p)
    return p.rstrip('/') or '/'


def to_regex(p):
    p = norm(p)
    parts = []
    for seg in p.split('/'):
        if seg.startswith('{') and seg.endswith('}'):
            parts.append(r'[^/]+')
        elif seg == '{x}':
            parts.append(r'[^/]+')
        else:
            parts.append(re.escape(seg))
    return re.compile('^' + '/'.join(parts) + '$')


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else '--all'
    eps = collect_backend_endpoints()
    calls = collect_frontend_calls()
    ep_res = [(e, to_regex(e['path'])) for e in eps]

    # 交易域前端调用：只看模块内的页面与 api 文件，不做全局前缀猜测
    domain = re.compile(r'^/(?:api/)?(?:erp/mall|v1/mall|mall|trade|payment|refund|reconciliation)')
    scalls = [c for c in calls if domain.search(c['path'])]

    out = []
    if mode in ('--all', '--links'):
        broken = [c for c in scalls
                  if not any(r.match(norm(c['path'])) for _e, r in ep_res)]
        out.append(('① 前端调用后端不存在的路径（断链）', broken,
                    lambda c: f"  {c['verb']:5s} {c['path']:58s} {c['file']}:{c['line']}"))

    if mode in ('--all', '--unused'):
        used = set()
        for c in scalls:
            t = norm(c['path'])
            for e, r in ep_res:
                if r.match(t):
                    used.add((e['verb'], e['path']))
        lits = collect_frontend_path_literals()
        for e in eps:
            if (e['verb'], e['path']) in used:
                continue
            if any(to_regex(l).match(norm(e['path'])) for l in lits):
                used.add((e['verb'], e['path']))
        unused = [e for e in eps if (e['verb'], e['path']) not in used]
        out.append(('② 后端端点在前端找不到调用方（冗余候选，需人工甄别导出/回调/定时任务）',
                    unused,
                    lambda e: f"  {e['verb']:5s} {e['path']:58s} {e['file']}:{e['line']}"))

    if mode in ('--all', '--perms'):
        naked = [e for e in eps if not e['anns']]
        out.append(('③ 无任何鉴权注解的端点', naked,
                    lambda e: f"  {e['verb']:5s} {e['path']:58s} {e['file']}:{e['line']}"))

    if mode in ('--all', '--codes'):
        codes = {}
        for e in eps:
            for c in e['codes']:
                codes.setdefault(c, []).append(e)
        out.append(('④ 端点使用的权限码清单（逐码核对是否在 sys_permission）', sorted(codes.items()),
                    lambda kv: f"  {kv[0]:45s} {len(kv[1])} 处  e.g. {kv[1][0]['file']}:{kv[1][0]['line']}"))

    print(f"后端交易相关端点 {len(eps)} 个；前端交易域调用 {len(scalls)} 处\n")
    for title, items, fmt in out:
        print('=' * 100)
        print(f"{title}（{len(items)} 条）")
        print('=' * 100)
        for it in items:
            print(fmt(it))
        print()


if __name__ == '__main__':
    main()
