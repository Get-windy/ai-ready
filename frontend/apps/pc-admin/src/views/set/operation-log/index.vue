<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        操作日志（设置 → 账套操作 → 操作日志，菜单 80630 / set:operation-log）
        对标 ql361「设置 → 账套操作 → 操作日志」（实测形态）：
          · 双 Tab：系统日志 | 登录日志（每个 Tab 独立列定义 + 独立查询条件 + 独立列配置 storage-key）
          · 工具栏：刷新 | 打印(F8) | 导出（「清理历史日志」对标无此按钮，默认关闭，可在页面配置中开启）
          · 数据源：系统日志 = sys_oper_log（13400+ 行）；登录日志 = sys_login_log（6200+ 行）
          · 分页：经典形态，每页 20 行（与对标一致）
        ⚠️ 取数：request.ts 拦截器已拆包，页面直接读 res.records / res.total，
           绝不可再取 res.data（历史 P0：写成 result.data.records → TypeError 被 catch 吞成
           「查询失败」→ 列表恒空）。详见 api/log.ts 顶部注释。
        规格书：docs/Yh-Spec/手动整理对标开发文档/设置模块/操作日志开发文档.md
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏右侧：刷新 / 打印(F8) / 导出（对标三个按钮） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip
              v-if="btnEnabled('pageConfig')"
              title="页面配置"
              placement="bottom"
            >
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <!-- 「清理历史日志」：对标无此按钮，默认关闭；开启后按保留天数清理（受 log:oper:delete 保护） -->
            <a-button
              v-if="activeTab === 'oper' && btnEnabled('clear')"
              v-permission="'log:oper:delete'"
              size="small"
              danger
              @click="handleClear"
            >
              <DeleteOutlined /> 清理历史日志
            </a-button>
            <a-button
              v-if="btnEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="fetchList"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="btnEnabled('print')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="btnEnabled('export')"
              v-permission="activeTab === 'oper' ? 'log:oper:export' : 'log:login:export'"
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区：横向自适应网格（禁止纵向单列） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div
              ref="gridRef"
              class="search-grid"
            >
              <!-- ── 系统日志 Tab：操作日期(起/止) · 模块 · 操作类型 · 操作员 · 状态 ── -->
              <template v-if="activeTab === 'oper'">
                <div
                  v-if="fieldVisible('operDateStart')"
                  class="search-field-item"
                >
                  <a-date-picker
                    v-model:value="operForm.startDate"
                    size="small"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                    placeholder="操作日期(起)"
                    @change="handleSearch"
                  />
                </div>
                <div
                  v-if="fieldVisible('operDateEnd')"
                  class="search-field-item"
                >
                  <a-date-picker
                    v-model:value="operForm.endDate"
                    size="small"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                    placeholder="操作日期(止)"
                    @change="handleSearch"
                  />
                </div>
                <div
                  v-if="fieldVisible('module')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="operForm.module"
                    size="small"
                    style="width: 100%"
                    placeholder="模块"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :options="moduleOptions"
                    @change="handleSearch"
                  />
                </div>
                <div
                  v-if="fieldVisible('operationType')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="operForm.operationType"
                    size="small"
                    style="width: 100%"
                    placeholder="操作类型"
                    allow-clear
                    :options="operationTypeOptions"
                    @change="handleSearch"
                  />
                </div>
                <div
                  v-if="fieldVisible('operatorName')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="operForm.operatorName"
                    size="small"
                    placeholder="操作员"
                    allow-clear
                    @press-enter="handleSearch"
                  />
                </div>
                <div
                  v-if="fieldVisible('status')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="operForm.status"
                    size="small"
                    style="width: 100%"
                    placeholder="状态"
                    allow-clear
                    :options="STATUS_OPTIONS"
                    @change="handleSearch"
                  />
                </div>
              </template>

              <!-- ── 登录日志 Tab：登录日期(起*)/(止*) 为必填 · 登录类型 · 操作员 · 登录结果 ── -->
              <template v-else>
                <div
                  v-if="fieldVisible('loginDateStart')"
                  class="search-field-item"
                >
                  <a-date-picker
                    v-model:value="loginForm.startDate"
                    size="small"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                    placeholder="登录日期(起)*"
                    @change="handleLoginDateChange"
                  />
                </div>
                <div
                  v-if="fieldVisible('loginDateEnd')"
                  class="search-field-item"
                >
                  <a-date-picker
                    v-model:value="loginForm.endDate"
                    size="small"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                    placeholder="登录日期(止)*"
                    @change="handleLoginDateChange"
                  />
                </div>
                <div
                  v-if="fieldVisible('loginType')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="loginForm.loginType"
                    size="small"
                    style="width: 100%"
                    placeholder="登录类型"
                    allow-clear
                    :options="LOGIN_TYPE_OPTIONS"
                    @change="handleSearch"
                  />
                </div>
                <div
                  v-if="fieldVisible('username')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="loginForm.username"
                    size="small"
                    placeholder="操作员"
                    allow-clear
                    @press-enter="handleSearch"
                  />
                </div>
                <div
                  v-if="fieldVisible('loginResult')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="loginForm.loginResult"
                    size="small"
                    style="width: 100%"
                    placeholder="登录结果"
                    allow-clear
                    :options="LOGIN_RESULT_OPTIONS"
                    @change="handleSearch"
                  />
                </div>
              </template>

              <div
                ref="actionRef"
                class="search-action-group"
                :style="{ gridColumn: 'span ' + actionSpan }"
              >
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
          </div>
        </template>

        <!-- ═══ 数据表：每个 Tab 独立列定义 + 独立 storage-key（切 Tab 必须换 key） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-if="activeTab === 'oper'"
              v-model:data-source="operRows"
              :columns="operColumns"
              :loading="loading"
              :view-mode="true"
              :storage-key="OPER_COLUMNS_KEY"
              :global-config-key="OPER_COLUMNS_KEY"
              row-key="id"
            >
              <!-- 时间 -->
              <template #operTimeCell="{ record }">
                {{ fmtDateTime(record.operTime) }}
              </template>
              <!-- 操作员（登录账号） -->
              <template #operUserCell="{ record }">
                {{ record.username || '-' }}
              </template>
              <!-- 姓名（后端由 sys_user.real_name 补齐，空则回退 nickname） -->
              <template #operRealNameCell="{ record }">
                {{ record.realName || '-' }}
              </template>
              <!-- 内容（对标核心列）：由 模块 + 操作类型 组合成可读文案；点击打开详情 -->
              <template #operContentCell="{ record }">
                <a
                  class="cell-link"
                  @click="handleDetail(record)"
                >{{ operContent(record) }}</a>
              </template>
              <!-- 状态 -->
              <template #operStatusCell="{ record }">
                <a-tag :color="record.status === 0 ? 'success' : 'error'">
                  {{ record.status === 0 ? '成功' : '失败' }}
                </a-tag>
              </template>
              <!-- 耗时：>1000ms 换算为秒，避免「3500ms」这类观感问题 -->
              <template #operCostCell="{ record }">
                {{ fmtCost(record.costTime) }}
              </template>
            </BillDetailTable>

            <BillDetailTable
              v-else
              v-model:data-source="loginRows"
              :columns="loginColumns"
              :loading="loading"
              :view-mode="true"
              :storage-key="LOGIN_COLUMNS_KEY"
              :global-config-key="LOGIN_COLUMNS_KEY"
              row-key="id"
            >
              <template #loginTimeCell="{ record }">
                {{ fmtDateTime(record.loginTime) }}
              </template>
              <template #loginUserCell="{ record }">
                {{ record.username || '-' }}
              </template>
              <template #loginRealNameCell="{ record }">
                {{ record.realName || '-' }}
              </template>
              <!-- 登录类型：对标实测内容形态「从【电脑端】登录,IP<...>」（设备端 + IP） -->
              <template #loginTypeCell="{ record }">
                {{ loginTypeContent(record) }}
              </template>
              <template #loginResultCell="{ record }">
                <a-tag :color="record.loginResult === 0 ? 'success' : 'error'">
                  {{ record.loginResult === 0 ? '成功' : '失败' }}
                </a-tag>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 底部：经典分页（首页/上页/第(x/y)页/下页/尾页/跳转/共 N 条记录/每页 N 行） ═══ -->
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
    </PageContainer>

    <!-- ═══ 页面配置（查询条件显隐 + 功能按钮启用；每个 Tab 独立 storage-key） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonsConfig"
      :default-query-fields-config="DEFAULT_QUERY_FIELDS"
      :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
      :hide-print-config="true"
      :storage-key="pageConfigKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 系统日志详情（字段来自 GET /log/{id}，列表接口已裁剪两个大字段） ═══ -->
    <a-modal
      v-model:open="detailVisible"
      title="操作日志详情"
      :width="760"
      :footer="null"
    >
      <a-spin :spinning="detailLoading">
        <a-descriptions
          :column="2"
          bordered
          size="small"
        >
          <a-descriptions-item
            label="日志ID"
            :span="2"
          >
            {{ detail?.id || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="操作时间">
            {{ fmtDateTime(detail?.operTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="操作员">
            {{ detail?.username || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="姓名">
            {{ detail?.realName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="模块">
            {{ detail?.module || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="操作类型">
            {{ actionText(detail?.action) }}
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="detail?.status === 0 ? 'success' : 'error'">
              {{ detail?.status === 0 ? '成功' : '失败' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="IP地址">
            {{ detail?.operIp || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="耗时">
            {{ fmtCost(detail?.costTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="请求方式">
            {{ detail?.requestMethod || '-' }}
          </a-descriptions-item>
          <a-descriptions-item
            label="请求URL"
            :span="2"
          >
            {{ detail?.requestUrl || '-' }}
          </a-descriptions-item>
          <a-descriptions-item
            v-if="detail?.errorMsg"
            label="错误信息"
            :span="2"
          >
            {{ detail?.errorMsg }}
          </a-descriptions-item>
          <a-descriptions-item
            label="请求参数"
            :span="2"
          >
            <pre class="detail-pre">{{ maskSensitive(detail?.requestParams) }}</pre>
          </a-descriptions-item>
          <a-descriptions-item
            label="响应结果"
            :span="2"
          >
            <pre class="detail-pre">{{ maskSensitive(detail?.responseResult) }}</pre>
          </a-descriptions-item>
        </a-descriptions>
      </a-spin>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  SettingOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { logApi, type OperLogRow, type LoginLogRow, type OperLogQuery, type LoginLogQuery } from '@/api/log'

defineOptions({ name: 'SetOperationLog' })

// ═══════════════════════════ 常量与字典 ═══════════════════════════

/** 页内 Tab（对标 ql361 实测逐字：系统日志 / 登录日志） */
const TABS = [
  { key: 'oper', label: '系统日志' },
  { key: 'login', label: '登录日志' },
]

/** 操作类型字典（后端 sys_oper_log.action 的实际值域，SQL 实测 8 个值） */
const ACTION_MAP: Record<string, string> = {
  CREATE: '新增',
  UPDATE: '修改',
  DELETE: '删除',
  QUERY: '查询',
  EXPORT: '导出',
  IMPORT: '导入',
  OTHER: '其它',
}

/** 登录方式字典（后端 sys_login_log.login_type） */
const LOGIN_TYPE_MAP: Record<number, string> = {
  1: '账号密码登录',
  2: '短信验证码登录',
  3: '第三方登录',
}

/** 登录端类型字典（sys_login_log.device_type）；对标「登录类型」列内容含「从【电脑端】登录」 */
const DEVICE_MAP: Record<string, string> = {
  PC: '电脑端',
  Mobile: '手机端',
  Tablet: '平板端',
  Unknown: '未知设备',
}

const STATUS_OPTIONS = [
  { label: '成功', value: 0 },
  { label: '失败', value: 1 },
]
const LOGIN_TYPE_OPTIONS = Object.entries(LOGIN_TYPE_MAP).map(([value, label]) => ({ label, value: Number(value) }))
const LOGIN_RESULT_OPTIONS = [
  { label: '成功', value: 0 },
  { label: '失败', value: 1 },
]

/** 「清理历史日志」的保留天数：与后端 /clear 默认值一致（后端 <7 天直接拒绝） */
const CLEAR_RETAIN_DAYS = 90

/**
 * 两个 Tab 的列配置 storage-key（必须不同值：切 Tab 即换 key，列显隐互不影响）。
 * 页面配置的 storage-key 另取 `set-operation-log-{tab}-page-config`（见 pageConfigKey），
 * 与列配置 key 不同值；global-config-key 与 storage-key 同值。
 *
 * 注：这里用「两个 BillDetailTable + v-if」（而非一个组件换 columns），
 * 是因为组件级已知陷阱 —— 单实例切 views 时列显隐状态会串用（见
 * 《BillDetailTable 多视图 Tab 状态》）；dms/payment 等多 Tab 页面同样采用 v-if 分实例。
 */
const OPER_COLUMNS_KEY = 'set-operation-log-columns-oper'
const LOGIN_COLUMNS_KEY = 'set-operation-log-columns-login'

// ═══════════════════════════ 状态 ═══════════════════════════

const activeTab = ref<'oper' | 'login'>('oper')
const loading = ref(false)
const exporting = ref(false)
const operRows = ref<OperLogRow[]>([])
const loginRows = ref<LoginLogRow[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// 查询区横向网格：动作组按内容宽度自动算 span
const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

/** 系统日志查询条件（日期入参后端兼容 yyyy-MM-dd 与 yyyy-MM-dd HH:mm:ss） */
const operForm = reactive({
  startDate: undefined as string | undefined,
  endDate: undefined as string | undefined,
  module: undefined as string | undefined,
  operationType: undefined as string | undefined,
  operatorName: '' as string,
  status: undefined as number | undefined,
})

/** 登录日志查询条件（对标：登录日期起止为必填 → 默认最近 7 天，开箱即有数据） */
const loginForm = reactive({
  startDate: dayjs().subtract(6, 'day').format('YYYY-MM-DD') as string | undefined,
  endDate: dayjs().format('YYYY-MM-DD') as string | undefined,
  loginType: undefined as number | undefined,
  username: '' as string,
  loginResult: undefined as number | undefined,
})

/** 模块 / 操作类型下拉值域（数据驱动，后端对 sys_oper_log 去重） */
const moduleOptions = ref<{ label: string; value: string }[]>([])
const operationTypeOptions = ref<{ label: string; value: string }[]>([])

// ═══════════════════════════ 数据表列定义 ═══════════════════════════
//
// 列策略（对齐《操作日志开发文档》§3.5 的关键结论）：
//   默认显示 = 对标 ql361 的 4 列（时间 · 操作员 · 姓名 · 内容 / 登录类型），
//   本系统更丰富的技术列（模块/状态/IP/耗时/…）全部 defaultHidden，
//   需要排查问题时由用户通过表头齿轮自行勾选 → 默认视图与对标一致，能力不减。

/** 系统日志列 */
const operColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  {
    key: 'operTime',
    title: '时间',
    type: 'slot',
    slotName: 'operTimeCell',
    width: 170,
    fixed: 'left',
    sortable: true,
    sorter: (a: any, b: any) => String(a.operTime || '').localeCompare(String(b.operTime || '')),
  },
  { key: 'username', title: '操作员', type: 'slot', slotName: 'operUserCell', width: 150, sortable: true },
  { key: 'realName', title: '姓名', type: 'slot', slotName: 'operRealNameCell', width: 130 },
  // 「内容」不是数据库列，而是由 模块 + 操作类型 组合的可读文案（对标核心列）；
  // 点击该列即打开详情（对标行内无操作按钮，故用主列链接承载，避免多出一列「操作」）
  { key: 'content', title: '内容', type: 'slot', slotName: 'operContentCell', width: 300 },
  // ── 以下为技术列：默认隐藏 ──
  { key: 'module', title: '模块', width: 140, defaultHidden: true },
  { key: 'action', title: '操作类型', width: 110, defaultHidden: true, formatter: (v: any) => actionText(v) },
  { key: 'status', title: '状态', type: 'slot', slotName: 'operStatusCell', width: 90, defaultHidden: true },
  { key: 'costTime', title: '耗时', type: 'slot', slotName: 'operCostCell', width: 90, defaultHidden: true },
  { key: 'operIp', title: 'IP地址', width: 140, defaultHidden: true },
  { key: 'operLocation', title: '操作地点', width: 130, defaultHidden: true },
  { key: 'requestMethod', title: '请求方式', width: 100, defaultHidden: true },
  { key: 'requestUrl', title: '请求URL', width: 260, defaultHidden: true },
  { key: 'errorMsg', title: '错误信息', width: 220, defaultHidden: true },
  { key: 'diffData', title: '变更对比', width: 200, defaultHidden: true },
]

/** 登录日志列 */
const loginColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  {
    key: 'loginTime',
    title: '时间',
    type: 'slot',
    slotName: 'loginTimeCell',
    width: 170,
    fixed: 'left',
    sortable: true,
    sorter: (a: any, b: any) => String(a.loginTime || '').localeCompare(String(b.loginTime || '')),
  },
  { key: 'username', title: '操作员', type: 'slot', slotName: 'loginUserCell', width: 160, sortable: true },
  { key: 'realName', title: '姓名', type: 'slot', slotName: 'loginRealNameCell', width: 130 },
  // 对标「登录类型」列实测内容形态：从【电脑端】登录,IP<36.142.195.162>
  { key: 'loginType', title: '登录类型', type: 'slot', slotName: 'loginTypeCell', width: 320 },
  // ── 以下为技术列：默认隐藏 ──
  { key: 'loginTypeName', title: '登录方式', width: 130, defaultHidden: true, formatter: (_v: any, r: any) => LOGIN_TYPE_MAP[r.loginType as number] || '未知' },
  { key: 'loginResult', title: '登录结果', type: 'slot', slotName: 'loginResultCell', width: 100, defaultHidden: true },
  { key: 'failReason', title: '失败原因', width: 180, defaultHidden: true },
  { key: 'loginIp', title: '登录IP', width: 150, defaultHidden: true },
  { key: 'loginLocation', title: '登录地点', width: 140, defaultHidden: true },
  { key: 'browser', title: '浏览器', width: 130, defaultHidden: true },
  { key: 'os', title: '操作系统', width: 130, defaultHidden: true },
  { key: 'deviceType', title: '设备类型', width: 110, defaultHidden: true },
  { key: 'logoutTime', title: '退出时间', width: 170, defaultHidden: true, formatter: (v: any) => fmtDateTime(v) },
  { key: 'remark', title: '备注', width: 160, defaultHidden: true },
]

// ═══════════════════════════ 页面配置（查询条件 + 功能按钮） ═══════════════════════════

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const OPER_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'operDateStart', label: '操作日期(起)', visible: true },
  { key: 'operDateEnd', label: '操作日期(止)', visible: true },
  { key: 'module', label: '模块', visible: true },
  { key: 'operationType', label: '操作类型', visible: true },
  { key: 'operatorName', label: '操作员', visible: true },
  { key: 'status', label: '状态', visible: false },
]
const LOGIN_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'loginDateStart', label: '登录日期(起)', visible: true },
  { key: 'loginDateEnd', label: '登录日期(止)', visible: true },
  { key: 'loginType', label: '登录类型', visible: true },
  { key: 'username', label: '操作员', visible: true },
  { key: 'loginResult', label: '登录结果', visible: false },
]

// 对标工具栏只有「刷新 / 打印(F8) / 导出」，故「清理历史日志」默认关闭（开启后受 log:oper:delete 保护）
const OPER_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'pageConfig', label: '页面配置', enabled: true },
  { key: 'clear', label: '清理历史日志', enabled: false },
]
const LOGIN_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'pageConfig', label: '页面配置', enabled: true },
]

const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>([])
const functionButtonsConfig = ref<FunctionButtonSetting[]>([])

/** 「恢复默认」的出厂基准（页面把「当前配置」传给 queryFieldsConfig，必须另给出厂值否则恢复默认无效） */
const DEFAULT_QUERY_FIELDS = computed(() =>
  activeTab.value === 'oper' ? OPER_QUERY_FIELDS : LOGIN_QUERY_FIELDS)
const DEFAULT_FUNCTION_BUTTONS = computed(() =>
  activeTab.value === 'oper' ? OPER_FUNCTION_BUTTONS : LOGIN_FUNCTION_BUTTONS)

/** 页面配置 storage-key（每个 Tab 独立；与列配置的 storage-key 必须不同值） */
const pageConfigKey = computed(() => `set-operation-log-${activeTab.value}-page-config`)

function loadPageConfig() {
  const fields = DEFAULT_QUERY_FIELDS.value
  const buttons = DEFAULT_FUNCTION_BUTTONS.value
  let saved: any = null
  try {
    const raw = localStorage.getItem(pageConfigKey.value)
    saved = raw ? JSON.parse(raw) : null
  } catch {
    saved = null
  }
  queryFieldsConfig.value = fields.map(def => {
    const hit = saved?.queryFields?.find((f: QueryFieldSetting) => f.key === def.key)
    return hit ? { ...def, visible: hit.visible !== false } : { ...def }
  })
  functionButtonsConfig.value = buttons.map(def => {
    const hit = saved?.functionButtons?.find((f: FunctionButtonSetting) => f.key === def.key)
    return hit ? { ...def, enabled: hit.enabled !== false } : { ...def }
  })
}

function handlePageConfigChange(config: any) {
  try {
    localStorage.setItem(pageConfigKey.value, JSON.stringify({
      queryFields: config.queryFields || [],
      functionButtons: config.functionButtons || [],
    }))
  } catch { /* 本地存储不可用时忽略 */ }
  if (Array.isArray(config.queryFields)) queryFieldsConfig.value = config.queryFields
  if (Array.isArray(config.functionButtons)) functionButtonsConfig.value = config.functionButtons
}

function fieldVisible(key: string): boolean {
  return queryFieldsConfig.value.find(f => f.key === key)?.visible !== false
}

function btnEnabled(key: string): boolean {
  return functionButtonsConfig.value.find(b => b.key === key)?.enabled !== false
}

// ═══════════════════════════ 格式化 ═══════════════════════════

function fmtDateTime(value?: string | null): string {
  if (!value) return '-'
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? String(value) : dayjs(d).format('YYYY-MM-DD HH:mm:ss')
}

/** 耗时：>1000ms 换算成秒（避免显示 3500ms） */
function fmtCost(cost?: number | null): string {
  if (cost == null) return '-'
  return cost > 1000 ? `${(cost / 1000).toFixed(2)}s` : `${cost}ms`
}

function actionText(action?: string | null): string {
  if (!action) return '-'
  return ACTION_MAP[action] || action
}

/** 系统日志「内容」列：模块 + 操作类型（对标内容形如「修改了商品[…]」，本系统按现有字段如实组合） */
function operContent(record: OperLogRow): string {
  const action = record.action ? (ACTION_MAP[record.action] || record.action) : ''
  if (record.module && action) return `${record.module} · ${action}`
  return record.module || action || '-'
}

/** 登录日志「登录类型」列：对标实测形态「从【电脑端】登录,IP<…>」 */
function loginTypeContent(record: LoginLogRow): string {
  const device = DEVICE_MAP[record.deviceType || ''] || '未知设备'
  const ip = record.loginIp ? `,IP<${record.loginIp}>` : ''
  return `从【${device}】登录${ip}`
}

/**
 * 敏感字段脱敏（详情弹窗的请求参数 / 响应结果）：
 * 这两列是 text，可能落进密码 / token（OWASP 日志准则明确禁止原文展示），
 * 故按字段名做掩码 —— 只改展示，不改数据。
 */
function maskSensitive(raw?: string | null): string {
  if (!raw) return '-'
  return String(raw).replace(
    /("(?:password|passwd|pwd|token|accessToken|refreshToken|secret|authorization|clientSecret)"\s*:\s*")[^"]*(")/gi,
    '$1******$2'
  )
}

// ═══════════════════════════ 查询参数 ═══════════════════════════

function buildOperParams(): OperLogQuery {
  return {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
    module: operForm.module || undefined,
    operationType: operForm.operationType || undefined,
    operatorName: operForm.operatorName || undefined,
    startDate: operForm.startDate || undefined,
    endDate: operForm.endDate || undefined,
    status: operForm.status,
  }
}

function buildLoginParams(): LoginLogQuery {
  return {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
    username: loginForm.username || undefined,
    loginType: loginForm.loginType,
    loginResult: loginForm.loginResult,
    startDate: loginForm.startDate || undefined,
    endDate: loginForm.endDate || undefined,
  }
}

// ═══════════════════════════ 数据加载 ═══════════════════════════
//
// ⚠️ 拦截器已拆包：res 本身就是 Page 对象（{ records, total }），直接读 res.records。
//    写成 res.data.records 会得到 undefined → TypeError → 被 catch 吞成「查询失败」→ 列表恒空。

async function fetchList() {
  loading.value = true
  try {
    if (activeTab.value === 'oper') {
      const res: any = await logApi.getPage(buildOperParams())
      operRows.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    } else {
      const res: any = await logApi.getLoginPage(buildLoginParams())
      loginRows.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    }
  } catch (error: any) {
    console.error('[操作日志] 加载列表失败', error)
    message.error(error?.response?.data?.message || '查询失败')
    if (activeTab.value === 'oper') {
      operRows.value = []
    } else {
      loginRows.value = []
    }
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 系统日志的模块 / 操作类型下拉值域（静默降级：失败只是下拉为空，不阻塞列表） */
async function loadOptions() {
  try {
    const mods: any = await logApi.getModules()
    const list = Array.isArray(mods) ? mods : []
    moduleOptions.value = list.map((m: string) => ({ label: m, value: m }))
  } catch (error) {
    console.warn('[操作日志] 模块下拉加载失败', error)
    moduleOptions.value = []
  }
  try {
    const types: any = await logApi.getOperationTypes()
    const list = Array.isArray(types) ? types : []
    operationTypeOptions.value = list.map((t: string) => ({ label: actionText(t), value: t }))
  } catch (error) {
    console.warn('[操作日志] 操作类型下拉加载失败', error)
    operationTypeOptions.value = []
  }
}

function handleSearch() {
  // 对标：登录日期(起)/(止) 为必填（列头带星号）
  if (activeTab.value === 'login' && (!loginForm.startDate || !loginForm.endDate)) {
    message.warning('请先选择登录日期(起)与登录日期(止)')
    return
  }
  pagination.current = 1
  fetchList()
}

/**
 * 登录日期变更：两个日期都填好才自动查询；
 * 只填一个时不弹「必填」提示（避免用户改日期途中被反复打断），由「查询」按钮统一校验。
 */
function handleLoginDateChange() {
  if (loginForm.startDate && loginForm.endDate) {
    pagination.current = 1
    fetchList()
  }
}

function handleReset() {
  if (activeTab.value === 'oper') {
    Object.assign(operForm, {
      startDate: undefined,
      endDate: undefined,
      module: undefined,
      operationType: undefined,
      operatorName: '',
      status: undefined,
    })
  } else {
    Object.assign(loginForm, {
      startDate: dayjs().subtract(6, 'day').format('YYYY-MM-DD'),
      endDate: dayjs().format('YYYY-MM-DD'),
      loginType: undefined,
      username: '',
      loginResult: undefined,
    })
  }
  pagination.current = 1
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

/** 切 Tab：列定义 / 查询条件 / 页面配置 / storage-key 全部随之切换，并重新取数 */
function handleTabChange(key: string) {
  if (key === activeTab.value) return
  activeTab.value = key === 'login' ? 'login' : 'oper'
  pagination.current = 1
  pagination.total = 0
  loadPageConfig()
  nextTick(() => fetchList())
}

// ═══════════════════════════ 详情弹窗 ═══════════════════════════

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<OperLogRow | null>(null)

/**
 * 打开详情：列表接口为性能/敏感面已裁剪 requestParams / responseResult，
 * 故这里真正调用 GET /log/{id}（历史实现直接用列表行对象，该端点永不被调用）；
 * 请求失败时降级为列表行数据。
 */
async function handleDetail(record: OperLogRow) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = record
  try {
    const res: any = await logApi.getById(record.id)
    if (res) detail.value = res
  } catch (error) {
    console.warn('[操作日志] 加载日志详情失败，降级为列表行数据', error)
  } finally {
    detailLoading.value = false
  }
}

// ═══════════════════════════ 清理历史日志 ═══════════════════════════

/**
 * 清理历史日志（语义订正）：
 * 后端 `DELETE /log/clear?days=N` **只删 oper_time 早于 N 天前的记录，不是「清空所有」**。
 * 因此这里先调 `/clear/preview` 取「将删除的条数」，在确认框里如实告知，
 * 避免历史缺陷（文案「清空所有」而实际只删 90 天前）造成的误导。
 */
async function handleClear() {
  if (activeTab.value !== 'oper') return
  let preview: number | null = null
  try {
    preview = Number(await logApi.previewClear(CLEAR_RETAIN_DAYS))
  } catch (error: any) {
    message.error(error?.response?.data?.message || '无法获取待清理条数')
    return
  }
  Modal.confirm({
    title: '清理历史日志',
    content: `将删除【${CLEAR_RETAIN_DAYS} 天前】的操作日志共 ${preview ?? 0} 条；`
      + `${CLEAR_RETAIN_DAYS} 天以内的日志不会被删除。此操作不可恢复。`,
    okText: '确认清理',
    okType: 'danger',
    async onOk() {
      const deleted = Number(await logApi.clearLogs(CLEAR_RETAIN_DAYS))
      message.success(`已清理 ${deleted || 0} 条历史日志`)
      fetchList()
    },
  })
}

// ═══════════════════════════ 导出（真实落盘） ═══════════════════════════

/**
 * 导出：调后端 CSV 端点（UTF-8 BOM，Excel 可直接打开中文），拿到 Blob 后**真实触发下载**。
 * 历史缺陷：旧实现拿到 Blob 后没有任何 createObjectURL / a.download 处理，却提示「导出成功」
 * （假成功 + 文件不落盘）→ 本轮修掉。
 */
async function handleExport() {
  if (exporting.value) return
  exporting.value = true
  try {
    const isOper = activeTab.value === 'oper'
    const blob: any = isOper
      ? await logApi.exportLogs(buildOperParams())
      : await logApi.exportLoginLogs(buildLoginParams())

    // 业务失败时后端返回 JSON（拦截器对 blob 直返，不拆包），此时不下载
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    if (!blob || (typeof blob.size === 'number' && blob.size === 0)) {
      message.warning('没有可导出的数据')
      return
    }

    const filename = `${isOper ? '系统日志' : '登录日志'}_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    console.error('[操作日志] 导出失败', error)
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══════════════════════════ 打印(F8) ═══════════════════════════

function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

/** 真实打印模板：按当前 Tab 的台账列出一张可打印的 HTML（新窗口 → print） */
function handlePrint() {
  const isOper = activeTab.value === 'oper'
  const rows = (isOper ? operRows.value : loginRows.value).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const title = isOper ? '系统日志' : '登录日志'
  const body = isOper
    ? (rows as OperLogRow[]).map((r, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(fmtDateTime(r.operTime))}</td>
      <td>${escapeHtml(r.username || '')}</td>
      <td>${escapeHtml(r.realName || '')}</td>
      <td>${escapeHtml(operContent(r))}</td>
      <td>${escapeHtml(r.status === 0 ? '成功' : '失败')}</td>
    </tr>`).join('')
    : (rows as LoginLogRow[]).map((r, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(fmtDateTime(r.loginTime))}</td>
      <td>${escapeHtml(r.username || '')}</td>
      <td>${escapeHtml(r.realName || '')}</td>
      <td>${escapeHtml(loginTypeContent(r))}</td>
      <td>${escapeHtml(LOGIN_TYPE_MAP[r.loginType as number] || '未知')}</td>
    </tr>`).join('')
  const head = isOper
    ? '<tr><th>#</th><th>时间</th><th>操作员</th><th>姓名</th><th>内容</th><th>状态</th></tr>'
    : '<tr><th>#</th><th>时间</th><th>操作员</th><th>姓名</th><th>登录类型</th><th>登录方式</th></tr>'
  const filterText = isOper
    ? `模块：${operForm.module || '全部'}｜操作类型：${operForm.operationType ? actionText(operForm.operationType) : '全部'}｜操作员：${operForm.operatorName || '全部'}`
    : `登录日期：${loginForm.startDate || '-'} ~ ${loginForm.endDate || '-'}｜操作员：${loginForm.username || '全部'}`

  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>操作日志-${title}</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>操作日志 - ${title}</h2>
    <div class="meta">
      <span>筛选条件：${escapeHtml(filterText)}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}（当前页）</span>
    </div>
    <table><thead>${head}</thead><tbody>${body}</tbody></table></body></html>`

  const win = window.open('', '_blank', 'width=1200,height=760')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

/** F8 快捷键打印（与 ql361 / 其他金标准页一致） */
function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[操作日志] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══════════════════════════ 初始化 ═══════════════════════════

onMounted(async () => {
  loadPageConfig()
  await nextTick()
  fetchList()
  loadOptions()
  window.addEventListener('keydown', handleF8Key)
})

onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
/* ═══ 查询区：横向自适应网格（禁止纵向单列） ═══ */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 8px 12px; }
.search-field-item { min-width: 150px; }
.search-action-group { display: flex; align-items: center; justify-content: flex-end; gap: 8px; min-width: 150px; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }

.detail-pre { margin: 0; max-height: 200px; overflow: auto; white-space: pre-wrap; word-break: break-all; font-size: 12px; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
