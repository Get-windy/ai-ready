<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="商品组合"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="商品组合"
      row-key="id"
      empty-text="暂无商品组合"
    >
      <template #header-extra>
        <a-button
          type="primary"
          @click="openCreate"
        >
          <template #icon>
            <PlusOutlined />
          </template>新增组合
        </a-button>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'kitType'">
          <a-tag color="blue">
            {{ KIT_TYPE_MAP[text]?.label || text || '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'active'">
          <a-tag :color="record.active ? 'green' : 'default'">
            {{ record.active ? '已启用' : '已停用' }}
          </a-tag>
        </template>
        <template v-else-if="['kitPrice', 'kitCost'].includes(column.dataIndex as string)">
          {{ formatMoney(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'profitRate'">
          {{ text === null || text === undefined ? '-' : Number(text).toFixed(2) + '%' }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-space>
            <a-button
              type="link"
              size="small"
              @click="openDetail(record as ProductKit)"
            >
              详情
            </a-button>
            <a-button
              type="link"
              size="small"
              @click="openEdit(record as ProductKit)"
            >
              编辑
            </a-button>
            <a-popconfirm
              :title="`确认复制组合「${record.kitName}」？`"
              ok-text="复制"
              cancel-text="取消"
              @confirm="handleCopy(record as ProductKit)"
            >
              <a-button
                type="link"
                size="small"
              >
                复制
              </a-button>
            </a-popconfirm>
            <a-button
              type="link"
              size="small"
              :style="record.active ? 'color: #fa8c16' : 'color: #52c41a'"
              @click="toggleActive(record as ProductKit)"
            >
              {{ record.active ? '停用' : '启用' }}
            </a-button>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <!-- 组合详情抽屉 -->
    <a-drawer
      v-model:open="detailVisible"
      :title="'组合详情 - ' + (detailData?.kitName || '')"
      width="720"
    >
      <a-descriptions
        v-if="detailData"
        bordered
        :column="2"
        size="small"
      >
        <a-descriptions-item label="组合编码">
          {{ detailData.kitCode }}
        </a-descriptions-item>
        <a-descriptions-item label="类型">
          {{ KIT_TYPE_MAP[detailData.kitType ?? -1]?.label || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="组合名称">
          {{ detailData.kitName }}
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="detailData.active ? 'green' : 'default'">
            {{ detailData.active ? '已启用' : '已停用' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="组合价">
          ¥{{ formatMoney(detailData.kitPrice) }}
        </a-descriptions-item>
        <a-descriptions-item label="组合成本">
          ¥{{ formatMoney(detailData.kitCost) }}
        </a-descriptions-item>
        <a-descriptions-item label="成品编码">
          {{ detailData.productCode || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="成品名称">
          {{ detailData.productName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item
          label="组合说明"
          :span="2"
        >
          {{ detailData.description || '-' }}
        </a-descriptions-item>
      </a-descriptions>

      <a-divider>组合组件</a-divider>
      <a-table
        :data-source="detailItems"
        :columns="detailItemColumns"
        :pagination="false"
        size="small"
        row-key="id"
        :loading="detailLoading"
        :locale="{ emptyText: '暂无组件' }"
      >
        <template #bodyCell="{ column, text }">
          <template v-if="['quantity'].includes(column.dataIndex as string)">
            {{ formatQty(text) }}
          </template>
          <template v-else-if="['unitCost', 'lineCost'].includes(column.dataIndex as string)">
            {{ formatMoney(text) }}
          </template>
        </template>
      </a-table>
    </a-drawer>

    <!-- 新增/编辑组合弹窗 -->
    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑商品组合' : '新增商品组合'"
      :confirm-loading="saving"
      width="860px"
      @ok="handleSave"
    >
      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="组合名称"
              name="kitName"
              :label-col="{ span: 8 }"
            >
              <a-input
                v-model:value="form.kitName"
                placeholder="如：办公套装A"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="组合类型"
              name="kitType"
              :label-col="{ span: 8 }"
            >
              <a-select
                v-model:value="form.kitType"
                :options="kitTypeOptions"
                placeholder="请选择类型"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="成品编码"
              name="productCode"
              :label-col="{ span: 8 }"
            >
              <a-input
                v-model:value="form.productCode"
                placeholder="组合对应的成品商品编码"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="成品名称"
              name="productName"
              :label-col="{ span: 8 }"
            >
              <a-input
                v-model:value="form.productName"
                placeholder="组合对应的成品商品名称"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="组合价"
              name="kitPrice"
              :label-col="{ span: 8 }"
            >
              <a-input-number
                v-model:value="form.kitPrice"
                :min="0"
                :precision="2"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="单位"
              name="productUnit"
              :label-col="{ span: 8 }"
            >
              <a-input
                v-model:value="form.productUnit"
                placeholder="如：套"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="允许拆分"
              name="allowSplit"
              :label-col="{ span: 8 }"
            >
              <a-switch v-model:checked="form.allowSplit" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="允许部分发货"
              name="allowPartial"
              :label-col="{ span: 8 }"
            >
              <a-switch v-model:checked="form.allowPartial" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item
              label="组合说明"
              name="description"
              :label-col="{ span: 4 }"
              :wrapper-col="{ span: 19 }"
            >
              <a-textarea
                v-model:value="form.description"
                :rows="2"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <a-divider orientation="left">
        组合组件
      </a-divider>
      <a-table
        :data-source="form.items"
        :columns="editItemColumns"
        :pagination="false"
        size="small"
        :row-key="(_: any, i: number) => String(i)"
        :locale="{ emptyText: '请添加组件商品' }"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.dataIndex === 'componentProductCode'">
            <a-input
              v-model:value="record.componentProductCode"
              size="small"
              placeholder="商品编码"
            />
          </template>
          <template v-else-if="column.dataIndex === 'componentProductName'">
            <a-input
              v-model:value="record.componentProductName"
              size="small"
              placeholder="商品名称"
            />
          </template>
          <template v-else-if="column.dataIndex === 'componentProductUnit'">
            <a-input
              v-model:value="record.componentProductUnit"
              size="small"
              placeholder="单位"
            />
          </template>
          <template v-else-if="column.dataIndex === 'quantity'">
            <a-input-number
              v-model:value="record.quantity"
              size="small"
              :min="0"
              :precision="2"
              style="width: 100%"
            />
          </template>
          <template v-else-if="column.dataIndex === 'unitCost'">
            <a-input-number
              v-model:value="record.unitCost"
              size="small"
              :min="0"
              :precision="2"
              style="width: 100%"
            />
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-button
              type="link"
              size="small"
              danger
              @click="removeItem(index)"
            >
              删除
            </a-button>
          </template>
        </template>
      </a-table>
      <a-button
        block
        type="dashed"
        style="margin-top: 8px"
        @click="addItem"
      >
        <template #icon>
          <PlusOutlined />
        </template>添加组件
      </a-button>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { productKitApi, type ProductKit, type ProductKitItem } from '@/api/erp/mall'

defineOptions({ name: 'MallProductCombo' })

// ═══ 套装类型（与后端 KitType 枚举一致） ═══
const KIT_TYPE_MAP: Record<number, { label: string }> = {
  1: { label: '组合套装' },
  2: { label: '捆绑套装' },
  3: { label: '礼品套装' },
  4: { label: '组装产品' },
  5: { label: '拆分产品' }
}

const kitTypeOptions = Object.entries(KIT_TYPE_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '组合编码/名称' },
  { key: 'kitType', type: 'select', label: '组合类型', placeholder: '全部类型', options: kitTypeOptions }
]

const columns: any[] = [
  { title: '组合编码', dataIndex: 'kitCode', key: 'kitCode', width: 160 },
  { title: '组合名称', dataIndex: 'kitName', key: 'kitName', width: 170, ellipsis: true },
  { title: '成品名称', dataIndex: 'productName', key: 'productName', width: 150, ellipsis: true },
  { title: '类型', dataIndex: 'kitType', key: 'kitType', width: 100 },
  { title: '组合价', dataIndex: 'kitPrice', key: 'kitPrice', width: 100, align: 'right' },
  { title: '组合成本', dataIndex: 'kitCost', key: 'kitCost', width: 100, align: 'right' },
  { title: '利润率', dataIndex: 'profitRate', key: 'profitRate', width: 90, align: 'right' },
  { title: '状态', dataIndex: 'active', key: 'active', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 210, fixed: 'right' }
]

const detailItemColumns: any[] = [
  { title: '商品编码', dataIndex: 'componentProductCode', key: 'componentProductCode', width: 110 },
  { title: '商品名称', dataIndex: 'componentProductName', key: 'componentProductName', ellipsis: true },
  { title: '单位', dataIndex: 'componentProductUnit', key: 'componentProductUnit', width: 70 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 90, align: 'right' },
  { title: '单位成本', dataIndex: 'unitCost', key: 'unitCost', width: 100, align: 'right' },
  { title: '行成本', dataIndex: 'lineCost', key: 'lineCost', width: 110, align: 'right' }
]

const editItemColumns: any[] = [
  { title: '商品编码', dataIndex: 'componentProductCode', key: 'componentProductCode', width: 130 },
  { title: '商品名称', dataIndex: 'componentProductName', key: 'componentProductName' },
  { title: '单位', dataIndex: 'componentProductUnit', key: 'componentProductUnit', width: 80 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 110 },
  { title: '单位成本', dataIndex: 'unitCost', key: 'unitCost', width: 110 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 60 }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

// ═══ 数据请求（erp-sales /erp/product-kit/page） ═══
function fetcher(params: Record<string, any>) {
  return productKitApi.page(params)
}

// ═══ 详情抽屉 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<ProductKit | null>(null)
const detailItems = ref<ProductKitItem[]>([])

async function openDetail(record: ProductKit) {
  detailData.value = record
  detailItems.value = []
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailItems.value = await productKitApi.items(record.id!) || []
  } catch (e) {
    console.warn('[商品组合] 组件获取失败', e)
  } finally {
    detailLoading.value = false
  }
}

// ═══ 新增/编辑 ═══
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

interface KitItemForm {
  componentProductCode: string
  componentProductName: string
  componentProductUnit: string
  quantity: number
  unitCost: number
}

const emptyForm = () => ({
  kitName: '',
  kitType: 1 as number,
  productCode: '',
  productName: '',
  productUnit: '',
  kitPrice: 0 as number,
  allowSplit: false,
  allowPartial: false,
  description: '',
  items: [] as KitItemForm[]
})
const form = reactive(emptyForm())

const rules: Record<string, Rule[]> = {
  kitName: [{ required: true, message: '请输入组合名称', trigger: 'blur' }],
  kitType: [{ required: true, message: '请选择组合类型', trigger: 'change' }]
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  modalOpen.value = true
}

async function openEdit(record: ProductKit) {
  editingId.value = record.id ?? null
  let items: KitItemForm[] = []
  try {
    const list = await productKitApi.items(record.id!) || []
    items = list.map((it: ProductKitItem) => ({
      componentProductCode: it.componentProductCode || '',
      componentProductName: it.componentProductName || '',
      componentProductUnit: it.componentProductUnit || '',
      quantity: Number(it.quantity) || 0,
      unitCost: Number(it.unitCost) || 0
    }))
  } catch (e) {
    console.warn('[商品组合] 组件获取失败', e)
  }
  Object.assign(form, emptyForm(), {
    kitName: record.kitName,
    kitType: record.kitType ?? 1,
    productCode: record.productCode || '',
    productName: record.productName || '',
    productUnit: record.productUnit || '',
    kitPrice: Number(record.kitPrice) || 0,
    allowSplit: !!record.allowSplit,
    allowPartial: !!record.allowPartial,
    description: record.description || '',
    items
  })
  modalOpen.value = true
}

function addItem() {
  form.items.push({ componentProductCode: '', componentProductName: '', componentProductUnit: '', quantity: 1, unitCost: 0 })
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (!form.items.length) {
    message.warning('请至少添加一个组件商品')
    return
  }
  if (form.items.some(it => !it.componentProductName.trim())) {
    message.warning('组件商品名称不能为空')
    return
  }
  saving.value = true
  try {
    const payload = {
      kitName: form.kitName,
      kitType: form.kitType,
      productCode: form.productCode,
      productName: form.productName,
      productUnit: form.productUnit,
      kitPrice: form.kitPrice,
      allowSplit: form.allowSplit,
      allowPartial: form.allowPartial,
      description: form.description,
      items: form.items.map(it => ({ ...it }))
    }
    if (editingId.value) {
      await productKitApi.update(editingId.value, payload)
      message.success('组合已更新')
    } else {
      await productKitApi.create(payload)
      message.success('组合已创建')
    }
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[商品组合] 保存失败', e)
  } finally {
    saving.value = false
  }
}

async function handleCopy(record: ProductKit) {
  try {
    await productKitApi.copy(record.id!)
    message.success(`已复制组合「${record.kitName}」`)
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[商品组合] 复制失败', e)
  }
}

async function toggleActive(record: ProductKit) {
  try {
    if (record.active) {
      await productKitApi.deactivate(record.id!)
      message.success(`组合「${record.kitName}」已停用`)
    } else {
      await productKitApi.activate(record.id!)
      message.success(`组合「${record.kitName}」已启用`)
    }
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[商品组合] 状态切换失败', e)
  }
}
</script>
