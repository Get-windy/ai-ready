# -*- coding: utf-8 -*-
"""
静态扫描：已装配包内的 Spring MVC 映射冲突（同 HTTP method + 同路径）。

背景：本仓库 `AiReadyApplication.scanBasePackages` 长期漏配若干包，导致包内控制器
「从未装配」—— 其中的重复路由也因此从未暴露。一旦把包补进扫描清单，Spring 会在启动时
抛 `IllegalStateException: Ambiguous mapping ...` 直接让应用起不来（2026-09-18 实踩：
`cn.aiedge.monitor` 的 SystemMonitorController 与 AlertManagementController 有 7 组完全相同的映射）。

用法：
    python tools/check-api-mapping-conflicts.py            # 扫「已装配包」
    python tools/check-api-mapping-conflicts.py --all      # 扫全部 cn.aiedge.* 控制器
"""
import io
import os
import re
import sys
from collections import defaultdict

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

BACKEND = r"I:\AI-Ready\backend"
APP = os.path.join(BACKEND, "core", "api", "core-api", "src", "main", "java", "cn", "aiedge", "AiReadyApplication.java")

MAPPING_ANNOTATIONS = {
    "GetMapping": "GET",
    "PostMapping": "POST",
    "PutMapping": "PUT",
    "DeleteMapping": "DELETE",
    "PatchMapping": "PATCH",
}


def strip_comments(text: str) -> str:
    """去掉 // 行注释（保留字符串内的 //，这里场景足够用）"""
    out = []
    for line in text.splitlines():
        # 仅在行首空白后出现 // 时按注释处理；行内注释保留（避免误伤 URL）
        if line.lstrip().startswith("//"):
            continue
        out.append(line)
    return "\n".join(out)


def scan_packages() -> list:
    src = strip_comments(open(APP, encoding="utf-8").read())
    m = re.search(r"scanBasePackages\s*=\s*\{(.*?)\}", src, re.S)
    if not m:
        return []
    return re.findall(r'"([^"]+)"', m.group(1))


def java_files():
    for root, dirs, files in os.walk(BACKEND):
        dirs[:] = [d for d in dirs if d not in ("target", "node_modules", ".git")]
        for f in files:
            if f.endswith(".java"):
                yield os.path.join(root, f)


def package_of(path: str) -> str:
    try:
        text = open(path, encoding="utf-8", errors="replace").read()
    except OSError:
        return ""
    m = re.search(r"^\s*package\s+([\w.]+)\s*;", text, re.M)
    return m.group(1) if m else ""


def norm(path: str) -> str:
    path = re.sub(r"\s+", "", path)
    path = path.strip('"')
    if not path.startswith("/"):
        path = "/" + path
    return re.sub(r"/{2,}", "/", path).rstrip("/") or "/"


def extract(path: str):
    """返回 [(http_method, full_path, class_name, method_name, lineno)]"""
    try:
        text = open(path, encoding="utf-8", errors="replace").read()
    except OSError:
        return [], []
    lines = text.splitlines()

    class_mapping = None
    class_name = ""
    findings = []
    unbounded = []

    pending_class = False
    pending_method = None  # (annotation, line_no)

    for i, raw in enumerate(lines, start=1):
        line = raw.strip()
        if line.startswith("//"):
            continue

        if re.search(r"@(RestController|Controller)\b", line):
            pending_class = True

        m = re.search(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?\{?\s*"([^"]*)"', line)
        if m:
            if pending_class and not class_name:
                class_mapping = m.group(1)
                pending_class = False
            else:
                # 方法级 @RequestMapping（带 method 属性时另处理）
                mm = re.search(r"method\s*=\s*RequestMethod\.(\w+)", text[i - 1:i + 3].__str__())
                findings.append((mm.group(1) if mm else "ANY", m.group(1), "", i))
            continue

        for ann, verb in MAPPING_ANNOTATIONS.items():
            m = re.search(r"@" + ann + r"\s*\(\s*(?:value\s*=\s*)?\{?\s*\"([^\"]*)\"", line)
            if m:
                pending_method = (verb, m.group(1), i)
                break
            if re.search(r"@" + ann + r"\s*\(\s*\)", line):
                pending_method = (verb, "", i)
                break
        else:
            if re.search(r"@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\s*$", line):
                pending_method = ("ANY", "", i)

        if pending_method:
            verb, sub, lineno = pending_method
            pending_method = None
            full = norm((class_mapping or "") + "/" + sub) if sub else norm(class_mapping or "/")
            findings.append((verb, full, "", lineno))

        cm = re.search(r"(?:public\s+)?class\s+(\w+)", line)
        if cm and not class_name:
            class_name = cm.group(1)

    if class_mapping is None and any("@RestController" in l or "@Controller" in l for l in lines):
        unbounded.append(path)

    pkg = package_of(path)
    named = [(v, p, f"{pkg}.{class_name}", f"line {n}") for (v, p, _, n) in findings]
    return named, unbounded


def main():
    scan_all = "--all" in sys.argv
    pkgs = scan_packages()

    index = defaultdict(list)
    scanned = 0
    for path in java_files():
        pkg = package_of(path)
        if not pkg.startswith("cn.aiedge"):
            continue
        if not scan_all:
            if not any(pkg == p or pkg.startswith(p + ".") for p in pkgs):
                continue
        entries, _ = extract(path)
        if entries:
            scanned += 1
        for verb, full, owner, where in entries:
            if full == "/":
                continue
            index[(verb, full)].append(f"{owner} ({where})")

    print(f"扫描范围: {'全部 cn.aiedge 控制器' if scan_all else '已装配包'}；控制器文件 {scanned} 个；映射 {len(index)} 条")

    # 只报「跨类」冲突：同类内的重复多半是本脚本对「方法级 @RequestMapping 无 value」
    # 等写法的解析误报，噪声极大；真正会让 Spring 启动失败的跨类冲突才是目标。
    dup = {}
    for k, owners in index.items():
        classes = {o.split(" (")[0] for o in owners}
        if len(classes) > 1:
            dup[k] = sorted(classes)
    if not dup:
        print("未发现跨类映射冲突 ✓")
        return 0
    print(f"\n发现 {len(dup)} 组跨类冲突（这些会让应用启动失败）：")
    for (verb, full), owners in sorted(dup.items(), key=lambda x: x[0][1]):
        print(f"\n  {verb} {full}")
        for o in owners:
            print(f"      - {o}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
