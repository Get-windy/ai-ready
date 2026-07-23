<template>
  <ErrorBoundary>
    <PageContainer title="总账月结">
      <!-- ═══ 期间选择 + 操作区 ═══ -->
      <div class="section-card">
        <a-form layout="inline">
          <a-form-item label="会计期间">
            <a-select
              v-model:value="selectedPeriod"
              :options="periodOptions"
              :loading="periodsLoading"
              placeholder="请选择会计期间"
              style="width: 220px"
              @change="handlePeriodChange"
            />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                :loading="executing"
                :disabled="!selectedPeriod || statusInfo.status === 0"
                @click="handleExecute"
              >
                <template #icon>
                  <AuditOutlined />
                </template>执行月结检查
              </a-button>
              <a-popconfirm
                title="确认反月结？"
                :description="`期间 ${selectedPeriod} 将重新开启，可继续录入/过账凭证`"
                ok-text="反月结"
                cancel-text="取消"
                @confirm="handleReopen"
              >
                <a-button
                  danger
                  :loading="reopening"
                  :disabled="!selectedPeriod || statusInfo.status !== 0"
                >
                  <template #icon>
                    <RollbackOutlined />
                  </template>反月结
                </a-button>
              </a-popconfirm>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- ═══ 期间状态卡 ═══ -->
      <div class="section-card">
        <div class="section-title">
          期间状态
        </div>
        <a-skeleton
          v-if="statusLoading"
          active
          :paragraph="{ rows: 1 }"
        />
        <a-empty
          v-else-if="!selectedPeriod"
          description="请选择会计期间"
          style="padding: 24px 0"
        />
        <a-empty
          v-else-if="statusInfo.exists === false"
          :description="`期间 ${selectedPeriod} 不存在，请先在「设置 > 会计期间」中新增`"
          style="padding: 24px 0"
        />
        <a-descriptions
          v-else
          :column="{ xs: 1, sm: 2, md: 3 }"
          size="small"
        >
          <a-descriptions-item label="期间编码">
            {{ statusInfo.periodCode || selectedPeriod }}
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusInfo.status === 1 ? 'green' : 'default'">
              {{ statusInfo.status === 1 ? '开启' : '关闭（已月结）' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="起止日期">
            {{ statusInfo.startDate || '-' }} 至 {{ statusInfo.endDate || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="月结操作人">
            {{ statusInfo.closedBy || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="月结时间">
            {{ statusInfo.closedTime || '-' }}
          </a-descriptions-item>
        </a-descriptions>
      </div>

      <!-- ═══ 月结检查结果 ═══ -->
      <div
        v-if="closingResult"
        class="section-card"
      >
        <div class="section-title">
          检查结果
        </div>
        <a-alert
          :type="closingResult.success ? 'success' : 'error'"
          :message="closingResult.message"
          show-icon
          class="result-alert"
        />
        <div
          v-for="check in closingResult.checks || []"
          :key="check.checkCode"
          class="check-item"
        >
          <a-tag :color="check.passed ? 'green' : 'red'">
            {{ check.passed ? '通过' : '未通过' }}
          </a-tag>
          <span class="check-name">{{ check.checkName }}</span>
          <span class="check-detail">{{ check.detail || '-' }}</span>
        </div>
      </div>

      <!-- ═══ 月结日志 ═══ -->
      <div class="section-card">
        <div class="section-title">
          月结日志
          <a-button
            size="small"
            class="reload-btn"
            @click="loadLogs"
          >
            <template #icon>
              <ReloadOutlined />
            </template>刷新
          </a-button>
        </div>
        <a-table
          :columns="logColumns"
          :data-source="logs"
          :loading="logsLoading"
          :pagination="logPagination"
          row-key="id"
          size="small"
          :locale="{ emptyText: '暂无月结日志' }"
          @change="handleLogTableChange"
        >
          <template #bodyCell="{ column, record, text }">
            <template v-if="column.dataIndex === 'action'">
              <a-tag :color="text === 'close' ? 'green' : 'orange'">
                {{ text === 'close' ? '月结' : '反月结' }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'checkResult'">
              <template v-if="parseCheckPassed(text) !== null">
                <a-tag :color="parseCheckPassed(text) ? 'green' : 'red'">
                  {{ parseCheckPassed(text) ? '检查通过' : '检查未通过' }}
                </a-tag>
              </template>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.dataIndex === 'operatorName'">
              {{ record.operatorName || record.operatorId || '-' }}
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { ReloadOutlined, AuditOutlined, RollbackOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import {
  accountingPeriodApi,
  monthClosingApi,
  type AccountingPeriod,
  type MonthClosingResult,
  type MonthClosingLog
} from '@/api/finance'

// ═══ 期间下拉 ═══
const periods = ref<AccountingPeriod[]>([])
const periodsLoading = ref(false)
const selectedPeriod = ref<string>()

const periodOptions = computed(() =>
  periods.value.map(p => ({
    label: `${p.periodCode}（${p.status === 1 ? '开启' : '已关闭'}）`,
    value: p.periodCode
  }))
)

// ═══ 期间状态 ═══
const statusInfo = ref<Record<string, any>>({})
const statusLoading = ref(false)

// ═══ 月结执行/反月结 ═══
const executing = ref(false)
const reopening = ref(false)
const closingResult = ref<MonthClosingResult | null>(null)

// ═══ 月结日志 ═══
const logs = ref<MonthClosingLog[]>([])
const logsLoading = ref(false)
const logPage = reactive({ current: 1, pageSize: 20, total: 0 })

const logColumns: any[] = [
  { title: '期间编码', dataIndex: 'periodCode', key: 'periodCode', width: 110 },
  { title: '操作类型', dataIndex: 'action', key: 'action', width: 100 },
  { title: '检查结果', dataIndex: 'checkResult', key: 'checkResult', width: 110 },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 120 },
  { title: '操作时间', dataIndex: 'createTime', key: 'createTime', width: 180 }
]

const logPagination = computed(() => ({
  current: logPage.current,
  pageSize: logPage.pageSize,
  total: logPage.total,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`
}))

/** 解析日志中的检查结果快照（JSON字符串），失败/为空返回 null */
function parseCheckPassed(json?: string): boolean | null {
  if (!json) return null
  try {
    const obj = JSON.parse(json)
    return typeof obj?.passed === 'boolean' ? obj.passed : null
  } catch {
    return null
  }
}

// ═══ 数据加载 ═══
async function loadPeriods() {
  periodsLoading.value = true
  try {
    const res = await accountingPeriodApi.getList()
    periods.value = Array.isArray(res) ? res : []
    // 默认选中当前月所在期间，无则选最新期间（列表按期间编码升序）
    if (periods.value.length && !selectedPeriod.value) {
      const currentCode = dayjs().format('YYYY-MM')
      const hit = periods.value.find(p => p.periodCode === currentCode)
      selectedPeriod.value = (hit || periods.value[periods.value.length - 1]).periodCode
    }
  } catch (e) {
    periods.value = []
    console.warn('[月结] 会计期间列表获取失败', e)
  } finally {
    periodsLoading.value = false
  }
}

async function loadStatus() {
  if (!selectedPeriod.value) {
    statusInfo.value = {}
    return
  }
  statusLoading.value = true
  try {
    const res = await monthClosingApi.getStatus(selectedPeriod.value)
    statusInfo.value = res && typeof res === 'object' ? res : {}
  } catch (e) {
    statusInfo.value = {}
    console.warn('[月结] 期间状态获取失败', e)
  } finally {
    statusLoading.value = false
  }
}

async function loadLogs() {
  logsLoading.value = true
  try {
    const res: any = await monthClosingApi.getLogsPage({
      periodCode: selectedPeriod.value || undefined,
      page: logPage.current,
      size: logPage.pageSize
    })
    logs.value = res?.records || []
    logPage.total = Number(res?.total) || 0
  } catch (e) {
    logs.value = []
    logPage.total = 0
    console.warn('[月结] 月结日志获取失败', e)
  } finally {
    logsLoading.value = false
  }
}

function handlePeriodChange() {
  closingResult.value = null
  logPage.current = 1
  loadStatus()
  loadLogs()
}

function handleLogTableChange(pag: { current?: number; pageSize?: number }) {
  logPage.current = pag.current || 1
  logPage.pageSize = pag.pageSize || 20
  loadLogs()
}

// ═══ 月结操作 ═══
async function handleExecute() {
  if (!selectedPeriod.value) return
  executing.value = true
  closingResult.value = null
  try {
    const res = await monthClosingApi.execute(selectedPeriod.value)
    closingResult.value = res
    if (res?.success) {
      message.success(res.message || '月结成功，期间已关闭')
    } else {
      message.warning(res?.message || '月结检查未通过，期间未关闭')
    }
    loadStatus()
    loadLogs()
    loadPeriods()
  } catch (e) {
    console.warn('[月结] 执行月结失败', e)
  } finally {
    executing.value = false
  }
}

async function handleReopen() {
  if (!selectedPeriod.value) return
  reopening.value = true
  try {
    const res = await monthClosingApi.reopen(selectedPeriod.value)
    message.success(res?.message || '反月结成功，期间已重新开启')
    closingResult.value = null
    loadStatus()
    loadLogs()
    loadPeriods()
  } catch (e) {
    console.warn('[月结] 反月结失败', e)
  } finally {
    reopening.value = false
  }
}

onMounted(async () => {
  await loadPeriods()
  loadStatus()
  loadLogs()
})
</script>

<style scoped>
.section-card {
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}
.reload-btn {
  margin-left: 12px;
}
.result-alert {
  margin-bottom: 12px;
}
.check-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
.check-item:last-child {
  border-bottom: none;
}
.check-name {
  font-weight: 500;
  white-space: nowrap;
}
.check-detail {
  color: #666;
  word-break: break-all;
}
</style>
