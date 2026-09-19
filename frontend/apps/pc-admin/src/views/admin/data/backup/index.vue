<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        备份管理（系统管理 → 数据管理 → 备份管理，菜单 62303）
        · 平台控制台页面；后端控制器 cn.aiedge.datasource.controller.BackupController（前缀 /api/data-source/backup）
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/备份管理开发文档.md
        · 后端能力（2026-09-19 起为真实实现，详见开发文档 §9.3「实现结果」）：
          ① 创建备份 = 真实调用官方 pg_dump（自定义格式）落盘到受控备份目录
             （app.data-maintenance.backup.dir），并把文件路径/大小/终态回写台账；
             整库 dump 耗时不可控，因此**异步执行** —— 接口返回的是「已受理」，
             终态由后台线程写入，必须回读确认；
          ② 恢复 = 真实调用 pg_restore --clean --if-exists，同步返回真实结论。三重闸门：
             记录必须是 success、备份文件必须存在且非空、请求必须带 confirm=true（缺省拒绝）；
          ③ 删除 = 删台账记录 + 删对应磁盘文件（路径必须仍落在备份根目录内，防目录穿越）。
        · 🔴 诚实性红线：本页仍然「调用后回读」核验，绝不把「已受理」写成「已完成」；
          恢复结果只认后端回写的 restoreStatus / restoreMessage，不凭按钮点击推断成功。
          仍未闭环的项（增量备份、备份保留策略、下载产物）见文档 §9.3。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：创建备份 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('create')"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <CloudUploadOutlined /> 创建备份
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

        <!-- ═══ 查询区（数据源走后端；备份名称/状态/时间范围后端不支持，走本地过滤） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('backupName')">
                <span class="search-label">备份名称</span>
                <!-- 后端 /list 只有 dataSourceId/page/pageSize 三个入参 → 本项为本地模糊过滤 -->
                <a-input
                  v-model:value="searchForm.backupName"
                  placeholder="请输入备份名称（本地过滤）"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('dataSourceId')">
                <span class="search-label">数据源</span>
                <!-- 唯一由后端真正支持的查询参数（直接下推 /list?dataSourceId=） -->
                <a-select
                  v-model:value="searchForm.dataSourceId"
                  placeholder="全部数据源"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  :options="dataSourceOptions"
                  @change="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('status')">
                <span class="search-label">状态</span>
                <!-- 后端 /list 无 status 入参 → 本地精确过滤 -->
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('timeRange')">
                <span class="search-label">时间范围</span>
                <!-- 后端 /list 无时间入参 → 本地按 startTime（缺失时兜底 createTime）过滤 -->
                <a-range-picker
                  v-model:value="searchForm.timeRange"
                  size="small"
                  style="width: 240px"
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
            <!-- 后端能力提示：如实标注真实实现与仍存在的边界 -->
            <a-alert
              class="honesty-alert"
              type="info"
              show-icon
              message="后端能力（2026-09-19 起为真实执行）：创建备份走 pg_dump（异步）、恢复走 pg_restore（需二次确认）"
            >
              <template #description>
                <ul class="honesty-list">
                  <li>
                    <b>创建备份</b>：后端真实调用 <code>pg_dump</code>（自定义格式）并落盘，
                    成功写 <b>文件路径 / 大小 / 结束时间</b>、失败写 <b>错误信息</b>。
                    整库 dump 是<b>异步</b>的，接口返回仅表示「已受理」，请点「刷新」查看终态。
                  </li>
                  <li>
                    <b>备份类型</b>：<code>pg_dump</code> 是逻辑备份、不支持增量，
                    选「增量备份」时后端仍按全量执行（返回信息中会写明这一点）。
                  </li>
                  <li>
                    <b>恢复</b>：后端真实调用 <code>pg_restore --clean --if-exists</code>，会<b>覆盖目标库对象</b>。
                    前置条件：备份状态为「成功」、备份文件存在且非空、请求携带显式二次确认开关。
                    结果写回 <b>restoreStatus / restoreTime / restoreMessage</b>，失败绝不谎报成功。
                  </li>
                  <li>
                    <b>删除</b>：同时删除台账记录与磁盘上的备份文件。
                  </li>
                  <li>
                    本页所有操作按钮都在调用后<b>回读列表核验</b>：核验不到可验证变化时，一律按「未发生」提示。
                  </li>
                </ul>
              </template>
            </a-alert>

            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="system-data-backup-table-columns"
              global-config-key="system-data-backup-table-columns"
            >
              <!-- 备份名称 -->
              <template #backupNameCell="{ record }">
                <span>{{ record.backupName || '-' }}</span>
              </template>

              <!-- 数据源 -->
              <template #dataSourceCell="{ record }">
                {{ dataSourceName(record.dataSourceId) }}
              </template>

              <!-- 备份类型（pg_dump 为逻辑备份，不支持增量，如实标注） -->
              <template #backupTypeCell="{ record }">
                <a-tooltip :title="record.backupType === 'incremental' ? 'pg_dump 是逻辑备份、不支持增量：选「增量」时后端仍按全量执行（返回信息中会写明）' : ''">
                  <a-tag :color="record.backupType === 'full' ? 'blue' : record.backupType === 'incremental' ? 'cyan' : 'default'">
                    {{ BACKUP_TYPE_MAP[record.backupType] || record.backupType || '-' }}
                  </a-tag>
                </a-tooltip>
              </template>

              <!-- 大小（由后台线程在 pg_dump 成功后回写） -->
              <template #fileSizeCell="{ record }">
                <a-tooltip :title="record.fileSize ? '' : '尚无产物大小：备份未成功（进行中或已失败）时该列为空'">
                  <span v-if="!record.fileSize" class="cell-empty">-</span>
                  <span v-else>{{ formatSize(record.fileSize) }}</span>
                </a-tooltip>
              </template>

              <!-- 状态（备份本身的状态） -->
              <template #statusCell="{ record }">
                <a-tooltip :title="statusTip(record.status)">
                  <a-tag :color="STATUS_MAP[record.status]?.color || 'default'">
                    {{ STATUS_MAP[record.status]?.label || record.status || '-' }}
                  </a-tag>
                </a-tooltip>
              </template>

              <!-- 开始时间 -->
              <template #startTimeCell="{ record }">
                {{ fmtTime(record.startTime) }}
              </template>

              <!-- 结束时间（终态时由后台线程回写） -->
              <template #endTimeCell="{ record }">
                <span v-if="!record.endTime" class="cell-empty">-</span>
                <span v-else>{{ fmtTime(record.endTime) }}</span>
              </template>

              <!-- 文件路径（服务端生成的备份文件绝对路径；文件名不接受前端输入） -->
              <template #filePathCell="{ record }">
                <span v-if="!record.filePath" class="cell-empty">-</span>
                <a-tooltip v-else :title="record.filePath">
                  <span>{{ record.filePath }}</span>
                </a-tooltip>
              </template>

              <!-- 创建时间 -->
              <template #createTimeCell="{ record }">
                {{ fmtTime(record.createTime) }}
              </template>

              <!-- 操作列 -->
              <template #actionCell="{ record }">
                <a-space :size="0">
                  <!-- 恢复：真实执行 pg_restore。前置条件 = 备份成功 + 有产物文件；不满足直接置灰并写明原因 -->
                  <a-tooltip :title="restoreTip(record)">
                    <a-popconfirm
                      v-if="canRestore(record)"
                      title="恢复会以 --clean 覆盖目标库中的同名对象，且不可撤销。确定继续?"
                      ok-text="确定恢复"
                      cancel-text="取消"
                      @confirm="restoreBackup(record)"
                    >
                      <a-button
                        type="link"
                        size="small"
                        danger
                      >
                        恢复
                      </a-button>
                    </a-popconfirm>
                    <!-- disabled 按钮不触发鼠标事件，需 span 包裹才能弹出 tooltip -->
                    <span v-else>
                      <a-button
                        type="link"
                        size="small"
                        disabled
                      >
                        恢复
                      </a-button>
                    </span>
                  </a-tooltip>
                  <a-popconfirm
                    title="确定删除此备份记录?"
                    @confirm="handleDelete(record)"
                  >
                    <a-button type="link" size="small" danger>
                      删除
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏（本地分页，见 script 注释） ═══ -->
        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="totalCount"
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
        storage-key="system-data-backup-page-config"
        :hide-print-config="true"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 创建备份弹窗（真实 pg_dump，异步执行，弹窗内写明「受理不等于完成」） ═══ -->
      <a-modal
        v-model:open="createVisible"
        title="创建备份"
        width="520px"
        :confirm-loading="creating"
        destroy-on-close
        @ok="handleCreate"
      >
        <a-alert
          type="info"
          show-icon
          style="margin-bottom:12px"
          message="提交后后端会真实执行 pg_dump 并落盘备份文件；整库 dump 耗时不可控，因此后台异步执行，提交成功只代表「已受理」。"
        />
        <a-alert
          v-if="!dataSources.length"
          type="warning"
          show-icon
          style="margin-bottom:12px"
          message="当前没有可用数据源，无法在本页提交备份。请先到「连接管理」登记一个 PostgreSQL 数据源（备份目标的连接信息与口令都来自该数据源）。"
        />
        <a-form
          ref="formRef"
          :model="createForm"
          :rules="createRules"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
        >
          <a-form-item
            label="数据源"
            name="dataSourceId"
          >
            <a-select
              v-model:value="createForm.dataSourceId"
              placeholder="选择要备份的数据源"
              :options="dataSourceOptions"
            />
          </a-form-item>
          <a-form-item
            label="备份名称"
            name="backupName"
          >
            <a-input
              v-model:value="createForm.backupName"
              placeholder="留空则由系统自动生成"
            />
          </a-form-item>
          <a-form-item
            label="备份类型"
            name="backupType"
          >
            <a-select
              v-model:value="createForm.backupType"
              :options="BACKUP_TYPE_OPTIONS"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import {
  CloudUploadOutlined,
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
import { backupApi, dataSourceApi, type BackupRecordItem, type DataSourceItem } from '@/api/admin'

defineOptions({ name: 'AdminDataBackup' })

// ═══ 枚举 ═══
/** 备份状态（与后端 BackupRecord.status 一致；注意后端实际只会写入 running） */
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  pending: { label: '待执行', color: 'default' },
  running: { label: '进行中', color: 'blue' },
  success: { label: '成功', color: 'green' },
  failed: { label: '失败', color: 'red' },
}
const STATUS_OPTIONS = [
  { label: '待执行', value: 'pending' },
  { label: '进行中', value: 'running' },
  { label: '成功', value: 'success' },
  { label: '失败', value: 'failed' },
]
const BACKUP_TYPE_MAP: Record<string, string> = {
  full: '全量备份',
  incremental: '增量备份',
}
const BACKUP_TYPE_OPTIONS = [
  { label: '全量备份', value: 'full' },
  // pg_dump 为逻辑备份、不支持增量：如实提示，避免用户以为两者行为不同
  { label: '增量备份（pg_dump 不支持，按全量执行）', value: 'incremental' },
]

// ═══ 页面配置（查询条件 / 功能按钮） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'backupName', label: '备份名称', visible: true },
  { key: 'dataSourceId', label: '数据源', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'timeRange', label: '时间范围', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'create', label: '创建备份', enabled: true },
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

// ═══ 列定义 ═══
// 序号列承载表头「列配置」齿轮；操作列为固定列
// 纯文本列用 type:'input'，所有自定义渲染列必须 type:'slot' + slotName（插槽透传 column）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 120, fixed: 'left' },
  { key: 'backupName', title: '备份名称', type: 'slot', slotName: 'backupNameCell', width: 220 },
  { key: 'dataSourceId', title: '数据源', type: 'slot', slotName: 'dataSourceCell', width: 150 },
  { key: 'backupType', title: '备份类型', type: 'slot', slotName: 'backupTypeCell', width: 120 },
  { key: 'fileSize', title: '大小', type: 'slot', slotName: 'fileSizeCell', width: 100, align: 'right' },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 110 },
  { key: 'startTime', title: '开始时间', type: 'slot', slotName: 'startTimeCell', width: 160 },
  { key: 'endTime', title: '结束时间', type: 'slot', slotName: 'endTimeCell', width: 160 },
  { key: 'filePath', title: '文件路径', type: 'slot', slotName: 'filePathCell', width: 240 },
  { key: 'errorMessage', title: '错误信息', type: 'input', width: 200 },
  { key: 'createBy', title: '创建人', type: 'input', width: 110 },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'createTimeCell', width: 160 },
]

// ═══ 状态 ═══
const loading = ref(false)
const creating = ref(false)
const createVisible = ref(false)
const allRows = ref<BackupRecordItem[]>([])
const dataSources = ref<DataSourceItem[]>([])

const pagination = reactive({ current: 1, pageSize: 20 })
const searchForm = reactive({
  backupName: '' as string,
  dataSourceId: undefined as number | undefined,
  status: undefined as string | undefined,
  timeRange: undefined as [Dayjs, Dayjs] | undefined,
})

const dataSourceOptions = computed(() => dataSources.value.map(ds => ({ label: ds.name, value: ds.id })))

// ═══ 本地过滤（备份名称 / 状态 / 时间范围 —— 后端 /list 不支持这三个参数） ═══
const filteredRows = computed(() => {
  const kw = searchForm.backupName.trim().toLowerCase()
  const range = searchForm.timeRange
  return allRows.value.filter((r) => {
    if (kw && !String(r.backupName || '').toLowerCase().includes(kw)) return false
    if (searchForm.status && r.status !== searchForm.status) return false
    if (range && range.length === 2) {
      // 后端 startTime 恒有值；仍兜底 createTime，避免历史数据缺列时被误过滤
      const t = dayjs(r.startTime || r.createTime)
      if (!t.isValid()) return false
      if (t.isBefore(range[0].startOf('day')) || t.isAfter(range[1].endOf('day'))) return false
    }
    return true
  })
})
const totalCount = computed(() => filteredRows.value.length)
// 本地分页：后端 list 虽支持 page/pageSize，但要先做本地过滤再分页，故统一由前端切片。
// 用 ref + watch 而非 computed：BillDetailTable 是 defineModel('dataSource')，
// 传只读 computed 在内部回写时会抛 Vue 只读警告。
const tableData = ref<BackupRecordItem[]>([])
watch([filteredRows, () => pagination.current, () => pagination.pageSize], () => {
  const start = (pagination.current - 1) * pagination.pageSize
  tableData.value = filteredRows.value.slice(start, start + pagination.pageSize)
}, { immediate: true })

// ═══ 格式化 ═══
function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'
}
function formatSize(bytes: number | null | undefined): string {
  const n = Number(bytes)
  if (!n || isNaN(n) || n <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let size = n
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(2) + ' ' + units[i]
}
function dataSourceName(id: number | null | undefined): string {
  if (!id) return '-'
  const ds = dataSources.value.find(d => d.id === id)
  return ds ? ds.name : `数据源#${id}`
}
/** 状态列提示：说明各状态的产生路径（均为后端真实写入） */
function statusTip(status: string | undefined): string {
  if (status === 'pending') return '已受理，等待后台线程开始执行 pg_dump'
  if (status === 'running') return '后台正在执行 pg_dump。若长时间停留在此状态，请查看服务端日志（备份目录/客户端工具问题）'
  if (status === 'success') return 'pg_dump 退出码为 0，且落盘文件已回读确认非空'
  if (status === 'failed') return '备份失败，失败原因见「错误信息」列'
  return ''
}

/** 恢复按钮可用性：必须是成功的备份，且台账里有产物路径（后端还会再校验文件真实存在且非空） */
function canRestore(record: BackupRecordItem): boolean {
  return record.status === 'success' && !!record.filePath
}
function restoreTip(record: BackupRecordItem): string {
  if (record.status !== 'success') {
    return `备份状态为「${STATUS_MAP[record.status]?.label || record.status}」，没有可用产物，不能恢复`
  }
  if (!record.filePath) {
    return '该备份台账缺少文件路径（可能是历史数据或文件已被清理），无法恢复'
  }
  if (record.restoreStatus === 'success') {
    return `最近一次恢复成功：${record.restoreMessage || ''}`
  }
  if (record.restoreStatus === 'failed') {
    return `最近一次恢复失败：${record.restoreMessage || '（无原因记录）'}。确认后可重试`
  }
  return '将执行 pg_restore --clean --if-exists 覆盖目标库同名对象（后端强制二次确认，不可撤销）'
}

// ═══ 数据加载 ═══
/**
 * 拉取备份台账。
 * 后端 list 支持 dataSourceId（下推到后端），其余查询条件在前端过滤，
 * 因此这里需要「全量」行数据 —— 用翻页循环取完，避免单次 pageSize=1000 的静默截断
 * （单次请求 records.length 不足 pageSize 或已达 total 即停止；最多 20 页兜底防止死循环）。
 */
async function fetchAllRows(): Promise<BackupRecordItem[]> {
  const PAGE_SIZE = 200
  const MAX_PAGES = 20
  const rows: BackupRecordItem[] = []
  for (let page = 1; page <= MAX_PAGES; page++) {
    const res: any = await backupApi.page({
      dataSourceId: searchForm.dataSourceId,
      page,
      pageSize: PAGE_SIZE,
    })
    const records: BackupRecordItem[] = res?.records || []
    rows.push(...records)
    const total = Number(res?.total) || 0
    if (records.length < PAGE_SIZE || rows.length >= total) break
  }
  return rows
}

async function fetchList() {
  loading.value = true
  try {
    allRows.value = await fetchAllRows()
    clampPage()
  } catch (error: any) {
    // 禁止假数据兜底：出错即清空并显式提示
    console.error('[备份管理] 加载备份台账失败', error)
    message.error(error?.message || '加载备份台账失败')
    allRows.value = []
  } finally {
    loading.value = false
  }
}

/** 过滤/删除后当前页可能越界，收敛到最后一页 */
function clampPage() {
  const maxPage = Math.max(1, Math.ceil(totalCount.value / pagination.pageSize))
  if (pagination.current > maxPage) pagination.current = maxPage
}

async function fetchDataSources() {
  try {
    const res: any = await dataSourceApi.page({ page: 1, pageSize: 200 })
    dataSources.value = res?.records || []
  } catch (error: any) {
    console.error('[备份管理] 加载数据源列表失败', error)
    message.error(error?.message || '加载数据源列表失败')
    dataSources.value = []
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  fetchList()
}
function handleReset() {
  searchForm.backupName = ''
  searchForm.dataSourceId = undefined
  searchForm.status = undefined
  searchForm.timeRange = undefined
  pagination.current = 1
  fetchList()
}
function handleRefresh() {
  fetchList()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
}

// ═══ 创建备份（只登记台账，回读核验后才提示） ═══
const formRef = ref()
const createForm = reactive({
  dataSourceId: undefined as number | undefined,
  backupName: '' as string,
  backupType: 'full' as string,
})
const createRules = {
  dataSourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
}

function openCreate() {
  createForm.dataSourceId = dataSources.value[0]?.id
  createForm.backupName = ''
  createForm.backupType = 'full'
  createVisible.value = true
}

/** 终态集合：台账进入其中之一就说明 dump 已有结论 */
const TERMINAL_STATUS = ['success', 'failed']

/**
 * 在后台 dump 结束前做有限次回读（最多 ~6 秒）。
 * 小库通常几秒内出终态，可立刻给出真实结论；大库仍在跑则如实告知「已受理、请刷新」，
 * 绝不因为没有立刻成功就谎报失败，也绝不提前承诺成功。
 */
async function pollUntilTerminal(recordId: number, maxTries = 5, intervalMs = 1200): Promise<BackupRecordItem | undefined> {
  for (let i = 0; i < maxTries; i++) {
    await new Promise(resolve => setTimeout(resolve, intervalMs))
    await fetchList()
    const hit = allRows.value.find(r => r.id === recordId)
    if (hit && TERMINAL_STATUS.includes(hit.status)) return hit
  }
  return allRows.value.find(r => r.id === recordId)
}

async function handleCreate() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  creating.value = true
  try {
    const created: any = await backupApi.create({
      dataSourceId: createForm.dataSourceId as number,
      backupName: createForm.backupName || undefined,
      backupType: createForm.backupType,
    })
    const createdId = created?.id
    // 🔴 回读核验：确认台账真的落库，而不是只信接口返回
    await fetchList()
    const hit = createdId ? allRows.value.find(r => r.id === createdId) : undefined
    if (!hit) {
      message.error('接口返回成功，但回读列表未找到该备份记录，请刷新后重试')
      return
    }
    createVisible.value = false

    // 后端返回的是「已受理」；再回读一次终态，只说回读到的真实结果
    const settled = await pollUntilTerminal(hit.id)
    if (!settled) {
      message.warning(`备份任务已受理（#${hit.id}），但回读不到该台账，请点「刷新」复核`)
    } else if (settled.status === 'success') {
      message.success(`备份完成（#${settled.id}）：${formatSize(settled.fileSize)} → ${settled.filePath || '（路径未回写）'}`)
    } else if (settled.status === 'failed') {
      message.error(`备份失败（#${settled.id}）：${settled.errorMessage || '后端未记录失败原因'}`)
    } else {
      // 仍在 pending/running：整库 dump 未结束，如实说明，不做任何成功承诺
      message.warning(`备份任务已受理（#${hit.id}），后台仍在执行 pg_dump；请点「刷新」查看最终状态`)
    }
  } catch (error: any) {
    // 失败不做任何乐观假设：保留已加载数据仅提示错误（禁止假数据兜底）
    console.error('[备份管理] 创建备份失败', error)
    message.error(error?.message || '创建备份失败')
  } finally {
    creating.value = false
  }
}

// ═══ 恢复（真实 pg_restore：结论只认后端回写的 restoreStatus/restoreMessage） ═══
async function restoreBackup(record: BackupRecordItem) {
  const beforeRestore = { status: record.restoreStatus, time: record.restoreTime }
  try {
    // confirm=true 是后端强制的二次确认开关（页面上已由 popconfirm 让用户确认过一次）
    await backupApi.restore(record.id, true)
    // 🔴 回读核验：恢复结果必须从台账读回来，不能凭接口返回就宣布成功
    await fetchList()
    const after = allRows.value.find(r => r.id === record.id)
    if (!after) {
      message.error('回读失败：该备份记录已不存在')
      return
    }
    const changed = after.restoreStatus !== beforeRestore.status || after.restoreTime !== beforeRestore.time
    if (!changed) {
      message.error('接口未报错，但回读未发现恢复结果被写入台账（restoreStatus 未变化），请刷新复核')
      return
    }
    if (after.restoreStatus === 'success') {
      message.success(`恢复已完成：${after.restoreMessage || '后端未提供摘要'}`)
    } else if (after.restoreStatus === 'failed') {
      message.error(`恢复失败：${after.restoreMessage || '后端未记录失败原因'}`)
    } else {
      message.warning(`恢复结果状态为「${after.restoreStatus}」，请刷新复核`)
    }
  } catch (error: any) {
    // 后端拒绝（如未通过文件校验/记录非 success）会以 success=false 返回，这里如实展示原因
    console.error('[备份管理] 恢复调用失败', error)
    await fetchList()
    message.error(error?.message || '恢复调用失败')
  }
}

// ═══ 删除（回读确认记录已消失后才提示成功；后端同时删除磁盘备份文件） ═══
async function handleDelete(record: BackupRecordItem) {
  try {
    const result: any = await backupApi.remove(record.id)
    await fetchList()
    if (allRows.value.some(r => r.id === record.id)) {
      message.error('删除接口已返回，但回读列表仍存在该记录，请刷新确认')
      return
    }
    // 后端会一并删除磁盘文件；未找到可删文件时如实说明，不夸大成「文件已清理」
    if (result && result.fileDeleted === false) {
      message.warning('备份记录已删除；未找到对应磁盘文件（可能本就没有产物或已被手工清理）')
    } else {
      message.success('备份记录与磁盘备份文件均已删除')
    }
  } catch (error: any) {
    console.error('[备份管理] 删除备份失败', error)
    message.error(error?.message || '删除备份失败')
  }
}

function handleError(error: Error) {
  console.error('[备份管理] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchList()
  fetchDataSources()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
/* 诚实性提示条：常驻表格上方，不参与滚动 */
.honesty-alert { flex-shrink: 0; margin-bottom: 8px; }
.honesty-list { margin: 0; padding-left: 18px; font-size: 12px; line-height: 20px; }
.cell-empty { color: #999; }
.btn-add { background: #fa8c16; border-color: #fa8c16; }
</style>
