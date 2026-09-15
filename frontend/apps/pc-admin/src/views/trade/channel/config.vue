<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        渠道配置（交易 → 外部平台 → 渠道配置，路由 trade/channel/config，菜单 604）
        · 定位：外部平台渠道对接配置中心（API 端点 / 应用密钥 / 同步开关·间隔 / 初始化 / 手动同步）
        · 对标：ql361 无独立页（渠道配置开发文档「对标判断」）→ 本系统独有页，按本系统实现 + 业界标准
        · 骨架（路线 A）：CategoryListLayout + BillDetailTable（表头序号齿轮列配置：个人/全局）+ PageConfigPanel
        · 数据：`GET /api/trade/channel/page`（本轮新增的管理端分页端点；原 `listEnabled` 一次性返回全部启用渠道）
                统计：`GET /api/trade/channel/stat`（后端真实聚合，全量口径，非当前页）
        · 密钥安全：appSecret / accessToken / refreshToken 由后端 **脱敏返回**（`GET /api/trade/channel/{id}/secret`），
          页面不回显明文；未修改密钥时提交不覆盖已存值
        · 同步开关语义（如实记录）：后端**无独立 sync 开关端点**，行内「同步开关」`<a-switch>` 复用启用/禁用端点
          `POST /api/trade/channel/{id}/toggle?enabled=`（该端点仅切换 `status`）；`syncEnabled` 字段本身只在
          新增/编辑弹窗保存时写入。后端若拆分同步开关端点，本页需同步改造。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增渠道 ═══ -->
        <template #toolbar-left>
          <a-button type="primary" size="small" class="btn-add" @click="openCreate">
            <PlusOutlined /> 新增渠道
          </a-button>
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

        <!-- ═══ 查询区（渠道编码/名称关键字 + 渠道类型 + 同步开关 + 启用状态） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div v-if="isQueryVisible('keyword')" class="search-item">
                <span class="search-label">渠道</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="渠道编码 / 渠道名称"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('channelType')" class="search-item">
                <span class="search-label">类型</span>
                <a-select
                  v-model:value="searchForm.channelType"
                  placeholder="全部类型"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="channelTypeOptions"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('syncEnabled')" class="search-item">
                <span class="search-label">同步开关</span>
                <a-select
                  v-model:value="searchForm.syncEnabled"
                  placeholder="全部"
                  size="small"
                  style="width: 110px"
                  allow-clear
                  :options="syncEnabledOptions"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('status')" class="search-item">
                <span class="search-label">启用状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 110px"
                  allow-clear
                  :options="statusOptions"
                  @change="handleSearch"
                />
              </div>
              <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
              <a-button size="small" @click="handleReset">重置</a-button>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（含统计卡片；列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="channel-body">
            <!-- 统计卡片：全部接后端真实聚合 `GET /api/trade/channel/stat`（全量口径，非当前页数据聚合） -->
            <a-row :gutter="12" class="stat-row">
              <a-col :span="6">
                <div class="stat-card stat-card-blue">
                  <div class="stat-title">渠道总数</div>
                  <div class="stat-value">{{ stats.total }}</div>
                  <div class="stat-desc">后端聚合（全量渠道）</div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-green">
                  <div class="stat-title">启用中</div>
                  <div class="stat-value">{{ stats.enabledCount }}</div>
                  <div class="stat-desc">后端聚合（status=1）</div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-purple">
                  <div class="stat-title">同步开启</div>
                  <div class="stat-value">{{ stats.syncEnabledCount }}</div>
                  <div class="stat-desc">后端聚合（syncEnabled=1）</div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="stat-card stat-card-red">
                  <div class="stat-title">异常</div>
                  <div class="stat-value">{{ stats.abnormalCount }}</div>
                  <div class="stat-desc">后端聚合（已禁用或令牌过期）</div>
                </div>
              </a-col>
            </a-row>

            <div class="table-area">
              <BillDetailTable
                v-model:data-source="tableData"
                :columns="columns"
                :loading="loading"
                :view-mode="true"
                :min-rows="20"
                storage-key="trade-channel-config-table-columns"
                global-config-key="trade-channel-config-table-columns"
              >
                <!-- 渠道（按渠道类型着色） -->
                <template #channelCell="{ record }">
                  <a-tag :color="channelTagColor(record.channelCode)">
                    {{ CHANNEL_CODE_MAP[record.channelCode]?.name || record.channelCode }}
                  </a-tag>
                </template>

                <!-- 类型 -->
                <template #typeCell="{ record }">
                  <a-tag :color="CHANNEL_TYPE_MAP[record.channelType]?.color || 'default'">
                    {{ CHANNEL_TYPE_MAP[record.channelType]?.name || record.channelType || '-' }}
                  </a-tag>
                </template>

                <!-- 连接状态 -->
                <template #connectedCell="{ record }">
                  <a-tag :color="record.accessToken ? 'success' : 'warning'">
                    {{ record.accessToken ? '已连接' : '待配置' }}
                  </a-tag>
                </template>

                <!-- 同步开关（切换即调 toggle） -->
                <template #syncEnabledCell="{ record }">
                  <a-switch
                    :checked="record.syncEnabled === 1"
                    :loading="togglingId === record.id"
                    checked-children="开"
                    un-checked-children="关"
                    @change="(checked: any) => handleToggleSync(record, checked)"
                  />
                </template>

                <!-- 最后同步 -->
                <template #lastSyncTimeCell="{ record }">
                  {{ fmtTime(record.lastSyncTime) }}
                </template>

                <!-- 状态 -->
                <template #statusCell="{ record }">
                  <a-tag :color="record.status === 1 ? 'success' : 'error'">
                    {{ record.status === 1 ? '正常' : '禁用' }}
                  </a-tag>
                </template>

                <!-- 操作列（配置 / 启用·禁用 / 初始化 / 同步 / 查看密钥 / 删除） -->
                <template #actionCell="{ record }">
                  <a-space v-if="!record.__ghost" :size="0">
                    <a-button type="link" size="small" @click="openEdit(record)">配置</a-button>
                    <a-button type="link" size="small" @click="handleToggleStatus(record)">
                      {{ record.status === 1 ? '禁用' : '启用' }}
                    </a-button>
                    <a-button v-if="!record.accessToken" type="link" size="small" @click="handleInitialize(record)">
                      初始化
                    </a-button>
                    <a-button type="link" size="small" @click="handleSync(record)">同步</a-button>
                    <a-button type="link" size="small" @click="openSecret(record)">查看密钥</a-button>
                    <a-popconfirm title="确定删除该渠道配置？" @confirm="handleDelete(record)">
                      <a-button type="link" size="small" danger>删除</a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </BillDetailTable>
            </div>
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

      <!-- ═══ 新增/编辑渠道弹窗（字段与后端 ExternalChannelConfig 一致） ═══ -->
      <a-modal
        v-model:open="modalVisible"
        :title="editingId ? '编辑渠道配置' : '新增渠道'"
        :width="700"
        :confirm-loading="saving"
        @ok="handleSave"
      >
        <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
          <a-form-item label="渠道编码" required>
            <a-select
              v-model:value="form.channelCode"
              placeholder="请选择渠道"
              :disabled="!!editingId"
              :options="channelCodeOptions"
              @change="handleChannelCodeChange"
            />
          </a-form-item>
          <a-form-item label="渠道名称">
            <a-input v-model:value="form.channelName" placeholder="渠道名称" />
          </a-form-item>
          <a-form-item label="API端点">
            <a-input v-model:value="form.apiEndpoint" placeholder="如: https://eco.taobao.com/router/rest" />
          </a-form-item>
          <a-form-item label="App ID">
            <a-input v-model:value="form.appId" placeholder="应用ID" />
          </a-form-item>
          <a-form-item label="App Secret">
            <a-input-password
              v-model:value="form.appSecret"
              :placeholder="editingId ? '留空 = 不修改已存密钥' : '应用密钥（后端加密存储）'"
            />
          </a-form-item>
          <a-form-item label="同步间隔">
            <a-input-number v-model:value="form.syncInterval" :min="5" :max="120" addon-after="分钟" style="width: 100%" />
          </a-form-item>
          <a-form-item label="同步开关">
            <a-switch v-model:checked="syncEnabledChecked" checked-children="开" un-checked-children="关" />
          </a-form-item>
          <a-form-item label="扩展配置">
            <a-textarea v-model:value="form.configJson" placeholder="JSON格式配置" :rows="3" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 查看密钥弹窗（后端脱敏返回，无明文） ═══ -->
      <a-modal
        v-model:open="secretVisible"
        title="查看渠道密钥"
        :width="560"
        :footer="null"
      >
        <a-spin :spinning="secretLoading">
          <a-alert
            type="warning"
            show-icon
            message="密钥安全"
            description="appSecret / accessToken / refreshToken 一律由后端脱敏返回，本页不展示明文；如需重置请在「配置」中填写新值后保存。"
            style="margin-bottom: 12px"
          />
          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="渠道">
              {{ CHANNEL_CODE_MAP[secretData.channelCode]?.name || secretData.channelCode || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="App ID">{{ secretData.appId || '-' }}</a-descriptions-item>
            <a-descriptions-item label="App Secret">
              {{ secretData.appSecret || (secretData.appSecretConfigured ? '-' : '未配置') }}
            </a-descriptions-item>
            <a-descriptions-item label="访问令牌">
              {{ secretData.accessToken || (secretData.accessTokenConfigured ? '-' : '未配置') }}
            </a-descriptions-item>
            <a-descriptions-item label="刷新令牌">
              {{ secretData.refreshToken || (secretData.refreshTokenConfigured ? '-' : '未配置') }}
            </a-descriptions-item>
            <a-descriptions-item label="令牌过期时间">{{ fmtTime(secretData.tokenExpireTime) }}</a-descriptions-item>
          </a-descriptions>
        </a-spin>
      </a-modal>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="trade-channel-config-page-config"
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
import { message } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, PrinterOutlined, DownloadOutlined, SettingOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { channelConfigApi, CHANNEL_CODE_MAP, CHANNEL_TYPE_MAP, type ExternalChannelConfig } from '@/api/trade'

defineOptions({ name: 'TradeChannelConfig' })

// ═══ 常量字典（与后端枚举一致） ═══
const channelCodeOptions = Object.entries(CHANNEL_CODE_MAP).map(([value, v]) => ({ value, label: v.name }))
const channelTypeOptions = Object.entries(CHANNEL_TYPE_MAP).map(([value, v]) => ({ value, label: v.name }))
const syncEnabledOptions = [
  { value: 1, label: '已开启' },
  { value: 0, label: '已关闭' }
]
const statusOptions = [
  { value: 1, label: '正常' },
  { value: 0, label: '禁用' }
]

function channelTagColor(channelCode: string): string {
  const type = CHANNEL_CODE_MAP[channelCode]?.type
  if (type === 'ECOMMERCE') return 'blue'
  if (type === 'SOCIAL') return 'green'
  if (type === 'ERP') return 'orange'
  return 'purple'
}

// ═══ 状态 ═══
const loading = ref(false)
const saving = ref(false)
const togglingId = ref<number | null>(null)
const tableData = ref<ExternalChannelConfig[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const searchForm = reactive<{
  keyword?: string
  channelType?: string
  syncEnabled?: number
  status?: number
}>({})

// 序号列承载表头「列配置」齿轮；操作列为固定列（9 列 + 行号 + 操作）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 290, fixed: 'left' },
  { key: 'channelCode', title: '渠道', type: 'slot', slotName: 'channelCell', width: 130 },
  { key: 'channelName', title: '渠道名称', type: 'input', width: 150 },
  { key: 'channelType', title: '类型', type: 'slot', slotName: 'typeCell', width: 110 },
  { key: 'connected', title: '连接状态', type: 'slot', slotName: 'connectedCell', width: 100 },
  { key: 'syncEnabled', title: '同步开关', type: 'slot', slotName: 'syncEnabledCell', width: 100 },
  { key: 'syncInterval', title: '同步间隔', type: 'input', width: 90, align: 'right' },
  { key: 'lastSyncTime', title: '最后同步', type: 'slot', slotName: 'lastSyncTimeCell', width: 160 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90 },
  { key: 'apiEndpoint', title: 'API端点', type: 'input', width: 240, defaultHidden: true },
  { key: 'appId', title: 'App ID', type: 'input', width: 160, defaultHidden: true },
  { key: 'tokenExpireTime', title: '令牌过期时间', type: 'slot', slotName: 'lastSyncTimeCell', width: 160, defaultHidden: true }
]

// ═══ 统计卡片（后端真实聚合：GET /api/trade/channel/stat，全量口径，不随查询区/分页变化） ═══
const stats = reactive<Record<string, any>>({})

// ═══ 页面配置 ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '渠道（编码/名称）', visible: true },
  { key: 'channelType', label: '渠道类型', visible: true },
  { key: 'syncEnabled', label: '同步开关', visible: true },
  { key: 'status', label: '启用状态', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
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
function fmtTime(val?: string | null): string {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm')
}

// ═══ 数据加载 ═══
/** 统计走后端聚合（`GET /api/trade/channel/stat`）；失败只告警，不影响列表 */
async function loadStats() {
  try {
    const res: any = await channelConfigApi.stat()
    Object.assign(stats, res || {})
  } catch (error) {
    console.warn('[渠道配置] 统计加载失败', error)
  }
}

async function fetchData() {
  loading.value = true
  loadStats()
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword.trim()
    if (searchForm.channelType) params.channelType = searchForm.channelType
    if (searchForm.syncEnabled != null) params.syncEnabled = searchForm.syncEnabled
    if (searchForm.status != null) params.status = searchForm.status
    const res: any = await channelConfigApi.page(params)
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[渠道配置] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载渠道配置失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  Object.assign(searchForm, {
    keyword: undefined, channelType: undefined,
    syncEnabled: undefined, status: undefined
  })
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 新增 / 编辑 ═══
const modalVisible = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  channelCode: '' as string,
  channelName: '' as string,
  channelType: 'ECOMMERCE' as string,
  apiEndpoint: '' as string,
  appId: '' as string,
  appSecret: '' as string,
  syncEnabled: 1 as number,
  syncInterval: 30 as number,
  configJson: '' as string,
  status: 1 as number
})
const form = reactive(emptyForm())
const syncEnabledChecked = computed({
  get: () => form.syncEnabled === 1,
  set: (val: boolean) => { form.syncEnabled = val ? 1 : 0 }
})

/** 渠道类型按渠道编码自动回填（与后端 CHANNEL_CODE_MAP 口径一致） */
function handleChannelCodeChange() {
  const meta = CHANNEL_CODE_MAP[form.channelCode]
  if (meta) {
    form.channelName = meta.name
    form.channelType = meta.type
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  modalVisible.value = true
}

function openEdit(record: any) {
  editingId.value = record.id
  Object.assign(form, emptyForm(), {
    channelCode: record.channelCode || '',
    channelName: record.channelName || '',
    channelType: record.channelType || 'ECOMMERCE',
    apiEndpoint: record.apiEndpoint || '',
    appId: record.appId || '',
    // 安全：密钥不回显，留空表示不修改
    appSecret: '',
    syncEnabled: record.syncEnabled ?? 0,
    syncInterval: record.syncInterval ?? 30,
    configJson: record.configJson || '',
    status: record.status ?? 1
  })
  modalVisible.value = true
}

async function handleSave() {
  if (!form.channelCode) {
    message.warning('请选择渠道')
    return
  }
  const meta = CHANNEL_CODE_MAP[form.channelCode]
  if (meta) {
    form.channelName = form.channelName || meta.name
    form.channelType = meta.type
  }
  // 密钥留空 = 不覆盖已存值（后端 update 为整体覆盖，故留空时移除该字段）
  const payload: any = { ...form }
  if (editingId.value && !payload.appSecret) {
    delete payload.appSecret
  }
  saving.value = true
  try {
    if (editingId.value) {
      await channelConfigApi.update(editingId.value, payload as ExternalChannelConfig)
      message.success('更新成功')
    } else {
      await channelConfigApi.create(payload as ExternalChannelConfig)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 行级操作 ═══
/**
 * 行内「同步开关」切换
 * ⚠️ 后端**没有独立的 sync 开关端点**：`POST /api/trade/channel/{id}/toggle?enabled=` 仅切换 `status`（启用/禁用），
 *   不写 `syncEnabled`。本页保留现状（不造端点），因此：切换后列表刷新时该开关仍显示库中 `syncEnabled` 原值，
 *   `syncEnabled` 的真实变更入口是「配置」弹窗保存（update）。语义拆分待后端提供独立端点。
 */
async function handleToggleSync(record: any, checked: boolean | string | number) {
  togglingId.value = record.id
  try {
    await channelConfigApi.toggleStatus(record.id, !!checked)
    message.success(checked ? '渠道已启用' : '渠道已禁用')
    fetchData()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '同步开关切换失败')
  } finally {
    togglingId.value = null
  }
}

async function handleToggleStatus(record: any) {
  const target = record.status === 1 ? 0 : 1
  try {
    // 后端 `/toggle` 只切换 status（启用/禁用）；同步开关 syncEnabled 由「配置」弹窗 update 承载
    await channelConfigApi.toggleStatus(record.id, target === 1)
    message.success(target === 1 ? '已启用' : '已禁用')
    fetchData()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '状态切换失败')
  }
}

async function handleInitialize(record: any) {
  try {
    await channelConfigApi.initialize(record.id)
    message.success('初始化完成')
    fetchData()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '初始化失败')
  }
}

async function handleSync(record: any) {
  message.info('开始同步...')
  try {
    const res: any = await channelConfigApi.sync(record.id)
    const data = res || {}
    message.success(`同步完成！同步订单: ${data.syncedOrders ?? 0}，同步商品: ${data.syncedProducts ?? 0}`)
    fetchData()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '同步失败')
  }
}

async function handleDelete(record: any) {
  try {
    await channelConfigApi.delete(record.id)
    message.success('删除成功')
    fetchData()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '删除失败')
  }
}

// ═══ 查看密钥（后端脱敏） ═══
const secretVisible = ref(false)
const secretLoading = ref(false)
const secretData = ref<Record<string, any>>({})

async function openSecret(record: any) {
  secretVisible.value = true
  secretLoading.value = true
  secretData.value = {}
  try {
    const res: any = await channelConfigApi.secret(record.id)
    secretData.value = res || {}
  } catch (error: any) {
    // 端点不可用时降级为列表行信息（不展示明文，仅显示是否已配置）
    secretData.value = {
      channelCode: record.channelCode,
      channelName: record.channelName,
      appId: record.appId,
      appSecretConfigured: !!record.appSecret,
      accessTokenConfigured: !!record.accessToken,
      refreshTokenConfigured: !!record.refreshToken,
      tokenExpireTime: record.tokenExpireTime
    }
    message.warning(error?.response?.data?.message || '密钥明细接口不可用，已仅显示配置状态')
  } finally {
    secretLoading.value = false
  }
}

// ═══ 打印(F8)：真实打印模板（与列表同口径） ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function printableRows(): any[] {
  return tableData.value.filter((r: any) => !r.__ghost)
}

function handlePrint() {
  const rows = printableRows()
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(CHANNEL_CODE_MAP[r.channelCode]?.name || r.channelCode || '')}</td>
      <td>${escapeHtml(r.channelName || '')}</td>
      <td>${escapeHtml(CHANNEL_TYPE_MAP[r.channelType]?.name || r.channelType || '')}</td>
      <td>${r.accessToken ? '已连接' : '待配置'}</td>
      <td>${r.syncEnabled === 1 ? '开' : '关'}</td>
      <td style="text-align:right">${escapeHtml(r.syncInterval ?? '')}</td>
      <td>${escapeHtml(fmtTime(r.lastSyncTime))}</td>
      <td>${r.status === 1 ? '正常' : '禁用'}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>渠道配置</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>渠道配置</h2>
    <div class="meta">
      <span>渠道筛选：${escapeHtml(searchForm.keyword || '全部')}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>渠道</th><th>渠道名称</th><th>类型</th><th>连接状态</th><th>同步开关</th><th>同步间隔</th><th>最后同步</th><th>状态</th></tr></thead>
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

// ═══ 导出（CSV，前端拼装；后端无渠道导出端点） ═══
function handleExport() {
  const rows = printableRows()
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['渠道', '渠道名称', '类型', '连接状态', '同步开关', '同步间隔(分钟)', '最后同步', '状态', 'API端点', 'AppID']
  const csv = [
    headers.join(','),
    ...rows.map((r: any) => [
      CHANNEL_CODE_MAP[r.channelCode]?.name || r.channelCode || '',
      r.channelName || '',
      CHANNEL_TYPE_MAP[r.channelType]?.name || r.channelType || '',
      r.accessToken ? '已连接' : '待配置',
      r.syncEnabled === 1 ? '开' : '关',
      r.syncInterval ?? '',
      r.lastSyncTime ? fmtTime(r.lastSyncTime) : '',
      r.status === 1 ? '正常' : '禁用',
      r.apiEndpoint || '',
      r.appId || ''
    ].map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
  ].join('\r\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `渠道配置_${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleError(error: Error) {
  console.error('[渠道配置] 页面错误', error)
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

.channel-body { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 12px 16px 0; }
.stat-row { flex-shrink: 0; margin-bottom: 12px; }
.stat-card { padding: 12px 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06); }
.stat-card.stat-card-blue { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); border: 1px solid #91d5ff; }
.stat-card.stat-card-green { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); border: 1px solid #b7eb8f; }
.stat-card.stat-card-purple { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); border: 1px solid #d3adf7; }
.stat-card.stat-card-red { background: linear-gradient(135deg, #fff2f0 0%, #ffd8d2 100%); border: 1px solid #ffbcb3; }
.stat-card.stat-card-orange { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); border: 1px solid #ffd591; }
.stat-title { font-size: 13px; color: #666; }
.stat-value { font-size: 22px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, monospace; }
.stat-desc { font-size: 12px; color: #999; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

/* 橙色新增按钮（交易模块统一） */
.btn-add { background: #ff6b35 !important; border-color: #ff6b35 !important; }
.btn-add:hover { background: #e55a2b !important; border-color: #e55a2b !important; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
