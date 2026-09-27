<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        关键词库（交易 → 商城设置 → 关键词库，对标 ql361 商城 → 商城设置 → 关键词库）
        · 单入口单视图列表页 + 新增/编辑弹窗；对标无「页面配置」弹窗（查询区/按钮为固定项）
        · 列配置齿轮在数据表表头 rowNo 列（个人配置 / 全局配置），storage-key 持久化
        · 对标列（实测 2 列，全默认显示）：关键词名称 / 备注；本系统保留 类型 / 排序 / 状态 以便启停筛选
        · 工具栏：新增关键词 ｜ 刷新 / 打印(F8)
        · 行内操作：修改 / 删除
        · 对标实测：docs/Yh-Spec/手动整理对标开发文档/交易模块/关键词库开发文档.md
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增关键词 ═══ -->
        <template #toolbar-left>
          <a-button
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新增关键词
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：刷新 + 打印(F8) ═══ -->
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
          </a-space>
        </template>

        <!-- ═══ 查询区（对标固定项：筛选条件 + 查询；本系统另含 类型/状态） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <span class="search-label">筛选条件</span>
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="请输入关键词"
                size="small"
                style="width: 200px"
                allow-clear
                @press-enter="handleSearch"
              />
              <a-select
                v-model:value="searchForm.keywordType"
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
              storage-key="mall-keyword-bank-table-columns"
              global-config-key="mall-keyword-bank-table-columns"
            >
              <!-- 关键词名称：点击进入编辑 -->
              <template #keywordCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.keyword }}</a>
              </template>

              <!-- 类型 -->
              <template #typeCell="{ record }">
                <a-tag :color="TYPE_MAP[record.keywordType]?.color || 'default'">
                  {{ TYPE_MAP[record.keywordType]?.label || '-' }}
                </a-tag>
              </template>

              <!-- 状态：启用 / 禁用 -->
              <template #statusCell="{ record }">
                <a-switch
                  :checked="record.status === 1"
                  :loading="togglingId === record.id"
                  checked-children="启用"
                  un-checked-children="禁用"
                  @change="(checked: any) => handleToggle(record, checked)"
                />
              </template>

              <!-- 创建时间 -->
              <template #createTimeCell="{ record }">
                {{ fmtTime(record.createTime) }}
              </template>

              <!-- 操作列（对标：行内 修改 / 删除） -->
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

      <!-- ═══ 新增/编辑关键词弹窗（对标字段：关键词* / 备注） ═══ -->
      <a-modal
        v-model:open="modalOpen"
        :title="editingId ? '编辑关键词' : '新增关键词'"
        :confirm-loading="saving"
        :width="480"
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
            label="关键词"
            name="keyword"
          >
            <a-input
              v-model:value="form.keyword"
              :maxlength="30"
              show-count
              placeholder="请输入关键词，不超过30字符"
            />
          </a-form-item>
          <a-form-item
            label="备注"
            name="remark"
          >
            <a-input
              v-model:value="form.remark"
              :maxlength="30"
              show-count
              placeholder="请输入备注，限30字"
            />
          </a-form-item>
          <a-form-item
            label="类型"
            name="keywordType"
          >
            <a-select
              v-model:value="form.keywordType"
              :options="typeOptions"
              placeholder="请选择关键词类型"
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
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="mall-keyword-bank"
      :print-data="printData"
    />
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
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { mallKeywordApi } from '@/api/erp/mall'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'MallKeywordBank' })

// ═══ 关键词类型（与后端 MallKeyword.keywordType 一致） ═══
const TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '热门', color: 'orange' },
  2: { label: '置顶', color: 'blue' },
  3: { label: '屏蔽', color: 'default' }
}
const typeOptions = Object.entries(TYPE_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])

// ═══ 查询条件 ═══
const searchForm = reactive({
  keyword: '' as string,
  keywordType: undefined as number | undefined,
  status: undefined as number | undefined,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// 序号列承载表头「列配置」齿轮；操作列为固定列
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 130, fixed: 'left' },
  { key: 'keyword', title: '关键词名称', type: 'slot', slotName: 'keywordCell', width: 220, sortable: true },
  { key: 'remark', title: '备注', type: 'input', width: 220 },
  { key: 'keywordType', title: '类型', type: 'slot', slotName: 'typeCell', width: 100 },
  { key: 'sort', title: '排序', type: 'input', width: 80 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 110 },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'createTimeCell', width: 160, defaultHidden: true },
]

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await mallKeywordApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      keywordType: searchForm.keywordType,
      status: searchForm.status,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[关键词库] 加载列表失败', error)
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
  keyword: '' as string,
  remark: '' as string,
  keywordType: 1 as number,
  sort: 0 as number
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  keyword: [{ required: true, message: '请输入关键词', trigger: 'blur' }],
  keywordType: [{ required: true, message: '请选择关键词类型', trigger: 'change' }]
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
    keyword: record.keyword || '',
    remark: record.remark || '',
    keywordType: record.keywordType ?? 1,
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
      await mallKeywordApi.update(editingId.value, { ...form })
      message.success('关键词更新成功')
    } else {
      await mallKeywordApi.create({ ...form })
      message.success('关键词创建成功')
    }
    modalOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 启停切换 / 删除 ═══
const togglingId = ref<number | null>(null)

async function handleToggle(record: any, checked: boolean | string | number) {
  const target = checked ? 1 : 0
  togglingId.value = record.id
  try {
    await mallKeywordApi.toggleStatus(record.id, target)
    message.success(target === 1 ? '已启用' : '已禁用')
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '状态切换失败')
  } finally {
    togglingId.value = null
  }
}

async function handleDelete(record: any) {
  try {
    await mallKeywordApi.delete(record.id)
    message.success('删除成功')
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '删除失败')
  }
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + 新开窗口打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 列取自原打印表格的 <th>（原「#」行号列由模板/引擎处理，不再由页面拼）。
const printColumns: any[] = [
  { title: '关键词名称', key: 'keyword' },
  { title: '备注', key: 'remark', formatter: (v: any) => v || '' },
  { title: '类型', key: 'keywordType', formatter: (v: any) => TYPE_MAP[v]?.label || '' },
  { title: '排序', key: 'sort' },
  { title: '状态', key: 'status', formatter: (v: any) => (v === 1 ? '启用' : '禁用') },
]

const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'mall-keyword-bank',
  title: '关键词库',
  rows: () => tableData.value.filter((r: any) => !r.__ghost),
  columns: () => printColumns,
  // 原打印抬头的筛选/记录数元信息行（打印时间由模板 pageHeader 负责）
  totalText: () => `筛选条件：${searchForm.keyword || '全部'}，记录数：${tableData.value.filter((r: any) => !r.__ghost).length}`,
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[关键词库] 页面错误', error)
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
