<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        使用统计（系统 → 模块管理 → 使用统计，菜单 62104）
        · 平台控制台页面：ql361 无对标 → 按 AWS SaaS Lens「每租户消耗度量」建模
        · 数据源：sys_module（模块目录）× sys_audit_log（活跃/调用）× sys_tenant_module（授权租户数）
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/使用统计开发文档.md
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <template #toolbar-left>
          <span class="window-tip">{{ windowTip }}</span>
        </template>

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

        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('window')">
                <span class="search-label">统计窗口</span>
                <a-select
                  v-model:value="searchForm.days"
                  size="small"
                  style="width: 140px"
                  :options="WINDOW_OPTIONS"
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

        <template #table>
          <div class="stat-cards">
            <a-row :gutter="16">
              <a-col
                v-for="card in statCards"
                :key="card.key"
                :span="6"
              >
                <a-card
                  :bordered="false"
                  size="small"
                >
                  <a-statistic
                    :title="card.title"
                    :value="card.value"
                    :suffix="card.suffix"
                    :formatter="(v: any) => `${v ?? 0}`"
                  >
                    <template #prefix>
                      <component
                        :is="card.icon"
                        :style="{ color: card.color }"
                      />
                    </template>
                  </a-statistic>
                </a-card>
              </a-col>
            </a-row>
          </div>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="system-module-usage-table-columns"
              global-config-key="system-module-usage-table-columns"
            >
              <template #rankCell="{ record }">
                <a-tag :color="record.rank <= 3 ? 'gold' : 'blue'">
                  #{{ record.rank }}
                </a-tag>
              </template>

              <template #moduleNameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="drillDown(record)"
                >{{ record.moduleName }}</a>
              </template>

              <template #usageRateCell="{ record }">
                <a-progress
                  :percent="record.usageRate"
                  size="small"
                  :status="record.usageRate > 80 ? 'exception' : 'active'"
                />
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
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  AppstoreOutlined,
  CheckCircleOutlined,
  TeamOutlined,
  DashboardOutlined,
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
import { moduleApi } from '@/api/admin'

defineOptions({ name: 'AdminModuleUsage' })

const router = useRouter()
const PAGE_CONFIG_STORAGE_KEY = 'system-module-usage-page-config'

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const WINDOW_OPTIONS = [
  { value: 7, label: '近 7 天' },
  { value: 30, label: '近 30 天' },
  { value: 90, label: '近 90 天' },
]

const loading = ref(false)
const tableData = ref<any[]>([])
const summary = ref<Record<string, any>>({})
const windowDays = ref(30)
const windowStart = ref('')

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

/** 全量模块统计（后端 /module/usage 不接收分页参数，返回的就是全部） */
const allRows = ref<any[]>([])

/**
 * 按当前页码把全量数据切成本页数据写进 tableData。
 *
 * 后端 `GET /module/usage?days=` 返回**全量**统计（total = records.length，无分页参数），
 * 分页必须在前端完成；此前 handlePageChange 只改页码、表格仍绑全量数组，
 * 表现为「翻页只有页码高亮变化、表格行不变」的假分页。
 */
function applyPage() {
  const start = (pagination.current - 1) * pagination.pageSize
  tableData.value = allRows.value.slice(start, start + pagination.pageSize)
}
const searchForm = reactive({ days: 30 })

const windowTip = computed(() =>
  windowStart.value ? `${windowDays.value} 天窗口（${windowStart.value} ~ 今天）` : '')

const statCards = computed(() => [
  { key: 'totalModules', title: '总模块数', value: Number(summary.value.totalModules ?? 0), suffix: '', icon: AppstoreOutlined, color: '#1890ff' },
  { key: 'enabledModules', title: '启用模块', value: Number(summary.value.enabledModules ?? 0), suffix: '', icon: CheckCircleOutlined, color: '#52c41a' },
  { key: 'grantedTenants', title: '已授权租户', value: Number(summary.value.grantedTenants ?? 0), suffix: '个', icon: TeamOutlined, color: '#722ed1' },
  { key: 'avgUsageRate', title: '平均使用率', value: Number(summary.value.avgUsageRate ?? 0), suffix: '%', icon: DashboardOutlined, color: '#faad14' },
])

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'window', label: '统计窗口', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
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

const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'rank', title: '排名', type: 'slot', slotName: 'rankCell', width: 80 },
  { key: 'moduleName', title: '模块名称', type: 'slot', slotName: 'moduleNameCell', width: 180 },
  { key: 'moduleCode', title: '模块编码', type: 'input', width: 130 },
  { key: 'tenantCount', title: '租户数量', type: 'input', width: 100 },
  { key: 'usageRate', title: '使用率', type: 'slot', slotName: 'usageRateCell', width: 180 },
  { key: 'monthlyActive', title: '活跃用户', type: 'input', width: 100 },
  { key: 'callCount', title: '调用次数', type: 'input', width: 110 },
]

async function fetchData() {
  loading.value = true
  try {
    const res: any = await moduleApi.usage(searchForm.days)
    allRows.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    applyPage()
    summary.value = res?.summary || {}
    windowDays.value = Number(res?.windowDays) || searchForm.days
    windowStart.value = res?.windowStart || ''
  } catch (error: any) {
    console.error('[使用统计] 加载失败', error)
    message.error(error?.message || '加载使用统计失败')
    allRows.value = []
    tableData.value = []
    pagination.total = 0
    summary.value = {}
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}
function handleReset() {
  searchForm.days = 30
  pagination.current = 1
  fetchData()
}
function handleRefresh() {
  fetchData()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  applyPage()
}

/** 下钻：跳到「模块列表」并带上模块名称/编码关键字过滤 */
function drillDown(record: any) {
  router.push({ path: '/admin/module/list', query: { keyword: record.moduleCode || '' } })
}

function handleError(error: Error) {
  console.error('[使用统计] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(fetchData)
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
.window-tip { font-size: 13px; color: #888; }
.stat-cards { padding: 12px 16px 0; background: #fff; flex-shrink: 0; }
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; padding-top: 12px; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
</style>
