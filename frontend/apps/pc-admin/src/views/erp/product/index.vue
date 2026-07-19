<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- 分类列表布局 -->
      <CategoryListLayout
        :show-category-panel="true"
        category-title="商品分类"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :category-error="categoryError"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="expandedKeys"
        :tabs="tabs"
        :active-tab="activeTab"
        :current-path="currentCategoryPath"
        :show-table-footer="!tableExpanded"
        @category-add="showCategoryModal(null)"
        @category-retry="fetchCategoryTree"
        @category-select="onCategorySelect"
        @category-expand="onExpand"
        @tab-change="onTabChange"
        @search="handleSearch"
      >
        <!-- 工具栏左侧 -->
        <template #toolbar-left>
          <a-space>
            <a-button
              type="primary"
              class="btn-add"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增
            </a-button>
            <a-button
              size="small"
              @click="handleImport"
            >
              <UploadOutlined /> 导入
            </a-button>
            <a-button
              size="small"
              @click="handleCloudImport"
            >
              <CloudUploadOutlined /> 云导入
            </a-button>
          </a-space>
        </template>

        <!-- 工具栏右侧 -->
        <template #toolbar-right>
          <a-space>
            <a-button
              size="small"
              @click="refreshAll"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              size="small"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
            <a-dropdown>
              <a-button size="small">
                更多 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="handleBatchPrice">
                    批量改价
                  </a-menu-item>
                  <a-menu-item @click="handleBatchStatus">
                    批量改状态
                  </a-menu-item>
                  <a-menu-item
                    danger
                    @click="handleBatchDelete"
                  >
                    批量删除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>

        <!-- 搜索字段 -->
        <template #search-fields>
          <div class="search-row">
            <div class="search-item">
              <span class="search-label">筛选条件</span>
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="商品名称/货号/条码/规格"
                size="small"
                style="width: 200px"
                allow-clear
                @press-enter="handleSearch"
              />
            </div>
            <div class="search-item">
              <span class="search-label">品牌</span>
              <a-select
                v-model:value="searchForm.brand"
                size="small"
                style="width: 140px"
                placeholder="全部"
                show-search
                allow-clear
                :filter-option="filterBrandOption"
              >
                <a-select-option
                  v-for="b in brandOptions"
                  :key="b"
                  :value="b"
                >
                  {{ b }}
                </a-select-option>
              </a-select>
            </div>
            <div class="search-item">
              <span class="search-label">新增日期（起）</span>
              <a-date-picker
                v-model:value="searchForm.createTimeStart"
                size="small"
                style="width: 140px"
                placeholder=""
              />
            </div>
            <div class="search-item">
              <span class="search-label">新增日期（止）</span>
              <a-date-picker
                v-model:value="searchForm.createTimeEnd"
                size="small"
                style="width: 140px"
                placeholder=""
              />
            </div>
            <div class="search-item">
              <span class="search-label">显示状态</span>
              <a-select
                v-model:value="searchForm.status"
                size="small"
                style="width: 120px"
                placeholder="全部"
                allow-clear
              >
                <a-select-option value="">
                  全部
                </a-select-option>
                <a-select-option value="ENABLED">
                  已启用
                </a-select-option>
                <a-select-option value="DISABLED">
                  已停用
                </a-select-option>
              </a-select>
            </div>
            <div class="search-item">
              <span class="search-label">使用优惠券</span>
              <a-select
                v-model:value="searchForm.useCoupon"
                size="small"
                style="width: 100px"
                placeholder="全部"
                allow-clear
              >
                <a-select-option value="">
                  全部
                </a-select-option>
                <a-select-option :value="1">
                  是
                </a-select-option>
                <a-select-option :value="0">
                  否
                </a-select-option>
              </a-select>
            </div>
            <div class="search-item">
              <span class="search-label">是否标品</span>
              <a-select
                v-model:value="searchForm.isStandardProduct"
                size="small"
                style="width: 100px"
                placeholder="全部"
                allow-clear
              >
                <a-select-option value="">
                  全部
                </a-select-option>
                <a-select-option :value="1">
                  是
                </a-select-option>
                <a-select-option :value="0">
                  否
                </a-select-option>
              </a-select>
            </div>
          </div>
          <div class="search-row second-row">
            <div class="search-item">
              <span class="search-label">所属行业类别</span>
              <a-select
                v-model:value="searchForm.industryCategory"
                size="small"
                style="width: 140px"
                placeholder="全部"
                show-search
                allow-clear
                :filter-option="filterIndustryOption"
              >
                <a-select-option
                  v-for="item in industryCategoryOptions"
                  :key="item"
                  :value="item"
                >
                  {{ item }}
                </a-select-option>
              </a-select>
            </div>
            <a-button
              type="primary"
              size="small"
              class="btn-search"
              @click="handleSearch"
            >
              查询
            </a-button>
            <a-checkbox
              v-model:checked="showHierarchy"
              size="small"
              style="margin-left: 12px"
            >
              显示层次结构
            </a-checkbox>
          </div>
        </template>

        <!-- 表格 -->
        <template #table>
          <BillDetailTable
            ref="tableRef"
            :columns="detailColumns"
            :data-source="tableData"
            :loading="loading"
            :view-mode="true"
            :fill-mode="true"
            storage-key="erp-product-list-columns"
            @checkbox-change="handleCheckboxChange"
            @checkbox-all="handleCheckboxAll"
            @expand-change="onTableExpand"
          >
            <!-- 自定义：操作列 -->
            <template #actionCell="{ record, index }">
              <a-space :size="0">
                <a-button
                  type="link"
                  size="small"
                  @click="handleEdit(record)"
                >
                  修改
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  @click="handleToggleStatus(record)"
                >
                  {{ record.status === 'ENABLED' ? '停用' : '启用' }}
                </a-button>
                <a-dropdown>
                  <a-button
                    type="link"
                    size="small"
                  >
                    更多
                  </a-button>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item @click="handleView(record)">
                        查看详情
                      </a-menu-item>
                      <a-menu-item @click="handleCopy(record)">
                        复制
                      </a-menu-item>
                      <a-menu-item
                        danger
                        @click="handleDelete(record)"
                      >
                        删除
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </a-space>
            </template>

            <!-- 自定义：图片列 -->
            <template #imageCell="{ record }">
              <a-image
                v-if="record.imageUrl"
                :src="record.imageUrl"
                style="width: 36px; height: 36px; border-radius: 4px; object-fit: cover;"
                :preview="{ mask: false }"
                fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
              />
              <span
                v-else
                class="no-image"
              >-</span>
            </template>

            <!-- 自定义：商品名称列（可点击） -->
            <template #productNameCell="{ record }">
              <a
                class="cell-link"
                @click="handleView(record)"
              >{{ record.productName }}</a>
            </template>
          </BillDetailTable>
        </template>

        <!-- 表格底部：分页器 -->
        <template #table-footer>
          <StandardPagination
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[10, 20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- 分类新增/编辑弹窗 -->
      <FullScreenDetail
        :visible="categoryModalVisible"
        :title="editingCategory ? '编辑分类' : '新增分类'"
        :save-loading="categoryModalLoading"
        @close="handleCategoryCancel"
        @save="handleCategoryOk"
      >
        <a-form
          ref="categoryFormRef"
          :model="categoryForm"
          :rules="categoryRules"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 17 }"
        >
          <a-form-item
            label="分类名称"
            name="categoryName"
          >
            <a-input
              v-model:value="categoryForm.categoryName"
              placeholder="请输入分类名称"
              size="small"
            />
          </a-form-item>
          <a-form-item label="分类编码">
            <a-input
              v-model:value="categoryForm.categoryCode"
              placeholder="留空则系统自动生成"
              size="small"
            />
          </a-form-item>
          <a-form-item label="上级分类">
            <a-tree-select
              v-model:value="categoryForm.parentId"
              :tree-data="categoryTreeData"
              :field-names="{ children: 'children', label: 'categoryName', value: 'id' }"
              placeholder="无(根节点)"
              allow-clear
              size="small"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="排序">
            <a-input-number
              v-model:value="categoryForm.sortOrder"
              :min="0"
              size="small"
              style="width: 100%"
            />
          </a-form-item>
        </a-form>
      </FullScreenDetail>

      <!-- 商品导入弹窗 -->
      <FullScreenDetail
        :visible="importModalVisible"
        title="导入商品"
        :save-loading="importLoading"
        @close="importModalVisible = false"
        @save="handleImportOk"
      >
        <div style="padding: 16px 0">
          <a-upload-dragger
            v-model:file-list="importFileList"
            :before-upload="beforeImportUpload"
            :max-count="1"
            accept=".xlsx,.xls,.csv"
          >
            <p class="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p class="ant-upload-text">
              点击或拖拽文件到此区域上传
            </p>
            <p class="ant-upload-hint">
              支持 .xlsx / .xls / .csv 格式，单次最多1个文件
            </p>
          </a-upload-dragger>
          <a-alert
            v-if="importResult"
            :type="importResult.success ? 'success' : 'error'"
            :message="importResult.message"
            show-icon
            style="margin-top: 12px"
          />
        </div>
      </FullScreenDetail>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  InboxOutlined,
  UploadOutlined,
  CloudUploadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  DownOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { productApi, productCategoryApi, productGradeApi } from '@/api/erp/product'
import type { ProductCategory, ProductGrade } from '@/api/erp/product'
import request from '@/utils/request'

const router = useRouter()

// ── localStorage 分页记忆 ──
const PAGE_SIZE_STORAGE_KEY = 'erp_product_page_size'
const getSavedPageSize = (): number => {
  const saved = localStorage.getItem(PAGE_SIZE_STORAGE_KEY)
  const n = saved ? parseInt(saved, 10) : NaN
  return (n && [10, 20, 50, 100].includes(n)) ? n : 20
}

// ── Tab标签 ──
const tabs = [
  { key: 'all', label: '全部商品' },
  { key: 'package', label: '套餐' },
  { key: 'shelf', label: '商品上架' },
  { key: 'auth', label: '商品授权' },
]
const activeTab = ref('all')

function onTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  fetchProducts()
}

/** 根据当前tab获取查询参数 */
function getTabQueryParams(): Record<string, any> {
  const params: Record<string, any> = {}
  switch (activeTab.value) {
    case 'package':
      params.productType = 'KIT'
      break
    case 'shelf':
      params.mallShelfStatus = 1
      break
    case 'auth':
      // 商品授权：暂无专用字段，先展示全部（预留）
      break
  }
  return params
}

// ── 品牌选项 ──
const brandOptions = ref<string[]>([])

async function fetchBrandOptions() {
  try {
    brandOptions.value = await productApi.getBrands()
  } catch {
    brandOptions.value = []
  }
}

function filterBrandOption(input: string, option: any) {
  return option.children[0].children.toLowerCase().includes(input.toLowerCase())
}

// ── 行业类别选项 ──
const industryCategoryOptions = ref<string[]>([])

async function fetchIndustryCategoryOptions() {
  try {
    const data = await productApi.getIndustryCategories()
    industryCategoryOptions.value = Array.isArray(data) ? data : []
  } catch {
    industryCategoryOptions.value = []
  }
}

function filterIndustryOption(input: string, option: any) {
  return option.children[0].children.toLowerCase().includes(input.toLowerCase())
}

// ── 分类管理 ──
const categoryLoading = ref(false)
const categoryError = ref(false)
const categoryTree = ref<any[]>([])
const selectedCategoryId = ref<string>('0')
const expandedKeys = ref<string[]>([])

const categoryTreeData = computed(() => categoryTree.value)

// ── 价格等级 ─
const grades = ref<ProductGrade[]>([])

async function fetchGrades() {
  try {
    const data = await productGradeApi.list()
    grades.value = (Array.isArray(data) ? data : [])
      .filter(g => g.status === 1)
      .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
  } catch (e) {
    console.error('[商品列表] 加载价格等级失败', e)
  }
}

// 计算当前选中分类的路径
const currentCategoryPath = computed(() => {
  if (selectedCategoryId.value === '0' || !categoryTree.value.length) return '全部商品'
  const path: string[] = []
  function find(nodes: any[], target: string): boolean {
    for (const node of nodes) {
      path.push(node.categoryName)
      if (String(node.id) === target) return true
      if (node.children?.length && find(node.children, target)) return true
      path.pop()
    }
    return false
  }
  find(categoryTree.value, selectedCategoryId.value)
  return path.length ? path.join(' / ') : '全部商品'
})

async function fetchCategoryTree() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const data = await productCategoryApi.getTree()
    categoryTree.value = Array.isArray(data) ? data : []
    const firstLevel = categoryTree.value.map(n => String(n.id))
    if (firstLevel.length > 0) {
      const existingSet = new Set(expandedKeys.value)
      const merged = new Set([...firstLevel, ...existingSet])
      expandedKeys.value = [...merged]
    }
  } catch (e) {
    console.error('[商品分类] 加载分类树失败', e)
    categoryError.value = true
  } finally {
    categoryLoading.value = false
  }
}

function onCategorySelect(keys: (string | number)[]) {
  const key = keys[0]
  selectedCategoryId.value = key != null ? String(key) : '0'
  pagination.current = 1
  fetchProducts()
}

function onExpand(keys: (string | number)[]) {
  expandedKeys.value = keys.map(String)
}

// ── 分类弹窗 ──
const categoryModalVisible = ref(false)
const categoryModalLoading = ref(false)
const editingCategory = ref<ProductCategory | null>(null)
const categoryFormRef = ref<any>(null)
const categoryForm = reactive({
  categoryName: '',
  categoryCode: '',
  parentId: undefined as string | undefined,
  sortOrder: 0,
})
const categoryRules = {
  categoryName: [{ required: true, message: '请输入分类名称' }],
}

function showCategoryModal(category: ProductCategory | null) {
  editingCategory.value = category
  if (category) {
    Object.assign(categoryForm, {
      categoryName: category.categoryName,
      categoryCode: category.categoryCode,
      parentId: category.parentId || undefined,
      sortOrder: category.sortOrder || 0,
    })
  } else {
    Object.assign(categoryForm, {
      categoryName: '',
      categoryCode: '',
      parentId: selectedCategoryId.value !== '0' ? selectedCategoryId.value : undefined,
      sortOrder: 0,
    })
  }
  categoryModalVisible.value = true
}

async function handleCategoryOk() {
  const parentIdForExpand = categoryForm.parentId ? String(categoryForm.parentId) : null
  try {
    await categoryFormRef.value?.validate()
    categoryModalLoading.value = true
    const catPayload: Partial<ProductCategory> = {
      categoryName: categoryForm.categoryName,
      categoryCode: categoryForm.categoryCode,
      parentId: categoryForm.parentId,
      sortOrder: categoryForm.sortOrder,
    }
    if (editingCategory.value) {
      await productCategoryApi.update(editingCategory.value.id, catPayload)
      message.success('更新成功')
    } else {
      await productCategoryApi.create(catPayload)
      message.success('新增成功')
    }
    categoryModalVisible.value = false
    await fetchCategoryTree()
    if (parentIdForExpand) {
      const keySet = new Set(expandedKeys.value)
      keySet.add(parentIdForExpand)
      expandedKeys.value = [...keySet]
    }
    await fetchProducts()
  } catch (e: unknown) {
    if (e && typeof e === 'object' && 'errorFields' in e) return
    const msg = e instanceof Error ? e.message : '操作失败'
    message.error(msg)
  } finally {
    categoryModalLoading.value = false
  }
}

function handleCategoryCancel() {
  categoryModalVisible.value = false
}

// ── 搜索表单 ──
const searchForm = reactive({
  keyword: '',
  brand: '',
  createTimeStart: null as any,
  createTimeEnd: null as any,
  status: '',
  useCoupon: '' as any,
  isStandardProduct: '' as any,
  industryCategory: '',
})
const showHierarchy = ref(false)

// ── 商品列表 ──
const loading = ref(false)
const tableData = ref<any[]>([])
const selectedRows = ref<any[]>([])
const tableRef = ref<any>(null)
const tableExpanded = ref(false)

function onTableExpand(expanded: boolean) {
  tableExpanded.value = expanded
}

// 表格列配置（所有列均支持隐藏）
const detailColumns = computed<DetailColumnConfig[]>(() => {
  const fixedColumns: DetailColumnConfig[] = [
    { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
    { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
    { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 100, fixed: 'left' },
    { key: 'imageUrl', title: '图片', type: 'slot', slotName: 'imageCell', width: 60, hideable: true },
    { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productNameCell', width: 200, sortable: true },
    { key: 'productCodeAlias', title: '商品货号', type: 'input', width: 120, sortable: true, hideable: true },
    { key: 'categoryName', title: '分类', type: 'input', width: 110, sortable: true, hideable: true },
  ]
  const tailColumns: DetailColumnConfig[] = [
    { key: 'industryCategory', title: '所属行业类别', type: 'input', width: 120, sortable: true, hideable: true },
    { key: 'barcode', title: '条码', type: 'input', width: 140, sortable: true, hideable: true },
    { key: 'spec', title: '规格', type: 'input', width: 120, sortable: true, hideable: true },
    { key: 'model', title: '型号', type: 'input', width: 100, sortable: true, hideable: true, defaultHidden: true },
    { key: 'origin', title: '产地', type: 'input', width: 100, sortable: true, hideable: true, defaultHidden: true },
    { key: 'brand', title: '品牌', type: 'input', width: 100, sortable: true, hideable: true },
    { key: 'unit', title: '单位', type: 'input', width: 80, hideable: true, defaultHidden: true },
    { key: 'status', title: '状态', type: 'input', width: 80, hideable: true },
  ]

  // 动态等级价格列
  const gradeColumns: DetailColumnConfig[] = grades.value.map((grade, index) => ({
    key: `_gp${index}`,
    title: grade.gradeName,
    type: 'input' as const,
    width: 90,
    align: 'right' as const,
    hideable: true,
    formatter: (v: any) => (v != null && v !== '' ? Number(v).toFixed(2) : '-'),
  }))

  return [...fixedColumns, ...gradeColumns, ...tailColumns]
})

const pagination = reactive({
  current: 1,
  pageSize: getSavedPageSize(),
  total: 0,
})

async function fetchProducts() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (selectedCategoryId.value && selectedCategoryId.value !== '0') {
      params.categoryId = selectedCategoryId.value
    }
    if (searchForm.keyword) {
      params.keyword = searchForm.keyword
    }
    if (searchForm.status) {
      params.status = searchForm.status
    }
    if (searchForm.brand) {
      params.brand = searchForm.brand
    }
    if (searchForm.industryCategory) {
      params.industryCategory = searchForm.industryCategory
    }
    if (searchForm.createTimeStart) {
      params.createTimeStart = searchForm.createTimeStart.format('YYYY-MM-DD')
    }
    if (searchForm.createTimeEnd) {
      params.createTimeEnd = searchForm.createTimeEnd.format('YYYY-MM-DD')
    }
    if (searchForm.useCoupon !== '' && searchForm.useCoupon !== null && searchForm.useCoupon !== undefined) {
      params.useCoupon = Number(searchForm.useCoupon)
    }
    if (searchForm.isStandardProduct !== '' && searchForm.isStandardProduct !== null && searchForm.isStandardProduct !== undefined) {
      params.isStandardProduct = Number(searchForm.isStandardProduct)
    }

    // Tab 过滤参数
    Object.assign(params, getTabQueryParams())

    const res = await productApi.page(params)
    const records = res.records || []
    tableData.value = records.map((r: any) => {
      const flat = { ...r }
      const gpm = r.gradePriceMap as Record<string, number> | undefined
      if (gpm) {
        grades.value.forEach((_grade, index) => {
          flat[`_gp${index}`] = gpm[`GRADE_${index + 1}`] ?? null
        })
      }
      return flat
    })
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[商品列表] 加载失败', e)
    message.error('加载商品列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchProducts()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  localStorage.setItem(PAGE_SIZE_STORAGE_KEY, String(pageSize))
  fetchProducts()
}

function handleCheckboxChange(_record: any, _rowIndex: number, _checked: boolean) {
  selectedRows.value = tableRef.value?.getCheckedRecords?.() || []
}

function handleCheckboxAll(_checked: boolean, records: any[]) {
  selectedRows.value = records
}

// ─ 操作按钮 ──
function handleAdd() {
  router.push('/erp/product/create')
}

function handleEdit(row: any) {
  router.push(`/erp/product/form/${row.id}`)
}

function handleView(row: any) {
  router.push(`/erp/product/form/${row.id}`)
}

function handleToggleStatus(row: any) {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  Modal.confirm({
    title: '确认',
    content: `确定要${newStatus === 'ENABLED' ? '启用' : '停用'}该商品吗？`,
    onOk: async () => {
      try {
        await productApi.updateStatus(row.id, newStatus)
        message.success(newStatus === 'ENABLED' ? '已启用' : '已停用')
        fetchProducts()
      } catch {
        message.error('操作失败')
      }
    },
  })
}

function handleDelete(row: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除商品 "${row.productName}" 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await productApi.delete(row.id)
        message.success('删除成功')
        // 删除后如果当前页只剩1条且不是第1页，自动回退到上一页
        if (tableData.value.length === 1 && pagination.current > 1) {
          pagination.current--
        }
        fetchProducts()
      } catch {
        message.error('删除失败')
      }
    },
  })
}

function handleCopy(row: any) {
  router.push(`/erp/product/create?copyFrom=${row.id}`)
}

// ── 导入功能 ──
const importModalVisible = ref(false)
const importLoading = ref(false)
const importFileList = ref<any[]>([])
const importResult = ref<{ success: boolean; message: string } | null>(null)

function handleImport() {
  importResult.value = null
  importFileList.value = []
  importModalVisible.value = true
}

function handleCloudImport() {
  message.info('云导入功能开发中')
}

function beforeImportUpload(file: File) {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!['xlsx', 'xls', 'csv'].includes(ext || '')) {
    message.error('仅支持 .xlsx / .xls / .csv 文件')
    return false
  }
  importResult.value = null
  return false // 阻止自动上传，手动控制
}

async function handleImportOk() {
  if (importFileList.value.length === 0) {
    message.warning('请先选择文件')
    return
  }
  const file = importFileList.value[0].originFileObj
  if (!file) {
    message.warning('请先选择文件')
    return
  }
  importLoading.value = true
  importResult.value = null
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/erp/product/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    importResult.value = {
      success: true,
      message: `导入成功，共导入 ${res?.count ?? 0} 条商品`,
    }
    importFileList.value = []
    await fetchProducts()
  } catch (e: any) {
    importResult.value = {
      success: false,
      message: e?.message || '导入失败，请检查文件格式',
    }
  } finally {
    importLoading.value = false
  }
}

function handlePrint() {
  window.print()
}

async function handleExport() {
  try {
    const params: any = {}
    if (selectedCategoryId.value && selectedCategoryId.value !== '0') {
      params.categoryId = selectedCategoryId.value
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.brand) params.brand = searchForm.brand
    if (searchForm.industryCategory) params.industryCategory = searchForm.industryCategory
    if (searchForm.createTimeStart) params.createTimeStart = searchForm.createTimeStart.format('YYYY-MM-DD')
    if (searchForm.createTimeEnd) params.createTimeEnd = searchForm.createTimeEnd.format('YYYY-MM-DD')
    if (searchForm.useCoupon !== '' && searchForm.useCoupon !== null && searchForm.useCoupon !== undefined) {
      params.useCoupon = Number(searchForm.useCoupon)
    }
    if (searchForm.isStandardProduct !== '' && searchForm.isStandardProduct !== null && searchForm.isStandardProduct !== undefined) {
      params.isStandardProduct = Number(searchForm.isStandardProduct)
    }

    const blob = await request.get('/erp/product/export', { params, responseType: 'blob' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `商品_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch {
    message.warning('导出失败')
  }
}

function handleBatchPrice() {
  if (selectedRows.value.length === 0) {
    message.warning('请先选择商品')
    return
  }
  router.push('/erp/product/price-batch')
}

function handleBatchStatus() {
  if (selectedRows.value.length === 0) {
    message.warning('请先选择商品')
    return
  }
  Modal.confirm({
    title: '批量改状态',
    content: `确定要将选中的 ${selectedRows.value.length} 个商品切换启用/停用状态吗？`,
    onOk: async () => {
      try {
        // 计算目标状态：以第一个选中项的相反状态为准
        const firstStatus = selectedRows.value[0].status
        const targetStatus = firstStatus === 'ENABLED' ? 'DISABLED' : 'ENABLED'
        const ids = selectedRows.value.map((row: any) => Number(row.id))
        await productApi.batchUpdateStatus(ids, targetStatus)
        message.success(`批量${targetStatus === 'ENABLED' ? '启用' : '停用'}成功`)
        fetchProducts()
      } catch {
        message.error('批量改状态失败')
      }
    },
  })
}

function handleBatchDelete() {
  if (selectedRows.value.length === 0) {
    message.warning('请先选择商品')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${selectedRows.value.length} 个商品吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        const ids = selectedRows.value.map((row: any) => Number(row.id))
        await productApi.batchDelete(ids)
        message.success('批量删除成功')
        fetchProducts()
      } catch {
        message.error('批量删除失败')
      }
    },
  })
}

function refreshAll() {
  fetchCategoryTree()
  fetchProducts()
}

function handleError(err: any) {
  console.warn('[商品列表] ErrorBoundary 捕获异常:', err)
}

// ─ 键盘快捷键 ─
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey) {
    e.preventDefault()
    refreshAll()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleAdd()
  }
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

onMounted(() => {
  fetchGrades()
  fetchCategoryTree()
  fetchProducts()
  fetchBrandOptions()
  fetchIndustryCategoryOptions()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
/* ── 橙色新增按钮 ── */
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

/* ── 表格样式 ── */
.no-image {
  color: #ccc;
}

.cell-link {
  color: #1890ff;
  cursor: pointer;
}
.cell-link:hover {
  text-decoration: underline;
}

/* ── 搜索栏布局 ── */
.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-row.second-row {
  margin-top: 8px;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}
.btn-search {
  margin-left: 8px;
}

/* ── BillDetailTable 样式覆盖 ── */
:deep(.ss-grid th) {
  background: #fafafa !important;
  font-weight: 600 !important;
  color: #333 !important;
}

:deep(.ss-row:hover td) {
  background: #e6f7ff !important;
}

:deep(.category-list-layout) {
  flex: 1;
  min-height: 0;
  height: auto !important;
}
</style>
