<template>
  <ARReportPage
    ref="reportRef"
    title="会员管理"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="会员卡"
    row-key="id"
  >
    <template #header-extra>
      <a-button
        type="primary"
        size="small"
        @click="openCreate"
      >
        <template #icon>
          <PlusOutlined />
        </template>新增会员卡
      </a-button>
    </template>
    <template #bodyCell="{ column, record, text }">
      <template v-if="column.dataIndex === 'programId'">
        {{ programName(text) }}
      </template>
      <template v-else-if="['points', 'totalEarned', 'totalRedeemed'].includes(column.dataIndex as string)">
        {{ formatNum(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'expirationDate'">
        {{ text ? dayjs(text).format('YYYY-MM-DD') : '-' }}
      </template>
      <template v-else-if="column.dataIndex === 'isActive'">
        <a-switch
          :checked="record.isActive === 1"
          checked-children="启用"
          un-checked-children="停用"
          size="small"
          @change="(checked: boolean) => toggleActive(record, checked)"
        />
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space>
          <a @click="openEdit(record)">编辑</a>
          <a-divider type="vertical" />
          <a @click="openPoints(record)">积分调整</a>
          <a-divider type="vertical" />
          <a-popconfirm
            title="确认删除该会员卡？"
            @confirm="handleDelete(record)"
          >
            <a class="text-danger">删除</a>
          </a-popconfirm>
        </a-space>
      </template>
    </template>
  </ARReportPage>

  <!-- 新增/编辑会员卡弹窗 -->
  <a-modal
    v-model:open="modalVisible"
    :title="editingCard ? '编辑会员卡' : '新增会员卡'"
    :confirm-loading="modalLoading"
    :width="520"
    @ok="handleModalOk"
    @cancel="modalVisible = false"
  >
    <a-form
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      style="margin-top: 16px"
    >
      <a-form-item
        label="卡号"
        required
      >
        <a-input
          v-model:value="modalForm.cardCode"
          placeholder="请输入会员卡号"
          :disabled="!!editingCard"
        />
      </a-form-item>
      <a-form-item
        label="会员ID"
        required
      >
        <a-input-number
          v-model:value="modalForm.partnerId"
          :min="1"
          placeholder="往来单位联系人ID"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="积分方案">
        <a-select
          v-model:value="modalForm.programId"
          :options="programOptions"
          placeholder="请选择积分方案"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="有效期至">
        <a-date-picker
          v-model:value="modalForm.expirationDate"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea
          v-model:value="modalForm.remark"
          :rows="2"
          placeholder="选填"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 积分调整弹窗 -->
  <a-modal
    v-model:open="pointsModalVisible"
    title="积分调整"
    :confirm-loading="modalLoading"
    :width="420"
    @ok="handlePointsOk"
    @cancel="pointsModalVisible = false"
  >
    <a-form
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      style="margin-top: 16px"
    >
      <a-form-item label="会员卡">
        <span>{{ pointsCard?.cardCode }}（当前积分 {{ formatNum(pointsCard?.points) }}）</span>
      </a-form-item>
      <a-form-item label="调整方向">
        <a-radio-group v-model:value="pointsForm.direction">
          <a-radio value="add">
            增加
          </a-radio>
          <a-radio value="deduct">
            扣减
          </a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item
        label="积分数量"
        required
      >
        <a-input-number
          v-model:value="pointsForm.points"
          :min="1"
          style="width: 100%"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { loyaltyCardApi, loyaltyProgramApi, type LoyaltyCard, type LoyaltyProgram } from '@/api/marketing'

const reportRef = ref<InstanceType<typeof ARReportPage>>()

const queryFields: ReportQueryField[] = [
  { key: 'memberId', type: 'input', label: '会员ID', placeholder: '会员(联系人)ID', width: 160 }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '卡号', dataIndex: 'cardCode', key: 'cardCode', width: 150 },
  { title: '会员ID', dataIndex: 'partnerId', key: 'partnerId', width: 90 },
  { title: '积分方案', dataIndex: 'programId', key: 'programId', width: 140, ellipsis: true },
  { title: '当前积分', dataIndex: 'points', key: 'points', width: 110, align: 'right' },
  { title: '累计获得', dataIndex: 'totalEarned', key: 'totalEarned', width: 110, align: 'right' },
  { title: '累计兑换', dataIndex: 'totalRedeemed', key: 'totalRedeemed', width: 110, align: 'right' },
  { title: '有效期至', dataIndex: 'expirationDate', key: 'expirationDate', width: 110 },
  { title: '状态', dataIndex: 'isActive', key: 'isActive', width: 90 },
  { title: '操作', key: 'action', width: 210, fixed: 'right' }
]

function formatNum(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

// ═══ 积分方案下拉（/program/list 真实端点） ═══
const programs = ref<LoyaltyProgram[]>([])

const programOptions = computed(() =>
  programs.value.map(p => ({ label: p.name || `#${p.id}`, value: p.id }))
)

function programName(programId: number | undefined): string {
  if (!programId) return '-'
  const p = programs.value.find(item => item.id === programId)
  return p?.name || `#${programId}`
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return loyaltyCardApi.page(params)
}

// ═══ 新增/编辑弹窗 ═══
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingCard = ref<LoyaltyCard | null>(null)
const modalForm = reactive<{
  cardCode?: string
  partnerId?: number
  programId?: number
  expirationDate?: Dayjs
  remark?: string
}>({})

function openCreate() {
  editingCard.value = null
  Object.assign(modalForm, { cardCode: undefined, partnerId: undefined, programId: undefined, expirationDate: undefined, remark: undefined })
  modalVisible.value = true
}

function openEdit(record: any) {
  editingCard.value = record
  Object.assign(modalForm, {
    cardCode: record.cardCode,
    partnerId: record.partnerId,
    programId: record.programId,
    expirationDate: record.expirationDate ? dayjs(record.expirationDate) : undefined,
    remark: record.remark
  })
  modalVisible.value = true
}

async function handleModalOk() {
  if (!modalForm.cardCode) {
    message.warning('请输入会员卡号')
    return
  }
  if (!modalForm.partnerId) {
    message.warning('请输入会员ID')
    return
  }
  modalLoading.value = true
  try {
    const payload: Partial<LoyaltyCard> = {
      cardCode: modalForm.cardCode,
      partnerId: modalForm.partnerId,
      programId: modalForm.programId,
      expirationDate: modalForm.expirationDate ? modalForm.expirationDate.format('YYYY-MM-DDT00:00:00') : undefined,
      remark: modalForm.remark
    }
    if (editingCard.value) {
      await loyaltyCardApi.update(editingCard.value.id, payload)
      message.success('更新成功')
    } else {
      await loyaltyCardApi.create(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[会员管理] 保存失败', e)
  } finally {
    modalLoading.value = false
  }
}

// ═══ 启用/停用 ═══
async function toggleActive(record: any, checked: boolean) {
  try {
    await loyaltyCardApi.update(record.id, { isActive: checked ? 1 : 0 })
    message.success(checked ? '已启用' : '已停用')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[会员管理] 状态更新失败', e)
  }
}

// ═══ 积分调整 ═══
const pointsModalVisible = ref(false)
const pointsCard = ref<LoyaltyCard | null>(null)
const pointsForm = reactive<{ direction: 'add' | 'deduct'; points?: number }>({ direction: 'add', points: undefined })

function openPoints(record: any) {
  pointsCard.value = record
  pointsForm.direction = 'add'
  pointsForm.points = undefined
  pointsModalVisible.value = true
}

async function handlePointsOk() {
  if (!pointsCard.value) return
  if (!pointsForm.points || pointsForm.points <= 0) {
    message.warning('请输入有效的积分数量')
    return
  }
  modalLoading.value = true
  try {
    if (pointsForm.direction === 'add') {
      await loyaltyCardApi.addPoints(pointsCard.value.id, pointsForm.points)
    } else {
      await loyaltyCardApi.deductPoints(pointsCard.value.id, pointsForm.points)
    }
    message.success('积分调整成功')
    pointsModalVisible.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[会员管理] 积分调整失败', e)
  } finally {
    modalLoading.value = false
  }
}

// ═══ 删除 ═══
async function handleDelete(record: any) {
  try {
    await loyaltyCardApi.remove(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[会员管理] 删除失败', e)
  }
}

onMounted(async () => {
  try {
    programs.value = await loyaltyProgramApi.list('LOYALTY')
  } catch (e) {
    console.warn('[会员管理] 积分方案列表获取失败', e)
  }
})
</script>

<style scoped>
.text-danger {
  color: #ff4d4f;
}
</style>
