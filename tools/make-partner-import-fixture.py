"""生成「其他往来单位」Excel 导入验证夹具（verify-md-partner.cjs H3 使用）。

表头与后端 /erp/md/customer/import-excel 的表头映射（baseImportField）保持一致：
「导入结果」列会被忽略，「单位类别」列对应分类名称（不存在时后端自动建分类）。
"""
import os
from openpyxl import Workbook

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'tool-results', 'partner-import-fixture.xlsx')


def main():
    wb = Workbook()
    ws = wb.active
    ws.title = '其他往来单位信息'
    ws.append(['导入结果', '单位编号(必填)', '单位名称(必填)', '单位类别',
               '联系人', '联系电话', '联系地址', '备注'])
    ws.append(['', '', '金标准导入验证单位_A', '银行', '导入联系人', '13800000001', '导入地址A', '导入备注A'])
    ws.append(['', '', '金标准导入验证单位_B', '银行', '导入联系人B', '13800000002', '导入地址B', ''])
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    wb.save(OUT)
    print('fixture written:', os.path.normpath(OUT))


if __name__ == '__main__':
    main()
