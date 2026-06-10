<template>
  <div class="tenant-approval">
    <a-card>
      <template #title>
        <div class="card-header">
          <span>租户注册审批</span>
          <a-badge :count="pendingList.length" :overflow-count="99">
            <a-tag color="orange">待审核</a-tag>
          </a-badge>
        </div>
      </template>

      <!-- 加载状态 -->
      <div v-if="loading" style="text-align: center; padding: 60px 0">
        <a-spin size="large" />
        <p style="margin-top: 16px; color: #999">加载中...</p>
      </div>

      <!-- 无数据 -->
      <a-empty v-else-if="pendingList.length === 0" description="暂无待审核的租户注册申请">
        <template #extra>
          <a-button type="primary" @click="fetchList">刷新</a-button>
        </template>
      </a-empty>

      <!-- 审核列表 -->
      <a-list v-else :data-source="pendingList" item-layout="vertical" size="large">
        <template #renderItem="{ item, index }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <div class="item-title">
                  <span>{{ item.tenantName }}</span>
                  <a-tag color="blue">{{ item.tenantCode }}</a-tag>
                </div>
              </template>
              <template #description>
                <a-descriptions :column="2" size="small">
                  <a-descriptions-item label="联系人">{{ item.contactPerson }}</a-descriptions-item>
                  <a-descriptions-item label="联系电话">{{ item.contactPhone }}</a-descriptions-item>
                  <a-descriptions-item label="邮箱">{{ item.contactEmail }}</a-descriptions-item>
                  <a-descriptions-item label="申请时间">{{ item.createTime }}</a-descriptions-item>
                </a-descriptions>
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-button type="primary" ghost size="small" @click="handleApprove(item)">
                <template #icon><CheckOutlined /></template>
                通过
              </a-button>
              <a-button danger ghost size="small" @click="handleReject(item)">
                <template #icon><CloseOutlined /></template>
                驳回
              </a-button>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </a-card>

    <!-- 通过审批确认 -->
    <a-modal
      v-model:open="approveModalVisible"
      title="确认通过审批"
      @ok="confirmApprove"
      :confirm-loading="approving"
    >
      <p>确认通过 <strong>{{ currentItem?.tenantName }}</strong> 的注册申请？</p>
      <p style="color: #666; font-size: 13px">
        通过后将自动创建管理员账号并初始化租户环境。
      </p>
      <a-textarea
        v-model:value="approveRemark"
        placeholder="备注（可选）"
        :rows="3"
      />
    </a-modal>

    <!-- 驳回确认 -->
    <a-modal
      v-model:open="rejectModalVisible"
      title="驳回注册申请"
      @ok="confirmReject"
      :confirm-loading="rejecting"
      :ok-button-props="{ danger: true }"
    >
      <p>确认驳回 <strong>{{ currentItem?.tenantName }}</strong> 的注册申请？</p>
      <a-form-item
        label="驳回原因"
        :validate-status="rejectReasonError ? 'error' : undefined"
        :help="rejectReasonError"
      >
        <a-textarea
          v-model:value="rejectReason"
          placeholder="请填写驳回原因（必填）"
          :rows="3"
        />
      </a-form-item>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { CheckOutlined, CloseOutlined } from '@ant-design/icons-vue'
import { tenantApprovalApi, type SysTenant } from '@/api/tenant'

const loading = ref(false)
const approving = ref(false)
const rejecting = ref(false)
const pendingList = ref<SysTenant[]>([])

const approveModalVisible = ref(false)
const rejectModalVisible = ref(false)
const currentItem = ref<SysTenant | null>(null)
const approveRemark = ref('')
const rejectReason = ref('')
const rejectReasonError = ref('')

onMounted(() => {
  fetchList()
})

// SSE 监听：当收到缓存失效通知时刷新列表
// 其他管理员审批后，当前用户会收到 SSE 通知
import { getSseClient } from '@/utils/sseClient'

onMounted(() => {
  fetchList()
  const sse = getSseClient()
  sse.on('cache-invalidate', () => {
    // 延迟一下再刷新，等后端处理完成
    setTimeout(() => fetchList(), 1000)
  })
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await tenantApprovalApi.getPending()
    pendingList.value = (res as any).data || []
  } catch (error: any) {
    message.error('获取待审核列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleApprove = (item: SysTenant) => {
  currentItem.value = item
  approveRemark.value = ''
  approveModalVisible.value = true
}

const confirmApprove = async () => {
  if (!currentItem.value) return
  approving.value = true
  try {
    await tenantApprovalApi.approve(currentItem.value.id, approveRemark.value || undefined)
    message.success(`已通过 ${currentItem.value.tenantName} 的注册申请`)
    approveModalVisible.value = false
    await fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '审批失败')
  } finally {
    approving.value = false
  }
}

const handleReject = (item: SysTenant) => {
  currentItem.value = item
  rejectReason.value = ''
  rejectReasonError.value = ''
  rejectModalVisible.value = true
}

const confirmReject = async () => {
  if (!currentItem.value) return
  if (!rejectReason.value.trim()) {
    rejectReasonError.value = '请填写驳回原因'
    return
  }
  rejectReasonError.value = ''
  rejecting.value = true
  try {
    await tenantApprovalApi.reject(currentItem.value.id, rejectReason.value.trim())
    message.success(`已驳回 ${currentItem.value.tenantName} 的注册申请`)
    rejectModalVisible.value = false
    await fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '操作失败')
  } finally {
    rejecting.value = false
  }
}
</script>

<style scoped>
.tenant-approval {
  padding: 0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
