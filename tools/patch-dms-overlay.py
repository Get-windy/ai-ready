"""把 javac 覆盖编译出的模块类打进一份 dms jar 副本（模块构建被并行会话阻塞时的应急通道）。

背景：`dms-delivery` 是单模块，任一并行会话留下编译错误（如 route/service/RouteVrpPlanner）
就会让 `mvn -pl dms install` 整体失败；此时可用「最近一次成功安装的 m2 dms jar 作基座
+ 只覆盖本次改动的类」继续验证，且**不触碰任何人的源码**。

用法：
    # 1) 覆盖编译（-parameters 必带：Spring 解析 @RequestParam 名称依赖它）
    javac -parameters -encoding UTF-8 -d /tmp/overlay \\
        -cp "C:/Users/<u>/.m2/repository/cn/aiedge/dms-delivery/<ver>/dms-delivery-<ver>.jar;<deps>" \\
        $(find src/main/java/cn/aiedge/dms/<pkg> -name "*.java")
    # 2) 打补丁
    python tools/patch-dms-overlay.py <base dms jar> <out dms jar> <overlay dir>

再用 tools/patch-fatjar.py 把它替换进 fat jar 副本即可启动验证。
"""
import os
import sys
import zipfile

base, out, overlay = sys.argv[1], sys.argv[2], sys.argv[3]

entries = {}
for root, _dirs, names in os.walk(overlay):
    for name in names:
        if name.endswith('.class'):
            full = os.path.join(root, name)
            entries[os.path.relpath(full, overlay).replace(os.sep, '/')] = full

if not entries:
    print('!! overlay 目录里没有 .class，先执行 javac 覆盖编译')
    sys.exit(2)

replaced = set()
with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        data = zin.read(item.filename)
        if item.filename in entries:
            with open(entries[item.filename], 'rb') as fh:
                data = fh.read()
            replaced.add(item.filename)
        zout.writestr(item, data)
    for rel, full in entries.items():
        if rel not in replaced:
            with open(full, 'rb') as fh:
                zout.writestr(rel, fh.read())

print(f'overlay={len(entries)} replaced={len(replaced)} added={len(entries) - len(replaced)}')
print(f'out={out} ({os.path.getsize(out)} bytes)')
