<template>
  <div class="editable-bill-grid">
    <!-- 工具栏：扫描枪录入开关 -->
    <div
      v-if="showScanner && !readonly"
      class="grid-toolbar"
    >
      <span class="scanner-label">扫描枪录入</span>
      <a-switch
        v-model:checked="scannerMode"
        size="small"
      />
      <span class="scanner-hint">开启后连续选品自动追加行</span>
    </div>

    <a-table
      :columns="mergedColumns"
      :data-source="rows"
      :pagination="false"
      row-key="_key"
      size="small"
      bordered
      :scroll="{ x: totalWidth }"
    >
      <template #bodyCell="{ column, record, index }">
        <!-- 操作列 -->
        <template v-if="column.key === '__action'">
          <a-space :size="2">
            <a-button
              v-if="showAdd && !readonly"
              type="link"
              size="small"
              @click="handleAddRow"
            >
              +
            </a-button>
            <a-button
              v-if="!readonly"
              type="link"
              size="small"
              danger
              @click="handleRemoveRow(index)"
            >
              ×
            </a-button>
          </a-space>
        </template>

        <!-- 库存状态列 -->
        <template v-else-if="column.key === '__stock'">
          <template v-if="record.availableStock !== undefined && record.availableStock !== null">
            <a-tag
              :color="record.availableStock >= (record.quantity || 0) ? 'success' : 'error'"
              size="small"
            >
              {{ record.availableStock >= (record.quantity || 0) ? '充足' : '不足' }} {{ record.availableStock }}
            </a-tag>
          </template>
          <span v-else>-</span>
        </template>

        <!-- 商品名称列：空行时可搜索选品 -->
        <template v-else-if="column.key === 'productName' && column.editable && !readonly && !record.productId">
          <ProductSelector
            :show-price="false"
            :show-stock="false"
            placeholder="搜索并选择商品..."
            @select="(products: any[]) => handleProductChange(products[0], record)"
          />
        </template>

        <!-- 可编辑单元格 -->
        <template v-else>
          <a-input
            v-if="isTextColumn(column) && column.editable && !readonly"
            v-model:value="record[column.key]"
            size="small"
            @change="onCellChange(record)"
          />
          <a-input-number
            v-else-if="column.valueType === 'money' && column.editable && !readonly"
            v-model:value="record[column.key]"
            :min="0"
            :precision="2"
            size="small"
            style="width: 100%"
            @change="onCellChange(record)"
          />
          <a-input-number
            v-else-if="column.valueType === 'number' && column.editable && !readonly"
            v-model:value="record[column.key]"
            :min="0"
            :precision="4"
            size="small"
            style="width: 100%"
            @change="onCellChange(record)"
          />
          <a-date-picker
            v-else-if="column.valueType === 'date' && column.editable && !readonly"
            v-model:value="record[column.key]"
            size="small"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
          <span
            v-else-if="column.valueType === 'money'"
            class="money-cell"
          >
            ¥{{ (Number(record[column.key]) || 0).toFixed(2) }}
          </span>
          <span v-else>{{ record[column.key] || '-' }}</span>
        </template>
      </template>
    </a-table>

    <!-- 合计行 -->
    <div
      v-if="showTotal"
      class="grid-total"
    >
      <span>合计:</span>
      <span>数量 <b>{{ totalQuantity.toFixed(2) }}</b></span>
      <span>金额 <b>¥{{ totalAmount.toFixed(2) }}</b></span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ProductSelector from '../ProductSelector/ProductSelector.vue'

export interface GridColumn {
  title: string
  key: string
  width?: number
  editable?: boolean
  valueType?: 'text' | 'number' | 'money' | 'date'
  align?: 'left' | 'right' | 'center'
}

const props = withDefaults(defineProps<{
  rows: any[]
  columns: GridColumn[]
  readonly?: boolean
  showAdd?: boolean
  showStock?: boolean
  stockMap?: Record<number, number>
  showTotal?: boolean
  showScanner?: boolean
}>(), {
  readonly: false,
  showAdd: false,
  showStock: false,
  showTotal: true,
  showScanner: true,
})

const emit = defineEmits<{
  'add-row': []
  'remove-row': [index: number]
  'update-rows': []
}>()

const scannerMode = ref(false)

const totalWidth = computed(() => {
  let w = props.showAdd ? 64 : 0
  if (props.showStock) w += 100
  for (const col of props.columns) w += (col.width || 120)
  return w
})

const mergedColumns = computed(() => {
  const cols: any[] = []
  if (props.showAdd) {
    cols.push({ title: '操作', key: '__action', width: 64, fixed: 'left', align: 'center' })
  }
  if (props.showStock) {
    cols.push({ title: '库存状态', key: '__stock', width: 100 })
  }
  for (const col of props.columns) {
    cols.push({
      title: col.title,
      key: col.key,
      width: col.width || 120,
      align: col.align || (col.valueType === 'money' ? 'right' : 'left'),
    })
  }
  return cols
})

const totalQuantity = computed(() =>
  props.rows.reduce((s: number, r: any) => s + (Number(r.quantity) || 0), 0)
)
const totalAmount = computed(() =>
  props.rows.reduce((s: number, r: any) => s + (Number(r.cost) || 0), 0)
)

function isTextColumn(col: any) {
  return !col.valueType || col.valueType === 'text'
}

function handleAddRow() {
  emit('add-row')
}

function handleRemoveRow(index: number) {
  emit('remove-row', index)
}

function onCellChange(record: any) {
  // 成本金额 = 数量 × 成本单价
  if ('cost' in record) {
    record.cost = (Number(record.quantity) || 0) * (Number(record.unitCost) || 0)
  }
  // 行内已含商品时，扫描模式自动追加新行（连续录入）
  if (scannerMode.value && record.productId && !record.__scanned) {
    record.__scanned = true
    emit('add-row')
  }
  emit('update-rows')
}

function handleProductChange(product: any, record: any, isNewRow: boolean) {
  if (!product) return
  record.productId = product.id
  record.productCode = product.productCode || ''
  record.productName = product.productName || product.name || ''
  record.spec = product.spec || ''
  record.unit = product.unit || ''
  if (product.costPrice) record.unitCost = Number(product.costPrice)
  onCellChange(record)
  if (scannerMode.value && !isNewRow) {
    record.__scanned = true
    emit('add-row')
  }
}

defineExpose({ handleProductChange })
</script>

<style scoped>
.editable-bill-grid {
  width: 100%;
}
.grid-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.scanner-label { font-size: 12px; color: #606266; }
.scanner-hint { font-size: 12px; color: #999; }
.money-cell {
  font-variant-numeric: tabular-nums;
  color: #303133;
}
.grid-total {
  display: flex;
  gap: 24px;
  justify-content: flex-end;
  padding: 8px 12px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
  color: #606266;
}
.grid-total b { color: #303133; }
</style>
