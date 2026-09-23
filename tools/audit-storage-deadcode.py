# -*- coding: utf-8 -*-
"""
仓储模块后端死代码扫描（只读）：
  对 erp-stock / wms 两个模块的每个顶层 Java 类，统计它在整个 backend 源码
  （排除自身文件、排除 target/）中被引用的文件数。0 引用 = 死代码候选。
  另单独统计「仅被 src/test 引用」的类。
输出: tool-results/storage-deadcode.json
用法: python tools/audit-storage-deadcode.py
"""
import os
import re
import io
import json
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND = os.path.join(ROOT, 'backend')
TARGET_MODULES = [
    os.path.join(BACKEND, 'erp', 'erp-stock', 'src', 'main', 'java'),
    os.path.join(BACKEND, 'wms', 'src', 'main', 'java'),
]


def main():
    # 1) 收集全部 java 文件文本
    files = {}
    for r, dirs, fs in os.walk(BACKEND):
        dirs[:] = [d for d in dirs if d not in ('target', 'node_modules')]
        for f in fs:
            if f.endswith('.java'):
                p = os.path.join(r, f).replace('\\', '/')
                try:
                    files[p] = open(p, encoding='utf-8', errors='replace').read()
                except Exception:
                    pass

    # 2) 目标模块的类
    targets = {}
    for base in TARGET_MODULES:
        if not os.path.isdir(base):
            continue
        for r, dirs, fs in os.walk(base):
            dirs[:] = [d for d in dirs if d != 'target']
            for f in fs:
                if not f.endswith('.java'):
                    continue
                p = os.path.join(r, f).replace('\\', '/')
                src = files.get(p, '')
                # 逐行找类型声明，跳过注释/字符串行 —— 否则注释里出现 "class" 字样会抓错类名
                m = None
                for line in src.split('\n'):
                    s = line.strip()
                    if s.startswith('*') or s.startswith('//') or s.startswith('/*'):
                        continue
                    m = re.search(r'(?:^|\s)(?:public\s+)?(?:final\s+|abstract\s+)?'
                                  r'(?:class|interface|enum|record)\s+(\w+)', s)
                    if m:
                        break
                if not m:
                    continue
                # ⚠️ 关键判据修正：Spring Bean（@Controller/@Service/@Component/@Configuration/@Mapper…）
                # 由容器/扫描发现，Java 源码里本就没有对实现类的引用 —— 按文本引用计数会把它们
                # 全部误判成死代码。这里只对「非容器管理的普通类」做引用计数。
                if re.search(r'@(RestController|Controller|Service|Component|Repository|Configuration|'
                             r'Mapper|Aspect|RestControllerAdvice|ControllerAdvice|MapperScan|'
                             r'ConfigurationProperties|Entity|TableName)\b', src):
                    continue
                targets[m.group(1)] = p

    dead, test_only, alive = [], [], []
    for cls, p in sorted(targets.items()):
        uses = []
        test_uses = []
        pat = re.compile(r'\b' + re.escape(cls) + r'\b')
        for q, src in files.items():
            if q == p:
                continue
            if not pat.search(src):
                continue
            if cls not in os.path.basename(q)[:-5]:  # 同名文件（如 XxxTest 引用 Xxx）仍需算
                pass
            (test_uses if '/src/test/' in q else uses).append(q)
        if not uses and not test_uses:
            dead.append((cls, p))
        elif not uses and test_uses:
            test_only.append((cls, p, test_uses))
        else:
            alive.append((cls, p, len(uses)))

    print('目标模块类总数: %d' % len(targets))
    print('完全无引用（死代码）: %d' % len(dead))
    print('仅被测试引用: %d' % len(test_only))
    print()
    print('=' * 100)
    print('① 无任何引用')
    print('=' * 100)
    for cls, p in dead:
        print('  %-42s %s' % (cls, os.path.relpath(p, ROOT).replace('\\', '/')))
    print()
    print('=' * 100)
    print('② 仅被单元测试引用（生产代码无消费方）')
    print('=' * 100)
    for cls, p, t in test_only:
        print('  %-42s %s' % (cls, os.path.relpath(p, ROOT).replace('\\', '/')))
        for q in t[:3]:
            print('         ← %s' % os.path.relpath(q, ROOT).replace('\\', '/'))

    os.makedirs(os.path.join(ROOT, 'tool-results'), exist_ok=True)
    dest = os.path.join(ROOT, 'tool-results', 'storage-deadcode.json')
    with open(dest, 'w', encoding='utf-8') as f:
        json.dump({
            'dead': [{'class': c, 'file': os.path.relpath(p, ROOT).replace('\\', '/')} for c, p in dead],
            'test_only': [{'class': c, 'file': os.path.relpath(p, ROOT).replace('\\', '/'),
                           'used_by': [os.path.relpath(q, ROOT).replace('\\', '/') for q in t]} for c, p, t in test_only],
        }, f, ensure_ascii=False, indent=1)
    print()
    print('输出: %s' % dest)


if __name__ == '__main__':
    main()
