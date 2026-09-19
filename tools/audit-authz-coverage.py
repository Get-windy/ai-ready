# -*- coding: utf-8 -*-
"""
后端鉴权覆盖率审计（只读）。

判定单位是「文件」：一个 Controller 只要类级或方法级出现任一 @SaCheckPermission /
@SaCheckLogin / @SaCheckRole，即视为「有鉴权」——类级注解覆盖整类，按方法判会大幅误报。

用法:
  python tools/audit-authz-coverage.py              # 打印统计 + 无鉴权控制器清单
  python tools/audit-authz-coverage.py --baseline   # 输出基线文件内容（供门禁使用）
  python tools/audit-authz-coverage.py --json       # 输出 JSON
"""
import os
import re
import sys
import io
import json

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND = os.path.join(ROOT, 'backend')

CTRL_ANN = re.compile(r'@(RestController|Controller)\b')
# 端点方法：Get/Post/Put/Delete/Patch Mapping（RequestMapping 单列，避免把类级算进去）
ENDPOINT = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\b')

# 「访问控制」注解 = 决定"这个请求能不能进来/有没有权限"的注解。
# 口径为「类级或方法级出现任一即算有鉴权」，因为类级注解覆盖整类。
#
# 注意自定义注解必须一并计入，否则会大幅高估缺口：
#   @RequirePermission / @RequireRole  由 core-api 的 PermissionAspect 通过 AOP 真实执行
#   @RequiresPermission                见 core-base/common/permission
#   @RequireFilePermission             文件访问专用
# 刻意**不计入**的（它们做的是数据范围过滤，不决定接口可达性）：
#   @DataPermission / @DataPermissionCheck / @BusinessPermissionCheck
AUTH_ANN = (
    '@SaCheckPermission', '@SaCheckLogin', '@SaCheckRole', '@SaIgnore',
    '@RequirePermission', '@RequireRole', '@RequiresPermission',
    '@RequireFilePermission',
)

# 仅用于报告：数据权限类注解的存在与否，说明作者是否考虑过权限，但不当作"接口已鉴权"
DATA_PERM_ANN = ('@DataPermission', '@DataPermissionCheck', '@BusinessPermissionCheck')


def scan():
    rows = []
    for dirpath, dirnames, filenames in os.walk(BACKEND):
        dirnames[:] = [d for d in dirnames if d not in ('target', 'node_modules')]
        if 'src/test' in dirpath.replace('\\', '/'):
            continue
        for fn in filenames:
            if not fn.endswith('.java'):
                continue
            p = os.path.join(dirpath, fn)
            try:
                raw = open(p, encoding='utf-8', errors='ignore').read()
            except Exception:
                continue
            # 关键：先剥掉注释再判定，否则 Javadoc 里提到的注解名会造成漏报（见 strip_comments）
            src = strip_comments(raw)
            if not CTRL_ANN.search(src):
                continue
            endpoints = len(ENDPOINT.findall(src))
            auth = [a for a in AUTH_ANN if a in src]
            rows.append({
                'file': os.path.relpath(p, ROOT).replace('\\', '/'),
                'package': package_of(src),
                'endpoints': endpoints,
                'auth': auth,
                'data_perm': [a for a in DATA_PERM_ANN if a in src],
            })
    return rows


def package_of(src):
    m = re.search(r'^\s*package\s+([\w.]+)\s*;', src, re.M)
    return m.group(1) if m else ''


def strip_comments(src):
    """
    去掉注释后再做注解匹配。

    为什么必须去注释：本项目出现过控制器在 Javadoc 里写「此处不加 {@code @SaCheckPermission}」
    的情况（`PriceApprovalController`）——按文本搜索会把它误判成「已鉴权」，产生**假阴性**
    （漏报一个真实的无鉴权接口）。注释不会写进 class 文件，所以门禁测试（搜字节码常量池）
    不受影响，本脚本必须对齐这个口径。

    权衡：这里按行截断 `//`，会连带截断字符串里的 URL。对本用途无影响
    —— 我们要找的注解名不可能出现在 URL 中。
    """
    src = re.sub(r'/\*.*?\*/', '', src, flags=re.S)
    out = []
    for line in src.split('\n'):
        idx = line.find('//')
        if idx >= 0:
            line = line[:idx]
        out.append(line)
    return '\n'.join(out)


def main():
    rows = scan()
    total = len(rows)
    ep_total = sum(r['endpoints'] for r in rows)
    no_perm = [r for r in rows if '@SaCheckPermission' not in r['auth']]
    no_any = [r for r in rows if not r['auth']]
    ep_no_any = sum(r['endpoints'] for r in no_any)

    if '--json' in sys.argv:
        print(json.dumps({'controllers': rows}, ensure_ascii=False, indent=1))
        return

    if '--baseline' in sys.argv:
        # 输出「无任何访问控制注解」的控制器全限定名，按包分组，供门禁基线使用
        groups = {}
        for r in no_any:
            groups.setdefault(r['package'], []).append(r['file'])
        for pkg in sorted(groups):
            print('# %s' % pkg)
            for f in sorted(groups[pkg]):
                print('%s.%s' % (pkg, os.path.basename(f)[:-5]))
        return

    print('Controller 总数: %d，端点方法总数: %d' % (total, ep_total))
    print('无 @SaCheckPermission: %d (%.0f%%)，覆盖端点 %d'
          % (len(no_perm), 100.0 * len(no_perm) / total, sum(r['endpoints'] for r in no_perm)))
    print('无任何 @SaCheck*:      %d (%.0f%%)，覆盖端点 %d'
          % (len(no_any), 100.0 * len(no_any) / total, ep_no_any))
    print()
    print('=== 无任何鉴权注解的控制器（按包分组）===')
    groups = {}
    for r in no_any:
        groups.setdefault(r['package'], []).append(r)
    for pkg in sorted(groups):
        items = sorted(groups[pkg], key=lambda x: x['file'])
        print('  %s  (%d 个，%d 端点)'
              % (pkg, len(items), sum(i['endpoints'] for i in items)))
        for i in items:
            print('      %-58s %d 端点' % (i['file'].split('src/main/java/')[-1], i['endpoints']))


if __name__ == '__main__':
    main()
