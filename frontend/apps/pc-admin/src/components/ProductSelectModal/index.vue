<template>
  <a-modal
    v-model:open="visible"
    title="选择商品"
    width="900px"
    :mask-closable="false"
    centered
    @ok="handleConfirm"
    @cancel="handleCancel"
  >
    <!-- 搜索筛选区域 -->
    <div class="search-filter-area">
      <a-form layout="inline" :model="searchParams">
        <a-form-item label="商品编码">
          <a-input v-model:value="searchParams.code" placeholder="请输入" allow-clear style="width: 120px" />
        </a-form-item>
        <a-form-item label="商品名称">
          <a-input v-model:value="searchParams.name" placeholder="请输入" allow-clear style="width: 140px" />
        </a-form-item>
        <a-form-item label="条码">
          <a-input v-model:value="searchParams.barcode" placeholder="请输入" allow-clear style="width: 120px" />
        </a-form-item>
        <a-form-item label="商品分类">
          <a-tree-select
            v-model:value="searchParams.categoryId"
            placeholder="请选择"
            allow-clear
            style="width: 140px"
            :tree-data="categoryTree"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">
              <template #icon><SearchOutlined /></template>
              搜索
            </a-button>
            <a-button @click="handleReset">
              <template #icon><ClearOutlined /></template>
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </div>

    <!-- 商品列表表格 -->
    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :row-selection="rowSelection"
      :scroll="{ y: 300 }"
      row-key="id"
      size="small"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-button type="link" size="small" @click="handleQuickSelect(record)">
            {{ record.name }}
          </a-button>
        </template>
        <template v-if="column.dataIndex === 'retailPrice' || column.dataIndex === 'costPrice'">
          <span class="currency-value">¥{{ formatPrice(record[column.dataIndex]) }}</span>
        </template>
      </template>
    </a-table>

    <!-- 底部选择信息 -->
    <div class="selection-info">
      <span class="selected-count">已选择 {{ selectedRows.length }} 个商品</span>
      <div v-if="selectedRows.length > 0" class="selected-preview">
        <a-tag v-for="item in selectedRows.slice(0, 5)" :key="item.id" closable @close="removeSelection(item)">
          {{ item.name }}
        </a-tag>
        <span v-if="selectedRows.length > 5" class="more-count">+{{ selectedRows.length - 5 }}</span>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import { productApi, productCategoryApi } from '@/api/erp/product'

defineOptions({ name: 'ProductSelectModal' })

const props = withDefaults(defineProps<{
  open: boolean
  /** 是否允许多选 */
  multiple?: boolean
  /** 已选中的商品ID列表 */
  selectedIds?: number[]
}>(), {
  open: false,
  multiple: false,
  selectedIds: () => [],
})

const emit = defineEmits<{
  'update:open': [value: boolean]
  'confirm': [products: any[]]
  'cancel': []
}>()

// ── 状态 ──
const visible = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val),
})

const loading = ref(false)
const tableData = ref<any[]>([])
const selectedRows = ref<any[]>([])

// 搜索参数
const searchParams = reactive({
  code: '',
  name: '',
  barcode: '',
  categoryId: undefined as number | undefined,
})

// 分类树数据
const categoryTree = ref<any[]>([])

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50'],
  showTotal: (total: number) => `共 ${total} 条`,
})

// ── 表格列配置 ──
const columns = [
  { title: '编码', dataIndex: 'code', width: 100 },
  { title: '名称', dataIndex: 'name', width: 150 },
  { title: '条码', dataIndex: 'barcode', width: 100 },
  { title: '规格', dataIndex: 'specification', width: 80 },
  { title: '单位', dataIndex: 'unit', width: 60 },
  { title: '零售价', dataIndex: 'retailPrice', width: 80, align: 'right' },
  { title: '成本价', dataIndex: 'costPrice', width: 80, align: 'right' },
  { title: '可用库存', dataIndex: 'availableStock', width: 80, align: 'right' },
]

// ── 行选择配置 ──
const rowSelection = computed(() => ({
  type: props.multiple ? 'checkbox' : 'radio',
  selectedRowKeys: selectedRows.value.map(r => r.id),
  onChange: (keys: number[], rows: any[]) => {
    selectedRows.value = rows
  },
}))

// ── 数据加载 ──
async function fetchData() {
  loading.value = true
  try {
    const res = await productApi.page({
      productCode: searchParams.code,
      productName: searchParams.name,
      barcode: searchParams.barcode,
      categoryId: searchParams.categoryId,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    if (res) {
      // 映射字段名：后端 productCode/productName -> 前端 code/name
      tableData.value = (res.records || res.data?.records || []).map((p: any) => ({
        id: p.id,
        code: p.productCode || p.code || '',
        name: p.productName || p.name || '',
        barcode: p.barcode || '',
        specification: p.spec || p.specification || '',
        unit: p.unit || '',
        retailPrice: p.retailPrice || p.standardPrice || 0,
        costPrice: p.costPrice || 0,
        availableStock: p.availableStock || 0,
        salePrice: p.wholesalePrice || p.standardPrice || 0,
      }))
      pagination.total = res.total || res.data?.total || 0
    }
  } catch (error: any) {
    console.warn('[ProductSelectModal] 加载商品列表失败', error)
    message.error(error?.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadCategoryTree() {
  try {
    const tree = await productCategoryApi.getTree()
    // 映射字段名：categoryName -> name
    categoryTree.value = (tree || []).map((c: any) => ({
      id: c.id,
      name: c.categoryName || c.name || '',
      children: c.children?.map((child: any) => ({
        id: child.id,
        name: child.categoryName || child.name || '',
        children: child.children || [],
      })) || [],
    }))
  } catch (e) {
    categoryTree.value = []
  }
}

// ── 事件处理 ──
function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  Object.assign(searchParams, {
    code: '',
    name: '',
    barcode: '',
    categoryId: undefined,
  })
  pagination.current = 1
  fetchData()
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

function handleQuickSelect(record: any) {
  // 单选时，点击名称直接选中并确认
  if (!props.multiple) {
    selectedRows.value = [record]
    handleConfirm()
  } else {
    // 多选时，添加到选中列表
    const exists = selectedRows.value.find(r => r.id === record.id)
    if (!exists) {
      selectedRows.value.push(record)
    }
  }
}

function removeSelection(item: any) {
  const index = selectedRows.value.findIndex(r => r.id === item.id)
  if (index > -1) {
    selectedRows.value.splice(index, 1)
  }
}

function handleConfirm() {
  if (selectedRows.value.length === 0) {
    message.warning('请选择商品')
    return
  }
  emit('confirm', selectedRows.value)
  visible.value = false
}

function handleCancel() {
  emit('cancel')
  visible.value = false
}

function formatPrice(price: number): string {
  if (!price) return '0.00'
  return price.toFixed(2)
}

// ── 弹窗打开时加载数据 ──
watch(visible, (val) => {
  if (val) {
    // 弹窗打开时加载数据
    fetchData()
    loadCategoryTree()
  } else {
    // 弹窗关闭时清空搜索条件和选中状态
    Object.assign(searchParams, {
      code: '',
      name: '',
      barcode: '',
      categoryId: undefined,
    })
    selectedRows.value = []
    pagination.current = 1
  }
})
</script>

<style scoped>
.search-filter-area {
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 4px;
  margin-bottom: 12px;
}

.selection-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.selected-count {
  font-size: 13px;
  color: #595959;
}

.selected-preview {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.more-count {
  font-size: 12px;
  color: #8c8c8c;
}

.currency-value {
  font-family: 'SFMono-Regular', Consolas, monospace;
}
</style>