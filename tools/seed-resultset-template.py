# -*- coding: utf-8 -*-
"""
给「结果集打印」页面落一份结果集模板（列由数据给的通用 v2 模板）。

为什么要有它：结果集页（列表/查询/报表）走的是「页面自己给 {title, columns, rows}」那条路，
模板本身与页面无关 —— 但 page_code 是模板的键，每个页面都得有自己的那一行
（用户要求：每个页面有自己的模板，之后可各自改）。几十个页面手点一遍不现实。

它做三件事：
  1. 解析页面的列定义，数出**可打印列数**（规则与前端 useListPrint.toPrintColumns 一致）；
     列定义可能是 `const columns = [...]`、`computed(() => [...])`，
     也可能是 `computed(() => ... ? STAFF_COLUMNS : DOC_COLUMNS)`（间接引用常量数组，取最大那个）；
  2. 按约定选纸张：**> 14 列用横向**（A4 `table-layout:fixed` + 表头 nowrap，纵向放不下），否则纵向；
  3. 委托 ql361-print-template-seed.py **--update** 落库（不带 --update 会再插一条同名模板 → 重复行）。

用法：
  python tools/seed-resultset-template.py <页面.vue 或 views 下的相对路径> <page_code> [--var columns] [--columns N] [--apply]
  例：python tools/seed-resultset-template.py purchase/doc-query/index.vue purchase-doc-query --apply
      python tools/seed-resultset-template.py wh/move-order/index.vue wh-move-order --var currentColumns --columns 12 --apply

默认 dry-run；--apply 才落库。--columns N 直接指定列数（列定义太动态、解析不出时用）。
"""
import io
import re
import subprocess
import sys
from pathlib import Path

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = Path(__file__).resolve().parent.parent
VIEWS = ROOT / 'frontend/apps/pc-admin/src/views'
TPL_DIR = ROOT / 'tool-results/print-template-v2'
PORTRAIT = TPL_DIR / '_result-set.json'
LANDSCAPE = TPL_DIR / '_result-set-landscape.json'
WIDE_COLUMN_THRESHOLD = 14

SKIP_TYPES = {'rowNo', 'seq', 'checkbox', 'select', 'action', 'expand', 'index'}
NOT_A_CONST = {'computed', 'value', 'activeTab', 'tab', 'if', 'return', 'true', 'false', 'props', 'ref'}


def find_columns_array(src: str, var: str):
    """抠出 `<var>` 那组列定义的数组正文（按括号配平）；认字面量与 computed 包裹两种。"""
    m = re.search(rf'^[ \t]*(?:const|let)\s+{re.escape(var)}\b', src, re.M)
    if not m:
        return None
    for mm in re.finditer(r'\[', src[m.end():]):
        i = m.end() + mm.start()
        depth = 0
        j = i
        while j < len(src):
            if src[j] == '[':
                depth += 1
            elif src[j] == ']':
                depth -= 1
                if depth == 0:
                    break
            j += 1
        body = src[i:j + 1] if j < len(src) else ''
        # ⚠️ 不能取「声明后第一个 [」：`computed<any[]>(` 里的 any[] 会先撞上 ——
        #    只在正文看起来像列对象（有 { 与 title）时才认。
        if '{' in body and 'title' in body:
            return body
    return None


def countable_columns(body: str) -> int:
    """按 useListPrint 的规则数可打印列"""
    n = 0
    for line in body.splitlines():
        line = line.strip()
        if not line.startswith('{'):
            continue
        if 'defaultHidden' in line:
            continue
        ty = (re.search(r"type:\s*'([^']*)'", line) or [None, None])[1]
        title = (re.search(r"title:\s*'([^']*)'", line) or [None, None])[1]
        key = (re.search(r"key:\s*'([^']*)'", line) or [None, None])[1]
        if not title or not key or ty in SKIP_TYPES:
            continue
        if ty == 'slot' and re.search(r'image|图片|photo', f'{key}{title}', re.I):
            continue
        n += 1
    return n


def resolve_columns_count(src: str, var: str):
    """
    返回 (可打印列数, 说明)。列定义可能是间接引用（`? A_COLUMNS : B_COLUMNS`），
    这时逐个候选数组都数一遍，取**最大**那个（宁可判成横向，也别把宽表塞进纵向）。
    """
    body = find_columns_array(src, var)
    if body is not None:
        return countable_columns(body), '直接数组'
    m = re.search(rf'^[ \t]*(?:const|let)\s+{re.escape(var)}\b.*?(?=\n[ \t]*(?:const|let|function|interface|/\*\*|//\s*═))',
                  src, re.M | re.S)
    stmt = m.group(0) if m else ''
    names = [n for n in set(re.findall(r'\b([A-Za-z_$][\w$]*)\b', stmt)) if n not in NOT_A_CONST and n != var]
    counts = []
    for n in names:
        b = find_columns_array(src, n)
        if b is not None:
            counts.append((countable_columns(b), n))
    if counts:
        best = max(counts)
        return best[0], f'间接引用 {", ".join(f"{c}@{n}" for c, n in sorted(counts, reverse=True))}'
    return None, '无法解析'


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    apply_ = '--apply' in sys.argv
    var = sys.argv[sys.argv.index('--var') + 1] if '--var' in sys.argv else 'columns'
    forced = int(sys.argv[sys.argv.index('--columns') + 1]) if '--columns' in sys.argv else None
    if len(args) < 2:
        print(__doc__)
        sys.exit(1)
    page, page_code = args[0], args[1]

    path = Path(page)
    if not path.exists():
        path = VIEWS / page
    if not path.exists():
        print(f'找不到页面文件：{page}')
        sys.exit(1)

    src = path.read_text(encoding='utf-8', errors='replace')
    if forced is not None:
        cols, how = forced, '--columns 指定'
    else:
        cols, how = resolve_columns_count(src, var)
        if cols is None:
            print(f'⚠️ 解析不出可打印列数（--var {var}，{how}）—— 用 --columns N 显式指定')
            sys.exit(1)

    wide = cols > WIDE_COLUMN_THRESHOLD
    tpl = LANDSCAPE if wide else PORTRAIT
    name = '结果集模板（横向·v2）' if wide else '结果集模板（标准·v2）'
    print(f'{page_code}: 可打印 {cols} 列（{how}）→ {"横向" if wide else "纵向"}（{tpl.name}）', flush=True)

    cmd = [sys.executable, str(ROOT / 'tools/ql361-print-template-seed.py'),
           str(tpl), page_code, '--name', name, '--update']
    if apply_:
        cmd.append('--apply')
    sys.exit(subprocess.call(cmd))


if __name__ == '__main__':
    main()
