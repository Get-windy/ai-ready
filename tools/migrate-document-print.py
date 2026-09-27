# -*- coding: utf-8 -*-
"""
把「单据表单页」的 `handlePrint(){ window.print() }` 换成「按单据打印」：
`<PrintDialog :document-id="formData.id">` + 打开弹窗。

与结果集页（tools/migrate-resultset-print.py）的区别：
  · 结果集页把 `{title, columns, rows}` 交给弹窗（列由数据给）；
  · 单据页只给**单据主键**，取数/挑模板/渲染都在服务端（该 pageCode 已登记 PrintDataProvider）。

页面侧的活只有三样：加两条 import、把打印函数换成打开弹窗、模板里挂上 `<PrintDialog>`。
保留原来的函数名（模板里 `@click="handlePrint"` / 快捷键都还认），所以只换函数体。

用法：
  python tools/migrate-document-print.py --plan
  python tools/migrate-document-print.py --apply
"""
import io
import re
import sys
from pathlib import Path

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = Path(__file__).resolve().parent.parent
VIEWS = ROOT / 'frontend/apps/pc-admin/src/views'

# (页面相对路径, pageCode, 说明——只进注释)
PAGES = [
    # —— 库存（erp-stock）——
    ('erp/stock-in/form.vue', 'stock-in', '采购入库单'),
    ('erp/stock-bom/form.vue', 'stock-bom', 'BOM 清单'),
    ('erp/stock-transfer/form.vue', 'stock-transfer', '库存调拨单'),
    ('erp/stock-split/form.vue', 'stock-split', '库存拆分单'),
    ('erp/stock-assemble/form.vue', 'stock-assemble', '库存组装单'),
    ('erp/stock-overflow/form.vue', 'stock-overflow', '库存报溢单'),
    ('erp/stock-cost-adjust/form.vue', 'stock-cost-adjust', '成本调整单'),
    ('erp/stocktake/form.vue', 'stock-take', '库存盘点单'),
    ('erp/stock-damage/form.vue', 'stock-damage', '库存报损单'),
    # —— 仓储（wms）——
    ('wh/borrow-in/form/index.vue', 'wh-borrow-in', '借入单'),
    ('wh/borrow-out/form/index.vue', 'wh-borrow-out', '借出单'),
    ('wh/move-order/form/index.vue', 'wh-move-order', '移库单'),
    ('wh/picking-order/form/index.vue', 'wh-picking-order', '拣货单'),
    ('wh/shipping-order/form/index.vue', 'wh-shipping-order', '发货单'),
    # —— 财务（erp-finance）——
    ('finance/advance-payment/form.vue', 'finance-advance-payment', '预付款单'),
    ('finance/advance-receipt/form.vue', 'finance-advance-receipt', '预收款单'),
    ('finance/ar-ap-adjust/form.vue', 'finance-ar-ap-adjust', '应收应付调整单'),
    ('finance/cash-transfer/form/index.vue', 'finance-cash-transfer', '资金调拨单'),
    ('finance/expense-doc/form.vue', 'finance-expense-doc', '费用单'),
    # —— 其他 ——
    ('sales/return-apply/form.vue', 'sale-return-apply', '退货申请单'),
    ('quality/standard/form.vue', 'quality-standard', '质量标准'),
    # 调度单：复用已注册的 dispatch-task 装配器（同一张 dms_task），不新登记
    ('dispatch/dispatch-order/form/index.vue', 'dispatch-task', '配送任务单'),
]

MARK = '// ═══ 打印（按单据打印） ═══'


def find_print_fn(src: str):
    """花括号配平地找 `function handlePrintX() { … window.print() … }`，返回 (名, 起, 止)"""
    for m in re.finditer(r'^[ \t]*(?:async\s+)?function\s+(\w*[Pp]rint\w*)\s*\([^)]*\)\s*\{', src, re.M):
        start = m.end() - 1
        depth, i = 0, start
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


def ensure_imports(out: str):
    if 'PrintDialog from' in out:
        return out, False
    stmts = list(re.finditer(r"^import[\s\S]*?from\s+'[^']+'", out, re.M))
    if not stmts:
        return out, False
    last = stmts[-1]
    add = "\nimport PrintDialog from '@/components/PrintDialog/index.vue'"
    return out[:last.end()] + add + out[last.end():], True


def block(fn_name: str, page_code: str, title: str) -> str:
    return f"""{MARK}
// {title}：后端已为 pageCode='{page_code}' 登记装配器并有已发布模板，
// 页面只给单据主键 —— 取数 / 挑模板 / 渲染都在服务端。
// 原先是 window.print() —— 打出来是整个后台界面（菜单、工具栏、翻页都跟着上纸）。
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const printData = ref<Record<string, any>>({{}})

function {fn_name}() {{
  if (!formData.id) {{
    message.warning('请先保存单据后再打印')
    return
  }}
  printData.value = {{ id: formData.id }}
  printDialogRef.value?.open?.()
}}"""


def patch(rel: str, page_code: str, title: str, apply_: bool) -> str:
    path = VIEWS / rel
    src = path.read_text(encoding='utf-8', errors='replace')
    if MARK in src:
        return 'skip: 已迁移'
    found = find_print_fn(src)
    if found:
        fn_name, s, e = found
        out = src[:s] + block(fn_name, page_code, title) + src[e:]
    else:
        # 有些页面的打印直接写在工具栏回调里：`case 'print': if (…) window.print() … break`
        # 这时没有函数可换，改的是那个 case 的正文；助手函数插到 </script> 之前。
        cm = re.search(r"(^[ \t]*case 'print':)([\s\S]*?)(\r?\n[ \t]*break)", src, re.M)
        if not cm:
            return "skip: 认不出打印函数（也没有 case 'print'）"
        fn_name = 'openPrintDialog'
        helper = block('__placeholder__', page_code, title).replace(
            'function __placeholder__() {', 'function openPrintDialog() {')
        out = src[:cm.start(2)] + '\n      openPrintDialog()' + src[cm.start(3):]
        out = out.replace('</script>', helper + '\n</script>')
    out, _ = ensure_imports(out)

    head = out.split('<script')[0]
    if 'page-code=' not in head:
        anchor = re.search(r'^[ \t]*</ErrorBoundary>', out, re.M)
        if not anchor:
            # 没有 ErrorBoundary 的页面（子组件 / 裸 div 根）：挂在「最后一个顶层收尾标签 + </template>」前
            # ⚠️ 行尾可能是 CRLF，锚点里必须容忍 \r
            anchor = re.search(r'^[ \t]*(</[A-Za-z][\w-]*>)[ \t]*\r?\n</template>', out, re.M)
            if not anchor:
                return 'skip: 模板里找不到可插入的位置'
        indent = re.match(r'[ \t]*', anchor.group(0)).group(0)
        tpl = (f'{indent}<!-- 打印：按单据打印 -->\n'
               f'{indent}<PrintDialog\n{indent}  ref="printDialogRef"\n'
               f'{indent}  page-code="{page_code}"\n{indent}  :document-id="printData.id"\n'
               f'{indent}  :print-data="printData"\n{indent}/>\n')
        out = out[:anchor.start()] + tpl + out[anchor.start():]
    if apply_:
        path.write_text(out, encoding='utf-8')
    return f'OK (fn={fn_name}, pageCode={page_code})'


def main():
    apply_ = '--apply' in sys.argv
    for rel, code, title in PAGES:
        print(f'{rel:44} → {patch(rel, code, title, apply_)}')


if __name__ == '__main__':
    main()
