<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        其他收入（资料 → 财务账户 → 其他收入，对标 ql361）
        · 单入口平铺列表；无左侧分类树、无页面配置弹窗；列配置齿轮在数据表表头 rowNo 列（个人配置 / 全局配置）
        · 工具栏：新增收入 ｜ 刷新 / 打印(F8) / 导出
        · 查询区（固定项）：显示停用（checkbox）+ 查询（对标无文本查询框）
        · 列 3 个：科目编号 / 科目名称 / 核算项（「操作」为固定列，不可配置）
        · 行内操作：修改 / 删除 / 更多（启用·停用）
        · 编辑弹窗即「会计科目」完整编辑器（共享组件 SubjectEditorModal）

        ⚠️ 本页是「收入类会计科目」视图：finance_account_subject 中 subject_type=5（损益类）
          且 direction=2（贷方）的科目；与《其他收入单》（finance:other-income-doc）严格区分，
          严禁另建收入类型字典表。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增收入 ═══ -->
        <template #toolbar-left>
          <a-button
            type="primary"
            size="small"
            class="btn-add"
            @click="handleAdd"
          >
            <PlusOutlined /> 新增收入
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
              <a-checkbox
                v-model:checked="searchForm.includeDisabled"
                @change="handleSearch"
              >
                显示停用
              </a-checkbox>
              <a-button
                type="primary"
                size="small"
                @click="handleSearch"
              >
                查询
              </a-button>
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
              storage-key="md-other-income-table-columns"
              global-config-key="md-other-income-table-columns"
            >
              <!-- 科目名称：点击进入「会计科目」编辑器 -->
              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="handleEdit(record)"
                >{{ record.subjectName }}</a>
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

      <!-- ═══ 编辑器：会计科目（共享组件；新增预置 损益类 + 贷方；API 走本页收入口径接口） ═══ -->
      <SubjectEditorModal
        v-model:open="editorOpen"
        :record="editingRecord"
        :api="mdOtherIncomeApi"
        :default-subject-type="5"
        :default-direction="2"
        title="新增收入"
        @saved="fetchList"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import SubjectEditorModal from '@/components/business/SubjectEditorModal/index.vue'
import { mdOtherIncomeApi, type MdOtherIncomeInfo } from '@/api/md'

defineOptions({ name: 'MdOtherIncome' })

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<MdOtherIncomeInfo[]>([])

// ═══ 查询条件（对标固定项：仅「显示停用」） ═══
const searchForm = reactive({
  includeDisabled: false,
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

// ═══ 数据加载（收入类会计科目视图，后端按 损益类+贷方 过滤） ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await mdOtherIncomeApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      includeDisabled: searchForm.includeDisabled,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[其他收入] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
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
  fetchList()
}

// ═══ 编辑器（共享「会计科目」编辑器） ═══
const editorOpen = ref(false)
const editingRecord = ref<any>(null)

function handleAdd() {
  editingRecord.value = null
  editorOpen.value = true
}

function handleEdit(record: MdOtherIncomeInfo) {
  editingRecord.value = record
  editorOpen.value = true
}

// ═══ 删除 ═══
function handleDelete(record: MdOtherIncomeInfo) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除收入科目「${record.subjectCode} ${record.subjectName}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await mdOtherIncomeApi.remove(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 启用 / 停用 ═══
async function handleToggleEnabled(record: MdOtherIncomeInfo) {
  const next = record.isEnabled === false
  try {
    await mdOtherIncomeApi.toggleEnabled(record.id, next)
    message.success(next ? '已启用' : '已停用')
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '操作失败')
  }
}

// ═══ 打印(F8)：真实打印模板（与列表同口径） ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.subjectCode)}</td>
      <td>${escapeHtml(r.subjectName)}</td>
      <td>${escapeHtml(r.auxiliaryTypeName || '')}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>其他收入</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>其他收入</h2>
    <div class="meta">
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
    const blob: any = await mdOtherIncomeApi.export({
      includeDisabled: searchForm.includeDisabled,
    })
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob instanceof Blob ? blob : new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = `其他收入_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`
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
  console.error('[其他收入] 页面错误', error)
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
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }

/* 橙色新增按钮（资料模块统一） */
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
