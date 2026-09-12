<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        支付方式（资料 → 支付管理 → 支付方式，菜单 80550）
        · 单入口：列表 + 新增/编辑弹窗（无独立表单页）
        · 无左侧分类树；列配置齿轮在数据表表头（个人配置 / 全局配置）
        · 工具栏：新增 ｜ 刷新 / 打印(F8) / 导出 / 更多(批量启用·停用)
        · 查询区（固定项）：筛选条件(编码/名称) + 类型 + 状态
        · ⚠️ 全局唯一支付方式字典：收款/付款/预收/预付单据统一引用（严禁另建重复字典）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              type="primary"
              size="small"
              class="btn-add"
              @click="openCreate"
            >
              <PlusOutlined /> 新增
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：刷新 + 打印(F8) + 导出 + 更多 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
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
            <a-button
              size="small"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
            <!-- 注意：#overlay 插槽内首个子节点必须是 a-menu，注释节点会让 antd 取到注释而渲染空浮层 -->
            <a-dropdown>
              <a-button size="small">
                更多 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item
                    :disabled="selectedRows.length === 0"
                    @click="handleBatchStatus(1)"
                  >
                    启用
                  </a-menu-item>
                  <a-menu-item
                    :disabled="selectedRows.length === 0"
                    @click="handleBatchStatus(0)"
                  >
                    停用
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>

        <!-- ═══ 查询区（固定项） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div class="search-item">
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="请输入支付方式编码/名称"
                  size="small"
                  style="width: 240px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">类型</span>
                <a-select
                  v-model:value="searchForm.methodType"
                  placeholder="全部类型"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  :options="METHOD_TYPE_OPTIONS"
                />
              </div>
              <div class="search-item">
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="STATUS_OPTIONS"
                />
              </div>
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

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列右上角） ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="columns"
              :data-source="tableData"
              :loading="loading"
              :pagination="false"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              storage-key="md-payment-method-columns"
              global-config-key="md-payment-method-columns-global"
              row-key="id"
              @selection-change="handleSelectionChange"
            >
              <template #nameCell="{ record }">
                <a
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.methodName }}</a>
              </template>

              <template #typeCell="{ record }">
                {{ METHOD_TYPE_MAP[record.methodType] || record.methodType || '-' }}
              </template>

              <template #feeRateCell="{ record }">
                {{ formatFeeRate(record.feeRate) }}
              </template>

              <template #defaultCell="{ record }">
                <a-tag
                  v-if="record.isDefault === 1"
                  color="blue"
                >
                  默认
                </a-tag>
                <span v-else>-</span>
              </template>

              <template #statusCell="{ record }">
                <a-tag :color="record.status === 1 ? 'green' : 'default'">
                  {{ record.status === 1 ? '启用' : '停用' }}
                </a-tag>
              </template>

              <template #timeCell="{ record }">
                {{ formatTime(record.createTime) }}
              </template>

              <template #actionCell="{ record }">
                <a-space :size="0">
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
                  <a-dropdown>
                    <a-button
                      type="link"
                      size="small"
                    >
                      更多
                    </a-button>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item @click="handleToggleStatus(record)">
                          {{ record.status === 1 ? '停用' : '启用' }}
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
              </template>
            </BillTableList>
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

      <!-- ═══ 新增 / 编辑弹窗 ═══ -->
      <a-modal
        v-model:open="modalOpen"
        :title="editingId ? '编辑支付方式' : '新增支付方式'"
        :confirm-loading="saving"
        width="620px"
        ok-text="保存"
        cancel-text="取消"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="form"
          :rules="rules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item
            label="编码"
            name="methodCode"
          >
            <a-input
              v-model:value="form.methodCode"
              placeholder="如 CASH / BANK / WECHAT（保存后不可修改）"
              :disabled="!!editingId"
            />
          </a-form-item>
          <a-form-item
            label="名称"
            name="methodName"
          >
            <a-input
              v-model:value="form.methodName"
              placeholder="如 现金 / 银行转账 / 微信支付"
            />
          </a-form-item>
          <a-form-item
            label="类型"
            name="methodType"
          >
            <a-select
              v-model:value="form.methodType"
              placeholder="请选择"
              :options="METHOD_TYPE_OPTIONS"
            />
          </a-form-item>
          <a-form-item label="默认入账账户">
            <a-select
              v-model:value="form.accountId"
              placeholder="不选则不关联"
              allow-clear
              show-search
              option-filter-prop="label"
              :options="accountOptions"
            />
          </a-form-item>
          <a-form-item label="手续费率(%)">
            <a-input-number
              v-model:value="form.feeRatePercent"
              :min="0"
              :max="100"
              :step="0.01"
              :precision="2"
              style="width: 100%"
              placeholder="如 0.6 表示 0.6%"
            />
          </a-form-item>
          <a-form-item label="排序">
            <a-input-number
              v-model:value="form.sort"
              :min="0"
              :precision="0"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="默认">
            <a-switch
              :checked="form.isDefault === 1"
              checked-children="是"
              un-checked-children="否"
              @change="(v: any) => (form.isDefault = v ? 1 : 0)"
            />
            <span class="field-hint">系统唯一默认支付方式（设为默认将自动取消其它默认）</span>
          </a-form-item>
          <a-form-item label="状态">
            <a-switch
              :checked="form.status === 1"
              checked-children="启用"
              un-checked-children="停用"
              @change="(v: any) => (form.status = v ? 1 : 0)"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea
              v-model:value="form.remark"
              :rows="2"
              :maxlength="500"
              placeholder="选填"
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
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  DownOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import optionsApi from '@/api/options'
import { paymentMethodApi, METHOD_TYPE_OPTIONS, METHOD_TYPE_MAP } from '@/api/payment/md'
import type { PaymentMethodQuery } from '@/api/payment/md'

defineOptions({ name: 'MdPaymentMethod' })

// ═══ 字典 ═══
const STATUS_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
]

function formatTime(val: string | null | undefined): string {
  return val ? String(val).replace('T', ' ').slice(0, 19) : '-'
}
/** 小数费率 → 百分比展示（0.0060 → 0.60%） */
function formatFeeRate(val: number | null | undefined): string {
  if (val === null || val === undefined) return '-'
  return `${(Number(val) * 100).toFixed(2)}%`
}

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const selectedRows = ref<any[]>([])

// ═══ 查询条件 ═══
const searchForm = reactive<{ keyword: string; methodType?: string; status?: number }>({
  keyword: '',
  methodType: undefined,
  status: undefined,
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 数据表列（默认 10 列；rowNo/勾选/操作为固定列） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '', key: 'rowCheck', type: 'checkbox', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 150, fixed: 'left' },
  { title: '支付方式编码', key: 'methodCode', width: 140 },
  { title: '支付方式名称', key: 'methodName', type: 'slot', slotName: 'nameCell', width: 200 },
  { title: '类型', key: 'methodType', type: 'slot', slotName: 'typeCell', width: 110 },
  { title: '默认入账账户', key: 'accountName', width: 180 },
  { title: '手续费率', key: 'feeRate', type: 'slot', slotName: 'feeRateCell', width: 100, align: 'right' },
  { title: '默认', key: 'isDefault', type: 'slot', slotName: 'defaultCell', width: 80, align: 'center' },
  { title: '排序', key: 'sort', width: 80, align: 'right' },
  { title: '状态', key: 'status', type: 'slot', slotName: 'statusCell', width: 90, align: 'center' },
  { title: '备注', key: 'remark', width: 200 },
  { title: '创建时间', key: 'createTime', type: 'slot', slotName: 'timeCell', width: 170 },
]

// ═══ 查询参数（列表与导出共用同一口径） ═══
function buildQuery(): PaymentMethodQuery {
  const params: PaymentMethodQuery = {}
  if (searchForm.keyword) params.keyword = searchForm.keyword.trim()
  if (searchForm.methodType) params.methodType = searchForm.methodType
  if (searchForm.status !== undefined && searchForm.status !== null) params.status = searchForm.status
  return params
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await paymentMethodApi.page({
      ...buildQuery(),
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[支付方式] 加载列表失败', error)
    message.error(error?.data?.message || error?.response?.data?.message || '加载列表失败')
  } finally {
    loading.value = false
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows || []
}

// ═══ 账户下拉（默认入账账户 → finance_account，与银行账户/收付款单同一口径） ═══
const accountOptions = ref<{ label: string; value: number }[]>([])
async function loadAccounts() {
  try {
    const list: any = await optionsApi.getAccounts()
    accountOptions.value = (Array.isArray(list) ? list : []).map((a: any) => ({
      label: a.name ? `${a.name}${a.code ? '(' + a.code + ')' : ''}` : String(a.id),
      value: a.id,
    }))
  } catch (error) {
    console.error('[支付方式] 加载财务账户失败', error)
  }
}

// ═══ 弹窗 ═══
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  methodCode: '',
  methodName: '',
  methodType: undefined as string | undefined,
  accountId: undefined as number | undefined,
  feeRatePercent: 0,
  sort: 0,
  isDefault: 0,
  status: 1,
  remark: '',
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  methodCode: [
    { required: true, message: '请输入支付方式编码', trigger: 'blur' },
    { max: 50, message: '编码不能超过 50 个字符', trigger: 'blur' },
  ],
  methodName: [
    { required: true, message: '请输入支付方式名称', trigger: 'blur' },
    { max: 100, message: '名称不能超过 100 个字符', trigger: 'blur' },
  ],
  methodType: [{ required: true, message: '请选择支付方式类型', trigger: 'change' }],
}

function resetForm(data?: Partial<ReturnType<typeof emptyForm>>) {
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
    methodCode: record.methodCode,
    methodName: record.methodName,
    methodType: record.methodType,
    accountId: record.accountId ?? undefined,
    feeRatePercent: record.feeRate ? Number((Number(record.feeRate) * 100).toFixed(2)) : 0,
    sort: record.sort ?? 0,
    isDefault: record.isDefault ?? 0,
    status: record.status ?? 1,
    remark: record.remark || '',
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
    const payload = {
      methodCode: form.methodCode.trim(),
      methodName: form.methodName.trim(),
      methodType: form.methodType,
      accountId: form.accountId || undefined,
      feeRate: form.feeRatePercent ? Number((form.feeRatePercent / 100).toFixed(4)) : 0,
      sort: form.sort ?? 0,
      isDefault: form.isDefault,
      status: form.status,
      remark: form.remark || '',
    }
    if (editingId.value) {
      await paymentMethodApi.update(editingId.value, payload as any)
      message.success('修改成功')
    } else {
      await paymentMethodApi.create(payload as any)
      message.success('新增成功')
    }
    modalOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.data?.message || error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 启用 / 停用 ═══
async function toggleStatus(record: any) {
  const target = record.status === 1 ? 0 : 1
  try {
    await paymentMethodApi.updateStatus(record.id, target)
    message.success(target === 1 ? '已启用' : '已停用')
    fetchList()
  } catch (error: any) {
    message.error(error?.data?.message || error?.response?.data?.message || '操作失败')
  }
}

function handleToggleStatus(record: any) {
  const actionText = record.status === 1 ? '停用' : '启用'
  Modal.confirm({
    title: '确认',
    content: `确定要${actionText}支付方式「${record.methodName}」吗？`,
    onOk: () => toggleStatus(record),
  })
}

/** 工具栏「更多 → 启用 / 停用」：对勾选行批量生效 */
function handleBatchStatus(status: number) {
  const actionText = status === 1 ? '启用' : '停用'
  Modal.confirm({
    title: '批量修改',
    content: `确定要${actionText}选中的 ${selectedRows.value.length} 条支付方式吗？`,
    onOk: async () => {
      try {
        await paymentMethodApi.batchStatus(selectedRows.value.map(r => r.id), status)
        message.success(`批量${actionText}成功`)
        fetchList()
      } catch (error: any) {
        message.error(error?.data?.message || error?.response?.data?.message || `批量${actionText}失败`)
      }
    },
  })
}

// ═══ 删除（后端做引用保护：被支付渠道/收付款单引用时拒绝） ═══
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除支付方式「${record.methodName}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await paymentMethodApi.remove(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.data?.message || error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 打印(F8) ═══
function handlePrint() {
  if (tableData.value.length === 0) {
    message.warning('没有可打印的数据')
    return
  }
  window.print()
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（真实 xlsx） ═══
async function handleExport() {
  try {
    const blob: any = await paymentMethodApi.export(buildQuery())
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `支付方式_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.data?.message || error?.response?.data?.message || '导出失败')
  }
}

function handleError(error: Error) {
  console.error('[支付方式] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  fetchList()
  loadAccounts()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.field-hint { margin-left: 8px; font-size: 12px; color: #999; }

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
