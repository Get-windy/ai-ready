# -*- coding: utf-8 -*-
"""
ql361 打印模板 → 本系统 sys_print_template.template_json 转换器

两端格式（均已实测）：

ql361（GetPrintCfgs 的 data.result[].template）：
  { papertype, pageWidth, pageHeight, billtype, reportTitle,
    pageHeaderItems:[...], pageFooterItems:[...],   # 绝对定位元素（有 style.top/left/width/height）
    headerItems:[...], footerItems:[...] }          # 列定义（colname/title/width，无坐标）
  元素形态：
    { type:"Text"|"LabelText", unique_id, title, dataField, visible, colspanline, width,
      style:{ top, left, width, height, "font-size":"10px", font:"宋体",
              "text-align":"center", "font-weight":"bold" } }
    · type=Text       → dataField 是**字面文本**（如 "皇朝|干饭郎 销售订单"）
    · type=LabelText  → dataField 是**字段占位**（如 "{dfullname}"），title 是显示名

本系统（FormatEngineImpl.renderToHtml 解析 template_json）：
  { "components": [
      { "type":"label", "content":"…", "x":…, "y":…, "w":…, "h":…, "style":{…} },
      { "type":"field", "field":"…",  "x":…, "y":…, "w":…, "h":…, "formatConfig":{…} },
      { "type":"table", "field":"items", "columns":[ {"header":"商品名称","field":"productName","width":"120px"} ] },
      { "type":"line" | "barcode" | "qrcode" | "image", … } ] }
  x/y/w/h 由组件自带的 x,y,w,h 决定（renderComponent 里读的就是它们）。

用法：
  python tools/ql361-print-template-convert.py <ql361模板JSON> <模板id> [输出JSON]
    例：python tools/ql361-print-template-convert.py \
          tool-results/ql361/print-template/销售出库单/_模板_销售订单604.json 28237
"""
import io
import json
import re
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

# ── 字段名映射：ql361 字段 → 本系统 printData 里的字段 ──
# ⚠️ 未列入的字段会**原样保留**并在报告里标出来，便于逐页补齐；不要瞎猜成不存在的字段。
FIELD_MAP = {
    # ── 单据头：映射依据 = 各页面打印时传给 PrintDialog 的 printData ──
    # 销售订单页（order-center）传的是 saleOrderApi.getById() 的返回，即 SaleOrder 实体，
    # 故此处按后端 SaleOrder 的字段名对齐（orderNo/orderDate/customerName/shippingAddress/…）。
    'billcode': 'orderNo', 'billdate': 'orderDate', 'dfullname': 'customerName',
    'addr': 'shippingAddress', 'contactor': 'contactName', 'phone': 'contactPhone',
    'soutfullname': 'warehouseName', 'cfullname': 'customerCode',
    'billmoney': 'billAmount', 'customerremark': 'customerRemark', 'remark': 'remark',
    'dcode': 'customerCode', 'daddr': 'shippingAddress', 'dphone': 'receiverPhone',
    'dlinkman': 'receiverName', 'handler': 'salesmanName', 'deptname': 'deptName',
    'uptaxedmoney': 'totalAmountUpper', 'taxedmoney': 'billAmount',
    'totalmoney': 'billAmount', 'discountmoney': 'discountAmount',
    # ⚠️ 未映射（保持原名，不瞎猜）：
    #   creatorfullname / oldarap / newarap / user_defined_* —— SaleOrder 实体里没有
    #   直接对应字段（制单人、此前欠款、欠款余额），需产品确认取数口径后再补。
}
# 明细列（bodyItem.cols 的 colname → 明细行字段，对齐后端 SaleOrderItem）
ITEM_FIELD_MAP = {
    'rownum': 'lineNo', 'gfullname': 'productName', 'productname': 'productName',
    'productcode': 'productCode', 'spec': 'specification', 'mufullname': 'unit',
    'unitname': 'unit', 'mprice': 'unitPrice', 'price': 'unitPrice',
    'qty': 'quantity', 'mqty': 'quantity', 'realqty': 'quantity',
    'shelflife': 'shelfLife', 'batchsinput': 'batchCode', 'batchno': 'batchCode',
    'totalmoney': 'lineAmount', 'taxedmoney': 'lineAmount', 'remark': 'remark',
    'discountmoney': 'discountAmount', 'packagename': 'packageName',
}


def px(v, fallback=0.0):
    """ql361 的尺寸可能是数字或 "10px" 字符串 → 统一成数字"""
    if v is None:
        return fallback
    if isinstance(v, (int, float)):
        return float(v)
    m = re.match(r'-?\d+(\.\d+)?', str(v))
    return float(m.group(0)) if m else fallback


def style_of(el):
    """ql361 style → 本系统 style（只保留渲染用得到的）"""
    s = el.get('style') or {}
    out = {}
    if s.get('font-size'):
        out['fontSize'] = s['font-size']
    if s.get('font-weight'):
        out['fontWeight'] = s['font-weight']
    if s.get('text-align'):
        out['textAlign'] = s['text-align']
    if s.get('font'):
        out['fontFamily'] = s['font']
    return out


def place(el):
    """取绝对定位坐标；ql361 在 style 里放 top/left/width/height"""
    s = el.get('style') or {}
    return (px(s.get('left')), px(s.get('top')), px(s.get('width')), px(s.get('height'), 20))


def is_field(el):
    """LabelText 或 dataField 形如 {xxx} 的都算字段绑定"""
    if el.get('type') == 'LabelText':
        return True
    return bool(re.match(r'^\{[^}]+\}$', str(el.get('dataField') or '').strip()))


def field_name(el):
    raw = str(el.get('dataField') or '').strip()
    m = re.match(r'^\{([^}]+)\}$', raw)
    return m.group(1).strip() if m else (el.get('unique_id') or '')


def convert_el(el, mapper, prefix=''):
    """单个绝对定位元素 → 本系统 component（不可见/无坐标的返回 None）"""
    if el.get('visible') is False:
        return None
    x, y, w, h = place(el)
    st = style_of(el)
    if is_field(el):
        f = field_name(el)
        return {
            'type': 'field',
            'field': mapper.get(f, f),          # 映射不到就原样保留
            '_ql361Field': f,                    # 留痕，便于核对映射
            'x': x, 'y': y, 'w': w, 'h': h,
            **({'style': st} if st else {}),
        }
    text = str(el.get('dataField') or el.get('title') or '').strip()
    if not text:
        return None
    return {'type': 'label', 'content': text, 'x': x, 'y': y, 'w': w, 'h': h,
            **({'style': st} if st else {})}


def build_table(template):
    """
    明细表 ← template.bodyItem（**不是 headerItems**！）
      bodyItem = { dataField:"grid", type:"Table", printable:true,
                   cols:[ { colname:"rownum", title:"行号", value:"#rownum", visible, width, ... } × 98 ] }
    """
    body = template.get('bodyItem') or {}
    cols_src = body.get('cols') or []
    cols = []
    for it in cols_src:
        if it.get('visible') is False:
            continue
        col = it.get('colname') or ''
        if not col:
            continue
        w = px(it.get('width'), 0)
        c = {'header': it.get('title') or col,
             'field': ITEM_FIELD_MAP.get(col, col),
             '_ql361Field': col}
        # value 里是取值表达式（#rownum / {xxx}），没有 colname 对应字段时留痕
        if it.get('value') and str(it['value']) != f'{{{col}}}':
            c['_ql361Value'] = str(it['value'])[:40]
        if w:
            c['width'] = f'{int(w)}px'
        cols.append(c)
    if not cols:
        return None
    return {'type': 'table', 'field': 'items', 'columns': cols, 'x': 0, 'y': 0, 'w': 0, 'h': 0}


def convert(template, billtype=None):
    comps = []
    unmapped = set()

    # 页头页脚 + 单据头字段 + 表尾字段：都是"元素"（pageHeader/pageFooter 有坐标，
    # headerItems/footerItems 是**单据头/表尾的字段区**，同样按 visible 取）
    for grp in ('pageHeaderItems', 'pageFooterItems', 'headerItems', 'footerItems'):
        for el in template.get(grp) or []:
            c = convert_el(el, FIELD_MAP)
            if c:
                f = c.get('_ql361Field')
                if f and FIELD_MAP.get(f) is None:
                    unmapped.add(f)
                comps.append(c)
    # reportTitle（顶层单元素）
    rt = template.get('reportTitle')
    if isinstance(rt, dict):
        c = convert_el(rt, FIELD_MAP)
        if c:
            comps.append(c)

    # 明细表（在 bodyItem.cols，不在 headerItems）
    tbl = build_table(template)
    if tbl:
        # 放在"最后一个有坐标元素"的下方，避免与页头重叠
        ymax = max([c.get('y', 0) + c.get('h', 0) for c in comps] or [0])
        tbl['y'] = ymax + 10
        tbl['x'] = px((template.get('uiBodyStyle') or {}).get('left'), 0)
        comps.append(tbl)
        for col in tbl['columns']:
            if ITEM_FIELD_MAP.get(col.get('_ql361Field')) is None:
                unmapped.add(col.get('_ql361Field'))

    # 按 y 再按 x 排序，渲染顺序稳定
    comps.sort(key=lambda c: (c.get('y', 0), c.get('x', 0)))
    return ({
        'source': 'ql361',
        'billtype': billtype or template.get('billtype'),
        'pageWidth': template.get('pageWidth'),
        'pageHeight': template.get('pageHeight'),
        'papertype': template.get('papertype'),
        'components': comps,
    }, unmapped)


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(1)
    src, tid = sys.argv[1], sys.argv[2]
    out_path = sys.argv[3] if len(sys.argv) > 3 else None
    data = json.load(open(src, encoding='utf-8'))
    arr = data if isinstance(data, list) else data.get('data', {}).get('result', [])
    tpl = next((x for x in arr if str(x.get('id')) == str(tid)), None)
    if not tpl:
        print(f'✗ 没找到模板 id={tid}；可选：' + ', '.join(f"{x['id']}({x.get('fullname')})" for x in arr))
        sys.exit(2)
    result, unmapped = convert(tpl.get('template') or {}, tpl.get('billtype'))
    # 包成 sys_print_template.template_json 的形状
    payload = {
        'pageCode': None,                      # 由落库脚本按页面填
        'templateName': tpl.get('fullname'),
        'migratedFrom': {'source': 'ql361', 'templateId': tpl.get('id'), 'billtype': tpl.get('billtype')},
        **result,
    }
    txt = json.dumps(payload, ensure_ascii=False, indent=1)
    if out_path:
        open(out_path, 'w', encoding='utf-8').write(txt)
        print(f'✓ 已写出 {out_path}')
    print(f"模板「{tpl.get('fullname')}」(id={tpl.get('id')}, billtype={tpl.get('billtype')})")
    print(f"  components: {len(result['components'])} 个 "
          f"（label {sum(1 for c in result['components'] if c['type']=='label')} / "
          f"field {sum(1 for c in result['components'] if c['type']=='field')} / "
          f"table {sum(1 for c in result['components'] if c['type']=='table')}）")
    tb = next((c for c in result['components'] if c['type'] == 'table'), None)
    if tb:
        print(f"  明细列: {len(tb['columns'])} 列 → " + ', '.join(c['header'] for c in tb['columns'][:10]) + ' …')
    if unmapped:
        print(f"  ⚠️ 未映射字段 {len(unmapped)} 个（已原样保留，需逐页补 FIELD_MAP/ITEM_FIELD_MAP）：")
        print('     ' + ', '.join(sorted(unmapped)[:40]))
    if not out_path:
        print('--- 预览（前 1200 字符）---')
        print(txt[:1200])


if __name__ == '__main__':
    main()
