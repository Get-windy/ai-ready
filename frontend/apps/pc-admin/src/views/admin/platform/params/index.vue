<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        平台参数（系统 → 平台设置 → 平台参数，菜单 62501）
        · 平台控制台页面：ql361 无对标 → 按 Odoo ir.config_parameter（键/值 + 唯一约束）建模
        · 后端 cn.aiedge.config.controller.SystemConfigController（前缀 /api/config）
        · 实测 2026-09-18：后端已真实读写 sys_config 表（写入落库、回读可见、删除生效）
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/平台参数开发文档.md
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增参数 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('add')"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新增参数
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：刷新缓存 + 刷新 + 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('refreshCache')"
              size="small"
              :loading="cacheRefreshing"
              title="清理旧版本写下的历史配置缓存键"
              @click="handleRefreshCache"
            >
              <ThunderboltOutlined /> 刷新缓存
            </a-button>
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

        <!-- ═══ 查询区（参数键 / 分组，两项均为后端过滤） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('configKey')">
                <span class="search-label">参数键</span>
                <a-input
                  v-model:value="searchForm.configKey"
                  placeholder="请输入参数键（后端模糊匹配）"
                  size="small"
                  style="width: 220px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('configGroup')">
                <span class="search-label">分组</span>
                <a-select
                  v-model:value="searchForm.configGroup"
                  placeholder="全部分组"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  :options="groupSelectOptions"
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
          <div class="table-wrap">
            <a-alert
              class="table-alert"
              type="info"
              show-icon
              message="本页参数真实读写数据库 sys_config 表（已实测：写入落库、回读可见、删除生效）"
              description="按实测现状标注，不用旧结论搪塞：① 内置参数（builtin=true）后端禁止删除，删除会返回 success:false；② 「变更日志」写在 Redis List、未落库，且写入时未记录操作人 → 该列恒为空；③ 参数变更未写 sys_audit_log，平台侧无审计留痕。"
            />
            <div class="table-area">
              <BillDetailTable
                v-model:data-source="tableData"
                :columns="columns"
                :loading="loading"
                :view-mode="true"
                :min-rows="20"
                storage-key="system-platform-params-table-columns"
                global-config-key="system-platform-params-table-columns"
              >
                <!-- 参数名称 -->
                <template #configNameCell="{ record }">
                  <span>{{ record.configName || '-' }}</span>
                </template>

                <!-- 类型 -->
                <template #configTypeCell="{ record }">
                  <a-tag :color="TYPE_COLORS[record.configType] || 'default'">
                    {{ typeName(record.configType) }}
                  </a-tag>
                </template>

                <!-- 内置 / 自定义 -->
                <template #systemConfigCell="{ record }">
                  <a-tag :color="record.systemConfig ? 'purple' : 'default'">
                    {{ record.systemConfig ? '内置' : '自定义' }}
                  </a-tag>
                </template>

                <!-- 更新时间 -->
                <template #updateTimeCell="{ record }">
                  {{ fmtTime(record.updateTime) }}
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
                    <!-- 内置参数后端禁止删除（真拒绝），此处直接禁用并说明，避免无效点击 -->
                    <a-tooltip
                      v-if="record.systemConfig"
                      title="内置参数不可删除（后端保护）"
                    >
                      <a-button
                        type="link"
                        size="small"
                        disabled
                      >
                        删除
                      </a-button>
                    </a-tooltip>
                    <a-popconfirm
                      v-else
                      title="确定删除此参数？"
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
                    <a-button
                      type="link"
                      size="small"
                      @click="openLogs(record)"
                    >
                      变更日志
                    </a-button>
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

      <!-- ═══ 新增参数弹窗 ═══ -->
      <a-modal
        v-model:open="createVisible"
        title="新增参数"
        width="560px"
        :confirm-loading="saving"
        destroy-on-close
        @ok="handleCreate"
        @cancel="closeCreate"
      >
        <a-form
          ref="createFormRef"
          :model="createForm"
          :rules="createRules"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
          style="margin-top:16px"
        >
          <a-form-item
            label="参数键"
            name="configKey"
          >
            <a-input
              v-model:value="createForm.configKey"
              placeholder="如: system.version（全局唯一）"
            />
          </a-form-item>
          <a-form-item
            label="参数名称"
            name="configName"
          >
            <a-input
              v-model:value="createForm.configName"
              placeholder="请输入参数名称"
            />
          </a-form-item>
          <a-form-item
            label="配置类型"
            name="configType"
          >
            <a-select
              v-model:value="createForm.configType"
              placeholder="请选择配置类型"
              :options="typeSelectOptions"
            />
          </a-form-item>
          <a-form-item
            label="配置分组"
            name="configGroup"
          >
            <a-select
              v-model:value="createForm.configGroup"
              placeholder="请选择配置分组"
              :options="groupSelectOptions"
            />
          </a-form-item>
          <a-form-item
            label="参数值"
            name="configValue"
          >
            <a-textarea
              v-model:value="createForm.configValue"
              :rows="3"
              placeholder="请输入参数值"
            />
          </a-form-item>
          <a-form-item
            label="描述"
            name="description"
          >
            <a-textarea
              v-model:value="createForm.description"
              :rows="2"
              placeholder="参数用途说明"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 编辑参数弹窗（参数键/参数名称只读，仅可改参数值） ═══ -->
      <a-modal
        v-model:open="editVisible"
        title="编辑参数"
        width="500px"
        :confirm-loading="saving"
        destroy-on-close
        @ok="handleSave"
      >
        <a-descriptions
          :column="1"
          size="small"
          style="margin-bottom:16px"
        >
          <a-descriptions-item label="参数键">
            {{ editingRecord?.configKey }}
          </a-descriptions-item>
          <a-descriptions-item label="参数名称">
            {{ editingRecord?.configName || '-' }}
          </a-descriptions-item>
        </a-descriptions>
        <a-form layout="vertical">
          <a-form-item label="参数值">
            <a-textarea
              v-model:value="editForm.configValue"
              :rows="4"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 变更日志弹窗 ═══ -->
      <a-modal
        v-model:open="logsVisible"
        :title="`变更日志 - ${logsRecord?.configKey || ''}`"
        width="800px"
        :footer="null"
        destroy-on-close
      >
        <a-alert
          v-if="!logsLoading && !logs.length"
          type="warning"
          show-icon
          style="margin-bottom:12px"
          message="暂无变更日志"
          description="变更日志写在 Redis List、未落库；无 Redis 或日志未命中时返回空数组。且写入时未记录操作人，「操作人」列恒为空。"
        />
        <a-table
          :data-source="logs"
          :columns="logColumns"
          :loading="logsLoading"
          row-key="logId"
          :pagination="false"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'changeType'">
              <a-tag>{{ changeTypeLabel(record.changeType) }}</a-tag>
            </template>
            <template v-else-if="column.key === 'operateTime'">
              {{ fmtTime(record.operateTime) }}
            </template>
            <template v-else-if="column.key === 'operatorName'">
              {{ record.operatorName || '-' }}
            </template>
          </template>
        </a-table>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  SettingOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { platformConfigApi, type PlatformConfigItem, type ConfigChangeLogItem } from '@/api/admin'

defineOptions({ name: 'AdminPlatformParams' })

const PAGE_CONFIG_STORAGE_KEY = 'system-platform-params-page-config'

// 页面配置的查询条件 / 功能按钮项（与 PageConfigPanel 的 prop 结构一致）
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// ═══ 常量 ═══
/** 类型标签颜色（后端 /config/types 只给 code+name，颜色在前端定） */
const TYPE_COLORS: Record<string, string> = {
  system: 'blue',
  security: 'red',
  business: 'green',
  notification: 'orange',
  integration: 'purple',
}
/** 变更操作类型中文（后端写入 create / update 两类） */
const CHANGE_TYPE_LABELS: Record<string, string> = {
  create: '新增',
  update: '修改',
}

// ═══ 状态 ═══
const loading = ref(false)
const saving = ref(false)
const cacheRefreshing = ref(false)
const tableData = ref<PlatformConfigItem[]>([])
const typeOptions = ref<{ code: string; name: string }[]>([])
const groupOptions = ref<{ code: string; name: string }[]>([])

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({
  configKey: '' as string,
  configGroup: undefined as string | undefined,
})

// ═══ 查询条件 / 功能按钮（页面配置） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'configKey', label: '参数键', visible: true },
  { key: 'configGroup', label: '分组', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增参数', enabled: true },
  { key: 'refreshCache', label: '刷新缓存', enabled: true },
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

const typeSelectOptions = computed(() => typeOptions.value.map(t => ({ label: t.name, value: t.code })))
const groupSelectOptions = computed(() => groupOptions.value.map(g => ({ label: g.name, value: g.code })))

// 序号列承载表头「列配置」齿轮；操作列为固定列
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 200, fixed: 'left' },
  { key: 'configName', title: '参数名称', type: 'slot', slotName: 'configNameCell', width: 180 },
  { key: 'configKey', title: '参数键', type: 'input', width: 220 },
  { key: 'configValue', title: '参数值', type: 'input', width: 260 },
  { key: 'configType', title: '类型', type: 'slot', slotName: 'configTypeCell', width: 110 },
  { key: 'configGroup', title: '分组', type: 'input', width: 110 },
  { key: 'systemConfig', title: '内置', type: 'slot', slotName: 'systemConfigCell', width: 90 },
  { key: 'updateTime', title: '更新时间', type: 'slot', slotName: 'updateTimeCell', width: 160 },
]

const logColumns = [
  { title: '操作类型', dataIndex: 'changeType', key: 'changeType', width: 100 },
  { title: '旧值', dataIndex: 'oldValue', key: 'oldValue', ellipsis: true },
  { title: '新值', dataIndex: 'newValue', key: 'newValue', ellipsis: true },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 110 },
  { title: '操作时间', dataIndex: 'operateTime', key: 'operateTime', width: 170 },
]

// ═══ 展示辅助 ═══
function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}
function typeName(code: string | undefined | null): string {
  if (!code) return '-'
  return typeOptions.value.find(t => t.code === code)?.name || code
}
function changeTypeLabel(code: string | undefined | null): string {
  if (!code) return '-'
  return CHANGE_TYPE_LABELS[code] || code
}

// ═══ 弹窗状态 ═══
const editVisible = ref(false)
const editingRecord = ref<PlatformConfigItem | null>(null)
const editForm = reactive({ configValue: '' })

const createVisible = ref(false)
const createFormRef = ref()
const emptyCreateForm = () => ({
  configKey: '' as string,
  configName: '' as string,
  configType: undefined as string | undefined,
  configGroup: undefined as string | undefined,
  configValue: '' as string,
  description: '' as string,
})
const createForm = reactive(emptyCreateForm())
const createRules = {
  configKey: [{ required: true, message: '请输入参数键', trigger: 'blur' }],
  configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
}

const logsVisible = ref(false)
const logsLoading = ref(false)
const logs = ref<ConfigChangeLogItem[]>([])
const logsRecord = ref<PlatformConfigItem | null>(null)

// ═══ 数据加载（后端真分页：/config/page） ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await platformConfigApi.page({
      configKey: searchForm.configKey.trim() || undefined,
      configGroup: searchForm.configGroup,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[平台参数] 加载列表失败', error)
    message.error(error?.message || '加载平台参数失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 回读单行：按参数键精确匹配（后端 LIKE 查询后再精确筛选，避免前缀命中他人） */
async function readBackConfig(configKey: string): Promise<PlatformConfigItem | null> {
  const res: any = await platformConfigApi.page({ configKey, pageNum: 1, pageSize: 50 })
  const rows: PlatformConfigItem[] = res?.records || []
  return rows.find(r => r.configKey === configKey) || null
}

async function loadOptions() {
  try {
    const [types, groups] = await Promise.all([platformConfigApi.types(), platformConfigApi.groups()])
    typeOptions.value = Array.isArray(types) ? types : []
    groupOptions.value = Array.isArray(groups) ? groups : []
  } catch (e: any) {
    console.error('[平台参数] 类型/分组选项获取失败', e)
    message.error(e?.message || '加载参数类型/分组选项失败')
    typeOptions.value = []
    groupOptions.value = []
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  fetchList()
}
function handleReset() {
  searchForm.configKey = ''
  searchForm.configGroup = undefined
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

// ═══ 新增（写后回读，查不到不报成功） ═══
function closeCreate() {
  createVisible.value = false
  Object.assign(createForm, emptyCreateForm())
}
function openCreate() {
  Object.assign(createForm, emptyCreateForm())
  createVisible.value = true
}
async function handleCreate() {
  try {
    await createFormRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  const key = createForm.configKey.trim()
  try {
    await platformConfigApi.create({
      configKey: key,
      configName: createForm.configName.trim(),
      configType: createForm.configType,
      configGroup: createForm.configGroup,
      configValue: createForm.configValue,
      description: createForm.description.trim(),
    })
    const back = await readBackConfig(key)
    if (back) {
      message.success('参数已新增')
      closeCreate()
    } else {
      message.warning('接口返回成功，但回读列表查不到该参数键，实际未落库')
      closeCreate()
    }
    fetchList()
  } catch (e: any) {
    console.error('[平台参数] 新增失败', e)
    message.error(e?.message || '新增参数失败')
  } finally {
    saving.value = false
  }
}

// ═══ 编辑（写后回读比对值，不一致就警告） ═══
function openEdit(record: PlatformConfigItem) {
  editingRecord.value = record
  editForm.configValue = record.configValue ?? ''
  editVisible.value = true
}
async function handleSave() {
  const record = editingRecord.value
  if (!record) return
  saving.value = true
  const nextValue = editForm.configValue
  try {
    await platformConfigApi.saveValue(record.configKey, nextValue)
    const back = await readBackConfig(record.configKey)
    if (back && back.configValue === nextValue) {
      message.success('参数已更新')
      editVisible.value = false
      editingRecord.value = null
    } else if (back) {
      message.warning(`接口返回成功，但回读值为「${back.configValue}」，与提交值不一致，请点「刷新」复核`)
    } else {
      message.warning('接口返回成功，但回读不到该参数键，修改可能未落库')
    }
    fetchList()
  } catch (e: any) {
    console.error('[平台参数] 保存失败', e)
    message.error(e?.message || '参数更新失败')
  } finally {
    saving.value = false
  }
}

// ═══ 删除（写后回读，仍在列表中就不报成功） ═══
async function handleDelete(record: PlatformConfigItem) {
  try {
    await platformConfigApi.remove(record.configKey)
    const back = await readBackConfig(record.configKey)
    if (back) {
      message.warning('接口返回成功，但回读列表该参数仍存在，删除未生效（内置参数会被后端拒绝）')
    } else {
      message.success('参数已删除')
    }
    fetchList()
  } catch (e: any) {
    console.error('[平台参数] 删除失败', e)
    message.error(e?.message || '删除失败')
  }
}

// ═══ 变更日志 ═══
async function openLogs(record: PlatformConfigItem) {
  logsRecord.value = record
  logsVisible.value = true
  logsLoading.value = true
  try {
    const res = await platformConfigApi.changeLogs(record.configKey)
    logs.value = Array.isArray(res) ? res : []
  } catch (e: any) {
    console.error('[平台参数] 加载变更日志失败', e)
    message.error(e?.message || '加载变更日志失败')
    logs.value = []
  } finally {
    logsLoading.value = false
  }
}

// ═══ 刷新缓存（回读 cleared 数量，如实说明清掉的是什么） ═══
async function handleRefreshCache() {
  cacheRefreshing.value = true
  try {
    const res = await platformConfigApi.refreshCache()
    const cleared = Number(res?.cleared)
    if (Number.isFinite(cleared) && cleared > 0) {
      message.success(`缓存刷新完成：清理历史配置缓存键 ${cleared} 个`)
    } else {
      message.success('缓存刷新完成：无历史配置缓存键需要清理（读取路径直连数据库，不依赖值缓存）')
    }
  } catch (e: any) {
    console.error('[平台参数] 刷新缓存失败', e)
    message.error(e?.message || '缓存刷新失败')
  } finally {
    cacheRefreshing.value = false
  }
}

function handleError(error: Error) {
  console.error('[平台参数] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchList()
  loadOptions()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
.table-wrap { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.table-alert { margin: 8px 8px 0; flex-shrink: 0; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.btn-add { background: #fa8c16; border-color: #fa8c16; }
</style>
