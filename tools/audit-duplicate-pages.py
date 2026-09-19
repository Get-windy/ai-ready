# -*- coding: utf-8 -*-
"""
前端页面级功能重复审计（只读）：找出「两个页面在做同一件事」。

判定：页面调用同一套 API 模块 + 同一批方法 ⇒ 功能重合。
指纹 = `模块#方法` 集合，来源是页面里 `@/api/*` 的导入与实际调用（含 `xxxApi.page()` 形式）。

刻意不按页面标题或字段名比对：中文标题重名常见（"客户"出现过 3 次），
字段名相似也可能只是同构表单；API 调用集是更硬的证据。

输出会标注每个页面是否已挂菜单（对照 audit-orphan-pages.py 的孤儿清单），
因为「两个孤儿页重复」和「两个在用页重复」的处置完全不同。

用法: python tools/audit-duplicate-pages.py
"""
import os
import re
import sys
import io
import json
import collections

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
APPS = os.path.join(ROOT, 'frontend', 'apps')

IMPORT_NAMED = re.compile(r"import\s*\{([^}]*)\}\s*from\s*['\"]([^'\"]+)['\"]")
IMPORT_DEFAULT = re.compile(r"import\s+(\w+)\s*(?:,\s*\{[^}]*\})?\s*from\s*['\"]([^'\"]+)['\"]")
SKIP_DIRS = {'node_modules', 'dist', '__tests__', 'tests', 'components'}
# 子页（新增/编辑/详情/弹窗）本身就是被父页拉起的，与父页共用 API 是设计使然，不算重复
SUB_PAGE = re.compile(r'(form|detail|edit|modal|dialog|drawer|tab|components?)(\.vue)?(/|$)', re.I)
CONST_NAME = re.compile(r'^[A-Z][A-Z0-9_]*$')

MIN_SHARED = 3      # 至少共同调用 3 个接口才算候选，避免小页面乱配
MIN_JACCARD = 0.6


def api_module(path):
    """只认 @/api/* 与相对 api 引用；其它导入（组件、工具）不参与指纹"""
    m = re.match(r'^@/api/(.+)$', path)
    if m:
        return m.group(1)
    m = re.match(r'^.*?/api/(.+)$', path)
    if m and 'api/' in path:
        return m.group(1)
    return None


def fingerprint(src):
    fp = set()
    default_vars = {}   # 变量名 -> 模块
    for members_s, path in IMPORT_NAMED.findall(src):
        mod = api_module(path)
        if not mod:
            continue
        for raw in members_s.split(','):
            raw = raw.strip()
            if raw.startswith('type '):      # TS 内联类型导入，不是接口
                continue
            name = raw.split(' as ')[-1].strip()
            if name and not CONST_NAME.match(name):
                fp.add('%s#%s' % (mod, name))
    for var, path in IMPORT_DEFAULT.findall(src):
        mod = api_module(path)
        if mod:
            default_vars[var] = mod
    for var, mod in default_vars.items():
        for meth in set(re.findall(r'\b%s\.(\w+)\s*\(' % re.escape(var), src)):
            fp.add('%s#%s' % (mod, meth))
    return fp


def collect():
    pages = {}
    orphan_keys = set()
    try:
        od = json.load(open(os.path.join(ROOT, 'tools', 'audit-orphan-pages.json'), encoding='utf-8'))
        orphan_keys = {o['key'] for o in od.get('orphan_keys', [])}
    except Exception:
        pass

    for app in sorted(os.listdir(APPS)):
        vroot = os.path.join(APPS, app, 'src', 'views')
        if not os.path.isdir(vroot):
            continue
        for dirpath, dirnames, filenames in os.walk(vroot):
            dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
            for fn in filenames:
                if not fn.endswith('.vue'):
                    continue
                p = os.path.join(dirpath, fn)
                rel = os.path.relpath(p, vroot).replace('\\', '/')
                if SUB_PAGE.search(rel[:-4]):
                    continue
                key = rel[:-4]
                if key.endswith('/index'):
                    key = key[:-6]
                try:
                    src = open(p, encoding='utf-8', errors='ignore').read()
                except Exception:
                    continue
                fp = fingerprint(src)
                if len(fp) < MIN_SHARED:
                    continue
                pages['%s:%s' % (app, rel)] = dict(
                    app=app, file=rel, key=key, fp=fp,
                    orphan=key in orphan_keys or rel[:-4] in orphan_keys)
    return pages


def main():
    pages = collect()
    print('参与比对的页面（API 调用 >= %d 个）: %d 个' % (MIN_SHARED, len(pages)))

    keys = sorted(pages)
    groups = []
    for i, a in enumerate(keys):
        for b in keys[i + 1:]:
            if pages[a]['key'] == pages[b]['key']:
                continue
            fa, fb = pages[a]['fp'], pages[b]['fp']
            inter = fa & fb
            if len(inter) < MIN_SHARED:
                continue
            j = len(inter) / float(len(fa | fb))
            if j >= MIN_JACCARD:
                groups.append((round(j, 2), a, b, sorted(inter)))

    groups.sort(reverse=True)
    print('功能重复候选组: %d 组' % len(groups))
    print()

    both_orphan = [g for g in groups if pages[g[1]]['orphan'] and pages[g[2]]['orphan']]
    both_live = [g for g in groups if not pages[g[1]]['orphan'] and not pages[g[2]]['orphan']]
    mixed = [g for g in groups if g not in both_orphan and g not in both_live]

    def show(title, items, limit=999):
        print('=== %s（%d 组）===' % (title, len(items)))
        for j, a, b, inter in items[:limit]:
            pa, pb = pages[a], pages[b]
            print('  [%.2f] %s  <->  %s' % (j, pa['file'], pb['file']))
            print('         共同接口 %d 个: %s' % (len(inter), ', '.join(x.split('#')[-1] for x in inter[:8])))
        print()

    show('双方都已挂菜单（真·重复建设）', both_live)
    show('一方挂菜单、一方孤儿（多为旧版未删）', mixed)
    show('双方都是孤儿（属孤儿页清理范畴）', both_orphan, 20)

    out = [dict(jaccard=j, a=pages[a]['file'], b=pages[b]['file'],
                a_orphan=pages[a]['orphan'], b_orphan=pages[b]['orphan'],
                shared=[x for x in inter]) for j, a, b, inter in groups]
    path = os.path.join(ROOT, 'tools', 'audit-duplicate-pages.json')
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(out, f, ensure_ascii=False, indent=1)
    print('已写入 %s' % os.path.relpath(path, ROOT))


if __name__ == '__main__':
    main()
