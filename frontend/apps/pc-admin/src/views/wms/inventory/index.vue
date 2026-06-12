<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>WMS / 库存查询</a-breadcrumb-item>
          </a-breadcrumb>
          <h2>库存查询</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge"><SyncOutlined /> {{ autoRefreshCountdown }}s</span>
          <a-button size="small" :loading="loading" @click="wms.debounce('refresh', wms.fetchData)">
            <ReloadOutlined /> 刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
        </div>
      </div>
    </template>

    <template #filter>
      <SearchBar :fields="searchFields" :loading="loading" @search="handleSearch" @reset="handleReset" />
    </template>

    <template #default>
      <div class="page-body">
        <div class="stat-cards" style="margin-bottom: 12px;">
          <div class="stat-card" style="border-top:3px solid #1890ff">
            <div class="stat-value" style="color:#1890ff">{{ stats.totalQty }}</div>
            <div class="stat-label">总库存量</div>
          </div>
          <div class="stat-card" style="border-top:3px solid #52c41a">
            <div class="stat-value" style="color:#52c41a">{{ stats.availableQty }}</div>
            <div class="stat-label">可用库存</div>
          </div>
          <div class="stat-card" style="border-top:3px solid #faad14">
            <div class="stat-value" style="color:#faad14">{{ stats.frozenQty }}</div>
            <div class="stat-label">冻结库存</div>
          </div>
          <div class="stat-card" style="border-top:3px solid #722ed1">
            <div class="stat-value" style="color:#722ed1">{{ pagination.total }}</div>
            <div class="stat-label">SKU种类</div>
          </div>
        </div>

        <a-skeleton active :loading="loading && dataList.length === 0">
        <a-table
          :dataSource="dataList"
          :columns="columns"
          :loading="loading"
          :pagination="pagination"
          rowKey="id"
          size="small"
          bordered
          @change="handleTableChange"
        >
          <template #emptyText>
            <div class="empty-state-wrapper">
              <template v-if="hasError">
                <WarningOutlined class="empty-state-icon" style="color: #faad14" />
                <p class="empty-state-text">加载失败</p>
                <a-button type="primary" size="small" @click="wms.debounce('refresh', wms.fetchData)" class="empty-state-action">
                  <ReloadOutlined /> 重试
                </a-button>
              </template>
              <template v-else>
                <InboxOutlined class="empty-state-icon" />
                <p class="empty-state-text">暂无库存数据</p>
              </template>
            </div>
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'quantity'">
              <span class="qty-cell">{{ record.quantity || 0 }}</span>
            </template>
            <template v-if="column.dataIndex === 'availableQty'">
              <span :style="{ color: (record.availableQty || 0) <= 0 ? '#ff4d4f' : '#52c41a', fontWeight: 600 }">
                {{ record.availableQty || 0 }}
              </span>
            </template>
            <template v-if="column.dataIndex === 'frozenQty'">
              <span class="qty-cell frozen">{{ record.frozenQty || 0 }}</span>
            </template>
            <template v-if="column.dataIndex === 'action'">
              <a-space :size="4">
                <a-tooltip title="查看日志"><a-button v-permission="'wms:inventory:view-log'" type="link" size="small" @click="handleViewLog(record as any)"><HistoryOutlined /></a-button></a-tooltip>
              </a-space>
            </template>
          </template>
        </a-table>
        </a-skeleton>
      </div>
    </template>
  </PageContainer></ErrorBoundary>

  <!-- 库存日志弹窗 -->
  <a-modal
    v-model:open="logVisible"
    title="库存变动日志"
    width="800px"
    :footer="null"
    @cancel="logVisible = false"
  >
    <a-table
      :dataSource="logData"
      :columns="logColumns"
      :loading="logLoading"
      :pagination="false as any"
      size="small"
      rowKey="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'changeQty'">
          <span :style="{ color: record.changeQty > 0 ? '#52c41a' : '#ff4d4f', fontWeight: 600 }">
            {{ record.changeQty > 0 ? '+' : '' }}{{ record.changeQty }}
          </span>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { inventoryApi, type WmsInventory, type WmsInventoryLog } from '@/api/wms/inventory'
import { useWmsTable } from '@/composables/useWmsTable'
import {
  ReloadOutlined, SyncOutlined, HistoryOutlined,
  WarningOutlined, InboxOutlined,
} from '@ant-design/icons-vue'

function handleError(err: any) { console.warn('[WMS库存]', err) }
const hasError = ref(false)

const columns = [
  { title: '产品编码', dataIndex: 'productCode', width: 130 },
  { title: '产品名称', dataIndex: 'productName', width: 160, ellipsis: true },
  { title: '规格', dataIndex: 'productSpec', width: 100 },
  { title: '仓库', dataIndex: 'warehouseName', width: 120 },
  { title: '货位', dataIndex: 'locationCode', width: 120 },
  { title: '批次号', dataIndex: 'batchNo', width: 130 },
  { title: '库存量', dataIndex: 'quantity', width: 90 },
  { title: '可用量', dataIndex: 'availableQty', width: 90 },
  { title: '冻结量', dataIndex: 'frozenQty', width: 80 },
  { title: '锁定量', dataIndex: 'lockedQty', width: 80 },
  { title: '最后入库', dataIndex: 'lastInboundTime', width: 160 },
  { title: '操作', dataIndex: 'action', width: 80, fixed: 'right' },
] as any

const logColumns = [
  { title: '时间', dataIndex: 'createTime', width: 160 },
  { title: '变动类型', dataIndex: 'changeType', width: 100 },
  { title: '变动数量', dataIndex: 'changeQty', width: 90 },
  { title: '变动前', dataIndex: 'beforeQty', width: 80 },
  { title: '变动后', dataIndex: 'afterQty', width: 80 },
  { title: '来源单号', dataIndex: 'sourceNo', width: 140 },
  { title: '操作人', dataIndex: 'operatorName', width: 100 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
]

const searchFields: SearchField[] = [
  { name: 'productCode', label: '产品编码', type: 'input', placeholder: '请输入产品编码' },
  { name: 'productName', label: '产品名称', type: 'input', placeholder: '请输入产品名称' },
  { name: 'warehouseName', label: '仓库', type: 'input', placeholder: '请输入仓库名称' },
  { name: 'locationCode', label: '货位', type: 'input', placeholder: '请输入货位编号' },
]

const wms = useWmsTable({
  fetchFn: inventoryApi.page as any,
  defaultPageSize: 20,
  refreshInterval: 30,
  shortcuts: { f5: 'refresh' },
})

const { tableData: dataList, loading, pagination, searchParams, lastUpdateTime, autoRefreshCountdown } = wms

const stats = computed(() => {
  const totalQty = dataList.reduce((s, r) => s + (r.quantity || 0), 0)
  const availableQty = dataList.reduce((s, r) => s + (r.availableQty || 0), 0)
  const frozenQty = dataList.reduce((s, r) => s + (r.frozenQty || 0), 0)
  return { totalQty, availableQty, frozenQty }
})

// ── 库存日志 ──
const logVisible = ref(false)
const logLoading = ref(false)
const logData = ref<WmsInventoryLog[]>([])

async function handleViewLog(record: WmsInventory) {
  logVisible.value = true
  logLoading.value = true
  try {
    const res = await inventoryApi.logByProduct(record.productId) as any
    logData.value = Array.isArray(res) ? res : res?.data || []
  } catch (err) {
    console.warn('[WMS库存] 获取日志失败', err)
    message.error('获取库存日志失败')
    logData.value = []
  } finally {
    logLoading.value = false
  }
}

function handleSearch(formData: Record<string, any>) {
  Object.assign(searchParams, formData)
  wms.handleSearch()
}

function handleReset() {
  Object.keys(searchParams).forEach(k => { searchParams[k] = undefined })
  wms.handleReset()
}

function handleTableChange(pag: any) {
  wms.handlePageChange(pag.current, pag.pageSize)
}

// ── 生命周期（Composable 已处理定时器/快捷键） ──
onUnmounted(() => { /* useWmsTable handles cleanup */ })
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header__left { display: flex; align-items: center; gap: 12px; }
.page-header__left h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
.update-time { font-size: 12px; color: #999; }
.auto-refresh-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; padding: 2px 8px; border-radius: 4px; background: #f5f7fa; user-select: none; }
.page-body { padding: 0; }
.stat-card { background: #fff; border-radius: 8px; padding: 14px 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.stat-value { font-size: 22px; font-weight: 700; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace; line-height: 1.2; }
.stat-label { font-size: 13px; color: #666; margin-top: 4px; }
.stat-cards { display: flex; gap: 12px; }
.stat-card { flex: 1; }
.qty-cell { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
.qty-cell.frozen { color: #faad14; }
:deep(.ant-input-sm), :deep(.ant-input-number-sm), :deep(.ant-select-single.ant-select-sm .ant-select-selector), :deep(.ant-picker-small), :deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 空状态 ──────────────────────── */
.empty-state-wrapper { display: flex; flex-direction: column; align-items: center; padding: 48px 0; }
.empty-state-icon { font-size: 48px; color: #d9d9d9; }
.empty-state-text { color: #999; margin-top: 12px; }
.empty-state-action { margin-top: 12px; }

/* ── vxe-table 表头 2px 边框 ─────── */
:deep(.vxe-table .vxe-header--row) { border-top: 2px solid #e8e8e8; }
</style>
