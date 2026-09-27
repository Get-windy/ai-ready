# -*- coding: utf-8 -*-
"""
把「结果集打印」页面的 `handlePrint(){ window.print() }` 批量换成
`useListPrint` + `<PrintDialog>`（列由数据给的通用结果集模板）。

为什么用脚本：这类页面还有几十个，写法几乎一样 —— 逐个手改既慢又容易抄漏，
而每页真正不同的只有三样（pageCode、标题、列/行/勾选用的是哪个变量），
这三个都能从页面源码里**推导**出来，推不出来的在 OVERRIDES 里显式写。

推导规则：
  · 列   ← 模板里 `:columns="X"`（取第一个，主表格）
  · 行   ← 模板里 `:data-source="X"` / `:data="X"`
  · 勾选 ← `const X = ref(...)` 且名字像 selected/checked（模板里也用到才认）
  · 标题 ← `<PageContainer title="…">` / `<SearchBar>` 附近，退化为路由段
  · pageCode ← 路径去 views/ 与 /index.vue，斜杠与下划线换成 '-'（**不能含 /**）

用法：
  python tools/migrate-resultset-print.py --plan            # 只列出推导结果（dry-run）
  python tools/migrate-resultset-print.py --apply           # 真改
  python tools/migrate-resultset-print.py --apply --only dms-route,md-route
"""
import io
import json
import re
import sys
from pathlib import Path

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = Path(__file__).resolve().parent.parent
VIEWS = ROOT / 'frontend/apps/pc-admin/src/views'

# 明确指定（推导不出来的）
OVERRIDES = {
    'dms/route/index.vue': dict(columns='stopColumns', rows='planResult.stops || []', selected=None, title='配送路线'),
    'finance/account-delivery/index.vue': dict(columns='currentColumns', rows='allRows', selected='selectedRows', title='代管账户'),
    'finance/balance-report/index.vue': dict(columns='columns', rows='rows', selected=None, title='余额报表'),
    'finance/pending-confirm/index.vue': dict(columns='currentColumns', rows='allRows', selected='selectedRows', title='待确认'),
    'finance/payment-by-doc/index.vue': dict(columns='currentColumns', rows='tableData', selected='selectedRows', title='按单付款'),
    'finance/receipt-by-doc/index.vue': dict(columns='currentColumns', rows='tableData', selected='selectedRows', title='按单收款'),
    'finance/profit-report/index.vue': dict(columns='columns', rows='rows', selected=None, title='利润报表'),
    'wh/borrow-query/index.vue': dict(columns='currentColumns', rows='allRows', selected=None, title='借出查询'),
    'wh/move-order/index.vue': dict(columns='currentColumns', rows='tableData', selected=None, title='移库单'),
    # ⚠️ 共享组件（唯一宿主 erp/product 已自带 PrintDialog 打印）—— 它的内置 window.print()
    #    与宿主是两套，先不动共享组件；已在任务里单独记着。
    # 'md/components/PartnerListPage.vue': …, 
    'md/product-supplement/index.vue': dict(columns='brandColumns', rows='tableData', selected=None, title='商品补充资料'),
    'erp/stock-bom/index.vue': dict(columns='vxeColumns', rows='tableData', selected=None, title='BOM 清单'),
}

# 已经是单据表单（要按单据打印，需后端装配器）—— 不在本脚本范围
SKIP = {
    # 这三个其实是**单据表单**（打印的是一张已保存的单据），要按单据打印 → 需后端装配器
    'erp/stock-assemble/form.vue', 'erp/stock-split/form.vue', 'erp/stocktake/form.vue',
    # 共享组件：宿主 erp/product 自带 PrintDialog；它的内置 window.print() 单独处理
    'md/components/PartnerListPage.vue',
}


def page_code_of(rel: str) -> str:
    p = rel.replace('/index.vue', '').replace('.vue', '')
    return re.sub(r'[/_]', '-', p)


def derive(src: str, rel: str):
    # ⚠️ 不能按第一个 </template> 切 —— 模板里到处都是 <template #slot>，
    #    切出来的只有第一行；模板正文到 <script 之前才算完。
    tpl = src.split('<script')[0] if '<script' in src else src
    cols = re.search(r':columns="([A-Za-z_$][\w$]*)"', tpl)
    rows = (re.search(r':data-source="([A-Za-z_$][\w$]*)"', tpl)
            or re.search(r':data="([A-Za-z_$][\w$]*)"', tpl))
    sel = re.search(r'[:@]?selected[-_]?row[-_]?keys?="([A-Za-z_$][\w$]*)"', tpl, re.I)
    if not sel:
        sel = re.search(r':selected-rows="([A-Za-z_$][\w$]*)"', tpl)
    title = (re.search(r'<PageContainer[^>]*\stitle="([^"]+)"', tpl)
             or re.search(r'<SearchBar[^>]*\stitle="([^"]+)"', tpl)
             or re.search(r'title="([^"]{2,12})"', tpl))
    return dict(
        columns=cols.group(1) if cols else None,
        rows=rows.group(1) if rows else None,
        selected=sel.group(1) if sel else None,
        title=title.group(1) if title else None,
    )


MARK = '// ═══ 打印（结果集打印） ═══'


def need_value(src: str, var: str) -> bool:
    """该变量在本文件里是不是 ref / computed（是的话引用时要 .value）"""
    if not re.fullmatch(r'[A-Za-z_$][\w$]*', var or ''):
        return False
    return bool(re.search(rf'^[ 	]*(?:const|let)\s+{re.escape(var)}\s*=\s*(?:ref|shallowRef|computed)\s*[(<]', src, re.M))


def as_expr(src: str, var: str) -> str:
    """把变量名变成可直接写进 () => … 的表达式"""
    if not var:
        return var
    return f'{var}.value' if need_value(src, var) else var


def repair(src: str):
    """
    修上一版脚本留下的残迹（两处）：

    1. 只换了函数体，`function handlePrint() ` 这截签名留在原地 → 文件成语法错误；
    2. `rows: () => tableData` 少了 `.value` —— 传进去的是 ref 而不是数组，
       `source.length` 是 undefined，composable 会判成「没有可打印的数据」而静默不打印。

    返回 (新内容, 是否修过)。
    """
    fixed, n = re.subn(
        r'^[ \t]*(?:async\s+)?function\s+\w*[Pp]rint\w*\s*\([^)]*\)[ \t]*(?=' + re.escape(MARK) + ')',
        '', src, flags=re.M)

    # 补 .value：只对「本文件里确实是 ref 的裸标识符」动手
    # 3. 两条 import 被插进**多行 import 语句中间**（`import {` 之后）→ 语法错误，先撤掉，
    #    交给后面正常的插入逻辑重新放到正确位置
    fixed2, n2 = re.subn(
        r"^(import \{)\n(import PrintDialog from '@/components/PrintDialog/index\.vue'\n"
        r"import \{ useListPrint \} from '@/composables/useListPrint'\n)",
        r'\1\n', fixed, flags=re.M)
    fixed, n = fixed2, n + n2

    for key in ('rows', 'selectedRows'):
        for m in list(re.finditer(rf'^\s*{key}: \(\) => ([A-Za-z_$][\w$]*),$', src, re.M)):
            var = m.group(1)
            if need_value(src, var):
                fixed = fixed.replace(
                    f'  {key}: () => {var},', f'  {key}: () => {var}.value,')
                n += 1
    return fixed, n > 0


def find_print_fn(src: str):
    """
    按**花括号配平**找 `function handlePrintX() { … window.print() … }`，
    返回 (函数名, 起始下标, 结束下标)。

    ⚠️ 起始下标必须落在**行首**（含 function 关键字），只从 `{` 开始换会把
    `function handlePrint()` 这截签名留下 —— 上一版就是这么把 23 个文件写成语法错误的。
    """
    for m in re.finditer(r'^[ \t]*(?:async\s+)?function\s+(\w*[Pp]rint\w*)\s*\([^)]*\)\s*\{', src, re.M):
        start = m.end() - 1
        depth = 0
        i = start
        while i < len(src):
            if src[i] == '{':
                depth += 1
            elif src[i] == '}':
                depth -= 1
                if depth == 0:
                    if 'window.print()' in src[start:i + 1]:
                        return m.group(1), m.start(), i + 1
                    break
            i += 1
    return None


def build_block(fn_name: str, page_code: str, title: str, cols: str, rows: str, sel: str | None) -> str:
    alias = '' if fn_name == 'handlePrint' else f'handlePrint: {fn_name}'
    parts = [
        '// ═══ 打印（结果集打印） ═══',
        '// 原先是 window.print() —— 打出来是整个后台界面（菜单/工具栏/翻页都跟着上纸）。',
        '// 列取页面自己的列定义，模板按数据里的列画表头（改列不用改模板）。',
        'const { printDialogRef, printData, ' + (alias or 'handlePrint') + ' } = useListPrint({',
        f"  pageCode: '{page_code}',",
        f"  title: '{title}',",
        f'  columns: () => {cols},',
        f'  rows: () => {rows},',
    ]
    if sel:
        parts.append(f'  selectedRows: () => {sel},')
    parts += ['  emptyTip: \'没有可打印的数据\',', '})']
    return '\n'.join(parts)


def ensure_imports(out: str):
    """把两条 import 补在最后一条 import **语句**之后（多行 import 也算一条）"""
    if 'PrintDialog from' in out and "from '@/composables/useListPrint'" in out:
        return out, False
    stmts = list(re.finditer(r"^import[\s\S]*?from\s+'[^']+'", out, re.M))
    if not stmts:
        return out, False
    add = ''
    if 'PrintDialog from' not in out:
        add += "\nimport PrintDialog from '@/components/PrintDialog/index.vue'"
    if "from '@/composables/useListPrint'" not in out:
        add += "\nimport { useListPrint } from '@/composables/useListPrint'"
    last = stmts[-1]
    return out[:last.end()] + add + out[last.end():], True


def patch(src: str, rel: str, cfg: dict, apply_: bool):
    src, repaired = repair(src)
    if 'useListPrint' in src:
        src, added = ensure_imports(src)
        if (repaired or added) and apply_:
            (VIEWS / rel).write_text(src, encoding='utf-8')
        if repaired or added:
            return f'OK（补齐：{"残留签名 " if repaired else ""}{"import" if added else ""}）'
        return 'skip: 已迁移'
    if not cfg.get('columns') or not cfg.get('rows'):
        return f"skip: 推导不出列/行（columns={cfg.get('columns')} rows={cfg.get('rows')}）"
    found = find_print_fn(src)
    if not found:
        return 'skip: 认不出打印函数'
    fn_name, fn_start, fn_end = found
    page_code = page_code_of(rel)
    block = build_block(fn_name, page_code, cfg['title'] or page_code,
                        as_expr(src, cfg['columns']), as_expr(src, cfg['rows']),
                        as_expr(src, cfg.get('selected')))
    out = src[:fn_start] + block + src[fn_end:]

    # import（挂在最后一条 import 语句后面）
    out, _ = ensure_imports(out)

    # <PrintDialog>：优先挂 </ErrorBoundary> 前；子组件（没有 ErrorBoundary）挂根元素收尾前
    head = out.split('<script')[0]
    if 'page-code=' not in head:
        anchor = re.search(r'^[ \t]*</ErrorBoundary>', out, re.M)
        indent = '    '
        if not anchor:
            # 子组件：找模板里最后一个"顶层收尾标签 + </template>"
            anchor = re.search(r'^[ \t]*(</[A-Za-z][\w-]*>)[ \t]*\n</template>', out, re.M)
            if not anchor:
                return 'skip: 模板里找不到可插入的位置'
            indent = re.match(r'[ \t]*', anchor.group(0)).group(0)
        tpl_block = (f'{indent}<!-- 打印：结果集打印 -->\n'
                     f'{indent}<PrintDialog\n{indent}  ref="printDialogRef"\n'
                     f'{indent}  page-code="{page_code}"\n{indent}  :print-data="printData"\n{indent}/>\n')
        out = out[:anchor.start()] + tpl_block + out[anchor.start():]
    if apply_:
        (VIEWS / rel).write_text(out, encoding='utf-8')
    return f'OK (fn={fn_name}, pageCode={page_code}, cols={cfg["columns"]}, rows={cfg["rows"]}, sel={cfg.get("selected")})'


def main():
    apply_ = '--apply' in sys.argv
    only = None
    if '--only' in sys.argv:
        only = set(sys.argv[sys.argv.index('--only') + 1].split(','))
    plan = json.loads((ROOT / 'tool-results/print-migration-plan.json').read_text(encoding='utf-8'))
    for p in plan:
        rel = p['rel']
        if rel in SKIP:
            continue
        src = (VIEWS / rel).read_text(encoding='utf-8', errors='replace')
        if 'useBillForm(' in src:
            continue
        d = derive(src, rel)
        cfg = {**d, **OVERRIDES.get(rel, {})}
        if only and page_code_of(rel) not in only:
            continue
        print(f"{rel:52} → {patch(src, rel, cfg, apply_)}")


if __name__ == '__main__':
    main()
