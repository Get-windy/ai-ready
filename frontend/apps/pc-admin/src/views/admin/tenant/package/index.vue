<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        租户套餐（系统 → 租户管理 → 租户套餐，菜单 62003）
        · 平台级档位定义：套餐为全平台共享，不按租户隔离（DB 实测 tenant_id=0）
        · 后端 GET /api/tenant-package/list 无任何入参、返回裸 Map{records,total} → 查询与分页均在前端本地完成
        · 价格 DB 以「分」存储，展示 /100、录入 *100（写入用 Math.round 规避浮点误差）
        · 数据表列配置齿轮在表头 rowNo 列（个人 / 全局），storage-key 持久化
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/租户套餐开发文档.md
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增套餐 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('add')"
            type="primary"
            size="small"
            @click="openForm()"
          >
            <PlusOutlined /> 新增套餐
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

        <!-- ═══ 查询区（套餐名称 + 状态，均为前端本地过滤） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('packageName')">
                <span class="search-label">套餐名称</span>
                <a-input
                  v-model:value="searchForm.packageName"
                  placeholder="请输入套餐名称"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('status')">
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  size="small"
                  style="width: 130px"
                  :options="STATUS_OPTIONS"
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
              storage-key="system-tenant-package-table-columns"
              global-config-key="system-tenant-package-table-columns"
            >
              <!-- 购买类型：smallint/字符串值域 → 中文映射（monthly 按月 / yearly 按年 / perpetual 永久） -->
              <template #purchaseTypeCell="{ record }">
                {{ PURCHASE_TYPE_MAP[record.purchaseType] || record.purchaseType || '-' }}
              </template>

              <!-- 价格：DB 存「分」，展示换算为「元」；0 = 免费（不可与未定价混淆） -->
              <template #priceCell="{ record }">
                {{ fmtPrice(record.price) }}
              </template>

              <!-- 状态：1=启用（此档位可售）/ 0=停用 -->
              <template #statusCell="{ record }">
                <a-tag :color="record.status === 1 ? 'green' : 'red'">
                  {{ record.status === 1 ? '启用' : '停用' }}
                </a-tag>
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
                    @click="openForm(record)"
                  >
                    编辑
                  </a-button>
                  <a-popconfirm
                    title="确定删除?"
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

        <!-- ═══ 底部：经典分页栏（后端不分页，前端本地分页） ═══ -->
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

      <!-- ═══ 新增/编辑套餐弹窗（9 字段，与文档 §3.7 逐项一致） ═══ -->
      <a-modal
        v-model:open="formVisible"
        :title="editingId ? '编辑套餐' : '新增套餐'"
        :confirm-loading="saving"
        destroy-on-close
        @ok="handleSave"
      >
        <a-form
          :model="form"
          layout="vertical"
        >
          <a-form-item
            label="套餐名称"
            required
          >
            <a-input
              v-model:value="form.packageName"
              placeholder="请输入套餐名称"
            />
          </a-form-item>
          <a-form-item
            label="套餐编码"
            required
          >
            <a-input
              v-model:value="form.packageCode"
              placeholder="请输入套餐编码"
            />
          </a-form-item>
          <a-form-item
            label="购买类型"
            required
          >
            <a-select
              v-model:value="form.purchaseType"
              :options="PURCHASE_TYPE_OPTIONS"
            />
          </a-form-item>
          <a-form-item label="价格（元）">
            <a-input-number
              v-model:value="formPriceYuan"
              :min="0"
              :precision="2"
              style="width:100%"
              placeholder="请输入价格"
            />
          </a-form-item>
          <a-form-item label="最大用户数">
            <a-input-number
              v-model:value="form.maxUsers"
              :min="1"
              style="width:100%"
            />
          </a-form-item>
          <a-form-item label="存储配额(GB)">
            <a-input-number
              v-model:value="form.storageQuota"
              :min="0"
              style="width:100%"
            />
          </a-form-item>
          <a-form-item label="API调用限制/月">
            <a-input-number
              v-model:value="form.apiCallLimit"
              :min="0"
              style="width:100%"
            />
          </a-form-item>
          <a-form-item label="状态">
            <a-switch
              v-model:checked="formStatus"
              checked-children="启用"
              un-checked-children="停用"
            />
          </a-form-item>
          <a-form-item label="描述">
            <a-textarea
              v-model:value="form.description"
              :rows="3"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
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
import { tenantPackageApi, type TenantPackageInfo } from '@/api/tenant'

defineOptions({ name: 'AdminTenantPackage' })

const PAGE_CONFIG_STORAGE_KEY = 'system-tenant-package-page-config'

// 页面配置的查询条件 / 功能按钮项（与 PageConfigPanel 的 prop 结构一致）
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// ═══ 枚举 ═══
/**
 * 状态下拉：全部 / 启用 / 停用
 * 后端 /list 无入参，本页为前端本地过滤，故用 -1 作为「全部」的哨兵值（不会发给后端）
 */
const STATUS_OPTIONS = [
  { label: '全部', value: -1 },
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
]
/** 购买类型值域（后端注释 monthly/yearly/perpetual，DB 实测 basic=monthly / professional=yearly / enterprise=perpetual） */
const PURCHASE_TYPE_OPTIONS = [
  { label: '按月', value: 'monthly' },
  { label: '按年', value: 'yearly' },
  { label: '永久', value: 'perpetual' },
]
/** 购买类型展示映射（用 Map 而非字面量对象做查表，规避原型链键陷阱） */
const PURCHASE_TYPE_MAP: Record<string, string> = Object.fromEntries(
  PURCHASE_TYPE_OPTIONS.map(o => [o.value, o.label]),
)

// ═══ 状态 ═══
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const editingId = ref<number | null>(null)
/** 后端一次返回全量（裸 Map{records,total}），本地过滤与分页都基于它 */
const allRows = ref<TenantPackageInfo[]>([])

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
// 后端无入参，暂无服务端筛选 → 以下两项仅在前端本地过滤
const searchForm = reactive({
  packageName: '' as string,
  status: -1 as number,
})

// ═══ 查询条件 / 功能按钮（页面配置） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'packageName', label: '套餐名称', visible: true },
  { key: 'status', label: '状态', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增套餐', enabled: true },
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

// ═══ 前端本地过滤 + 本地分页 ═══
/** 过滤后的全量数据（决定分页总数） */
const matchedList = computed(() => {
  const kw = searchForm.packageName.trim().toLowerCase()
  return allRows.value.filter((row) => {
    if (kw && !String(row.packageName || '').toLowerCase().includes(kw)) return false
    if (searchForm.status !== -1 && row.status !== searchForm.status) return false
    return true
  })
})
/**
 * 当前页要渲染的数据 —— 即表格的 data-source。
 * 命名保留 filteredList：数据源同时经过「本地过滤 + 本地分页」两道加工（后端不分页）。
 */
const filteredList = computed({
  get: () => {
    const start = (pagination.current - 1) * pagination.pageSize
    return matchedList.value.slice(start, start + pagination.pageSize)
  },
  // 只读视图（表格 :view-mode="true"）：组件正常不会回写；若回写则忽略，数据源始终由「本地过滤 + 本地分页」派生
  set: () => {},
})
// 总数/越界回退：过滤条件或数据变化时同步分页状态（只读视图，组件不会回写 dataSource）
watch(matchedList, (rows) => {
  pagination.total = rows.length
  const maxPage = Math.max(1, Math.ceil(rows.length / pagination.pageSize))
  if (pagination.current > maxPage) pagination.current = maxPage
}, { immediate: true })

// 序号列承载表头「列配置」齿轮；操作列为固定列
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 150, fixed: 'left' },
  { key: 'packageName', title: '套餐名称', type: 'input', width: 180 },
  { key: 'packageCode', title: '套餐编码', type: 'input', width: 140 },
  // 值域需映射为中文 → 必须用 slot（只写 slotName 而不写 type:'slot' 会静默渲染原值）
  { key: 'purchaseType', title: '购买类型', type: 'slot', slotName: 'purchaseTypeCell', width: 100 },
  { key: 'price', title: '价格', type: 'slot', slotName: 'priceCell', width: 110, align: 'right' },
  { key: 'maxUsers', title: '最大用户数', type: 'input', width: 110, align: 'right' },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90 },
]

// ═══ 弹窗表单（9 字段，字段清单与文档 §3.7 一致；若某项后端不落库仍保留展示） ═══
const form = ref<Partial<TenantPackageInfo>>({
  packageName: '',
  packageCode: '',
  purchaseType: 'monthly',
  maxUsers: 10,
  storageQuota: 10,
  apiCallLimit: 10000,
  status: 1,
  description: '',
})

/** 元 ↔ 分：写入用 Math.round 规避 99.99 * 100 = 9998.999… 的浮点误差 */
const formPriceYuan = computed({
  get: () => (form.value.price ?? 0) / 100,
  set: (val: number) => { form.value.price = Math.round(val * 100) },
})

/** 布尔 ↔ 1/0（状态开关） */
const formStatus = computed({
  get: () => form.value.status === 1,
  set: (val: boolean) => { form.value.status = val ? 1 : 0 },
})

/** 金额展示：null/undefined → '-'；0 是合法价格（免费档）不可并入 '-' */
function fmtPrice(price: number | null | undefined): string {
  if (price === null || price === undefined) return '-'
  return `¥${(price / 100).toFixed(2)}`
}

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const res = await tenantPackageApi.getList()
    allRows.value = res?.records || []
  } catch (e: any) {
    // 后端失败（如 403/500）时必须显式提示并清空，避免页面空白却无反馈
    console.error('[租户套餐] 加载列表失败', e)
    message.error(e?.response?.data?.message || e?.message || '加载套餐列表失败')
    allRows.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// ═══ 事件 ═══
/** 本地过滤为实时计算，此处回到第 1 页 */
function handleSearch() {
  pagination.current = 1
}
function handleReset() {
  searchForm.packageName = ''
  searchForm.status = -1
  pagination.current = 1
}
function handleRefresh() {
  fetchData()
}
function handlePageChange(page: number, pageSize: number) {
  if (pageSize !== pagination.pageSize) pagination.current = 1
  else pagination.current = page
  pagination.pageSize = pageSize
}

// ═══ 新增 / 编辑 ═══
function openForm(record?: Record<string, any>) {
  if (record) {
    editingId.value = record.id as number
    form.value = { ...(record as TenantPackageInfo) }
  } else {
    editingId.value = null
    form.value = {
      packageName: '',
      packageCode: '',
      purchaseType: 'monthly',
      maxUsers: 10,
      storageQuota: 10,
      apiCallLimit: 10000,
      status: 1,
      description: '',
    }
  }
  formVisible.value = true
}

async function handleSave() {
  if (!form.value.packageName || !form.value.packageCode) {
    message.warning('请填写套餐名称和编码')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await tenantPackageApi.update(editingId.value, form.value)
      message.success('更新成功')
    } else {
      await tenantPackageApi.create(form.value)
      message.success('创建成功')
    }
    formVisible.value = false
    await fetchData()
  } catch (e: any) {
    // 优先展示后端返回的原文（如「套餐不存在」），避免关键信息被通用文案吞掉
    message.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: Record<string, any>) {
  try {
    await tenantPackageApi.delete(record.id)
    message.success('删除成功')
    await fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '删除失败')
  }
}

function handleError(error: Error) {
  console.error('[租户套餐] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(fetchData)
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
</style>
