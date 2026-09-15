<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        配送参数（配送 → 配送配置 → 配送参数，菜单 80750 `dms:config-params`）
        · 定位（《配送参数开发文档》§3）：**参数中心** —— DMS 各域运行参数的唯一可写入口
          （《配送配置》已收敛为只读总览，消除两套 KV 维护）
        · 骨架：CategoryListLayout（左：参数分组树）+ BillTableList（序号齿轮列配置）+ PageConfigPanel
        · **元数据驱动**：后端 `DmsConfigMetaRegistry` 定义类型/默认值/范围/单位/生效方式/枚举，
          前端只按 valueType 渲染控件（数字/开关/下拉/JSON/时间范围/文本），不做业务判断
        · 危险操作（批量保存 / 恢复默认）均二次确认；敏感键脱敏展示、留空=不修改
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="true"
        category-title="参数分组"
        :category-editable="false"
        :category-tree-data="groupTree"
        :selected-category-id="selectedGroup"
        :show-table-footer="true"
        @category-select="handleGroupSelect"
      >
        <!-- ═══ 工具栏左侧：保存 + 恢复默认 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('save')"
              type="primary"
              size="small"
              class="btn-add"
              :loading="saving"
              :disabled="dirtyCount === 0"
              @click="handleBatchSave"
            >
              <SaveOutlined /> 保存修改（{{ dirtyCount }}）
            </a-button>
            <a-button
              v-if="isButtonEnabled('resetGroup')"
              size="small"
              :disabled="!selectedGroup"
              @click="handleResetGroup"
            >
              <UndoOutlined /> 本组恢复默认
            </a-button>
            <a-button
              size="small"
              :disabled="dirtyCount === 0"
              @click="reload"
            >
              放弃修改
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：导入/导出 + 页面配置 + 刷新 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('import')"
              size="small"
              @click="openImportModal"
            >
              <ImportOutlined /> 导入参数
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              :loading="exporting"
              @click="openExportModal"
            >
              <ExportOutlined /> 导出参数
            </a-button>
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
              @click="reload"
            >
              <ReloadOutlined /> 刷新
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区 ═══ -->
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
                  placeholder="参数键/参数名/说明"
                  size="small"
                  style="width: 220px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('valueType')"
                class="search-item"
              >
                <span class="search-label">参数类型</span>
                <a-select
                  v-model:value="searchForm.valueType"
                  placeholder="全部"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="VALUE_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('scope')"
                class="search-item"
              >
                <span class="search-label">作用域</span>
                <a-select
                  v-model:value="searchForm.scope"
                  placeholder="全部"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="SCOPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('editable')"
                class="search-item"
              >
                <span class="search-label">是否可改</span>
                <a-select
                  v-model:value="searchForm.editable"
                  placeholder="全部"
                  size="small"
                  style="width: 100px"
                  allow-clear
                  :options="EDITABLE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('effect')"
                class="search-item"
              >
                <span class="search-label">生效方式</span>
                <a-select
                  v-model:value="searchForm.effect"
                  placeholder="全部"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="EFFECT_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('updateTime')"
                class="search-item"
              >
                <span class="search-label">更新时间</span>
                <a-range-picker
                  v-model:value="searchForm.updateRange"
                  size="small"
                  style="width: 220px"
                  value-format="YYYY-MM-DD"
                  :placeholder="['开始日期', '结束日期']"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('configuredOnly')"
                class="search-item"
              >
                <a-checkbox
                  v-model:checked="searchForm.configuredOnly"
                  @change="handleSearch"
                >
                  仅看已配置
                </a-checkbox>
              </div>
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
              <span
                v-if="dirtyCount > 0"
                class="dirty-tip"
              >有 {{ dirtyCount }} 项未保存（保存后立即热生效）</span>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表 ═══ -->
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
              :selectable="false"
              storage-key="dms-config-params-columns"
              global-config-key="dms-config-params-columns"
              row-key="configKey"
            >
              <template #nameCell="{ record }">
                <span v-if="!record.__ghost">
                  {{ record.name }}
                  <a-tag
                    v-if="record.secret"
                    color="orange"
                    class="tag-gap"
                  >
                    敏感
                  </a-tag>
                  <a-tag
                    v-if="record.tenantOverride"
                    color="blue"
                    class="tag-gap"
                  >
                    租户覆盖
                  </a-tag>
                </span>
              </template>

              <template #valueCell="{ record }">
                <div
                  v-if="!record.__ghost"
                  class="value-cell"
                >
                  <a-input-number
                    v-if="record.valueType === 'NUMBER'"
                    :value="numValue(record)"
                    :min="record.min != null ? Number(record.min) : undefined"
                    :max="record.max != null ? Number(record.max) : undefined"
                    :precision="numberPrecision(record)"
                    :disabled="record.editable === false"
                    size="small"
                    style="width: 100%"
                    @change="(v: any) => onValueChange(record, v == null ? '' : String(v))"
                  />
                  <a-switch
                    v-else-if="record.valueType === 'BOOLEAN'"
                    :checked="String(record.__edit ?? record.configValue) === 'true' || String(record.__edit ?? record.configValue) === '1'"
                    :disabled="record.editable === false"
                    size="small"
                    @change="(v: any) => onValueChange(record, v ? 'true' : 'false')"
                  />
                  <a-select
                    v-else-if="record.valueType === 'ENUM'"
                    :value="record.__edit ?? record.configValue"
                    :options="record.options || []"
                    :disabled="record.editable === false"
                    size="small"
                    style="width: 100%"
                    @change="(v: any) => onValueChange(record, String(v ?? ''))"
                  />
                  <a-input
                    v-else
                    :value="record.__edit ?? record.configValue"
                    :placeholder="record.secret ? (record.configured ? '已配置（留空=不修改）' : '未配置') : (record.defaultValue ? `默认 ${record.defaultValue}` : '')"
                    :disabled="record.editable === false"
                    size="small"
                    @change="(e: any) => onValueChange(record, e.target.value)"
                  />
                  <span
                    v-if="record.unit"
                    class="unit-text"
                  >{{ record.unit }}</span>
                </div>
              </template>

              <template #rangeCell="{ record }">
                <span v-if="!record.__ghost" class="range-text">{{ rangeText(record) }}</span>
              </template>

              <template #effectCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="record.effect === 'RESTART' ? 'orange' : 'green'"
                >
                  {{ record.effect === 'RESTART' ? '需重启' : '热生效' }}
                </a-tag>
              </template>

              <template #descCell="{ record }">
                <span
                  v-if="!record.__ghost"
                  class="desc-text"
                  :title="record.desc"
                >{{ record.desc || '-' }}</span>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    :disabled="record.editable === false || !record.defaultValue && record.valueType !== 'TEXT'"
                    @click="handleResetOne(record)"
                  >
                    恢复默认
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="openHistory(record)"
                  >
                    变更历史
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

      <!-- ═══ 变更历史抽屉（审计 + 回滚） ═══ -->
      <a-drawer
        v-model:open="historyVisible"
        :title="`变更历史：${historyKey}`"
        :width="720"
        destroy-on-close
      >
        <a-table
          :data-source="historyList"
          :columns="historyColumns"
          :loading="historyLoading"
          :pagination="{ pageSize: 20 }"
          row-key="id"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'changeType'">
              <a-tag>{{ CHANGE_TYPE_MAP[record.changeType] || record.changeType }}</a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
              {{ String(record.createTime || '').replace('T', ' ').slice(0, 19) }}
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <a-button
                type="link"
                size="small"
                @click="handleRollback(record)"
              >
                回滚到此版本
              </a-button>
            </template>
          </template>
        </a-table>
      </a-drawer>

      <!-- ═══ 导入参数（JSON 参数集）：粘贴/选择文件 → 预览差异 → 确认导入 ═══ -->
      <a-modal
        v-model:open="importVisible"
        title="导入参数"
        :width="880"
        destroy-on-close
        :confirm-loading="importing"
        ok-text="确认导入"
        @ok="handleImportApply"
        @cancel="resetImport"
      >
        <a-alert
          type="info"
          show-icon
          class="import-tip"
          message="导入 JSON 参数集"
          description="结构与「导出参数」一致（{ items: [{ configKey, configValue }] }）；仅支持已有参数键；敏感键留空=不修改；存在非法项时整体拒绝、不写入任何参数。"
        />
        <div class="import-toolbar">
          <a-upload
            :before-upload="handleImportFile"
            :show-upload-list="false"
            accept=".json,application/json"
          >
            <a-button size="small">
              <UploadOutlined /> 选择 JSON 文件
            </a-button>
          </a-upload>
          <span
            v-if="importFileName"
            class="import-file"
          >{{ importFileName }}</span>
          <a-checkbox v-model:checked="importOverwrite">
            覆盖已存在的租户配置
          </a-checkbox>
          <a-button
            type="primary"
            size="small"
            :loading="importPreviewing"
            @click="handleImportPreview"
          >
            预览差异
          </a-button>
        </div>
        <a-textarea
          v-model:value="importText"
          :rows="6"
          class="import-text"
          placeholder='粘贴参数集 JSON，例如 {"items":[{"configKey":"dms.dispatch.strategy","configValue":"BALANCED"}]}'
        />
        <a-alert
          v-if="importResult && importResult.failed.length"
          type="error"
          show-icon
          class="import-tip"
          :message="`${importResult.failed.length} 项校验失败（整体拒绝，未写入任何参数）`"
        >
          <template #description>
            <div
              v-for="f in importResult.failed"
              :key="f.configKey"
              class="import-fail"
            >
              · {{ f.configKey }}：{{ f.reason }}
            </div>
          </template>
        </a-alert>
        <div
          v-else-if="importResult"
          class="import-summary"
        >
          共 {{ importResult.total }} 项：将变更 <b>{{ importResult.changed }}</b> 项、值相同 {{ importResult.unchanged }} 项、跳过 {{ importResult.skipped }} 项
        </div>
        <a-table
          v-if="importResult && importResult.preview.length"
          :data-source="importResult.preview"
          :columns="IMPORT_PREVIEW_COLUMNS"
          :pagination="{ pageSize: 10 }"
          row-key="configKey"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'action'">
              <a-tag :color="IMPORT_ACTION_COLOR[record.action]">
                {{ IMPORT_ACTION_MAP[record.action] || record.action }}
              </a-tag>
              <span
                v-if="record.reason"
                class="import-reason"
              >{{ record.reason }}</span>
            </template>
          </template>
        </a-table>
      </a-modal>

      <!-- ═══ 导出参数（JSON 参数清单；敏感键不导出明文） ═══ -->
      <a-modal
        v-model:open="exportVisible"
        title="导出参数"
        :width="520"
        destroy-on-close
        :confirm-loading="exporting"
        ok-text="导出 JSON"
        @ok="handleExportConfirm"
      >
        <a-radio-group v-model:value="exportScope">
          <a-radio value="filtered">
            当前筛选结果
          </a-radio>
          <a-radio value="all">
            全部参数
          </a-radio>
        </a-radio-group>
        <a-alert
          type="warning"
          show-icon
          class="import-tip"
          message="敏感键（密钥/令牌/密码）不导出明文；导入时按「留空=不修改」跳过。"
        />
      </a-modal>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="dms-config-params-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined, SettingOutlined, SaveOutlined, UndoOutlined,
  ImportOutlined, ExportOutlined, UploadOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import {
  configApi, type ConfigItem, type ConfigQuery,
  type ConfigExportPayload, type ConfigImportResult,
} from '@/api/dms/config'

defineOptions({ name: 'DmsConfigParams' })

const VALUE_TYPE_OPTIONS = [
  { label: '数字', value: 'NUMBER' }, { label: '文本', value: 'TEXT' }, { label: '开关', value: 'BOOLEAN' },
  { label: '枚举', value: 'ENUM' }, { label: 'JSON', value: 'JSON' }, { label: '时间范围', value: 'TIME_RANGE' },
]
const EFFECT_OPTIONS = [{ label: '热生效', value: 'HOT' }, { label: '需重启', value: 'RESTART' }]
/** 作用域：值来源（租户覆盖 / 继承全局默认） */
const SCOPE_OPTIONS = [{ label: '租户覆盖', value: 'TENANT' }, { label: '继承全局', value: 'GLOBAL' }]
const EDITABLE_OPTIONS = [{ label: '可改', value: 'true' }, { label: '只读', value: 'false' }]
const CHANGE_TYPE_MAP: Record<string, string> = {
  CREATE: '新增', UPDATE: '修改', CLEAR: '清空', ROLLBACK: '回滚', IMPORT: '导入',
}
const IMPORT_ACTION_MAP: Record<string, string> = {
  CREATE: '新增覆盖', UPDATE: '更新覆盖', SKIP: '跳过', SAME: '值相同',
}
const IMPORT_ACTION_COLOR: Record<string, string> = {
  CREATE: 'blue', UPDATE: 'orange', SKIP: 'default', SAME: 'green',
}
const IMPORT_PREVIEW_COLUMNS = [
  { title: '参数键', dataIndex: 'configKey', width: 240 },
  { title: '参数名', dataIndex: 'name', width: 150 },
  { title: '当前值', dataIndex: 'oldValue', width: 150, ellipsis: true },
  { title: '导入值', dataIndex: 'newValue', width: 150, ellipsis: true },
  { title: '动作', dataIndex: 'action', width: 200 },
]

// ═══ 状态 ═══
const loading = ref(false)
const saving = ref(false)
const tableData = ref<ConfigItem[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedGroup = ref<string>('')
const groupTree = ref<{ id: string; categoryName: string }[]>([])

const searchForm = reactive({
  keyword: '',
  valueType: undefined as string | undefined,
  scope: undefined as string | undefined,
  editable: undefined as string | undefined,
  effect: undefined as string | undefined,
  updateRange: undefined as [string, string] | undefined,
  configuredOnly: false,
})

/** 未保存的修改：key → 目标值 */
const edits = reactive<Record<string, string>>({})
const dirtyCount = computed(() => Object.keys(edits).length)

// ═══ 列（元数据驱动：值列内联控件） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 170, fixed: 'left' },
  { title: '参数名', key: 'name', type: 'slot', slotName: 'nameCell', width: 190 },
  { title: '参数键', key: 'configKey', width: 250 },
  { title: '当前值', key: 'configValue', type: 'slot', slotName: 'valueCell', width: 220 },
  { title: '默认值', key: 'defaultValue', width: 110 },
  { title: '取值/单位', key: 'range', type: 'slot', slotName: 'rangeCell', width: 130 },
  { title: '生效方式', key: 'effect', type: 'slot', slotName: 'effectCell', width: 100 },
  { title: '说明', key: 'desc', type: 'slot', slotName: 'descCell', width: 320 },
  { title: '更新时间', key: 'updateTime', width: 170, defaultHidden: true },
  { title: '作用域', key: 'scope', width: 100, defaultHidden: true },
]

function buildParams(): ConfigQuery {
  return {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
    ...(searchForm.keyword ? { keyword: searchForm.keyword.trim() } : {}),
    ...(selectedGroup.value ? { group: selectedGroup.value } : {}),
    ...(searchForm.valueType ? { valueType: searchForm.valueType } : {}),
    ...(searchForm.scope ? { tenantOverride: searchForm.scope === 'TENANT' } : {}),
    ...(searchForm.editable ? { editable: searchForm.editable === 'true' } : {}),
    ...(searchForm.effect ? { effect: searchForm.effect } : {}),
    ...(searchForm.updateRange?.[0] ? { updateTimeStart: searchForm.updateRange[0] } : {}),
    ...(searchForm.updateRange?.[1] ? { updateTimeEnd: searchForm.updateRange[1] } : {}),
    ...(searchForm.configuredOnly ? { configuredOnly: true } : {}),
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await configApi.page(buildParams())
    tableData.value = (res?.records || []) as ConfigItem[]
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载参数失败')
  } finally {
    loading.value = false
  }
}

async function fetchGroups() {
  try {
    const res: any = await configApi.meta()
    const groups: any[] = res?.groups || []
    groupTree.value = [
      { id: '', categoryName: `全部参数(${groups.reduce((s, g) => s + Number(g.count || 0), 0)})` },
      ...groups.map(g => ({ id: g.key, categoryName: `${g.text}(${g.count})` })),
    ]
  } catch (error) {
    console.warn('[配送参数] 分组加载失败', error)
  }
}

function reload() {
  Object.keys(edits).forEach(k => delete edits[k])
  fetchList()
  fetchGroups()
}

function handleGroupSelect(keys: (string | number)[]) {
  selectedGroup.value = keys?.[0] == null ? '' : String(keys[0])
  pagination.current = 1
  fetchList()
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.valueType = undefined
  searchForm.scope = undefined
  searchForm.editable = undefined
  searchForm.effect = undefined
  searchForm.updateRange = undefined
  searchForm.configuredOnly = false
  selectedGroup.value = ''
  pagination.current = 1
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 行内编辑（收集改动，统一批量保存） ═══
function onValueChange(record: ConfigItem, value: string) {
  const original = record.configValue == null ? '' : String(record.configValue)
  if (value === original) {
    delete edits[record.configKey]
  } else {
    edits[record.configKey] = value
  }
  record.__edit = value
}

function numValue(record: ConfigItem) {
  const raw = record.__edit ?? record.configValue
  if (raw == null || raw === '') return undefined
  const n = Number(raw)
  return Number.isNaN(n) ? undefined : n
}

function numberPrecision(record: ConfigItem) {
  // 含小数的单位保留 2 位（如 元/km、倍、m³），其余整数
  return ['元/km', '倍', 'm³'].includes(String(record.unit)) ? 2 : 0
}

function rangeText(record: ConfigItem) {
  if (record.options?.length) {
    return record.options.map(o => o.label).join(' / ')
  }
  const min = record.min != null ? Number(record.min) : null
  const max = record.max != null ? Number(record.max) : null
  if (min == null && max == null) return record.unit || '-'
  const base = `${min == null ? '不限' : min} ~ ${max == null ? '不限' : max}`
  return record.unit ? `${base} ${record.unit}` : base
}

// ═══ 批量保存（二次确认 + diff 预览） ═══
function handleBatchSave() {
  const rows = Object.entries(edits)
  if (rows.length === 0) {
    message.warning('没有需要保存的修改')
    return
  }
  const diff = rows.slice(0, 8).map(([key, value]) => `${key} → ${value === '' ? '(空)' : value}`).join('\n')
  Modal.confirm({
    title: '确认保存参数修改',
    content: `将保存 ${rows.length} 项（保存后立即热生效）：\n${diff}${rows.length > 8 ? '\n…' : ''}`,
    okText: '确认保存',
    onOk: async () => {
      saving.value = true
      try {
        // 敏感键留空=不修改（后端语义）；其余键空串=清空
        const items = rows.map(([configKey, configValue]) => ({ configKey, configValue }))
        await configApi.batch(items)
        message.success(`已保存 ${items.length} 项（热生效）`)
        reload()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '保存失败')
      } finally {
        saving.value = false
      }
    },
  })
}

// ═══ 恢复默认（单行 / 本组） ═══
function handleResetOne(record: ConfigItem) {
  Modal.confirm({
    title: '恢复默认值',
    content: `将「${record.name}」恢复为默认值 ${record.defaultValue === '' ? '(空)' : record.defaultValue}？`,
    okText: '恢复默认',
    onOk: async () => {
      try {
        await configApi.reset(record.configKey)
        message.success('已恢复默认')
        reload()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '恢复默认失败')
      }
    },
  })
}

function handleResetGroup() {
  const rows = tableData.value.filter(r => r.group === selectedGroup.value && r.editable !== false)
  if (rows.length === 0) {
    message.warning('当前分组没有可恢复的参数')
    return
  }
  Modal.confirm({
    title: '本组恢复默认',
    content: `将「${groupTree.value.find(g => g.id === selectedGroup.value)?.categoryName || selectedGroup.value}」下 ${rows.length} 项恢复为默认值？未定义默认值的项会被跳过。`,
    okText: '确认恢复',
    onOk: async () => {
      let ok = 0
      let skip = 0
      for (const row of rows) {
        try {
          await configApi.reset(row.configKey)
          ok++
        } catch (e) {
          skip++
        }
      }
      message.success(`已恢复 ${ok} 项${skip ? `，跳过 ${skip} 项（未定义默认值）` : ''}`)
      reload()
    },
  })
}

// ═══ 变更历史 ═══
const historyVisible = ref(false)
const historyLoading = ref(false)
const historyKey = ref('')
const historyList = ref<any[]>([])
const historyColumns = [
  { title: '变更类型', dataIndex: 'changeType', width: 100 },
  { title: '变更前值', dataIndex: 'oldValue', width: 170 },
  { title: '变更后值', dataIndex: 'newValue', width: 170 },
  { title: '操作人', dataIndex: 'changedByName', width: 110 },
  { title: '时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', dataIndex: 'action', width: 130 },
]

async function openHistory(record: ConfigItem) {
  historyKey.value = record.configKey
  historyVisible.value = true
  historyLoading.value = true
  try {
    const res: any = await configApi.history(record.configKey)
    historyList.value = Array.isArray(res) ? res : []
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载变更历史失败')
    historyList.value = []
  } finally {
    historyLoading.value = false
  }
}

function handleRollback(record: any) {
  Modal.confirm({
    title: '回滚配置',
    content: `将「${historyKey.value}」回滚到该历史版本（变更前值）？`,
    okText: '确认回滚',
    onOk: async () => {
      try {
        await configApi.rollback(historyKey.value, record.id)
        message.success('已回滚')
        historyVisible.value = false
        reload()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '回滚失败')
      }
    },
  })
}

// ═══ 导出参数集（JSON 参数清单） ═══
const exportVisible = ref(false)
const exporting = ref(false)
const exportScope = ref<'filtered' | 'all'>('filtered')

function openExportModal() {
  exportScope.value = 'filtered'
  exportVisible.value = true
}

async function handleExportConfirm() {
  exporting.value = true
  try {
    // 导出为全量清单（不分页）：scope=all 时不带筛选条件
    const params: ConfigQuery = exportScope.value === 'all' ? {} : buildParams()
    params.pageNum = undefined
    params.pageSize = undefined
    const payload: ConfigExportPayload = await configApi.exportConfigs(params)
    downloadJson(payload)
    message.success(`已导出 ${payload.count} 项参数（敏感键不含明文）`)
    exportVisible.value = false
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

function downloadJson(payload: ConfigExportPayload) {
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `dms-config-params-${new Date().toISOString().slice(0, 10)}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

// ═══ 导入参数集（JSON）：预览差异 → 确认导入（整体事务） ═══
const importVisible = ref(false)
const importText = ref('')
const importFileName = ref('')
const importOverwrite = ref(true)
const importPreviewing = ref(false)
const importing = ref(false)
const importResult = ref<ConfigImportResult | null>(null)

function openImportModal() {
  resetImport()
  importVisible.value = true
}

function resetImport() {
  importText.value = ''
  importFileName.value = ''
  importOverwrite.value = true
  importResult.value = null
}

/** 选择 JSON 文件 → 读进文本框（本地解析，不走上传接口） */
function handleImportFile(file: File) {
  importFileName.value = file.name
  const reader = new FileReader()
  reader.onload = () => { importText.value = String(reader.result || '') }
  reader.readAsText(file, 'utf-8')
  return false
}

/** 解析导入项：兼容「导出物 {items:[…]}」与裸数组 */
function parseImportItems(): ConfigExportPayload['items'] {
  const raw = importText.value.trim()
  if (!raw) throw new Error('请先选择或粘贴参数集 JSON')
  let parsed: any
  try {
    parsed = JSON.parse(raw)
  } catch (e) {
    throw new Error(`JSON 解析失败：${(e as Error).message}`)
  }
  const items = Array.isArray(parsed) ? parsed : parsed?.items
  if (!Array.isArray(items) || items.length === 0) throw new Error('未找到可导入的 items')
  return items
}

async function handleImportPreview() {
  let items: ConfigExportPayload['items']
  try {
    items = parseImportItems()
  } catch (e: any) {
    message.warning(e.message)
    return
  }
  importPreviewing.value = true
  try {
    importResult.value = await configApi.importConfigs({
      mode: 'PREVIEW', overwrite: importOverwrite.value, items,
    })
    const r = importResult.value
    if (r.failed.length) message.error(`${r.failed.length} 项校验失败，未写入任何参数`)
    else message.success(`预览完成：将变更 ${r.changed} 项`)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '预览失败')
  } finally {
    importPreviewing.value = false
  }
}

async function handleImportApply() {
  let items: ConfigExportPayload['items']
  try {
    items = parseImportItems()
  } catch (e: any) {
    message.warning(e.message)
    return
  }
  if (importResult.value?.failed?.length) {
    message.error('存在校验失败项，请修正后重新预览')
    return
  }
  importing.value = true
  try {
    const r = await configApi.importConfigs({
      mode: 'APPLY', overwrite: importOverwrite.value, items,
    })
    if (r.failed.length) {
      importResult.value = r
      message.error(`${r.failed.length} 项校验失败，已整体拒绝`)
      return
    }
    message.success(`导入完成：变更 ${r.changed} 项、跳过 ${r.skipped} 项（热生效）`)
    importVisible.value = false
    reload()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导入失败')
  } finally {
    importing.value = false
  }
}

// ═══ 页面配置 ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '筛选条件', visible: true },
  { key: 'valueType', label: '参数类型', visible: true },
  { key: 'scope', label: '作用域', visible: true },
  { key: 'editable', label: '是否可改', visible: true },
  { key: 'effect', label: '生效方式', visible: true },
  { key: 'updateTime', label: '更新时间', visible: true },
  { key: 'configuredOnly', label: '仅看已配置', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'save', label: '保存修改', enabled: true },
  { key: 'resetGroup', label: '本组恢复默认', enabled: true },
  { key: 'import', label: '导入参数', enabled: true },
  { key: 'export', label: '导出参数', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
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
  console.error('[配送参数] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchList()
  fetchGroups()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.dirty-tip { font-size: 12px; color: #d46b08; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.value-cell { display: flex; align-items: center; gap: 6px; }
.unit-text { font-size: 12px; color: #8c8c8c; white-space: nowrap; }
.range-text { font-size: 12px; color: #595959; }
.desc-text { font-size: 12px; color: #8c8c8c; display: inline-block; max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tag-gap { margin-left: 4px; }

.import-tip { margin-bottom: 10px; }
.import-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; flex-wrap: wrap; }
.import-file { font-size: 12px; color: #595959; }
.import-text { margin-bottom: 10px; font-family: Consolas, Monaco, monospace; font-size: 12px; }
.import-summary { margin: 10px 0; font-size: 13px; color: #333; }
.import-fail { font-size: 12px; }
.import-reason { margin-left: 6px; font-size: 12px; color: #8c8c8c; }

.btn-add { background: #ff6b35 !important; border-color: #ff6b35 !important; }
.btn-add:hover { background: #e55a2b !important; border-color: #e55a2b !important; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
