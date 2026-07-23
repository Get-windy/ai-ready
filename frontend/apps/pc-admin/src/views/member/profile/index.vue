<template>
  <ARReportPage
    ref="reportRef"
    title="会员档案"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="会员档案"
    row-key="id"
  >
    <template #header-extra>
      <a-button type="primary" size="small" @click="openCreate">
        <template #icon><PlusOutlined /></template>新增会员
      </a-button>
    </template>
    <template #bodyCell="{ column, record, text }">
      <template v-if="column.dataIndex === 'gender'">
        {{ genderText(text) }}
      </template>
      <template v-if="column.dataIndex === 'points'">
        <span style="font-weight: 500">{{ text ?? 0 }}</span>
      </template>
      <template v-else-if="column.dataIndex === 'birthday'">
        {{ text ? dayjs(text).format('YYYY-MM-DD') : '-' }}
      </template>
      <template v-else-if="column.dataIndex === 'active'">
        <a-switch :checked="record.active === 1" checked-children="启用" un-checked-children="停用" size="small" @change="(checked: boolean) => toggleActive(record, checked)" />
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space>
          <a @click="openView(record)">查看</a>
          <a-divider type="vertical" />
          <a @click="openEdit(record)">编辑</a>
          <a-divider type="vertical" />
          <a @click="openPoints(record)">积分</a>
        </a-space>
      </template>
    </template>
  </ARReportPage>

  <!-- 会员表单弹窗 -->
  <a-modal v-model:open="modalVisible" :title="editingMember ? '编辑会员' : '新增会员'" :confirm-loading="modalLoading" :width="560" @ok="handleModalOk" @cancel="modalVisible = false">
    <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" style="margin-top: 16px">
      <a-form-item label="姓名" required>
        <a-input v-model:value="modalForm.name" placeholder="请输入姓名" />
      </a-form-item>
      <a-form-item label="手机号" required>
        <a-input v-model:value="modalForm.phone" placeholder="请输入手机号" />
      </a-form-item>
      <a-form-item label="性别">
        <a-radio-group v-model:value="modalForm.gender">
          <a-radio :value="0">未知</a-radio>
          <a-radio :value="1">男</a-radio>
          <a-radio :value="2">女</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="生日">
        <a-date-picker v-model:value="modalForm.birthday" style="width: 100%" />
      </a-form-item>
      <a-form-item label="邮箱">
        <a-input v-model:value="modalForm.email" placeholder="请输入邮箱" />
      </a-form-item>
      <a-form-item label="地址">
        <a-input v-model:value="modalForm.address" placeholder="请输入地址" />
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea v-model:value="modalForm.remark" :rows="2" placeholder="选填" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 会员详情抽屉 -->
  <a-drawer v-model:open="detailVisible" title="会员详情" placement="right" width="640px" :footer="null">
    <a-spin :spinning="detailLoading">
      <template v-if="detailData.id">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="姓名">{{ detailData.name }}</a-descriptions-item>
          <a-descriptions-item label="手机号">{{ detailData.phone }}</a-descriptions-item>
          <a-descriptions-item label="性别">{{ genderText(detailData.gender) }}</a-descriptions-item>
          <a-descriptions-item label="生日">{{ detailData.birthday || '-' }}</a-descriptions-item>
          <a-descriptions-item label="邮箱">{{ detailData.email || '-' }}</a-descriptions-item>
          <a-descriptions-item label="积分">{{ detailData.points ?? 0 }}</a-descriptions-item>
          <a-descriptions-item label="累计消费">¥{{ formatMoney(detailData.totalSpent) }}</a-descriptions-item>
          <a-descriptions-item label="累计积分">{{ detailData.totalPoints ?? 0 }}</a-descriptions-item>
          <a-descriptions-item label="注册时间">{{ detailData.createTime }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="detailData.active === 1 ? 'green' : 'default'">{{ detailData.active === 1 ? '启用' : '停用' }}</a-tag>
          </a-descriptions-item>
        </a-descriptions>

        <a-divider>积分记录</a-divider>
        <a-table :columns="pointsColumns" :data-source="detailData.pointsRecords || []" :pagination="false" size="small" row-key="id">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'points'">
              <span :style="{ color: record.points > 0 ? '#52c41a' : '#f5222d', fontWeight: 500 }">
                {{ record.points > 0 ? '+' : '' }}{{ record.points }}
              </span>
            </template>
          </template>
        </a-table>
      </a-spin>
    </a-drawer>
  </a-drawer>

  <!-- 积分调整弹窗 -->
  <a-modal v-model:open="pointsVisible" title="积分调整" :confirm-loading="pointsLoading" width="400px" @ok="handlePointsOk">
    <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="当前积分">
        <span style="font-weight: 500">{{ currentMemberPoints }}</span>
      </a-form-item>
      <a-form-item label="调整类型">
        <a-radio-group v-model:value="pointsForm.type">
          <a-radio value="add">增加</a-radio>
          <a-radio value="deduct">扣除</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="调整数量" required>
        <a-input-number v-model:value="pointsForm.amount" :min="1" style="width: 100%" />
      </a-form-item>
      <a-form-item label="原因">
        <a-input v-model:value="pointsForm.reason" placeholder="如 手动调整" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { memberApi } from '@/api/erp'

const reportRef = ref<InstanceType<typeof ARReportPage>>()

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '姓名/手机号', width: 200 },
  { key: 'gender', type: 'select', label: '性别', placeholder: '全部', options: [
    { label: '男', value: 1 }, { label: '女', value: 2 }
  ]}
]

const columns: any[] = [
  { title: '姓名', dataIndex: 'name', key: 'name', width: 100 },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '性别', dataIndex: 'gender', key: 'gender', width: 60 },
  { title: '积分', dataIndex: 'points', key: 'points', width: 80, align: 'right' },
  { title: '累计消费', dataIndex: 'totalSpent', key: 'totalSpent', width: 120, align: 'right' },
  { title: '累计积分', dataIndex: 'totalPoints', key: 'totalPoints', width: 100, align: 'right' },
  { title: '生日', dataIndex: 'birthday', key: 'birthday', width: 110 },
  { title: '状态', dataIndex: 'active', key: 'active', width: 80 },
  { title: '注册时间', dataIndex: 'createTime', key: 'createTime', width: 150 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

const pointsColumns: any[] = [
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 150 },
  { title: '积分变动', dataIndex: 'points', key: 'points', width: 100 },
  { title: '类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '说明', dataIndex: 'remark', key: 'remark', width: 150 }
]

function genderText(val: number | undefined): string {
  return val === 1 ? '男' : val === 2 ? '女' : '-'
}

function formatMoney(val: number | null | undefined): string {
  return (val ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fetcher(params: Record<string, any>) {
  return memberApi.page(params)
}

const modalVisible = ref(false)
const modalLoading = ref(false)
const editingMember = ref<any>(null)
const modalForm = reactive<Record<string, any>>({})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<any>({})

const pointsVisible = ref(false)
const pointsLoading = ref(false)
const currentMemberPoints = ref(0)
const pointsForm = reactive({ memberId: undefined as number | undefined, type: 'add', amount: 0, reason: '' })

function openCreate() {
  editingMember.value = null
  Object.assign(modalForm, { name: '', phone: '', gender: 0, birthday: undefined, email: '', address: '', remark: '' })
  modalVisible.value = true
}

function openEdit(record: any) {
  editingMember.value = record
  Object.assign(modalForm, {
    name: record.name, phone: record.phone, gender: record.gender ?? 0,
    birthday: record.birthday ? dayjs(record.birthday) : undefined,
    email: record.email || '', address: record.address || '', remark: record.remark || ''
  })
  modalVisible.value = true
}

async function openView(record: any) {
  detailLoading.value = true
  detailVisible.value = true
  try {
    const res = await memberApi.getById(record.id)
    detailData.value = res as any
  } catch (e) {
    console.warn('[会员档案] 加载详情失败', e)
    detailData.value = record
  } finally {
    detailLoading.value = false
  }
}

function openPoints(record: any) {
  currentMemberPoints.value = record.points || 0
  pointsForm.memberId = record.id
  pointsForm.type = 'add'
  pointsForm.amount = 0
  pointsForm.reason = ''
  pointsVisible.value = true
}

async function handleModalOk() {
  if (!modalForm.name) { message.warning('请输入姓名'); return }
  if (!modalForm.phone) { message.warning('请输入手机号'); return }
  modalLoading.value = true
  try {
    const payload = {
      ...modalForm,
      birthday: modalForm.birthday ? dayjs(modalForm.birthday).format('YYYY-MM-DD') : undefined
    }
    if (editingMember.value) {
      await memberApi.update(editingMember.value.id, payload)
      message.success('更新成功')
    } else {
      await memberApi.create(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  } finally {
    modalLoading.value = false
  }
}

async function toggleActive(record: any, checked: boolean) {
  try {
    await memberApi.update(record.id, { active: checked ? 1 : 0 })
    message.success(checked ? '已启用' : '已停用')
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  }
}

async function handlePointsOk() {
  if (!pointsForm.amount || pointsForm.amount <= 0) { message.warning('请输入调整数量'); return }
  pointsLoading.value = true
  try {
    const payload = {
      memberId: pointsForm.memberId,
      points: pointsForm.type === 'add' ? pointsForm.amount : -pointsForm.amount,
      reason: pointsForm.reason || '手动调整'
    }
    await memberApi.adjustPoints(payload)
    message.success('积分调整成功')
    pointsVisible.value = false
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.message || '积分调整失败')
  } finally {
    pointsLoading.value = false
  }
}
</script>
