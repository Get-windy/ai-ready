<template>
  <div class="pricing-tiers">
    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
            <DatabaseOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">价层总数</div>
            <div class="summary-value">{{ tiers.length }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">启用中</div>
            <div class="summary-value">{{ activeTierCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #f5222d 0%, #cf1322 100%);">
            <StopOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已禁用</div>
            <div class="summary-value warning">{{ inactiveTierCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
            <SettingOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">定价模式</div>
            <div class="summary-value">{{ pricingModeCount }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-card title="价格层级配置" style="flex: 1; overflow: hidden;" :bodyStyle="{ display: 'flex', flexDirection: 'column', height: 'calc(100% - 57px)' }">
      <template #extra>
        <a-button type="primary" @click="showAddModal">
          <PlusOutlined /> 新增价层
        </a-button>
      </template>

      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tiers"
        :loading="loading"
        :pagination="{ pageSize: 10, total: tiers.length, showSizeChanger: true, showQuickJumper: true }"
        row-key="tierId"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #statusCell="{ record }">
          <StatusTag :status="record.status" :map="TIER_STATUS_MAP" />
        </template>
        <template #pricingModeCell="{ record }">
          <a-tag>{{ pricingModeLabel(record.pricingMode) }}</a-tag>
        </template>
        <template #customerLevelCell="{ record }">
          {{ levelLabel(record.customerLevel) || '-' }}
        </template>
        <template #priceInfoCell="{ record }">
          <template v-if="record.pricingMode === 'factor'">×{{ record.priceFactor }}</template>
          <template v-else-if="record.pricingMode === 'discount'">{{ record.discountRate }}%</template>
          <template v-else-if="record.pricingMode === 'fixed'">¥{{ record.tierPrice }}</template>
          <template v-else>-</template>
        </template>
        <template #quantityRangeCell="{ record }">
          {{ record.minQuantity || 0 }} - {{ record.maxQuantity || '∞' }}
        </template>
        <template #action="{ record }">
          <a-space :size="4">
            <a-button type="link" size="small" @click="editTier(record)">编辑</a-button>
            <a-popconfirm title="确定要删除此价层吗？" @confirm="deleteTier(record)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
            <a-button type="link" size="small" @click="toggleStatus(record)">
              {{ record.status === 'active' ? '禁用' : '启用' }}
            </a-button>
          </a-space>
        </template>
      </VxeTableList>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingTier ? '编辑价层' : '新增价层'"
      :confirm-loading="modalLoading"
      width="640px"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="层级名称" name="tierName">
          <a-input v-model:value="formData.tierName" placeholder="如：战略客户价" />
        </a-form-item>
        <a-form-item label="层级编码" name="tierCode">
          <a-input v-model:value="formData.tierCode" placeholder="如：strategic" />
        </a-form-item>
        <a-form-item label="适用客户等级">
          <a-select v-model:value="formData.customerLevel" placeholder="选择客户等级" allow-clear>
            <a-select-option value="">全部</a-select-option>
            <a-select-option v-for="lv in customerLevels" :key="lv" :value="lv">{{ levelLabel(lv) }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="定价模式" name="pricingMode">
          <a-radio-group v-model:value="formData.pricingMode">
            <a-radio value="factor">系数法</a-radio>
            <a-radio value="discount">折扣法</a-radio>
            <a-radio value="fixed">固定价</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="formData.pricingMode === 'factor'" label="价格系数" name="priceFactor">
          <a-input-number v-model:value="formData.priceFactor" :min="0" :max="10" :step="0.01" style="width: 200px" />
          <span style="margin-left: 8px; color: #888">基准价 × 系数</span>
        </a-form-item>
        <a-form-item v-if="formData.pricingMode === 'discount'" label="折扣率(%)" name="discountRate">
          <a-input-number v-model:value="formData.discountRate" :min="0" :max="100" :step="0.1" style="width: 200px" />
          <span style="margin-left: 8px; color: #888">折扣百分比</span>
        </a-form-item>
        <a-form-item v-if="formData.pricingMode === 'fixed'" label="层级单价" name="tierPrice">
          <a-input-number v-model:value="formData.tierPrice" :min="0" :precision="2" style="width: 200px" />
        </a-form-item>
        <a-form-item label="最低数量">
          <a-input-number v-model:value="formData.minQuantity" :min="0" style="width: 200px" />
        </a-form-item>
        <a-form-item label="最高数量">
          <a-input-number v-model:value="formData.maxQuantity" :min="0" style="width: 200px" />
        </a-form-item>
        <a-form-item label="优先级">
          <a-input-number v-model:value="formData.priority" :min="1" style="width: 200px" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined, DatabaseOutlined, CheckCircleOutlined, StopOutlined, SettingOutlined } from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import request from '@/utils/request'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { requiredRule, requiredSelectRule } from '@/utils/formRules'

interface PriceTier {
  tierId: string
  tierName: string
  tierCode: string
  customerLevel: string
  minQuantity: number
  maxQuantity: number
  tierPrice: number
  discountRate: number
  priceFactor: number
  pricingMode: string
  priority: number
  status: string
}

const TIER_STATUS_MAP: Record<string, { text: string; color: string }> = {
  active: { text: '启用', color: 'success' },
  inactive: { text: '禁用', color: 'error' }
}

const customerLevels = ['strategic', 'core', 'normal', 'new']
const pricingModeCount = computed(() => new Set(tiers.value.map(t => t.pricingMode)).size)

const loading = ref(false)
const tiers = ref<PriceTier[]>([])
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingTier = ref<PriceTier | null>(null)
const formRef = ref<FormInstance>()

const activeTierCount = computed(() => tiers.value.filter(t => t.status === 'active').length)
const inactiveTierCount = computed(() => tiers.value.filter(t => t.status !== 'active').length)

const tableRef = ref()

const vxeColumns = computed(() => [
  { field: 'tierName', title: '层级名称', width: 130 },
  { field: 'tierCode', title: '层级编码', width: 120 },
  { field: 'customerLevel', title: '客户等级', width: 110, slotName: 'customerLevelCell' },
  { field: 'pricingMode', title: '定价模式', width: 100, slotName: 'pricingModeCell' },
  { field: 'priceInfo', title: '价格系数/折扣', width: 130, slotName: 'priceInfoCell' },
  { field: 'quantityRange', title: '数量范围', width: 120, slotName: 'quantityRangeCell' },
  { field: 'priority', title: '优先级', width: 80, align: 'center' },
  { field: 'status', title: '状态', width: 80, align: 'center', slotName: 'statusCell' },
  { field: 'action', title: '操作', width: 180, fixed: 'right', type: 'action' },
])

const formRules: Record<string, any> = {
  tierName: [requiredRule('层级名称')],
  tierCode: [requiredRule('层级编码')],
  pricingMode: [requiredSelectRule('定价模式')]
}

const formData = reactive({
  tierName: '',
  tierCode: '',
  customerLevel: '',
  pricingMode: 'factor',
  priceFactor: 1,
  discountRate: 0,
  tierPrice: 0,
  minQuantity: 0,
  maxQuantity: 0,
  priority: 100
})

function pricingModeLabel(mode: string) {
  const map: Record<string, string> = { factor: '系数法', discount: '折扣法', fixed: '固定价' }
  return map[mode] || mode
}

function levelLabel(level: string) {
  const map: Record<string, string> = { strategic: '战略客户', core: '核心客户', normal: '普通客户', new: '新客户' }
  return map[level] || level
}

async function fetchTiers() {
  loading.value = true
  try {
    const res = await request.get('/erp/pricing/tiers/list')
    tiers.value = res?.data || []
  } catch { message.error('获取价层列表失败') }
  finally { loading.value = false }
}

function showAddModal() {
  editingTier.value = null
  Object.assign(formData, { tierName: '', tierCode: '', customerLevel: '', pricingMode: 'factor', priceFactor: 1, discountRate: 0, tierPrice: 0, minQuantity: 0, maxQuantity: 0, priority: 100 })
  modalVisible.value = true
}

function editTier(tier: PriceTier) {
  editingTier.value = tier
  Object.assign(formData, tier)
  modalVisible.value = true
}

async function handleModalOk() {
  try {
    await formRef.value?.validate()
    modalLoading.value = true
    if (editingTier.value) {
      await request.put(`/erp/pricing/tiers/${editingTier.value.tierId}`, { ...editingTier.value, ...formData })
      message.success('更新成功')
    } else {
      await request.post('/erp/pricing/tiers', formData)
      message.success('创建成功')
    }
    modalVisible.value = false
    await fetchTiers()
  } catch (error: any) {
    if (error?.errorFields) return
    message.error(error?.message || '操作失败')
  } finally { modalLoading.value = false }
}

function handleModalCancel() { modalVisible.value = false }

async function deleteTier(tier: PriceTier) {
  try {
    await request.delete(`/erp/pricing/tiers/${tier.tierId}`)
    message.success('删除成功')
    await fetchTiers()
  } catch { message.error('删除失败') }
}

async function toggleStatus(tier: PriceTier) {
  const newStatus = tier.status === 'active' ? 'inactive' : 'active'
  try {
    await request.put(`/erp/pricing/tiers/${tier.tierId}/status`, { status: newStatus })
    message.success(newStatus === 'active' ? '已启用' : '已禁用')
    await fetchTiers()
  } catch { message.error('操作失败') }
}

onMounted(() => { fetchTiers() })
</script>

<style scoped>
.pricing-tiers { padding: 16px; height: 100%; display: flex; flex-direction: column; overflow: hidden; min-height: 0; }

/* 统计卡片样式 */
.summary-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.summary-card.highlight {
  background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%);
  border: 1px solid #d3adf7;
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
}

.summary-content {
  flex: 1;
}

.summary-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.summary-value.warning {
  color: #f5222d;
}
</style>
