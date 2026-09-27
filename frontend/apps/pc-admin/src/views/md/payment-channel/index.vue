<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        支付渠道主数据（资料 → 支付管理 → 支付渠道）
        · 单入口；无左侧分类树；列配置齿轮在数据表表头（个人配置 / 全局配置）
        · 工具栏：新增 / 导入 ｜ 页面配置 / 刷新 / 打印(F8) / 导出
        · 查询区：筛选条件（编码/名称/商户号）· 支付方式 · 显示状态
        · 列表：渠道编码 / 渠道名称 / 支付方式 / 商户号 / 排序 / 状态 / 创建时间（+ 备注、渠道配置隐藏列）
        · 弹窗：渠道编码(编辑禁用) / 渠道名称 / 支付方式 / 商户号 / 排序 / 状态 / 备注 / 渠道配置(JSON)
        ⚠️ 全系统唯一渠道主数据（md_payment_channel），收付款单资金路由统一引用，禁止另建渠道表。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增 + 导入 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('add')"
              type="primary"
              size="small"
              class="btn-add"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增
            </a-button>
            <a-button
              v-if="isButtonEnabled('import')"
              size="small"
              @click="importModalVisible = true"
            >
              <UploadOutlined /> 导入
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 + 刷新 + 打印(F8) + 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="fetchList"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('printF8')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（字段显隐由页面配置控制） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div
                v-if="isQueryVisible('keyword')"
                class="search-item"
              >
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="请输入渠道编码/名称/商户号"
                  size="small"
                  style="width: 240px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('methodId')"
                class="search-item"
              >
                <span class="search-label">支付方式</span>
                <a-select
                  v-model:value="searchForm.methodId"
                  placeholder="全部支付方式"
                  size="small"
                  style="width: 190px"
                  allow-clear
                  :options="methodOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('status')"
                class="search-item"
              >
                <span class="search-label">显示状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  size="small"
                  style="width: 110px"
                  @change="handleSearch"
                >
                  <a-select-option :value="''">
                    全部
                  </a-select-option>
                  <a-select-option :value="1">
                    已启用
                  </a-select-option>
                  <a-select-option :value="0">
                    已停用
                  </a-select-option>
                </a-select>
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
              storage-key="md-payment-channel-columns"
              global-config-key="md-payment-channel-columns-global"
              row-key="id"
              @sort-change="handleSortChange"
            >
              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="handleEdit(record)"
                >{{ record.channelName }}</a>
              </template>

              <template #methodCell="{ record }">
                <span v-if="!record.__ghost">{{ methodText(record) }}</span>
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="record.status === 1 ? 'green' : 'default'"
                >
                  {{ record.status === 1 ? '已启用' : '已停用' }}
                </a-tag>
              </template>

              <template #configCell="{ record }">
                <a-tag
                  v-if="!record.__ghost && record.configJson"
                  color="blue"
                >
                  已配置
                </a-tag>
                <span v-else-if="!record.__ghost">-</span>
              </template>

              <template #timeCell="{ record }">
                <span v-if="!record.__ghost">{{ formatTime(record.createTime) }}</span>
              </template>

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
                    @click="handleToggleStatus(record)"
                  >
                    {{ record.status === 1 ? '停用' : '启用' }}
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

      <!-- ═══ 新增 / 修改弹窗 ═══ -->
      <a-modal
        v-model:open="modalVisible"
        title="支付渠道"
        :width="640"
        :mask-closable="false"
        :confirm-loading="saving"
        ok-text="保存(Enter)"
        cancel-text="关闭(Esc)"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="formState"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
          size="small"
        >
          <a-form-item
            label="渠道编码"
            required
          >
            <a-input
              v-model:value="formState.channelCode"
              placeholder="如 WECHAT_MP / ALIPAY_WEB"
              :maxlength="50"
              :disabled="!!formState.id"
            />
          </a-form-item>

          <a-form-item
            label="渠道名称"
            required
          >
            <a-input
              v-model:value="formState.channelName"
              placeholder="如 微信-公众号支付"
              :maxlength="100"
            />
          </a-form-item>

          <a-form-item
            label="支付方式"
            required
          >
            <a-select
              v-model:value="formState.methodId"
              placeholder="请选择支付方式"
              :options="methodOptions"
              allow-clear
            />
          </a-form-item>

          <a-form-item label="商户号">
            <a-input
              v-model:value="formState.merchantNo"
              placeholder="微信商户号 / 支付宝PID"
              :maxlength="200"
            />
          </a-form-item>

          <a-form-item label="排序">
            <a-input-number
              v-model:value="formState.sort"
              :min="0"
              :precision="0"
              style="width: 100%"
            />
          </a-form-item>

          <a-form-item label="状态">
            <a-switch
              :checked="formState.status === 1"
              checked-children="启用"
              un-checked-children="停用"
              @change="(v: any) => (formState.status = v ? 1 : 0)"
            />
          </a-form-item>

          <a-form-item label="渠道配置">
            <a-textarea
              v-model:value="formState.configJson"
              placeholder='渠道扩展配置，JSON 格式，如 {"appId":"wx123","apiKey":"***"}'
              :rows="3"
              :maxlength="2000"
            />
          </a-form-item>

          <a-form-item label="备注">
            <a-textarea
              v-model:value="formState.remark"
              placeholder="请输入备注"
              :rows="2"
              :maxlength="500"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 导入向导（三步：下载模板 / 导入Excel / 完成） ═══ -->
      <BaseDataImportWizard
        v-model:open="importModalVisible"
        title="基本信息导入"
        template-url="/erp/md/payment-channel/import-template"
        template-file-name="支付渠道导入模板"
        import-url="/erp/md/payment-channel/import-excel"
        @success="handleImportSuccess"
      />

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="md-payment-channel-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="md-payment-channel"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  UploadOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import BaseDataImportWizard from '@/components/business/BaseDataImportWizard/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { paymentChannelApi, paymentMethodApi } from '@/api/payment/md'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'MdPaymentChannel' })

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])

// ═══ 查询条件 ═══
const searchForm = reactive({
  keyword: '',
  methodId: undefined as number | undefined,
  status: '' as '' | 0 | 1,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 数据表列（默认 9 列可见 + 备注/渠道配置隐藏列） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 150, fixed: 'left' },
  { title: '渠道编码', key: 'channelCode', width: 150, sortable: true },
  { title: '渠道名称', key: 'channelName', type: 'slot', slotName: 'nameCell', width: 220, sortable: true },
  { title: '支付方式', key: 'methodName', type: 'slot', slotName: 'methodCell', width: 190 },
  { title: '商户号', key: 'merchantNo', width: 180 },
  { title: '排序', key: 'sort', width: 80, sortable: true },
  { title: '状态', key: 'status', type: 'slot', slotName: 'statusCell', width: 100, sortable: true },
  { title: '创建时间', key: 'createTime', type: 'slot', slotName: 'timeCell', width: 170, sortable: true },
  { title: '备注', key: 'remark', width: 200, defaultHidden: true },
  { title: '渠道配置', key: 'configJson', type: 'slot', slotName: 'configCell', width: 110, defaultHidden: true },
]

// ═══ 支付方式下拉（[编码] 名称） ═══
const methodOptions = ref<{ label: string; value: number }[]>([])
async function loadMethods() {
  try {
    const res: any = await paymentMethodApi.list()
    const list: any[] = Array.isArray(res) ? res : res?.records || res?.data || []
    methodOptions.value = list.map((m: any) => ({ label: `[${m.methodCode}] ${m.methodName}`, value: m.id }))
  } catch (error) {
    console.warn('[支付渠道] 加载支付方式失败', error)
  }
}

function methodText(record: any): string {
  if (!record?.methodName) return '-'
  return record.methodCode ? `[${record.methodCode}] ${record.methodName}` : record.methodName
}

function formatTime(val: string | null | undefined): string {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

// ═══ 查询参数拼装 ═══
const sortState = reactive<{ field?: string; order?: string }>({ field: undefined, order: undefined })

function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (searchForm.keyword) params.keyword = searchForm.keyword
  if (searchForm.methodId != null) params.methodId = searchForm.methodId
  if (searchForm.status !== '') params.status = searchForm.status
  if (sortState.field) {
    params.sortField = sortState.field
    params.sortOrder = sortState.order
  }
  return params
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await paymentChannelApi.page(buildParams())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[支付渠道] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
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

/** 表头排序（服务端排序，白名单列生效，其余回落默认口径） */
function handleSortChange(key: string | null, order: string | null) {
  sortState.field = key || undefined
  sortState.order = order || undefined
  pagination.current = 1
  fetchList()
}

// ═══ 新增 / 修改弹窗 ═══
const modalVisible = ref(false)
const formState = reactive({
  id: null as number | null,
  channelCode: '',
  channelName: '',
  methodId: undefined as number | undefined,
  merchantNo: '',
  configJson: '',
  sort: 0,
  status: 1,
  remark: '',
})

function resetForm() {
  formState.id = null
  formState.channelCode = ''
  formState.channelName = ''
  formState.methodId = undefined
  formState.merchantNo = ''
  formState.configJson = ''
  formState.sort = 0
  formState.status = 1
  formState.remark = ''
}

function handleAdd() {
  resetForm()
  modalVisible.value = true
}

async function handleEdit(record: any) {
  resetForm()
  modalVisible.value = true
  try {
    const detail: any = await paymentChannelApi.getById(record.id)
    if (!detail) return
    formState.id = detail.id
    formState.channelCode = detail.channelCode || ''
    formState.channelName = detail.channelName || ''
    formState.methodId = detail.methodId
    formState.merchantNo = detail.merchantNo || ''
    formState.configJson = detail.configJson || ''
    formState.sort = detail.sort ?? 0
    formState.status = detail.status ?? 1
    formState.remark = detail.remark || ''
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载支付渠道详情失败')
  }
}

function assertConfigJson(): boolean {
  const text = (formState.configJson || '').trim()
  if (!text) return true
  try {
    JSON.parse(text)
    return true
  } catch {
    message.warning('渠道配置必须是合法的 JSON 格式')
    return false
  }
}

async function handleSave() {
  if (!formState.channelCode.trim()) {
    message.warning('请输入渠道编码')
    return
  }
  if (!formState.channelName.trim()) {
    message.warning('请输入渠道名称')
    return
  }
  if (formState.methodId == null) {
    message.warning('请选择支付方式')
    return
  }
  if (!assertConfigJson()) return

  const payload = {
    channelCode: formState.channelCode.trim(),
    channelName: formState.channelName.trim(),
    methodId: formState.methodId,
    merchantNo: formState.merchantNo || null,
    configJson: formState.configJson ? formState.configJson.trim() : null,
    sort: formState.sort ?? 0,
    status: formState.status,
    remark: formState.remark || null,
  }

  saving.value = true
  try {
    if (formState.id) {
      await paymentChannelApi.update(formState.id, payload)
      message.success('修改成功')
    } else {
      await paymentChannelApi.create(payload)
      message.success('新增成功')
    }
    modalVisible.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 删除 ═══
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除支付渠道「${record.channelName}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await paymentChannelApi.remove(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 启用 / 停用 ═══
function handleToggleStatus(record: any) {
  const target = record.status === 1 ? 0 : 1
  const actionText = target === 1 ? '启用' : '停用'
  Modal.confirm({
    title: '确认',
    content: `确定要${actionText}支付渠道「${record.channelName}」吗？`,
    onOk: async () => {
      try {
        await paymentChannelApi.updateStatus(record.id, target)
        message.success(`已${actionText}`)
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '操作失败')
      }
    },
  })
}

// ═══ 打印(F8) ═══
// ═══ 打印（结果集打印） ═══
// 原先是 window.print() —— 打出来是整个后台界面（菜单/工具栏/翻页都跟着上纸）。
// 列取页面自己的列定义，模板按数据里的列画表头（改列不用改模板）。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'md-payment-channel',
  title: '页面配置',
  columns: () => columns,
  rows: () => tableData.value,
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
  // 弹窗内 Enter 保存（多行文本域内不触发）
  if (e.key === 'Enter' && modalVisible.value && !e.ctrlKey && !e.altKey && !e.metaKey) {
    const target = e.target as HTMLElement | null
    if (target && target.tagName === 'TEXTAREA') return
    e.preventDefault()
    handleSave()
  }
}

// ═══ 导出（真实 Excel） ═══
async function handleExport() {
  exporting.value = true
  try {
    const params: Record<string, any> = {}
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.methodId != null) params.methodId = searchForm.methodId
    if (searchForm.status !== '') params.status = searchForm.status

    const blob: any = await paymentChannelApi.export(params)
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `支付渠道_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 导入（三步向导） ═══
const importModalVisible = ref(false)

function handleImportSuccess(result: any) {
  if (!result) return
  if (result.failure > 0) {
    message.warning(`导入完成：成功 ${result.success} 条，失败 ${result.failure} 条`)
  } else {
    message.success(`导入成功 ${result.success} 条`)
  }
  fetchList()
}

// ═══ 页面配置（查询条件显隐 / 功能按钮开关） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '筛选条件', visible: true },
  { key: 'methodId', label: '支付方式', visible: true },
  { key: 'status', label: '显示状态', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'import', label: '导入', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
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
  return hit ? hit.enabled : false
}

function handlePageConfigChange(config: { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }) {
  if (config?.queryFields) queryFields.value = config.queryFields.map(f => ({ ...f }))
  if (config?.functionButtons) functionButtons.value = config.functionButtons.map(b => ({ ...b }))
}

function handleError(error: Error) {
  console.error('[支付渠道] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadMethods()
  fetchList()
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
