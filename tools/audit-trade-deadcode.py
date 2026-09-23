# -*- coding: utf-8 -*-
"""交易模块死代码扫描（只读）：后端 Java 类在本仓（含其它模块、前端、SQL、脚本）零引用者。

口径提醒：
  · Spring 组件（@RestController/@Service/@Component/@Mapper/@Configuration）由容器装配，
    不依赖「有人 new / 有人 import」⇒ 零引用**不等于**可删。本脚本把它单独分组，仅作线索。
  · 只有「既零引用、又不是 Spring 组件、也没被反射/SPI 名单点到」的类才是删候选。
"""
import os
import re
import io
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOTS = [
    os.path.join(ROOT, 'backend', 'erp', 'erp-mall', 'src', 'main', 'java'),
    os.path.join(ROOT, 'backend', 'core', 'base', 'core-base', 'src', 'main', 'java', 'cn', 'aiedge', 'trade'),
    os.path.join(ROOT, 'backend', 'core', 'payment', 'core-payment', 'src', 'main', 'java'),
]
BACKEND = os.path.join(ROOT, 'backend')
FRONT = os.path.join(ROOT, 'frontend', 'apps')

SPRING_ANN = re.compile(
    r'@(RestController|Controller|Service|Component|Mapper|Configuration|Repository)\b')

targets = {}
for r in ROOTS:
    for dp, _dn, fns in os.walk(r):
        for fn in fns:
            if fn.endswith('.java'):
                targets.setdefault(fn[:-5], os.path.join(dp, fn))

pool = []
for base in (BACKEND, FRONT):
    for dp, dn, fns in os.walk(base):
        dn[:] = [d for d in dn if d not in ('node_modules', 'dist', 'target', '.git')]
        for fn in fns:
            if fn.endswith(('.java', '.vue', '.ts', '.js', '.xml', '.sql', '.yml')):
                pool.append(os.path.join(dp, fn))

texts = {}
for p in pool:
    try:
        texts[p] = open(p, encoding='utf-8', errors='replace').read()
    except OSError:
        pass

print('模块类 %d 个；全仓扫描文件 %d 个\n' % (len(targets), len(texts)))
plain = []
spring = []
for cls, path in sorted(targets.items()):
    if cls.endswith('Application'):
        continue
    hits = 0
    for p, t in texts.items():
        if os.path.normpath(p) == os.path.normpath(path):
            continue
        if re.search(r'\b' + re.escape(cls) + r'\b', t):
            hits += 1
    if hits:
        continue
    src = texts.get(path, '')
    rel = os.path.relpath(path, ROOT).replace('\\', '/')
    # ⚠️ 必须扫全文再看注解：本仓类的 Javadoc 动辄 60+ 行，
    # 只看前 1500 字符会把 @Service/@RestController 落在窗口之外 ⇒ 误报「非 Spring 组件」
    # （实测 MallPaymentController / ApiMonitorServiceImpl 就是这种假阳性）。
    (spring if SPRING_ANN.search(src) else plain).append((cls, rel))

print('=== A. 零引用且非 Spring 组件（%d 个，删除候选）===' % len(plain))
for cls, p in plain:
    print('  %-42s %s' % (cls, p))
print()
print('=== B. 零引用但是 Spring 组件（%d 个，容器装配不算引用；仅作线索）===' % len(spring))
for cls, p in spring:
    print('  %-42s %s' % (cls, p))
