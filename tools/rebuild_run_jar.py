# -*- coding: utf-8 -*-
"""把编译好的 erp-stock jar 与新迁移脚本替换进 core-api fat jar，产出运行副本。
用法： python tools/rebuild_run_jar.py <src-exec-jar> <out-jar>
注意：BOOT-INF/lib/*.jar 必须以 ZIP_STORED 写入，否则 Spring Boot loader 读不出嵌套 jar
（会 NoClassDefFoundError）。
"""
import sys, zipfile, shutil, os

SRC = sys.argv[1]
OUT = sys.argv[2]
REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

NEW_LIB = os.path.join(REPO, 'backend', 'erp', 'erp-stock', 'target', 'erp-stock-0.3.17.jar')
MIG_DIR = os.path.join(REPO, 'backend', 'core', 'api', 'core-api', 'src', 'main', 'resources', 'db', 'migration')
NEW_MIG = 'V11.154.0__Add_Weight_Volume_To_Product_Unit.sql'

LIB_ENTRY = 'BOOT-INF/lib/erp-stock-0.3.17.jar'
MIG_ENTRY = 'BOOT-INF/classes/db/migration/' + NEW_MIG


def main():
    assert os.path.exists(NEW_LIB), NEW_LIB
    mig_path = os.path.join(MIG_DIR, NEW_MIG)
    assert os.path.exists(mig_path), mig_path

    zin = zipfile.ZipFile(SRC, 'r')
    tmp = OUT + '.tmp'
    zout = zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED, allowZip64=True)

    replaced_lib = replaced_mig = False
    for item in zin.infolist():
        data = zin.read(item.filename)
        if item.filename == LIB_ENTRY:
            with open(NEW_LIB, 'rb') as f:
                data = f.read()
            zi = zipfile.ZipInfo(item.filename, date_time=item.date_time)
            zi.compress_type = zipfile.ZIP_STORED   # 嵌套 jar 必须 STORED
            zi.external_attr = item.external_attr
            zout.writestr(zi, data)
            replaced_lib = True
            print('[ok] replaced', item.filename, len(data))
            continue
        if item.filename == MIG_ENTRY:
            with open(mig_path, 'rb') as f:
                data = f.read()
            zout.writestr(item, data)
            replaced_mig = True
            print('[ok] replaced', item.filename)
            continue
        zout.writestr(item, data)

    if not replaced_mig:
        zi = zipfile.ZipInfo(MIG_ENTRY)
        zi.compress_type = zipfile.ZIP_DEFLATED
        with open(mig_path, 'rb') as f:
            zout.writestr(zi, f.read())
        print('[ok] added', MIG_ENTRY)

    zout.close()
    zin.close()
    if os.path.exists(OUT):
        os.remove(OUT)
    shutil.move(tmp, OUT)
    print('[done]', OUT, os.path.getsize(OUT))
    print('replaced_lib=', replaced_lib, 'replaced_mig=', replaced_mig)


if __name__ == '__main__':
    main()
