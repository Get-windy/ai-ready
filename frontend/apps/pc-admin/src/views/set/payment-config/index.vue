<template>
  <ErrorBoundary>
    <PageContainer title="支付配置">
      <a-alert
        type="warning"
        show-icon
        class="tip-alert"
        message="支付渠道列表来自后端 /api/payment/channels 实时数据；渠道参数（商户号、密钥、回调地址）后端暂无专用配置接口，暂存至系统参数（/api/config，configGroup=payment）。"
      />

      <!-- 支付渠道 -->
      <div class="content-card">
        <div class="section-header">
          <span class="section-title">支付渠道</span>
          <div class="section-actions">
            <span class="amount-label">按交易金额筛选（元）</span>
            <a-input-number
              v-model:value="queryAmount"
              :min="0.01"
              :precision="2"
              style="width: 140px"
            />
            <a-button
              type="primary"
              @click="loadChannels"
            >
              <template #icon>
                <ReloadOutlined />
              </template>
              查询可用渠道
            </a-button>
          </div>
        </div>
        <a-table
          :columns="channelColumns"
          :data-source="channelList"
          :loading="channelLoading"
          :pagination="false"
          row-key="code"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <a-tag :color="channelTag(record.code).color">
                {{ record.name || channelTag(record.code).name }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'limit'">
              {{ formatMoney(record.minAmount) }} ~ {{ formatMoney(record.maxAmount) }}
            </template>
            <template v-else-if="column.key === 'available'">
              <a-tag :color="record.available ? 'success' : 'default'">
                {{ record.available ? '可用' : '不可用' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button
                type="link"
                size="small"
                @click="openParamDrawer(record)"
              >
                参数配置
              </a-button>
            </template>
          </template>
        </a-table>
      </div>

      <!-- 支付单据 -->
      <div class="content-card">
        <a-tabs v-model:activeKey="activeTab">
          <a-tab-pane
            key="request"
            tab="支付请求"
          >
            <a-table
              :columns="requestColumns"
              :data-source="requestData"
              :loading="requestLoading"
              :pagination="requestPagination"
              row-key="id"
              size="small"
              @change="handleRequestTableChange"
            >
              <template #bodyCell="{ column, record, text }">
                <template v-if="column.key === 'amount'">
                  {{ formatMoney(text) }}
                </template>
                <template v-else-if="column.key === 'channel'">
                  <a-tag :color="channelTag(text).color">
                    {{ channelTag(text).name }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'status'">
                  <a-tag :color="statusTag(text).color">
                    {{ statusTag(text).text }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'createTime'">
                  {{ formatTime(text) }}
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane
            key="record"
            tab="支付记录"
          >
            <a-table
              :columns="recordColumns"
              :data-source="recordData"
              :loading="recordLoading"
              :pagination="recordPagination"
              row-key="id"
              size="small"
              @change="handleRecordTableChange"
            >
              <template #bodyCell="{ column, record, text }">
                <template v-if="column.key === 'amount'">
                  {{ formatMoney(text) }}
                </template>
                <template v-else-if="column.key === 'channel'">
                  <a-tag :color="channelTag(text).color">
                    {{ channelTag(text).name }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'status'">
                  <a-tag :color="statusTag(text).color">
                    {{ statusTag(text).text }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'createTime'">
                  {{ formatTime(text) }}
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </div>
    </PageContainer>

    <!-- 渠道参数配置抽屉 -->
    <a-drawer
      v-model:open="paramVisible"
      :title="`渠道参数配置 - ${currentChannel?.name || currentChannel?.code || ''}`"
      width="480px"
    >
      <a-spin :spinning="paramLoading">
        <a-form
          :model="paramForm"
          layout="vertical"
        >
          <a-form-item label="APP ID">
            <a-input
              v-model:value="paramForm.appId"
              placeholder="渠道分配的 APP ID"
            />
          </a-form-item>
          <a-form-item label="商户号">
            <a-input
              v-model:value="paramForm.merchantNo"
              placeholder="商户号 / MCH ID"
            />
          </a-form-item>
          <a-form-item label="API 密钥">
            <a-input-password
              v-model:value="paramForm.appSecret"
              placeholder="API 密钥 / API Secret"
            />
          </a-form-item>
          <a-form-item label="异步通知 URL">
            <a-input
              v-model:value="paramForm.notifyUrl"
              placeholder="https://..."
            />
          </a-form-item>
          <a-form-item label="是否启用">
            <a-switch v-model:checked="paramForm.enabled" />
          </a-form-item>
          <a-button
            type="primary"
            block
            :loading="paramSaving"
            @click="saveParam"
          >
            保存参数
          </a-button>
        </a-form>
      </a-spin>
    </a-drawer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import {
  paymentApi,
  paymentChannelConfigApi,
  PAYMENT_CHANNEL_MAP,
  PAYMENT_STATUS_MAP,
  type ChannelInfo,
  type PaymentRequest,
  type PaymentRecord,
  type PaymentChannelParam
} from '@/api/payment'

// ═══ 支付渠道 ═══
const channelLoading = ref(false)
const channelList = ref<ChannelInfo[]>([])
const queryAmount = ref<number>(1)

const channelColumns: any[] = [
  { title: '渠道编码', dataIndex: 'code', key: 'code', width: 120 },
  { title: '渠道名称', dataIndex: 'name', key: 'name', width: 140 },
  { title: '单笔限额（元）', key: 'limit', width: 200 },
  { title: '状态', dataIndex: 'available', key: 'available', width: 100 },
  { title: '操作', key: 'action', width: 110, fixed: 'right' }
]

function channelTag(code: string): { name: string; color: string } {
  return PAYMENT_CHANNEL_MAP[String(code).toUpperCase()] || { name: code || '-', color: 'default' }
}

async function loadChannels() {
  channelLoading.value = true
  try {
    const res: any = await paymentApi.getChannels(queryAmount.value || 1)
    channelList.value = Array.isArray(res) ? res : res?.data || []
  } catch (e) {
    console.warn('[支付配置] 渠道列表获取失败', e)
  } finally {
    channelLoading.value = false
  }
}

// ═══ 渠道参数配置 ═══
const paramVisible = ref(false)
const paramLoading = ref(false)
const paramSaving = ref(false)
const currentChannel = ref<ChannelInfo | null>(null)

const paramForm = reactive<Required<PaymentChannelParam>>({
  appId: '',
  merchantNo: '',
  appSecret: '',
  notifyUrl: '',
  enabled: true
})

async function openParamDrawer(record: any) {
  const channel = record as ChannelInfo
  currentChannel.value = channel
  paramForm.appId = ''
  paramForm.merchantNo = ''
  paramForm.appSecret = ''
  paramForm.notifyUrl = ''
  paramForm.enabled = true
  paramVisible.value = true
  paramLoading.value = true
  try {
    const saved = await paymentChannelConfigApi.load(record.code)
    if (saved) {
      paramForm.appId = saved.appId || ''
      paramForm.merchantNo = saved.merchantNo || ''
      paramForm.appSecret = saved.appSecret || ''
      paramForm.notifyUrl = saved.notifyUrl || ''
      paramForm.enabled = saved.enabled !== false
    }
  } catch (e) {
    console.warn('[支付配置] 渠道参数读取失败', e)
  } finally {
    paramLoading.value = false
  }
}

async function saveParam() {
  if (!currentChannel.value) return
  paramSaving.value = true
  try {
    const ok = await paymentChannelConfigApi.save(currentChannel.value.code, { ...paramForm })
    if (ok) {
      message.success('保存成功')
      paramVisible.value = false
    } else {
      message.error('保存失败，请检查系统参数权限')
    }
  } finally {
    paramSaving.value = false
  }
}

// ═══ 支付请求 / 支付记录 ═══
const activeTab = ref('request')
const requestLoading = ref(false)
const recordLoading = ref(false)
const requestData = ref<PaymentRequest[]>([])
const recordData = ref<PaymentRecord[]>([])
const requestPagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })
const recordPagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })

const requestColumns: any[] = [
  { title: '业务单号', dataIndex: 'bizNo', key: 'bizNo', width: 170, ellipsis: true },
  { title: '业务类型', dataIndex: 'bizType', key: 'bizType', width: 110 },
  { title: '金额（元）', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' },
  { title: '支付渠道', dataIndex: 'channel', key: 'channel', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '渠道交易号', dataIndex: 'channelTradeNo', key: 'channelTradeNo', width: 170, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 }
]

const recordColumns: any[] = [
  { title: '记录ID', dataIndex: 'id', key: 'id', width: 90 },
  { title: '渠道订单号', dataIndex: 'channelOrderNo', key: 'channelOrderNo', width: 180, ellipsis: true },
  { title: '渠道交易号', dataIndex: 'channelTradeNo', key: 'channelTradeNo', width: 180, ellipsis: true },
  { title: '支付渠道', dataIndex: 'channel', key: 'channel', width: 110 },
  { title: '金额（元）', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 }
]

function statusTag(status: number): { text: string; color: string } {
  return PAYMENT_STATUS_MAP[status] || { text: String(status ?? '-'), color: 'default' }
}

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatTime(val: string | null | undefined): string {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

function unwrapPage(res: any): { records: any[]; total: number } {
  const page = res?.data ?? res
  return { records: page?.records || [], total: Number(page?.total) || 0 }
}

async function loadRequests() {
  requestLoading.value = true
  try {
    const res: any = await paymentApi.pageRequest({
      pageNum: requestPagination.current,
      pageSize: requestPagination.pageSize
    })
    const page = unwrapPage(res)
    requestData.value = page.records
    requestPagination.total = page.total
  } catch (e) {
    console.warn('[支付配置] 支付请求获取失败', e)
  } finally {
    requestLoading.value = false
  }
}

async function loadRecords() {
  recordLoading.value = true
  try {
    const res: any = await paymentApi.pageRecord({
      pageNum: recordPagination.current,
      pageSize: recordPagination.pageSize
    })
    const page = unwrapPage(res)
    recordData.value = page.records
    recordPagination.total = page.total
  } catch (e) {
    console.warn('[支付配置] 支付记录获取失败', e)
  } finally {
    recordLoading.value = false
  }
}

function handleRequestTableChange(p: any) {
  requestPagination.current = p.current
  requestPagination.pageSize = p.pageSize
  loadRequests()
}

function handleRecordTableChange(p: any) {
  recordPagination.current = p.current
  recordPagination.pageSize = p.pageSize
  loadRecords()
}

onMounted(() => {
  loadChannels()
  loadRequests()
  loadRecords()
})
</script>

<style scoped>
.tip-alert { margin-bottom: 12px; }
.content-card { background: #fff; padding: 16px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); margin-bottom: 16px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-wrap: wrap; gap: 8px; }
.section-title { font-size: 15px; font-weight: 600; }
.section-actions { display: flex; align-items: center; gap: 8px; }
.amount-label { color: #666; font-size: 13px; }
</style>
