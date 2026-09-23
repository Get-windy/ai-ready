<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        购物车（交易 → 商城订单组 → 购物车，路由 trade/cart/list）
        · 定位：商城前台顾客加购集合的**后台查看/清理页**（只读列表 + 行级删除/批量删除）
        · 对标：ql361 无独立购物车页（购物车开发文档「对标来源」），本页按本系统实现 + 业界标准
        · 骨架（路线 A）：CategoryListLayout + BillDetailTable（表头序号齿轮列配置：个人/全局）+ PageConfigPanel
        · 后端（`MallCartController @RequestMapping("/api/v1/mall/cart")`；request baseURL 为 `/api`，故前端路径 `/v1/mall/cart`）：
          - `GET /page`      分页 + 会员关键字 memberKeyword + 商品关键字 productKeyword（真实分页 SQL，返回 {records,total}）
          - `DELETE /batch`  body 裸数组 `[1,2,3]`
          - `DELETE /{id}`   单条删除；`GET /` 裸数组（未分页）
          - ⚠️ `DELETE /`（清空）**不可用于管理端**：它按调用者本人清空（见模板内注释与审计报告 P1-8）
        · 分页/查询口径：**服务端分页**（mallCartApi.page），StandardPagination 的 total 用后端 total
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：批量删除 ═══ -->
        <!--
          ⚠️ 2026-09-23 移除「清空购物车」按钮：它调的是 `DELETE /v1/mall/cart`，
          该端点在后端按**调用者本人**（`StpUtil.getLoginIdAsLong()` 当 customerId）清空，
          而本页是管理端、列表展示的是**全体会员**的购物车 ⇒ 点了对列表没有任何影响，
          属"看似能用实则无效"的假按钮（详见 TRADE_MODULE_AUDIT_20260923.md P1-8）。
          要清空某位会员的购物车，用「勾选该会员的行 → 批量删除」即可（本页已具备）。
          若将来要做「按会员一键清空」，需后端新增按 customerId 的管理端端点，不能复用 C 端的 DELETE /。
        -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('batchDelete')"
              size="small"
              danger
              :disabled="selectedRowKeys.length === 0"
              @click="handleBatchDelete"
            >
              <DeleteOutlined /> 批量删除{{ selectedRowKeys.length ? ` (${selectedRowKeys.length})` : '' }}
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 / 刷新 / 打印(F8) / 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="页面配置" placement="bottom" :mouse-enter-delay="0.4">
              <a-button size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button v-if="isButtonEnabled('refresh')" size="small" :loading="loading" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="isButtonEnabled('printF8')" size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button v-if="isButtonEnabled('export')" size="small" @click="handleExport">
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（服务端查询：商品关键字 / 会员关键字） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div v-if="isQueryVisible('productName')" class="search-item">
                <span class="search-label">商品名称</span>
                <a-input
                  v-model:value="searchForm.productKeyword"
                  placeholder="商品名称/编码"
                  size="small"
                  style="width: 170px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('memberName')" class="search-item">
                <span class="search-label">会员</span>
                <a-input
                  v-model:value="searchForm.memberKeyword"
                  placeholder="会员名称/账号/手机号"
                  size="small"
                  style="width: 170px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
              <a-button size="small" @click="handleReset">重置</a-button>
              <span class="toolbar-tip">
                共 {{ pagination.total }} 条 / 本页商品总数 {{ totalItems }}
              </span>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              ref="tableRef"
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="trade-cart-table-columns"
              global-config-key="trade-cart-table-columns"
              @checkbox-change="handleCheckboxChange"
              @checkbox-all="handleCheckboxAll"
            >
              <!-- 商品名称 -->
              <template #productNameCell="{ record }">
                <span class="cell-strong">{{ record.productName || '-' }}</span>
              </template>

              <!-- 会员（字段以后端 CartDTO 为准，缺列时显示 -） -->
              <template #memberCell="{ record }">
                {{ record.memberName || record.memberAccount || '-' }}
              </template>

              <!-- 小计 -->
              <template #totalPriceCell="{ record }">
                <span class="currency-value">¥{{ formatAmount(record.totalPrice) }}</span>
              </template>

              <!-- 加入时间 -->
              <template #createTimeCell="{ record }">
                {{ fmtTime(record.createTime) }}
              </template>

              <!-- 操作列（行级：删除，popconfirm 确认） -->
              <template #actionCell="{ record }">
                <a-popconfirm
                  v-if="!record.__ghost"
                  title="确定删除该商品？"
                  @confirm="handleDelete(record)"
                >
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏（前端内存分页） ═══ -->
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

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="trade-cart-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined, PrinterOutlined, DownloadOutlined, SettingOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { mallCartApi } from '@/api/erp/mall'

defineOptions({ name: 'TradeCartList' })

// ═══ 状态 ═══
const tableRef = ref()
const loading = ref(false)
const tableData = ref<any[]>([])
/** 当前页勾选的行（BillDetailTable 按 rowIndex 持有勾选，换页/换查询会清空） */
const selectedRecords = ref<any[]>([])
const selectedRowKeys = computed<number[]>(() =>
  selectedRecords.value.filter((r: any) => !r.__ghost).map((r: any) => r.id).filter((id: any) => id != null)
)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

/** 服务端查询参数（与后端 GET /v1/mall/cart/page 对齐） */
const searchForm = reactive<{
  productKeyword?: string
  memberKeyword?: string
}>({})

// 序号列承载表头「列配置」齿轮；勾选列/操作列为锁定列
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 90, fixed: 'left' },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productNameCell', width: 200 },
  { key: 'productCode', title: '商品编码', type: 'input', width: 130 },
  { key: 'specification', title: '规格', type: 'input', width: 110 },
  { key: 'unitName', title: '单位', type: 'input', width: 80, defaultHidden: true },
  { key: 'quantity', title: '数量', type: 'input', width: 80, align: 'right' },
  { key: 'price', title: '单价', type: 'input', width: 100, align: 'right' },
  { key: 'totalPrice', title: '小计', type: 'slot', slotName: 'totalPriceCell', width: 120, align: 'right' },
  { key: 'member', title: '会员', type: 'slot', slotName: 'memberCell', width: 130, defaultHidden: true },
  { key: 'createTime', title: '加入时间', type: 'slot', slotName: 'createTimeCell', width: 160, defaultHidden: true }
]

// ═══ 页面配置（查询条件显隐 + 功能按钮启用） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'productName', label: '商品名称', visible: true },
  { key: 'memberName', label: '会员', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'batchDelete', label: '批量删除', enabled: true }
  // 2026-09-23 移除 'clearCart' 按钮项（假按钮，见模板内注释与审计报告 P1-8）
]
const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)

function isQueryVisible(key: string): boolean {
  const hit = queryFields.value.find(f => f.key === key)
  return hit ? hit.visible : false
}

function isButtonEnabled(key: string): boolean {
  const hit = functionButtons.value.find(b => b.key === key)
  return hit ? hit.enabled : true
}

function handlePageConfigChange(config: { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }) {
  if (config?.queryFields) queryFields.value = config.queryFields.map(f => ({ ...f }))
  if (config?.functionButtons) functionButtons.value = config.functionButtons.map(b => ({ ...b }))
}

// ═══ 格式化 ═══
function formatAmount(v: number | undefined | null): string {
  return (v ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmtTime(val?: string | null): string {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm')
}

// ═══ 统计（本页商品件数合计；总条数用后端 total） ═══
const totalItems = computed(() =>
  tableData.value.reduce((sum: number, row: any) => sum + Number(row.quantity || 0), 0))

// ═══ 数据加载（服务端分页：GET /v1/mall/cart/page） ═══
async function fetchData() {
  loading.value = true
  try {
    const res: any = await mallCartApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      memberKeyword: searchForm.memberKeyword || undefined,
      productKeyword: searchForm.productKeyword || undefined
    })
    // 响应拦截器对 {records,total} 分页体透传不拆包
    const payload = res?.records !== undefined ? res : (res?.data || {})
    tableData.value = Array.isArray(payload.records) ? payload.records : []
    pagination.total = Number(payload.total) || 0
    selectedRecords.value = []
    tableRef.value?.clearSelection?.()
  } catch (error: any) {
    console.error('[购物车] 加载列表失败', error)
    message.error(error?.response?.data?.message || error?.message || '加载购物车失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// ═══ 查询 / 分页（服务端：查询与切页均重新请求） ═══
function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  Object.assign(searchForm, { productKeyword: undefined, memberKeyword: undefined })
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 勾选 / 删除 ═══
function handleCheckboxChange(record: any, _rowIndex: number, checked: boolean) {
  if (!record || record.__ghost) return
  if (checked) {
    if (!selectedRecords.value.some((r: any) => r.id === record.id)) {
      selectedRecords.value = [...selectedRecords.value, record]
    }
  } else {
    selectedRecords.value = selectedRecords.value.filter((r: any) => r.id !== record.id)
  }
}

function handleCheckboxAll(checked: boolean, records: any[]) {
  const rows = (records || []).filter((r: any) => r && !r.__ghost)
  selectedRecords.value = checked ? rows : []
}

async function handleDelete(record: any) {
  try {
    await mallCartApi.removeItem(record.id)
    message.success('已删除')
    fetchData()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '删除失败')
  }
}

function handleBatchDelete() {
  const ids = selectedRowKeys.value
  if (ids.length === 0) {
    message.warning('请先勾选要删除的购物车商品')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确定删除选中的 ${ids.length} 条购物车商品？`,
    okType: 'danger',
    onOk: async () => {
      try {
        // 后端批量删除端点：DELETE /api/v1/mall/cart/batch，body 为裸数组 [1,2,3]
        await mallCartApi.batchRemove(ids)
        message.success(`已删除 ${ids.length} 条`)
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || error?.message || '批量删除失败')
      }
    }
  })
}

// 2026-09-23 移除 handleClearCart()：原实现调 mallCartApi.clearCart()
// （`DELETE /v1/mall/cart`），该端点按**调用者本人**清空，与管理端列表口径不符，
// 点了对列表无影响（审计报告 P1-8）。清空需求用「勾选行 → 批量删除」覆盖。

// ═══ 打印(F8)：真实打印模板（与列表同口径，打印当前页） ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  const rows = tableData.value.filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.productName || '')}</td>
      <td>${escapeHtml(r.productCode || '')}</td>
      <td>${escapeHtml(r.specification || '')}</td>
      <td style="text-align:right">${escapeHtml(r.quantity ?? '')}</td>
      <td style="text-align:right">${formatAmount(r.price)}</td>
      <td style="text-align:right">${formatAmount(r.totalPrice)}</td>
      <td>${escapeHtml(r.memberName || r.memberAccount || '')}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>购物车</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>购物车</h2>
    <div class="meta">
      <span>商品名称：${escapeHtml(searchForm.productKeyword || '全部')}</span>
      <span>共(全部)：${pagination.total}</span>
      <span>本页商品总数：${totalItems.value}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>商品名称</th><th>商品编码</th><th>规格</th><th>数量</th><th>单价</th><th>小计</th><th>会员</th></tr></thead>
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

// ═══ 导出（CSV，前端拼装；后端购物车端点无导出接口 → 导出当前页） ═══
function handleExport() {
  const rows = tableData.value.filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['商品名称', '商品编码', '规格', '数量', '单价', '小计', '会员', '加入时间']
  const csv = [
    headers.join(','),
    ...rows.map((r: any) => [
      r.productName || '', r.productCode || '', r.specification || '',
      r.quantity ?? '', r.price ?? '', r.totalPrice ?? '',
      r.memberName || r.memberAccount || '', r.createTime ? fmtTime(r.createTime) : ''
    ].map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
  ].join('\r\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `购物车_${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleError(error: Error) {
  console.error('[购物车] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  fetchData()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.toolbar-tip { font-size: 12px; color: #8c8c8c; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-strong { font-size: 13px; }
.currency-value { font-family: 'SFMono-Regular', Consolas, monospace; font-variant-numeric: tabular-nums; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
