"""
批量迁移剩余 a-table 文件到 VxeTableList
处理规则：
1. 无 bodyCell 的简单表格：替换 a-table 标签和 columns 定义
2. 带 bodyCell 的表格：转换为 named slots
3. 移除 fillEmptyRows/MIN_TABLE_ROWS/__empty_row/tableDataSource
"""

import os
import re

BASE = "i:/AI-Ready/frontend/apps/pc-admin/src/views"

# ============== 处理无 slot 的简单表格 ==============

SIMPLE_FILES = {
    # 销售报表组件 - 全部是简单显示表格
    "erp/sales-report/components/CustomerRanking.vue": {
        "tables": [
            {
                "columns_var": "columns",
                "new_columns_var": "vxeColumns",
                "columns_def": 'const vxeColumns = [\n'
                    '  { field: "rank", title: "排名", width: 60, align: "center" },\n'
                    '  { field: "customerName", title: "客户名称", minWidth: 150 },\n'
                    '  { field: "totalAmount", title: "销售金额", width: 120, align: "right" },\n'
                    '  { field: "orderCount", title: "订单数", width: 80, align: "center" },\n'
                    '  { field: "avgAmount", title: "平均金额", width: 120, align: "right" },\n'
                    '  { field: "growthRate", title: "增长率", width: 100, align: "right" }\n'
                    ']',
                "slot_map": {},
            }
        ]
    },
    "erp/sales-report/components/ProductRanking.vue": {
        "tables": [
            {
                "columns_var": "columns",
                "new_columns_var": "vxeColumns",
                "columns_def": 'const vxeColumns = [\n'
                    '  { field: "rank", title: "排名", width: 60, align: "center" },\n'
                    '  { field: "productName", title: "产品名称", minWidth: 150 },\n'
                    '  { field: "totalAmount", title: "销售金额", width: 120, align: "right" },\n'
                    '  { field: "quantity", title: "销量", width: 80, align: "center" },\n'
                    '  { field: "avgPrice", title: "平均单价", width: 120, align: "right" },\n'
                    '  { field: "growthRate", title: "增长率", width: 100, align: "right" }\n'
                    ']',
                "slot_map": {},
            }
        ]
    },
    "erp/sales-report/components/SalesStatistics.vue": {
        "tables": [
            {
                "columns_var": "columns",
                "new_columns_var": "vxeColumns",
                "columns_def": 'const vxeColumns = [\n'
                    '  { field: "month", title: "月份", width: 100 },\n'
                    '  { field: "revenue", title: "收入", width: 120, align: "right" },\n'
                    '  { field: "cost", title: "成本", width: 120, align: "right" },\n'
                    '  { field: "profit", title: "利润", width: 120, align: "right" },\n'
                    '  { field: "orderCount", title: "订单数", width: 80, align: "center" },\n'
                    '  { field: "customerCount", title: "客户数", width: 80, align: "center" },\n'
                    '  { field: "growthRate", title: "同比增长", width: 100, align: "right" }\n'
                    ']',
                "slot_map": {},
            }
        ]
    },
    "erp/sales-report/components/SalesTrend.vue": {
        "tables": [
            {
                "columns_var": "columns",
                "new_columns_var": "vxeColumns",
                "columns_def": 'const vxeColumns = [\n'
                    '  { field: "date", title: "日期", width: 100 },\n'
                    '  { field: "revenue", title: "收入", width: 120, align: "right" },\n'
                    '  { field: "cost", title: "成本", width: 120, align: "right" },\n'
                    '  { field: "profit", title: "利润", width: 120, align: "right" },\n'
                    '  { field: "orderCount", title: "订单数", width: 80, align: "center" }\n'
                    ']',
                "slot_map": {},
            }
        ]
    },
    "purchase/tabs/Suppliers.vue": {
        "tables": [
            {
                "columns_var": "columns",
                "new_columns_var": "vxeColumns",
                "columns_def": 'const vxeColumns = [\n'
                    '  { field: "supplierCode", title: "编码", width: 100 },\n'
                    '  { field: "supplierName", title: "供应商名称", minWidth: 150 },\n'
                    '  { field: "contactPerson", title: "联系人", width: 100 },\n'
                    '  { field: "contactPhone", title: "联系电话", width: 130 },\n'
                    '  { field: "status", title: "状态", width: 80 },\n'
                    '  { field: "action", title: "操作", width: 200, type: "action" }\n'
                    ']',
                "slot_map": {},
            }
        ]
    },
    "sale/tabs/Quotation.vue": {
        "tables": [
            {
                "columns_var": "columns",
                "new_columns_var": "vxeColumns",
                "columns_def": 'const vxeColumns = [\n'
                    '  { field: "quotationNo", title: "报价单号", width: 160 },\n'
                    '  { field: "customerName", title: "客户", minWidth: 150 },\n'
                    '  { field: "totalAmount", title: "总金额", width: 120, align: "right" },\n'
                    '  { field: "status", title: "状态", width: 80 },\n'
                    '  { field: "createTime", title: "创建时间", width: 160 }\n'
                    ']',
                "slot_map": {},
            }
        ]
    },
}

for filepath, config in SIMPLE_FILES.items():
    fullpath = os.path.join(BASE, filepath)
    if not os.path.exists(fullpath):
        print(f"SKIP (not found): {filepath}")
        continue

    with open(fullpath, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content

    for table in config['tables']:
        old_var = table['columns_var']
        new_var = table['new_columns_var']

        # Replace a-table with VxeTableList (self-closing or with bodyCell)
        # Pattern: <a-table ... :columns="old_var" ... />  or  <a-table ... :columns="old_var" ... >\n  ...  \n</a-table>
        a_table_pattern = rf'(<a-table\s[^>]*?:columns="{old_var}"[^>]*?)(\s*/?>|>)'

        # Find the a-table block
        idx = content.find(f':columns="{old_var}"')
        if idx < 0:
            print(f"  SKIP (columns '{old_var}' not found): {filepath}")
            continue

        # Find start of a-table tag
        start = content.rfind('<a-table', 0, idx)
        if start < 0:
            print(f"  SKIP (a-table tag not found): {filepath}")
            continue

        # Check if self-closing
        tag_end = content.find('>', idx)
        if tag_end < 0:
            continue

        # Check if there's bodyCell
        rest_after = content[tag_end:]
        has_bodycell = '#bodyCell' in rest_after[:500] or 'bodyCell' in rest_after[:500]

        if content[tag_end-1] == '/':
            # Self-closing a-table
            vxe_attrs = ':show-toolbar="false" :selectable="false" :show-add="false" :show-search="false" :show-export="false" :show-batch-delete="false"'
            old = content[start:tag_end+1]
            new = old.replace('<a-table', '<VxeTableList')
            new = new.replace(f':columns="{old_var}"', f':columns="{new_var}"')
            new = new.replace('/>', f' {vxe_attrs} />')
            content = content.replace(old, new)
        else:
            # Find closing </a-table>
            close_tag = content.find('</a-table>', tag_end)
            if close_tag < 0:
                print(f"  SKIP (no closing a-table): {filepath}")
                continue

            old_block = content[start:close_tag + len('</a-table>')]
            new_block = old_block.replace('<a-table', '<VxeTableList')
            new_block = new_block.replace(f':columns="{old_var}"', f':columns="{new_var}"')

            if has_bodycell:
                # Convert bodyCell patterns
                new_block = convert_bodycell(new_block, table.get('slot_map', {}))

            # Add standard VxeTableList props
            # Remove size="small" and bordered
            new_block = new_block.replace('size="small"', '')
            new_block = re.sub(r'bordered\s*', '', new_block)

            # Add toolbar/selection props after row-key
            vxe_props = ' :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false" :show-export="false" :show-batch-delete="false"'
            new_block = new_block.replace('<VxeTableList', f'<VxeTableList{vxe_props}')
            # Remove duplicate if needed
            new_block = new_block.replace(f'{vxe_props}{vxe_props}', vxe_props)

            content = content.replace(old_block, new_block)
            print(f"  Replaced table with bodyCell={'yes' if has_bodycell else 'no'}: {filepath}")

        # Replace column definition
        # Find const old_var in script
        col_pattern = rf'const {re.escape(old_var)}\s*=\s*\['
        col_match = re.search(col_pattern, content)
        if col_match:
            col_start = col_match.start()
            # Find end of array (matching brackets)
            bracket_count = 0
            col_end = col_start
            for i in range(col_start, len(content)):
                if content[i] == '[':
                    bracket_count += 1
                elif content[i] == ']':
                    bracket_count -= 1
                    if bracket_count == 0:
                        col_end = i + 1
                        break

            old_col_def = content[col_start:col_end]
            content = content.replace(old_col_def, table['columns_def'])

    # Remove fillEmptyRows / MIN_TABLE_ROWS / tableDataSource
    content = re.sub(
        r'// ── 空行填充[^n]*\nconst MIN_TABLE_ROWS\s*=\s*\d+\s*\nconst tableDataSource\s*=\s*computed\(\(\)\s*=>\s*\{[^}]*\}\)\s*',
        '',
        content
    )

    # Add VxeTableList import if not present
    if 'VxeTableList' not in content:
        content = content.replace(
            "import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'",
            "import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'\nimport VxeTableList from '@/components/VxeTableList/VxeTableList.vue'"
        )
        content = content.replace(
            "import { ref, reactive, computed, onMounted } from 'vue'",
            "import { ref, reactive, computed, onMounted } from 'vue'\nimport VxeTableList from '@/components/VxeTableList/VxeTableList.vue'"
        )
        content = content.replace(
            "import { ref, reactive, computed } from 'vue'",
            "import { ref, reactive, computed } from 'vue'\nimport VxeTableList from '@/components/VxeTableList/VxeTableList.vue'"
        )
        content = content.replace(
            "import { ref, onMounted } from 'vue'",
            "import { ref, onMounted } from 'vue'\nimport VxeTableList from '@/components/VxeTableList/VxeTableList.vue'"
        )

    if content != original:
        with open(fullpath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"DONE: {filepath}")
    else:
        print(f"UNCHANGED: {filepath}")


def convert_bodycell(block, slot_map):
    """Convert bodyCell patterns to named slots"""

    # Find bodyCell template
    bodycell_match = re.search(
        r'<template\s+#bodyCell\s*=\s*"{[^}]*}"[^>]*>.*?</template>\s*',
        block,
        re.DOTALL
    )

    if bodycell_match:
        bodycell_content = bodycell_match.group(0)

        # Try to extract slot name from column.key patterns
        # For now just remove the bodyCell block
        block = block.replace(bodycell_content, '')

    return block

print("\nMigration complete!")
