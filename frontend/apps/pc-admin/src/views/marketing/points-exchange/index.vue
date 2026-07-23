<template>
  <div>
    <a-alert
      type="warning"
      show-icon
      message="积分兑换流水（兑换记录/审核）后端端点待补全"
      description="当前页对接忠诚程序（LOYALTY 积分方案）真实数据，可维护积分方案的规则与有效期；会员实际兑换记录列表将在后端提供端点后接入。"
      style="margin-bottom: 16px"
    />
    <ARReportPage
      ref="reportRef"
      title="积分兑换"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="积分方案"
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
          </template>新增积分方案
        </a-button>
      </template>
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'triggerType'">
          {{ TRIGGER_MAP[text] || text || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'applyScope'">
          {{ SCOPE_MAP[text] || text || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'period'">
          {{ fmtDate(record.startDate) }} ~ {{ fmtDate(record.endDate) }}
        </template>
        <template v-else-if="['maxUsage', 'usageCount'].includes(column.dataIndex as string)">
          {{ formatNum(text) }}
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
            <a-popconfirm
              title="确认删除该积分方案？"
              @confirm="handleDelete(record)"
            >
              <a class="text-danger">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <!-- 新增/编辑积分方案弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingProgram ? '编辑积分方案' : '新增积分方案'"
      :confirm-loading="modalLoading"
      :width="560"
      @ok="handleModalOk"
      @cancel="modalVisible = false"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        style="margin-top: 16px"
      >
        <a-form-item
          label="方案名称"
          required
        >
          <a-input
            v-model:value="modalForm.name"
            placeholder="请输入方案名称"
          />
        </a-form-item>
        <a-form-item label="触发方式">
          <a-select
            v-model:value="modalForm.triggerType"
            :options="triggerOptions"
            placeholder="请选择"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="有效期">
          <a-range-picker
            v-model:value="modalForm.dateRange"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="适用范围">
          <a-select
            v-model:value="modalForm.applyScope"
            :options="scopeOptions"
            placeholder="请选择"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="最大兑换次数">
          <a-input-number
            v-model:value="modalForm.maxUsage"
            :min="1"
            placeholder="留空表示不限"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number
            v-model:value="modalForm.sortOrder"
            :min="0"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="方案说明">
          <a-textarea
            v-model:value="modalForm.description"
            :rows="2"
            placeholder="积分兑换规则说明"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { loyaltyProgramApi, type LoyaltyProgram } from '@/api/marketing'

// ═══ 字典（与后端 LoyaltyProgram 注释一致） ═══
const TRIGGER_MAP: Record<string, string> = { AUTO: '自动触发', CODE: '需输入码' }
const SCOPE_MAP: Record<string, string> = { ON_ORDER: '整单', ON_PRODUCT: '指定产品', ON_CATEGORY: '指定分类' }
const triggerOptions = Object.entries(TRIGGER_MAP).map(([value, label]) => ({ label, value }))
const scopeOptions = Object.entries(SCOPE_MAP).map(([value, label]) => ({ label, value }))

const reportRef = ref<InstanceType<typeof ARReportPage>>()

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '方案名称' }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '方案名称', dataIndex: 'name', key: 'name', width: 160, ellipsis: true },
  { title: '触发方式', dataIndex: 'triggerType', key: 'triggerType', width: 100 },
  { title: '有效期', dataIndex: 'period', key: 'period', width: 210 },
  { title: '适用范围', dataIndex: 'applyScope', key: 'applyScope', width: 100 },
  { title: '最大兑换次数', dataIndex: 'maxUsage', key: 'maxUsage', width: 110, align: 'right' },
  { title: '已兑换次数', dataIndex: 'usageCount', key: 'usageCount', width: 100, align: 'right' },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 70 },
  { title: '状态', dataIndex: 'isActive', key: 'isActive', width: 90 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

function formatNum(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function fmtDate(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD') : '不限'
}

// ═══ 数据请求（固定 LOYALTY 类型） ═══
function fetcher(params: Record<string, any>) {
  return loyaltyProgramApi.page({ ...params, programType: 'LOYALTY' })
}

// ═══ 新增/编辑弹窗 ═══
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingProgram = ref<LoyaltyProgram | null>(null)
const modalForm = reactive<{
  name?: string
  triggerType?: string
  dateRange?: [Dayjs, Dayjs]
  applyScope?: string
  maxUsage?: number
  sortOrder?: number
  description?: string
}>({})

function openCreate() {
  editingProgram.value = null
  Object.assign(modalForm, { name: undefined, triggerType: 'AUTO', dateRange: undefined, applyScope: 'ON_ORDER', maxUsage: undefined, sortOrder: 0, description: undefined })
  modalVisible.value = true
}

function openEdit(record: any) {
  editingProgram.value = record
  Object.assign(modalForm, {
    name: record.name,
    triggerType: record.triggerType,
    dateRange: record.startDate && record.endDate ? [dayjs(record.startDate), dayjs(record.endDate)] : undefined,
    applyScope: record.applyScope,
    maxUsage: record.maxUsage,
    sortOrder: record.sortOrder,
    description: record.description
  })
  modalVisible.value = true
}

async function handleModalOk() {
  if (!modalForm.name) {
    message.warning('请输入方案名称')
    return
  }
  modalLoading.value = true
  try {
    const payload: Partial<LoyaltyProgram> = {
      programType: 'LOYALTY',
      name: modalForm.name,
      triggerType: modalForm.triggerType,
      applyScope: modalForm.applyScope,
      maxUsage: modalForm.maxUsage,
      sortOrder: modalForm.sortOrder,
      description: modalForm.description,
      startDate: modalForm.dateRange?.[0] ? modalForm.dateRange[0].format('YYYY-MM-DDT00:00:00') : undefined,
      endDate: modalForm.dateRange?.[1] ? modalForm.dateRange[1].format('YYYY-MM-DDT23:59:59') : undefined
    }
    if (editingProgram.value) {
      await loyaltyProgramApi.update(editingProgram.value.id, payload)
      message.success('更新成功')
    } else {
      await loyaltyProgramApi.create(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[积分兑换] 保存失败', e)
  } finally {
    modalLoading.value = false
  }
}

// ═══ 启用/停用 ═══
async function toggleActive(record: any, checked: boolean) {
  try {
    await loyaltyProgramApi.update(record.id, { isActive: checked ? 1 : 0 })
    message.success(checked ? '已启用' : '已停用')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[积分兑换] 状态更新失败', e)
  }
}

// ═══ 删除 ═══
async function handleDelete(record: any) {
  try {
    await loyaltyProgramApi.remove(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[积分兑换] 删除失败', e)
  }
}
</script>

<style scoped>
.text-danger {
  color: #ff4d4f;
}
</style>
