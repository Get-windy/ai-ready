# -*- coding: utf-8 -*-
"""
仓储模块后端盘点（只读）：
  1. 扫描 erp-stock / wms 两个模块的 @RestController，提取类级前缀 + 方法级路径
  2. 逐端点提取鉴权注解（@SaCheckPermission / @RequirePermission / @SaCheckRole / @SaIgnore / @SaCheckLogin）
  3. 汇总：端点总数 / 无任何鉴权端点 / 使用的权限码集合
输出: tool-results/storage-backend-audit.json
用法: python tools/audit-storage-backend.py
"""
import os
import re
import io
import json
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODULES = [
    ('erp-stock', os.path.join(ROOT, 'backend', 'erp', 'erp-stock', 'src', 'main', 'java')),
    ('wms', os.path.join(ROOT, 'backend', 'wms', 'src', 'main', 'java')),
]

MAPPING = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']')
MAPPING_BARE = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\s*(?!\()')
REQMAP = re.compile(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']')
AUTH = re.compile(r'@(SaCheckPermission|RequirePermission|SaCheckRole|SaCheckLogin|SaIgnore|PreAuthorize)\s*\(?([^)\n]*)\)?')


def scan_file(path):
    src = open(path, encoding='utf-8', errors='replace').read()
    if '@RestController' not in src and '@Controller' not in src:
        return None
    cls = os.path.basename(path)[:-5]
    m = re.search(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']', src)
    prefix = m.group(1) if m else ''
    # 类级注解（第一个方法声明之前的注解区）
    first_method = re.search(r'\n\s*(?:public|protected)\s+[\w<>\[\],\s\.]+\s+\w+\s*\(', src)
    head = src[:first_method.start()] if first_method else src[:2000]
    cls_auth = AUTH.findall(head)

    endpoints = []
    # 按行扫描，记录每个 mapping 注解所在位置及其上方最近的权限注解与所属方法
    lines = src.split('\n')
    for i, ln in enumerate(lines):
        if not re.match(r'\s*@(Get|Post|Put|Delete|Patch)Mapping', ln):
            continue
        mm = MAPPING.search(ln)
        if mm:
            verb, sub = mm.group(1).upper(), mm.group(2)
        elif MAPPING_BARE.search(ln):
            verb, sub = MAPPING_BARE.search(ln).group(1).upper(), ''
        else:
            continue
        # 向上找权限注解（最多 12 行，遇到上一个 mapping/方法结束即停）
        auths = []
        for j in range(i - 1, max(-1, i - 13), -1):
            up = lines[j]
            if re.match(r'\s*@(Get|Post|Put|Delete|Patch)Mapping', up):
                break
            found = AUTH.findall(up)
            if found:
                auths.extend(found)
            if re.match(r'\s*\}\s*$', up):
                break
        # 方法名（向下找 public xxx name(）
        mname = ''
        for j in range(i, min(len(lines), i + 6)):
            fm = re.search(r'(?:public|protected)\s+[\w<>\[\],\s\.]+\s+(\w+)\s*\(', lines[j])
            if fm:
                mname = fm.group(1)
                break
        endpoints.append({
            'verb': verb,
            'sub': sub,
            'full': (prefix.rstrip('/') + '/' + sub.lstrip('/')).rstrip('/') or prefix,
            'method': mname,
            'auth': [a[0] + ('(' + a[1].strip() + ')' if a[1].strip() else '') for a in auths],
            'line': i + 1,
        })
    return {'module': '', 'class': cls, 'file': path, 'prefix': prefix,
            'class_auth': [a[0] + ('(' + a[1].strip() + ')' if a[1].strip() else '') for a in cls_auth],
            'endpoints': endpoints}


def main():
    out = []
    for mod, base in MODULES:
        if not os.path.isdir(base):
            continue
        for r, _, files in os.walk(base):
            for f in files:
                if not f.endswith('.java'):
                    continue
                info = scan_file(os.path.join(r, f))
                if info:
                    info['module'] = mod
                    info['file'] = os.path.relpath(info['file'], ROOT).replace('\\', '/')
                    out.append(info)

    total = sum(len(c['endpoints']) for c in out)
    bare = []
    codes = {}
    for c in out:
        cauth = ' '.join(c['class_auth'])
        for e in c['endpoints']:
            eff = e['auth'] or ([cauth] if 'SaIgnore' not in cauth and any(k in cauth for k in ('SaCheckPermission', 'RequirePermission', 'SaCheckRole', 'SaCheckLogin', 'PreAuthorize')) else [])
            if not eff:
                bare.append(e)
            for a in e['auth']:
                for code in re.findall(r'["\']([^"\']+)["\']', a):
                    codes.setdefault(code, []).append('%s#%s' % (c['class'], e['method']))

    os.makedirs(os.path.join(ROOT, 'tool-results'), exist_ok=True)
    dest = os.path.join(ROOT, 'tool-results', 'storage-backend-audit.json')
    with open(dest, 'w', encoding='utf-8') as f:
        json.dump({'controllers': out, 'bare_endpoints': bare, 'permission_codes': codes}, f,
                  ensure_ascii=False, indent=1)

    print('控制器: %d 个，端点: %d 个' % (len(out), total))
    print('无任何鉴权注解的端点: %d (%.0f%%)' % (len(bare), 100.0 * len(bare) / max(1, total)))
    print('端点级权限码: %d 个' % len(codes))
    print()
    print('--- 按控制器 ---')
    for c in sorted(out, key=lambda x: (x['module'], x['class'])):
        n = len(c['endpoints'])
        nb = sum(1 for e in c['endpoints'] if not e['auth'] and not c['class_auth'])
        print('  [%s] %-42s 端点=%-4d 类注解=%s 端点无注解=%d' % (
            c['module'], c['class'], n, ','.join(c['class_auth']) or '-', nb))
    print()
    print('--- 无鉴权端点明细 ---')
    for c in out:
        for e in c['endpoints']:
            if not e['auth'] and not c['class_auth']:
                print('  %-6s %-58s %s#%s:%d' % (e['verb'], e['full'], c['class'], e['method'], e['line']))
    print('输出: %s' % dest)


if __name__ == '__main__':
    main()
