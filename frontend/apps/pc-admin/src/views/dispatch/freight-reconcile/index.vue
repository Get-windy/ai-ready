<template>
  <!--
    物流运费对账（配发收 → 发货业务 → 物流运费对账，菜单 70162）
    落地《物流发货-业界做法调研.md》P2-4 / P2-6：
      ① 运费规则：承运商 × 区域 × 重量区间（首重/续重/基础费）
      ② 运费对账：我方计费(shipping_fee) vs 承运商账单金额 → 差异清单 + 标记已对账
      ③ 发货通知（ASN）：发货后幂等生成台账，按配置回调地址推送 / 可重试
  -->
  <PageContainer title="物流运费对账">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <!-- ═══ ① 运费规则 ═══ -->
      <a-tab-pane key="rule" tab="运费规则">
        <div class="toolbar">
          <a-space>
            <a-select
              v-model:value="ruleCarrierId"
              :options="carrierOptions"
              placeholder="全部承运商"
              allow-clear
              show-search
              option-filter-prop="label"
              style="width: 200px"
              @change="loadRules"
            />
            <a-button type="primary" @click="openRuleForm()">新增规则</a-button>
            <a-button @click="loadRules">刷新</a-button>
          </a-space>
          <span class="hint">匹配优先级：承运商+区域 &gt; 承运商 &gt; 区域 &gt; 通用；同级按优先级降序</span>
        </div>
        <a-table :data-source="rules" :columns="ruleColumns" :pagination="false" row-key="id" size="small" bordered>
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'carrier'">{{ record.carrierName || '通用' }}</template>
            <template v-else-if="column.key === 'range'">
              {{ record.minWeight ?? 0 }} ~ {{ record.maxWeight ?? '∞' }} kg
            </template>
            <template v-else-if="column.key === 'enabled'">
              <a-tag :color="record.enabled === 1 ? 'green' : 'default'">{{ record.enabled === 1 ? '启用' : '停用' }}</a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space :size="4">
                <a-button type="link" size="small" @click="openRuleForm(record)">编辑</a-button>
                <a-button type="link" size="small" danger @click="removeRule(record)">删除</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <!-- ═══ ② 运费对账 ═══ -->
      <a-tab-pane key="reconcile" tab="运费对账">
        <div class="toolbar">
          <a-space>
            <a-select
              v-model:value="recCarrierId"
              :options="carrierOptions"
              placeholder="全部承运商"
              allow-clear
              show-search
              option-filter-prop="label"
              style="width: 200px"
            />
            <a-range-picker v-model:value="recRange" value-format="YYYY-MM-DD" />
            <a-button type="primary" @click="loadReconcile">查询</a-button>
            <a-button :disabled="!selectedPkgIds.length" @click="markReconciled">标记已对账</a-button>
          </a-space>
        </div>
        <a-row :gutter="12" class="stat-row">
          <a-col :span="6"><a-card size="small"><a-statistic title="运单数" :value="rec.total || 0" /></a-card></a-col>
          <a-col :span="6"><a-card size="small"><a-statistic title="我方计费(元)" :value="rec.chargedAmount || 0" :precision="2" /></a-card></a-col>
          <a-col :span="6"><a-card size="small"><a-statistic title="承运商账单(元)" :value="rec.billedAmount || 0" :precision="2" /></a-card></a-col>
          <a-col :span="6">
            <a-card size="small">
              <a-statistic
                title="差异(元)"
                :value="rec.diffAmount || 0"
                :precision="2"
                :value-style="{ color: Number(rec.diffAmount || 0) !== 0 ? '#cf1322' : '#3f8600' }"
              />
            </a-card>
          </a-col>
        </a-row>
        <a-table
          :data-source="rec.details || []"
          :columns="recColumns"
          :pagination="{ pageSize: 20 }"
          row-key="packageId"
          size="small"
          bordered
          :row-selection="{ selectedRowKeys: selectedPkgIds, onChange: (keys: any) => (selectedPkgIds = keys) }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'billAmount'">
              <a-input-number
                :value="record.billAmount"
                :min="0"
                :precision="2"
                size="small"
                style="width: 110px"
                placeholder="录入账单"
                @blur="(e: any) => saveBill(record, e.target.value)"
                @press-enter="(e: any) => saveBill(record, e.target.value)"
              />
            </template>
            <template v-else-if="column.key === 'diff'">
              <span :style="{ color: Math.abs(Number(record.diff || 0)) > 0.01 ? '#cf1322' : '#3f8600' }">
                {{ Number(record.diff || 0).toFixed(2) }}
              </span>
            </template>
            <template v-else-if="column.key === 'reconciled'">
              <a-tag :color="record.reconciled === 1 ? 'green' : 'orange'">
                {{ record.reconciled === 1 ? '已对账' : '未对账' }}
              </a-tag>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <!-- ═══ ③ 发货通知（ASN）═══ -->
      <a-tab-pane key="notify" tab="发货通知">
        <div class="toolbar">
          <a-space>
            <a-select v-model:value="notifyStatus" placeholder="全部状态" allow-clear style="width: 160px" :options="NOTIFY_STATUS_OPTIONS" />
            <a-input v-model:value="notifyOrderNo" placeholder="订单编号" allow-clear style="width: 180px" />
            <a-button type="primary" @click="loadNotifies">查询</a-button>
            <a-button @click="sendPending">批量发送待发送/失败</a-button>
          </a-space>
          <span class="hint">
            {{ notifyConfigured ? '已配置回调地址（erp.shipment.asn.callback-url）' : '未配置回调地址：通知只落台账，配置后点「发送」即可推送' }}
          </span>
        </div>
        <a-table :data-source="notifies" :columns="notifyColumns" :pagination="{ pageSize: 20 }" row-key="id" size="small" bordered>
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="NOTIFY_STATUS[record.status]?.color">{{ NOTIFY_STATUS[record.status]?.label || '-' }}</a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" size="small" :disabled="record.status === 1" @click="sendOne(record)">发送</a-button>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <!-- 运费规则表单 -->
    <a-modal v-model:open="ruleFormVisible" :title="ruleForm.id ? '编辑运费规则' : '新增运费规则'" width="680px" :confirm-loading="saving" @ok="saveRule">
      <a-form :label-col="{ span: 7 }" :wrapper-col="{ span: 17 }">
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="承运商">
              <a-select v-model:value="ruleForm.carrierId" :options="carrierOptions" allow-clear show-search option-filter-prop="label" placeholder="留空=通用规则" />
            </a-form-item>
          </a-col>
          <a-col :span="12"><a-form-item label="区域/线路"><a-input v-model:value="ruleForm.area" placeholder="留空=通用" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="重量下限(kg)"><a-input-number v-model:value="ruleForm.minWeight" :min="0" :precision="3" style="width: 100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="重量上限(kg)"><a-input-number v-model:value="ruleForm.maxWeight" :min="0" :precision="3" style="width: 100%" placeholder="留空=不限" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="首重(kg)"><a-input-number v-model:value="ruleForm.firstWeight" :min="0" :precision="3" style="width: 100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="首重价(元)"><a-input-number v-model:value="ruleForm.firstPrice" :min="0" :precision="2" style="width: 100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="续重步长(kg)"><a-input-number v-model:value="ruleForm.addStep" :min="0" :precision="3" style="width: 100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="续重单价(元)"><a-input-number v-model:value="ruleForm.addPrice" :min="0" :precision="2" style="width: 100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="基础费(元)"><a-input-number v-model:value="ruleForm.baseFee" :min="0" :precision="2" style="width: 100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="优先级"><a-input-number v-model:value="ruleForm.priority" :min="0" style="width: 100%" /></a-form-item></a-col>
          <a-col :span="12">
            <a-form-item label="启用">
              <a-switch :checked="ruleForm.enabled === 1" @change="(v: any) => (ruleForm.enabled = v ? 1 : 0)" />
            </a-form-item>
          </a-col>
          <a-col :span="24"><a-form-item label="备注" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }"><a-input v-model:value="ruleForm.remark" /></a-form-item></a-col>
        </a-row>
      </a-form>
      <div class="preview-tip">
        试算：首重价 + 基础费 + ⌈(重量−首重)/续重步长⌉ × 续重单价；同一订单包裹保存时会按规则自动带入运费（可手工覆盖）。
      </div>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { saleLogisticsApi } from '@/api/erp'
import { partnerApi } from '@/api/erp/partner'

defineOptions({ name: 'DispatchFreightReconcile' })

const activeTab = ref('rule')
const carrierOptions = ref<{ label: string; value: number | string }[]>([])
const saving = ref(false)

const NOTIFY_STATUS: Record<number, { label: string; color: string }> = {
  0: { label: '待发送', color: 'default' },
  1: { label: '已发送', color: 'green' },
  2: { label: '发送失败', color: 'red' },
}
const NOTIFY_STATUS_OPTIONS = [
  { label: '待发送', value: 0 }, { label: '已发送', value: 1 }, { label: '发送失败', value: 2 },
]

const pick = (res: any) => res?.data ?? res ?? null

// ── 承运商档案 ──
async function loadCarriers() {
  if (carrierOptions.value.length) return
  const res: any = await partnerApi.list('LOGISTICS', '1', 500).catch(() => null)
  const list = pick(res) || []
  carrierOptions.value = list.map((p: any) => ({ label: p.partyName || p.name, value: p.id }))
}

// ── ① 规则 ──
const rules = ref<any[]>([])
const ruleCarrierId = ref<number | string | undefined>()
const ruleColumns = [
  { title: '承运商', key: 'carrier', width: 140 },
  { title: '区域', dataIndex: 'area', key: 'area', width: 110 },
  { title: '重量区间', key: 'range', width: 150 },
  { title: '首重', dataIndex: 'firstWeight', key: 'firstWeight', width: 80 },
  { title: '首重价', dataIndex: 'firstPrice', key: 'firstPrice', width: 90 },
  { title: '续重步长', dataIndex: 'addStep', key: 'addStep', width: 90 },
  { title: '续重单价', dataIndex: 'addPrice', key: 'addPrice', width: 90 },
  { title: '基础费', dataIndex: 'baseFee', key: 'baseFee', width: 80 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 70 },
  { title: '状态', key: 'enabled', width: 80 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const },
]

async function loadRules() {
  const res: any = await saleLogisticsApi.freightRules(ruleCarrierId.value).catch(() => null)
  rules.value = pick(res) || []
}

const ruleFormVisible = ref(false)
const emptyRule = () => ({ enabled: 1, priority: 0 } as Record<string, any>)
const ruleForm = reactive<Record<string, any>>(emptyRule())
const openRuleForm = (record?: any) => {
  Object.assign(ruleForm, emptyRule(), record || {})
  ruleFormVisible.value = true
}
async function saveRule() {
  saving.value = true
  try {
    await saleLogisticsApi.saveFreightRule({ ...ruleForm })
    message.success('已保存')
    ruleFormVisible.value = false
    loadRules()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
function removeRule(record: any) {
  Modal.confirm({
    title: '删除运费规则',
    content: `确认删除该规则（${record.carrierName || '通用'} / ${record.area || '通用'}）？`,
    onOk: async () => {
      try {
        await saleLogisticsApi.deleteFreightRule(record.id)
        message.success('已删除')
        loadRules()
      } catch (e: any) {
        message.error(e?.message || '删除失败')
      }
    },
  })
}

// ── ② 对账 ──
const rec = ref<Record<string, any>>({})
const recCarrierId = ref<number | string | undefined>()
const recRange = ref<any>(null)
const selectedPkgIds = ref<any[]>([])
const recColumns = [
  { title: '订单编号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '承运商', dataIndex: 'carrierName', key: 'carrierName', width: 130 },
  { title: '运单号', dataIndex: 'waybillNo', key: 'waybillNo', width: 160 },
  { title: '重量(kg)', dataIndex: 'weight', key: 'weight', width: 90 },
  { title: '我方计费', dataIndex: 'shippingFee', key: 'shippingFee', width: 100 },
  { title: '账单金额', key: 'billAmount', width: 130 },
  { title: '差异', key: 'diff', width: 100 },
  { title: '对账', key: 'reconciled', width: 90 },
]
async function loadReconcile() {
  const params: any = { carrierId: recCarrierId.value }
  if (recRange.value?.length === 2) {
    params.startDate = recRange.value[0]
    params.endDate = recRange.value[1]
  }
  const res: any = await saleLogisticsApi.freightReconcile(params).catch(() => null)
  rec.value = pick(res) || {}
  selectedPkgIds.value = []
}
async function saveBill(record: any, value: any) {
  const amount = Number(value)
  if (Number.isNaN(amount)) return
  try {
    await saleLogisticsApi.saveFreightBill(record.packageId, amount)
    message.success('账单金额已录入')
    loadReconcile()
  } catch (e: any) {
    message.error(e?.message || '录入失败')
  }
}
async function markReconciled() {
  try {
    await saleLogisticsApi.markReconciled(selectedPkgIds.value)
    message.success('已标记对账')
    loadReconcile()
  } catch (e: any) {
    message.error(e?.message || '标记失败')
  }
}

// ── ③ 发货通知 ──
const notifies = ref<any[]>([])
const notifyStatus = ref<number | undefined>()
const notifyOrderNo = ref('')
const notifyConfigured = ref(false)
const notifyColumns = [
  { title: '订单编号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '承运商', dataIndex: 'carrierName', key: 'carrierName', width: 130 },
  { title: '运单号', dataIndex: 'waybillNo', key: 'waybillNo', width: 160 },
  { title: '状态', key: 'status', width: 100 },
  { title: '重试次数', dataIndex: 'retryCount', key: 'retryCount', width: 90 },
  { title: '发送时间', dataIndex: 'sentTime', key: 'sentTime', width: 170 },
  { title: '最后错误', dataIndex: 'lastError', key: 'lastError', ellipsis: true },
  { title: '操作', key: 'action', width: 90, fixed: 'right' as const },
]
async function loadNotifies() {
  const res: any = await saleLogisticsApi.notifyPage({
    status: notifyStatus.value, orderNo: notifyOrderNo.value || undefined, current: 1, size: 100,
  }).catch(() => null)
  notifies.value = pick(res)?.records || []
}
async function sendOne(record: any) {
  try {
    const res: any = await saleLogisticsApi.sendNotify(record.id)
    const after = pick(res)
    if (after?.status === 1) {
      message.success('已发送')
    } else {
      message.warning(after?.lastError || '未发送（未配置回调地址）')
    }
    loadNotifies()
  } catch (e: any) {
    message.error(e?.message || '发送失败')
  }
}
async function sendPending() {
  try {
    const res: any = await saleLogisticsApi.sendPendingNotify()
    const d = pick(res) || {}
    message.info(d.configured ? `发送完成：成功 ${d.sent} / 失败 ${d.failed}` : '未配置回调地址，通知仅落台账')
    loadNotifies()
  } catch (e: any) {
    message.error(e?.message || '批量发送失败')
  }
}

const onTabChange = (key: string) => {
  if (key === 'rule') loadRules()
  if (key === 'reconcile') loadReconcile()
  if (key === 'notify') loadNotifies()
}

onMounted(async () => {
  await loadCarriers()
  await loadRules()
  const st: any = await saleLogisticsApi.waybillStatus().catch(() => null)
  notifyConfigured.value = Boolean(pick(st)?.asnConfigured)
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}
.hint {
  color: #8c8c8c;
  font-size: 12px;
}
.stat-row {
  margin-bottom: 12px;
}
.preview-tip {
  margin-top: 4px;
  padding: 8px 12px;
  background: #f7f8fa;
  border-radius: 4px;
  color: #8c8c8c;
  font-size: 12px;
  line-height: 1.6;
}
</style>
