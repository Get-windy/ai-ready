# -*- coding: utf-8 -*-
"""系统模块审计 · 契约对照（只读）
1) 后端端点全集（类级 @RequestMapping 前缀 + 方法级 Mapping），带鉴权注解
2) 前端调用路径全集（按 app 分）
3) 系统模块域（views/admin + views/system + 相关 api 封装）的调用点
输出 tools/system-contract.json
"""
import os, re, io, json, sys, collections

sys.stdout.reconfigure(encoding="utf-8")
ROOT = r"I:\AI-Ready"
BE = os.path.join(ROOT, "backend")
FE = os.path.join(ROOT, "frontend", "apps")
OUT = os.path.join(ROOT, "tools", "system-contract.json")

MTH = re.compile(r'@(Get|Post|Put|Delete|Patch|Request)Mapping\s*(?:\(\s*(?:value\s*=\s*)?[{]?\s*"([^"]*)")?')
CLS = re.compile(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?[{]?\s*"([^"]*)"')
AUTH = re.compile(r'@(SaCheckPermission|SaCheckRole|SaCheckLogin|SaCheckOr|RequirePermission|PreAuthorize)\s*\(([^)]*)\)')


def norm(p):
    p = re.sub(r'\$\{[^}]*\}', '{}', p or "")
    p = re.sub(r'\{[^}]*\}', '{}', p)
    p = re.sub(r'/+', '/', p)
    if not p.startswith('/'):
        p = '/' + p
    return p.rstrip('/') or '/'


def scan_backend():
    eps = []
    for dp, dn, fn in os.walk(BE):
        dn[:] = [d for d in dn if d not in ("target", ".git")]
        for f in fn:
            if not f.endswith(".java"):
                continue
            path = os.path.join(dp, f)
            t = io.open(path, encoding="utf-8", errors="replace").read()
            if "@RestController" not in t:
                continue
            cls = f[:-5]
            m = CLS.search(t)
            prefix = m.group(1) if m else ""
            pkg = (re.search(r"package\s+([\w\.]+);", t) or [None, ""])[1]
            mod = os.path.relpath(path, BE).replace("\\", "/").split("/")[0]
            # 只处理方法级映射：定位类声明起点，之前的类级注解不算端点
            clsdecl = re.search(r"\b(?:public|final|abstract|\s)*class\s+" + re.escape(cls) + r"\b", t)
            body_at = clsdecl.end() if clsdecl else 0
            for mm in MTH.finditer(t):
                if mm.start() < body_at:
                    continue
                verb = mm.group(1).upper()
                sub = mm.group(2)
                # 注解块：从上一个方法结束/注解起点到本行
                start = t.rfind("\n}", 0, mm.start())
                seg = t[max(0, start):mm.start() + 200]
                if len(seg) > 4000:
                    seg = t[mm.start() - 1500:mm.start() + 300]
                auths = [a.group(1) + ":" + a.group(2).strip()[:90] for a in AUTH.finditer(seg)]
                line = t[:mm.start()].count("\n") + 1
                full = norm(prefix + "/" + (sub or "")) if sub is not None else norm(prefix)
                eps.append({"method": "GET" if verb == "REQUEST" else verb,
                            "path": full, "raw_path": (prefix + "/" + (sub or "")),
                            "cls": cls, "pkg": pkg, "module": mod,
                            "file": os.path.relpath(path, ROOT).replace("\\", "/"), "line": line,
                            "auth": auths})
    return eps


REQ = re.compile(r"""request\s*\.\s*(get|post|put|delete|patch)\s*(?:<[^>]*>)?\s*\(\s*([^,)\n]{0,300})""")
REQX = re.compile(r"""request\s*\.\s*(get|post|put|delete|patch)\s*(?:<[^>]*>)?\s*\(\s*\n\s*([^,\n]{0,200})""")
REQ2 = re.compile(r"""(?:apiReq|request)\s*\(\s*[`'"](?:GET|POST|PUT|DELETE|PATCH)[`'"]\s*,\s*([^,)\n]{0,200})""")
# 项目内自建封装：api/*.ts 里的 mutate('post', '/path', data) / mutateRaw(...)
MUT = re.compile(r"""mutate(?:Raw)?\s*\(\s*[`'"](get|post|put|delete|patch)[`'"]\s*,\s*([^,)\n]{0,300})""")
CONST = re.compile(r"""(?:const|let|var)\s+([A-Za-z_$][\w$]*)\s*(?::[^=]+)?=\s*[`'"]([^`'"]+)[`'"]""")
LIT = re.compile(r"""([`'"])((?:\\.|(?!\1)[^\\])*)\1""")


def resolve_expr(expr, consts):
    """把请求表达式解析成归一化路径：常量展开 + 字面量拼接 + 变量占位"""
    e = expr
    # 1) 已知常量名 → 标记（先处理 "${NAME}"，再处理裸标识符）
    kvals = {}
    for i, (name, val) in enumerate(sorted(consts.items(), key=lambda x: -len(x[0]))):
        kvals[i] = val
        e = re.sub(r"\$\{" + re.escape(name) + r"\}", "\x02%d\x02" % i, e)
        e = re.sub(r"(?<![\w$.])" + re.escape(name) + r"(?![\w$])", "\x02%d\x02" % i, e)
    # 2) 抽出字面量，其余标识符/调用变 {}
    lits = []

    def _grab(m):
        lits.append(m.group(2))
        return "\x01%d\x01" % (len(lits) - 1)
    e = LIT.sub(_grab, e)
    # 3) 截断到第一个顶层逗号（此时字符串已占位，逗号全在代码层）
    e = e.split(",")[0]
    e = re.sub(r"[A-Za-z_$][\w$.]*(\s*\([^()]*\))?", "{}", e)
    e = re.sub(r"\s*\+\s*", "", e)
    for i, v in enumerate(lits):
        e = e.replace("\x01%d\x01" % i, v)
    for i, v in kvals.items():
        e = e.replace("\x02%d\x02" % i, v)
    e = re.sub(r"[)\s;]+$", "", e.strip())
    return e.strip()


def scan_fe():
    calls = []          # {app, file, line, raw}
    for app in os.listdir(FE):
        base = os.path.join(FE, app)
        if not os.path.isdir(base):
            continue
        src = os.path.join(base, "src")
        if not os.path.isdir(src):
            continue
        for dp, dn, fn in os.walk(src):
            dn[:] = [d for d in dn if d not in ("node_modules", "dist", ".git")]
            for f in fn:
                if not f.endswith((".ts", ".vue", ".js")):
                    continue
                p = os.path.join(dp, f)
                t = io.open(p, encoding="utf-8", errors="replace").read()
                rel = os.path.relpath(p, ROOT).replace("\\", "/")
                consts = {m.group(1): m.group(2) for m in CONST.finditer(t)}
                for mm in REQ.finditer(t):
                    raw = resolve_expr(mm.group(2), consts)
                    calls.append({"app": app, "file": rel, "line": t[:mm.start()].count("\n") + 1,
                                  "method": mm.group(1).upper(), "raw": raw, "path": norm(raw)})
                for mm in REQX.finditer(t):
                    raw = resolve_expr(mm.group(2), consts)
                    calls.append({"app": app, "file": rel, "line": t[:mm.start()].count("\n") + 1,
                                  "method": mm.group(1).upper(), "raw": raw, "path": norm(raw)})
                for mm in REQ2.finditer(t):
                    raw = resolve_expr(mm.group(1), consts)
                    calls.append({"app": app, "file": rel, "line": t[:mm.start()].count("\n") + 1,
                                  "method": "ANY", "raw": raw, "path": norm(raw)})
                for mm in MUT.finditer(t):
                    raw = resolve_expr(mm.group(2), consts)
                    calls.append({"app": app, "file": rel, "line": t[:mm.start()].count("\n") + 1,
                                  "method": mm.group(1).upper(), "raw": raw, "path": norm(raw)})
    return calls


def key(p):
    """归一化 baseURL 差异：前端 axios baseURL='/api'，封装里写 /menu/tree → 后端 /api/menu/tree"""
    if p.startswith("/api/"):
        p = p[4:]
    elif p == "/api":
        p = "/"
    return p or "/"


def main():
    eps = scan_backend()
    calls = scan_fe()
    be_paths = collections.defaultdict(list)
    for e in eps:
        be_paths[key(e["path"])].append(e)
    calls = [c for c in calls if "..." not in c["raw"] and "/*" not in c["raw"]]
    fe_paths = set(key(c["path"]) for c in calls)

    # 前端调了、后端没有
    dangling = collections.defaultdict(list)
    for c in calls:
        if key(c["path"]) not in be_paths:
            dangling[c["path"]].append(c)

    out = {"endpoints": eps, "calls": calls,
           "dangling": {k: v[:6] for k, v in sorted(dangling.items())},
           "be_count": len(eps), "be_path_count": len(be_paths),
           "fe_call_count": len(calls), "fe_path_count": len(fe_paths)}
    io.open(OUT, "w", encoding="utf-8").write(json.dumps(out, ensure_ascii=False, indent=1))
    print("后端端点 %d（路径 %d）｜前端调用 %d（路径 %d）" % (len(eps), len(be_paths), len(calls), len(fe_paths)))
    print("前端调了但后端无此路径: %d 条" % len(dangling))


if __name__ == "__main__":
    main()
