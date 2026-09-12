#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""把新的模块 jar 替换进 Spring Boot fat jar 的 BOOT-INF/lib（ZIP_STORED，Spring Boot 嵌套 jar 要求）。

用法：python tools/swap-nested-jar.py <base-fat-jar> <new-module-jar> <输出-fat-jar>
"""
import sys
import zipfile

BASE, MOD, OUT = sys.argv[1], sys.argv[2], sys.argv[3]

with zipfile.ZipFile(MOD) as m:
    mod_name = MOD.replace('\\', '/').split('/')[-1]
    mod_bytes = open(MOD, 'rb').read()

target_entry = 'BOOT-INF/lib/' + mod_name
replaced = False

with zipfile.ZipFile(BASE) as zin, zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        data = zin.read(item.filename)
        if item.filename == target_entry:
            data = mod_bytes
            replaced = True
            zi = zipfile.ZipInfo(item.filename, item.date_time)
            zi.compress_type = zipfile.ZIP_STORED
        else:
            zi = item
            if item.filename == 'BOOT-INF/lib/' + mod_name:
                zi.compress_type = zipfile.ZIP_STORED
        zi.external_attr = item.external_attr
        zout.writestr(zi, data)

print('replaced' if replaced else 'TARGET_NOT_FOUND', target_entry)
