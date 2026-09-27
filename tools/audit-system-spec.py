# -*- coding: utf-8 -*-
"""系统模块审计 · 规格提取器
从 docs/Yh-Spec/手动整理对标开发文档/系统模块/*.md 抽取每页「规格头」，
输出 tools/system-spec.json 供后续与代码/DB 对照。
"""
import os, re, json, io

DOC = r"I:\AI-Ready\docs\Yh-Spec\手动整理对标开发文档\系统模块"
OUT = r"I:\AI-Ready\tools\system-spec.json"


def grab(text, pat, group=1):
    m = re.search(pat, text)
    return m.group(group).strip() if m else None


def parse(path, fname):
    raw = io.open(path, encoding="utf-8").read()
    # 头部 20 行（元信息块）
    head = "\n".join(raw.splitlines()[:22])
    d = {"doc": fname}

    # 路由
    m = re.search(r"本系统路由：\*\*\s*(.+?)(?:\n|$)", head)
    if m:
        routes = re.findall(r"`([^`]+)`", m.group(1))
        d["route"] = routes[0] if routes else m.group(1).strip()
        d["entry_mode"] = "双入口" if "双入口" in m.group(1) else "单入口"

    # 菜单块
    m = re.search(r"菜单：\*\*\s*(.+?)(?:\n|$)", head)
    if m:
        blk = m.group(1)
        d["menu_id"] = grab(blk, r"ID\s*`(\d+)`")
        d["menu_code"] = grab(blk, r"menu_code\s*=\s*`?([A-Za-z0-9:_\-]+)")
        d["client_type"] = grab(blk, r"client_type\s*=\s*`?'?([a-z\-]+)")
        d["path"] = grab(blk, r"path\s*=\s*`?([A-Za-z0-9/:_\-]+)")
        d["component"] = grab(blk, r"component\s*=\s*`?([A-Za-z0-9/_\-\.]+)")
        d["parent_menu_id"] = grab(blk, r"父[^`]*`(\d+)`")

    # 后端类 / 前缀 / 端点数
    m = re.search(r"后端[：:]\*\*\s*(.+?)(?:\n|$)", head)
    if m:
        d["backend_raw"] = m.group(1).strip()[:300]
        d["backend_prefix"] = grab(m.group(1), r"(/api/[A-Za-z0-9/_\{\}\-]+)")
        d["endpoint_count_head"] = grab(m.group(1), r"\*\*?(\d+)\s*个端点")
    # 权限码
    m = re.search(r"权限码[：:]\*\*\s*(.+?)(?:\n|$)", head)
    if m:
        d["perm_raw"] = m.group(1).strip()[:300]
    # 全文中出现的权限码样本
    d["perm_codes_mentioned"] = sorted(set(re.findall(r"`((?:system|platform|monitor|datasource|export|module|tenant|menu|cache|scheduler|audit)[a-z0-9_:%]*:[a-z0-9:_%\-]+)`", raw)))[:40]
    # 数据表
    d["tables"] = sorted(set(re.findall(r"`(sys_[a-z0-9_]+|sync_[a-z0-9_]+|fin_[a-z0-9_]+|batch_[a-z0-9_]+)`", raw)))
    d["lines"] = len(raw.splitlines())
    return d


def main():
    items = []
    for f in sorted(os.listdir(DOC)):
        if not f.endswith(".md") or f == "README.md":
            continue
        items.append(parse(os.path.join(DOC, f), f))
    io.open(OUT, "w", encoding="utf-8").write(json.dumps(items, ensure_ascii=False, indent=1))
    print("docs:", len(items))
    for d in items:
        print("%-32s %-8s %-28s %-30s %s" % (
            d["doc"][:30], d.get("menu_id"), (d.get("menu_code") or "")[:26],
            (d.get("component") or "")[:28], d.get("entry_mode")))


if __name__ == "__main__":
    main()
