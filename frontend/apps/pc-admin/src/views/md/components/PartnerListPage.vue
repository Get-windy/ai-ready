<template>
  <div class="partner-page-layout">
    <!-- 左侧类型导航 -->
    <PartnerTypeSidebar :active-key="partnerType" />

    <!-- 右侧主内容区 -->
    <div class="main-content">
      <!-- Tab标签栏 -->
      <div
        v-if="tabs.length > 1"
        class="tab-bar"
      >
        <div class="tab-items">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            :class="['tab-item', { active: activeTab === tab.key }]"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </div>
        </div>
      </div>

      <!-- 工具栏 -->
      <div class="toolbar-section">
        <div class="toolbar-left">
          <a-button
            type="primary"
            class="btn-add"
            @click="handleAdd"
          >
            <PlusOutlined /> 新增
          </a-button>
          <slot name="toolbar-left" />
          <a-button
            size="small"
            @click="showImportModal"
          >
            <UploadOutlined /> 导入
          </a-button>
        </div>
        <div class="toolbar-right">
          <slot name="toolbar-right" />
          <!-- 列设置 -->
          <a-dropdown v-if="columns.length > 0">
            <a-button
              size="small"
              title="列设置"
            >
              <SettingOutlined />
            </a-button>
            <template #overlay>
              <a-menu class="column-setting-menu">
                <a-menu-item
                  v-for="col in props.columns"
                  :key="col.key"
                  @click.stop
                >
                  <a-checkbox
                    :checked="visibleColumns.has(col.key)"
                    @change="toggleColumn(col.key)"
                  >
                    {{ col.title }}
                  </a-checkbox>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
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
          <a-button
            size="small"
            @click="handleBatchStatus"
          >
            批量修改
          </a-button>
          <a-dropdown>
            <a-button size="small">
              更多 <DownOutlined />
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item
                  danger
                  @click="handleBatchDelete"
                >
                  批量删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>

      <!-- 搜索栏（支持插槽自定义，否则使用默认搜索） -->
      <div class="search-section">
        <slot name="search-fields">
          <!-- 默认搜索行 -->
          <div class="search-row">
            <div class="search-item">
              <span class="search-label">筛选条件</span>
              <a-input
                v-model:value="searchForm.keyword"
                :placeholder="searchPlaceholder"
                size="small"
                style="width: 220px"
                allow-clear
                @press-enter="handleSearch"
              />
            </div>
            <div
              v-if="showStatusFilter"
              class="search-item"
            >
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
            <a-button
              type="primary"
              size="small"
              class="btn-search"
              @click="handleSearch"
            >
              查询
            </a-button>
          </div>
        </slot>
        <!-- 复选框过滤行 -->
        <div
          v-if="$slots['checkbox-filters']"
          class="checkbox-row"
        >
          <slot name="checkbox-filters" />
        </div>
      </div>

      <!-- 内容行：左侧分类树 + 右侧表格 -->
      <div class="content-row">
        <!-- 左侧分类面板 -->
        <div
          v-if="!categoryCollapsed"
          class="category-panel"
        >
          <div class="category-header">
            <span class="category-title">{{ categoryTitle }}</span>
            <div class="category-header-actions">
              <a-button
                type="link"
                size="small"
                title="新增分类"
                @click="showCategoryModal(null)"
              >
                <PlusOutlined />
              </a-button>
              <a-button
                type="link"
                size="small"
                title="折叠"
                @click="categoryCollapsed = true"
              >
                <MenuFoldOutlined />
              </a-button>
            </div>
          </div>
          <div class="category-tree-container">
            <a-spin :spinning="categoryLoading">
              <template v-if="categoryError">
                <div class="category-error">
                  <a-result
                    status="warning"
                    title="加载失败"
                    sub-title="点击重试"
                  >
                    <template #extra>
                      <a-button
                        size="small"
                        @click="fetchCategoryTree"
                      >
                        <ReloadOutlined /> 重试
                      </a-button>
                    </template>
                  </a-result>
                </div>
              </template>
              <template v-else-if="!categoryLoading && categoryTreeData.length === 0">
                <div class="category-empty">
                  <InboxOutlined class="category-empty-icon" />
                  <p class="category-empty-text">
                    暂无分类
                  </p>
                </div>
              </template>
              <a-tree
                v-else-if="!categoryLoading"
                :tree-data="categoryTreeData"
                :field-names="{ key: 'id', title: 'categoryName', children: 'children' }"
                :selected-keys="[selectedCategoryId]"
                :expanded-keys="expandedKeys"
                show-icon
                block-node
                @select="onCategorySelect"
                @expand="onExpand"
              >
                <template #title="{ categoryName }">
                  <span>{{ categoryName }}</span>
                </template>
                <template #icon="{ expanded }">
                  <FolderOpenOutlined
                    v-if="expanded"
                    style="color: #faad14"
                  />
                  <FolderOutlined
                    v-else
                    style="color: #faad14"
                  />
                </template>
              </a-tree>
            </a-spin>
          </div>
          <div class="category-breadcrumb">
            <span class="breadcrumb-text">当前路径：</span>
            <span class="breadcrumb-path">{{ currentCategoryPath }}</span>
          </div>
        </div>

        <!-- 折叠状态 -->
        <div
          v-else
          class="category-collapse-bar"
        >
          <a-button
            type="text"
            class="collapse-toggle-btn"
            title="展开"
            @click="categoryCollapsed = false"
          >
            <MenuUnfoldOutlined />
          </a-button>
        </div>

        <!-- 右侧表格区域 -->
        <div class="table-panel">
          <div class="table-section">
            <BillDetailTable
              ref="tableRef"
              :columns="mergedColumns"
              :data-source="tableData"
              :loading="loading"
              :view-mode="true"
              :fill-mode="true"
              @checkbox-change="handleCheckboxChange"
              @checkbox-all="handleCheckboxAll"
            >
              <!-- 操作列（优先使用子页面自定义，否则用默认） -->
              <template #actionCell="{ record }">
                <slot
                  name="actionCell"
                  :record="record"
                >
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
                </slot>
              </template>

              <!-- 名称列（可点击） -->
              <template #nameCell="{ record }">
                <a
                  class="cell-link"
                  @click="handleView(record)"
                >{{ record.partnerName }}</a>
              </template>

              <!-- 状态列 -->
              <template #statusCell="{ record }">
                <a-tag :color="record.status === 'ENABLED' ? 'green' : 'red'">
                  {{ record.status === 'ENABLED' ? '启用' : '停用' }}
                </a-tag>
              </template>

              <!-- 附件列 -->
              <template #attachmentCell="{ record }">
                <a-button
                  v-if="record.attachmentCount"
                  type="link"
                  size="small"
                >
                  <PaperClipOutlined /> {{ record.attachmentCount }}
                </a-button>
                <span v-else>-</span>
              </template>

              <!-- 透传外部自定义插槽 -->
              <template
                v-for="(_, name) in $slots"
                #[name]="slotData"
              >
                <slot
                  :name="name"
                  v-bind="slotData || {}"
                />
              </template>
            </BillDetailTable>
          </div>

          <!-- 分页器 -->
          <StandardPagination
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="pageSizeOptions"
            @change="handlePageChange"
          />
        </div>
      </div>

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

      <!-- 导入弹窗 -->
      <a-modal
        v-model:open="importModalVisible"
        title="数据导入"
        :confirm-loading="importLoading"
        @ok="handleImportUpload"
        @cancel="importModalVisible = false"
      >
        <a-alert
          message="请上传Excel文件（.xlsx/.xls），系统将自动解析并导入数据。"
          type="info"
          show-icon
          style="margin-bottom: 16px"
        />
        <a-upload-dragger
          v-model:file-list="importFileList"
          :before-upload="() => false"
          accept=".xlsx,.xls"
          :max-count="1"
        >
          <p class="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p class="ant-upload-text">
            点击或拖拽文件到此区域上传
          </p>
          <p class="ant-upload-hint">
            仅支持 .xlsx / .xls 格式的Excel文件
          </p>
        </a-upload-dragger>
      </a-modal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  DownloadOutlined,
  DownOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  FolderOutlined,
  FolderOpenOutlined,
  InboxOutlined,
  PaperClipOutlined,
  SettingOutlined,
  PrinterOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import PartnerTypeSidebar from './PartnerTypeSidebar.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { partnerApi, partnerCategoryApi } from '@/api/erp/partner'
import type { PartnerCategory } from '@/api/erp/partner'
import request from '@/utils/request'

// ── Props ─
const props = withDefaults(defineProps<{
  /** 往来单位类型 */
  partnerType: 'customer' | 'supplier' | 'logistics' | 'partner'
  /** API partnerType参数 */
  apiPartnerType: string
  /** 页面标题 */
  pageTitle: string
  /** 分类标题 */
  categoryTitle?: string
  /** 搜索框placeholder */
  searchPlaceholder?: string
  /** 表格列定义（不含操作列和序号列，由组件自动添加） */
  columns: DetailColumnConfig[]
  /** Tab配置 */
  tabs?: Array<{ key: string; label: string }>
  /** 是否显示状态筛选 */
  showStatusFilter?: boolean
  /** 新增表单路由 */
  formRoute?: string
  /** 编辑表单路由模板 (含:id占位符) */
  editFormRoute?: string
  /** 分页大小选项 */
  pageSizeOptions?: number[]
  /** 默认每页条数 */
  defaultPageSize?: number
}>(), {
  categoryTitle: '分类',
  searchPlaceholder: '名称/编码/联系人',
  showStatusFilter: true,
  tabs: () => [{ key: 'all', label: '全部' }],
  formRoute: '',
  editFormRoute: '',
  pageSizeOptions: () => [20, 50, 100],
  defaultPageSize: 20,
})

const router = useRouter()

// ── Tab ──
const activeTab = ref(props.tabs[0]?.key || '')

// ─ 搜索 ──
const searchForm = reactive({
  keyword: '',
  status: '' as string,
})

// ── 列可见性 ──
const visibleColumns = ref<Set<string>>(new Set(props.columns.map(c => c.key)))

function toggleColumn(key: string) {
  if (visibleColumns.value.has(key)) {
    visibleColumns.value.delete(key)
  } else {
    visibleColumns.value.add(key)
  }
  visibleColumns.value = new Set(visibleColumns.value) // trigger reactivity
}

// ── 分类管理 ──
const categoryLoading = ref(false)
const categoryError = ref(false)
const categoryTree = ref<any[]>([])
const selectedCategoryId = ref<string>('0')
const expandedKeys = ref<string[]>([])
const categoryCollapsed = ref(false)

const categoryTreeData = computed(() => categoryTree.value)

// 当前分类路径
const currentCategoryPath = computed(() => {
  if (selectedCategoryId.value === '0' || !categoryTree.value.length) return `全部${props.categoryTitle}`
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
  return path.length ? path.join(' / ') : `全部${props.categoryTitle}`
})

// 加载分类树
async function fetchCategoryTree() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const data = await partnerCategoryApi.getTree(props.apiPartnerType)
    categoryTree.value = Array.isArray(data) ? data : []
    const firstLevel = categoryTree.value.map(n => String(n.id))
    if (firstLevel.length > 0) {
      const existingSet = new Set(expandedKeys.value)
      const merged = new Set([...firstLevel, ...existingSet])
      expandedKeys.value = [...merged]
    }
  } catch (e) {
    console.error(`[${props.pageTitle}] 加载分类树失败`, e)
    categoryError.value = true
  } finally {
    categoryLoading.value = false
  }
}

function onCategorySelect(keys: (string | number)[]) {
  const key = keys[0]
  selectedCategoryId.value = key != null ? String(key) : '0'
  fetchList()
}

function onExpand(keys: (string | number)[]) {
  expandedKeys.value = keys.map(String)
}

// ── 分类弹窗 ─
const categoryModalVisible = ref(false)
const categoryModalLoading = ref(false)
const editingCategory = ref<PartnerCategory | null>(null)
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

function showCategoryModal(category: PartnerCategory | null) {
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
    const catPayload: Partial<PartnerCategory> = {
      categoryName: categoryForm.categoryName,
      categoryCode: categoryForm.categoryCode,
      parentId: categoryForm.parentId ? Number(categoryForm.parentId) : undefined,
      sortOrder: categoryForm.sortOrder,
      categoryType: props.apiPartnerType,
    }
    if (editingCategory.value) {
      await partnerCategoryApi.update(editingCategory.value.id, catPayload)
      message.success('更新成功')
    } else {
      await partnerCategoryApi.create(catPayload)
      message.success('新增成功')
    }
    categoryModalVisible.value = false
    await fetchCategoryTree()
    if (parentIdForExpand) {
      const keySet = new Set(expandedKeys.value)
      keySet.add(parentIdForExpand)
      expandedKeys.value = [...keySet]
    }
    await fetchList()
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

// ── 列表 ──
const loading = ref(false)
const tableData = ref<any[]>([])
const selectedRows = ref<any[]>([])
const tableRef = ref<any>(null)

const pagination = reactive({
  current: 1,
  pageSize: props.defaultPageSize,
  total: 0,
})

// 合并列配置：自动添加序号、复选框、操作列（按可见性过滤）
const mergedColumns = computed<DetailColumnConfig[]>(() => {
  const fixedLeft: DetailColumnConfig[] = [
    { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
    { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
    { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 140, fixed: 'left' },
  ]
  const visibleDataColumns = props.columns.filter(c => visibleColumns.value.has(c.key))
  return [...fixedLeft, ...visibleDataColumns]
})

// 加载列表
async function fetchList() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      partnerType: props.apiPartnerType,
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
    if (activeTab.value && activeTab.value !== 'all') {
      params.tab = activeTab.value
    }

    const res = await partnerApi.page(params)
    tableData.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (e) {
    console.error(`[${props.pageTitle}] 加载列表失败`, e)
    message.error('加载列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

function handleCheckboxChange() {
  selectedRows.value = tableRef.value?.getCheckedRecords?.() || []
}

function handleCheckboxAll(_checked: boolean, records: any[]) {
  selectedRows.value = records
}

// ── 操作 ─
function handleAdd() {
  if (props.formRoute) {
    router.push(props.formRoute)
  }
}

function getEditRoute(record: any): string {
  const route = props.editFormRoute || props.formRoute
  if (!route) return ''
  return route.replace(':id', String(record.id))
}

function handleEdit(record: any) {
  const route = getEditRoute(record)
  if (route) {
    if (route.includes('?')) {
      const [path, query] = route.split('?')
      router.push({ path, query: Object.fromEntries(new URLSearchParams(query)) })
    } else {
      router.push(route)
    }
  }
}

function handleView(record: any) {
  handleEdit(record)
}

function handleToggleStatus(record: any) {
  const newStatus = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const actionText = newStatus === 'ENABLED' ? '启用' : '停用'
  Modal.confirm({
    title: '确认',
    content: `确定要${actionText}该记录吗？`,
    onOk: async () => {
      try {
        await partnerApi.updateStatus(record.id, newStatus)
        message.success(`已${actionText}`)
        fetchList()
      } catch {
        message.error('操作失败')
      }
    },
  })
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除 "${record.partnerName}" 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await partnerApi.delete(record.id)
        message.success('删除成功')
        fetchList()
      } catch {
        message.error('删除失败')
      }
    },
  })
}

async function handleExport() {
  try {
    const blob = await request.get('/erp/md/customer/export', {
      responseType: 'blob',
      params: { partnerType: props.apiPartnerType },
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${props.pageTitle}_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch {
    message.warning('导出失败')
  }
}

function handleBatchStatus() {
  if (selectedRows.value.length === 0) {
    message.warning('请先选择记录')
    return
  }
  Modal.confirm({
    title: '批量改状态',
    content: `确定要将选中的 ${selectedRows.value.length} 条记录切换启用/停用状态吗？`,
    onOk: async () => {
      try {
        await Promise.all(selectedRows.value.map((row: any) => {
          const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
          return partnerApi.updateStatus(row.id, newStatus)
        }))
        message.success('批量改状态成功')
        fetchList()
      } catch {
        message.error('批量改状态失败')
      }
    },
  })
}

function handlePrint() {
  window.print()
}

function handleBatchDelete() {
  if (selectedRows.value.length === 0) {
    message.warning('请先选择记录')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${selectedRows.value.length} 条记录吗？`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await Promise.all(selectedRows.value.map((row: any) => partnerApi.delete(row.id)))
        message.success('批量删除成功')
        fetchList()
      } catch {
        message.error('批量删除失败')
      }
    },
  })
}

function refreshAll() {
  fetchCategoryTree()
  fetchList()
}

// ── 导入功能 ──
const importModalVisible = ref(false)
const importLoading = ref(false)
const importFileList = ref<any[]>([])

function showImportModal() {
  importFileList.value = []
  importModalVisible.value = true
}

async function handleImportUpload() {
  if (importFileList.value.length === 0) {
    message.warning('请先选择要上传的文件')
    return
  }
  const file = importFileList.value[0]
  const formData = new FormData()
  formData.append('file', file.originFileObj || file)

  const dataTypeMap: Record<string, string> = {
    customer: 'customer', supplier: 'customer', logistics: 'customer', partner: 'customer',
  }
  const dataType = dataTypeMap[props.partnerType] || 'customer'

  importLoading.value = true
  try {
    await request.post(`/import/v2/excel/${dataType}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    message.success('导入成功')
    importModalVisible.value = false
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '导入失败')
  } finally {
    importLoading.value = false
  }
}

// ── 键盘快捷键 ──
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

function onPartnerRefresh() {
  fetchList()
}

onMounted(() => {
  fetchCategoryTree()
  fetchList()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('partner-refresh', onPartnerRefresh)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('partner-refresh', onPartnerRefresh)
})
</script>

<style scoped>
/* ── 整体页面布局 ── */
.partner-page-layout {
  display: flex;
  height: 100%;
  overflow: hidden;
  background: #f5f7fa;
}

/* ── 右侧主内容区 ── */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  background: #fff;
}

/* ── Tab标签栏 ── */
.tab-bar {
  background: #4a4a4a;
  padding: 0;
  flex-shrink: 0;
  position: relative;
  z-index: 3;
}
.tab-items {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 4px;
  height: 38px;
  padding: 0 20px;
}
.tab-item {
  padding: 6px 22px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: color 0.2s;
  user-select: none;
  white-space: nowrap;
  position: relative;
  border-radius: 6px 6px 0 0;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-bottom: none;
}
.tab-item:hover:not(.active) {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}
.tab-item.active {
  color: #333;
  background: #fff;
  font-weight: 500;
  border-color: #fff;
  padding-bottom: 8px;
  margin-bottom: -1px;
  z-index: 2;
}

/* ── 工具栏 ── */
.toolbar-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  height: 40px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.toolbar-left,
.toolbar-right {
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

/* ─ 搜索栏 ── */
.search-section {
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}
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

/* ─ 复选框过滤行 ── */
.checkbox-row {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 8px;
  padding-left: 0;
}

/* ── 内容行：分类树 + 表格 ── */
.content-row {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: #fff;
}

/* ── 左侧分类面板 ── */
.category-panel {
  width: 220px;
  min-width: 180px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}
.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  background: #fafafa;
}
.category-header-actions {
  display: flex;
  align-items: center;
}
.category-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
.category-tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  min-height: 0;
}
.category-error {
  padding: 12px;
}
.category-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 12px;
  text-align: center;
}
.category-empty-icon {
  font-size: 36px;
  color: #d9d9d9;
  margin-bottom: 8px;
}
.category-empty-text {
  font-size: 13px;
  color: #999;
  margin: 0 0 8px 0;
}
.category-breadcrumb {
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  flex-shrink: 0;
  font-size: 12px;
  color: #888;
}
.breadcrumb-text { color: #888; }
.breadcrumb-path { color: #409eff; }

/* ── 分类折叠 ── */
.category-collapse-bar {
  width: 28px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 4px;
  flex-shrink: 0;
}
.collapse-toggle-btn {
  padding: 2px 4px;
  font-size: 14px;
  color: #999;
}
.collapse-toggle-btn:hover { color: #409eff; }

/* ── 右侧表格面板 ── */
.table-panel {
  flex: 1;
  min-width: 0;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.table-section {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ── 链接样式 ─ */
.cell-link {
  color: #1890ff;
  cursor: pointer;
}
.cell-link:hover {
  text-decoration: underline;
}

/* ── 表格样式覆盖 ── */
:deep(.ss-grid th) {
  background: #fafafa !important;
  font-weight: 600 !important;
  color: #333 !important;
}
:deep(.ss-row:hover td) {
  background: #e6f7ff !important;
}

/* ── 分类树选中高亮 ── */
:deep(.ant-tree-node-selected) {
  background: #fff3e0 !important;
}
:deep(.ant-tree-node-selected:hover) {
  background: #ffe0b2 !important;
}

/* ── 列设置菜单 ─ */
.column-setting-menu {
  padding: 8px 12px;
  min-width: 160px;
}
.column-setting-menu .ant-menu-item {
  padding: 4px 0;
  line-height: 24px;
}

/* ── 紧凑尺寸 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
