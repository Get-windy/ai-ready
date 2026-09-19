# -*- coding: utf-8 -*-
"""
后端接口使用率审计（只读）：
  1. 扫出所有 @RestController 的接口（类级 @RequestMapping + 方法级 @XxxMapping）
  2. 扫出所有前端应用真实发起的请求路径（request.get/post/...，按各 app 的 baseURL 归一化）
  3. 对比，列出「前端从未调用」的接口 —— 这些是冗余接口的候选

注意：未调用 ≠ 一定冗余。定时任务、SSE 推送、外部平台回调、被其它后端模块
通过 HTTP 调用的接口，本来就不会出现在前端代码里。脚本会把这一类单独标注出来。

用法: python tools/audit-api-usage.py
"""
import os
import re
import sys
import io
import json
import collections

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND = os.path.join(ROOT, 'backend')
FRONTEND_APPS = os.path.join(ROOT, 'frontend', 'apps')

# 前端请求方法
REQ = re.compile(r"""request\s*\.\s*(get|post|put|delete|patch)\s*\(\s*[`'"]([^`'"]+)""")
FETCH = re.compile(r"""fetch\s*\(\s*[`'"]([^`'"]+)""")
APIREQ = re.compile(r"""apiReq\s*\(\s*[`'"](?:GET|POST|PUT|DELETE|PATCH)[`'"]\s*,\s*[`'"]([^`'"]+)""")

CLS_MAP = re.compile(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?[{]?\s*"([^"]*)"')
MTH_MAP = re.compile(r'@(Get|Post|Put|Delete|Patch|Request)Mapping\s*\(\s*(?:value\s*=\s*)?[{]?\s*"([^"]*)"')


def norm_path(p):
    """归一化：去掉 baseURL 前缀差异，把 {xxx} 与 ${xxx} 统一为 {}"""
    p = re.sub(r'\$\{[^}]*\}', '{}', p)
    p = re.sub(r'\{[^}]*\}', '{}', p)
    p = re.sub(r':[^/]+', '', p)          # /{id:\\d+} 之类
    p = re.sub(r'/+', '/', p)
    if not p.startswith('/'):
        p = '/' + p
    return p.rstrip('/') or '/'


def scan_backend():
    """返回 {norm_path: [(method, 文件, 行号)]}"""
    apis = collections.defaultdict(list)
    for dirpath, dirnames, filenames in os.walk(BACKEND):
        dirnames[:] = [d for d in dirnames if d not in ('target',)]
        if 'src/test' in dirpath.replace('\\', '/'):
            continue
        for fn in filenames:
            if not fn.endswith('.java'):
                continue
            p = os.path.join(dirpath, fn)
            try:
                src = open(p, encoding='utf-8', errors='ignore').read()
            except Exception:
                continue
            if '@RestController' not in src and '@Controller' not in src:
                continue
            m = CLS_MAP.search(src)
            base = m.group(1) if m else ''
            for mm in MTH_MAP.finditer(src):
                verb, sub = mm.group(1), mm.group(2)
                if verb == 'Request':
                    continue
                line = src[:mm.start()].count('\n') + 1
                apis[norm_path(base + '/' + sub)].append(
                    (verb.upper(), os.path.relpath(p, ROOT).replace('\\', '/'), line))
    return apis


def scan_frontend():
    """返回前端请求过的归一化路径集合"""
    used = set()
    for app in os.listdir(FRONTEND_APPS):
        base_dir = os.path.join(FRONTEND_APPS, app, 'src')
        if not os.path.isdir(base_dir):
            continue
        for dirpath, dirnames, filenames in os.walk(base_dir):
            dirnames[:] = [d for d in dirnames if d != 'node_modules']
            for fn in filenames:
                if not fn.endswith(('.ts', '.vue', '.js', '.tsx')):
                    continue
                try:
                    s = open(os.path.join(dirpath, fn), encoding='utf-8', errors='ignore').read()
                except Exception:
                    continue
                for rx, is_url in ((REQ, False), (FETCH, True), (APIREQ, True)):
                    for m in rx.finditer(s):
                        path = m.group(2) if rx is REQ else m.group(1)
                        if not path or path.startswith('http') and '/api' not in path:
                            continue
                        # 去掉 http(s)://host 前缀
                        path = re.sub(r'^https?://[^/]+', '', path)
                        used.add(norm_path(path))
    # /api/xxx 与 /xxx 都登记一份（各 app baseURL 不同：pc-admin=/api，PDA 可能=/api/v1）
    out = set()
    for u in used:
        out.add(u)
        if u.startswith('/api/'):
            out.add(u[4:])
        else:
            out.add('/api' + u)
    return out


def main():
    apis = scan_backend()
    used = scan_frontend()

    total = sum(len(v) for v in apis.values())
    unused = {k: v for k, v in apis.items() if k not in used}

    # 归类：明显属于定时任务/回调/流式/内部的，单列
    INTERNAL_PAT = re.compile(
        r'/(sse|stream|callback|notify|webhook|job|task|schedul|cron|xxl|health|actuator|metrics|internal|sync)',
        re.I)

    def bucket(path):
        return 'internal' if INTERNAL_PAT.search(path) else 'normal'

    normal_unused = {k: v for k, v in unused.items() if bucket(k) == 'normal'}
    internal_unused = {k: v for k, v in unused.items() if bucket(k) == 'internal'}

    print('后端接口总数（method+path 去重后）: %d 条，路径 %d 个' % (total, len(apis)))
    print('前端代码中出现的请求路径: %d 个' % len(used))
    print()
    print('前端从未调用的接口: %d 条' % sum(len(v) for v in unused.values()))
    print('  其中疑似内部/回调/任务类: %d 条' % sum(len(v) for v in internal_unused.values()))
    print('  其余（真正的冗余候选）:   %d 条' % sum(len(v) for v in normal_unused.values()))

    # 按模块聚合冗余候选
    by_mod = collections.defaultdict(list)
    for path, items in normal_unused.items():
        for verb, f, line in items:
            parts = f.split('/')
            mod = parts[2] if len(parts) > 2 and parts[1] == 'erp' else (parts[1] if len(parts) > 1 else '?')
            by_mod[mod].append((verb, path, f, line))

    print()
    print('=== 冗余候选按模块分布 ===')
    for mod, items in sorted(by_mod.items(), key=lambda x: -len(x[1])):
        print('  %-24s %d 条' % (mod, len(items)))

    out = {
        'total_endpoints': total,
        'total_paths': len(apis),
        'frontend_paths': len(used),
        'unused_normal': {k: v for k, v in normal_unused.items()},
        'unused_internal': {k: v for k, v in internal_unused.items()},
        'by_module': {k: len(v) for k, v in by_mod.items()},
    }
    dst = os.path.join(ROOT, 'tools', 'audit-api-usage.json')
    with open(dst, 'w', encoding='utf-8') as f:
        json.dump(out, f, ensure_ascii=False, indent=1)
    print()
    print('已写入 tools/audit-api-usage.json')


if __name__ == '__main__':
    main()
