<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        热门搜索词推荐（营销 → 商城营销 → 热门搜索词推荐，菜单 80325）
        对标 ql361「营销 → 商城营销 → 热门搜索词推荐」：单视图列表页，3 列（全部默认可见）
        列：关键词名称 / 排序 / 最后修改时间
        对标实测工具栏：新增关键词 ｜ 提示「最多新增20个热门关键词」｜ 筛选条件（请输入关键词）+ 查询
        对标文档：docs/Yh-Spec/手动整理对标开发文档/营销模块/热门搜索词推荐开发文档.md
        后端：复用既有 /erp/mall/admin/keyword（mall_keyword 表）
        对标无「页面配置」弹窗
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <template #toolbar-left>
          <a-button
            type="primary"
            size="small"
            class="btn-add"
            :disabled="total >= MAX_KEYWORDS"
            @click="openCreate"
          >
            <PlusOutlined /> 新增关键词
          </a-button>
          <span class="limit-tip">最多新增{{ MAX_KEYWORDS }}个热门关键词</span>
        </template>

        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              :loading="loading"
              @click="fetchList"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
          </a-space>
        </template>

        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <span class="search-label">筛选条件</span>
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="请输入关键词"
                size="small"
                style="width: 220px"
                allow-clear
                @press-enter="handleSearch"
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

        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="marketing-hot-keywords-table-columns"
              global-config-key="marketing-hot-keywords-table-columns"
            >
              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.keyword }}</a>
              </template>

              <template #updateTimeCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtTime(record.updateTime || record.createTime) }}</span>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="openEdit(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

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

      <!-- ═══ 新增/修改关键词 ═══ -->
      <a-modal
        v-model:open="formOpen"
        :title="editingId ? '修改关键词' : '新增关键词'"
        :confirm-loading="saving"
        width="480px"
        @ok="handleSave"
      >
        <a-form
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 17 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-form-item
            label="关键词名称"
            required
          >
            <a-input
              v-model:value="form.keyword"
              :maxlength="30"
              show-count
              placeholder="请输入关键词，不超过30字符"
            />
          </a-form-item>
          <a-form-item label="排序">
            <a-input-number
              v-model:value="form.sort"
              :min="0"
              :precision="0"
              style="width: 160px"
              placeholder="数值越小越靠前"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-input
              v-model:value="form.remark"
              :maxlength="60"
              placeholder="请输入备注"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined, ReloadOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { mallKeywordApi } from '@/api/erp/mall'

defineOptions({ name: 'MarketingHotKeywords' })

/** 对标实测提示：最多新增 20 个热门关键词 */
const MAX_KEYWORDS = 20

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const searchForm = reactive({ keyword: '' })
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// 对标 3 列（全部默认可见）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 130, fixed: 'left' },
  { key: 'keyword', title: '关键词名称', type: 'slot', slotName: 'nameCell', width: 320 },
  { key: 'sort', title: '排序', type: 'input', width: 120 },
  { key: 'updateTime', title: '最后修改时间', type: 'slot', slotName: 'updateTimeCell', width: 180 },
]

function fmtTime(v: any): string {
  return v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await mallKeywordApi.page({
      keyword: searchForm.keyword || undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    total.value = pagination.total
  } catch (error: any) {
    console.error('[热门搜索词推荐] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    tableData.value = []
    pagination.total = 0
    total.value = 0
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

const formOpen = ref(false)
const saving = ref(false)
const editingId = ref<any>(null)
const emptyForm = () => ({ keyword: '', sort: 0 as number | undefined, remark: '' })
const form = reactive(emptyForm())

function openCreate() {
  if (total.value >= MAX_KEYWORDS) {
    message.warning(`最多新增 ${MAX_KEYWORDS} 个热门关键词`)
    return
  }
  editingId.value = null
  Object.assign(form, emptyForm())
  formOpen.value = true
}
function openEdit(record: any) {
  editingId.value = record.id
  Object.assign(form, emptyForm(), {
    keyword: record.keyword || '',
    sort: record.sort ?? 0,
    remark: record.remark || '',
  })
  formOpen.value = true
}

async function handleSave() {
  if (!form.keyword?.trim()) {
    message.warning('请输入关键词')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await mallKeywordApi.update(editingId.value, { ...form })
      message.success('关键词已更新')
    } else {
      await mallKeywordApi.create({ ...form, keywordType: 1, status: 1 })
      message.success('关键词已新增')
    }
    formOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除热门关键词「${record.keyword}」吗？`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await mallKeywordApi.delete(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}
const PRINT_COLUMNS = [
  { key: 'keyword', title: '关键词名称' },
  { key: 'sort', title: '排序' },
  { key: 'updateTimeText', title: '最后修改时间' },
]

function handlePrint() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `<tr><td>${i + 1}</td>${
    PRINT_COLUMNS.map(c => `<td>${escapeHtml(c.key === 'updateTimeText'
      ? fmtTime(r.updateTime || r.createTime) : r[c.key])}</td>`).join('')}</tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" /><title>热门搜索词推荐</title>
    <style>body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px}
    h2{text-align:center;margin:0 0 12px;font-size:18px}
    table{width:100%;border-collapse:collapse;font-size:12px}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left}th{background:#f2f2f2}</style></head><body>
    <h2>热门搜索词推荐</h2>
    <table><thead><tr><th>#</th>${PRINT_COLUMNS.map(c => `<th>${c.title}</th>`).join('')}</tr></thead>
    <tbody>${body}</tbody></table></body></html>`
  const win = window.open('', '_blank', 'width=1200,height=800')
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

function handleError(error: Error) {
  console.error('[热门搜索词推荐] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 0; }
.search-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.limit-tip { font-size: 12px; color: #999; margin-left: 8px; }
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
