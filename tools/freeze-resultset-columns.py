# -*- coding: utf-8 -*-
"""
把结果集页面的列**冻结进模板**（模板说了算），并落库。

为什么：业界（Odoo「模板决定外观、动作决定分发」、SAP B1 打印布局与屏幕解耦）都是
**打印的列由模板定义**，屏幕改列不该悄悄改变打印产出 —— 打印是要留档的。
我们原来只靠 `items.columnsFrom`（列由数据给），现在改成：
  · 模板里写死一份列清单（取该页的默认可见列）；
  · `columnsFrom` 保留，供显式声明 `useDataColumns: true` 的页面覆盖。

用法：
  python tools/freeze-resultset-columns.py <页面相对 views 的路径> <page_code> [--var columns] [--no-seed]
  例：python tools/freeze-resultset-columns.py md/payment-method/index.vue md-payment-method
默认 dry-run（只打印将写入的列）；加 --apply 落库（走 ql361 的 --update）。
"""
import io
import json
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
WIDE = 14
SKIP_TYPES = {'rowNo', 'seq', 'checkbox', 'select', 'action', 'expand', 'index'}
NUMERIC = re.compile(r'(quantity|qty|amount|price|money|rate|weight|volume|stock|balance|debt|fee|total)', re.I)
TEXT_NUM = re.compile(r'(no|code|phone|date)$', re.I)      # 名字里带数量词但是文本的（单号/编码/电话/日期）


def find_columns_array(src: str, var: str):
    m = re.search(rf'^[ \t]*(?:const|let)\s+{re.escape(var)}\b', src, re.M)
    if not m:
        return None
    for mm in re.finditer(r'\[', src[m.end():]):
        i = m.end() + mm.start()
        depth, j = 0, i
        while j < len(src):
            if src[j] == '[':
                depth += 1
            elif src[j] == ']':
                depth -= 1
                if depth == 0:
                    break
            j += 1
        body = src[i:j + 1] if j < len(src) else ''
        if '{' in body and 'title' in body:
            return body
    return None


def parse_columns(body: str):
    """页面列定义 → 可打印列（key/title/align/digits/agg）"""
    cols = []
    for line in (body or '').splitlines():
        l = line.strip()
        if not l.startswith('{'):
            continue
        if 'defaultHidden' in l:
            continue
        get = lambda k: (re.search(rf"{k}:\s*'([^']*)'", l) or [None, None])[1]
        key, title, ty, align = get('key') or get('field'), get('title'), get('type'), get('align')
        if not key or not title or ty in SKIP_TYPES:
            continue
        if ty == 'slot' and re.search(r'image|图片|photo', f'{key}{title}', re.I):
            continue
        col = {'header': title, 'field': key}
        if align:
            col['align'] = align
        if NUMERIC.search(key) and not TEXT_NUM.search(key):
            col.update({'align': align or 'right', 'digits': 2})
            # 金额/数量列给模板一个求和依据（引擎按模板列上的 agg 算，不是前端算）
            if re.search(r'(amount|quantity|qty)', key, re.I):
                col['agg'] = 'sum'
        col['width'] = 'auto' if re.search(r'name|remark|address|title', key, re.I) else '96px'
        cols.append(col)
    return cols


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    apply_ = '--apply' in sys.argv
    var = sys.argv[sys.argv.index('--var') + 1] if '--var' in sys.argv else 'columns'
    if len(args) < 2:
        print(__doc__)
        sys.exit(1)
    page, page_code = args[0], args[1]

    path = Path(page)
    if not path.exists():
        path = VIEWS / page
    src = path.read_text(encoding='utf-8', errors='replace')
    body = find_columns_array(src, var)
    if body is None:
        print(f'✗ {page_code}: 认不出列定义（--var {var}）')
        sys.exit(1)
    cols = parse_columns(body)
    wide = len(cols) > WIDE
    base = json.loads((LANDSCAPE if wide else PORTRAIT).read_text(encoding='utf-8'))
    base['sections']['items']['columns'] = cols
    base['sections']['items']['columnsFrom'] = 'columns'   # 显式 useDataColumns 时仍可覆盖

    out = TPL_DIR / f'{page_code}.json'
    out.write_text(json.dumps(base, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    print(f'{page_code}: 冻结 {len(cols)} 列（{"横向" if wide else "纵向"}）→ {out.name}')
    if not apply_:
        return
    name = '结果集模板（横向·v2）' if wide else '结果集模板（标准·v2）'
    rc = subprocess.call([sys.executable, str(ROOT / 'tools/ql361-print-template-seed.py'),
                          str(out), page_code, '--name', name, '--update', '--apply'])
    if rc != 0:
        print(f'✗ {page_code}: 落库失败')
        sys.exit(rc)


if __name__ == '__main__':
    main()
