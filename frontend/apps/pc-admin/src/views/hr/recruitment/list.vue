<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__left">
        <h2 class="page-title">招聘管理</h2>
      </div>
      <div class="page-header__right">
        <a-button type="primary" @click="showCreateModal">
          <template #icon><PlusOutlined /></template>
          新增招聘
        </a-button>
      </div>
    </div>

    <div class="page-container__body">
      <a-card :bordered="false" class="search-card">
        <a-form layout="inline">
          <a-form-item label="岗位名称">
            <a-input v-model:value="searchForm.positionName" placeholder="请输入岗位名称" allow-clear />
          </a-form-item>
          <a-form-item label="招聘渠道">
            <a-select v-model:value="searchForm.channel" placeholder="请选择渠道" allow-clear style="width: 140px">
              <a-select-option value="ONLINE">网络招聘</a-select-option>
              <a-select-option value="HEADHUNTER">猎头</a-select-option>
              <a-select-option value="REFERRAL">内部推荐</a-select-option>
              <a-select-option value="CAMPUS">校园招聘</a-select-option>
              <a-select-option value="OTHER">其他</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="searchForm.status" placeholder="请选择状态" allow-clear style="width: 120px">
              <a-select-option :value="0">待审批</a-select-option>
              <a-select-option :value="1">招聘中</a-select-option>
              <a-select-option :value="2">已暂停</a-select-option>
              <a-select-option :value="3">已完成</a-select-option>
              <a-select-option :value="4">已关闭</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="紧急程度">
            <a-select v-model:value="searchForm.urgency" placeholder="请选择" allow-clear style="width: 120px">
              <a-select-option :value="1">普通</a-select-option>
              <a-select-option :value="2">紧急</a-select-option>
              <a-select-option :value="3">特急</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
          </a-form-item>
        </a-form>
      </a-card>

      <a-card :bordered="false" class="table-card">
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'urgency'">
              <a-tag :color="urgencyMap[record.urgency]?.color">{{ urgencyMap[record.urgency]?.text }}</a-tag>
            </template>
            <template v-if="column.key === 'channel'">
              <span>{{ channelMap[record.channel] || record.channel }}</span>
            </template>
            <template v-if="column.key === 'salary'">
              <span v-if="record.salaryMin || record.salaryMax">
                {{ formatSalary(record.salaryMin) }} - {{ formatSalary(record.salaryMax) }}
              </span>
              <span v-else>-</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="statusMap[record.status]?.color">{{ statusMap[record.status]?.text }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="showEditModal(record)">编辑</a-button>
                <a-button type="link" size="small" @click="showCandidates(record)">候选人</a-button>
                <a-popconfirm title="确认删除?" @confirm="handleDelete(record.id)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑招聘职位' : '新增招聘职位'"
      width="750px"
      @ok="handleSave"
    >
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="岗位名称" required>
              <a-input v-model:value="form.positionName" placeholder="请输入岗位名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="招聘人数" required>
              <a-input-number v-model:value="form.headcount" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="招聘渠道">
              <a-select v-model:value="form.channel" placeholder="请选择渠道">
                <a-select-option value="ONLINE">网络招聘</a-select-option>
                <a-select-option value="HEADHUNTER">猎头</a-select-option>
                <a-select-option value="REFERRAL">内部推荐</a-select-option>
                <a-select-option value="CAMPUS">校园招聘</a-select-option>
                <a-select-option value="OTHER">其他</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="紧急程度">
              <a-select v-model:value="form.urgency" placeholder="请选择">
                <a-select-option :value="1">普通</a-select-option>
                <a-select-option :value="2">紧急</a-select-option>
                <a-select-option :value="3">特急</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="薪资下限">
              <a-input-number v-model:value="form.salaryMin" :min="0" :precision="2" style="width: 100%" placeholder="元/月" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="薪资上限">
              <a-input-number v-model:value="form.salaryMax" :min="0" :precision="2" style="width: 100%" placeholder="元/月" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="学历要求">
              <a-select v-model:value="form.requiredEducation" placeholder="请选择" allow-clear>
                <a-select-option :value="3">高中</a-select-option>
                <a-select-option :value="4">大专</a-select-option>
                <a-select-option :value="5">本科</a-select-option>
                <a-select-option :value="6">硕士</a-select-option>
                <a-select-option :value="7">博士</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="经验要求">
              <a-input v-model:value="form.requiredExperience" placeholder="如: 3年以上" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="发布日期">
              <a-date-picker v-model:value="form.publishDate" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="截止日期">
              <a-date-picker v-model:value="form.expireDate" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="岗位描述" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
          <a-textarea v-model:value="form.description" placeholder="请输入岗位描述" :rows="3" />
        </a-form-item>
        <a-form-item label="任职要求" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
          <a-textarea v-model:value="form.requirements" placeholder="请输入任职要求" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

interface Recruitment {
  id: number
  positionName: string
  deptName: string
  headcount: number
  channel: string
  urgency: number
  status: number
  salaryMin: number
  salaryMax: number
  applicantCount: number
  hiredCount: number
  publishDate: string
  description: string
  requirements: string
  requiredEducation: number
  requiredExperience: string
  expireDate: string
}

const loading = ref(false)
const tableData = ref<Recruitment[]>([])
const modalVisible = ref(false)
const editingId = ref<number | null>(null)

const searchForm = reactive({
  positionName: '',
  channel: undefined as string | undefined,
  status: undefined as number | undefined,
  urgency: undefined as number | undefined,
})

const form = reactive({
  positionName: '',
  headcount: 1,
  channel: 'ONLINE',
  urgency: 1,
  salaryMin: undefined as number | undefined,
  salaryMax: undefined as number | undefined,
  requiredEducation: undefined as number | undefined,
  requiredExperience: '',
  publishDate: '',
  expireDate: '',
  description: '',
  requirements: '',
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  { title: '岗位名称', dataIndex: 'positionName', key: 'positionName', width: 150 },
  { title: '部门', dataIndex: 'deptName', key: 'deptName', width: 100 },
  { title: '招聘人数', dataIndex: 'headcount', key: 'headcount', width: 80, align: 'center' as const },
  { title: '渠道', dataIndex: 'channel', key: 'channel', width: 100 },
  { title: '紧急程度', key: 'urgency', width: 80, align: 'center' as const },
  { title: '薪资范围', key: 'salary', width: 140 },
  { title: '应聘/录用', key: 'counts', width: 100, align: 'center' as const,
    customRender: ({ record }: any) => `${record.applicantCount || 0} / ${record.hiredCount || 0}` },
  { title: '状态', key: 'status', width: 90, align: 'center' as const },
  { title: '发布日期', dataIndex: 'publishDate', key: 'publishDate', width: 110 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const },
]

const statusMap: Record<number, { text: string; color: string }> = {
  0: { text: '待审批', color: 'default' },
  1: { text: '招聘中', color: 'success' },
  2: { text: '已暂停', color: 'warning' },
  3: { text: '已完成', color: 'processing' },
  4: { text: '已关闭', color: 'error' },
}

const urgencyMap: Record<number, { text: string; color: string }> = {
  1: { text: '普通', color: 'default' },
  2: { text: '紧急', color: 'warning' },
  3: { text: '特急', color: 'error' },
}

const channelMap: Record<string, string> = {
  ONLINE: '网络招聘',
  HEADHUNTER: '猎头',
  REFERRAL: '内部推荐',
  CAMPUS: '校园招聘',
  OTHER: '其他',
}

const formatSalary = (val: number | undefined) => {
  if (!val) return '-'
  return val >= 10000 ? `${(val / 10000).toFixed(1)}万` : `${val}`
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/hr/recruitment/page', {
      params: {
        current: pagination.current,
        size: pagination.pageSize,
        positionName: searchForm.positionName || undefined,
        channel: searchForm.channel || undefined,
        status: searchForm.status,
        urgency: searchForm.urgency,
      },
    })
    const data = res.data?.data || res.data || {}
    tableData.value = data.records || []
    pagination.total = data.total || 0
  } catch (e: any) {
    message.error('获取招聘列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchForm, { positionName: '', channel: undefined, status: undefined, urgency: undefined })
  handleSearch()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const resetForm = () => {
  Object.assign(form, {
    positionName: '', headcount: 1, channel: 'ONLINE', urgency: 1,
    salaryMin: undefined, salaryMax: undefined, requiredEducation: undefined,
    requiredExperience: '', publishDate: '', expireDate: '', description: '', requirements: '',
  })
  editingId.value = null
}

const showCreateModal = () => {
  resetForm()
  modalVisible.value = true
}

const showEditModal = (record: Recruitment) => {
  editingId.value = record.id
  Object.assign(form, {
    positionName: record.positionName,
    headcount: record.headcount,
    channel: record.channel,
    urgency: record.urgency,
    salaryMin: record.salaryMin,
    salaryMax: record.salaryMax,
    requiredEducation: record.requiredEducation,
    requiredExperience: record.requiredExperience,
    publishDate: record.publishDate,
    expireDate: record.expireDate,
    description: record.description,
    requirements: record.requirements,
  })
  modalVisible.value = true
}

const handleSave = async () => {
  if (!form.positionName) {
    message.warning('请输入岗位名称')
    return
  }
  try {
    if (editingId.value) {
      await request.put(`/api/hr/recruitment/${editingId.value}`, form)
      message.success('更新成功')
    } else {
      await request.post('/api/hr/recruitment', form)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '操作失败')
  }
}

const handleDelete = async (id: number) => {
  try {
    await request.delete(`/api/hr/recruitment/${id}`)
    message.success('删除成功')
    fetchData()
  } catch (e: any) {
    message.error('删除失败')
  }
}

const showCandidates = (record: Recruitment) => {
  message.info(`候选人管理: ${record.positionName} (开发中)`)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.page-container {
  padding: 16px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
.search-card {
  margin-bottom: 16px;
}
.table-card {
  margin-bottom: 16px;
}
</style>
