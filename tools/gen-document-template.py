# -*- coding: utf-8 -*-
"""
按「后端实体 + 前端页面列标题」生成一张单据的 v2 打印模板。

为什么要有它：单据打印的模板本该「照着实体写」—— 字段名就是实体属性名。
而中文标签前端页面上**已经有了**（明细表格的列标题就是同一批字段），
所以标签从前端取、字段从后端实体取，两边不会各写一套、也不会对不上。

生成规则：
  · 单据头：按 PREFERRED_HEADER 的顺序挑字段（单号/日期/往来单位/仓库/经手人/…），
    跳过 ID 类字段（末尾 Id）与审计字段（tenantId/deleted/versionNo/createBy…）；
  · 明细列：实体的字段 ∩ 页面上有中文标题的字段（没标题的不硬塞，宁可少打一列）；
  · 合计：数量类与金额类各给一个（走模板的 agg:sum，渲染时按明细算）；
  · 金额大写：挑一个叫 xxxAmount/xxxMoney 的字段做 rmbUpper。

用法：
  python tools/gen-document-template.py \
      <页面.vue> <pageCode> <主实体.java> <明细实体.java> [--title 单据名] [--out 文件名]
  例：python tools/gen-document-template.py \
      frontend/apps/pc-admin/src/views/erp/stock-in/form.vue stock-in \
      backend/erp/erp-stock/src/main/java/cn/aiedge/erp/stock/entity/StockIn.java \
      backend/erp/erp-stock/src/main/java/cn/aiedge/erp/stock/entity/StockInItem.java --title 采购入库单
生成到 tool-results/print-template-v2/<pageCode>.json（不落库；用 ql361-print-template-seed.py 落）
"""
import io
import json
import re
import sys
from pathlib import Path

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / 'tool-results/print-template-v2'

# 单据头字段的偏好顺序（前面的先放），值为中文标签
PREFERRED_HEADER = [
    ('No', '单号'), ('Date', '日期'), ('status', '单据状态'), ('statusName', '单据状态'),
    ('partnerName', '往来单位'), ('customerName', '客户'), ('supplierName', '供应商'),
    ('warehouseName', '仓库'), ('handlerName', '经手人'), ('deptName', '部门'),
    ('salesmanName', '销售员'), ('receiverName', '收货人'), ('receiverPhone', '收货电话'),
    ('shippingAddress', '收货地址'), ('region', '区域'),
    ('totalQuantity', '数量合计'), ('totalAmount', '金额合计'),
    ('summary', '摘要'), ('remark', '单据备注'), ('creatorName', '制单人'), ('createTime', '制单时间'),
]
# 直接不上纸的字段（主键/租户/审计/内部）
SKIP_ALWAYS = {'id', 'tenantId', 'deleted', 'versionNo', 'createBy', 'updateBy', 'extInfo',
               'printCount', 'attachment', 'bookkeeperId', 'submitBy', 'approvedBy'}
# 常见字段的中文标签兜底（页面没给标题时用）
LABELS = {
    'lineNo': '行号', 'productCode': '商品编码', 'itemCode': '货号', 'productName': '商品名称',
    'productSpec': '规格', 'specification': '规格', 'model': '型号', 'origin': '产地', 'brand': '品牌',
    'productUnit': '单位', 'unit': '单位', 'barcode': '条码', 'batchCode': '批号', 'batchNo': '批号',
    'quantity': '数量', 'totalQuantity': '数量合计', 'unitPrice': '单价', 'amount': '金额',
    'lineAmount': '金额', 'discountedAmount': '折后金额', 'discountRate': '折扣(%)',
    'location': '货位', 'storageLocation': '货位', 'remark': '备注', 'gift': '赠品', 'isGift': '赠品',
    'productionDate': '生产日期', 'expiryDate': '到期日期', 'shelfLife': '保质期',
    # 主数据/单表单据（页面的明细列里没有这些名字）
    # 预算模块（JPA 模型，字段名自成一套）
    'budgetNo': '预算单号', 'fiscalYear': '年度', 'templateName': '预算模板', 'departmentName': '部门',
    'departmentId': '部门', 'totalApprovedAmount': '已下达', 'totalUsedAmount': '已使用',
    'totalRemainingAmount': '剩余', 'totalFrozenAmount': '冻结', 'executionRate': '执行率',
    'auditorName': '审核人', 'auditTime': '审核时间', 'auditRemark': '审核意见', 'budgetDate': '编制日期',
    'subjectCode': '科目编码', 'subjectName': '科目名称', 'budgetAmount': '预算金额',
    'usedAmount': '已用金额', 'remainingAmount': '剩余金额', 'frozenAmount': '冻结金额',
    'sortOrder': '序号', 'budgetId': '预算单',
    'standardCode': '标准编号', 'standardName': '标准名称', 'inspectionType': '检验类型',
    'inspectionItems': '检验项目', 'sampleRate': '抽检比例(%)', 'passThreshold': '合格阈值',
    'description': '描述', 'status': '状态', 'docNo': '单号', 'docDate': '单据日期',
    'payAccountName': '付款账户', 'payAmount': '付款金额', 'receiptAccountName': '收款账户',
    'partnerCode': '往来单位编号', 'handlerName': '经手人', 'deptName': '部门', 'summary': '摘要',
    'creatorName': '制单人', 'totalQuantity': '数量合计', 'totalAmount': '金额合计', 'amount': '金额',
    'quantity': '数量', 'unitPrice': '单价', 'returnNo': '退货单号', 'orderNo': '单号',
    'taskNo': '任务单号', 'pickNo': '拣货单号', 'shipNo': '发货单号', 'moveNo': '移库单号',
    'borrowNo': '借用单号', 'billDate': '单据日期', 'expectedReturnDate': '预计归还日期',
    'direction': '方向', 'transferNo': '调拨单号', 'adjustNo': '调整单号', 'prePaymentNo': '预付单号',
    'preReceiptNo': '预收单号', 'warehouseName': '仓库', 'partnerName': '往来单位', 'remark': '备注',
}


def entity_fields(java_path: Path):
    """从 Java 源码里取字段名（private <Type> name;），保持声明顺序"""
    src = java_path.read_text(encoding='utf-8', errors='replace')
    fields = []
    for line in src.splitlines():
        m = re.match(r'\s*private\s+[\w<>\[\], .]+\s+(\w+)\s*;', line)
        if m and not line.strip().startswith('//'):
            fields.append(m.group(1))
    return fields


def page_labels(vue_path: Path):
    """从页面列定义里取 key → 中文标题（`:columns=` 指向的那个数组优先）"""
    src = vue_path.read_text(encoding='utf-8', errors='replace')
    m = re.search(r':columns="([A-Za-z_$][\w$]*)"', src)
    var = m.group(1) if m else 'columns'
    body = None
    mm = re.search(rf'^[ \t]*(?:const|let)\s+{re.escape(var)}\b', src, re.M)
    if mm:
        for c in re.finditer(r'\[', src[mm.end():]):
            i = mm.end() + c.start()
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
            cand = src[i:j + 1]
            if '{' in cand and 'title' in cand:
                body = cand
                break
    labels = {}
    for line in (body or '').splitlines():
        l = line.strip()
        if not l.startswith('{'):
            continue
        # 与前端 useListPrint.toPrintColumns 同规则：defaultHidden 的列不上纸
        #（单据表单的明细列有几十个可选项，全打上纸就是 43 列，纸面放不下）
        if 'defaultHidden' in l:
            continue
        key = (re.search(r"(?:key|field):\s*'([^']*)'", l) or [None, None])[1]
        title = (re.search(r"title:\s*'([^']*)'", l) or [None, None])[1]
        if key and title:
            labels[key] = title
    return labels


def pick_header(doc_fields, labels=None, cap=10):
    """
    挑单据头字段：先按 PREFERRED_HEADER 的偏好顺序，再用「页面上有中文标题的标量字段」补齐。

    只按偏好表挑会漏 —— 质量标准那种字段名（standardName / inspectionType / sampleRate）
    一个都不匹配，结果只挑出 1 个字段，模板几乎是空的。
    """
    out, used = [], set()
    for key, label in PREFERRED_HEADER:
        for f in doc_fields:
            if f in used or f in SKIP_ALWAYS or f.endswith('Id'):
                continue
            if f == key or (key == 'No' and f.endswith('No')) or (key == 'Date' and f.endswith('Date')):
                out.append((f, label))
                used.add(f)
                break
    # 补齐：页面给了中文标题的字段（说明它是要给人看的），且不是审计/扩展噪声
    NOISE = re.compile(r'(updateTime|createTime$|^footerExt|^ext(Num|Text)|By$|books?|bookkeeper)', re.I)
    for f, label in {**(labels or {}), **LABELS}.items() if False else list({**LABELS, **(labels or {})}.items()):
        if len(out) >= cap:
            break
        if f in used or f not in doc_fields or f in SKIP_ALWAYS or f.endswith('Id'):
            continue
        if NOISE.search(f):
            continue
        out.append((f, label))
        used.add(f)
    return out[:cap]


def money_field(item_fields):
    for pref in ('amount', 'lineAmount', 'totalAmount', 'billAmount', 'money'):
        if pref in item_fields:
            return pref
    for f in item_fields:
        if 'amount' in f.lower() or 'money' in f.lower():
            return f
    return None


def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    if len(args) < 3:
        print(__doc__)
        sys.exit(1)
    vue, page_code, doc_java = args[0], args[1], args[2]
    # 明细实体可省（`-` 或不给）—— 费用单/质量标准这类单表单据没有明细子表
    item_java = args[3] if len(args) > 3 and args[3] != '-' else None
    title = sys.argv[sys.argv.index('--title') + 1] if '--title' in sys.argv else page_code
    out_name = sys.argv[sys.argv.index('--out') + 1] if '--out' in sys.argv else f'{page_code}.json'

    doc_fields = entity_fields(Path(doc_java))
    item_fields = entity_fields(Path(item_java)) if item_java else []
    labels = page_labels(ROOT / vue)
    header = pick_header(doc_fields, labels)

    # ⚠️ 明细列按**页面列顺序**取（= 用户在那张表上看到的列），再与实体字段求交集 ——
    #    不要遍历实体字段，那样会把价格等级、大小包装这类没上表的字段一起打上纸
    #    （实测采购入库单会打出 43 列，纸面根本放不下）。
    seen, item_cols = set(), []
    ordered = list(labels.items()) + [(f, LABELS[f]) for f in item_fields if f in LABELS and f not in labels]
    for f, label in ordered:
        if f in seen or f not in item_fields or f in SKIP_ALWAYS or f.endswith('Id'):
            continue
        seen.add(f)
        col = {'header': label, 'field': f, 'width': '84px'}
        if re.search(r'quantity|qty', f, re.I):
            col.update({'align': 'right', 'agg': 'sum', 'digits': 2})
        elif re.search(r'price|amount|money', f, re.I):
            col.update({'align': 'right', 'digits': 2})
            if f in ('amount', 'lineAmount'):
                col['agg'] = 'sum'
        item_cols.append(col)
    # 商品名称列给宽一点
    for c in item_cols:
        if 'productName' in c['field'] or c['field'] in ('specification', 'productSpec'):
            c['width'] = 'auto'
            c.pop('align', None)

    # 合计挂**实体自带的合计字段**（装配器已经给了，是权威值）；没有才退回明细列求和
    def header_field(*names):
        for n in names:
            if n in doc_fields:
                return n
        return None

    qty_f = header_field('totalQuantity', 'preOrderQuantity', 'totalQty')
    amt_f = header_field('totalAmount', 'orderAmount', 'billAmount', 'totalLineAmount')
    if not qty_f:
        qty_f = next((c['field'] for c in item_cols if 'quantity' in c['field'].lower()), None)
    if not amt_f:
        amt_f = money_field(item_fields)
    summary = []
    if qty_f:
        summary.append({'type': 'field', 'label': '数量合计', 'field': qty_f, 'span': 3,
                        'formatConfig': {'type': 'number', 'decimals': 2, 'thousands': True}})
    if amt_f:
        summary.append({'type': 'field', 'label': '金额合计', 'field': amt_f, 'span': 3,
                        'formatConfig': {'type': 'number', 'decimals': 2, 'thousands': True}})
        summary.append({'type': 'field', 'label': '金额大写', 'field': amt_f, 'span': 6,
                        'formatConfig': {'type': 'rmbUpper'}})

    tpl = {
        'version': 2, 'freeLayout': False,
        'paperSize': 'A4', 'pageWidth': 210, 'pageHeight': 297,
        'margin': {'top': 10, 'right': 10, 'bottom': 12, 'left': 10},
        'sections': {
            'pageHeader': {'repeat': True, 'height': 16, 'items': [
                {'type': 'label', 'content': title,
                 'style': {'textAlign': 'center', 'fontSize': '18px', 'fontWeight': 'bold', 'lineHeight': '26px'}}]},
            'docHeader': {'columns': 12, 'items': [
                {'type': 'field', 'label': lbl, 'field': f,
                 **({'span': 4} if f.endswith('No') else {'span': 4}),
                 **({'formatConfig': {'type': 'date', 'pattern': 'yyyy-MM-dd'}} if f.endswith('Date') else {})}
                for f, lbl in header]},
            **({'items': {'field': 'items', 'repeatHeader': True, 'rowsPerPage': 0, 'rowHeight': 7,
                          'fillBlankRows': False, 'emptyWhenNoData': True, 'columns': item_cols}}
               if item_cols else {}),
            'summary': {'columns': 12, 'items': summary},
            'pageFooter': {'repeat': True, 'position': 'bottom', 'items': [
                {'type': 'label', 'content': '制单：____________    审核：____________    经办：____________',
                 'style': {'textAlign': 'left', 'fontSize': '12px'}},
                {'type': 'pageNumber', 'format': '第{0}页 / 共{1}页', 'style': {'fontSize': '11px'}}]},
        },
    }
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    out = OUT_DIR / out_name
    out.write_text(json.dumps(tpl, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
    print(f'{page_code}: 单据头 {len(header)} 项、明细 {len(item_cols)} 列、合计 {qty_f}/{amt_f} → {out}')
    print(f'   单据头：{", ".join(f"{f}({l})" for f, l in header)}')
    print(f'   明细列：{", ".join(c["field"] for c in item_cols)}')
    missing = [f for f in item_fields if f not in SKIP_ALWAYS and not f.endswith('Id')
               and f not in [c['field'] for c in item_cols]]
    if missing:
        print(f'   ⚠️ 明细里没给标题、未上纸的字段（需要就补到页面列标题或 LABELS）：{missing[:12]}')


if __name__ == '__main__':
    main()
