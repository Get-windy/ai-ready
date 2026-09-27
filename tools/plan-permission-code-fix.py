# -*- coding: utf-8 -*-
"""
前端 v-permission 权限码漂移「出方案」脚本（只读，不改任何文件/数据）。

背景与分工（用户 2026-09-27 明确的授权三层模型）：
  · 系统模块 = 系统级；其余模块 = 租户级；
  · 由**系统**给租户授予模块使用权；
  · 租户内，**租户超管**给自己的员工与角色做权限控制。
  ⇒ 「权限码在 sys_permission 里不存在」属**系统级定义缺失** —— 租户侧无权也无力新增，
     必须在系统层补码；补完码之后「授给谁」才是租户级的事。所以本脚本只出方案，不动库。

判据：`v-permission` 校验不到就 **removeChild**（不是置灰）⇒ 码写错 = 按钮消失且不报错。
后端侧不在此列：`tools/audit-permission-codes.py` 已实测 missing_in_db = 0。

用法:
  python tools/plan-permission-code-fix.py              # 全部
  python tools/plan-permission-code-fix.py --json out.json
"""
import io
import json
import os
import re
import sys
from collections import defaultdict

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FRONTEND_SRC = os.path.join(ROOT, 'frontend', 'apps', 'pc-admin', 'src')
SKIP_DIRS = {'node_modules', 'dist', '.git'}

DSN = dict(host='localhost', port=5432, dbname='devdb', user='devuser', password='devuser123')

# ⚠️ 只放**同一动作的不同叫法**（改这个才不会改变语义）。
#    业务上不同的动作（打印 vs 导出、门户 vs 列表、绩效 vs 列表…）一律不进这里 ——
#    那种"映射"会把一个功能点的 gate 偷偷换成另一个功能点的权限，
#    按钮从此按另一条规则显隐，比不修更糟。它们归到 manual/define-or-drop 由人定。
VERB_ALIASES = {
    'add': 'create', 'new': 'create', 'save': 'create', 'createinquiry': 'create',
    'createorder': 'create', 'create-tasks': 'create',
    'edit': 'update', 'modify': 'update', 'savedraft': 'update',
    'del': 'delete', 'remove': 'delete', 'deleteconfirm': 'delete',
    'confirmdelete': 'delete', 'batchdelete': 'delete',
    'query': 'list', 'search': 'list', 'page': 'list', 'index': 'list',
    'info': 'detail', 'show': 'detail', 'viewdetail': 'detail', 'detailinfo': 'detail',
    'viewquotation': 'detail',
    'enable': 'status', 'disable': 'status', 'toggle': 'status', 'changestatus': 'status',
    'toggle-status': 'status',
    'resetkey': 'update', 'reset': 'update',
    'executechain': 'execute', 'openexecutechain': 'execute', 'process': 'execute',
    'canceltask': 'cancel',
    'confirmscreenshot': 'confirm',
    'retryscreenshot': 'retry',
    'preview': 'view', 'batchexport': 'export',
}

# 前缀别名（前端命名空间 → 后端命名空间）
PREFIX_ALIASES = {
    'printing': 'print',
    'settings': 'set',
    'setting': 'set',
    'sys': 'system',
}

# 查询类动作词：这些词之间互不相同但语义接近，库里模块只用其中一个时八成就是它
QUERY_VERBS = {'view', 'list', 'detail', 'query', 'search', 'index', 'page', 'info'}


def walk_files(root, exts):
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for name in filenames:
            if name.endswith(exts):
                yield os.path.join(dirpath, name)


def read(path):
    with open(path, encoding='utf-8', errors='replace') as f:
        return f.read()


def frontend_codes():
    """前端 v-permission 用到的码 → 引用处列表"""
    used = defaultdict(list)
    for path in walk_files(FRONTEND_SRC, ('.vue', '.ts')):
        text = read(path)
        rel = os.path.relpath(path, ROOT)
        for line_no, line in enumerate(text.splitlines(), 1):
            # 跳过注释行里的示例（如指令文档里的 v-permission="'system:user:add'"）
            if line.strip().startswith(('//', '*', '/*')):
                continue
            for code in re.findall(r"v-permission(?:\.\w+)?=\"'([^']+)'\"", line):
                used[code].append(f'{rel}:{line_no}')
    return used


def db_codes():
    import psycopg2
    conn = psycopg2.connect(**DSN)
    cur = conn.cursor()
    cur.execute('SELECT permission_code FROM sys_permission WHERE deleted = 0')
    codes = {r[0] for r in cur.fetchall()}
    cur.close()
    conn.close()
    return codes


def candidates(code, known):
    """
    给一个库里没有的码，只给出**可信**的对应既有码。

    ⚠️ 刻意不做「任意前缀 + 同义动词」这种宽松匹配：它会把
    `erp:fixed-asset:report:query` 猜成 `agreement:list` 这种毫不相干的码，
    让方案看起来"有对应"从而被误改成别的模块的权限。宁可判成「新增」。

    只保留三种保守规则（都要求**除被替换的那一段外，其余各段完全相同**）：
      ① 只换命名空间   printing:template:edit → print:template:edit
      ② 只换动作词     X:add → X:create / X:edit → X:update / X:deleteconfirm → X:delete
      ③ 两者同时换
    """
    out = []
    segs = code.split(':')
    if len(segs) < 2:
        return out
    prefix, verb = segs[0], segs[-1]
    middle = segs[1:-1]

    def probe(p, v):
        cand = ':'.join([p] + middle + [v])
        if cand in known and cand != code and cand not in out:
            out.append(cand)

    alias_prefix = PREFIX_ALIASES.get(prefix)
    alias_verb = VERB_ALIASES.get(verb)
    if alias_prefix:
        probe(alias_prefix, verb)          # ①
    if alias_verb:
        probe(prefix, alias_verb)          # ②
    if alias_prefix and alias_verb:
        probe(alias_prefix, alias_verb)    # ③

    # ④ 前端多写了一层命名空间：erp:fixed-asset:asset:list → fixed-asset:asset:list
    #    （实测：固定资产/序列号等模块的前端码前面多了个 `erp:`，库里没有这一层）
    for drop in range(1, len(segs) - 1):
        cand = ':'.join(segs[drop:])
        if cand in known and cand not in out:
            out.append(cand)
            break
        head, v = cand.rsplit(':', 1)
        if v in VERB_ALIASES and f'{head}:{VERB_ALIASES[v]}' in known:
            out.append(f'{head}:{VERB_ALIASES[v]}')
            break

    # ⚠️ 刻意**不做**「同根 + 同动词唯一」这种猜测：它会把 budget:plan:view 猜成
    #    budget:report:view、erp:serial:export 猜成 erp:batch:export —— 子模块名不同就是
    #    不同功能点，猜错等于把 A 功能的 gate 换成 B 功能的权限。这类交给人定。
    return out[:3]


# 动词族：同族之间只是叫法不同，库里模块用哪个就跟哪个
VERB_FAMILY = {
    'query': {'view', 'list', 'detail', 'query', 'search', 'index', 'page', 'info', 'get'},
    'create': {'add', 'create', 'new', 'save', 'submit', 'batchsubmit', 'import'},
    'update': {'edit', 'update', 'modify', 'batchpublish', 'publish', 'set', 'assign', 'resetkey'},
    'delete': {'delete', 'del', 'remove', 'deleteconfirm', 'confirmdelete', 'batchdelete'},
    # ⚠️ print 不在这里：打印与导出是两个功能点（sale:return-doc 有 export 但没 print，
    #    把退货单的打印 gate 改成 export 语义就错了 —— 该补的是 print 码）
    'export': {'export', 'download'},
}


def module_sibling(code, known):
    """
    同模块前缀下，按**动词族**挑一个既有码：前端用 `view`、库里写 `list` 这种不一致，
    换个动词就通了，不该为它新增权限点。
    """
    parts = code.split(':')
    if len(parts) < 2:
        return None
    prefix = ':'.join(parts[:-1]) + ':'
    verb = parts[-1]
    siblings = [c for c in known if c.startswith(prefix)]
    if not siblings:
        return None
    # 同族优先
    for family in VERB_FAMILY.values():
        if verb in family:
            same = [c for c in siblings if c.split(':')[-1] in family]
            if len(same) == 1:
                return same[0]
    # 精确同名动词
    exact = [c for c in siblings if c.split(':')[-1] == verb]
    if len(exact) == 1:
        return exact[0]
    # 该模块下某个既有码的动词是它的前缀（createinquiry → create）或反之
    for c in siblings:
        sibling_verb = c.split(':')[-1]
        if sibling_verb and (verb.startswith(sibling_verb) or sibling_verb.startswith(verb)):
            return c
    return None


def module_has_codes(code, known):
    """
    该码的**模块路径**（去掉末段动作词）在库里有没有任何权限码。

    这是分类的关键判据：
      · 有 → 前端多半只是把动作词猜错了（改用库里那个即可，**不动库**）；
      · 没有 → 整个模块根本没有权限点定义（要么补码 = 系统级定义，要么确认这些页面
        本就没有后端鉴权点、前端的 gate 该去掉，而不是留一个永远为假的判断）。
    """
    parts = code.split(':')
    if len(parts) < 2:
        return False
    prefix = ':'.join(parts[:-1]) + ':'
    return any(c.startswith(prefix) for c in known)


def main():
    print('前端 v-permission 权限码漂移规划（只读，不改库）\n')
    used = frontend_codes()
    known = db_codes()
    print(f'前端 v-permission 用到 {len(used)} 个码；sys_permission 有 {len(known)} 个')

    missing = {c: loc for c, loc in used.items() if c not in known}
    print(f'其中**库中不存在**的 {len(missing)} 个 → 这些按钮当前对所有人隐藏\n')
    if not missing:
        print('✓ 无漂移')
        return

    # 按模块归组，便于分批决策
    by_module = defaultdict(list)
    for code, locs in sorted(missing.items()):
        by_module[code.split(':')[0]].append((code, locs))

    plan = []
    for module in sorted(by_module):
        print(f'── {module}（{len(by_module[module])} 个）')
        for code, locs in by_module[module]:
            cands = candidates(code, known)
            has_module = module_has_codes(code, known)
            target = None
            if cands:
                verdict = f'改名 → {cands[0]}'
                if len(cands) > 1:
                    verdict += f'（备选 {", ".join(cands[1:])}）'
                action = 'rename'
                target = cands[0]
            elif has_module:
                sib = module_sibling(code, known)
                if sib:
                    verdict = f'改用同模块既有码 → {sib}'
                    action = 'rename'
                    target = sib
                else:
                    verdict = '同模块有码但动词无同族可对（需人工挑一个）'
                    action = 'manual'
            else:
                verdict = '该模块库里**一个权限码都没有** → 要么补码(系统级)，要么去掉这个 gate'
                action = 'define-or-drop'
            print(f'   {code:38s} {verdict}')
            print(f'      引用: {locs[0]}' + (f' 等 {len(locs)} 处' if len(locs) > 1 else ''))
            plan.append({'code': code, 'refs': locs, 'candidates': cands, 'target': target,
                         'module_has_codes': has_module, 'action': action})
        print()

    ren = [p for p in plan if p['action'] == 'rename']
    mod = [p for p in plan if p['action'] == 'rename-module']
    defi = [p for p in plan if p['action'] == 'define-or-drop']
    print('═══ 小结（按处置方式分档）═══')
    print(f' ① 改名到库中既有码（只改前端，不动库）      : {len(ren)} 个')
    print(f' ② 模块内已有权限码、前端猜错动作词（改前端）: {len(mod)} 个')
    print(f' ③ 整模块无权限点定义（要补码=系统级 / 或去掉 gate）: {len(defi)} 个')
    if defi:
        print('\n ③ 涉及的模块路径（这些模块要在系统层定权限点）：')
        seen = []
        for p in defi:
            parts = p['code'].split(':')
            mp = ':'.join(parts[:-1])
            if mp not in seen:
                seen.append(mp)
                print(f'    {mp}' + (f'  ← 现用码如 {p["code"]}' if len(parts) > 1 else ''))

    if '--json' in sys.argv:
        out = sys.argv[sys.argv.index('--json') + 1]
        json.dump(plan, open(out, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
        print(f'已写出 {out}')


if __name__ == '__main__':
    main()
