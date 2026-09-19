<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        系统日志（系统 → 系统监控 → 系统日志，菜单 62204）
        · 平台控制台页面：ql361 无对标 → 按 SAP SAL「安全审计日志（Security Audit Log / SM20）」能力模型建模
        · 只读台账：查询区 4 项（模块 / 操作类型 / 操作人 / 日期范围）+ 统计卡 + 全字段台账表 + 详情抽屉
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/系统日志开发文档.md
        · 🔴 本轮修复（只改前端）：
          ① 响应解包缺陷（文档 §9.2-1）：拦截器已拆掉 Result 外层，旧代码读 res.data → 恒 undefined
             → 表格 / 2 个下拉 / 统计卡全部恒空（DB 实测 1.3 万行却一行不显示）。现直接读 res.records / res.total；
          ② 5 个列字段名与后端实体错配（文档 §9.2-8）：operationType→action、operatorName→username、
             ipAddress→operIp、operationTime→operTime，description 后端无此字段 → 删列；
          ③ 3 处插槽列缺陷（文档 §9.2-4/5）：status / costTime 缺 type:'slot'（直出裸 0/1）、
             action 列缺 slotName → 操作列空白；
          ④ 恒定报错提示（文档 §9.2-9）：message.error 写在 try/catch 之外的 bug；
          ⑤ 日期范围传 Dayjs 未格式化（文档 §9.2-10）；
          ⑥ 跨页列配置污染（文档 §9.2-14）：未传 storage-key → 落到默认键 product-unit-columns-config。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：导出 + 清空日志 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
            <!--
              危险按钮：显隐由页面配置控制，同时保留权限码兜底。
              权限码用后端真实校验的 log:oper:delete（旧代码写的 log:audit:delete 后端并不校验）
            -->
            <a-button
              v-if="isButtonEnabled('clear')"
              v-permission="'log:oper:delete'"
              danger
              size="small"
              :loading="clearing"
              @click="handleClearLogs"
            >
              <DeleteOutlined /> 清空日志
            </a-button>
          </a-space>
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

        <!-- ═══ 查询区（模块 / 操作类型 / 操作人 / 日期范围，横向 flex 自适应） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('module')">
                <span class="search-label">模块</span>
                <a-select
                  v-model:value="searchForm.module"
                  placeholder="全部模块"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  show-search
                  :filter-option="filterOption"
                  :options="moduleOptions"
                  @change="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('operationType')">
                <span class="search-label">操作类型</span>
                <a-select
                  v-model:value="searchForm.operationType"
                  placeholder="全部类型"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  show-search
                  :filter-option="filterOption"
                  :options="operationTypeOptions"
                  @change="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('operatorName')">
                <span class="search-label">操作人</span>
                <!-- 后端 operatorName 参数实际是 LIKE username（登录账号）→ 占位文案如实写「操作员账号」 -->
                <a-input
                  v-model:value="searchForm.operatorName"
                  placeholder="请输入操作员账号"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('dateRange')">
                <span class="search-label">操作时间</span>
                <a-range-picker
                  v-model:value="searchForm.dateRange"
                  size="small"
                  show-time
                  style="width: 340px"
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

        <!-- ═══ 数据表（统计卡 + 台账，列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <!--
              统计卡：后端 /api/log/* 无统计端点（文档 §10.1-㉒），故「成功 / 失败 / 慢请求」只能按
              **当前页**样本统计。为避免误读为全局口径（文档 §9.2-11），卡片标题显式标注「本页」；
              「日志总数」取后端返回的筛选后总条数，是真实全量口径。
            -->
            <a-row
              :gutter="12"
              class="stat-row"
            >
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="stat-card"
                >
                  <a-statistic
                    title="日志总数"
                    :value="pagination.total"
                    :value-style="{ color: '#1677ff' }"
                  />
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="stat-card"
                >
                  <a-statistic
                    title="本页成功"
                    :value="pageSuccessCount"
                    :value-style="{ color: '#52c41a' }"
                  />
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="stat-card"
                >
                  <a-statistic
                    title="本页失败"
                    :value="pageFailCount"
                    :value-style="{ color: '#ff4d4f' }"
                  />
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="stat-card"
                >
                  <a-statistic
                    title="本页慢请求(>1s)"
                    :value="pageSlowCount"
                    :value-style="{ color: '#faad14' }"
                  />
                </a-card>
              </a-col>
            </a-row>

            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :storage-key="TABLE_STORAGE_KEY"
              :global-config-key="TABLE_STORAGE_KEY"
            >
              <!-- 模块 + 操作类型 组合成可读「内容」并兼作详情入口（对标主列形态） -->
              <template #contentCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openDetail(record)"
                >{{ contentText(record) }}</a>
              </template>

              <!-- 操作类型：真实字段是 action（不是 operationType） -->
              <template #actionCell="{ record }">
                <span v-if="!record.__ghost">{{ actionText(record.action) }}</span>
              </template>

              <!--
                操作人：真实数据里 username（登录账号）常为 null，姓名由后端 realName 补齐
                （取 sys_user.real_name，空则回退 nickname）→ 优先展示姓名，回退账号
              -->
              <template #realNameCell="{ record }">
                <span v-if="!record.__ghost">{{ record.realName || record.username || '-' }}</span>
              </template>

              <!-- 操作时间：真实字段是 operTime（不是 operationTime），后端返回 ISO 串需格式化 -->
              <template #operTimeCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtDateTime(record.operTime) }}</span>
              </template>

              <!-- 耗时：⚠️ 必须 type:'slot' 才能自定义渲染（只写 slotName 会静默直出裸数字） -->
              <template #costTimeCell="{ record }">
                <span
                  v-if="!record.__ghost"
                  :class="costTimeClass(record.costTime)"
                >{{ fmtCost(record.costTime) }}</span>
              </template>

              <!-- 状态：⚠️ 必须 type:'slot' 才能渲染成彩色 tag（否则直出裸 0/1） -->
              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost && record.status != null"
                  :color="record.status === 0 ? 'success' : 'error'"
                >
                  {{ record.status === 0 ? '成功' : '失败' }}
                </a-tag>
                <span v-else-if="!record.__ghost">-</span>
              </template>

              <!-- 操作列：⚠️ slotName 必须为 actionCell（组件回落插槽名就是 actionCell） -->
              <template #actionBtnCell="{ record }">
                <a-button
                  v-if="!record.__ghost"
                  type="link"
                  size="small"
                  @click="openDetail(record)"
                >
                  详情
                </a-button>
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

      <!-- ═══ 日志详情抽屉（真实加载 GET /api/log/{id}） ═══ -->
      <a-drawer
        v-model:open="detailVisible"
        title="日志详情"
        width="760"
        destroy-on-close
      >
        <a-spin :spinning="detailLoading">
          <!-- 详情加载失败：如实提示，不做假数据兜底 -->
          <a-alert
            v-if="detailError"
            type="error"
            show-icon
            :message="detailError"
            class="detail-alert"
          />
          <a-descriptions
            v-else-if="detailItem"
            :column="2"
            size="small"
            bordered
          >
            <a-descriptions-item label="日志ID">
              {{ detailItem.id ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="模块">
              {{ detailItem.module || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="操作类型">
              {{ actionText(detailItem.action) }}
            </a-descriptions-item>
            <a-descriptions-item label="操作人">
              {{ detailItem.realName || detailItem.username || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="操作员账号">
              {{ detailItem.username || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="操作人ID">
              {{ detailItem.userId ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="操作时间">
              {{ fmtDateTime(detailItem.operTime) }}
            </a-descriptions-item>
            <a-descriptions-item label="耗时">
              {{ fmtCost(detailItem.costTime) }}
            </a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag
                v-if="detailItem.status != null"
                :color="detailItem.status === 0 ? 'success' : 'error'"
              >
                {{ detailItem.status === 0 ? '成功' : '失败' }}
              </a-tag>
              <span v-else>-</span>
            </a-descriptions-item>
            <a-descriptions-item label="IP地址">
              {{ detailItem.operIp || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="操作地点">
              {{ detailItem.operLocation || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="请求方式">
              <a-tag
                v-if="detailItem.requestMethod"
                :color="methodColor(detailItem.requestMethod)"
              >
                {{ detailItem.requestMethod }}
              </a-tag>
              <span v-else>-</span>
            </a-descriptions-item>
            <a-descriptions-item
              label="请求URL"
              :span="2"
            >
              {{ detailItem.requestUrl || '-' }}
            </a-descriptions-item>
            <a-descriptions-item
              label="方法名"
              :span="2"
            >
              {{ detailItem.method || '-' }}
            </a-descriptions-item>
            <a-descriptions-item
              label="错误信息"
              :span="2"
            >
              {{ detailItem.errorMsg || '-' }}
            </a-descriptions-item>
          </a-descriptions>

          <template v-if="detailItem">
            <a-divider>请求/响应详情</a-divider>
            <a-tabs>
              <a-tab-pane
                key="request"
                tab="请求参数"
              >
                <pre class="json-content">{{ prettyJson(detailItem.requestParams) }}</pre>
              </a-tab-pane>
              <a-tab-pane
                key="response"
                tab="响应结果"
              >
                <pre class="json-content">{{ prettyJson(detailItem.responseResult) }}</pre>
              </a-tab-pane>
              <a-tab-pane
                key="diff"
                tab="变更对比"
              >
                <pre class="json-content">{{ prettyJson(detailItem.diffData) }}</pre>
              </a-tab-pane>
            </a-tabs>
          </template>
        </a-spin>
      </a-drawer>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  DeleteOutlined,
  DownloadOutlined,
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
import { logApi, type OperLogRow } from '@/api/log'

defineOptions({ name: 'SystemLog' })

// 存储键：必须传独立键，否则 BillDetailTable 会落到默认键 product-unit-columns-config，
// 与商品单位表等页面共用同一份列配置（文档 §9.2-14 跨页污染）
const TABLE_STORAGE_KEY = 'system-log-table-columns'
const PAGE_CONFIG_STORAGE_KEY = 'system-log-page-config'

/** 清理时保留的天数：后端 days < 7 直接拒绝，默认 90 */
const RETENTION_DAYS = 90

// 页面配置的查询条件 / 功能按钮项（与 PageConfigPanel 的 prop 结构一致）
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// ═══ 字典（与「设置 → 操作日志」页保持同一套取值口径，避免同系统两处译名不一致） ═══
/** 后端 sys_oper_log.action 值域：英文动作码 + 少量中文动作（如「顶替下线」，未知值原样展示） */
const ACTION_MAP: Record<string, string> = {
  CREATE: '新增',
  UPDATE: '修改',
  DELETE: '删除',
  QUERY: '查询',
  EXPORT: '导出',
  IMPORT: '导入',
  OTHER: '其它',
}

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const clearing = ref(false)
const tableData = ref<OperLogRow[]>([])

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({
  module: undefined as string | undefined,
  operationType: undefined as string | undefined,
  operatorName: '' as string,
  // a-range-picker（show-time）的值为 [Dayjs, Dayjs]，提交前须 format 成后端可解析的字符串
  dateRange: undefined as [Dayjs, Dayjs] | undefined,
})

const moduleOptions = ref<{ label: string; value: string }[]>([])
const operationTypeOptions = ref<{ label: string; value: string }[]>([])

// ═══ 统计卡（后端无统计端点 → 后三张按当前页样本统计，标题已标注「本页」） ═══
const pageSuccessCount = computed(() => tableData.value.filter(r => r.status === 0).length)
const pageFailCount = computed(() => tableData.value.filter(r => r.status != null && r.status !== 0).length)
const pageSlowCount = computed(() => tableData.value.filter(r => (r.costTime ?? 0) > 1000).length)

// ═══ 查询条件 / 功能按钮（页面配置） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'module', label: '模块', visible: true },
  { key: 'operationType', label: '操作类型', visible: true },
  { key: 'operatorName', label: '操作人', visible: true },
  { key: 'dateRange', label: '操作时间', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'export', label: '导出', enabled: true },
  { key: 'clear', label: '清空日志', enabled: true },
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

// 序号列承载表头「列配置」齿轮；操作列固定左侧。
// ⚠️ 操作列的 key 用 rowAction，避免与数据字段 action（操作类型）撞名。
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'rowAction', title: '操作', type: 'action', slotName: 'actionBtnCell', width: 80, fixed: 'left' },
  { key: 'content', title: '内容', type: 'slot', slotName: 'contentCell', width: 260 },
  { key: 'module', title: '模块', width: 140 },
  // ⚠️ 必须写 type:'slot'：只写 slotName 会静默直出原值（如裸 0/1）
  { key: 'action', title: '操作类型', type: 'slot', slotName: 'actionCell', width: 110 },
  { key: 'realName', title: '操作人', type: 'slot', slotName: 'realNameCell', width: 130 },
  { key: 'operTime', title: '操作时间', type: 'slot', slotName: 'operTimeCell', width: 170 },
  { key: 'costTime', title: '耗时', type: 'slot', slotName: 'costTimeCell', width: 100 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90 },
  // ── 以下为技术列：默认隐藏 ──
  { key: 'username', title: '操作员账号', width: 140, defaultHidden: true },
  { key: 'operIp', title: 'IP地址', width: 140, defaultHidden: true },
  { key: 'operLocation', title: '操作地点', width: 130, defaultHidden: true },
  { key: 'requestMethod', title: '请求方式', width: 100, defaultHidden: true },
  { key: 'requestUrl', title: '请求URL', width: 260, defaultHidden: true },
  { key: 'method', title: '方法名', width: 200, defaultHidden: true },
  { key: 'errorMsg', title: '错误信息', width: 220, defaultHidden: true },
  { key: 'diffData', title: '变更对比', width: 200, defaultHidden: true },
  { key: 'userId', title: '操作人ID', width: 130, defaultHidden: true },
  { key: 'id', title: '日志ID', width: 190, defaultHidden: true },
]

// ═══ 数据加载 ═══
/** 查询条件 → 请求参数（日期格式与后端 parseDateTime 支持的 yyyy-MM-dd HH:mm:ss 一致） */
function buildQueryParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (searchForm.module) params.module = searchForm.module
  // 后端参数名叫 operationType，内部匹配的实体字段是 action
  if (searchForm.operationType) params.operationType = searchForm.operationType
  if (searchForm.operatorName) params.operatorName = searchForm.operatorName
  if (searchForm.dateRange?.[0]) params.startDate = searchForm.dateRange[0].format('YYYY-MM-DD HH:mm:ss')
  if (searchForm.dateRange?.[1]) params.endDate = searchForm.dateRange[1].format('YYYY-MM-DD HH:mm:ss')
  return params
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await logApi.getPage(buildQueryParams())
    // ⚠️ utils/request.ts 的响应拦截器已拆掉 Result 外层 → res 就是 MyBatis-Plus Page：
    //    { records, total, size, current, pages }，**没有** data 属性（旧代码读 res.data 恒 undefined）
    tableData.value = res?.records || []
    // ⚠️ total 被后端 Jackson 序列化为字符串（长整型防精度丢失），必须 Number 归一
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[系统日志] 加载日志列表失败', error)
    message.error(error?.message || '加载日志列表失败')
    // 失败即清空，不做假数据兜底
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 模块下拉值域：后端对 module 去重返回 string[]（拦截器已拆包，res 不是 { data: [...] }） */
async function fetchModules() {
  try {
    const res: any = await logApi.getModules()
    moduleOptions.value = Array.isArray(res) ? res.filter(Boolean).map(m => ({ label: m, value: m })) : []
  } catch (error: any) {
    // ⚠️ 旧代码把 message.error 写在 try/catch 之外 → 接口成功也必弹一次（文档 §9.2-9）
    console.error('[系统日志] 加载模块下拉失败', error)
    message.error(error?.message || '加载模块列表失败')
    moduleOptions.value = []
  }
}

/** 操作类型下拉值域：后端对 action 去重返回 string[] */
async function fetchOperationTypes() {
  try {
    const res: any = await logApi.getOperationTypes()
    operationTypeOptions.value = Array.isArray(res)
      ? res.filter(Boolean).map(t => ({ label: ACTION_MAP[t] ? `${ACTION_MAP[t]}（${t}）` : t, value: t }))
      : []
  } catch (error: any) {
    console.error('[系统日志] 加载操作类型下拉失败', error)
    message.error(error?.message || '加载操作类型列表失败')
    operationTypeOptions.value = []
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  fetchData()
}
function handleReset() {
  searchForm.module = undefined
  searchForm.operationType = undefined
  searchForm.operatorName = ''
  searchForm.dateRange = undefined
  pagination.current = 1
  fetchData()
}
function handleRefresh() {
  fetchData()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}
/** 下拉搜索（模块名 / 操作类型都是自由文本，show-search 需自定义过滤） */
function filterOption(input: string, option: any) {
  return String(option?.label || '').toLowerCase().includes(String(input || '').toLowerCase())
}

// ═══ 清理历史日志 ═══
/**
 * 后端 DELETE /api/log/clear 的真实语义是「删除 oper_time 早于保留天数的记录」，**不是清空全部**
 * （文档 §9.2-12）。故先用 /log/clear/preview 取回将删除条数，再在确认框里如实告知，
 * 不再沿用旧文案「确定要清空所有操作日志吗」。
 */
async function handleClearLogs() {
  clearing.value = true
  let willDelete: number
  try {
    const res: any = await logApi.previewClear(RETENTION_DAYS)
    willDelete = Number(res) || 0
  } catch (error: any) {
    console.error('[系统日志] 清理预览失败', error)
    message.error(error?.message || '清理预览失败')
    return
  } finally {
    clearing.value = false
  }

  Modal.confirm({
    title: '确认清理历史日志',
    content: `将删除操作时间早于 ${RETENTION_DAYS} 天前的日志共 ${willDelete} 条，此操作不可恢复。`,
    okType: 'danger',
    async onOk() {
      clearing.value = true
      try {
        const res: any = await logApi.clearLogs(RETENTION_DAYS)
        message.success(`已清理 ${Number(res) || 0} 条历史日志`)
        pagination.current = 1
        fetchData()
      } catch (error: any) {
        console.error('[系统日志] 清理历史日志失败', error)
        message.error(error?.message || '清理失败')
      } finally {
        clearing.value = false
      }
    },
  })
}

// ═══ 导出（真实调用 GET /api/log/export，后端返回 CSV + UTF-8 BOM） ═══
async function handleExport() {
  exporting.value = true
  try {
    const params = buildQueryParams()
    // 导出走全量（后端自身限制最多 10000 条），不带分页参数
    delete params.pageNum
    delete params.pageSize
    const blob: any = await logApi.exportLogs(params)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    // 后端实际产物是 CSV（文档 §9.2-18：旧代码命名为 .xlsx 与内容不符）
    link.download = `系统日志_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    link.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    console.error('[系统日志] 导出日志失败', error)
    message.error(error?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 详情抽屉（真实调用 GET /api/log/{id}，列表接口裁剪的两个大字段由详情接口返回） ═══
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailItem = ref<any>(null)

async function openDetail(record: any) {
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detailItem.value = null
  try {
    const res: any = await logApi.getById(record.id)
    if (!res || !res.id) {
      detailError.value = '详情接口未返回该日志'
      return
    }
    detailItem.value = res
  } catch (error: any) {
    console.error('[系统日志] 加载日志详情失败', error)
    detailError.value = error?.message || '加载日志详情失败'
    message.error(detailError.value)
  } finally {
    detailLoading.value = false
  }
}

// ═══ 展示辅助 ═══
function actionText(action?: string | null): string {
  if (!action) return '-'
  return ACTION_MAP[action] || action
}
/** 「内容」列：模块 · 操作类型（后端无 description 字段，按现有字段如实组合成可读文案） */
function contentText(record: OperLogRow): string {
  const action = record.action ? (ACTION_MAP[record.action] || record.action) : ''
  if (record.module && action) return `${record.module} · ${action}`
  return record.module || action || '-'
}
function fmtDateTime(value?: string | null): string {
  if (!value) return '-'
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? String(value) : dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}
/** 耗时：>1000ms 红色、>500ms 橙色，空值显示 '-' */
function fmtCost(cost?: number | null): string {
  if (cost == null) return '-'
  return cost > 1000 ? `${(cost / 1000).toFixed(2)}s` : `${cost}ms`
}
function costTimeClass(cost?: number | null): string {
  if (cost == null) return ''
  if (cost > 1000) return 'cost-slow'
  if (cost > 500) return 'cost-warn'
  return ''
}
function methodColor(method: string): string {
  const colors: Record<string, string> = {
    GET: 'green',
    POST: 'blue',
    PUT: 'orange',
    PATCH: 'purple',
    DELETE: 'red',
  }
  return colors[method] || 'default'
}
/** JSON 串美化；不是合法 JSON 时原样返回 */
function prettyJson(val: any): string {
  if (val == null || val === '') return '无数据'
  if (typeof val !== 'string') return JSON.stringify(val, null, 2)
  try {
    return JSON.stringify(JSON.parse(val), null, 2)
  } catch {
    return val
  }
}

// ═══ 快捷键：F5 / Ctrl+R 刷新（本页原有能力，予以保留） ═══
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    fetchData()
  }
}

function handleError(error: Error) {
  console.error('[系统日志] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchData()
  fetchModules()
  fetchOperationTypes()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
/* 统计卡固定高度，不参与表格的伸缩 */
.stat-row { flex-shrink: 0; padding: 12px 16px 0; }
.stat-card { background: #fafafa; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
/* 耗时着色：>1s 红、>500ms 橙 */
.cost-slow { color: #ff4d4f; }
.cost-warn { color: #faad14; }
.detail-alert { margin-bottom: 8px; }
.json-content {
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 12px;
  max-height: 300px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
