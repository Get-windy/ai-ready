<template>
  <DetailLayout
    :breadcrumb-items="breadcrumbItems"
    :title="customer?.name || ''"
    :status="customer?.status === 0 ? '正常' : '停用'"
    :status-type="customer?.status === 0 ? 'success' : 'danger'"
    :show-pager="true"
    :current-index="currentIndex"
    :total-count="totalCount"
    :tabs="tabs"
    :active-tab="activeTab"
    :show-related-documents="true"
    :related-documents="relatedDocuments"
    :show-activity-log="true"
    :activity-logs="activityLogs"
    :loading="loading"
    :error="error"
    @breadcrumb-click="handleBreadcrumbClick"
    @prev="handlePrev"
    @next="handleNext"
    @tab-change="handleTabChange"
    @related-click="handleRelatedClick"
  >
    <template #header-extra>
      <a-tag :color="getLevelColor(customer?.level)">
        {{ getLevelName(customer?.level) }}
      </a-tag>
    </template>

    <template #actions>
      <a-button type="primary" @click="handleEdit">编辑</a-button>
      <a-button @click="handleFollow">添加跟进</a-button>
      <PrintButton
        template-type="customer"
        :business-id="customer?.id"
        business-type="customer"
        button-text="打印"
        @print-success="handlePrintSuccess"
        @print-error="handlePrintError"
      />
    </template>

    <template #more-actions>
      <a-dropdown>
        <a-button>更多操作</a-button>
        <template #overlay>
          <a-menu>
            <a-menu-item @click="handleConvertToOpportunity">转为商机</a-menu-item>
            <a-menu-item @click="handleMergeCustomer">合并客户</a-menu-item>
            <a-menu-divider />
            <a-menu-item danger @click="handleDelete">删除</a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </template>

    <!-- 基本信息 Tab -->
    <template #tab-basic>
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="客户编码">{{ customer?.code }}</a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ customer?.name }}</a-descriptions-item>
        <a-descriptions-item label="联系人">{{ customer?.contactPerson }}</a-descriptions-item>
        <a-descriptions-item label="联系电话">{{ customer?.phone }}</a-descriptions-item>
        <a-descriptions-item label="邮箱">{{ customer?.email }}</a-descriptions-item>
        <a-descriptions-item label="行业">{{ customer?.industry }}</a-descriptions-item>
        <a-descriptions-item label="客户等级">
          <a-tag :color="getLevelColor(customer?.level)">{{ getLevelName(customer?.level) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="customer?.status === 0 ? 'success' : 'error'">
            {{ customer?.status === 0 ? '正常' : '停用' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="地址" :span="2">{{ customer?.address || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ customer?.createTime }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ customer?.description || '-' }}</a-descriptions-item>
      </a-descriptions>
    </template>

    <!-- 关联单据 Tab -->
    <template #tab-related>
      <a-tabs default-active-key="quotations" size="small">
        <a-tab-pane key="quotations" tab="报价单">
          <VxeTableList :columns="quotationVxeColumns" :data-source="quotations" row-key="id" :pagination="false" :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false" :show-export="false" :show-batch-delete="false" />
        </a-tab-pane>
        <a-tab-pane key="contracts" tab="合同">
          <VxeTableList :columns="contractVxeColumns" :data-source="contracts" row-key="id" :pagination="false" :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false" :show-export="false" :show-batch-delete="false" />
        </a-tab-pane>
        <a-tab-pane key="orders" tab="订单">
          <VxeTableList :columns="orderVxeColumns" :data-source="orders" row-key="id" :pagination="false" :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false" :show-export="false" :show-batch-delete="false" />
        </a-tab-pane>
      </a-tabs>
    </template>

    <!-- 跟进记录 Tab -->
    <template #tab-follows>
      <a-timeline>
        <a-timeline-item
          v-for="follow in follows"
          :key="follow.id"
          :color="getFollowColor(follow.followType)"
        >
          <p class="follow-header">
            <span class="follow-type">{{ getFollowTypeName(follow.followType) }}</span>
            <span class="follow-time">{{ follow.createTime }}</span>
          </p>
          <p class="follow-content">{{ follow.content }}</p>
          <p v-if="follow.result" class="follow-result">
            结果：{{ getFollowResultName(follow.result) }}
          </p>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-if="follows.length === 0" description="暂无跟进记录" />
    </template>
  </DetailLayout>

  <!-- 添加跟进记录弹窗 -->
  <a-modal
    v-model:open="followModalVisible"
    title="添加跟进记录"
    width="550px"
    :confirm-loading="followModalLoading"
    @ok="handleFollowSubmit"
    @cancel="followModalVisible = false"
  >
    <a-form :model="followForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="跟进类型" required>
        <a-select v-model:value="followForm.followType" placeholder="请选择跟进类型">
          <a-select-option :value="1">电话</a-select-option>
          <a-select-option :value="2">拜访</a-select-option>
          <a-select-option :value="3">邮件</a-select-option>
          <a-select-option :value="4">微信</a-select-option>
          <a-select-option :value="5">其他</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="跟进日期" required>
        <a-date-picker v-model:value="followForm.followDate" style="width: 100%" placeholder="请选择跟进日期" />
      </a-form-item>
      <a-form-item label="跟进内容" required>
        <a-textarea v-model:value="followForm.content" placeholder="请输入跟进内容" :rows="5" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { DetailLayout } from '@ai-ready/components'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { customerApi, type CustomerInfo } from '@/api/customer'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

const router = useRouter()
const route = useRoute()

const customer = ref<CustomerInfo | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const currentIndex = ref(1)
const totalCount = ref(1)
const activeTab = ref('basic')

// ── 跟进记录弹窗 ──────────────────────────────────────
const followModalVisible = ref(false)
const followModalLoading = ref(false)
const followForm = reactive({
  followType: 1,
  content: '',
  followDate: undefined as any
})

// 关联数据
const quotations = ref<any[]>([])
const contracts = ref<any[]>([])
const orders = ref<any[]>([])
const follows = ref<any[]>([])

const breadcrumbItems = computed(() => [
  { text: 'CRM', path: '/crm' },
  { text: '客户管理', path: '/crm/customer' },
  { text: customer.value?.name || '' }
])

const tabs = [
  { key: 'basic', label: '基本信息' },
  { key: 'related', label: '关联单据' },
  { key: 'follows', label: '跟进记录', count: follows.value.length }
]

const quotationVxeColumns = [
  { field: 'quotationNo', title: '报价单号', width: 160 },
  { field: 'totalAmount', title: '金额', width: 100 },
  { field: 'createTime', title: '日期', width: 140 },
  { field: 'status', title: '状态', width: 80 }
]

const contractVxeColumns = [
  { field: 'contractNo', title: '合同号', width: 160 },
  { field: 'totalAmount', title: '金额', width: 100 },
  { field: 'createTime', title: '日期', width: 140 },
  { field: 'status', title: '状态', width: 80 }
]

const orderVxeColumns = [
  { field: 'orderNo', title: '订单号', width: 160 },
  { field: 'totalAmount', title: '金额', width: 100 },
  { field: 'createTime', title: '日期', width: 140 },
  { field: 'status', title: '状态', width: 80 }
]

const relatedDocuments = computed(() => {
  const docs = []
  if (quotations.value.length > 0) docs.push({ id: 'q', type: '报价单', no: `${quotations.value.length} 条`, path: '' })
  if (contracts.value.length > 0) docs.push({ id: 'c', type: '合同', no: `${contracts.value.length} 条`, path: '' })
  if (orders.value.length > 0) docs.push({ id: 'o', type: '订单', no: `${orders.value.length} 条`, path: '' })
  return docs
})

const activityLogs = computed(() => {
  return follows.value.slice(0, 5).map(f => ({
    id: f.id,
    time: f.createTime,
    user: f.creatorName || '系统',
    action: `添加了${getFollowTypeName(f.followType)}跟进`
  }))
})

// 辅助方法
const getLevelColor = (level?: number) => {
  const colors: Record<number, string> = { 1: '#ff4d4f', 2: '#faad14', 3: '#1890ff', 4: '#52c41a' }
  return level ? (colors[level] || '#999') : '#999'
}
const getLevelName = (level?: number) => {
  const names: Record<number, string> = { 1: 'VIP客户', 2: '重要客户', 3: '普通客户', 4: '潜在客户' }
  return level ? (names[level] || '未知') : '未知'
}
const getFollowTypeName = (type: number) => {
  const names: Record<number, string> = { 1: '电话', 2: '拜访', 3: '邮件', 4: '微信', 5: '其他' }
  return names[type] || '未知'
}
const getFollowResultName = (result: number) => {
  const names: Record<number, string> = { 1: '有意向', 2: '无意向', 3: '待跟进' }
  return names[result] || '未知'
}
const getFollowColor = (type: number) => {
  const colors: Record<number, string> = { 1: 'blue', 2: 'green', 3: 'orange', 4: 'cyan', 5: 'gray' }
  return colors[type] || 'gray'
}

const fetchCustomerDetail = async () => {
  loading.value = true; error.value = null
  try {
    const customerId = Number(route.params.id)
    if (isNaN(customerId)) { error.value = '无效的客户ID'; return }
    const res = await customerApi.getById(customerId)
    customer.value = res.data
  } catch (err: any) {
    console.warn('[CRM客户详情] 获取客户详情失败', err)
    error.value = err?.message || '获取客户详情失败'
  } finally {
    loading.value = false
  }
}

const handleBreadcrumbClick = (item: any) => {
  if (item.path) router.push(item.path)
}
const handlePrev = () => {
  const id = Number(route.params.id)
  if (isNaN(id)) return
  if (customer.value && currentIndex.value > 1) {
    router.push(`/crm/customer/${id - 1}`)
  } else {
    message.warning('已经是第一条记录')
  }
}
const handleNext = () => {
  const id = Number(route.params.id)
  if (isNaN(id)) return
  if (customer.value && currentIndex.value < totalCount.value) {
    router.push(`/crm/customer/${id + 1}`)
  } else {
    message.warning('已经是最后一条记录')
  }
}
const handleTabChange = (key: string) => { activeTab.value = key }
const handleRelatedClick = () => { activeTab.value = 'related' }
const handleEdit = () => router.push(`/crm/customer/${route.params.id}/edit`)
const handleFollow = () => {
  const today = new Date()
  followForm.followType = 1
  followForm.content = ''
  followForm.followDate = today
  followModalVisible.value = true
}

const handleFollowSubmit = async () => {
  if (!followForm.content.trim()) {
    message.warning('请输入跟进内容')
    return
  }
  if (!followForm.followDate) {
    message.warning('请选择跟进日期')
    return
  }
  followModalLoading.value = true
  try {
    await customerApi.addFollowRecord(customer.value!.id, {
      customerId: customer.value!.id,
      followType: followForm.followType,
      content: followForm.content,
      followDate: followForm.followDate
    })
    message.success('跟进记录添加成功')
    followModalVisible.value = false
    activeTab.value = 'follows'
  } catch (err) {
    console.warn('[CRM客户详情] 添加跟进记录失败', err)
    message.error('添加跟进记录失败')
  } finally {
    followModalLoading.value = false
  }
}
const handleDelete = () => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除客户"${customer.value?.name}"吗？`,
    async onOk() {
      if (customer.value) {
        try { await customerApi.delete(customer.value.id); message.success('删除成功'); router.push('/crm/customer') }
        catch (err) { console.warn('[CRM客户详情] 删除客户失败', err); message.error('删除失败') }
      }
    }
  })
}
const handleConvertToOpportunity = () => {
  Modal.confirm({
    title: '转为商机',
    content: `确定要将客户"${customer.value?.name}"转为商机吗？`,
    okText: '确认转换',
    cancelText: '取消',
    centered: true,
    onOk() {
      router.push(`/crm/opportunity/create?customerId=${customer.value?.id}`)
    }
  })
}
const handleMergeCustomer = () => {
  Modal.confirm({
    title: '合并客户',
    content: `确定要合并客户"${customer.value?.name}"吗？请选择目标客户进行合并操作。`,
    okText: '去合并',
    cancelText: '取消',
    centered: true,
    onOk() {
      router.push(`/crm/customer/merge?sourceId=${customer.value?.id}`)
    }
  })
}
const handlePrintSuccess = () => message.success('打印成功')
const handlePrintError = (err: any) => message.error(`打印失败: ${err?.message || '未知错误'}`)

onMounted(() => fetchCustomerDetail())
defineExpose({ handleQuery: fetchCustomerDetail })
</script>

<style scoped>
.follow-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.follow-type {
  font-weight: 500;
  color: #303133;
}
.follow-time {
  font-size: 12px;
  color: #909399;
}
.follow-content {
  margin: 4px 0;
  color: #606266;
}
.follow-result {
  font-size: 12px;
  color: #909399;
}
</style>
