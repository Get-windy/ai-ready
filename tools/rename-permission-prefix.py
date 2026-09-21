# -*- coding: utf-8 -*-
"""
把「租户内的人与权限管理」这批权限码从旧前缀改名为 `tenant-admin:` 码族
（跨后端注解 + 前端 v-permission + 种子配置）。

为什么要脚本而不是手工 sed：
  本仓的铁律是「**权限码必须先在 sys_permission 里存在，注解才能挂**」——
  改成一半（比如 DB 改了、后端注解没改）的症状是**所有非超管一律 403**，
  而且不会编译失败、不会启动失败，只在被调用时才暴露。
  所以改名必须：① 先列全引用点；② 一次性改干净；③ 事后断言"旧码零残留"。

⚠️ 为什么**不能按前缀**替换（本脚本第一版踩到的坑）：
  `position:` 这个前缀不只是权限码 —— 打印引擎里到处是 CSS 的
  `position:relative;` / `position:absolute;`（实测 4 处）。按前缀替换会把打印样式改坏。
  故本脚本先**从真库读出这批码的完整清单**，再按**整码精确匹配**替换：
  只有 `department:list` 这种真实存在的码才会被改，`position:relative` 天然不匹配。

安全设计：
  · 码清单来自 `sys_permission`（真库），不手写；
  · 边界要求：码的前后都不能是 `[A-Za-z0-9_:-]`（避免 `system:role:view` 命中 `x-system:role:view` 之类）；
  · `--plan` 只统计与列点，`--apply` 才写文件，写完自动复扫，发现残留即 exit 1。

用法:
  python tools/rename-permission-prefix.py --plan
  python tools/rename-permission-prefix.py --apply
"""
import os
import re
import io
import sys
import json
import subprocess

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SKIP_DIRS = {'node_modules', 'target', 'dist', 'test-results', 'playwright-report', '.git'}
EXTS = ('.java', '.vue', '.ts')

NEW_FAMILY = 'tenant-admin'

# 从「系统」域剥离出来、并入租户管理族的子域
FROM_SYSTEM = ('user', 'role', 'permission', 'data-scope', 'field-permission',
               'record-rule', 'sod-rule')
# 本来就是裸前缀的子域（一并纳入同一个码族，避免"半新半旧"两种写法）
# ⚠️ `data-scope` 是**补漏**进来的：同一个「数据权限」子域里居然混着两种写法 ——
#    3 条是 `system:data-scope:{assign,delete,list}`（配 SysDataScopeController），
#    另 2 条是裸的 `data-scope:{view,set}`（配 UserDataScopeController）。
#    第一版只搬了 system: 那半，结果这 2 条既留在裸前缀、又因为映射表里 `data-scope:`
#    那行被删掉而**变成"无模块归属"** —— 靠 `verify-module-mapping.cjs` 的
#    "100% 在役码必须有归属"断言才暴露出来。
BARE = ('department', 'position', 'permission-template', 'role-inheritance', 'data-scope')


def load_db_codes():
    """从真库读出这批码的完整清单（唯一事实来源）"""
    like = ' OR '.join(
        ["permission_code LIKE 'system:%s:%%'" % f for f in FROM_SYSTEM] +
        ["permission_code LIKE '%s:%%'" % f for f in BARE])
    stmt = ("SELECT permission_code FROM sys_permission WHERE deleted = 0 AND (%s) "
            "ORDER BY permission_code" % like)
    out = subprocess.run(['node', os.path.join(ROOT, 'tools', 'sql.cjs'), stmt],
                         capture_output=True, text=True, encoding='utf-8', cwd=ROOT).stdout
    lines = [l for l in out.replace('\r', '').split('\n')
             if l.strip() and not l.startswith('permission_code') and not l.startswith('-')
             and not re.match(r'^\(\d+ rows\)$', l.strip())]
    return [l.strip() for l in lines]


def new_name(code):
    """旧码 → 新码"""
    for fam in FROM_SYSTEM:
        if code.startswith('system:%s:' % fam):
            return NEW_FAMILY + ':' + code[len('system:'):]
    for fam in BARE:
        if code.startswith(fam + ':'):
            return NEW_FAMILY + ':' + code
    raise ValueError('不属于本次改名范围的码: %s' % code)


def code_pattern(code):
    """整码精确匹配：前后都不得是标识符/冒号/连字符"""
    return re.compile(r'(?<![A-Za-z0-9_:-])' + re.escape(code) + r'(?![A-Za-z0-9_:-])')


def scan(rename):
    found = []
    for base in ('backend', 'frontend', 'docs'):
        base_dir = os.path.join(ROOT, base)
        if not os.path.isdir(base_dir):
            continue
        for dirpath, dirnames, filenames in os.walk(base_dir):
            dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
            for fn in filenames:
                if not fn.endswith(EXTS):
                    continue
                p = os.path.join(dirpath, fn)
                try:
                    lines = open(p, encoding='utf-8', errors='replace').read().split('\n')
                except Exception:
                    continue
                rel = os.path.relpath(p, ROOT).replace('\\', '/')
                for i, line in enumerate(lines, 1):
                    for code in rename:
                        if code_pattern(code).search(line):
                            found.append((rel, i, line.strip()[:150], code))
    return found


def main():
    apply = '--apply' in sys.argv
    codes = load_db_codes()
    if not codes:
        print('!! 从库里没读到这批码，先确认 DB 与筛选条件')
        sys.exit(2)
    rename = {c: new_name(c) for c in codes}

    hits = scan(rename)
    by_file = {}
    for h in hits:
        by_file[h[0]] = by_file.get(h[0], 0) + 1

    print('待改名权限码 %d 条（来自真库 sys_permission）' % len(rename))
    print('  %s' % ', '.join(sorted(rename.values())[:6]) + ' …')
    print('\n引用点合计 %d 处，涉及 %d 个文件\n' % (len(hits), len(by_file)))
    print('── 按文件（前 25）──')
    for f, n in sorted(by_file.items(), key=lambda kv: -kv[1])[:25]:
        print('  %-88s %3d' % (f, n))
    if len(by_file) > 25:
        print('  …… 另 %d 个文件' % (len(by_file) - 25))

    if not apply:
        print('\n（--plan 模式，未改动任何文件）')
        print('\n── 逐处明细（前 15）──')
        for h in hits[:15]:
            print('  %s:%d  %s' % (h[0], h[1], h[2]))
        return

    changed = 0
    for rel in sorted(by_file):
        p = os.path.join(ROOT, rel)
        src = open(p, encoding='utf-8', errors='replace').read()
        out = src
        for old in sorted(rename, key=len, reverse=True):   # 长码优先，避免前缀码吃掉长码
            out = code_pattern(old).sub(rename[old], out)
        if out != src:
            with open(p, 'w', encoding='utf-8', newline='') as fp:
                fp.write(out)
            changed += 1
    print('\n已改写 %d 个文件' % changed)

    left = scan(rename)
    if left:
        print('\n!! 仍有 %d 处旧码残留（必须人工处理）：' % len(left))
        for h in left[:20]:
            print('   %s:%d  %s' % (h[0], h[1], h[2]))
        sys.exit(1)
    print('✅ 复扫确认：%d 条旧码在源码里**零残留**' % len(rename))


if __name__ == '__main__':
    main()
