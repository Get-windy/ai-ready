<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        配额管理（系统 → 租户管理 → 配额管理，菜单 62005）
        · 平台控制台页面：ql361 无对标 → 按 SAP BTP「配额（Quota）」能力模型建模（不照搬界面）
        · 本页是平台侧的租户配额台账：一张 8 列表格 + 页内新增/编辑弹窗 + 行内编辑/删除
        · 🔴 本轮修复：弹窗「租户名称/租户编码」由自由文本改为**租户下拉选择器**并显式提交 tenantId
          （此前不传 tenantId → 归属被多租户插件按会话租户注入，超管会话恒为 1，与所填名称无关）
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/配额管理开发文档.md
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增配额 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('add')"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新增配额
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：刷新 + 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（租户名称 + 使用率） ═══
             后端 GET /api/tenant-quota/list 无入参，暂无服务端筛选 → 两项均为前端本地过滤 -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('tenantName')">
                <span class="search-label">租户名称</span>
                <a-input
                  v-model:value="searchForm.tenantName"
                  placeholder="请输入租户名称/编码"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('usage')">
                <span class="search-label">使用率</span>
                <a-select
                  v-model:value="searchForm.usage"
                  size="small"
                  style="width: 130px"
                  :options="USAGE_OPTIONS"
                  @change="handleSearch"
                />
              </template>
              <a-button
                type="primary"
                size="small"
                @click="handleSearch"
              >
                查询
              </a-button>
              <a-button
                size="small"
                @click="handleReset"
              >
                重置
              </a-button>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="filteredList"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="system-tenant-quota-table-columns"
              global-config-key="system-tenant-quota-table-columns"
            >
              <!-- 使用率（口径：usedApiCalls / maxApiCalls，仅 API 调用维度） -->
              <template #usageCell="{ record }">
                <a-progress
                  :percent="calcUsagePercent(record)"
                  size="small"
                  :status="calcUsagePercent(record) > 80 ? 'exception' : undefined"
                />
              </template>

              <!-- 操作列 -->
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
                    编辑
                  </a-button>
                  <a-popconfirm
                    title="确定删除此配额?"
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

        <!-- ═══ 底部：经典分页栏（前端本地分页） ═══ -->
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

      <!-- ═══ 页面配置（查询条件显隐 + 功能按钮启用） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="PAGE_CONFIG_STORAGE_KEY"
        :hide-print-config="true"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 新增/编辑配额弹窗 ═══ -->
      <a-modal
        v-model:open="editVisible"
        :title="editingId ? '编辑配额' : '新增配额'"
        width="520px"
        :confirm-loading="saving"
        destroy-on-close
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="editForm"
          :rules="formRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          style="margin-top:16px"
        >
          <!-- 🔴 租户选择器（原「租户名称 / 租户编码」两个自由文本框已删除）：
               选中后同时填充 tenantId / tenantName / tenantCode，保证归属不再由插件按会话租户猜 -->
          <a-form-item
            label="租户"
            name="tenantId"
          >
            <a-select
              v-model:value="editForm.tenantId"
              placeholder="请选择租户"
              show-search
              :options="tenantOptions"
              :filter-option="filterTenantOption"
              :loading="tenantLoading"
              @change="handleTenantChange"
            />
          </a-form-item>
          <a-form-item
            label="租户编码"
            name="tenantCode"
          >
            <!-- 编码由所选租户带出，只读展示（避免与 sys_tenant 不一致） -->
            <a-input
              :value="tenantCodeDisplay"
              disabled
              placeholder="选择租户后自动带出"
            />
          </a-form-item>
          <a-form-item
            label="最大用户数"
            name="maxUsers"
          >
            <a-input-number
              v-model:value="editForm.maxUsers"
              :min="1"
              style="width:100%"
            />
          </a-form-item>
          <a-form-item
            label="存储配额"
            name="maxStorage"
          >
            <a-select
              v-model:value="editForm.maxStorage"
              :options="STORAGE_OPTIONS"
            />
          </a-form-item>
          <a-form-item
            label="API调用限制/月"
            name="maxApiCalls"
          >
            <a-input-number
              v-model:value="editForm.maxApiCalls"
              :min="0"
              style="width:100%"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { tenantQuotaApi, tenantApi, type TenantQuotaInfo, type TenantInfo } from '@/api/tenant'

defineOptions({ name: 'AdminTenantQuota' })

const PAGE_CONFIG_STORAGE_KEY = 'system-tenant-quota-page-config'

// 页面配置的查询条件 / 功能按钮项（与 PageConfigPanel 的 prop 结构一致）
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// ═══ 枚举 ═══
/** 使用率筛选项：后端无入参，前端按 calcUsagePercent 结果本地过滤 */
const USAGE_OPTIONS = [
  { label: '全部', value: 'all' },
  { label: '≥80%', value: 'gte80' },
  { label: '<80%', value: 'lt80' },
]
/** 存储配额候选值：与库中 max_storage varchar(20) 的既有取值保持一致（5GB/10GB/50GB/100GB/500GB/1TB） */
const STORAGE_OPTIONS = ['5GB', '10GB', '50GB', '100GB', '500GB', '1TB'].map(v => ({ label: v, value: v }))

// ═══ 状态 ═══
const loading = ref(false)
const saving = ref(false)
const allList = ref<TenantQuotaInfo[]>([])
/** 前端「过滤 + 当前页切片」后的结果，直接喂给表格（本页 v-model:data-source） */
const filteredList = ref<TenantQuotaInfo[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({
  tenantName: '' as string,
  usage: 'all' as string,
})

// ═══ 查询条件 / 功能按钮（页面配置） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'tenantName', label: '租户名称', visible: true },
  { key: 'usage', label: '使用率', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增配额', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
]
const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)

function isQueryFieldVisible(key: string) {
  return queryFields.value.find(f => f.key === key)?.visible !== false
}
function isButtonEnabled(key: string) {
  return functionButtons.value.find(f => f.key === key)?.enabled !== false
}
function handlePageConfigChange(config: PageConfig) {
  if (config?.queryFields?.length) queryFields.value = config.queryFields
  if (config?.functionButtons?.length) functionButtons.value = config.functionButtons
}

// 序号列承载表头「列配置」齿轮；操作列为固定列
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 150, fixed: 'left' },
  { key: 'tenantName', title: '租户名称', type: 'input', width: 180 },
  { key: 'tenantCode', title: '租户编码', type: 'input', width: 150 },
  { key: 'maxUsers', title: '最大用户数', type: 'input', width: 100 },
  { key: 'maxStorage', title: '存储配额', type: 'input', width: 100 },
  { key: 'maxApiCalls', title: 'API调用限制', type: 'input', width: 120 },
  // ⚠️ 必须写 type:'slot'：只写 slotName 会静默直出原值
  { key: 'usage', title: '使用率', type: 'slot', slotName: 'usageCell', width: 150 },
  { key: 'createTime', title: '创建时间', type: 'input', width: 180 },
]

/**
 * 使用率口径（本系统自定，非业界标准）：只与 API 调用次数有关 ——
 * `usedApiCalls / maxApiCalls × 100`，四舍五入并截顶 100；两值任一为 0/空 → 0。
 * ⚠️ 与存储配额、用户数**完全无关**（库中另有 used_users/used_storage，页面不参与计算）。
 */
function calcUsagePercent(record: Record<string, any>): number {
  const maxApiCalls = Number(record?.maxApiCalls)
  const usedApiCalls = Number(record?.usedApiCalls)
  if (!maxApiCalls || !usedApiCalls) return 0
  return Math.min(Math.round((usedApiCalls / maxApiCalls) * 100), 100)
}

// ═══ 编辑弹窗 ═══
const editVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()
interface QuotaEditForm {
  tenantId?: number | string
  tenantName: string
  tenantCode: string
  maxUsers: number
  maxStorage: string
  maxApiCalls: number
}
const emptyForm = (): QuotaEditForm => ({
  tenantId: undefined,
  tenantName: '',
  tenantCode: '',
  maxUsers: 10,
  maxStorage: '10GB',
  maxApiCalls: 10000,
})
const editForm = reactive<QuotaEditForm>(emptyForm())
const formRules = {
  tenantId: [{ required: true, message: '请选择租户', trigger: 'change' }],
}

// ═══ 租户下拉数据源（GET /api/tenant/page） ═══
const tenants = ref<TenantInfo[]>([])
const tenantLoading = ref(false)

/**
 * 下拉选项：label 用「租户名称（租户编码）」，value 用租户 id。
 * ⚠️ 编辑种子行时其 tenantId 可能对应一个**已不存在的租户**（如 tenant_id=3），
 *    此时把当前行自身补进选项，避免下拉显示空白导致「看起来没选但实际有值」。
 */
const tenantOptions = computed(() => {
  const opts = tenants.value.map(t => ({
    label: `${t.tenantName}（${t.tenantCode}）`,
    value: t.id as any,
  }))
  const cur = editForm.tenantId
  if (cur !== undefined && cur !== null && cur !== '' && !opts.some(o => String(o.value) === String(cur))) {
    opts.unshift({
      label: `${editForm.tenantName || '未知租户'}（${editForm.tenantCode || '-'}）`,
      value: cur as any,
    })
  }
  return opts
})

/** 下拉搜索：同时匹配 label 里的名称与编码 */
function filterTenantOption(input: string, option: any) {
  return String(option?.label || '').toLowerCase().includes(String(input || '').toLowerCase())
}

/** 当前所选租户的主数据行（`sys_tenant` 为准） */
const selectedTenant = computed(() => {
  const cur = editForm.tenantId
  if (cur === undefined || cur === null || cur === '') return null
  return tenants.value.find(t => String(t.id) === String(cur)) || null
})

/**
 * 租户编码展示值 / 提交值：始终与实际选中的租户同源。
 * ⚠️ 种子行存在「tenant_id=1 却写着『默认租户/DEFAULT』」的脏数据（真实租户为『系统租户/SYSTEM』）；
 *    选中项能对上主数据时以主数据为准，只有租户已被删除（如 tenant_id=3）才回退到行内存储值。
 */
const tenantCodeDisplay = computed(
  () => selectedTenant.value?.tenantCode || editForm.tenantCode || '',
)

async function fetchTenants() {
  tenantLoading.value = true
  try {
    const res: any = await tenantApi.getPage({ pageNum: 1, pageSize: 500 })
    // 响应拦截器已拆掉 Result 外层；兼容两种形态（同租户列表页口径）
    const page = res?.data?.records ? res.data : (res || {})
    tenants.value = page.records || []
  } catch (error: any) {
    console.error('[配额管理] 加载租户下拉失败', error)
    message.error(error?.response?.data?.message || error?.message || '加载租户列表失败')
    tenants.value = []
  } finally {
    tenantLoading.value = false
  }
}

/** 选中租户后把 tenantName / tenantCode 一并带出，提交时与 tenantId 同发 */
function handleTenantChange(value: any) {
  const t = tenants.value.find(item => String(item.id) === String(value))
  if (t) {
    editForm.tenantName = t.tenantName
    editForm.tenantCode = t.tenantCode
  }
}

// ═══ 数据加载（后端 GET /list 无入参 → 全量取回后前端过滤 + 分页） ═══
async function fetchData() {
  loading.value = true
  try {
    const res = await tenantQuotaApi.getList()
    allList.value = res?.records || []
    applyLocalFilter()
  } catch (error: any) {
    console.error('[配额管理] 加载配额列表失败', error)
    message.error(
      (error as any)?.response?.data?.message || error?.message || '加载配额列表失败',
    )
    // 失败即清空，不做假数据兜底
    allList.value = []
    filteredList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 本地过滤 + 本地分页：后端无入参，暂无服务端筛选 */
function applyLocalFilter() {
  const keyword = searchForm.tenantName.trim().toLowerCase()
  let matched = allList.value
  if (keyword) {
    // 关键字同时匹配「租户名称」与「租户编码」（列标题只写租户名称，编码一并检索更实用）
    matched = matched.filter((row) => {
      const name = String(row?.tenantName || '').toLowerCase()
      const code = String(row?.tenantCode || '').toLowerCase()
      return name.includes(keyword) || code.includes(keyword)
    })
  }
  if (searchForm.usage === 'gte80') {
    matched = matched.filter(row => calcUsagePercent(row) >= 80)
  } else if (searchForm.usage === 'lt80') {
    matched = matched.filter(row => calcUsagePercent(row) < 80)
  }

  pagination.total = matched.length
  const maxPage = Math.max(1, Math.ceil(matched.length / pagination.pageSize))
  if (pagination.current > maxPage) pagination.current = maxPage
  const start = (pagination.current - 1) * pagination.pageSize
  filteredList.value = matched.slice(start, start + pagination.pageSize)
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  applyLocalFilter()
}
function handleReset() {
  searchForm.tenantName = ''
  searchForm.usage = 'all'
  pagination.current = 1
  applyLocalFilter()
}
function handleRefresh() {
  fetchData()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  applyLocalFilter()
}

// ═══ 新增 / 编辑 ═══
function openCreate() {
  editingId.value = null
  Object.assign(editForm, emptyForm())
  editVisible.value = true
  if (!tenants.value.length) fetchTenants()
}

function openEdit(record: TenantQuotaInfo) {
  editingId.value = record.id
  Object.assign(editForm, emptyForm(), {
    tenantId: record.tenantId,
    tenantName: record.tenantName || '',
    tenantCode: record.tenantCode || '',
    maxUsers: record.maxUsers ?? 10,
    maxStorage: record.maxStorage || '10GB',
    maxApiCalls: record.maxApiCalls ?? 10000,
  })
  editVisible.value = true
  if (!tenants.value.length) fetchTenants()
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    // 显式提交 tenantId + tenantName/tenantCode（同一次选择三者同源，归属不再靠插件猜）
    const picked = selectedTenant.value
    const payload: Partial<TenantQuotaInfo> = {
      tenantId: editForm.tenantId as number,
      tenantName: picked?.tenantName || editForm.tenantName,
      tenantCode: picked?.tenantCode || editForm.tenantCode,
      maxUsers: editForm.maxUsers,
      maxStorage: editForm.maxStorage,
      maxApiCalls: editForm.maxApiCalls,
    }
    if (editingId.value) {
      await tenantQuotaApi.update(editingId.value, payload)
      message.success('配额已更新')
    } else {
      await tenantQuotaApi.create(payload)
      message.success('配额已创建')
    }
    editVisible.value = false
    editingId.value = null
    await fetchData()
  } catch (e: any) {
    // 优先展示后端原文（如 403「没有操作权限」）
    message.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: TenantQuotaInfo) {
  try {
    await tenantQuotaApi.delete(record.id)
    message.success('配额已删除')
    await fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '删除失败')
  }
}

function handleError(error: Error) {
  console.error('[配额管理] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchData()
  fetchTenants()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.btn-add { background: #fa8c16; border-color: #fa8c16; }
</style>
