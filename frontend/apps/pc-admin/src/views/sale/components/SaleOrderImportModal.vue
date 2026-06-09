<template>
  <a-modal
    :open="open"
    title="导入销售订单"
    :width="800"
    :footer="null"
    :destroy-on-close="true"
    @cancel="handleClose"
  >
    <!-- ========== Step 1: 上传文件 ========== -->
    <div v-if="step === 1" class="import-step">
      <div
        class="upload-area"
        @dragover.prevent
        @drop.prevent="handleDrop"
        @click="triggerFileInput"
      >
        <InboxOutlined style="font-size: 48px; color: var(--color-primary, #1890ff)" />
        <p style="font-size: 14px; color: #333; margin-top: 12px;">点击或拖拽文件到此区域上传</p>
        <p style="font-size: 12px; color: #999;">支持 .csv 格式（UTF-8 编码）</p>
      </div>
      <input
        ref="fileInputRef"
        type="file"
        accept=".csv"
        style="display: none"
        @change="handleFileSelect"
      />
      <div style="text-align: center; margin-top: 12px;">
        <a-button type="link" @click="downloadTemplate">下载导入模板</a-button>
      </div>
    </div>

    <!-- ========== Step 2: 预览与确认 ========== -->
    <div v-if="step === 2" class="import-step">
      <a-alert
        :message="`已解析 ${parsedRows.length} 条数据`"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      >
        <template #description>
          系统将按以下字段匹配：<strong>订单号</strong>、<strong>客户</strong>、<strong>商品编码</strong>、<strong>数量</strong>、<strong>单价</strong>。
          客户和商品将尝试自动匹配系统数据。
        </template>
      </a-alert>

      <VxeTableList
        :columns="previewVxeColumns"
        :data-source="parsedRows.slice(0, 10)"
        :pagination="false"
        row-key="__rowIndex"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #statusCell="{ record }">
          <a-tag v-if="record.__valid" color="green">有效</a-tag>
          <a-tag v-else color="red" :title="record.__errors?.join('; ')">无效</a-tag>
        </template>
      </VxeTableList>

      <p v-if="parsedRows.length > 10" style="color: #999; font-size: 12px; margin-top: 8px;">
        仅显示前 10 条预览，共 {{ parsedRows.length }} 条
      </p>

      <div class="step-actions">
        <a-button :disabled="importing" @click="step = 1">返回重选</a-button>
        <a-button type="primary" :loading="importing" :disabled="validCount === 0" @click="handleImport">
          {{ importing ? `正在导入 ${importProgress.current}/${importProgress.total}` : '开始导入' }}
        </a-button>
      </div>

      <!-- 导入进度 -->
      <div v-if="importing" style="margin-top: 16px;">
        <a-progress
          :percent="Math.round((importProgress.current / importProgress.total) * 100)"
          :status="importProgress.hasError ? 'exception' : 'active'"
        />
        <p style="font-size: 12px; color: #666; margin-top: 4px;">
          成功: {{ importProgress.success }} /
          失败: {{ importProgress.fail }} /
          跳过: {{ importProgress.skip }}
        </p>
      </div>
    </div>

    <!-- ========== Step 3: 导入结果 ========== -->
    <div v-if="step === 3" class="import-step">
      <a-result
        :status="importResult.status"
        :title="importResult.title"
        :sub-title="importResult.subtitle"
      >
        <template #extra>
          <a-button type="primary" @click="handleDone">完成</a-button>
          <a-button v-if="importResult.hasErrors" @click="downloadErrorLog">下载错误日志</a-button>
        </template>
      </a-result>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { message } from 'ant-design-vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { saleOrderApi } from '@/api/erp'
import optionsApi from '@/api/options'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ (e: 'update:open', val: boolean): void; (e: 'success'): void }>()

const step = ref(1)
const importing = ref(false)
const fileInputRef = ref<HTMLInputElement>()
const parsedRows = ref<any[]>([])

const importProgress = reactive({
  current: 0,
  total: 0,
  success: 0,
  fail: 0,
  skip: 0,
  hasError: false,
  errors: [] as { row: number; message: string }[]
})

const importResult = reactive({
  status: 'success' as 'success' | 'warning' | 'error',
  title: '',
  subtitle: '',
  hasErrors: false
})

// 预览列
const previewVxeColumns = [
  { field: '__rowIndex', title: '行号', width: 60 },
  { field: 'orderNo', title: '订单号', width: 130 },
  { field: 'customerName', title: '客户', width: 100 },
  { field: 'productCode', title: '商品编码', width: 100 },
  { field: 'quantity', title: '数量', width: 60 },
  { field: 'unitPrice', title: '单价', width: 80 },
  { field: 'amount', title: '金额', width: 100 },
  { field: '__status', title: '状态', width: 60, slotName: 'statusCell' }
]

const validCount = computed(() => parsedRows.value.filter(r => r.__valid).length)

// 缓存查询数据
const customerMap = ref<Map<string, number>>(new Map())
const productMap = ref<Map<string, any>>(new Map())
const userMap = ref<Map<string, number>>(new Map())

async function loadReferenceData() {
  try {
    const [customers, products, users] = await Promise.all([
      optionsApi.getCustomers(),
      optionsApi.getProducts(),
      optionsApi.getUsers('salesman')
    ])
    const custList = Array.isArray(customers) ? customers : (customers as any)?.data ?? []
    const prodList = Array.isArray(products) ? products : (products as any)?.data ?? []
    const userList = Array.isArray(users) ? users : (users as any)?.data ?? []

    customerMap.value = new Map(custList.map((c: any) => [c.name, c.id]))
    productMap.value = new Map(prodList.map((p: any) => [p.code || p.productCode, p]))
    userMap.value = new Map(userList.map((u: any) => [u.name, u.id]))
  } catch {
    // 引用数据加载失败不影响后续操作
  }
}

// ====== CSV 解析 ======
function parseCSV(text: string): string[][] {
  const rows: string[][] = []
  let currentRow: string[] = []
  let currentField = ''
  let inQuotes = false

  for (let i = 0; i < text.length; i++) {
    const char = text[i]
    const nextChar = text[i + 1]

    if (inQuotes) {
      if (char === '"') {
        if (nextChar === '"') { currentField += '"'; i++ }
        else { inQuotes = false }
      } else { currentField += char }
    } else {
      if (char === '"') { inQuotes = true }
      else if (char === ',') { currentRow.push(currentField.trim()); currentField = '' }
      else if (char === '\n') {
        currentRow.push(currentField.trim())
        if (currentRow.length > 0 && currentRow.some(f => f !== '')) rows.push(currentRow)
        currentRow = []; currentField = ''
      } else if (char === '\r') { /* skip */ }
      else { currentField += char }
    }
  }
  currentRow.push(currentField.trim())
  if (currentRow.length > 0 && currentRow.some(f => f !== '')) rows.push(currentRow)
  return rows
}

// ====== 文件处理 ======
function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleDrop(e: DragEvent) {
  const file = e.dataTransfer?.files?.[0]
  if (file) processFile(file)
}

function handleFileSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) processFile(file)
  target.value = ''
}

function processFile(file: File) {
  if (!file.name.endsWith('.csv') && !file.name.endsWith('.CSV')) {
    message.warning('仅支持 .csv 格式文件')
    return
  }
  const reader = new FileReader()
  reader.onload = (e) => {
    const text = e.target?.result as string
    const rawRows = parseCSV(text)
    if (rawRows.length < 2) {
      message.error('文件为空或格式不正确')
      return
    }
    const headers = rawRows[0].map(h => h.trim())
    const dataRows = rawRows.slice(1)
    parsedRows.value = mapRows(headers, dataRows)
    step.value = 2
    loadReferenceData()
  }
  reader.onerror = () => message.error('文件读取失败')
  reader.readAsText(file)
}

// ====== 字段映射 ======
const FIELD_ALIASES: Record<string, string> = {
  '订单号': 'orderNo', 'orderNo': 'orderNo', 'orderno': 'orderNo', '订单编号': 'orderNo',
  '客户': 'customerName', '客户名称': 'customerName', 'customerName': 'customerName', 'customername': 'customerName',
  '客户ID': 'customerId', 'customerId': 'customerId',
  '商品编码': 'productCode', '产品编码': 'productCode', '编码': 'productCode',
  'productCode': 'productCode', 'productcode': 'productCode',
  '商品名称': 'productName', '产品名称': 'productName', 'productName': 'productName',
  '数量': 'quantity', 'quantity': 'quantity',
  '单价': 'unitPrice', 'unitPrice': 'unitprice', '价格': 'unitPrice',
  '折扣': 'discount', 'discount': 'discount',
  '订单日期': 'orderDate', '日期': 'orderDate', 'orderDate': 'orderdate',
  '销售员': 'salespersonName', 'salespersonName': 'salespersonName',
  '备注': 'remark', 'remark': 'remark'
}

function detectField(header: string): string {
  const trimmed = header.trim()
  return FIELD_ALIASES[trimmed] || FIELD_ALIASES[trimmed.toLowerCase()] || trimmed
}

function mapRows(headers: string[], rows: string[][]): any[] {
  const fieldMap = headers.map(h => detectField(h))
  return rows.map((row, idx) => {
    const record: any = { __rowIndex: idx + 2, __valid: true, __errors: [] }
    fieldMap.forEach((field, colIdx) => {
      if (colIdx < row.length) {
        record[field] = row[colIdx].trim()
      }
    })
    // 基础校验
    if (!record.customerName && !record.customerId) {
      record.__valid = false; record.__errors.push('缺少客户信息')
    }
    if (!record.productCode) {
      record.__valid = false; record.__errors.push('缺少商品编码')
    }
    record.quantity = Number(record.quantity) || 1
    record.unitPrice = Number(record.unitPrice) || 0
    record.amount = (record.quantity * record.unitPrice).toFixed(2)
    return record
  })
}

// ====== 导入执行 ======
async function handleImport() {
  if (importing.value) return
  const validRows = parsedRows.value.filter(r => r.__valid)
  if (validRows.length === 0) { message.error('没有有效数据可导入'); return }

  importing.value = true
  importProgress.current = 0
  importProgress.total = validRows.length
  importProgress.success = 0
  importProgress.fail = 0
  importProgress.skip = 0
  importProgress.hasError = false
  importProgress.errors = []

  for (const row of validRows) {
    importProgress.current++
    try {
      const customerId = customerMap.value.get(row.customerName) || row.customerId
      const product = productMap.value.get(row.productCode)
      const salespersonId = row.salespersonName ? userMap.value.get(row.salespersonName) : undefined

      if (!customerId && !row.customerId) {
        throw new Error(`客户「${row.customerName}」未在系统中找到`)
      }
      if (!product) {
        throw new Error(`商品编码「${row.productCode}」未在系统中找到`)
      }

      const detail = {
        productId: product.id,
        productCode: row.productCode,
        productName: product.name || row.productName || '',
        quantity: row.quantity,
        unitPrice: row.unitPrice,
        discount: Number(row.discount) || 0,
        totalAmount: row.quantity * row.unitPrice,
        remark: ''
      }

      const submitData: Record<string, any> = {
        customerId: customerId || row.customerId,
        customerName: row.customerName,
        orderDate: row.orderDate || dayjs().format('YYYY-MM-DD'),
        salespersonId: salespersonId,
        salesperson: row.salespersonName || '',
        remark: row.remark || '',
        totalAmount: detail.totalAmount,
        finalAmount: detail.totalAmount,
        details: [detail]
      }

      await saleOrderApi.create(submitData)
      importProgress.success++
    } catch (err: any) {
      importProgress.fail++
      importProgress.hasError = true
      const errMsg = err?.response?.data?.message || err?.message || '未知错误'
      importProgress.errors.push({ row: row.__rowIndex, message: errMsg })
    }
  }

  importing.value = false
  step.value = 3

  if (importProgress.fail === 0) {
    importResult.status = 'success'
    importResult.title = '导入完成'
    importResult.subtitle = `成功导入 ${importProgress.success} 条记录`
    importResult.hasErrors = false
  } else if (importProgress.success > 0) {
    importResult.status = 'warning'
    importResult.title = '部分导入成功'
    importResult.subtitle = `成功 ${importProgress.success} 条，失败 ${importProgress.fail} 条`
    importResult.hasErrors = true
  } else {
    importResult.status = 'error'
    importResult.title = '导入失败'
    importResult.subtitle = `全部 ${importProgress.fail} 条记录均导入失败，请检查数据后重试`
    importResult.hasErrors = true
  }
}

// ====== 下载模板 ======
function downloadTemplate() {
  const headers = ['订单号', '客户', '商品编码', '数量', '单价', '折扣', '订单日期', '销售员', '备注']
  const sample = ['SO20240101001', '示例客户', 'P001', '10', '100', '0', dayjs().format('YYYY-MM-DD'), '销售员', '示例备注']
  const BOM = '\uFEFF'
  const csv = BOM + headers.join(',') + '\n' + sample.join(',')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `销售订单导入模板_${dayjs().format('YYYYMMDD')}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

// ====== 下载错误日志 ======
function downloadErrorLog() {
  const headers = ['行号', '错误信息']
  const rows = importProgress.errors.map(e => [`第${e.row}行`, e.message])
  const BOM = '\uFEFF'
  const csv = BOM + headers.join(',') + '\n' + rows.map(r => r.map(v => `"${v}"`).join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `导入错误日志_${dayjs().format('YYYYMMDDHHmmss')}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

// ====== 关闭与重置 ======
function handleClose() {
  if (importing.value) {
    message.warning('导入进行中，请等待完成')
    return
  }
  resetState()
  emit('update:open', false)
}

function handleDone() {
  resetState()
  emit('update:open', false)
  if (importProgress.success > 0) {
    emit('success')
  }
}

function resetState() {
  step.value = 1
  parsedRows.value = []
  importing.value = false
  importProgress.current = 0
  importProgress.total = 0
  importProgress.success = 0
  importProgress.fail = 0
  importProgress.skip = 0
  importProgress.hasError = false
  importProgress.errors = []
}
</script>

<style scoped>
.import-step {
  min-height: 200px;
}

.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  border: 2px dashed var(--color-border, #d9d9d9);
  border-radius: 8px;
  background: var(--color-bg-layout, #fafafa);
  cursor: pointer;
  transition: all 0.3s;
}

.upload-area:hover {
  border-color: var(--color-primary, #1890ff);
  background: var(--color-primary-bg, #e6f7ff);
}

.step-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>
