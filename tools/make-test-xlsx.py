# -*- coding: utf-8 -*-
"""生成一个最小但合法的 .xlsx（用于导入链路真机验证，零第三方依赖）。

用法: python tools/make-test-xlsx.py 输出路径.xlsx "表头1,表头2" "值1,值2" "值1,值2"
说明: 用 inlineStr 写单元格，省掉 sharedStrings；xlsx 本质是 zip，故只用标准库。
"""
import sys, zipfile, io

CT = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>'''

RELS = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>'''

WB = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets>
</workbook>'''

WBRELS = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>'''


def esc(s):
    return (s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))


def col(i):
    """0 → A, 25 → Z, 26 → AA"""
    s = ""
    i += 1
    while i:
        i, r = divmod(i - 1, 26)
        s = chr(65 + r) + s
    return s


def sheet(rows):
    out = ['<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
           '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>']
    for ri, row in enumerate(rows, 1):
        out.append('<row r="%d">' % ri)
        for ci, v in enumerate(row):
            if v is None or v == "":
                continue
            out.append('<c r="%s%d" t="inlineStr"><is><t>%s</t></is></c>' % (col(ci), ri, esc(str(v))))
        out.append('</row>')
    out.append('</sheetData></worksheet>')
    return "".join(out)


def main():
    path = sys.argv[1]
    rows = [a.split(",") for a in sys.argv[2:]]
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", CT)
        z.writestr("_rels/.rels", RELS)
        z.writestr("xl/workbook.xml", WB)
        z.writestr("xl/_rels/workbook.xml.rels", WBRELS)
        z.writestr("xl/worksheets/sheet1.xml", sheet(rows))
    print("已生成 %s（%d 行）" % (path, len(rows)))


if __name__ == "__main__":
    main()
