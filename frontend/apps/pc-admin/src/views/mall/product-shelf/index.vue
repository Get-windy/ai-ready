<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="商品上架"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="商城商品"
      row-key="id"
      empty-text="暂无商城商品"
    >
      <template #header-extra>
        <a-button
          type="primary"
          @click="openCreate"
        >
          <template #icon>
            <PlusOutlined />
          </template>新增商品
        </a-button>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'imageUrl'">
          <a-image
            v-if="text"
            :src="text"
            :width="40"
            :height="40"
            style="object-fit: cover; border-radius: 4px"
          />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag :color="text === 'ON_SHELF' ? 'green' : 'default'">
            {{ text === 'ON_SHELF' ? '已上架' : '已下架' }}
          </a-tag>
        </template>
        <template v-else-if="['salePrice', 'marketPrice'].includes(column.dataIndex as string)">
          {{ formatMoney(text) }}
        </template>
        <template v-else-if="['stockQuantity', 'salesCount'].includes(column.dataIndex as string)">
          {{ formatQty(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-space>
            <a-button
              type="link"
              size="small"
              @click="openEdit(record as MallProduct)"
            >
              编辑
            </a-button>
            <a-button
              type="link"
              size="small"
              :style="record.status === 'ON_SHELF' ? 'color: #fa8c16' : 'color: #52c41a'"
              @click="toggleShelf(record as MallProduct)"
            >
              {{ record.status === 'ON_SHELF' ? '下架' : '上架' }}
            </a-button>
            <a-popconfirm
              title="确认删除该商城商品？"
              ok-text="删除"
              cancel-text="取消"
              @confirm="handleDelete(record as MallProduct)"
            >
              <a-button
                type="link"
                size="small"
                danger
              >
                删除
              </a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <!-- 新增/编辑商品弹窗 -->
    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑商城商品' : '新增商城商品'"
      :confirm-loading="saving"
      width="640px"
      @ok="handleSave"
    >
      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="商品编码"
          name="productId"
        >
          <a-input
            v-model:value="form.productId"
            placeholder="关联ERP商品编码"
            :disabled="!!editingId"
          />
        </a-form-item>
        <a-form-item
          label="商品名称"
          name="productName"
        >
          <a-input
            v-model:value="form.productName"
            placeholder="请输入商品名称"
          />
        </a-form-item>
        <a-form-item
          label="商品图片"
          name="imageUrl"
        >
          <a-input
            v-model:value="form.imageUrl"
            placeholder="图片URL"
          />
        </a-form-item>
        <a-form-item
          label="销售价"
          name="salePrice"
        >
          <a-input-number
            v-model:value="form.salePrice"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="商城销售价"
          />
        </a-form-item>
        <a-form-item
          label="市场价"
          name="marketPrice"
        >
          <a-input-number
            v-model:value="form.marketPrice"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="划线市场价"
          />
        </a-form-item>
        <a-form-item
          label="分类名称"
          name="categoryName"
        >
          <a-input
            v-model:value="form.categoryName"
            placeholder="商城展示分类"
          />
        </a-form-item>
        <a-form-item
          label="可售库存"
          name="stockQuantity"
        >
          <a-input-number
            v-model:value="form.stockQuantity"
            :min="0"
            :precision="0"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item
          label="上架状态"
          name="status"
        >
          <a-radio-group v-model:value="form.status">
            <a-radio value="ON_SHELF">
              上架
            </a-radio>
            <a-radio value="OFF_SHELF">
              下架
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item
          label="商品描述"
          name="description"
        >
          <a-textarea
            v-model:value="form.description"
            :rows="2"
            placeholder="商城详情描述"
          />
        </a-form-item>
      </a-form>
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
import { mallProductApi, type MallProduct } from '@/api/erp/mall'

defineOptions({ name: 'MallProductShelf' })

// ═══ 上架状态（与后端 mall_product.status 一致：ON_SHELF/OFF_SHELF） ═══
const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '商品编码/名称' },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: [
      { label: '已上架', value: 'ON_SHELF' },
      { label: '已下架', value: 'OFF_SHELF' }
    ]
  }
]

const columns: any[] = [
  { title: '图片', dataIndex: 'imageUrl', key: 'imageUrl', width: 70 },
  { title: '商品编码', dataIndex: 'productId', key: 'productId', width: 110 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 180, ellipsis: true },
  { title: '分类', dataIndex: 'categoryName', key: 'categoryName', width: 110 },
  { title: '销售价', dataIndex: 'salePrice', key: 'salePrice', width: 100, align: 'right' },
  { title: '市场价', dataIndex: 'marketPrice', key: 'marketPrice', width: 100, align: 'right' },
  { title: '可售库存', dataIndex: 'stockQuantity', key: 'stockQuantity', width: 90, align: 'right' },
  { title: '销量', dataIndex: 'salesCount', key: 'salesCount', width: 80, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 160, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return mallProductApi.page(params)
}

// ═══ 新增/编辑/上下架/删除 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  productId: '',
  productName: '',
  imageUrl: '',
  salePrice: 0 as number,
  marketPrice: 0 as number,
  categoryName: '',
  stockQuantity: 0 as number,
  status: 'OFF_SHELF' as string,
  description: ''
})
const form = reactive(emptyForm())

const rules: Record<string, Rule[]> = {
  productId: [{ required: true, message: '请输入商品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  salePrice: [{ required: true, message: '请输入销售价', trigger: 'blur' }]
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  modalOpen.value = true
}

function openEdit(record: MallProduct) {
  editingId.value = record.id ?? null
  Object.assign(form, emptyForm(), {
    productId: record.productId,
    productName: record.productName,
    imageUrl: record.imageUrl,
    salePrice: record.salePrice,
    marketPrice: record.marketPrice,
    categoryName: record.categoryName,
    stockQuantity: record.stockQuantity ?? 0,
    status: record.status,
    description: record.description || ''
  })
  modalOpen.value = true
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await mallProductApi.update(editingId.value, { ...form } as MallProduct)
      message.success('商品已更新')
    } else {
      await mallProductApi.create({ ...form } as MallProduct)
      message.success('商品已创建')
    }
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[商品上架] 保存失败', e)
  } finally {
    saving.value = false
  }
}

async function toggleShelf(record: MallProduct) {
  const target = record.status === 'ON_SHELF' ? 'OFF_SHELF' : 'ON_SHELF'
  try {
    await mallProductApi.update(record.id!, { ...record, status: target })
    message.success(target === 'ON_SHELF' ? `「${record.productName}」已上架` : `「${record.productName}」已下架`)
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[商品上架] 上下架失败', e)
  }
}

async function handleDelete(record: MallProduct) {
  try {
    await mallProductApi.delete(record.id!)
    message.success('商品已删除')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[商品上架] 删除失败', e)
  }
}
</script>
