# -*- coding: utf-8 -*-
"""
打印模块接入检查（只读）。

打印这套东西曾是**前后端各修各的**，中间没有契约也没有检查，于是漂移出了三个 P0
（2026-09-26 发现）：
  ① 前端查模板发 `status: 1`（数字），后端列是 varchar 存 'PUBLISHED'
     → `where status='1'` 恒不命中，打印弹窗的模板下拉永远是空的；
  ② 前端 17 个权限码写成 `printing:*`，库里一个都没有（后端是 `print:*`）
     → `v-permission` 校验不到就移除元素，模板/链路/客户端/任务/设计器 5 个页面的
       编辑、发布、复制、删除、取消任务按钮**对所有人隐藏**（管理员也看不见）；
  ③ 4 处页面各自重抄了一遍「挑模板 → 取数 → 渲染」流水线，连带复制了 ① 的缺陷。

本脚本把这三类漂移变成可执行的检查，接入新页面时跑一次就知道漏了什么。

用法:
  python tools/check-print-wiring.py            # 全部检查
  python tools/check-print-wiring.py --codes    # 只看 pageCode 三方对齐
  python tools/check-print-wiring.py --perms    # 只看权限码
  python tools/check-print-wiring.py --apis     # 只看前端调用的打印接口是否存在

退出码: 0 = 全部通过；1 = 有需要人工处理的不一致
"""
import io
import json
import os
import re
import sys

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND = os.path.join(ROOT, 'backend')
FRONTEND_SRC = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src')
PRINTING_SRC = os.path.join(BACKEND, 'erp', 'erp-printing', 'src', 'main', 'java')

DSN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')
TENANT_ID = 1

issues = []


def note(ok, msg):
    if not ok:
        issues.append(msg)
    print(('  ✓ ' if ok else '  ✗ ') + msg)


def walk_files(root, exts, skip=('node_modules', 'target', 'dist', '.git')):
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in skip]
        for name in filenames:
            if name.endswith(exts):
                yield os.path.join(dirpath, name)


def read(path):
    with open(path, encoding='utf-8', errors='replace') as f:
        return f.read()


# ──────────────────────────────────────────────────────────────
# ① pageCode 三方对齐：后端装配器 / 前端调用 / 库里的模板
# ──────────────────────────────────────────────────────────────
def backend_page_codes():
    """
    静态扫出已注册的装配器，**并检出同一 pageCode 被两个类注册**。

    后者是要命的：`PrintDataProviderRegistry` 对重复 pageCode 是刻意 fail-fast
    （宁可起不来，也不要静默用错装配器）⇒ 真出现重复就是**整个应用启动失败**。
    多会话并行开发时两边同时给一个页面加装配器就会撞上，所以这里必须先报出来。

    识别的写法（找不到会打印文件供人工核对）：
      implements PrintDataProvider 的类里
        · PAGE_CODE = "xxx"            → 取该常量
        · pageCode() { return "xxx"; } → 取字面量
    """
    found = {}          # pageCode -> [文件…]（>1 即重复注册）
    unknown = []        # 认不出 pageCode 的实现
    # 扫整个 backend，不能只扫 erp/ —— 装配器按业务模块散落：
    # erp-sales / erp-purchase 在 erp/ 下，配送任务的在 dms/ 下（漏扫会得出"没注册"的错结论）
    for path in walk_files(BACKEND, ('.java',)):
        text = read(path)
        if 'implements PrintDataProvider' not in text:
            continue
        literals = re.findall(r'PAGE_CODE\s*=\s*"([^"]+)"', text)
        if not literals:
            literals = re.findall(r'pageCode\(\)\s*\{[^}]*return\s+"([^"]+)"', text, re.S)
        if not literals:
            literals = re.findall(r'return\s+"([a-z][a-z0-9\-]*)"\s*;', text)
        rel = os.path.relpath(path, ROOT)
        if len(literals) == 1:
            found.setdefault(literals[0], []).append(rel)
        else:
            unknown.append(rel)
    return found, unknown


def frontend_page_codes():
    """
    前端用到的 page-code。

    ⚠️ 必须区分四种写法，否则会误报：
      page-code="sale"            → 字面量，静态可判
      :page-code="expr"           → 动态绑定，**静态判不了**，单独列出来供人工看
      pageCode: 'sale'            → 模板查询的字面量
      pageCode: someVar           → 动态，跳过
    """
    static, dynamic = {}, {}
    for path in walk_files(FRONTEND_SRC, ('.vue', '.ts')):
        text = read(path)
        rel = os.path.relpath(path, FRONTEND_SRC)
        # 前置负向断言排除 `:page-code` / `v-bind:page-code` 这类绑定写法
        for code in re.findall(r'(?<![:\w-])page-code="([^"{]+)"', text):
            static.setdefault(code, set()).add(rel)
        for expr in re.findall(r'(?<![:\w-]):page-code="([^"]+)"', text):
            dynamic.setdefault(expr, set()).add(rel)
        for code in re.findall(r"pageCode:\s*'([^']+)'", text):
            static.setdefault(code, set()).add(rel)
    return static, dynamic


def db_page_codes():
    try:
        import psycopg2
    except ImportError:
        return None
    conn = psycopg2.connect(**DSN)
    cur = conn.cursor()
    cur.execute("SELECT page_code, count(*) FROM sys_print_template "
                "WHERE tenant_id=%s AND deleted=0 GROUP BY page_code ORDER BY page_code", (TENANT_ID,))
    rows = cur.fetchall()
    cur.close()
    conn.close()
    return dict(rows)


def check_codes():
    print('\n① pageCode 三方对齐（后端装配器 / 前端调用 / 库里的模板）')
    back_map, unknown = backend_page_codes()
    back = set(back_map)
    front, dynamic = frontend_page_codes()
    db = db_page_codes()

    for expr, files in sorted(dynamic.items()):
        print(f'  ⚠️ 动态绑定的 page-code 判不了，需人工确认: `:page-code="{expr}"` ← {sorted(files)}')

    print(f'  后端已注册装配器（{len(back)} 个）: {sorted(back) or "无"}')
    for rel in unknown:
        print(f'  ⚠️ 无法自动识别 pageCode，请人工核对: {rel}')

    # 重复注册 = 应用起不来（注册表刻意 fail-fast），必须先报
    dup = {code: files for code, files in back_map.items() if len(files) > 1}
    for code, files in sorted(dup.items()):
        note(False, f'pageCode「{code}」被 {len(files)} 个装配器同时注册 → '
                    f'应用启动会失败，必须只留一个: {files}')

    front_codes = sorted(front)
    print(f'  前端用到的 page-code（{len(front_codes)} 个）: {front_codes}')

    if db is None:
        print('  ⚠️ 没装 psycopg2，跳过库里的模板比对')
        return
    print(f'  库里有模板的 page_code: {sorted(db)}')

    # 前端在用、但后端没有装配器 → 只能走「页面自己给数据」的兼容路径（不是错误，但要知情）
    compat = [c for c in front_codes if c not in back]
    if compat:
        print(f'  · 走兼容路径（无后端装配器，需页面自己传 print-data）: {compat}')

    # 前端在用、后端也有装配器，但库里没有已发布模板 → 点了打印会报「还没有已发布的模板」
    missing = [c for c in front_codes if c in back and not db.get(c)]
    note(not missing,
         f'以下页面已注册装配器但库里没有模板，打印会报错: {missing}' if missing
         else '已注册装配器的页面都有模板')

    # 库里有模板但前端没用 → 可能是历史遗留
    orphan = [c for c in db if c not in front]
    if orphan:
        print(f'  · 库里有模板但前端未见引用（历史遗留？）: {orphan}')


# ──────────────────────────────────────────────────────────────
# ② 前端 v-permission 权限码 → 库 sys_permission
# ──────────────────────────────────────────────────────────────
def db_permissions():
    try:
        import psycopg2
    except ImportError:
        return None
    conn = psycopg2.connect(**DSN)
    cur = conn.cursor()
    cur.execute('SELECT permission_code FROM sys_permission WHERE deleted=0')
    codes = {r[0] for r in cur.fetchall()}
    cur.close()
    conn.close()
    return codes


def check_perms(scope_only=True):
    label = '打印模块' if scope_only else '全站'
    print(f'\n② {label}前端权限码 → 库 sys_permission')
    known = db_permissions()
    if known is None:
        print('  ⚠️ 没装 psycopg2，跳过')
        return

    root = os.path.join(FRONTEND_SRC, 'views', 'printing') if scope_only else FRONTEND_SRC
    used = {}
    for path in walk_files(root, ('.vue', '.ts')):
        text = read(path)
        rel = os.path.relpath(path, FRONTEND_SRC)
        for code in re.findall(r'v-permission(?:\.\w+)?="\'([^\']+)\'"', text):
            used.setdefault(code, set()).add(rel)

    if not used:
        print('  （未发现 v-permission 用法）')
        return

    bad = {c: f for c, f in used.items() if c not in known}
    print(f'  共 {len(used)} 个权限码，其中库里不存在 {len(bad)} 个')
    for code, files in sorted(bad.items()):
        # 猜一下最接近的（前缀 printing: → print:）
        guess = code.replace('printing:', 'print:', 1)
        hint = f'  可能想写: {guess}' if guess in known else ''
        note(False, f'{code} ← {sorted(files)}{hint}')
    if not bad:
        note(True, f'{label}用到的权限码在库里全部存在')


# ──────────────────────────────────────────────────────────────
# ③ 前端调用的打印接口 → 后端是否存在
# ──────────────────────────────────────────────────────────────
MAPPING_RE = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping(?:\(\s*(?:value\s*=\s*)?"([^"]*)"[^)]*\))?')


def backend_print_endpoints():
    endpoints = set()
    for path in walk_files(PRINTING_SRC, ('.java',)):
        text = read(path)
        cls = re.search(r'@RequestMapping\(\s*(?:value\s*=\s*)?"([^"]+)"', text)
        if not cls:
            continue
        prefix = cls.group(1)
        for verb, sub in MAPPING_RE.findall(text):
            sub = (sub or '').strip()
            full = (prefix.rstrip('/') + '/' + sub.lstrip('/')).rstrip('/') or prefix
            endpoints.add((verb.upper(), full))
    return endpoints


def check_apis():
    print('\n③ 前端 printingApi 调用的路径 → 后端端点')
    endpoints = backend_print_endpoints()
    api_file = os.path.join(FRONTEND_SRC, 'api', 'printing', 'index.ts')
    text = read(api_file)
    prefix_match = re.search(r"const PREFIX\s*=\s*'([^']+)'", text)
    prefix = prefix_match.group(1) if prefix_match else ''
    # 后端端点里把 /api 前缀去掉再比（前端的 PREFIX 是 '/v2/print'）
    back_paths = {p for _, p in endpoints}
    back_norm = {re.sub(r'^/api', '', p) for p in back_paths}
    template_norm = {re.sub(r'\{[^}]+\}', '{}', p) for p in back_norm}

    used = re.findall(r"request\.(get|post|put|delete)\(\s*`\$\{PREFIX\}([^`]*)`", text)
    if not used:
        print('  （未解析到调用）')
        return
    miss = []
    for verb, tail in used:
        path = prefix + tail
        path = re.sub(r'\$\{[^}]*\}', '{}', path)
        norm = re.sub(r'\{[^}]+\}', '{}', re.sub(r'^/api', '', path))
        if norm not in template_norm:
            miss.append(f'{verb.upper()} {path}')
    print(f'  解析到 {len(used)} 处调用，后端找不到的 {len(miss)} 处')
    for m in miss:
        note(False, m)
    if not miss:
        note(True, 'printingApi 调用的路径后端都存在')


# ──────────────────────────────────────────────────────────────
# ④ 模板引用的字段 vs 装配器产出的键
# ──────────────────────────────────────────────────────────────
TEMPLATE_DIR = os.path.join(ROOT, 'tool-results', 'print-template-v2')


def template_field_refs(tpl):
    """
    模板里所有 field 引用：sections.*.items[].field 与 items.columns[].field。

    ⚠️ `columns` 在 docHeader/summary 里是**栅格列数（整数）**，
    在 items 里才是**列定义数组** —— 两者别当同一种东西用。
    """
    out = set()
    for sec in (tpl.get('sections') or {}).values():
        for item in (sec.get('items') or []):
            if item.get('type') == 'field' and item.get('field'):
                out.add(item['field'])
        columns = sec.get('columns')
        if isinstance(columns, list):
            for col in columns:
                if isinstance(col, dict) and col.get('field'):
                    out.add(col['field'])
    return out


def provider_keys(path):
    """
    静态抽出装配器产出的键（data.put / row.put 的字面量）。

    这是**约定式解析**：装配器都按 `data.put("键", …)` / `row.put("键", …)` 写。
    它不保证 100% 准确（认不出就少报，不会误报），但足以抓住
    「模板引用了一个装配器根本不给的字段」这类最常见的填空事故。
    """
    top, row = set(), set()
    in_row_method = False
    for line in read(path).splitlines():
        if 'Map<String, Object> itemRow' in line:
            in_row_method = True
        elif in_row_method and line.strip().startswith('private '):
            in_row_method = False
        m = re.search(r'\b(data|row)\.put\(\s*"([^"]+)"', line)
        if not m:
            continue
        (row if m.group(1) == 'row' or in_row_method else top).add(m.group(2))
    return top, row


def check_fields():
    print('\n④ 模板引用的字段 ↔ 装配器产出的键')
    providers, unknown = backend_page_codes()
    if not os.path.isdir(TEMPLATE_DIR):
        print(f'  （没有 {os.path.relpath(TEMPLATE_DIR, ROOT)} 模板目录，跳过）')
        return

    checked = 0
    for name in sorted(os.listdir(TEMPLATE_DIR)):
        # `_` 前缀是本目录的约定：样例数据 / 通用母版之类的辅助文件，不是某个页面的模板
        if not name.endswith('.json') or name.startswith('_'):
            continue
        page_code = name[:-5]
        rels = providers.get(page_code)
        if not rels:
            print(f'  · {page_code}: 没有对应装配器（结果集打印？），跳过')
            continue
        tpl_path = os.path.join(TEMPLATE_DIR, name)
        try:
            tpl = json.load(open(tpl_path, encoding='utf-8'))
        except Exception as e:
            note(False, f'{name} 不是合法 JSON: {e}')
            continue

        refs = template_field_refs(tpl)
        top, row = provider_keys(os.path.join(ROOT, rels[0]))
        if not top and not row:
            # 委托型装配器（自己不产出键，转调另一个装配器，见 PurchaseReceivePrintDataProvider）：
            # 静态看不出来，跳过而不是误报一片「字段缺失」
            print(f'  · {page_code}: 装配器是委托实现（{rels[0]}），无法静态核对字段，跳过')
            continue
        # 结果集字段：页面模板里凡是明细列引用，都应由 itemRow 给；其余按单据头算
        item_refs = set()
        items_sec = (tpl.get('sections') or {}).get('items') or {}
        for col in (items_sec.get('columns') or []):
            if col.get('field'):
                item_refs.add(col['field'])
        head_refs = refs - item_refs

        missing_head = sorted(head_refs - top - row)
        missing_item = sorted(item_refs - row - top)
        checked += 1
        if missing_head or missing_item:
            detail = []
            if missing_head:
                detail.append(f'单据头缺: {missing_head}')
            if missing_item:
                detail.append(f'明细缺: {missing_item}')
            note(False, f'{page_code}: 模板引用了装配器不给的字段 → ' + '；'.join(detail)
                        + f'（装配器 {rels[0]}）')
        else:
            note(True, f'{page_code}: 模板 {len(refs)} 个字段装配器都给了')
    if not checked:
        print('  （没有可校验的 pageCode）')


def main():
    argv = sys.argv[1:]
    only = [a for a in argv if a.startswith('--')]
    print('打印模块接入检查（只读）')
    if not only or '--codes' in only:
        check_codes()
    if not only or '--perms' in only:
        check_perms()
    if not only or '--apis' in only:
        check_apis()
    if not only or '--fields' in only:
        check_fields()
    print()
    if issues:
        print(f'✗ 有 {len(issues)} 项需要处理')
        sys.exit(1)
    print('✓ 全部通过')


if __name__ == '__main__':
    main()
