# -*- coding: utf-8 -*-
"""
一次遍历，统计给定权限码在**全仓**（后端 + 前端 + 迁移 SQL）里的引用点。

用途：删码前的最后一道人工核对。生效性清单只统计「注解 / v-permission / checkPermission」
两类消费方；但一个码还可能出现在：
  · 迁移 SQL 的 seed 里（历史登记，不算消费方，但要知道出处）
  · 菜单种子 / 文档 / 前端动态拼字符串
所以删之前用本脚本确认「除了登记处以外，没人真的检查它」。

用法:
  python tools/refs-of-codes.py 码1 码2 ...          # 命令行给定
  python tools/refs-of-codes.py --from-zombies B     # 直接用 tools/zombie-permissions.json 的 B 类清单
"""
import os
import io
import sys
import json

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SKIP_DIRS = {'node_modules', 'target', 'dist', 'test-results', 'playwright-report', '.git'}
SKIP_FILES = {'permission-effectivity.json'}
EXTS = ('.java', '.vue', '.ts', '.js', '.sql', '.json', '.md')


def load_codes(argv):
    if '--from-zombies' in argv:
        which = argv[argv.index('--from-zombies') + 1]
        data = json.load(open(os.path.join(ROOT, 'tools', 'zombie-permissions.json'), encoding='utf-8'))
        key = 'noEndpoint' if which.upper() == 'B' else 'hasEndpoint'
        return sorted(x['code'] for x in data[key])
    return [a for a in argv if ':' in a]


def main():
    codes = load_codes(sys.argv[1:])
    if not codes:
        print(__doc__)
        return
    hits = {c: [] for c in codes}
    for base in ('backend', 'frontend', 'docs', 'tools'):
        base_dir = os.path.join(ROOT, base)
        if not os.path.isdir(base_dir):
            continue
        for dirpath, dirnames, filenames in os.walk(base_dir):
            dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
            for fn in filenames:
                if not fn.endswith(EXTS) or fn in SKIP_FILES:
                    continue
                p = os.path.join(dirpath, fn)
                try:
                    src = open(p, encoding='utf-8', errors='replace').read()
                except Exception:
                    continue
                for c in codes:
                    if c in src:
                        rel = os.path.relpath(p, ROOT).replace('\\', '/')
                        for i, line in enumerate(src.split('\n'), 1):
                            if c in line:
                                hits[c].append('%s:%d' % (rel, i))
    print('核对 %d 个权限码的全仓引用点\n' % len(codes))
    silent, spoken = [], []
    for c in codes:
        (spoken if hits[c] else silent).append(c)
    print('═══ 全仓零引用（纯登记残留）%d 条 ═══' % len(silent))
    for c in silent:
        print('  %s' % c)
    print('\n═══ 仍有引用 %d 条（逐条看是不是消费方）═══' % len(spoken))
    for c in spoken:
        print('  %s' % c)
        for h in hits[c][:6]:
            print('       %s' % h)
        if len(hits[c]) > 6:
            print('       … 另 %d 处' % (len(hits[c]) - 6))


if __name__ == '__main__':
    main()
