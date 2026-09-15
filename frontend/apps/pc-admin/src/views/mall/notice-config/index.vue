<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        公告设置（交易 → 商城 → 商城设置 → 公告设置，对标 ql361「商城 → 商城设置 → 公告设置」）
        · 对标文档：docs/Yh-Spec/手动整理对标开发文档/交易模块/公告设置开发文档.md
        · 页面定位：单入口单视图列表页（无子 Tab）+ 新增/编辑公告弹窗；对标无「页面配置」弹窗（查询区/按钮为固定项）
        · 列配置：走 BillDetailTable 表头 rowNo 列齿轮（个人配置 / 全局配置），storage-key 持久化
        · 列数：可配置 8 列 = 对标 3 列（公告名称 / 发布人 / 发布时间，全默认显示）
                              + 本系统扩展 5 列（类型 / 状态 默认显示；内容 / 排序 / 创建时间 默认隐藏）
          默认显示 5 列、默认隐藏 3 列；固定列（非列配置）：序号、操作
        · 工具栏：新增公告 ｜ 刷新 / 打印(F8) / 导出
        · 行级操作：编辑 / 发布（状态≠已发布）/ 下线（状态=已发布）/ 删除
        · 数据来源：mall_notice（API /erp/mall/admin/notice：page/create/update/delete/publish/offline）
        · 后端字段：mall_notice 已补 publisher（Flyway V11.361.3，MallNotice.publisher），「发布人」列有数据源；
          「发布人」查询参数仍未提供（仅列表展示）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增公告 ═══ -->
        <template #toolbar-left>
          <a-button
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新增公告
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：刷新 + 打印(F8) + 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              :loading="loading"
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
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标固定项：筛选条件=公告名称；本系统另含 类型/状态） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <span class="search-label">筛选条件</span>
              <a-input
                v-model:value="searchForm.title"
                placeholder="请输入公告名称"
                size="small"
                style="width: 200px"
                allow-clear
                @press-enter="handleSearch"
              />
              <a-select
                v-model:value="searchForm.noticeType"
                placeholder="全部类型"
                size="small"
                style="width: 130px"
                allow-clear
                :options="typeOptions"
                @change="handleSearch"
              />
              <a-select
                v-model:value="searchForm.status"
                placeholder="全部状态"
                size="small"
                style="width: 130px"
                allow-clear
                :options="statusOptions"
                @change="handleSearch"
              />
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
              storage-key="mall-notice-config-table-columns"
              global-config-key="mall-notice-config-table-columns"
            >
              <!-- 公告名称：点击进入编辑 -->
              <template #titleCell="{ record, column }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record[column.key] || '-' }}</a>
              </template>

              <!-- 发布人（mall_notice.publisher，已补字段） -->
              <template #publisherCell="{ record }">
                <span v-if="!record.__ghost">{{ record.publisherName || record.publisher || '-' }}</span>
              </template>

              <!-- 发布时间 -->
              <template #publishTimeCell="{ record }">
                {{ fmtTime(record.publishTime) }}
              </template>

              <!-- 类型 -->
              <template #noticeTypeCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="TYPE_MAP[record.noticeType]?.color || 'default'"
                >
                  {{ TYPE_MAP[record.noticeType]?.label || '-' }}
                </a-tag>
              </template>

              <!-- 状态：草稿 / 已发布 / 已下线 -->
              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="STATUS_MAP[record.status]?.color || 'default'"
                >
                  {{ STATUS_MAP[record.status]?.label || '-' }}
                </a-tag>
              </template>

              <!-- 创建时间 -->
              <template #createTimeCell="{ record }">
                {{ fmtTime(record.createTime) }}
              </template>

              <!-- 操作列（对标：编辑 / 发布 / 下线 / 删除） -->
              <template #actionCell="{ record, column }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                  :data-col="column.key"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="openEdit(record)"
                  >
                    编辑
                  </a-button>
                  <a-popconfirm
                    v-if="record.status !== 1"
                    title="确认发布该公告？发布后商城端立即可见"
                    ok-text="发布"
                    cancel-text="取消"
                    @confirm="handlePublish(record)"
                  >
                    <a-button
                      type="link"
                      size="small"
                    >
                      发布
                    </a-button>
                  </a-popconfirm>
                  <a-popconfirm
                    v-if="record.status === 1"
                    title="确认下线该公告？下线后商城端不再展示"
                    ok-text="下线"
                    cancel-text="取消"
                    @confirm="handleOffline(record)"
                  >
                    <a-button
                      type="link"
                      size="small"
                    >
                      下线
                    </a-button>
                  </a-popconfirm>
                  <a-popconfirm
                    title="确认删除该公告？"
                    ok-text="删除"
                    cancel-text="取消"
                    @confirm="handleDelete(record)"
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

      <!-- ═══ 新增/编辑公告弹窗（对标字段：公告标题 + 公告内容[textarea]） ═══ -->
      <a-modal
        v-model:open="modalOpen"
        :title="editingId ? '编辑公告' : '新增公告'"
        :confirm-loading="saving"
        :width="600"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="form"
          :rules="rules"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 17 }"
          style="margin-top: 16px"
        >
          <a-form-item
            label="公告标题"
            name="title"
          >
            <a-input
              v-model:value="form.title"
              :maxlength="100"
              placeholder="展示在商城首页的公告标题"
            />
          </a-form-item>
          <a-form-item
            label="公告类型"
            name="noticeType"
          >
            <a-select
              v-model:value="form.noticeType"
              :options="typeOptions"
              placeholder="请选择公告类型"
            />
          </a-form-item>
          <a-form-item
            label="公告内容"
            name="content"
          >
            <a-textarea
              v-model:value="form.content"
              :rows="5"
              :maxlength="500"
              show-count
              placeholder="公告正文内容"
            />
          </a-form-item>
          <a-form-item
            label="排序"
            name="sort"
          >
            <a-input-number
              v-model:value="form.sort"
              :min="0"
              :precision="0"
              style="width: 100%"
              placeholder="数值越小越靠前"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
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
import { mallNoticeApi } from '@/api/erp/mall'

defineOptions({ name: 'MallNoticeConfig' })

// ═══ 公告类型（与后端 MallNotice.noticeType 一致：1公告 2活动 3系统） ═══
const TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '公告', color: 'blue' },
  2: { label: '活动', color: 'orange' },
  3: { label: '系统', color: 'purple' }
}

// ═══ 公告状态（与后端 MallNotice.status 一致：0草稿 1已发布 2已下线） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '已发布', color: 'green' },
  2: { label: '已下线', color: 'orange' }
}

const typeOptions = Object.entries(TYPE_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
const statusOptions = Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])

// ═══ 查询条件（对标固定项：筛选条件=公告名称；类型/状态为本系统扩展） ═══
const searchForm = reactive({
  title: '' as string,
  noticeType: undefined as number | undefined,
  status: undefined as number | undefined,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

/**
 * 列定义（DetailColumnConfig[]）
 * 对标 3 列（默认显示）：公告名称 / 发布人 / 发布时间
 * 本系统扩展：类型、状态（默认显示，便于筛选与启停判断）；内容、排序、创建时间（默认隐藏）
 * 固定列（不进列配置面板）：rowNo（承载列配置齿轮）、action
 */
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 200, fixed: 'left' },
  { key: 'title', title: '公告名称', type: 'slot', slotName: 'titleCell', width: 240, sortable: true },
  { key: 'publisher', title: '发布人', type: 'slot', slotName: 'publisherCell', width: 120 },
  { key: 'publishTime', title: '发布时间', type: 'slot', slotName: 'publishTimeCell', width: 160 },
  { key: 'noticeType', title: '类型', type: 'slot', slotName: 'noticeTypeCell', width: 100 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'content', title: '内容', type: 'input', width: 240, defaultHidden: true },
  { key: 'sort', title: '排序', type: 'input', width: 80, align: 'right', defaultHidden: true },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'createTimeCell', width: 160, defaultHidden: true },
]

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await mallNoticeApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      title: searchForm.title || undefined,
      noticeType: searchForm.noticeType,
      status: searchForm.status,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[公告设置] 加载列表失败', error)
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

// ═══ 新增/编辑弹窗 ═══
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  title: '' as string,
  noticeType: 1 as number,
  content: '' as string,
  sort: 0 as number
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  noticeType: [{ required: true, message: '请选择公告类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

function resetForm(data?: Partial<typeof form>) {
  Object.assign(form, emptyForm(), data || {})
}

function openCreate() {
  editingId.value = null
  resetForm()
  modalOpen.value = true
}

function openEdit(record: any) {
  editingId.value = record.id
  resetForm({
    title: record.title || '',
    noticeType: record.noticeType ?? 1,
    content: record.content || '',
    sort: record.sort ?? 0
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
      await mallNoticeApi.update(editingId.value, { ...form })
      message.success('公告更新成功')
    } else {
      await mallNoticeApi.create({ ...form })
      message.success('公告创建成功（草稿状态，发布后商城可见）')
    }
    modalOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 发布 / 下线 / 删除 ═══
async function handlePublish(record: any) {
  try {
    await mallNoticeApi.publish(record.id)
    message.success('公告已发布')
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '发布失败')
  }
}

async function handleOffline(record: any) {
  try {
    await mallNoticeApi.offline(record.id)
    message.success('公告已下线')
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '下线失败')
  }
}

async function handleDelete(record: any) {
  try {
    await mallNoticeApi.delete(record.id)
    message.success('删除成功')
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '删除失败')
  }
}

// ═══ 打印(F8)：与列表同口径渲染后打印 ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function currentRows(): any[] {
  return (tableData.value || []).filter((r: any) => !r.__ghost)
}

function handlePrint() {
  const rows = currentRows()
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.title)}</td>
      <td>${escapeHtml(r.publisherName || r.publisher || '')}</td>
      <td>${escapeHtml(fmtTime(r.publishTime))}</td>
      <td>${escapeHtml(TYPE_MAP[r.noticeType]?.label || '')}</td>
      <td>${escapeHtml(STATUS_MAP[r.status]?.label || '')}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>公告设置</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>公告设置</h2>
    <div class="meta">
      <span>筛选条件：${escapeHtml(searchForm.title || '全部')}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>公告名称</th><th>发布人</th><th>发布时间</th><th>类型</th><th>状态</th></tr></thead>
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

// ═══ 导出（前端 CSV，\uFEFF BOM 保证 Excel 中文不乱码） ═══
function handleExport() {
  const rows = currentRows()
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const header = ['公告名称', '发布人', '发布时间', '类型', '状态', '排序', '创建时间']
  const lines = rows.map((r: any) => [
    r.title ?? '',
    r.publisherName || r.publisher || '',
    fmtTime(r.publishTime),
    TYPE_MAP[r.noticeType]?.label || '',
    STATUS_MAP[r.status]?.label || '',
    r.sort ?? '',
    fmtTime(r.createTime),
  ])
  const csv = [header, ...lines]
    .map(cols => cols.map(c => `"${String(c ?? '').replace(/"/g, '""')}"`).join(','))
    .join('\r\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `公告设置_${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[公告设置] 页面错误', error)
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
.search-label { font-size: 13px; color: #666; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }

/* 橙色新增按钮（交易模块统一） */
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
