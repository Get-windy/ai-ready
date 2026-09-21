# -*- coding: utf-8 -*-
"""
为「零权限码模块」生成权限种子迁移 + 端点→权限码对照表（只读扫描，不执行、不连库）。

## 背景（E-04）

E-04 实测：`sys_permission` 500 条里 `erp:*` 只占 31 条，而
**stock / wms / marketing / b2b / payment / party / budget / invoice / fixedasset
九个模块的域前缀在权限码库里完全不存在**（或只有个位数）。
没有码，就**不能**给这些控制器补 `@SaCheckPermission` ——
注解引用库中不存在的码会让该接口对**所有非超管一律 403**（本仓历史事故，见
记忆 `permission-code-missing-lockout`）。

⚠️ **反向纪律**：只补码不补注解 = 凭空造出几百条「僵尸码」（能在矩阵里勾选、勾了不生效），
正好加重 E-02。所以本脚本产出的码**必须**与 E-01 的注解改动同批次落地。

## 口径

1. **粒度**对齐既有约定：一码 = 一「(资源, 动作)」，不是一码一端点。
   实测 `PurchaseOrderController` 16 个端点 → 8 个码（`purchase:order:{list,detail,
   create,update,delete,submit,approve,cancel}`）。
2. **命名**对齐同域既有风格 `<域>:<资源>:<动作>`。
   动作词优先复用库中已有的（`create/update/delete/list/detail/view/approve/submit/export/...`），
   不新造同义词（既有的 `view`↔`detail`、`edit`↔`update` 两套并存是历史包袱，不再扩大）。
3. **资源名**取类级 `@RequestMapping` 去掉模块前缀后的剩余段（多段用 `-` 连接）。

## 用法

    python tools/gen-module-permission-seed.py budget            # 打印对照表
    python tools/gen-module-permission-seed.py budget --sql      # 打印迁移 SQL
    python tools/gen-module-permission-seed.py --list            # 列出已配置模块

新增模块：在 `MODULES` 里加一项（`strip` 填类级路径的公共前缀）。
"""
import os
import re
import sys
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# ── id 号段（2026-09-21 实测空闲；禁止与既有种子复用）──
#   权限码：每模块一个 1000 槽，起点 100000 + slot*1000（9xxxx 段已被 V11.42x 短块占满）
#   角色关联：9500000 + slot*10000（9400000 段被 V11.439.0 占用）
#   ⚠️ 各模块必须用不同槽位，否则多个迁移互相撞主键（本仓 sys_permission.id 无序列默认值）
PERM_SLOT_BASE = 100000
RP_SLOT_BASE = 9500000
SORT_BASE = 1200

# 【域如何确定】不按「模块目录」而按**类级路由路径**推导 ——
# 实测这本仓两者并不对应：`erp-stock` 目录里装的是**商品/主数据**控制器
# （`/api/erp/product/*`、`/api/erp/md/*`），`wms` 里混着 `/api/v1/warehouse/*`，
# `erp-mall` 里既有后台 `/api/erp/mall/admin/*` 也有 C 端 `/api/v1/mall/*`。
# 按目录指定前缀会把大量控制器判成「资源=完整路径」，产出 `api-erp-product-xxx` 这类垃圾码。
#
# 规则：去掉 `/api`、跳过 `erp` 或 `v1` 这类容器段后，**第一段即域**，其余段用 `-` 连成资源；
# 资源为空则省略该段（产出两段码 `<域>:<动作>`，库中已有先例：`purchase:manage`、`crm:create`）。
# 值一律是 (域, 附加资源名)：附加资源名用于把「第一段里已经含了资源信息」的路径
# 还原成正确的域+资源，例如 `mall-tag` 应为 `mall:tag:*` 而不是 `mall-tag:*`。
DOMAIN_ALIAS = {
    'warehouse': ('wms', None),          # /api/v1/warehouse/* 属仓储域
    'capital-flow': ('finance', 'capital-flow'),
    'receipt': ('finance', 'receipt'), 'pre-receipt': ('finance', 'pre-receipt'),
    'pre-payment': ('finance', 'pre-payment'), 'offset': ('finance', 'offset'),
    'write-off': ('finance', 'write-off'), 'deposit-condition': ('finance', 'deposit-condition'),
    'inventory-mode': ('stock', 'inventory-mode'),
    'stock-alert': ('stock', 'alert'), 'stock-alert-config': ('stock', 'alert-config'),
    'mall-tag': ('mall', 'tag'),
    'product-unit-dict': ('product', 'unit-dict'),
    'product-unit-group': ('product', 'unit-group'),
    'customer': ('party', 'customer'), 'contact': ('party', 'contact'),
    'member-level': ('party', 'member-level'),
    'company-subject': ('system', 'company-subject'),
    # 往来单位：库里历史码用的是 `partner:merge`，但代码/表/包的主词汇是
    # `biz_party` / `PartyController` / `cn.aiedge.erp.party`。
    # 两者本是同一业务对象被拆到两个域 ⇒ 统一到 `party`（`partner:merge` 已随本轮改名）。
    'partner': ('party', None),
}

MODULES = {
    'budget': {
        'label': '预算',
        'slot': 0,
        'domain': 'budget',
        'dirs': ['backend/erp/erp-budget/src/main/java/cn/aiedge/erp/budget/controller'],
    },
    'fixedasset': {
        'label': '固定资产',
        'slot': 1,
        'domain': 'fixedasset',
        'dirs': ['backend/erp/erp-fixed-asset/src/main/java/cn/aiedge/erp/fixedasset/controller'],
    },
    'stock': {
        'label': '库存/商品',
        'slot': 2,
        'domain': 'stock',
        'dirs': ['backend/erp/erp-stock/src/main/java/cn/aiedge/erp/stock/controller'],
    },
    'marketing': {
        'label': '营销',
        'slot': 3,
        'domain': 'marketing',
        'dirs': ['backend/erp/erp-marketing/src/main/java/cn/aiedge/erp/marketing/controller'],
    },
    'party': {
        'label': '往来单位',
        'slot': 4,
        'domain': 'party',
        'dirs': ['backend/erp/erp-partner/src/main/java/cn/aiedge/erp/party/controller'],
    },
    'invoice': {
        'label': '发票',
        'slot': 5,
        'domain': 'invoice',
        'dirs': ['backend/erp/erp-finance/src/main/java/cn/aiedge/erp/invoice/controller'],
    },
    'payment': {
        'label': '财务收付',
        'slot': 6,
        'domain': 'payment',
        'dirs': ['backend/erp/erp-finance/src/main/java/cn/aiedge/erp/payment/controller'],
    },
    # 以下两个模块**暂不纳入自动补注解**（见文件头 E-01 批次说明）：
    #   wms —— PDA 设备鉴权策略未定（PDA 是设备端，不能用员工账号的口令模型直接套）；
    #   b2b —— `/api/v1/mall/**` 是 C 端公开接口，自动补注解会把商城顾客挡在门外。
    # 它们的路径规则保留在此，仅供 `--review` 出清单，不参与 --apply。
    'wms': {
        'label': '仓储',
        'slot': 7,
        'domain': 'wms',
        'manual_only': True,
        'dirs': ['backend/wms/src/main/java/cn/aiedge/wms/controller'],
    },
    'b2b': {
        'label': '商城',
        'slot': 8,
        'domain': 'mall',
        'manual_only': True,
        'dirs': ['backend/erp/erp-mall/src/main/java/cn/aiedge/erp/b2b/controller'],
    },

    # ── 2026-09-21 新增：E-02 僵尸码 A 类（"有端点但没注解"）与 E-03 缺 api_path 的码
    #    集中的模块。这七个模块的码**早已在库**（不是零码模块），所以它们的正确用法是
    #      `--apply` 把注解补到端点上 + 用推导结果回填 `api_path`，
    #    **不是**再生成一份种子迁移（那会造出重复码）。
    #    dirs 给到模块根即可 —— scan/apply 已改为递归。
    'hr': {
        'label': '人力资源',
        'slot': 9,
        'domain': 'hr',
        'dirs': ['backend/hr/hr-base/src/main/java/cn/aiedge'],
    },
    'crm': {
        'label': '客户关系',
        'slot': 10,
        'domain': 'crm',
        'dirs': ['backend/crm/src/main/java/cn/aiedge'],
    },
    # core-api 一个根就覆盖 system / user / role / permission / department / position /
    # tenant / platform / datasource / scheduler / docquery 等域（域由**类级路径**推导，与目录无关）
    'coreapi': {
        'label': '平台与系统',
        'slot': 11,
        'domain': 'system',
        'dirs': ['backend/core/api/core-api/src/main/java/cn/aiedge'],
    },
    'erpfinance': {
        'label': '财务域其余',
        'slot': 12,
        'domain': 'finance',
        'dirs': ['backend/erp/erp-finance/src/main/java/cn/aiedge'],
    },
    'dms': {
        'label': '配送',
        'slot': 13,
        'domain': 'dms',
        'dirs': ['backend/dms/src/main/java/cn/aiedge'],
    },
    'sales': {
        'label': '销售',
        'slot': 14,
        'domain': 'sale',
        'dirs': ['backend/erp/erp-sales/src/main/java/cn/aiedge'],
    },
    'purchase': {
        'label': '采购',
        'slot': 15,
        'domain': 'purchase',
        'dirs': ['backend/erp/erp-purchase/src/main/java/cn/aiedge'],
    },
}

# 端点 → 动作。**有序**，首个匹配生效。
# 只使用库中已有的动作词；不新造同义词。
RULES = [
    # ── GET ──
    ('GET',    r'.*/(export|download)(/.*)?$', 'export'),
    # 注意要含 `list-by-*`（如 list-by-budget/{id}、list-by-year）——
    # 只写 `list` 会漏掉它们，落到下面的 `detail`/`view` 兜底规则上。
    ('GET',    r'.*/(page|list|list-by-[^/]*|rows|items|logs|warnings|next-no|doc-query|options|tree)(/.*)?$', 'list'),
    ('GET',    r'.*/\{[^/}]+\}$',              'detail'),
    ('GET',    r'.*',                          'view'),      # 兜底：报表/汇总/统计类读接口
    # ── POST ──
    ('POST',   r'.*/batch-delete$',            'delete'),
    ('POST',   r'.*/batch-submit$',            'submit'),
    ('POST',   r'.*/batch-(approve|reject)$',  'approve'),
    ('POST',   r'.*/(approve|reject|audit)$',  'approve'),
    ('POST',   r'.*/submit$',                  'submit'),
    ('POST',   r'.*/(start-exec|execute|run)$', 'execute'),
    ('POST',   r'.*/close$',                   'close'),
    ('POST',   r'.*/print$',                   'print'),
    ('POST',   r'.*/publish$',                 'publish'),
    ('POST',   r'.*/check$',                   'check'),
    ('POST',   r'.*/freeze$',                  'freeze'),
    ('POST',   r'.*/release$',                 'release'),
    ('POST',   r'.*/consume$',                 'consume'),
    ('POST',   r'.*/import$',                  'import'),
    ('POST',   r'.*',                          'create'),    # 兜底：新建/保存
    # ── PUT / PATCH / DELETE ──
    ('PUT',    r'.*',                          'update'),
    ('PATCH',  r'.*',                          'update'),
    ('DELETE', r'.*',                          'delete'),
]

# 中文名后缀，用于 sys_permission.permission_name（矩阵里显示的就是它）
ACTION_LABEL = {
    'list': '查询', 'detail': '详情', 'view': '查看', 'create': '新增',
    'update': '编辑', 'delete': '删除', 'submit': '提交', 'approve': '审批',
    'export': '导出', 'import': '导入', 'execute': '执行', 'close': '关闭',
    'print': '打印', 'publish': '发布', 'check': '校验', 'freeze': '冻结',
    'release': '解冻', 'consume': '占用', 'cancel': '取消',
}

# ⚠️ 资源标签**不要**再带模块名（模块名由 `MODULES[..]['label']` 提供），
#    否则会拼出「预算预算调整审批」这种重复。
RESOURCE_LABEL = {
    'annual': '年度', 'adjustment': '调整', 'control': '控制',
    'execution': '执行', 'item': '明细', 'report': '报表',
    'template': '模板',
}

CLS_RM = re.compile(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']')
MAPPING = re.compile(
    r'@(Get|Post|Put|Delete|Patch)Mapping\s*(?:\(\s*(?:value\s*=\s*)?(?:["\']([^"\']*)["\'])?[^)]*\))?')
REST = re.compile(r'@(RestController|Controller)\b')


def iter_controllers(dirs):
    """递归产出 dirs 下所有「控制器」Java 文件的绝对路径。

    ⚠️ 原实现用 `os.listdir`（**只扫一层**），于是「控制器按子包分目录」的模块整片漏掉：
    `crm/contract/controller`、`erp-finance/expensedoc/controller`、
    `erp-stock/controller/initial` 在旧实现下**一个端点都扫不到**。
    改为递归后，MODULES 里每个模块只需给一个**模块根目录**。
    """
    for d in dirs:
        full = os.path.join(ROOT, d)
        if not os.path.isdir(full):
            continue
        for dirpath, dirnames, filenames in os.walk(full):
            dirnames[:] = [x for x in dirnames if x != 'target']
            if os.sep + 'src' + os.sep + 'test' in dirpath:
                continue
            for name in sorted(filenames):
                if name.endswith('.java'):
                    yield os.path.join(dirpath, name)


def scan(dirs):
    """返回 [(controller, resource, verb, path, endpoint)]，按扫描顺序。"""
    rows = []
    for fp in iter_controllers(dirs):
        text = open(fp, encoding='utf-8').read()
        if not REST.search(text):
            continue
        m = CLS_RM.search(text)
        if not m:
            continue
        base = m.group(1).rstrip('/')
        for verb, path in MAPPING.findall(text):
            verb = verb.upper()
            path = (path or '').strip()
            rows.append({
                'file': os.path.basename(fp), 'base': base, 'verb': verb, 'path': path,
                'endpoint': f'{verb} {base}{path}',
            })
    return rows


# 路径级覆盖：**整条基路径**决定域+资源，优先于一切段级规则。
# 用于消解「同名不同物」——最简单的段级规则解决不了这类冲突。
# 例：`payment` 这个域在库里**历史上指第三方支付网关**（`/api/payment/*`、`/refund`、
# `/reconciliation`，核心支付模块）；而 `/api/erp/payment` 是 **ERP 付款单**（财务域）。
# 两者共用一个域会让权限矩阵出现「支付-查询」这种指代不明的条目 —— 故后者强制归 finance。
PATH_OVERRIDE = {
    '/api/erp/payment': ('finance', 'payment'),
}

# 已知域：用于把 `product-category`、`stock-in` 这类**域+资源连写**的第一段拆回「域, 资源」，
# 否则会产出一堆 `product-category:*`、`warehouse-category:*` 这种一级域，
# 与 `/api/erp/product/units → product:units:*` 自相矛盾。
KNOWN_DOMAINS = {
    'stock', 'product', 'md', 'marketing', 'mall', 'party', 'partner', 'invoice',
    'payment', 'finance', 'purchase', 'sale', 'system', 'crm', 'hr', 'dms', 'wms',
    'budget', 'fixed-asset', 'doc', 'log', 'workflow', 'tenant', 'user', 'role',
    'permission', 'datasource', 'platform', 'set', 'position', 'department',
    'receipt', 'print', 'erp',
}


def split_head(head):
    """把路径首段拆成 (域, 附加资源名)。"""
    if head in DOMAIN_ALIAS:
        return DOMAIN_ALIAS[head]
    if '-' in head:
        pre, rest = head.split('-', 1)
        if pre in DOMAIN_ALIAS:
            d, e = DOMAIN_ALIAS[pre]
            return d, (f'{e}-{rest}' if e else rest)
        if pre in KNOWN_DOMAINS:
            return pre, rest
    return head, None


def domain_resource_of(base):
    """由类级路由路径推出 (域, 资源)。资源为空时返回 None（产出两段码）。"""
    for prefix, (dom, res) in PATH_OVERRIDE.items():
        if base == prefix or base.startswith(prefix + '/'):
            tail = base[len(prefix):].lstrip('/')
            parts = ([res] if res else []) + ([tail] if tail else [])
            return dom, ('-'.join(parts) if parts else None)
    segs = [s for s in base.strip('/').split('/') if s]
    if segs and segs[0] == 'api':
        segs = segs[1:]
    # 跳过容器段：/api/erp/... 与 /api/v1/... 里的 erp / v1
    # ⚠️ 不能跳 'admin'：`/api/erp/mall/admin/*` 里 admin 是**资源段**
    if segs and segs[0] in ('erp', 'v1', 'v2'):
        segs = segs[1:]
    if not segs:
        return 'misc', None
    dom, extra = split_head(segs[0])
    rest = segs[1:]
    parts = ([extra] if extra else []) + rest
    return dom, ('-'.join(parts) if parts else None)


def resource_of(base):
    """仅返回资源部分（无资源时返回域自身，避免 NPE）。"""
    dom, res = domain_resource_of(base)
    return res or dom


def domain_of(base, fallback):
    return domain_resource_of(base)[0] or fallback


def code_of(base, verb, path, fallback_domain):
    dom, res = domain_resource_of(base)
    dom = dom or fallback_domain
    act = action_of(verb, path)
    return f'{dom}:{res}:{act}' if res else f'{dom}:{act}'


def action_of(verb, path):
    for v, rx, act in RULES:
        if v == verb and re.fullmatch(rx, path):
            return act
    return 'view'


def build(module):
    cfg = MODULES[module]
    rows = scan(cfg['dirs'])
    for r in rows:
        dom, res = domain_resource_of(r['base'])
        r['domain'] = dom or cfg['domain']
        r['resource'] = res or dom or cfg['domain']
        r['action'] = action_of(r['verb'], r['path'])
        r['code'] = (f"{r['domain']}:{res}:{r['action']}" if res
                     else f"{r['domain']}:{r['action']}")
    codes = {}
    for r in rows:
        codes.setdefault(r['code'], []).append(r)
    return rows, codes


def label_for(module, resource, action):
    res = RESOURCE_LABEL.get(resource, resource)
    return f"{MODULES[module]['label']}{res}{ACTION_LABEL.get(action, action)}"


MAP_LINE = re.compile(r'^(\s*)@(Get|Post|Put|Delete|Patch)Mapping\b(.*)$')
# 类级覆盖判定：任何鉴权注解都算"这个类已经管了"
ANY_AUTH = re.compile(r'@(SaCheckPermission|SaCheckLogin|SaCheckRole|RequirePermission|SaIgnore)\b')
# 方法级幂等判定：只认**权限类**注解。
# ⚠️ 不能复用 ANY_AUTH —— 本仓方法上普遍有 `@SaCheckLogin`（只要登录即可访问），
# 若把它当成"已有权限控制"，E-01 要补的注解会被**整片跳过**（看起来跑了、实际什么都没补）。
PERM_ONLY = re.compile(r'@(SaCheckPermission|RequirePermission)\b')
IMPORT_LINE = 'import cn.dev33.satoken.annotation.SaCheckPermission;\n'


def load_db_codes():
    """只读 `sys_permission.permission_code`（用于 --apply 前的铁律校验）。

    ⚠️ **这是本脚本唯一连库的地方，且只读**。为什么要连库：
    `--apply` 会给端点插 `@SaCheckPermission("<推导出的码>")`，而**如果那个码不在库里，
    该接口会对所有非超管一律 403**（本仓铁律，见记忆 `permission-code-missing-lockout`）。
    推导规则（路径 → 码）与库里历史码**不一定逐字一致**（历史码是人写的），
    所以必须逐条比对；不一致的一律**不插**并列入 `missing` 报告。
    """
    if 'codes' in DB_CODE_CACHE:
        return DB_CODE_CACHE['codes']
    import psycopg2
    conn = psycopg2.connect(host='localhost', port=5432, dbname='devdb',
                            user='devuser', password='devuser123')
    try:
        cur = conn.cursor()
        cur.execute('SELECT permission_code FROM sys_permission')
        codes = {r[0] for r in cur.fetchall()}
        cur.close()
    finally:
        conn.close()
    DB_CODE_CACHE['codes'] = codes
    return codes


DB_CODE_CACHE = {}


def apply_annotations(module, check_db=True):
    """把 @SaCheckPermission("码") 插到各映射注解之前（幂等 + 铁律校验）。

    为什么用脚本而不是手改：端点→码的映射是**同一套规则**推导出来的，
    手改 7 个文件（后续还有 120+ 个）必然出现漏改/改错，而漏改是静默失效、改错是 403。
    脚本逐行处理，并**显式报告**每一处跳过/无法判定/码不在库，不静默放过。

    `check_db=True`（默认）：推导出的码**必须在库**才插注解，否则跳过并计入 missing。
    """
    cfg = MODULES[module]
    changed, skipped, unresolved, missing = [], [], [], []
    known = load_db_codes() if check_db else None
    for fp in iter_controllers(cfg['dirs']):
        name = os.path.basename(fp)
        text = open(fp, encoding='utf-8').read()
        if not REST.search(text):
            continue
        m = CLS_RM.search(text)
        if not m:
            continue
        base = m.group(1).rstrip('/')
        lines = text.splitlines(keepends=True)
        # 类级已有鉴权注解 ⇒ 整类已覆盖，不动
        if ANY_AUTH.search(text.split('class ')[0]):
            skipped.append((name, '类级已有鉴权注解'))
            continue

        out, i, hits = [], 0, 0
        while i < len(lines):
            line = lines[i]
            mm = MAP_LINE.match(line)
            if not mm:
                out.append(line)
                i += 1
                continue
            indent, verb, rest = mm.group(1), mm.group(2).upper(), mm.group(3)
            # 路径可能写在同一行，也可能换行写在下一行
            pm = re.search(r'["\']([^"\']*)["\']', rest)
            path = pm.group(1) if pm else ''
            if not pm and i + 1 < len(lines):
                pm2 = re.search(r'["\']([^"\']*)["\']', lines[i + 1])
                if pm2:
                    path = pm2.group(1)
            # 已在该方法上有注解 ⇒ 幂等跳过。
            # ⚠️ 必须**同时往前往后看**，只看前一行会漏判并**重复插入同一条注解**。
            # 本仓两种写法都有：`@Operation` / `@SaCheckPermission` / `@GetMapping`（注解在映射前）
            # 和 `@Operation` / `@PostMapping` / `@SaCheckPermission`（注解在映射后）。
            # 2026-09-21 在销售域实测踩到：只回溯前几行时，后一种写法会被判成"没注解"，
            # 一次 --apply 就在 SaleOrderController 里插出重复注解。
            # 双向窗口都以「空行 / 方法体边界」为终止，避免把相邻方法的注解当成本方法的。
            window = []
            for prev in reversed(out[-6:]):
                s = prev.strip()
                if s == '' or s.startswith('}') or s.startswith('{'):
                    break
                window.append(s)
            for nxt in lines[i + 1:i + 4]:
                s = nxt.strip()
                if s == '' or s.startswith('}') or s.startswith('{'):
                    break
                window.append(s)
            if PERM_ONLY.search('\n'.join(window)):
                out.append(line)
                i += 1
                continue
            if path is None:
                unresolved.append((name, f'{verb} {rest.strip()}'))
                out.append(line)
                i += 1
                continue
            code = code_of(base, verb, path, cfg['domain'])
            if known is not None and code not in known:
                # 铁律：码不在库 ⇒ 绝不能插注解（插了就是该接口对所有非超管 403）
                missing.append((name, code, f'{verb} {base}{path}'))
                out.append(line)
                i += 1
                continue
            out.append(f'{indent}@SaCheckPermission("{code}")\n')
            out.append(line)
            hits += 1
            i += 1

        if hits:
            new = ''.join(out)
            if 'import cn.dev33.satoken.annotation.SaCheckPermission;' not in new:
                # 插到最后一个 import 之后，保持 import 块连续
                nl = new.splitlines(keepends=True)
                last = max(idx for idx, l in enumerate(nl) if l.startswith('import '))
                nl.insert(last + 1, IMPORT_LINE)
                new = ''.join(nl)
            open(fp, 'w', encoding='utf-8', newline='\n').write(new)
            changed.append((name, hits))
    return changed, skipped, unresolved, missing


def main():
    args = sys.argv[1:]
    if not args or args[0] == '--list':
        for k, v in MODULES.items():
            flag = ' (manual_only)' if v.get('manual_only') else ''
            print(f"{k:12s} {v['label']:6s} domain={v['domain']:12s} slot={v.get('slot')}{flag}")
        return
    module = args[0]
    if module not in MODULES:
        print(f'未知模块: {module}（用 --list 查看）', file=sys.stderr)
        sys.exit(2)

    cfg = MODULES[module]

    if '--apply' in args:
        check_db = '--no-db-check' not in args
        changed, skipped, unresolved, missing = apply_annotations(module, check_db=check_db)
        print(f"═══ {cfg['label']}（{module}）注解写入 ═══\n")
        for name, n in changed:
            print(f"  ✅ {name:42s} +{n} 处 @SaCheckPermission")
        for name, why in skipped:
            print(f"  ⏭  {name:42s} 跳过（{why}）")
        for name, what in unresolved:
            print(f"  ⚠️  {name:42s} 无法判定路径：{what}")
        if missing:
            print(f"\n  🛑 {len(missing)} 处**码不在库**，已按铁律跳过（未插注解）：")
            seen = set()
            for name, code, ep in missing:
                if code in seen:
                    continue
                seen.add(code)
                print(f"     {code:44s} ← {name} {ep}")
            print('     ⇒ 要么补种子迁移把这些码建出来，要么说明推导规则与历史码不一致（需人工定夺）')
        print(f"\n改动 {len(changed)} 个文件 / 新增 {sum(n for _, n in changed)} 处注解"
              + (f"；跳过（码不在库）{len(missing)} 处" if missing else ""))
        if unresolved or missing:
            print('⚠️ 有未处理项（无法判定路径 / 码不在库），请人工处理后重跑', file=sys.stderr)
            sys.exit(3)
        return

    rows, codes = build(module)
    want_sql = '--sql' in args

    if not want_sql:
        print(f"═══ {cfg['label']}（{module}）端点 → 权限码 ═══\n")
        cur = None
        for r in rows:
            if r['resource'] != cur:
                cur = r['resource']
                print(f"\n── 资源 {cur}（{r['base']}）──")
            print(f"  {r['endpoint']:60s} → {r['code']}")
        print(f"\n端点 {len(rows)} 个 → 权限码 {len(codes)} 个")
        print("\n动作分布：", end='')
        dist = {}
        for c in codes:
            a = c.rsplit(':', 1)[1]
            dist[a] = dist.get(a, 0) + 1
        print(', '.join(f'{k}×{v}' for k, v in sorted(dist.items(), key=lambda x: -x[1])))
        return

    # ── 生成 SQL ──
    lines = []
    lines.append(f"-- {cfg['label']}域权限码种子（E-04，2026-09-21）")
    lines.append('--')
    lines.append(f"-- 【为什么】{module} 域在 sys_permission 中没有任何码，而 E-01 要给该域控制器")
    lines.append('--   补 @SaCheckPermission —— 没有码就直接补注解，会让这些接口对**所有非超管一律 403**')
    lines.append('--   （本仓历史事故，铁律：先补种子后补注解）。')
    lines.append('--')
    lines.append('-- 【粒度】一码 = 一「资源 × 动作」，对齐既有约定（实测 PurchaseOrderController')
    lines.append('--   16 端点 → 8 码）。本批：' + f'{len(rows)} 端点 → {len(codes)} 码。')
    lines.append('--')
    perm_base = PERM_SLOT_BASE + cfg['slot'] * 1000
    rp_base = RP_SLOT_BASE + cfg['slot'] * 10000
    lines.append('-- 【id 号段】权限码 %d 起（本模块独占槽位 slot=%d）、角色关联 %d 起。' % (perm_base, cfg['slot'], rp_base))
    lines.append('--   9xxxx 段已被 V11.42x 系列的短块占满，故另开 100000 段；')
    lines.append('--   各模块用不同槽位避免多迁移撞主键（本仓 sys_permission.id 无序列默认值）。')
    lines.append('--')
    lines.append('-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。')
    lines.append('-- =============================================================')
    lines.append('')
    # 用 INSERT ... SELECT ... WHERE NOT EXISTS 而非 VALUES：
    # `sys_permission.permission_code` **没有唯一约束**，若某个生成码恰好已存在
    # （实测 486 个里有 1 个：`stock:list`），VALUES 会插入**同码不同 id 的第二行**，
    # 授权矩阵里出现两条同名项、后端取码行为依赖行序 —— 静默的脏数据。
    lines.append('INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,')
    lines.append('                            permission_name, permission_code, permission_type,')
    lines.append('                            api_path, method, sort, visible, status)')
    lines.append('SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0')
    lines.append('FROM (VALUES')
    vals = []
    order = sorted(codes.keys())
    for i, code in enumerate(order):
        parts = code.split(':')
        dom, act = parts[0], parts[-1]
        res = '-'.join(parts[1:-1])
        name = label_for(module, res, act)
        # api_path/method 顺带落地（对应 E-03「api_path 只填 238/474」）：
        # 一个码可能带多个端点，取扫描到的第一个作为代表路径。
        rep = codes[code][0]
        api = rep['base'] + rep['path']
        vals.append(f" ({perm_base + i}, '{name}', '{code}', '{api}', '{rep['verb']}', {SORT_BASE + i})")
    lines.append(',\n'.join(vals))
    lines.append(') AS v(id, name, code, api, method, sort)')
    lines.append('WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);')
    lines.append('')
    lines.append('INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)')
    lines.append('SELECT %d + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()' % rp_base)
    lines.append('FROM sys_permission p')
    # 只按 id 区间圈定本批（域随控制器而不同，不能再按域前缀 LIKE）
    lines.append(f'WHERE p.id BETWEEN {perm_base} AND {perm_base + len(order)}')
    lines.append('  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp')
    lines.append('                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);')
    print('\n'.join(lines))
    print(f"\n-- 统计：端点 {len(rows)} / 权限码 {len(codes)}", file=sys.stderr)


if __name__ == '__main__':
    main()
