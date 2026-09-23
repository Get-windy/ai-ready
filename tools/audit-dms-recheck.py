# -*- coding: utf-8 -*-
"""
配送模块审计结论稳健性复核（只读）—— 按「端点/死代码扫描三个必然误判」的口径修正后重跑：

  ① 全后端端点集：断链判定不能只用模块目录，否则跨模块调用全是假断链。
     本脚本扫 backend/ 全量（排除 target），分别产出
       - 模块集（dms + erp-delivery-route + core-base/trade + erp-sales 物流）→ 鉴权覆盖统计
       - 全后端集 → 断链判定
  ② 鉴权注解窗口：back_start = max(上一个映射注解结束位置, 上一个方法签名行结束位置)，
     fwd 止于本方法签名 —— 避免「上一个方法的注解漏给下一个方法」。

用法: python tools/audit-dms-recheck.py
输出: tool-results/dms-recheck.json
"""
import os
import re
import io
import json
import sys
import collections

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND = os.path.join(ROOT, 'backend')
SRC = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src')

MODULE_DIRS = [
    os.path.join(BACKEND, 'dms', 'src', 'main', 'java'),
    os.path.join(BACKEND, 'erp', 'erp-delivery-route', 'src', 'main', 'java'),
    os.path.join(BACKEND, 'core', 'base', 'core-base', 'src', 'main', 'java', 'cn', 'aiedge', 'trade'),
]

MAPPING = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']')
MAPPING_BARE = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\s*(?!\()')
REQ = re.compile(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']')
AUTH = re.compile(r'@(SaCheckPermission|RequirePermission|SaCheckRole|SaCheckLogin|SaIgnore|PreAuthorize)\s*(\([^)\n]*\))?')
SIG = re.compile(r'^\s*(?:public|protected|private)\s+[\w<>\[\],\s\.\$]+\s+\w+\s*\(')


def scan(path):
    """返回 (prefix, class_auth, endpoints)；注解窗口按正确口径计算"""
    src = open(path, encoding='utf-8', errors='replace').read()
    if '@RestController' not in src and '@Controller' not in src:
        return None
    cls = os.path.basename(path)[:-5]
    m = REQ.search(src)
    prefix = m.group(1) if m else ''
    lines = src.split('\n')

    # 每个映射注解所在行 + 其前后边界
    map_lines = [i for i, ln in enumerate(lines) if re.match(r'\s*@(Get|Post|Put|Delete|Patch)Mapping', ln)]
    sig_lines = [i for i, ln in enumerate(lines) if SIG.match(ln)]

    # 类级注解：第一个方法签名之前
    head_end = sig_lines[0] if sig_lines else len(lines)
    cls_auth = [a[0] + (a[1] or '') for a in AUTH.findall('\n'.join(lines[:head_end]))]

    endpoints = []
    for i in map_lines:
        ln = lines[i]
        mm = MAPPING.search(ln)
        if mm:
            verb, sub = mm.group(1).upper(), mm.group(2)
        elif MAPPING_BARE.search(ln):
            verb, sub = MAPPING_BARE.search(ln).group(1).upper(), ''
        else:
            continue
        # 本方法签名：向下最近的签名行
        fwd = next((j for j in sig_lines if j > i), min(len(lines) - 1, i + 9))
        # 上一个方法签名 / 上一个映射注解 —— 取更晚者作为 back_start
        prev_sig = max([j for j in sig_lines if j < i], default=-1)
        prev_map = max([j for j in map_lines if j < i], default=-1)
        back = max(prev_sig, prev_map)
        window = lines[back + 1:fwd + 1]
        auths = AUTH.findall('\n'.join(window))
        # 方法名
        mname = ''
        for j in range(fwd, min(len(lines), fwd + 4)):
            fm = re.search(r'(?:public|protected|private)\s+[\w<>\[\],\s\.\$]+\s+(\w+)\s*\(', lines[j])
            if fm:
                mname = fm.group(1)
                break
        endpoints.append({
            'verb': verb, 'sub': sub,
            'full': (prefix.rstrip('/') + '/' + sub.lstrip('/')).rstrip('/') or prefix,
            'method': mname, 'line': i + 1,
            'auth': [a[0] + (a[1] or '') for a in auths],
        })
    return {'class': cls, 'file': path, 'prefix': prefix, 'class_auth': cls_auth, 'endpoints': endpoints}


def norm(u):
    u = u.strip()
    if u.startswith('/api/'):
        u = u[len('/api'):]
    u = re.sub(r'\$\{[^}]*\}|\{[^}]*\}', '{}', u)
    u = re.sub(r'/+$', '', u)
    return u if u.startswith('/') else '/' + u


CALL = re.compile(r'request\s*\.\s*(get|post|put|delete|patch)\b')


def url_after(src, pos):
    i, n = pos, len(src)
    while i < n:
        c = src[i]
        if c.isspace():
            i += 1
            continue
        if c == '<':
            d = 0
            while i < n:
                if src[i] == '<':
                    d += 1
                elif src[i] == '>':
                    d -= 1
                    if d == 0:
                        i += 1
                        break
                i += 1
            continue
        break
    if i >= n or src[i] != '(':
        return None
    i += 1
    while i < n and src[i].isspace():
        i += 1
    if i < n and src[i] in '`\'"':
        q = src[i]
        j = src.find(q, i + 1)
        if j > i:
            return src[i + 1:j]
    return None


DMS_PREFIXES = ('/dms/', '/dms', '/delivery/route', '/erp/md/route',
                '/trade/api-monitor', '/trade/inventory-sync', '/trade/external-order',
                '/trade/channel', '/erp/sale/logistics')


def is_dms(p):
    return any(p == x or p.startswith(x) for x in DMS_PREFIXES)


def main():
    # ── 全后端扫描 ──
    all_ctrls = []
    for r, dirs, fs in os.walk(BACKEND):
        dirs[:] = [d for d in dirs if d not in ('target', 'node_modules')]
        if '/src/test/' in r.replace('\\', '/'):
            continue
        for f in fs:
            if f.endswith('.java'):
                info = scan(os.path.join(r, f))
                if info:
                    info['file'] = os.path.relpath(info['file'], ROOT).replace('\\', '/')
                    info['in_module'] = any(
                        os.path.abspath(os.path.join(r, f)).startswith(os.path.abspath(d)) for d in MODULE_DIRS)
                    all_ctrls.append(info)

    module_eps, all_eps = collections.defaultdict(list), collections.defaultdict(list)
    for c in all_ctrls:
        for e in c['endpoints']:
            k = (e['verb'], norm(e['full']))
            if not is_dms(k[1]):
                continue
            all_eps[k].append('%s#%s' % (c['class'], e['method']))
            if c['in_module']:
                module_eps[k].append('%s#%s' % (c['class'], e['method']))

    # ── 前端调用（api/ + views/ 直调）──
    front = collections.defaultdict(list)
    for r, dirs, fs in os.walk(SRC):
        dirs[:] = [d for d in dirs if d not in ('node_modules', 'dist')]
        for f in fs:
            if not f.endswith(('.ts', '.vue')):
                continue
            p = os.path.join(r, f)
            s = open(p, encoding='utf-8', errors='replace').read()
            rel = os.path.relpath(p, ROOT).replace('\\', '/')
            for m in CALL.finditer(s):
                u = url_after(s, m.end())
                if not u or not u.startswith('/'):
                    continue
                n = norm(u)
                if not is_dms(n):
                    continue
                front[(m.group(1).upper(), n)].append('%s:%d' % (rel, s[:m.start()].count('\n') + 1))

    missing_all = sorted(k for k in front if k not in all_eps)
    missing_mod = sorted(k for k in front if k not in module_eps)

    bare_module = []
    for c in all_ctrls:
        if not c['in_module']:
            continue
        cauth = ' '.join(c['class_auth'])
        for e in c['endpoints']:
            eff = e['auth'] or ([cauth] if 'SaIgnore' not in cauth and any(
                k in cauth for k in ('SaCheckPermission', 'RequirePermission', 'SaCheckRole',
                                     'SaCheckLogin', 'PreAuthorize')) else [])
            if not eff:
                bare_module.append('%s %s  %s#%s' % (e['verb'], e['full'], c['class'], e['method']))

    print('后端控制器总数（全后端，排除 test）: %d' % len(all_ctrls))
    print('配送域端点 —— 全后端集: %d / 模块集: %d' % (len(all_eps), len(module_eps)))
    print('前端配送域调用（不同 verb+path）: %d' % len(front))
    print()
    print('① 断链判定（用【全后端集】—— 正确口径）: %d 条' % len(missing_all))
    for k in missing_all:
        print('    %-6s %-56s %s' % (k[0], k[1], front[k][0]))
    print()
    print('② 对照：若只用【模块集】会误报成 %d 条（假断链）' % len(missing_mod))
    for k in missing_mod:
        if k not in all_eps:
            print('    假断链: %-6s %-52s 实际由 %s 提供' % (k[0], k[1], (all_eps.get(k) or ['?'])[0]))
    print()
    print('③ 模块内无鉴权端点（正确注解窗口口径）: %d 条' % len(bare_module))
    for x in bare_module:
        print('    %s' % x)

    os.makedirs(os.path.join(ROOT, 'tool-results'), exist_ok=True)
    json.dump({'missing_all': ['%s %s' % k for k in missing_all],
               'missing_module_only': ['%s %s' % k for k in missing_mod],
               'bare_module': bare_module},
              open(os.path.join(ROOT, 'tool-results', 'dms-recheck.json'), 'w', encoding='utf-8'),
              ensure_ascii=False, indent=1)
    print()
    print('输出: tool-results/dms-recheck.json')


if __name__ == '__main__':
    main()
