import os, re

views = r'i:\AI-Ready\frontend\apps\pc-admin\src\views'

# ── 1. stock/detail/StockDetail.vue (simple, no bodyCell) ──
fp = os.path.join(views, r'stock\detail\StockDetail.vue')
with open(fp, 'r', encoding='utf-8') as f:
    c = f.read()

# Replace a-table
c = c.replace(
    '<a-table :columns="txCols" :data-source="transactions" row-key="id" :pagination="{ pageSize: 5 }" size="small" />',
    '<VxeTableList\n        :columns="txVxeCols"\n        :data-source="transactions"\n        row-key="id"\n        :pagination="{ pageSize: 5 }"\n        :show-toolbar="false"\n        :selectable="false"\n        :show-add="false"\n        :show-search="false"\n        :show-export="false"\n        :show-batch-delete="false"\n      />'
)
# Convert columns
c = c.replace(
    'const txCols = [\n  { title: \'单号\', dataIndex: \'orderNo\', width: 160 }, { title: \'类型\', dataIndex: \'type\', width: 80 },\n  { title: \'数量\', dataIndex: \'quantity\', width: 80 }, { title: \'时间\', dataIndex: \'createTime\', width: 160 }\n]',
    'const txVxeCols = [\n  { field: \'orderNo\', title: \'单号\', width: 160 },\n  { field: \'type\', title: \'类型\', width: 80 },\n  { field: \'quantity\', title: \'数量\', width: 80 },\n  { field: \'createTime\', title: \'时间\', width: 160 },\n]'
)
# Add VxeTableList import
c = c.replace(
    "import { stockApi, inboundApi, outboundApi, type StockItem } from '@/api/erp'",
    "import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'\nimport { stockApi, inboundApi, outboundApi, type StockItem } from '@/api/erp'"
)
with open(fp, 'w', encoding='utf-8') as f:
    f.write(c)
print('stock/detail/StockDetail.vue done')

# ── 2. purchase/detail/OrderDetail.vue ──
fp = os.path.join(views, r'purchase\detail\OrderDetail.vue')
with open(fp, 'r', encoding='utf-8') as f:
    c = f.read()

old = '''      <a-table
        :columns="detailColumns"
        :data-source="order?.details || []"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            ¥{{ record.amount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'taxAmount'">
            ¥{{ record.taxAmount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            ¥{{ record.totalAmount?.toFixed(2) }}
          </template>
        </template>
      </a-table>'''

new = '''      <VxeTableList
        :columns="detailVxeColumns"
        :data-source="order?.details || []"
        row-key="id"
        :pagination="false"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #amountCell="{ record }">
          ¥{{ record.amount?.toFixed(2) }}
        </template>
        <template #taxAmountCell="{ record }">
          ¥{{ record.taxAmount?.toFixed(2) }}
        </template>
        <template #totalAmountCell="{ record }">
          ¥{{ record.totalAmount?.toFixed(2) }}
        </template>
      </VxeTableList>'''

c = c.replace(old, new)

# Convert columns
old = '''const detailColumns = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 150 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格型号', dataIndex: 'specification', key: 'specification', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '税率', dataIndex: 'taxRate', key: 'taxRate', width: 80 },
  { title: '税额', dataIndex: 'taxAmount', key: 'taxAmount', width: 100 },'''

new = '''const detailVxeColumns = [
  { field: 'productCode', title: '产品编码', width: 150 },
  { field: 'productName', title: '产品名称' },
  { field: 'specification', title: '规格型号', width: 120 },
  { field: 'quantity', title: '数量', width: 80 },
  { field: 'unitPrice', title: '单价', width: 100 },
  { field: 'amount', title: '金额', width: 100, slotName: 'amountCell' },
  { field: 'taxRate', title: '税率', width: 80 },
  { field: 'taxAmount', title: '税额', width: 100, slotName: 'taxAmountCell' },'''

c = c.replace(old, new)

# Find and replace totalAmount column definition
old = '  { title: \'价税合计\', dataIndex: \'totalAmount\', key: \'totalAmount\', width: 100 }'
new = '  { field: \'totalAmount\', title: \'价税合计\', width: 100, slotName: \'totalAmountCell\' }'
c = c.replace(old, new)

# Add import
c = c.replace(
    "import { purchaseOrderApi, type PurchaseOrder } from '@/api/purchase'",
    "import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'\nimport { purchaseOrderApi, type PurchaseOrder } from '@/api/purchase'"
)
with open(fp, 'w', encoding='utf-8') as f:
    f.write(c)
print('purchase/detail/OrderDetail.vue done')

# ── 3. purchase/detail/inbound/InboundDetail.vue ──
fp = os.path.join(views, r'purchase\detail\inbound\InboundDetail.vue')
with open(fp, 'r', encoding='utf-8') as f:
    c = f.read()

old = '''  <a-table
    :columns="detailColumns"
    :data-source="items"
    row-key="id"
    :pagination="false"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'amount'">
        ¥{{ record.amount?.toFixed(2) }}
      </template>
    </template>
  </a-table>'''

new = '''  <VxeTableList
    :columns="detailVxeColumns"
    :data-source="items"
    row-key="id"
    :pagination="false"
    :show-toolbar="false"
    :selectable="false"
    :show-add="false"
    :show-search="false"
    :show-export="false"
    :show-batch-delete="false"
  >
    <template #amountCell="{ record }">
      ¥{{ record.amount?.toFixed(2) }}
    </template>
  </VxeTableList>'''

c = c.replace(old, new)

old = '''const detailColumns = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode' },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格型号', dataIndex: 'specification', key: 'specification' },
  { title: '数量', dataIndex: 'quantity', key: 'quantity' },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice' },
  { title: '金额', key: 'amount' },
]'''

new = '''const detailVxeColumns = [
  { field: 'productCode', title: '产品编码' },
  { field: 'productName', title: '产品名称' },
  { field: 'specification', title: '规格型号' },
  { field: 'quantity', title: '数量' },
  { field: 'unitPrice', title: '单价' },
  { field: 'amount', title: '金额', slotName: 'amountCell' },
]'''

c = c.replace(old, new)

c = c.replace(
    'import { inboundApi } from \'@/api/erp\'',
    'import VxeTableList from \'@/components/VxeTableList/VxeTableList.vue\'\nimport { inboundApi } from \'@/api/erp\''
)
with open(fp, 'w', encoding='utf-8') as f:
    f.write(c)
print('purchase/detail/inbound/InboundDetail.vue done')

# ── 4. purchase/detail/inquiry/InquiryDetail.vue ──
fp = os.path.join(views, r'purchase\detail\inquiry\InquiryDetail.vue')
with open(fp, 'r', encoding='utf-8') as f:
    c = f.read()

# Need to read whole file first to understand both a-tables
# Let me find them
if '<a-table' in c:
    # Replace first a-table (quotation items)
    old = '''<a-table
      :columns="quotationColumns"
      :data-source="quotationData"
      row-key="id"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'amount' || column.key === 'unitPrice'">
          ¥{{ record[column.key]?.toFixed(2) }}
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="inqStatusColor(record.status)">{{ inqStatusText(record.status) }}</a-tag>
        </template>
      </template>
    </a-table>'''

    new = '''<VxeTableList
      :columns="quotationVxeColumns"
      :data-source="quotationData"
      row-key="id"
      :pagination="false"
      :show-toolbar="false"
      :selectable="false"
      :show-add="false"
      :show-search="false"
      :show-export="false"
      :show-batch-delete="false"
    >
      <template #unitPriceCell="{ record }">
        ¥{{ record.unitPrice?.toFixed(2) }}
      </template>
      <template #amountCell="{ record }">
        ¥{{ record.amount?.toFixed(2) }}
      </template>
      <template #statusCell="{ record }">
        <a-tag :color="inqStatusColor(record.status)">{{ inqStatusText(record.status) }}</a-tag>
      </template>
    </VxeTableList>'''

    c = c.replace(old, new)

    # Replace second a-table (sourcing results)
    old = '''<a-table
      :columns="sourcingColumns"
      :data-source="sourcingData"
      row-key="id"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'totalAmount'">
          ¥{{ record.totalAmount?.toFixed(2) }}
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="srcStatusColor(record.status)">{{ srcStatusText(record.status) }}</a-tag>
        </template>
      </template>
    </a-table>'''

    new = '''<VxeTableList
      :columns="sourcingVxeColumns"
      :data-source="sourcingData"
      row-key="id"
      :pagination="false"
      :show-toolbar="false"
      :selectable="false"
      :show-add="false"
      :show-search="false"
      :show-export="false"
      :show-batch-delete="false"
    >
      <template #totalAmountCell="{ record }">
        ¥{{ record.totalAmount?.toFixed(2) }}
      </template>
      <template #statusCell="{ record }">
        <a-tag :color="srcStatusColor(record.status)">{{ srcStatusText(record.status) }}</a-tag>
      </template>
    </VxeTableList>'''

    c = c.replace(old, new)

    # Convert column definitions
    old = '''const quotationColumns = [
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 150 },
  { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson' },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone' },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice' },
  { title: '数量', dataIndex: 'quantity', key: 'quantity' },
  { title: '金额', dataIndex: 'amount', key: 'amount' },
  { title: '交货期', dataIndex: 'deliveryDate', key: 'deliveryDate' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]'''

    new = '''const quotationVxeColumns = [
  { field: 'supplierName', title: '供应商', width: 150 },
  { field: 'contactPerson', title: '联系人' },
  { field: 'contactPhone', title: '联系电话' },
  { field: 'unitPrice', title: '单价', slotName: 'unitPriceCell' },
  { field: 'quantity', title: '数量' },
  { field: 'amount', title: '金额', slotName: 'amountCell' },
  { field: 'deliveryDate', title: '交货期' },
  { field: 'status', title: '状态', slotName: 'statusCell' },
]'''

    c = c.replace(old, new)

    old = '''const sourcingColumns = [
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '评分', dataIndex: 'score', key: 'score' },
  { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount' },
  { title: '状态', dataIndex: 'status', key: 'status' },
]'''

    new = '''const sourcingVxeColumns = [
  { field: 'supplierName', title: '供应商' },
  { field: 'score', title: '评分' },
  { field: 'totalAmount', title: '总金额', slotName: 'totalAmountCell' },
  { field: 'status', title: '状态', slotName: 'statusCell' },
]'''

    c = c.replace(old, new)

    # Add import
    old_import = "import { inquiryApi } from '@/api/erp'"
    new_import = "import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'\nimport { inquiryApi } from '@/api/erp'"
    # Find which import line to use
    for line_end in ("import { inquiryApi } from '@/api/erp'", "import { inquiryApi } from '@/api/erp'", "import { inquiryApi } from '@/api/erp'"):
        if line_end in c:
            c = c.replace(line_end, new_import)
            break

    with open(fp, 'w', encoding='utf-8') as f:
        f.write(c)
print('purchase/detail/inquiry/InquiryDetail.vue done')

# ── 5. sale/detail/OrderDetail.vue ──
fp = os.path.join(views, r'sale\detail\OrderDetail.vue')
with open(fp, 'r', encoding='utf-8') as f:
    c = f.read()

old = '''<a-table
        :columns="detailColumns"
        :data-source="order?.details || []"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            ¥{{ record.amount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'taxAmount'">
            ¥{{ record.taxAmount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            ¥{{ record.totalAmount?.toFixed(2) }}
          </template>
        </template>
      </a-table>'''

new = '''<VxeTableList
        :columns="detailVxeColumns"
        :data-source="order?.details || []"
        row-key="id"
        :pagination="false"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #amountCell="{ record }">
          ¥{{ record.amount?.toFixed(2) }}
        </template>
        <template #taxAmountCell="{ record }">
          ¥{{ record.taxAmount?.toFixed(2) }}
        </template>
        <template #totalAmountCell="{ record }">
          ¥{{ record.totalAmount?.toFixed(2) }}
        </template>
      </VxeTableList>'''

c = c.replace(old, new)

old_cols = '''const detailColumns = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 150 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格型号', dataIndex: 'specification', key: 'specification', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '折扣(%)', dataIndex: 'discount', key: 'discount', width: 80 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '税率', dataIndex: 'taxRate', key: 'taxRate', width: 80 },
  { title: '税额', dataIndex: 'taxAmount', key: 'taxAmount', width: 100 },
  { title: '价税合计', dataIndex: 'totalAmount', key: 'totalAmount', width: 100 },
]'''

new_cols = '''const detailVxeColumns = [
  { field: 'productCode', title: '产品编码', width: 150 },
  { field: 'productName', title: '产品名称' },
  { field: 'specification', title: '规格型号', width: 120 },
  { field: 'quantity', title: '数量', width: 80 },
  { field: 'unitPrice', title: '单价', width: 100 },
  { field: 'discount', title: '折扣(%)', width: 80 },
  { field: 'amount', title: '金额', width: 100, slotName: 'amountCell' },
  { field: 'taxRate', title: '税率', width: 80 },
  { field: 'taxAmount', title: '税额', width: 100, slotName: 'taxAmountCell' },
  { field: 'totalAmount', title: '价税合计', width: 100, slotName: 'totalAmountCell' },
]'''

c = c.replace(old_cols, new_cols)

c = c.replace(
    "import { saleOrderApi, type SaleOrder } from '@/api/sale'",
    "import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'\nimport { saleOrderApi, type SaleOrder } from '@/api/sale'"
)
with open(fp, 'w', encoding='utf-8') as f:
    f.write(c)
print('sale/detail/OrderDetail.vue done')

# ── 6. finance/voucher/VoucherDetail.vue ──
fp = os.path.join(views, r'finance\voucher\VoucherDetail.vue')
with open(fp, 'r', encoding='utf-8') as f:
    c = f.read()

old = '''  <a-table
    :columns="entryColumns"
    :data-source="entries"
    :pagination="false"
    size="small"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'debitAmount' || column.key === 'creditAmount'">
        {{ (record[column.key] || 0).toFixed(2) }}
      </template>
    </template>
  </a-table>'''

new = '''  <VxeTableList
    :columns="entryVxeColumns"
    :data-source="entries"
    :pagination="false"
    :show-toolbar="false"
    :selectable="false"
    :show-add="false"
    :show-search="false"
    :show-export="false"
    :show-batch-delete="false"
  />'''

c = c.replace(old, new)

old = '''const entryColumns = [
  { title: '摘要', dataIndex: 'summary', key: 'summary' },
  { title: '科目编码', dataIndex: 'subjectCode', key: 'subjectCode' },
  { title: '科目名称', dataIndex: 'subjectName', key: 'subjectName' },
  { title: '借方金额', dataIndex: 'debitAmount', key: 'debitAmount', align: 'right' as const },
  { title: '贷方金额', dataIndex: 'creditAmount', key: 'creditAmount', align: 'right' as const },
]'''

new = '''const amountFmt = ({ cellValue }: any) => (cellValue || 0).toFixed(2)

const entryVxeColumns = [
  { field: 'summary', title: '摘要' },
  { field: 'subjectCode', title: '科目编码' },
  { field: 'subjectName', title: '科目名称' },
  { field: 'debitAmount', title: '借方金额', align: 'right', formatter: amountFmt },
  { field: 'creditAmount', title: '贷方金额', align: 'right', formatter: amountFmt },
]'''

c = c.replace(old, new)

c = c.replace(
    "import { voucherApi, type Voucher } from '@/api/finance'",
    "import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'\nimport { voucherApi, type Voucher } from '@/api/finance'"
)
with open(fp, 'w', encoding='utf-8') as f:
    f.write(c)
print('finance/voucher/VoucherDetail.vue done')

# ── 7. finance/accounts-payable/components/PendingApproval.vue ──
fp = os.path.join(views, r'finance\accounts-payable\components\PendingApproval.vue')
with open(fp, 'r', encoding='utf-8') as f:
    c = f.read()

old = '''      <a-table
        :columns="columns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="record.__empty_row">
            <span class="empty-placeholder">&nbsp;</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleApprove(record)">审批</a-button>
              <a-button type="link" size="small" danger @click="handleReject(record)">拒绝</a-button>
              <a-button type="link" size="small" @click="handleViewDetail(record)">查看</a-button>
            </a-space>
          </template>
        </template>
      </a-table>'''

new = '''      <VxeTableList
        :columns="vxeColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
        @page-change="handlePageChange"
      >
        <template #action="{ record }">
          <a-space>
            <a-button type="link" size="small" @click="handleApprove(record)">审批</a-button>
            <a-button type="link" size="small" danger @click="handleReject(record)">拒绝</a-button>
            <a-button type="link" size="small" @click="handleViewDetail(record)">查看</a-button>
          </a-space>
        </template>
      </VxeTableList>'''

c = c.replace(old, new)

# Remove fillEmptyRows
import re
c = re.sub(r'\n\s*// ── .*?\n\s*const MIN_TABLE_ROWS = \d+\n\s*const tableDataSource = computed\(\(\) => \{\n\s*const data = \[\.\.\.tableData\.value\]\n\s*const emptyCount = Math\.max\(0, MIN_TABLE_ROWS - data\.length\)\n\s*for \(let i = 0; i < emptyCount; i\+\+\) \{\n\s*data\.push\(\{ __empty_row: true, id: `__empty_\$\{i\}` \}\)\n\s*\}\n\s*return data\n\s*\}\)', '', c)

# Convert columns
old = '''const columns = [
  { title: '付款单号', dataIndex: 'paymentNo', key: 'paymentNo', width: 160 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 140 },
  { title: '付款金额', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' as const },
  { title: '申请日期', dataIndex: 'applyDate', key: 'applyDate', width: 100 },
  { title: '申请人', dataIndex: 'applicant', key: 'applicant', width: 90 },
  { title: '付款方式', dataIndex: 'paymentMethod', key: 'paymentMethod', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 220, fixed: 'right' as const },
]'''

new = '''const vxeColumns = [
  { field: 'paymentNo', title: '付款单号', width: 160 },
  { field: 'supplierName', title: '供应商', width: 140 },
  { field: 'amount', title: '付款金额', width: 120, align: 'right' },
  { field: 'applyDate', title: '申请日期', width: 100 },
  { field: 'applicant', title: '申请人', width: 90 },
  { field: 'paymentMethod', title: '付款方式', width: 100 },
  { field: 'remark', title: '备注', ellipsis: true },
  { field: 'action', title: '操作', width: 220, fixed: 'right', type: 'action' },
]'''

c = c.replace(old, new)

# Replace handleTableChange
c = c.replace(
    'const handleTableChange: TableProps[\'onChange\'] = (pag) => {',
    'const handlePageChange = (page: number, size: number) => {'
)
c = c.replace(
    '  pagination.current = pag.current || 1\n  pagination.pageSize = pag.pageSize || 10',
    '  pagination.current = page\n  pagination.pageSize = size'
)

# Add import
c = c.replace(
    "import request from '@/utils/request'",
    "import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'\nimport request from '@/utils/request'"
)

# Remove unused imports
c = c.replace("import type { TableProps } from 'ant-design-vue'\n", "")

with open(fp, 'w', encoding='utf-8') as f:
    f.write(c)
print('finance/accounts-payable/components/PendingApproval.vue done')

# ── 8. finance/accounts-payable/components/ApprovedApproval.vue ──
fp = os.path.join(views, r'finance\accounts-payable\components\ApprovedApproval.vue')
with open(fp, 'r', encoding='utf-8') as f:
    c = f.read()

# Same pattern as PendingApproval
old = '''      <a-table
        :columns="columns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="record.__empty_row">
            <span class="empty-placeholder">&nbsp;</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="handleViewDetail(record)">查看</a-button>
          </template>
        </template>
      </a-table>'''

new = '''      <VxeTableList
        :columns="vxeColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
        @page-change="handlePageChange"
      >
        <template #action="{ record }">
          <a-button type="link" size="small" @click="handleViewDetail(record)">查看</a-button>
        </template>
      </VxeTableList>'''

c = c.replace(old, new)

# Remove fillEmptyRows
c = re.sub(r'\n\s*// ── .*?\n\s*const MIN_TABLE_ROWS = \d+\n\s*const tableDataSource = computed\(\(\) => \{\n\s*const data = \[\.\.\.tableData\.value\]\n\s*const emptyCount = Math\.max\(0, MIN_TABLE_ROWS - data\.length\)\n\s*for \(let i = 0; i < emptyCount; i\+\+\) \{\n\s*data\.push\(\{ __empty_row: true, id: `__empty_\$\{i\}` \}\)\n\s*\}\n\s*return data\n\s*\}\)', '', c)

# Convert columns
old = '''const columns = [
  { title: '付款单号', dataIndex: 'paymentNo', key: 'paymentNo', width: 160 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 140 },
  { title: '付款金额', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' as const },
  { title: '审批日期', dataIndex: 'approveDate', key: 'approveDate', width: 100 },
  { title: '审批人', dataIndex: 'approver', key: 'approver', width: 90 },
  { title: '付款方式', dataIndex: 'paymentMethod', key: 'paymentMethod', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
]'''

new = '''const vxeColumns = [
  { field: 'paymentNo', title: '付款单号', width: 160 },
  { field: 'supplierName', title: '供应商', width: 140 },
  { field: 'amount', title: '付款金额', width: 120, align: 'right' },
  { field: 'approveDate', title: '审批日期', width: 100 },
  { field: 'approver', title: '审批人', width: 90 },
  { field: 'paymentMethod', title: '付款方式', width: 100 },
  { field: 'remark', title: '备注', ellipsis: true },
  { field: 'action', title: '操作', width: 100, fixed: 'right', type: 'action' },
]'''

c = c.replace(old, new)

# Replace handleTableChange
c = c.replace(
    'const handleTableChange: TableProps[\'onChange\'] = (pag) => {',
    'const handlePageChange = (page: number, size: number) => {'
)
c = c.replace(
    '  pagination.current = pag.current || 1\n  pagination.pageSize = pag.pageSize || 10',
    '  pagination.current = page\n  pagination.pageSize = size'
)

# Add import
c = c.replace(
    "import request from '@/utils/request'",
    "import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'\nimport request from '@/utils/request'"
)

# Remove unused imports
c = c.replace("import type { TableProps } from 'ant-design-vue'\n", "")

with open(fp, 'w', encoding='utf-8') as f:
    f.write(c)
print('finance/accounts-payable/components/ApprovedApproval.vue done')

print('\nAll done!')
