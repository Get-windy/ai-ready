<template>
  <ErrorBoundary>
    <div class="md-subject-page">
      <!-- 工具栏 -->
      <div class="toolbar-section">
        <div class="toolbar-left">
          <a-button
            type="primary"
            class="btn-add"
            @click="handleAdd"
          >
            <PlusOutlined /> 新增会计科目
          </a-button>
        </div>
        <div class="toolbar-right">
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
            :loading="exporting"
            @click="handleExport"
          >
            <DownloadOutlined /> 导出
          </a-button>
        </div>
      </div>

      <!-- 查询区 -->
      <div class="search-section">
        <div class="search-row">
          <div class="search-item">
            <span class="search-label">筛选条件</span>
            <a-input
              v-model:value="searchForm.keyword"
              placeholder="请输入科目名称/编号"
              size="small"
              style="width: 200px"
              allow-clear
              @press-enter="handleSearch"
            />
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
            v-model:checked="searchForm.includeDisabled"
            @change="handleSearch"
          >
            显示停用
          </a-checkbox>
          <a-checkbox v-model:checked="searchForm.hierarchical">
            显示层次结构
          </a-checkbox>
        </div>
      </div>

      <!-- 内容行：左侧科目分类 + 右侧数据表 -->
      <div class="content-row">
        <div
          v-if="!categoryCollapsed"
          class="category-panel"
        >
          <div class="category-header">
            <span class="category-title">科目分类</span>
            <a-button
              type="text"
              size="small"
              title="折叠"
              @click="categoryCollapsed = true"
            >
              <DoubleLeftOutlined />
            </a-button>
          </div>
          <div class="category-tree-container">
            <div
              :class="['category-node', { active: activeCategory === null }]"
              @click="handleCategorySelect(null)"
            >
              <FolderOpenOutlined class="cat-icon" />
              <span>全部</span>
            </div>
            <div
              v-for="cat in SUBJECT_CATEGORIES"
              :key="cat.value"
              :class="['category-node', { active: activeCategory === cat.value }]"
              @click="handleCategorySelect(cat.value)"
            >
              <FolderOutlined class="cat-icon" />
              <span>{{ cat.label }}</span>
            </div>
          </div>
          <div class="category-breadcrumb">
            <span class="breadcrumb-text">当前路径：</span>
            <span class="breadcrumb-path">{{ currentCategoryPath }}</span>
          </div>
        </div>

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
            <DoubleRightOutlined />
          </a-button>
        </div>

        <div class="table-panel">
          <div class="table-section">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="md-accounting-subject-table-columns"
              show-pagination
              :current="pagination.current"
              :page-size="pagination.pageSize"
              :total="pagination.total"
              :page-size-options="[20, 50, 100]"
              @page-change="handlePageChange"
            >
              <!-- 科目名称：按层级缩进 + 展开/收起 -->
              <template #subjectNameCell="{ record }">
                <span
                  v-if="record.__ghost"
                  class="subject-name-ghost"
                />
                <span
                  v-else
                  class="subject-name-cell"
                  :style="{ paddingLeft: `${(record._depth || 0) * 16}px` }"
                >
                  <a
                    v-if="hasChildren(record)"
                    class="tree-toggle"
                    :title="record._expanded ? '收起' : '展开'"
                    @click.stop="toggleExpand(record)"
                  >
                    <CaretDownOutlined v-if="record._expanded" />
                    <CaretRightOutlined v-else />
                  </a>
                  <span
                    v-else
                    class="tree-toggle-placeholder"
                  />
                  <span
                    :class="{ 'subject-disabled': record.isEnabled === false }"
                  >{{ record.subjectName }}</span>
                </span>
              </template>

              <!-- 操作列（对标：行内 修改/删除/更多） -->
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
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
                    :disabled="hasChildren(record)"
                    :title="hasChildren(record) ? '该科目下有子科目，无法删除' : ''"
                    @click="handleDelete(record)"
                  >
                    删除
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
                        <a-menu-item @click="handleAddChild(record)">
                          新增下级
                        </a-menu-item>
                        <a-menu-item @click="handleToggleEnabled(record)">
                          {{ record.isEnabled === false ? '启用' : '停用' }}
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </div>
      </div>

      <!-- 会计科目编辑器（共享组件，与其他收入页共用同一编辑器） -->
      <SubjectEditorModal
        v-model:open="editorOpen"
        :record="editingRecord"
        :parent="parentRecord"
        :default-subject-type="defaultSubjectType"
        :default-direction="1"
        @saved="fetchList"
      />
    </div>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="md-accounting-subject"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined, ReloadOutlined, DownloadOutlined, PrinterOutlined,
  FolderOutlined, FolderOpenOutlined, DoubleLeftOutlined, DoubleRightOutlined,
  CaretDownOutlined, CaretRightOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import SubjectEditorModal from '@/components/business/SubjectEditorModal/index.vue'
import { accountSubjectApi } from '@/api/finance'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'MdAccountingSubject' })

/** 科目分类（与后端 subject_type 1-5 一一对应，全系统单一口径） */
const SUBJECT_CATEGORIES = [
  { value: 1, label: '资产类' },
  { value: 2, label: '负债类' },
  { value: 3, label: '所有者权益类' },
  { value: 4, label: '成本类' },
  { value: 5, label: '损益类' },
]

const TYPE_LABEL: Record<number, string> = SUBJECT_CATEGORIES.reduce(
  (acc, c) => ({ ...acc, [c.value]: c.label }), {} as Record<number, string>,
)

// ── 查询 ──
const searchForm = reactive({
  keyword: '',
  includeDisabled: false,
  hierarchical: true,
})

// ── 科目分类面板 ──
const activeCategory = ref<number | null>(null)
const categoryCollapsed = ref(false)
const currentCategoryPath = computed(() =>
  activeCategory.value === null ? '全部' : (TYPE_LABEL[activeCategory.value] || '全部'),
)

function handleCategorySelect(value: number | null) {
  activeCategory.value = value
  pagination.current = 1
  fetchList()
}

// ── 列表 ──
const loading = ref(false)
const treeData = ref<any[]>([])
const tableData = ref<any[]>([])
const expandedKeys = ref<Set<string>>(new Set())
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// 序号列承载表头「列配置」齿轮，操作列为固定列（与对标一致：操作 | 科目编号 | 科目名称 | 核算项）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 170, fixed: 'left' },
  { key: 'subjectCode', title: '科目编号', type: 'input', width: 180, sortable: true },
  { key: 'subjectName', title: '科目名称', type: 'slot', slotName: 'subjectNameCell', width: 360, sortable: true },
  { key: 'auxiliaryTypeName', title: '核算项', type: 'input', width: 160 },
]

function hasChildren(record: any): boolean {
  return Array.isArray(record?.children) && record.children.length > 0
}

/** 按展开状态把科目树压平成表格行（含层级深度） */
function flattenTree(nodes: any[], depth = 0, out: any[] = []): any[] {
  for (const node of nodes) {
    const hasChild = hasChildren(node)
    const expanded = !searchForm.hierarchical || !hasChild || expandedKeys.value.has(String(node.id))
    out.push({ ...node, _depth: depth, _expanded: expanded })
    if (hasChild && expanded) {
      flattenTree(node.children, depth + 1, out)
    }
  }
  return out
}

const flatRows = computed<any[]>(() => {
  if (!searchForm.hierarchical) {
    return treeData.value.map((r: any) => ({ ...r, _depth: 0, _expanded: false }))
  }
  return flattenTree(treeData.value)
})

function syncPagedRows() {
  const all = flatRows.value
  pagination.total = all.length
  const maxPage = Math.max(1, Math.ceil(all.length / pagination.pageSize))
  if (pagination.current > maxPage) pagination.current = maxPage
  const start = (pagination.current - 1) * pagination.pageSize
  tableData.value = all.slice(start, start + pagination.pageSize)
}

function collectExpandableIds(nodes: any[], out: string[] = []): string[] {
  for (const n of nodes) {
    if (hasChildren(n)) {
      out.push(String(n.id))
      collectExpandableIds(n.children, out)
    }
  }
  return out
}

function toggleExpand(record: any) {
  const key = String(record.id)
  if (expandedKeys.value.has(key)) {
    expandedKeys.value.delete(key)
  } else {
    expandedKeys.value.add(key)
  }
  expandedKeys.value = new Set(expandedKeys.value)
  syncPagedRows()
}

async function fetchList() {
  loading.value = true
  try {
    const params: any = {
      includeDisabled: searchForm.includeDisabled,
      hierarchical: searchForm.hierarchical,
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (activeCategory.value !== null) params.subjectType = activeCategory.value

    const res: any = await accountSubjectApi.getTree(params)
    const data = res?.data ?? res ?? []
    treeData.value = Array.isArray(data) ? data : []
    // 首次/刷新后默认展开全部层级（对标「显示层次结构」勾选态）
    const ids = collectExpandableIds(treeData.value)
    if (ids.length) {
      expandedKeys.value = new Set(ids)
    } else {
      expandedKeys.value = new Set()
    }
    syncPagedRows()
  } catch (e) {
    console.error('[会计科目] 加载失败', e)
    message.error('加载会计科目失败')
    treeData.value = []
    tableData.value = []
    pagination.total = 0
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
  syncPagedRows()
}

function refreshAll() {
  fetchList()
}

// ── 编辑器（共享组件 SubjectEditorModal：与其他收入页共用同一「会计科目」编辑器） ──
const editorOpen = ref(false)
const editingRecord = ref<any>(null)
const parentRecord = ref<any>(null)
const defaultSubjectType = ref<number | undefined>(undefined)

function handleAdd() {
  if (activeCategory.value === null) {
    message.warning('请在科目分类下操作新增。')
    return
  }
  editingRecord.value = null
  parentRecord.value = null
  defaultSubjectType.value = activeCategory.value
  editorOpen.value = true
}

function handleAddChild(record: any) {
  editingRecord.value = null
  parentRecord.value = record
  defaultSubjectType.value = record.subjectType
  editorOpen.value = true
  if (hasChildren(record)) {
    expandedKeys.value.add(String(record.id))
    expandedKeys.value = new Set(expandedKeys.value)
    syncPagedRows()
  }
}

function handleEdit(record: any) {
  editingRecord.value = record
  parentRecord.value = null
  editorOpen.value = true
}

// ── 行操作 ──
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除科目「${record.subjectCode} ${record.subjectName}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await accountSubjectApi.delete(record.id)
        message.success('删除成功')
        await fetchList()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '删除失败')
      }
    },
  })
}

async function handleToggleEnabled(record: any) {
  const next = record.isEnabled === false
  try {
    await accountSubjectApi.toggleEnabled(record.id, next)
    message.success(next ? '已启用' : '已停用')
    await fetchList()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '操作失败')
  }
}

// ── 打印（结果集打印） ──
// 原先是自己拼 HTML + 新开窗口打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 列取自原打印表格的 <th>（原「#」行号列由模板/引擎处理，不再由页面拼）。
// 树形层级：原打印按 _depth 缩进，这里在名称前补全角空格（'\u3000' 是转义写法，别写成字面全角字符）。
const printColumns: any[] = [
  { title: '科目编号', key: 'subjectCode' },
  { title: '科目名称', key: 'subjectName', formatter: (v: any, r: any) => '\u3000'.repeat(Number(r._depth) || 0) + (v ?? '') },
  { title: '核算项', key: 'auxiliaryTypeName' },
]

const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'md-accounting-subject',
  title: '会计科目',
  rows: () => flatRows.value,
  columns: () => printColumns,
  // 原打印抬头的一行元信息（打印时间由模板 pageHeader 负责）
  totalText: () => `科目分类：${currentCategoryPath.value}，筛选条件：${searchForm.keyword || '全部'}，是否含停用：${searchForm.includeDisabled ? '是' : '否'}，记录数：${flatRows.value.length}`,
  emptyTip: '没有可打印的数据',
})

const exporting = ref(false)

async function handleExport() {
  exporting.value = true
  try {
    const params: any = { includeDisabled: searchForm.includeDisabled, hierarchical: false }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (activeCategory.value !== null) params.subjectType = activeCategory.value

    const blob: any = await accountSubjectApi.export(params)
    const url = window.URL.createObjectURL(blob instanceof Blob ? blob : new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = `会计科目_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e: any) {
    message.error(e?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ── 快捷键 ──
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey) {
    e.preventDefault()
    refreshAll()
  }
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

onMounted(() => {
  fetchList()
  document.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.md-subject-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: #fff;
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

/* ── 橙色新增按钮（资料模块统一） ── */
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

/* ── 查询区 ── */
.search-section {
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.search-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
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

/* ── 内容行 ── */
.content-row {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: #fff;
}

/* ── 左侧科目分类面板 ── */
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
  padding: 6px 8px 6px 12px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  background: #fafafa;
}
.category-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
.category-tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
  min-height: 0;
}
.category-node {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: 13px;
  color: #303133;
  cursor: pointer;
  user-select: none;
}
.category-node:hover {
  background: #f5f7fa;
}
.category-node.active {
  background: #fff3e0;
  color: #d46b08;
}
.cat-icon {
  color: #faad14;
}
.category-breadcrumb {
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  flex-shrink: 0;
  font-size: 12px;
  color: #888;
}
.breadcrumb-path {
  color: #409eff;
}

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
.collapse-toggle-btn:hover {
  color: #409eff;
}

/* ── 右侧表格 ── */
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

/* ── 树形缩进 ── */
.subject-name-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.tree-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  flex: 0 0 14px;
  color: #595959;
  cursor: pointer;
}
.tree-toggle-placeholder {
  display: inline-block;
  width: 14px;
  flex: 0 0 14px;
}
.subject-disabled {
  color: #bbb;
  text-decoration: line-through;
}

:deep(.ss-grid th) {
  background: #fafafa !important;
  font-weight: 600 !important;
  color: #333 !important;
}
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
