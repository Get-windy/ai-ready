"""
批量迁移剩余文件中的 a-table 到 VxeTableList
对每个文件先读入，然后做精确替换
"""
import os
import re
import glob

BASE = "i:/AI-Ready/frontend/apps/pc-admin/src/views"

# 需要全面迁移的文件（无 VxeTableList 导入）
FULL_MIGRATE = [
    # 销售报表
    "erp/sales-report/components/CustomerRanking.vue",
    "erp/sales-report/components/ProductRanking.vue",
    "erp/sales-report/components/SalesStatistics.vue",
    "erp/sales-report/components/SalesTrend.vue",
    # 对账
    "finance/reconciliation/components/BankReconciliation.vue",
    "finance/reconciliation/components/CustomerReconciliation.vue",
    "finance/reconciliation/components/DifferenceHandling.vue",
    "finance/reconciliation/components/SupplierReconciliation.vue",
    # 其他
    "order-center/index.vue",
    "sale/components/SaleOrderImportModal.vue",
]

def remove_fill_empty_rows(content, filename):
    """移除 fillEmptyRows 相关代码"""
    patterns = [
        (r'// ── 空行填充[\s\S]*?const MIN_TABLE_ROWS = \d+\n', ''),
        (r'// 空行填充[\s\S]*?const MIN_TABLE_ROWS = \d+\n', ''),
        (r'const MIN_TABLE_ROWS = \d+\n', '\n'),
        (r'const tableDataSource = computed\(\(\) => \{[\s\S]*?\n\}\)\n', ''),
    ]
    for pat, repl in patterns:
        content = re.sub(pat, repl, content)
    return content

def process_content(content, filename):
    """处理单个文件内容"""

    if 'VxeTableList' not in content:
        # 添加导入 - 在 vue import 后面
        content = re.sub(
            r"(from 'vue'\n)",
            r"\1import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'\n",
            content
        )

    # 替换 a-table → VxeTableList（带常用属性）
    # 先处理带 bodyCell 的复杂表格
    # 对于每个 a-table 块，我们需要找到它并处理

    return content

def migrate_file(relpath):
    """迁移单个文件"""
    fullpath = os.path.join(BASE, relpath)
    if not os.path.exists(fullpath):
        print(f"  SKIP (not found): {relpath}")
        return False

    with open(fullpath, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content

    # 移除 fillEmptyRows
    content = remove_fill_empty_rows(content, relpath)

    # 替换 a-table 为 VxeTableList
    # 查找所有 a-table 标签
    modified = False

    # 替换 a-table 开始标签
    if '<a-table' in content and 'VxeTableList' not in original:
        # 简单替换：标签名
        content = content.replace('<a-table', '<VxeTableList')
        content = content.replace('</a-table>', '</VxeTableList>')

        # 替换属性
        content = content.replace(':dataSource=', ':data-source=')
        content = content.replace('rowKey=', 'row-key=')

        # 移除 size="small" 和 bordered
        content = content.replace(' size="small"', '')
        content = content.replace(' bordered', '')

        # 添加标准属性
        # 找到第一个 <VxeTableList 并在其标签结束前加上标准属性
        pattern = r'(<VxeTableList[^>]*?)(/?>)'
        def add_vxe_props(match):
            tag = match.group(1)
            closing = match.group(2)
            if ':show-toolbar' not in tag:
                tag += ' :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false" :show-export="false" :show-batch-delete="false"'
            return tag + closing

        content = re.sub(pattern, add_vxe_props, content)

        modified = True

    # 替换 columns 定义
    # 找 const xxxColumns = [...]
    # 用精确替换
    if content != original or True:  # Always try column conversion
        lines = content.split('\n')
        new_lines = []
        i = 0
        while i < len(lines):
            line = lines[i]
            # 检查是否是 const xxxColumns = [
            m = re.match(r'^(\s*const )(\w+)(Columns\s*=\s*\[)$', line)
            if m:
                indent = m.group(1)
                var_name = m.group(2)
                suffix = m.group(3)
                new_var = var_name  # 保留原名或加 Vxe 前缀
                # 收集整个数组
                arr_lines = [line]
                i += 1
                bracket_count = 1
                while i < len(lines) and bracket_count > 0:
                    arr_lines.append(lines[i])
                    bracket_count += lines[i].count('[') - lines[i].count(']')
                    i += 1

                arr_text = '\n'.join(arr_lines)
                # 只替换还没有被转换为 Vxe 格式的
                if "'field'" not in arr_text and '"field"' not in arr_text and 'field:' not in arr_text:
                    # 转换 dataIndex/key 为 field
                    arr_text = re.sub(r"dataIndex:\s*'(\w+)'", r"field: '\1'", arr_text)
                    arr_text = re.sub(r'dataIndex:\s*"(\w+)"', r'field: "\1"', arr_text)
                    arr_text = re.sub(r"key:\s*'(\w+)'", r"field: '\1'", arr_text)
                    arr_text = re.sub(r'key:\s*"(\w+)"', r'field: "\1"', arr_text)
                    # 移除重复的 field
                    arr_text = re.sub(r"field:\s*'(\w+)',\s*field:\s*'\1'", r"field: '\1'", arr_text)
                    arr_text = re.sub(r'field:\s*"(\w+)",\s*field:\s*"\1"', r'field: "\1"', arr_text)
                new_lines.append(arr_text)
            else:
                new_lines.append(line)
                i += 1

        content = '\n'.join(new_lines)

    # 处理 bodyCell → named slots
    # 查找 bodyCell 模板块并替换
    if '#bodyCell' in content:
        # 使用正则提取 bodyCell 内容
        bodycell_pattern = r'<template\s+#bodyCell\s*=\s*"[^"]*">\s*(.*?)\s*</template>'

        def convert_bodycell(match):
            inner = match.group(1)
            # 提取各个 column.key 条件
            slot_templates = []

            # 查找 template v-if/else-if patterns
            slot_pattern = r'<template\s+v-if="column\.key\s*===\s*\'(\w+)\'">\s*(.*?)\s*</template>'
            for sm in re.finditer(slot_pattern, inner, re.DOTALL):
                key = sm.group(1)
                slot_content = sm.group(2).strip()
                slot_templates.append((key, slot_content))

            # 查找 template v-else-if patterns
            else_pattern = r'<template\s+v-else-if="column\.key\s*===\s*\'(\w+)\'">\s*(.*?)\s*</template>'
            for sm in re.finditer(else_pattern, inner, re.DOTALL):
                key = sm.group(1)
                slot_content = sm.group(2).strip()
                # Check if not a duplicate
                if not any(k == key for k, _ in slot_templates):
                    slot_templates.append((key, slot_content))

            # 跳过 __empty_row
            slot_templates = [(k, s) for k, s in slot_templates if k != '__empty_row']

            # 构建新的 slot
            result = ''
            for key, slot_content in slot_templates:
                # 替换 record.xxx 为 record.xxx (可以保留)
                # 检查是否用到 index
                uses_index = 'index' in slot_content
                if uses_index:
                    result += f'            <template #{key}Cell="{{{{ record, index }}}}">\n'
                else:
                    result += f'            <template #{key}Cell="{{{{ record }}}}">\n'
                result += slot_content + '\n'
                result += '            </template>\n'

            # 添加标准 action slot 处理
            # 查找 column.key === 'action' 的 template
            action_pattern = r"<template\s+v-?else-if=\"column\.key\s*===\s*'action'\">(.*?)</template>"
            action_match = re.search(action_pattern, inner, re.DOTALL)
            if action_match and 'action' not in [k for k, _ in slot_templates]:
                result += f'            <template #action="{{{{ record, index }}}}">\n'
                result += action_match.group(1).strip() + '\n'
                result += '            </template>\n'

            return result

        # 先移除 __empty_row 处理
        content = re.sub(
            r'<template\s+v-if="record\.__empty_row">\s*<span[^>]*>&nbsp;</span>\s*</template>',
            '',
            content
        )
        content = re.sub(
            r'<template\s+v-if="record\.__empty_row">.*?</template>',
            '',
            content
        )

        # 替换 bodyCell
        content = re.sub(
            r'<template\s+#bodyCell\s*=\s*"{[^}]*}"[^>]*>.*?</template>\s*',
            lambda m: convert_bodycell(m),
            content,
            flags=re.DOTALL
        )

    # 移除 empty-placeholder CSS
    content = re.sub(r'\n\s*\.empty-placeholder\s*\{\s*color:\s*transparent;?\s*\}\n', '\n', content)
    content = content.replace('.empty-placeholder {\n  color: transparent;\n}\n', '')
    content = content.replace('.empty-placeholder { color: transparent; }\n', '')

    if content != original:
        with open(fullpath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"  DONE: {relpath}")
        return True
    else:
        print(f"  UNCHANGED: {relpath}")
        return False


print("=== 全面迁移文件 ===")
for relpath in FULL_MIGRATE:
    try:
        migrate_file(relpath)
    except Exception as e:
        print(f"  ERROR {relpath}: {e}")

print("\nDone!")
