# -*- coding: utf-8 -*-
"""
把「未生效权限码」（僵尸码，E-02）逐条定性 —— 是「有端点但没注解」还是「端点不存在」。

用法: python tools/classify-zombie-permissions.py [--json]

背景：
  permission-effectivity.json 判「生效」的口径是「有消费方」= 后端 @SaCheckPermission
  或前端 v-permission/checkPermission。于是每一条 ineffective 都有两种截然不同的处置：

    A) **端点存在、只是没注解** → 补 @SaCheckPermission（E-01 的活），删码会误伤；
    B) **端点压根不存在** → 该码是历史残留，应从 sys_permission 删除（迁移），
       否则它永远是「能在矩阵勾选、勾了没用」的僵尸。

  两种处置混着做必然出错，所以先分类再动手。

判定方法（保守，宁可选 A 不选 B）：
  扫全部控制器，按类级 @RequestMapping 得到 base 路径与 HTTP 端点；
  对每条僵尸码 `domain:resource:action`：
    · 若存在某端点，其 (base 路径) 含 domain 片段 **且** (方法路径) 含 resource 片段
      **且** HTTP 方法与该 action 的语义匹配（list/detail/export/view→GET，
      create/update/delete/approve/... →写方法）→ 判 A（列出候选文件:行）
    · 否则判 B
  含 `batch` / 多词 action 的放宽为「路径含 resource 即算候选」，避免把
  `crm:contract:batchapprove` 这类漏判成 B。
"""
import os
import re
import sys
import io
import json
import collections

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BACKEND = os.path.join(ROOT, 'backend')
EFFECTIVITY = os.path.join(BACKEND, 'core', 'base', 'core-base', 'src', 'main',
                           'resources', 'permission-effectivity.json')

MAPPING = re.compile(r'@(Get|Post|Put|Delete|Patch|Request)Mapping\s*\(\s*(?:value\s*=\s*)?'
                     r'(?:\{[^}]*\}|\"([^\"]*)\")?', re.S)
CLASS_MAPPING = re.compile(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?\"([^\"]*)\"')
CLASS_DECL = re.compile(r'class\s+(\w+)')
PERM_ANNO = re.compile(r'@(?:SaCheckPermission|RequirePermission)\s*\(')

# action → 允许的 HTTP 方法（读类动作只认 GET；写类动作认写方法）
READ_ACTIONS = {'list', 'detail', 'view', 'page', 'export', 'query', 'tree', 'options', 'logs', 'history'}
WRITE_HINTS = ('create', 'update', 'delete', 'add', 'edit', 'save', 'remove', 'approve', 'reject',
               'submit', 'cancel', 'enable', 'disable', 'reset', 'refresh', 'import', 'assign',
               'audit', 'confirm', 'execute', 'batch', 'generate', 'sync', 'send', 'test')


def scan_controllers():
    """返回 [(file, line, cls, base, http_method, method_path, has_perm_anno)]"""
    out = []
    for dirpath, dirnames, filenames in os.walk(BACKEND):
        dirnames[:] = [d for d in dirnames if d != 'target']
        if '/src/test' in dirpath.replace('\\', '/'):
            continue
        for fn in filenames:
            if not fn.endswith('Controller.java'):
                continue
            fp = os.path.join(dirpath, fn)
            try:
                src = open(fp, encoding='utf-8').read()
            except Exception:
                continue
            lines = src.split('\n')
            base = ''
            m = CLASS_MAPPING.search(src)
            if m:
                base = m.group(1)
            cls = ''
            mc = CLASS_DECL.search(src)
            if mc:
                cls = mc.group(1)
            for i, ln in enumerate(lines, start=1):
                mm = MAPPING.search(ln)
                if not mm:
                    continue
                kind = mm.group(1)
                if kind == 'Request':
                    continue
                http = {'Get': 'GET', 'Post': 'POST', 'Put': 'PUT',
                        'Delete': 'DELETE', 'Patch': 'PATCH'}[kind]
                path = mm.group(2) or ''
                # 该注解后面 6 行内是否有权限注解
                window = '\n'.join(lines[i - 1:i + 6])
                out.append((os.path.relpath(fp, ROOT).replace('\\', '/'), i, cls, base,
                            http, path, bool(PERM_ANNO.search(window))))
    return out


def action_matches(action, http):
    a = action.lower()
    if a in READ_ACTIONS:
        return http == 'GET'
    if any(h in a for h in WRITE_HINTS):
        return http in ('POST', 'PUT', 'DELETE', 'PATCH')
    # 未知动作：不算匹配（避免放宽过头）
    return False


def main():
    data = json.load(open(EFFECTIVITY, encoding='utf-8'))
    zombies = data['ineffective']
    ctrls = scan_controllers()

    # 按「base 路径关键词」索引控制器，避免 O(n*m) 全扫
    has_endpoint, no_endpoint = [], []
    for code in zombies:
        parts = code.split(':')
        domain = parts[0]
        resource = parts[1] if len(parts) > 2 else ''
        action = parts[-1]
        cands = []
        for (fp, ln, cls, base, http, path, has_anno) in ctrls:
            base_l = base.lower()
            path_l = path.lower()
            if domain not in base_l and domain not in fp.lower():
                continue
            key = resource.replace('-', '').lower()
            if key and key not in (path_l + base_l).replace('-', ''):
                continue
            if not action_matches(action, http):
                continue
            cands.append(f'{fp}:{ln} [{http} {base}{path}]' + ('' if has_anno else ' ★无注解'))
        if cands:
            has_endpoint.append({'code': code, 'candidates': cands[:3]})
        else:
            no_endpoint.append({'code': code, 'domain': domain})

    if '--json' in sys.argv:
        print(json.dumps({'hasEndpoint': has_endpoint, 'noEndpoint': no_endpoint},
                         ensure_ascii=False, indent=1))
        return

    print(f'僵尸码共 {len(zombies)} 条\n')
    print(f'═══ A) 端点存在、只是没注解 → 补 @SaCheckPermission（{len(has_endpoint)} 条）═══')
    by_dom = collections.Counter(x['code'].split(':')[0] for x in has_endpoint)
    print('  按域：' + ', '.join(f'{k}×{v}' for k, v in by_dom.most_common()))
    for x in has_endpoint:
        print(f"  {x['code']:44s} ← {x['candidates'][0]}")
    print(f'\n═══ B) 端点不存在 → 从 sys_permission 删除（{len(no_endpoint)} 条）═══')
    by_dom2 = collections.Counter(x['domain'] for x in no_endpoint)
    print('  按域：' + ', '.join(f'{k}×{v}' for k, v in by_dom2.most_common()))
    for x in no_endpoint:
        print(f"  {x['code']}")


if __name__ == '__main__':
    main()
