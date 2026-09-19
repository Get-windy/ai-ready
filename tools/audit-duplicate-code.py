# -*- coding: utf-8 -*-
"""
服务方法与工具函数级重复审计（只读）。

两件事：
  1. 后端 —— 同一「长方法名」出现在多个类里。方法名越具体（`calculateCommission`、
     `buildOrderNo`），跨类同名越说明是同一业务逻辑各写了一遍；`getById`/`page` 这类
     通用名会被长度阈值挡掉，否则全是噪声。
  2. 前端 —— utils/composables/hooks 下的同名导出函数。

用法: python tools/audit-duplicate-code.py
"""
import os
import re
import sys
import io
import json
import collections

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

JAVA_METHOD = re.compile(
    r'\b(?:public|protected|private)\s+(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?'
    r'(?!class\b|interface\b|enum\b)([\w<>,.\[\]\s?]+?)\s+(\w+)\s*\([^;{]*?\)\s*'
    r'(?:throws\s+[\w,.\s]+?)?\{')
COMMENT_LINE = re.compile(r'//[^\n]*')
COMMENT_BLOCK = re.compile(r'/\*(?:.|\n)*?\*/')
STR_LIT = re.compile(r'"(?:[^"\\]|\\.)*"')
NUM_LIT = re.compile(r'\b\d+(?:\.\d+)?[LlFfDd]?\b')
WS = re.compile(r'\s+')
TS_EXPORT = re.compile(r'export\s+(?:async\s+)?function\s+(\w+)|'
                       r'export\s+const\s+(\w+)\s*=\s*(?:async\s*)?\(')
SKIP_DIRS = {'node_modules', 'target', 'dist', '.git', '__pycache__', '.atcode',
             'test-results', 'playwright-report', 'uploads', 'logs', 'tool-results',
             '__tests__', 'tests'}
MIN_BODY_LEN = 300      # 太短的方法体（getter、单行委托）归一化后必然相同，不是重复
MIN_FILES = 2

def method_bodies(src):
    """逐个方法取「花括号配对」的方法体。返回 (方法名, 归一化体)。"""
    for m in JAVA_METHOD.finditer(src):
        name = m.group(2)
        i = src.find('{', m.end() - 1)
        if i < 0:
            continue
        depth, j = 0, i
        while j < len(src):
            c = src[j]
            if c == '{':
                depth += 1
            elif c == '}':
                depth -= 1
                if depth == 0:
                    break
            j += 1
        if depth != 0:
            continue
        yield name, src[i:j + 1]


def normalize(body):
    """抹掉注释、字符串、数字 —— 只留代码结构，这样「同一逻辑换了变量名」也能撞上。"""
    b = COMMENT_BLOCK.sub(' ', body)
    b = COMMENT_LINE.sub(' ', b)
    b = STR_LIT.sub('"S"', b)
    b = NUM_LIT.sub('0', b)
    return WS.sub(' ', b).strip()


def java_methods():
    """返回 归一化体hash -> [(文件, 方法名)]，用于找复制粘贴的方法。"""
    hits = collections.defaultdict(set)
    for dirpath, dirnames, filenames in os.walk(os.path.join(ROOT, 'backend')):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
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
            rel = os.path.relpath(p, ROOT).replace('\\', '/')
            for name, body in method_bodies(src):
                nb = normalize(body)
                if len(nb) < MIN_BODY_LEN:
                    continue
                hits[hash(nb)].add((rel, name))
    return hits


def ts_exports():
    hits = collections.defaultdict(set)
    for app in sorted(os.listdir(os.path.join(ROOT, 'frontend', 'apps'))):
        for sub in ('utils', 'composables', 'hooks', 'helpers'):
            base = os.path.join(ROOT, 'frontend', 'apps', app, 'src', sub)
            if not os.path.isdir(base):
                continue
            for dirpath, dirnames, filenames in os.walk(base):
                dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
                for fn in filenames:
                    if not fn.endswith(('.ts', '.js')):
                        continue
                    p = os.path.join(dirpath, fn)
                    try:
                        src = open(p, encoding='utf-8', errors='ignore').read()
                    except Exception:
                        continue
                    rel = os.path.relpath(p, ROOT).replace('\\', '/')
                    for a, b in TS_EXPORT.findall(src):
                        name = a or b
                        if len(name) >= 10:
                            hits[name].add(rel)
    return hits


def java_report(hits):
    """同一条归一化方法体出现在 >=2 个文件 —— 这才是「同一逻辑各写一遍」"""
    dups = {}
    for k, v in hits.items():
        files = {f for f, _ in v}
        if len(files) >= MIN_FILES:
            dups[k] = sorted(v)
    print('=== 后端：方法体完全相同的实现（归一化后 >= %d 字符）===' % MIN_BODY_LEN)
    print('提取到方法体 %d 段，其中 %d 段在不同文件中重复出现' % (len(hits), len(dups)))
    print()
    out = []
    for k, items in sorted(dups.items(), key=lambda kv: -len(kv[1])):
        names = sorted({n for _, n in items})
        files = sorted({f for f, _ in items})
        print('  [%d 处] %s' % (len(files), ', '.join(n[:34] for n in names[:3])))
        for f in files[:4]:
            print('        %s' % f)
        if len(files) > 4:
            print('        … 另 %d 处' % (len(files) - 4))
        out.append(dict(methods=names, files=files))
    print()
    return out


def ts_report(hits):
    dups = {k: sorted(v) for k, v in hits.items() if len(v) >= MIN_FILES}
    print('=== 前端：utils/composables/hooks 同名导出 ===')
    print('提取到导出名 %d 个，其中 %d 个同名出现在 >= %d 个文件' % (len(hits), len(dups), MIN_FILES))
    print()
    for name, files in sorted(dups.items(), key=lambda kv: -len(kv[1])):
        print('  %-42s %d 处' % (name, len(files)))
        for f in files[:4]:
            print('        %s' % f)
    print()
    return dups


def main():
    jd = java_report(java_methods())
    td = ts_report(ts_exports())

    path = os.path.join(ROOT, 'tools', 'audit-duplicate-code.json')
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(dict(java_duplicate_bodies=jd, ts_duplicate_exports=td), f,
                  ensure_ascii=False, indent=1)
    print('已写入 %s' % os.path.relpath(path, ROOT))


if __name__ == '__main__':
    main()
