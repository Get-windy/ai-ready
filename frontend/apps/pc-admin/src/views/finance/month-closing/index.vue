<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="false"
      >
        <!-- ═══ 工具栏右侧：月结 / 批量月结 / 反月结 / 刷新（列配置齿轮在表头 rowNo 右上角） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              type="primary"
              :loading="executing"
              @click="handleClose"
            >
              <CheckCircleOutlined /> 月结
            </a-button>
            <a-button
              size="small"
              :loading="batchLoading"
              @click="handleBatchClose"
            >
              <AuditOutlined /> 批量月结
            </a-button>
            <a-button
              size="small"
              danger
              :loading="reopening"
              @click="handleBatchReopen"
            >
              <RollbackOutlined /> 反月结
            </a-button>
            <a-button size="small" @click="handleRefresh">
              <ReloadOutlined /> 刷新
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区：会计年 / 状态 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div class="search-item">
                <span class="search-label">会计年</span>
                <a-select
                  v-model:value="searchParams.periodYear"
                  style="width: 110px"
                  size="small"
                  allow-clear
                  placeholder="全部"
                  @change="handleSearch"
                >
                  <a-select-option :value="undefined">全部</a-select-option>
                  <a-select-option
                    v-for="y in yearOptions"
                    :key="y"
                    :value="y"
                  >{{ y }}年</a-select-option>
                </a-select>
              </div>
              <div class="search-item">
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchParams.status"
                  style="width: 110px"
                  size="small"
                  allow-clear
                  placeholder="全部"
                  @change="handleSearch"
                >
                  <a-select-option :value="undefined">全部</a-select-option>
                  <a-select-option :value="1">未结账</a-select-option>
                  <a-select-option :value="0">已结账</a-select-option>
                </a-select>
              </div>
              <div class="search-item">
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 表格（5 列 + checkbox + 行操作，列配置齿轮在 rowNo 表头） ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="columns"
              :data-source="tableData"
              :loading="loading"
              :pagination="pagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              storage-key="finance-month-closing-columns"
              row-key="id"
              @page-change="handlePageChange"
              @selection-change="handleSelectionChange"
            >
              <template #statusCell="{ record }">
                <a-tag :color="record.status === 1 ? 'default' : 'green'">
                  {{ record.status === 1 ? '未结账' : '已结账' }}
                </a-tag>
              </template>
              <template #actionCell="{ record }">
                <template v-if="record.status === 1">
                  <a-button type="link" size="small" @click="handleCloseRow(record)">月结</a-button>
                </template>
                <template v-else>
                  <a-button type="link" size="small" danger @click="handleReopenRow(record)">反月结</a-button>
                </template>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>

      <!-- ═══ 月结检查结果弹窗 ═══ -->
      <a-modal v-model:open="resultModalOpen" title="月结检查结果" :width="620" :footer="null">
        <a-alert
          v-if="lastResult"
          :type="lastResult.success ? 'success' : 'error'"
          :message="lastResult.message"
          show-icon
          class="result-alert"
        />
        <template
          v-if="lastResult && lastResult.carryOverVoucherNo"
        >
          <a-descriptions size="small" :column="1" class="carry-desc">
            <a-descriptions-item label="结转凭证">
              <a-tag color="blue">{{ lastResult.carryOverVoucherNo }}</a-tag>
            </a-descriptions-item>
          </a-descriptions>
        </template>
        <div
          v-for="check in (lastResult?.checks || [])"
          :key="check.checkCode"
          class="check-item"
        >
          <a-tag :color="check.passed ? 'green' : 'red'">
            {{ check.passed ? '通过' : '未通过' }}
          </a-tag>
          <span class="check-name">{{ check.checkName }}</span>
          <span class="check-detail">{{ check.detail || '-' }}</span>
        </div>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  RollbackOutlined,
  ReloadOutlined,
  AuditOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { accountingPeriodApi, monthClosingApi, type MonthClosingResult } from '@/api/finance'

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const yearOptions = ref<number[]>([new Date().getFullYear()])

// ═══ 操作状态 ═══
const executing = ref(false)
const batchLoading = ref(false)
const reopening = ref(false)

// ═══ 搜索参数 ═══
const searchParams = reactive<Record<string, any>>({
  periodYear: undefined,
  status: undefined,
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 勾选结果 ═══
const selectedRows = ref<any[]>([])

// ═══ 月结结果弹窗 ═══
const resultModalOpen = ref(false)
const lastResult = ref<MonthClosingResult | null>(null)

// ═══ 列定义（rowNo 首列内置【列配置】齿轮；操作列锁定不进配置） ═══
const columns: any[] = [
  { title: '', key: 'rowCheckbox', type: 'checkbox', width: 44, fixed: 'left' },
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '会计年', field: 'periodYear', key: 'periodYear', width: 90, align: 'center' },
  { title: '会计月', field: 'periodMonth', key: 'periodMonth', width: 90, align: 'center' },
  { title: '开始日期', field: 'startDate', key: 'startDate', width: 130 },
  { title: '结束日期', field: 'endDate', key: 'endDate', width: 130 },
  { title: '状态', field: 'status', key: 'status', width: 110, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '操作', key: 'action', type: 'action', width: 120, fixed: 'right', slotName: 'actionCell' },
]

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: pagination.current, size: pagination.pageSize }
    if (searchParams.periodYear !== undefined && searchParams.periodYear !== null && searchParams.periodYear !== '') {
      params.periodYear = searchParams.periodYear
    }
    if (searchParams.status !== undefined && searchParams.status !== null && searchParams.status !== '') {
      params.status = searchParams.status
    }
    const res: any = await accountingPeriodApi.getPage(params)
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0

    // 动态收集年份选项
    const years = new Set<number>(yearOptions.value)
    tableData.value.forEach((r: any) => {
      if (r.periodYear) years.add(Number(r.periodYear))
    })
    yearOptions.value = Array.from(years).sort((a, b) => b - a)
  } catch (error: any) {
    console.warn('[月结] 获取期间列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// ═══ 事件处理 ═══
function handleSearch() {
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
function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

// ═══ 月结 ═══
async function doClose(periodCode: string) {
  const res = await monthClosingApi.execute(periodCode)
  lastResult.value = res
  resultModalOpen.value = true
  if (res?.success) {
    message.success(res.message || '月结成功，期间已关闭')
  } else {
    message.warning(res?.message || '月结检查未通过，期间未关闭')
  }
  fetchData()
  return res
}

/** 行级月结（未结账期间） */
async function handleCloseRow(record: any) {
  if (executing.value) return
  executing.value = true
  try {
    await doClose(record.periodCode)
  } catch (e) {
    console.warn('[月结] 执行月结失败', e)
  } finally {
    executing.value = false
  }
}

/** 顶部【月结】：对勾选的未结账期间逐个执行 */
async function handleClose() {
  const targets = selectedRows.value.filter(r => r.status === 1)
  if (targets.length === 0) {
    message.warning('请勾选未结账的会计期间')
    return
  }
  if (executing.value) return
  executing.value = true
  try {
    let last: MonthClosingResult | null = null
    for (const r of targets) {
      last = await doClose(r.periodCode)
    }
    if (last) {
      lastResult.value = last
      resultModalOpen.value = false
    }
  } catch (e) {
    console.warn('[月结] 执行月结失败', e)
  } finally {
    executing.value = false
  }
}

/** 顶部【批量月结】：后端批量执行 */
async function handleBatchClose() {
  const targets = selectedRows.value.filter(r => r.status === 1)
  if (targets.length === 0) {
    message.warning('请勾选未结账的会计期间')
    return
  }
  if (batchLoading.value) return
  batchLoading.value = true
  try {
    const codes = targets.map(r => r.periodCode)
    const res = await monthClosingApi.batchExecute(codes)
    const failed = (res || []).filter(r => !r.success)
    const successCount = (res || []).filter(r => r.success).length
    if (failed.length === 0) {
      message.success(`批量月结成功，共 ${successCount} 个期间`)
    } else {
      message.warning(`批量月结完成：成功 ${successCount} 个，失败 ${failed.length} 个`)
    }
    // 展示最后一个检查结果
    lastResult.value = (res || []).find(r => r.success) || (res || []).find(r => !r.success) || null
    resultModalOpen.value = true
    fetchData()
  } catch (e: any) {
    console.warn('[月结] 批量月结失败', e)
    message.error(e?.response?.data?.message || '批量月结失败')
  } finally {
    batchLoading.value = false
  }
}

// ═══ 反月结 ═══
async function doReopen(periodCode: string) {
  const res = await monthClosingApi.reopen(periodCode)
  message.success(res?.message || '反月结成功，期间已重新开启')
  lastResult.value = null
  resultModalOpen.value = false
  fetchData()
  return res
}

/** 行级反月结（已结账期间） */
async function handleReopenRow(record: any) {
  if (reopening.value) return
  reopening.value = true
  try {
    await doReopen(record.periodCode)
  } catch (e) {
    console.warn('[月结] 反月结失败', e)
  } finally {
    reopening.value = false
  }
}

/** 顶部【反月结】：对勾选的已结账期间逐个重新开启 */
async function handleBatchReopen() {
  const targets = selectedRows.value.filter(r => r.status === 0)
  if (targets.length === 0) {
    message.warning('请勾选已结账的会计期间')
    return
  }
  if (reopening.value) return
  reopening.value = true
  try {
    for (const r of targets) {
      await monthClosingApi.reopen(r.periodCode)
    }
    message.success(`反月结成功，共开启 ${targets.length} 个期间`)
    fetchData()
  } catch (e) {
    console.warn('[月结] 反月结失败', e)
  } finally {
    reopening.value = false
  }
}

function handleError(error: Error) {
  console.error('[月结] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.search-area {
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.search-item :deep(.ant-input),
.search-item :deep(.ant-select) {
  font-size: 13px;
}
.search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}
.table-area {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.result-alert {
  margin-bottom: 12px;
}
.carry-desc {
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
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
