<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        费用类型（资料 → 财务账户 → 费用类型，对标 ql361）
        · 单入口；无左侧分类树、无页面配置弹窗；列配置齿轮在数据表表头 rowNo 列（个人配置 / 全局配置）
        · 工具栏：新增费用 ｜ 刷新 / 打印(F8) / 导出
        · 查询区（固定项）：筛选条件（科目名称/编号）+ 查询 + ☐显示停用(默认不勾) + ☑显示层次结构(默认勾选)
        · 列 3 个：科目编号 / 科目名称 / 核算项（「操作」为固定列，不可配置）
        · 行内操作：修改 / 删除 / 更多（新增下级 · 启用/停用）
        · 编辑弹窗即「会计科目」完整编辑器（共享组件 SubjectEditorModal）

        ⚠️ 本页是「费用类会计科目」视图：finance_account_subject 中 subject_type=5（损益类）
          且 direction=1（借方）的科目；对标实测费用类型页与会计科目页调用同一接口
          （ql361 cc.erp.bll.bas.account.getlist，仅 bastype=fee/account 不同），
          行内「修改」打开的就是「会计科目」编辑器。与《费用单》（finance:expense-doc）严格区分，
          严禁另建费用类型字典表。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增费用 ═══ -->
        <template #toolbar-left>
          <a-button
            type="primary"
            size="small"
            class="btn-add"
            @click="handleAdd"
          >
            <PlusOutlined /> 新增费用
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：刷新 + 打印(F8) + 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              @click="handleRefresh"
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
          </a-space>
        </template>

        <!-- ═══ 查询区（固定项，对标无页面配置弹窗） ═══ -->
        <template #search-fields>
          <div class="search-area">
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
              <a-checkbox
                v-model:checked="searchForm.hierarchical"
                @change="handleSearch"
              >
                显示层次结构
              </a-checkbox>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="md-expense-type-table-columns"
              global-config-key="md-expense-type-table-columns"
            >
              <!-- 科目名称：按层级缩进 + 展开/收起 -->
              <template #nameCell="{ record }">
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
                  <span :class="{ 'subject-disabled': record.isEnabled === false }">{{ record.subjectName }}</span>
                </span>
              </template>

              <!-- 操作列（对标：行内 修改 / 删除 / 更多） -->
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
        </template>

        <!-- ═══ 底部：经典分页栏 ═══ -->
        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 编辑器：会计科目（共享组件；新增预置 损益类 + 借方） ═══ -->
      <SubjectEditorModal
        v-model:open="editorOpen"
        :record="editingRecord"
        :parent="parentRecord"
        :default-subject-type="5"
        :default-direction="1"
        :api="mdExpenseTypeApi"
        title="新增费用"
        @saved="fetchList"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import SubjectEditorModal from '@/components/business/SubjectEditorModal/index.vue'
import { mdExpenseTypeApi, type MdExpenseTypeInfo } from '@/api/md'

defineOptions({ name: 'MdExpenseType' })

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const treeData = ref<MdExpenseTypeInfo[]>([])
const tableData = ref<any[]>([])
const expandedKeys = ref<Set<string>>(new Set())

// ═══ 查询条件（对标固定项：筛选条件 + 显示停用 + 显示层次结构） ═══
const searchForm = reactive({
  keyword: '',
  includeDisabled: false,
  hierarchical: true,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// 序号列承载表头「列配置」齿轮；操作列为固定列（对标：操作 | 科目编号 | 科目名称 | 核算项）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 170, fixed: 'left' },
  { key: 'subjectCode', title: '科目编号', type: 'input', width: 180, sortable: true },
  { key: 'subjectName', title: '科目名称', type: 'slot', slotName: 'nameCell', width: 360, sortable: true },
  { key: 'auxiliaryTypeName', title: '核算项', type: 'input', width: 160 },
]

// ═══ 树形展开 / 分页（客户端，费用科目为个位数量级） ═══
function hasChildren(record: any): boolean {
  return Array.isArray(record?.children) && record.children.length > 0
}

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

// ═══ 数据加载（费用类会计科目视图，后端按 损益类+借方 过滤） ═══
async function fetchList() {
  loading.value = true
  try {
    const params: any = {
      includeDisabled: searchForm.includeDisabled,
      hierarchical: searchForm.hierarchical,
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword

    const res: any = await mdExpenseTypeApi.tree(params)
    const data = res?.data ?? res ?? []
    treeData.value = Array.isArray(data) ? data : []
    // 刷新后默认展开全部层级（对标「显示层次结构」勾选态）
    const ids = collectExpandableIds(treeData.value)
    expandedKeys.value = new Set(ids)
    syncPagedRows()
  } catch (error: any) {
    console.error('[费用类型] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    treeData.value = []
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleRefresh() {
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  syncPagedRows()
}

// ═══ 编辑器（共享「会计科目」编辑器，走费用类型菜单路径 API） ═══
const editorOpen = ref(false)
const editingRecord = ref<any>(null)
const parentRecord = ref<any>(null)

function handleAdd() {
  editingRecord.value = null
  parentRecord.value = null
  editorOpen.value = true
}

function handleAddChild(record: any) {
  editingRecord.value = null
  parentRecord.value = record
  editorOpen.value = true
  if (hasChildren(record)) {
    expandedKeys.value.add(String(record.id))
    expandedKeys.value = new Set(expandedKeys.value)
    syncPagedRows()
  }
}

function handleEdit(record: MdExpenseTypeInfo) {
  editingRecord.value = record
  parentRecord.value = null
  editorOpen.value = true
}

// ═══ 删除 ═══
function handleDelete(record: MdExpenseTypeInfo) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除费用科目「${record.subjectCode} ${record.subjectName}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await mdExpenseTypeApi.remove(record.id)
        message.success('删除成功')
        await fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 启用 / 停用 ═══
async function handleToggleEnabled(record: MdExpenseTypeInfo) {
  const next = record.isEnabled === false
  try {
    await mdExpenseTypeApi.toggleEnabled(record.id, next)
    message.success(next ? '已启用' : '已停用')
    await fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '操作失败')
  }
}

// ═══ 打印(F8)：真实打印模板（与列表同口径，含全部层级） ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  const rows = flatRows.value
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.subjectCode)}</td>
      <td>${'&nbsp;'.repeat((r._depth || 0) * 4)}${escapeHtml(r.subjectName)}</td>
      <td>${escapeHtml(r.auxiliaryTypeName || '')}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>费用类型</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>费用类型</h2>
    <div class="meta">
      <span>筛选条件：${escapeHtml(searchForm.keyword || '全部')}</span>
      <span>是否含停用：${searchForm.includeDisabled ? '是' : '否'}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>科目编号</th><th>科目名称</th><th>核算项</th></tr></thead>
      <tbody>${body}</tbody>
    </table></body></html>`
  const win = window.open('', '_blank', 'width=1000,height=700')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（真实 Excel） ═══
async function handleExport() {
  exporting.value = true
  try {
    const params: any = { includeDisabled: searchForm.includeDisabled, hierarchical: false }
    if (searchForm.keyword) params.keyword = searchForm.keyword

    const blob: any = await mdExpenseTypeApi.export(params)
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob instanceof Blob ? blob : new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = `费用类型_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

function handleError(error: Error) {
  console.error('[费用类型] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

/* 橙色新增按钮（资料模块统一） */
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

/* 树形缩进 */
.subject-name-cell { display: inline-flex; align-items: center; gap: 4px; }
.tree-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  flex: 0 0 14px;
  color: #595959;
  cursor: pointer;
}
.tree-toggle-placeholder { display: inline-block; width: 14px; flex: 0 0 14px; }
.subject-disabled { color: #bbb; text-decoration: line-through; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
