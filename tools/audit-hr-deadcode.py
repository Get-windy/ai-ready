# -*- coding: utf-8 -*-
"""
人力资源模块后端死类扫描（只读）：
  逐个 hr-base 下的 Java 类，在「全仓源码（backend + 前端 pc-admin）」里找同名引用。
  无任何源码引用 → 死类候选

⚠️ 误报口径（务必先看这条再下结论）：
  · `*Impl` / `*Config` / `*Listener` 等**由 Spring 容器按接口或注解装配**的类，
    grep 天然看不到「引用方」（例如 `HrSalaryPaymentServiceImpl` 只在
    `@Service` + 接口注入处出现）—— 故本脚本默认跳过 `*Impl`。
  · 反过来，「不被任何源码引用」也**不等于可删**：可能是反射/SPI/配置文件登记的类。
    判定前必须回到「有没有消费方」这个业务问题上，而不是只看 grep。

用法: python tools/audit-hr-deadcode.py
"""
import os
import re
import io
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'backend', 'hr', 'hr-base', 'src', 'main', 'java')
SRC_ROOTS = [os.path.join(ROOT, 'backend'),
             os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src')]
SKIP_DIRS = {'node_modules', 'dist', 'target', '.atcode', '.git'}
SRC_EXT = ('.java', '.ts', '.vue')

texts = {}
for base in SRC_ROOTS:
    for r, dirs, files in os.walk(base):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for f in files:
            if f.endswith(SRC_EXT):
                p = os.path.join(r, f)
                try:
                    texts[p] = open(p, encoding='utf-8', errors='replace').read()
                except Exception:
                    pass

print('扫描源码文件数: %d' % len(texts))
print('=' * 90)
dead = []
for r, _, files in os.walk(SRC):
    for f in files:
        if not f.endswith('.java'):
            continue
        if f.endswith('Impl.java'):      # Spring 按接口装配，grep 看不到引用方（见模块注释）
            continue
        p = os.path.join(r, f)
        name = f[:-5]
        pat = re.compile(r'\b' + re.escape(name) + r'\b')
        hits = [k for k, v in texts.items() if k != p and pat.search(v)]
        if not hits:
            dead.append(os.path.relpath(p, ROOT).replace('\\', '/'))
            print('· 无任何源码引用: %s' % os.path.relpath(p, ROOT).replace('\\', '/'))
print('=' * 90)
print('死类候选合计: %d' % len(dead))
