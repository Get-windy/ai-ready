<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- Tab标签栏（居中） -->
      <div class="tab-bar">
        <div class="tab-bar-inner">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            :class="['tab-item', { active: activeTab === tab.key }]"
            @click="switchTab(tab.key)"
          >
            {{ tab.label }}
          </div>
        </div>
      </div>

      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <a-button type="primary" class="btn-add" @click="handleAdd">
            <PlusOutlined /> 新增{{ currentTabLabel }}
          </a-button>
        </div>
        <div class="toolbar-right">
          <a-input
            v-model:value="searchKeyword"
            :placeholder="searchPlaceholder"
            size="small"
            style="width: 200px"
            allow-clear
            @press-enter="handleSearch"
          />
          <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
          <a-button size="small" @click="handleRefresh">
            <ReloadOutlined /> 刷新
          </a-button>
        </div>
      </div>

      <!-- 表格区域 -->
      <!-- 品牌/单位/标签 使用 BillDetailTable -->
      <div v-if="activeTab !== 'category'" class="table-wrapper">
        <BillDetailTable
          :columns="currentColumns"
          :data-source="tableData"
          :loading="loading"
          :view-mode="true"
          :max-height="tableMaxHeight"
          fill-mode
        >
          <!-- 对应商品列（标签tab专用） -->
          <template #relatedProductsCell="{ record }">
            <a-button type="link" size="small" @click="openProductSelector(record)">
              选择商品
            </a-button>
            <span v-if="record.productCount" class="product-count">{{ record.productCount }}个</span>
          </template>
        </BillDetailTable>
        <a-empty
          v-if="!loading && tableData.length === 0"
          description="暂无数据"
          style="padding: 40px 0"
        />
      </div>

      <!-- 分类树表格（ColumnConfigTable 内置列配置 + 填充列） -->
      <div v-else class="table-wrapper">
        <ColumnConfigTable
          :column-defs="categoryColDefs"
          storage-key="product-supplement-category-columns"
          fill-mode
          :data-source="categoryFlatRows"
          :pagination="false"
          size="small"
          :bordered="true"
          row-key="id"
          class="category-tree-table"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="!column"></template>
            <template v-else-if="column.key === 'rowNo'">
              {{ index + 1 }}
            </template>
            <template v-else-if="column.key === 'categoryName'">
              <span class="tree-indent" :style="{ paddingLeft: (record._level || 0) * 20 + 'px' }">
                <span
                  v-if="record._hasChildren"
                  class="tree-expand-icon"
                  @click.stop="toggleCategoryExpand(record.id)"
                >
                  <CaretDownOutlined v-if="categoryExpandedKeys.includes(record.id)" />
                  <CaretRightOutlined v-else />
                </span>
                <span v-else class="tree-expand-placeholder" />
                <span
                  :class="{ 'tree-node-clickable': record._hasChildren }"
                  @click="record._hasChildren && toggleCategoryExpand(record.id)"
                >{{ record.categoryName }}</span>
              </span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" size="small" @click="handleAddSubCategory(record)">新增</a-button>
              <a-button type="link" size="small" @click="handleEditCategory(record)">修改</a-button>
              <a-button type="link" size="small" danger @click="handleDeleteCategory(record)">删除</a-button>
            </template>
          </template>
          <template #emptyText>
            <a-empty description="暂无分类" />
          </template>
        </ColumnConfigTable>
      </div>

      <!-- 底部分页（页面级，所有tab共享，固定在PageContainer底部） -->
      <template #footer>
        <StandardPagination
          :current="pagination.current"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          :page-size-options="[10, 20, 50, 100]"
          @change="handlePageChange"
        />
      </template>

      <!-- ── 品牌编辑弹窗 ── -->
      <a-modal
        v-model:open="brandModalVisible"
        :title="isEdit ? '修改商品品牌' : '新增商品品牌'"
        @ok="handleBrandSubmit"
        @cancel="brandModalVisible = false"
        :confirm-loading="modalLoading"
        width="480px"
      >
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" style="margin-top: 16px">
          <a-form-item label="品牌名称" :required="true">
            <a-input v-model:value="brandForm.brandName" placeholder="请输入品牌名称" />
          </a-form-item>
          <a-form-item label="助记码">
            <a-input v-model:value="brandForm.mnemonicCode" placeholder="请输入助记码(拼音首字母)" />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea v-model:value="brandForm.remark" placeholder="请输入备注" :rows="3" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ── 单位编辑弹窗 ── -->
      <a-modal
        v-model:open="unitModalVisible"
        :title="isEdit ? '修改商品单位' : '新增商品单位'"
        @ok="handleUnitSubmit"
        @cancel="unitModalVisible = false"
        :confirm-loading="modalLoading"
        width="480px"
      >
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" style="margin-top: 16px">
          <a-form-item label="单位名称" :required="true">
            <a-input v-model:value="unitForm.unitName" placeholder="请输入单位名称" />
          </a-form-item>
          <a-form-item label="助记码">
            <a-input v-model:value="unitForm.mnemonicCode" placeholder="请输入助记码(拼音首字母)" />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea v-model:value="unitForm.remark" placeholder="请输入备注" :rows="3" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ── 标签编辑弹窗 ── -->
      <a-modal
        v-model:open="tagModalVisible"
        :title="isEdit ? '修改商品标签' : '新增商品标签'"
        @ok="handleTagSubmit"
        @cancel="tagModalVisible = false"
        :confirm-loading="modalLoading"
        width="480px"
      >
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" style="margin-top: 16px">
          <a-form-item label="标签名称" :required="true">
            <a-input v-model:value="tagForm.tagName" placeholder="请输入标签名称" />
          </a-form-item>
          <a-form-item label="排序">
            <a-input-number v-model:value="tagForm.sortOrder" :min="0" style="width: 100%" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ── 分类编辑弹窗 ── -->
      <a-modal
        v-model:open="categoryModalVisible"
        :title="isEdit ? '修改商品分类' : '新增商品分类'"
        @ok="handleCategorySubmit"
        @cancel="categoryModalVisible = false"
        :confirm-loading="modalLoading"
        width="480px"
      >
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" style="margin-top: 16px">
          <a-form-item label="分类名称" :required="true">
            <a-input v-model:value="categoryForm.categoryName" placeholder="请输入分类名称" />
          </a-form-item>
          <a-form-item label="分类编码">
            <a-input v-model:value="categoryForm.categoryCode" placeholder="请输入分类编码" />
          </a-form-item>
          <a-form-item label="上级分类">
            <a-tree-select
              v-model:value="categoryForm.parentId"
              :tree-data="categoryTreeData"
              :field-names="{ children: 'children', label: 'categoryName', value: 'id' }"
              placeholder="无(根节点)"
              allow-clear
              style="width: 100%"
              tree-check-strictly
            />
          </a-form-item>
          <a-form-item label="排序">
            <a-input-number v-model:value="categoryForm.sortOrder" :min="0" style="width: 100%" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ── 商品选择弹窗（标签-对应商品） ── -->
      <a-modal
        v-model:open="productSelectorVisible"
        title="选择对应商品"
        width="700px"
        @ok="handleProductSelectorOk"
        @cancel="productSelectorVisible = false"
        :confirm-loading="productSelectorLoading"
      >
        <div style="margin-bottom: 12px">
          <a-input-search
            v-model:value="productSearchKeyword"
            placeholder="搜索商品名称/编码"
            style="width: 300px"
            @search="searchProductsForTag"
            allow-clear
          />
        </div>
        <a-table
          :columns="productSelectorColumns"
          :data-source="productSelectorData"
          :pagination="false"
          :row-selection="{ selectedRowKeys: selectedProductIds, onChange: onProductSelectChange }"
          size="small"
          :bordered="true"
          row-key="id"
          :scroll="{ y: 360 }"
          :loading="productSelectorLoading"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'productName'">
              {{ record.productName }}
            </template>
          </template>
        </a-table>
        <div style="margin-top: 8px; color: #999; font-size: 12px">
          已选择 {{ selectedProductIds.length }} 个商品
        </div>
      </a-modal>

    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import ColumnConfigTable from '@/components/ColumnConfigTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { toMnemonicCode } from '@/composables/useMnemonicCode'
import {
  productBrandApi,
  productUnitDictApi,
  mallTagApi,
  productCategoryApi,
  productApi,
} from '@/api/erp/product'
import type { ProductBrand, ProductUnitDict, MallTag, ProductCategory, Product } from '@/api/erp/product'

// ── Tab配置 ──
const tabs = ref([
  { key: 'brand', label: '商品品牌' },
  { key: 'unit', label: '商品单位' },
  { key: 'tag', label: '商品标签' },
  { key: 'category', label: '商品分类' },
])
const activeTab = ref('brand')

const currentTabLabel = computed(() => {
  return tabs.value.find(t => t.key === activeTab.value)?.label.replace('商品', '') || ''
})

const searchPlaceholder = computed(() => {
  switch (activeTab.value) {
    case 'brand': return '品牌名称/助记码'
    case 'unit': return '单位名称'
    case 'tag': return '标签名称'
    default: return '请输入关键字'
  }
})

function switchTab(key: string) {
  activeTab.value = key
  searchKeyword.value = ''
  if (key === 'category') {
    fetchCategoryTree()
  } else {
    pagination.current = 1
    fetchData()
  }
}

// ── 表格高度 ──
const tableMaxHeight = computed(() => {
  if (typeof window !== 'undefined') {
    return window.innerHeight - 300
  }
  return 400
})

// ── 搜索 & 刷新 ──
const searchKeyword = ref('')

function handleSearch() {
  if (activeTab.value === 'category') {
    fetchCategoryTree()
  } else {
    pagination.current = 1
    fetchData()
  }
}

function handleRefresh() {
  if (activeTab.value === 'category') {
    fetchCategoryTree()
  } else {
    fetchData()
  }
}

// ── 数据加载（品牌/单位/标签）─
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value

    let res: any
    switch (activeTab.value) {
      case 'brand':
        res = await productBrandApi.page(params)
        tableData.value = res.records || []
        pagination.total = res.total || 0
        break
      case 'unit':
        res = await productUnitDictApi.page(params)
        tableData.value = res.records || []
        pagination.total = res.total || 0
        break
      case 'tag': {
        const allTags = (await mallTagApi.list()) as any[]
        const filtered = searchKeyword.value
          ? allTags.filter(t => t.tagName?.includes(searchKeyword.value))
          : allTags
        pagination.total = filtered.length
        const start = (pagination.current - 1) * pagination.pageSize
        tableData.value = filtered.slice(start, start + pagination.pageSize)
        break
      }
    }
  } catch (e) {
    console.error('[商品辅助资料] 加载失败', e)
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// ── 品牌列配置（BillDetailTable 格式）──
const brandColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 60 },
  { key: 'action', title: '操作', type: 'button', width: 120, buttons: [
    { label: '修改', type: 'link', onClick: (rec: any) => handleEdit(rec) },
    { label: '删除', type: 'link', danger: true, onClick: (rec: any) => handleDelete(rec) },
  ]},
  { key: 'brandName', title: '品牌名称', width: 200 },
  { key: 'mnemonicCode', title: '助记码', width: 120 },
  { key: 'remark', title: '备注' },
]

// ── 单位列配置 ──
const unitColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 60 },
  { key: 'action', title: '操作', type: 'button', width: 120, buttons: [
    { label: '修改', type: 'link', onClick: (rec: any) => handleEdit(rec) },
    { label: '删除', type: 'link', danger: true, onClick: (rec: any) => handleDelete(rec) },
  ]},
  { key: 'unitName', title: '单位名称', width: 200 },
  { key: 'mnemonicCode', title: '助记码', width: 120 },
  { key: 'remark', title: '备注' },
]

// ── 标签列配置（含对应商品列）─
const tagColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 60 },
  { key: 'action', title: '操作', type: 'button', width: 120, buttons: [
    { label: '修改', type: 'link', onClick: (rec: any) => handleEdit(rec) },
    { label: '删除', type: 'link', danger: true, onClick: (rec: any) => handleDelete(rec) },
  ]},
  { key: 'tagName', title: '标签名称', width: 200 },
  { key: 'relatedProducts', title: '对应商品', type: 'slot', slotName: 'relatedProductsCell', width: 140 },
  { key: 'sortOrder', title: '排序', width: 80 },
]

const currentColumns = computed(() => {
  switch (activeTab.value) {
    case 'brand': return brandColumns
    case 'unit': return unitColumns
    case 'tag': return tagColumns
    default: return brandColumns
  }
})

// ── 分类树表格 ──
const categoryTreeData = ref<ProductCategory[]>([])
const categoryExpandedKeys = ref<string[]>([])

interface FlatCategory extends ProductCategory {
  _level: number
  _hasChildren: boolean
}

const categoryFlatRows = computed<FlatCategory[]>(() => {
  const result: FlatCategory[] = []
  const expandedSet = new Set(categoryExpandedKeys.value)
  const walk = (nodes: ProductCategory[], level: number) => {
    for (const node of nodes) {
      const hasChildren = !!(node.children?.length)
      const { children: _children, ...rest } = node
      result.push({ ...rest, _level: level, _hasChildren: hasChildren } as FlatCategory)
      if (hasChildren && expandedSet.has(node.id)) {
        walk(node.children!, level + 1)
      }
    }
  }
  walk(categoryTreeData.value, 0)
  return result
})

function toggleCategoryExpand(id: string) {
  const idx = categoryExpandedKeys.value.indexOf(id)
  if (idx === -1) {
    categoryExpandedKeys.value = [...categoryExpandedKeys.value, id]
  } else {
    categoryExpandedKeys.value = categoryExpandedKeys.value.filter(k => k !== id)
  }
}

// ── 分类列配置 ──
const categoryColDefs = [
  { title: '', key: 'rowNo', width: 60, align: 'center' as const },
  { title: '操作', key: 'action', width: 180, align: 'center' as const },
  { title: '分类名称', dataIndex: 'categoryName', key: 'categoryName', width: 240 },
  { title: '分类编码', dataIndex: 'categoryCode', key: 'categoryCode', width: 120 },
  { title: '层级', dataIndex: 'categoryLevel', key: 'categoryLevel', width: 80, align: 'center' as const },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80, align: 'center' as const },
]

async function fetchCategoryTree() {
  loading.value = true
  try {
    const data = await productCategoryApi.getTree()
    categoryTreeData.value = Array.isArray(data) ? data : []
    const firstLevelIds: string[] = []
    for (const node of categoryTreeData.value) {
      if (node.children?.length) firstLevelIds.push(node.id)
    }
    categoryExpandedKeys.value = firstLevelIds
  } catch (e) {
    console.error('[商品辅助资料] 加载分类树失败', e)
  } finally {
    loading.value = false
  }
}

// ── 品牌弹窗 ──
const brandModalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editingId = ref<string | null>(null)
const brandForm = reactive({ brandName: '', mnemonicCode: '', remark: '' })

// 品牌名称变化时自动生成助记码
watch(() => brandForm.brandName, (val) => {
  brandForm.mnemonicCode = toMnemonicCode(val || '')
})

function handleAdd() {
  isEdit.value = false
  editingId.value = null
  switch (activeTab.value) {
    case 'brand':
      Object.assign(brandForm, { brandName: '', mnemonicCode: '', remark: '' })
      brandModalVisible.value = true
      break
    case 'unit':
      Object.assign(unitForm, { unitName: '', mnemonicCode: '', remark: '' })
      unitModalVisible.value = true
      break
    case 'tag':
      Object.assign(tagForm, { tagName: '', sortOrder: 0 })
      tagModalVisible.value = true
      break
    case 'category':
      Object.assign(categoryForm, { categoryName: '', categoryCode: '', parentId: undefined, sortOrder: 0 })
      categoryModalVisible.value = true
      break
  }
}

function handleEdit(record: any) {
  isEdit.value = true
  editingId.value = record.id
  switch (activeTab.value) {
    case 'brand':
      Object.assign(brandForm, {
        brandName: record.brandName || '',
        mnemonicCode: record.mnemonicCode || '',
        remark: record.remark || '',
      })
      brandModalVisible.value = true
      break
    case 'unit':
      Object.assign(unitForm, {
        unitName: record.unitName || '',
        mnemonicCode: record.mnemonicCode || '',
        remark: record.remark || '',
      })
      unitModalVisible.value = true
      break
    case 'tag':
      Object.assign(tagForm, {
        tagName: record.tagName || '',
        sortOrder: record.sortOrder ?? 0,
      })
      tagModalVisible.value = true
      break
  }
}

async function handleBrandSubmit() {
  if (!brandForm.brandName?.trim()) { message.warning('请输入品牌名称'); return }
  modalLoading.value = true
  try {
    const payload: Partial<ProductBrand> = {
      brandName: brandForm.brandName,
      mnemonicCode: brandForm.mnemonicCode,
      remark: brandForm.remark,
    }
    if (isEdit.value && editingId.value) {
      await productBrandApi.update(editingId.value, payload)
    } else {
      await productBrandApi.create(payload)
    }
    message.success(isEdit.value ? '修改成功' : '新增成功')
    brandModalVisible.value = false
    fetchData()
  } catch { message.error('操作失败') } finally { modalLoading.value = false }
}

// ── 单位弹窗 ──
const unitModalVisible = ref(false)
const unitForm = reactive({ unitName: '', mnemonicCode: '', remark: '' })

// 单位名称变化时自动生成助记码
watch(() => unitForm.unitName, (val) => {
  unitForm.mnemonicCode = toMnemonicCode(val || '')
})

async function handleUnitSubmit() {
  if (!unitForm.unitName?.trim()) { message.warning('请输入单位名称'); return }
  modalLoading.value = true
  try {
    const payload: Partial<ProductUnitDict> = {
      unitName: unitForm.unitName,
      mnemonicCode: unitForm.mnemonicCode,
      remark: unitForm.remark,
    }
    if (isEdit.value && editingId.value) {
      await productUnitDictApi.update(editingId.value, payload)
    } else {
      await productUnitDictApi.create(payload)
    }
    message.success(isEdit.value ? '修改成功' : '新增成功')
    unitModalVisible.value = false
    fetchData()
  } catch { message.error('操作失败') } finally { modalLoading.value = false }
}

// ── 标签弹窗 ──
const tagModalVisible = ref(false)
const tagForm = reactive({ tagName: '', sortOrder: 0 })

async function handleTagSubmit() {
  if (!tagForm.tagName?.trim()) { message.warning('请输入标签名称'); return }
  modalLoading.value = true
  try {
    const payload: Partial<MallTag> = { tagName: tagForm.tagName, sortOrder: tagForm.sortOrder }
    if (isEdit.value && editingId.value) {
      await mallTagApi.update(editingId.value, payload)
    } else {
      await mallTagApi.create(payload)
    }
    message.success(isEdit.value ? '修改成功' : '新增成功')
    tagModalVisible.value = false
    fetchData()
  } catch { message.error('操作失败') } finally { modalLoading.value = false }
}

// ── 分类弹窗 ──
const categoryModalVisible = ref(false)
const categoryForm = reactive({
  categoryName: '', categoryCode: '', parentId: undefined as string | undefined, sortOrder: 0,
})

function handleAddSubCategory(parentRecord: any) {
  isEdit.value = false
  Object.assign(categoryForm, { categoryName: '', categoryCode: '', parentId: parentRecord.id, sortOrder: 0 })
  categoryModalVisible.value = true
}

function handleEditCategory(record: any) {
  isEdit.value = true
  editingId.value = record.id
  Object.assign(categoryForm, {
    categoryName: record.categoryName || '',
    categoryCode: record.categoryCode || '',
    parentId: record.parentId && record.parentId !== '0' ? record.parentId : undefined,
    sortOrder: record.sortOrder ?? 0,
  })
  categoryModalVisible.value = true
}

async function handleCategorySubmit() {
  if (!categoryForm.categoryName?.trim()) { message.warning('请输入分类名称'); return }
  modalLoading.value = true
  try {
    const payload: any = {
      categoryName: categoryForm.categoryName,
      categoryCode: categoryForm.categoryCode,
      parentId: categoryForm.parentId || '0',
      sortOrder: categoryForm.sortOrder,
    }
    if (isEdit.value && editingId.value) {
      await productCategoryApi.update(editingId.value, payload)
    } else {
      await productCategoryApi.create(payload)
    }
    message.success(isEdit.value ? '修改成功' : '新增成功')
    categoryModalVisible.value = false
    fetchCategoryTree()
  } catch { message.error('操作失败') } finally { modalLoading.value = false }
}

// ── 删除 ──
function handleDelete(record: any) {
  const name = record.brandName || record.unitName || record.tagName || ''
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除"${name}"吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        switch (activeTab.value) {
          case 'brand': await productBrandApi.delete(record.id); break
          case 'unit': await productUnitDictApi.delete(record.id); break
          case 'tag': await mallTagApi.delete(record.id); break
        }
        message.success('删除成功')
        fetchData()
      } catch { message.error('删除失败') }
    },
  })
}

function handleDeleteCategory(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除分类"${record.categoryName}"吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await productCategoryApi.delete(record.id)
        message.success('删除成功')
        fetchCategoryTree()
      } catch { message.error('删除失败') }
    },
  })
}

// ── 标签-对应商品选择器 ──
const productSelectorVisible = ref(false)
const productSelectorLoading = ref(false)
const productSelectorData = ref<Product[]>([])
const selectedProductIds = ref<(string | number)[]>([])
const productSearchKeyword = ref('')
let currentTagForProducts: MallTag | null = null

const productSelectorColumns = [
  { title: '商品编码', dataIndex: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName' },
  { title: '规格', dataIndex: 'spec', width: 100 },
  { title: '单位', dataIndex: 'unit', width: 60 },
]

async function openProductSelector(tag: MallTag) {
  currentTagForProducts = tag
  selectedProductIds.value = []
  productSearchKeyword.value = ''
  productSelectorVisible.value = true
  await loadProductsForTag()
}

async function loadProductsForTag() {
  productSelectorLoading.value = true
  try {
    const res = await productApi.page({
      pageNum: 1,
      pageSize: 200,
      ...(productSearchKeyword.value ? { productName: productSearchKeyword.value } : {}),
    } as any)
    productSelectorData.value = res.records || []
  } catch (e) {
    console.error('[商品辅助资料] 加载商品列表失败', e)
  } finally {
    productSelectorLoading.value = false
  }
}

function searchProductsForTag() { loadProductsForTag() }
function onProductSelectChange(keys: (string | number)[]) { selectedProductIds.value = keys }

async function handleProductSelectorOk() {
  message.success(`已选择 ${selectedProductIds.value.length} 个商品关联到标签"${currentTagForProducts?.tagName}"`)
  productSelectorVisible.value = false
}

// ── 分页 ──
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ── 键盘快捷键 ──
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); handleRefresh() }
}

// ── 初始化 ──
onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

function handleError(err: any) { console.warn('[商品辅助资料] ErrorBoundary:', err) }
</script>

<style scoped>
/* ── Tab标签栏（居中） ── */
.tab-bar {
  background: #4a5568;
  flex-shrink: 0;
}
.tab-bar-inner {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  height: 38px;
  gap: 2px;
  padding: 0 16px;
}
.tab-item {
  padding: 8px 24px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
  white-space: nowrap;
  border-radius: 4px 4px 0 0;
}
.tab-item:hover:not(.active) {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}
.tab-item.active {
  color: #333;
  background: #fff;
  font-weight: 500;
  padding-bottom: 10px;
  margin-bottom: -1px;
}

/* ── 工具栏 ── */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ── 橙色新增按钮 ── */
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

/* ── 表格区域 ── */
.table-wrapper {
  flex: 1;
  min-height: 0;
  overflow: hidden; /* 让 BillDetailTable 内部控制滚动 */
  background: #fff;
  display: flex;
  flex-direction: column;
}

/* ── 树缩进（分类tab） ── */
.tree-indent {
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
}
.tree-expand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin-right: 4px;
  cursor: pointer;
  border-radius: 3px;
  font-size: 10px;
  color: #606266;
  transition: background 0.15s;
  flex-shrink: 0;
}
.tree-expand-icon:hover {
  background: #e8e8e8;
}
.tree-expand-placeholder {
  display: inline-block;
  width: 16px;
  height: 16px;
  margin-right: 4px;
  flex-shrink: 0;
}
.tree-node-clickable { cursor: pointer; }
.tree-node-clickable:hover { color: #1890ff; }

/* ── 分类树表格样式 ── */
.category-tree-table :deep(.ant-table) { font-size: 13px; }
.category-tree-table :deep(.ant-table table) { border-collapse: separate; border-spacing: 0; }
.category-tree-table :deep(.ant-table-thead > tr > th) {
  position: sticky; top: 0; z-index: 10;
  background: #fafafa !important;
  border-right: 1px solid #e8e8e8;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 6px !important;
  text-align: center; font-weight: 600; color: #262626;
  font-size: 13px; height: 32px !important; line-height: 32px !important;
  white-space: nowrap; vertical-align: middle;
}
.category-tree-table :deep(.ant-table-thead > tr > th:last-child) { border-right: none; }
.category-tree-table :deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e8e8e8;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 6px !important;
  height: 28px !important; max-height: 28px !important;
  line-height: 28px !important; font-size: 13px;
  vertical-align: middle; box-sizing: border-box;
  overflow: hidden;
}
.category-tree-table :deep(.ant-table-tbody > tr > td:last-child) { border-right: none; }
.category-tree-table :deep(.ant-table-tbody > tr:hover > td) { background: #f5f7fa !important; }
.category-tree-table :deep(.ant-table-thead > tr > th:first-child) {
  text-align: center;
  padding: 0 !important;
}
/* 移除 th 的 ::before 列分隔线，避免与 border-bottom 叠加 */
.category-tree-table :deep(.ant-table-thead > tr > th::before) {
  display: none !important;
}
/* 确保表头/表体分割线仅 1px */
.category-tree-table :deep(.ant-table-thead > tr > th) {
  border-bottom: 1px solid #e8e8e8 !important;
}
.category-tree-table :deep(.ant-table-tbody .ant-btn-link) {
  height: 22px !important; line-height: 22px !important;
  padding: 0 4px !important; margin: 0 !important; font-size: 13px;
}

/* ── 底部分页 ── */
.pagination-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #fafafa;
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.total-text { font-size: 13px; color: #666; }

/* ── 对应商品数量标记 ── */
.product-count { font-size: 12px; color: #1890ff; margin-left: 4px; }

/* ── BillDetailTable 内联样式覆盖 ── */
:deep(.bill-detail-table) { flex: 1; }
:deep(.bill-detail-table .spreadsheet-table) { min-height: 100%; }

/* ── 填充列：表头保留背景色，表体自动继承表格边框 ── */
.category-tree-table :deep(.ant-table-thead .ss-filler-col) {
  background: #fafafa;
}
.category-tree-table :deep(.ant-table-thead .ss-filler-col::before) {
  display: none !important;
}
.category-tree-table :deep(.ant-table-thead .ss-filler-col .ant-table-column-sorter) {
  display: none;
}

/* ── 紧凑尺寸 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
