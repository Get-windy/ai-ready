<template>
  <ARReportPage
    ref="reportRef"
    title="套餐管理"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="套餐"
    row-key="id"
  >
    <template #header-extra>
      <a-button
        type="primary"
        size="small"
        @click="openCreate"
      >
        <template #icon>
          <PlusOutlined />
        </template>新增套餐
      </a-button>
    </template>
    <template #bodyCell="{ column, record, text }">
      <template v-if="column.dataIndex === 'kitType'">
        {{ KIT_TYPE_MAP[text] || text || '-' }}
      </template>
      <template v-else-if="['kitPrice', 'kitCost'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'profitRate'">
        {{ text != null ? `${Number(text).toFixed(1)}%` : '-' }}
      </template>
      <template v-else-if="column.dataIndex === 'active'">
        <a-tag :color="record.active ? 'green' : 'default'">
          {{ record.active ? '启用' : '停用' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space>
          <a @click="openItems(record)">组件</a>
          <a-divider type="vertical" />
          <a @click="openEdit(record)">编辑</a>
          <a-divider type="vertical" />
          <a-popconfirm
            :title="record.active ? '确认停用该套餐？' : '确认启用该套餐？'"
            @confirm="toggleActive(record)"
          >
            <a :class="record.active ? 'text-warning' : ''">{{ record.active ? '停用' : '启用' }}</a>
          </a-popconfirm>
        </a-space>
      </template>
    </template>
  </ARReportPage>

  <!-- 新增/编辑套餐弹窗 -->
  <a-modal
    v-model:open="modalVisible"
    :title="editingKit ? '编辑套餐' : '新增套餐'"
    :confirm-loading="modalLoading"
    :width="560"
    @ok="handleModalOk"
    @cancel="modalVisible = false"
  >
    <a-form
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      style="margin-top: 16px"
    >
      <a-form-item
        label="套餐名称"
        required
      >
        <a-input
          v-model:value="modalForm.kitName"
          placeholder="请输入套餐名称"
        />
      </a-form-item>
      <a-form-item
        label="套餐商品"
        required
      >
        <a-select
          v-model:value="modalForm.productId"
          :options="productOptions"
          placeholder="套餐对应的销售商品"
          :filter-option="filterOption"
          show-search
        />
      </a-form-item>
      <a-form-item label="套餐类型">
        <a-select
          v-model:value="modalForm.kitType"
          :options="kitTypeOptions"
          placeholder="请选择"
        />
      </a-form-item>
      <a-form-item
        label="套餐价"
        required
      >
        <a-input-number
          v-model:value="modalForm.kitPrice"
          :min="0"
          :precision="2"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="数量限制">
        <a-space>
          <a-input-number
            v-model:value="modalForm.minQuantity"
            :min="1"
            placeholder="最少"
          />
          <span>~</span>
          <a-input-number
            v-model:value="modalForm.maxQuantity"
            :min="1"
            placeholder="最多"
          />
        </a-space>
      </a-form-item>
      <a-form-item label="允许拆分">
        <a-switch
          v-model:checked="modalForm.allowSplit"
          checked-children="是"
          un-checked-children="否"
        />
      </a-form-item>
      <a-form-item label="套餐说明">
        <a-textarea
          v-model:value="modalForm.description"
          :rows="2"
          placeholder="选填"
        />
      </a-form-item>
    </a-form>
    <a-alert
      v-if="!editingKit"
      type="info"
      show-icon
      message="套餐组件请在创建后通过「组件」功能维护"
      style="margin-top: 8px"
    />
  </a-modal>

  <!-- 组件明细抽屉 -->
  <a-drawer
    v-model:open="itemsVisible"
    :title="`套餐组件 - ${itemsKit?.kitName || ''}`"
    :width="640"
  >
    <a-table
      :columns="itemColumns"
      :data-source="items"
      :loading="itemsLoading"
      row-key="id"
      size="small"
      :pagination="false"
    >
      <template #bodyCell="{ column, text }">
        <template v-if="['quantity', 'unitCost', 'lineCost'].includes(column.dataIndex as string)">
          {{ formatNum(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'optional'">
          <a-tag :color="text ? 'orange' : 'default'">
            {{ text ? '可选' : '必选' }}
          </a-tag>
        </template>
      </template>
    </a-table>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { productKitApi, type ProductKit, type ProductKitItem } from '@/api/marketing'
import { optionsApi, type OptionItem } from '@/api/options'

// ═══ 套餐类型（后端为数值，本页统一维护取值语义） ═══
const KIT_TYPE_MAP: Record<number, string> = { 1: '固定套餐', 2: '自选组合' }
const kitTypeOptions = Object.entries(KIT_TYPE_MAP).map(([value, label]) => ({ label, value: Number(value) }))

const reportRef = ref<InstanceType<typeof ARReportPage>>()

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '套餐名称/编码' },
  { key: 'kitType', type: 'select', label: '类型', placeholder: '全部类型', options: kitTypeOptions },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: [
      { label: '启用', value: 1 },
      { label: '停用', value: 0 }
    ]
  }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '套餐编码', dataIndex: 'kitCode', key: 'kitCode', width: 130 },
  { title: '套餐名称', dataIndex: 'kitName', key: 'kitName', width: 170, ellipsis: true },
  { title: '套餐商品', dataIndex: 'productName', key: 'productName', width: 150, ellipsis: true },
  { title: '类型', dataIndex: 'kitType', key: 'kitType', width: 100 },
  { title: '套餐价', dataIndex: 'kitPrice', key: 'kitPrice', width: 110, align: 'right' },
  { title: '套餐成本', dataIndex: 'kitCost', key: 'kitCost', width: 110, align: 'right' },
  { title: '毛利率', dataIndex: 'profitRate', key: 'profitRate', width: 90, align: 'right' },
  { title: '状态', dataIndex: 'active', key: 'active', width: 80 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatNum(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return productKitApi.page(params)
}

// ═══ 商品下拉 ═══
const productOptions = ref<{ label: string; value: number }[]>([])

function filterOption(input: string, option: any) {
  return (option?.label || '').toLowerCase().includes(input.toLowerCase())
}

// ═══ 新增/编辑弹窗 ═══
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingKit = ref<ProductKit | null>(null)
const modalForm = reactive<{
  kitName?: string
  productId?: number
  kitType?: number
  kitPrice?: number
  minQuantity?: number
  maxQuantity?: number
  allowSplit?: boolean
  description?: string
}>({})

function openCreate() {
  editingKit.value = null
  Object.assign(modalForm, { kitName: undefined, productId: undefined, kitType: 1, kitPrice: undefined, minQuantity: 1, maxQuantity: undefined, allowSplit: false, description: undefined })
  modalVisible.value = true
}

function openEdit(record: any) {
  editingKit.value = record
  Object.assign(modalForm, {
    kitName: record.kitName,
    productId: record.productId,
    kitType: record.kitType,
    kitPrice: record.kitPrice,
    minQuantity: record.minQuantity,
    maxQuantity: record.maxQuantity,
    allowSplit: !!record.allowSplit,
    description: record.description
  })
  modalVisible.value = true
}

async function handleModalOk() {
  if (!modalForm.kitName) {
    message.warning('请输入套餐名称')
    return
  }
  if (!modalForm.productId) {
    message.warning('请选择套餐商品')
    return
  }
  if (modalForm.kitPrice === undefined || modalForm.kitPrice === null) {
    message.warning('请输入套餐价')
    return
  }
  modalLoading.value = true
  try {
    const product = productOptions.value.find(p => p.value === modalForm.productId)
    const payload: Partial<ProductKit> = {
      kitName: modalForm.kitName,
      productId: modalForm.productId,
      productName: product?.label,
      kitType: modalForm.kitType,
      kitPrice: modalForm.kitPrice,
      minQuantity: modalForm.minQuantity,
      maxQuantity: modalForm.maxQuantity,
      allowSplit: modalForm.allowSplit,
      description: modalForm.description
    }
    if (editingKit.value) {
      await productKitApi.update(editingKit.value.id, payload)
      message.success('更新成功')
    } else {
      await productKitApi.create(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[套餐管理] 保存失败', e)
  } finally {
    modalLoading.value = false
  }
}

// ═══ 启用/停用 ═══
async function toggleActive(record: any) {
  try {
    if (record.active) {
      await productKitApi.deactivate(record.id)
      message.success('已停用')
    } else {
      await productKitApi.activate(record.id)
      message.success('已启用')
    }
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[套餐管理] 状态更新失败', e)
  }
}

// ═══ 组件明细抽屉 ═══
const itemsVisible = ref(false)
const itemsLoading = ref(false)
const items = ref<ProductKitItem[]>([])
const itemsKit = ref<ProductKit | null>(null)

const itemColumns: any[] = [
  { title: '行号', dataIndex: 'lineNo', key: 'lineNo', width: 60 },
  { title: '组件编码', dataIndex: 'componentProductCode', key: 'componentProductCode', width: 120 },
  { title: '组件名称', dataIndex: 'componentProductName', key: 'componentProductName', ellipsis: true },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'right' },
  { title: '单位成本', dataIndex: 'unitCost', key: 'unitCost', width: 100, align: 'right' },
  { title: '行成本', dataIndex: 'lineCost', key: 'lineCost', width: 100, align: 'right' },
  { title: '属性', dataIndex: 'optional', key: 'optional', width: 80 }
]

async function openItems(record: any) {
  itemsKit.value = record
  itemsVisible.value = true
  itemsLoading.value = true
  try {
    items.value = await productKitApi.items(record.id)
  } catch (e) {
    items.value = []
    console.warn('[套餐管理] 组件明细获取失败', e)
  } finally {
    itemsLoading.value = false
  }
}

onMounted(async () => {
  try {
    const list: OptionItem[] = await optionsApi.getProducts()
    productOptions.value = list.map(p => ({ label: p.name, value: p.id }))
  } catch (e) {
    console.warn('[套餐管理] 商品列表获取失败', e)
  }
})
</script>

<style scoped>
.text-warning {
  color: #faad14;
}
</style>
