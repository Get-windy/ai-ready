"""把改动过的模块产物替换进一份独立的 fat jar 副本。

背景：共享的 target/core-api-0.3.17-exec.jar 正被其它会话的 5655 实例使用，
不能覆盖；这里从既有 fat jar 复制一份到独立路径再替换嵌套内容。
约定（见项目踩坑记录）：嵌套 jar 必须 ZIP_STORED，压缩写会 NoClassDefFoundError。

用法： python tools/patch-fatjar.py <源fat jar> <目标fat jar> <条目=新文件> [...]
"""
import os
import sys
import zipfile

src, dst = sys.argv[1], sys.argv[2]
targets = {}
for pair in sys.argv[3:]:
    entry, path = pair.split('=', 1)
    targets[entry] = path

zin = zipfile.ZipFile(src, 'r')
names = set(zin.namelist())
missing = [e for e in targets if e not in names]
if missing:
    print('!! 以下条目不在 fat jar 中：', missing)
    sys.exit(2)

replaced = []
with zipfile.ZipFile(dst, 'w', zipfile.ZIP_DEFLATED, allowZip64=True) as zout:
    for item in zin.infolist():
        data = zin.read(item.filename)
        if item.filename in targets:
            with open(targets[item.filename], 'rb') as f:
                data = f.read()
            info = zipfile.ZipInfo(item.filename, date_time=item.date_time)
            # 嵌套 jar 必须 STORED（压缩会导致启动 NoClassDefFoundError）
            info.compress_type = zipfile.ZIP_STORED if item.filename.endswith('.jar') else zipfile.ZIP_DEFLATED
            info.external_attr = item.external_attr
            zout.writestr(info, data)
            replaced.append((item.filename, len(data)))
        else:
            zout.writestr(item, data)
zin.close()

for name, size in replaced:
    print(f'  替换 {name} -> {size} bytes')
print(f'完成：{dst} ({os.path.getsize(dst)} bytes)')
