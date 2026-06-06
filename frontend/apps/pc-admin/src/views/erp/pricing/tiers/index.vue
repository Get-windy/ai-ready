<template>
  <div class="pricing-tiers">
    <a-card title="价格层级配置">
      <template #extra>
        <a-button type="primary" @click="showAddModal">
          <PlusOutlined /> 新增价层
        </a-button>
      </template>

      <!-- 统计概览 -->
      <a-row :gutter="16" style="margin-bottom: 24px">
        <a-col :span="6">
          <a-statistic title="价层总数" :value="tiers.length" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="启用中" :value="activeTierCount" value-style="color: #52c41a" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="已禁用" :value="inactiveTierCount" value-style="color: #ff4d4f" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="定价模式" :value="pricingModeCount" />
        </a-col>
      </a-row>

      <!-- 价层列表 -->
      <a-table
        :columns="columns"
        :data-source="tiers"
        :loading="loading"
        :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
        row-key="tierId"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <StatusTag :status="record.status" :map="TIER_STATUS_MAP" />
          </template>
          <template v-else-if="column.key === 'pricingMode'">
            <a-tag>{{ pricingModeLabel(record.pricingMode) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'customerLevel'">
            {{ levelLabel(record.customerLevel) || '-' }}
          </template>
          <template v-else-if="column.key === 'priceInfo'">
            <template v-if="record.pricingMode === 'factor'">×{{ record.priceFactor }}</template>
            <template v-else-if="record.pricingMode === 'discount'">{{ record.discountRate }}%</template>
            <template v-else-if="record.pricingMode === 'fixed'">¥{{ record.tierPrice }}</template>
            <template v-else>-</template>
          </template>
          <template v-else-if="column.key === 'quantityRange'">
            {{ record.minQuantity || 0 }} - {{ record.maxQuantity || '∞' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="editTier(record)">编辑</a>
              <a-popconfirm title="确定要删除此价层吗？" @confirm="deleteTier(record)">
                <a class="danger">删除</a>
              </a-popconfirm>
              <a @click="toggleStatus(record)">
                {{ record.status === 'active' ? '禁用' : '启用' }}
              </a>
            </a-space>
          </template>
        </template>
      </a-table>
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
import { PlusOutlined } from '@ant-design/icons-vue'
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

const columns = [
  { title: '层级名称', dataIndex: 'tierName', key: 'tierName' },
  { title: '层级编码', dataIndex: 'tierCode', key: 'tierCode' },
  { title: '客户等级', key: 'customerLevel' },
  { title: '定价模式', key: 'pricingMode' },
  { title: '价格系数/折扣', key: 'priceInfo' },
  { title: '数量范围', key: 'quantityRange' },
  { title: '优先级', dataIndex: 'priority', key: 'priority' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action' }
]

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
.pricing-tiers { padding: 16px; }
.danger { color: #ff4d4f; }
</style>
