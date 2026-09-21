# -*- coding: utf-8 -*-
"""
把 erp-finance 的 @PreAuthorize("hasPermission('<资源>', '<权限码>')") 统一改成本仓主流的
@SaCheckPermission("<权限码>")。

为什么要改（2026-09-20 实测结论）：
  · 全仓**没有** @EnableMethodSecurity / @EnableGlobalMethodSecurity / PermissionEvaluator
    ⇒ 这 100+ 处 Spring Security 注解**不产生任何鉴权效果**（会计凭证/总账/明细账/报表全在坑里）。
  · 而 erp-finance 自己的代码注释也警告过：「sa-token 登录不会填充 Spring Security 的
    SecurityContext，@PreAuthorize 恒抛 AccessDeniedException」
    ⇒ 若改成"启用 method security"，只会从「静默失效」变成「恒抛异常」，更糟。
  · 本仓另外 3000+ 端点都跑在 Sa-Token 上，统一到 @SaCheckPermission 是收敛而非新增。

前置条件（顺序不能反）：
  必须先补 32 个缺失权限码的种子（V11.434.0），否则改完注解 = 这些接口对非超管全 403。

用法:
  python tools/migrate-preauthorize-to-sacheck.py          # 预演（不改文件）
  python tools/migrate-preauthorize-to-sacheck.py --apply  # 实际改写
"""
import os
import re
import sys
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGET_DIR = os.path.join(ROOT, 'backend', 'erp', 'erp-finance')

ANNOTATION = re.compile(
    r'@PreAuthorize\(\s*"hasPermission\(\s*(?P<resource>[^,]+?)\s*,\s*\'(?P<code>[^\']+)\'\s*\)"\s*\)'
)
PREAUTH_IMPORT = re.compile(r'^\s*import\s+org\.springframework\.security\.access\.prepost\.PreAuthorize;\s*$',
                            re.M)
SACHECK_IMPORT = 'import cn.dev33.satoken.annotation.SaCheckPermission;'


def migrate_file(path, apply):
    with open(path, encoding='utf-8') as f:
        original = f.read()

    matches = list(ANNOTATION.finditer(original))
    if not matches:
        return 0, 0, []

    codes = [m.group('code') for m in matches]
    updated = ANNOTATION.sub(lambda m: '@SaCheckPermission("%s")' % m.group('code'), original)

    # 若文件里已不存在真正的 @PreAuthorize 注解用法（注释里提到的不算），删掉它的 import
    leftover = len(ANNOTATION.findall(updated))
    removed_import = 0
    if leftover == 0 and '@PreAuthorize(' not in updated.replace('@PreAuthorize("hasPermission', ''):
        new_text, n = PREAUTH_IMPORT.subn('', updated)
        removed_import = n
        updated = new_text

    # 补 SaCheckPermission 的 import
    inserted_import = 0
    if SACHECK_IMPORT not in updated:
        lines = updated.split('\n')
        for i, line in enumerate(lines):
            if line.startswith('import '):
                lines.insert(i, SACHECK_IMPORT)
                inserted_import = 1
                break
        updated = '\n'.join(lines)

    if apply and updated != original:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(updated)

    return len(matches), removed_import + inserted_import, codes


def main():
    apply = '--apply' in sys.argv
    total_ann = 0
    total_files = 0
    all_codes = set()

    for dirpath, dirnames, filenames in os.walk(TARGET_DIR):
        dirnames[:] = [d for d in dirnames if d != 'target']
        for fn in filenames:
            if not fn.endswith('.java'):
                continue
            path = os.path.join(dirpath, fn)
            count, imports, codes = migrate_file(path, apply)
            if count:
                total_files += 1
                total_ann += count
                all_codes.update(codes)
                rel = os.path.relpath(path, ROOT).replace('\\', '/')
                print('  %-96s %2d 处' % (rel, count))

    print()
    print('%s：%d 个文件、%d 处注解；涉及 %d 个不同权限码'
          % ('已改写' if apply else '预演（未改文件）', total_files, total_ann, len(all_codes)))

    # 校验：所有权限码都必须已在库中（缺失会导致接口对非超管全 403）
    try:
        import psycopg2
        conn = psycopg2.connect(host='localhost', port=5432, dbname='devdb',
                                user='devuser', password='devuser123')
        cur = conn.cursor()
        cur.execute('SELECT permission_code FROM sys_permission WHERE deleted = 0')
        db = {r[0] for r in cur.fetchall()}
        missing = sorted(c for c in all_codes if c not in db)
        if missing:
            print('!! 以下权限码在库中不存在，改完会导致这些接口对非超管一律 403：')
            for c in missing:
                print('     ', c)
        else:
            print('校验通过：涉及的 %d 个权限码全部已在 sys_permission 中' % len(all_codes))
    except Exception as e:
        print('（跳过库校验：%s）' % e)


if __name__ == '__main__':
    main()
