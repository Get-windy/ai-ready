<template>
  <div class="approval-detail-page">
    <van-nav-bar
      title="审批详情"
      left-arrow
      @click-left="goBack"
    >
      <template #right>
        <van-icon name="ellipsis" size="18" @click="showActions = true" />
      </template>
    </van-nav-bar>

    <div class="detail-content" v-if="approvalDetail">
      <div class="status-card" :class="approvalDetail.status">
        <div class="status-icon">
          <van-icon :name="getStatusIcon(approvalDetail.status)" size="32" />
        </div>
        <div class="status-text">{{ approvalDetail.statusLabel }}</div>
        <div class="status-time">{{ approvalDetail.createTime }}</div>
      </div>

      <van-cell-group inset title="基本信息">
        <van-cell title="审批类型" :value="approvalDetail.typeLabel" />
        <van-cell title="审批标题" :value="approvalDetail.title" />
        <van-cell title="申请金额" :value="approvalDetail.amount" />
        <van-cell title="申请人" :value="approvalDetail.applicant" />
        <van-cell title="申请部门" :value="approvalDetail.department" />
        <van-cell title="申请时间" :value="approvalDetail.createTime" />
      </van-cell-group>

      <van-cell-group inset title="审批内容">
        <div class="content-box">
          {{ approvalDetail.content }}
        </div>
      </van-cell-group>

      <van-cell-group inset title="附件">
        <van-cell v-for="file in approvalDetail.attachments" :key="file.id" :title="file.name">
          <template #icon>
            <van-icon :name="getFileIcon(file.type)" size="20" class="file-icon" />
          </template>
          <template #value>
            <van-button size="small" type="primary" plain @click="previewFile(file)">预览</van-button>
          </template>
        </van-cell>
        <van-empty v-if="!approvalDetail.attachments?.length" description="暂无附件" />
      </van-cell-group>

      <van-cell-group inset title="审批流程">
        <van-steps direction="vertical" :active="approvalDetail.currentStep">
          <van-step v-for="step in approvalDetail.flowSteps" :key="step.id">
            <div class="step-content">
              <div class="step-title">{{ step.title }}</div>
              <div class="step-user">{{ step.userName }}</div>
              <div class="step-time">{{ step.time }}</div>
              <div class="step-remark" v-if="step.remark">{{ step.remark }}</div>
            </div>
          </van-step>
        </van-steps>
      </van-cell-group>

      <van-cell-group inset title="审批记录">
        <van-cell
          v-for="record in approvalDetail.records"
          :key="record.id"
          :title="record.userName"
          :label="record.time"
        >
          <template #icon>
            <van-icon :name="record.action === 'approve' ? 'success' : 'cross'" :class="record.action" />
          </template>
          <template #value>
            <van-tag :type="record.action === 'approve' ? 'success' : 'danger'">
              {{ record.action === 'approve' ? '通过' : '拒绝' }}
            </van-tag>
          </template>
        </van-cell>
      </van-cell-group>
    </div>

    <van-action-bar v-if="approvalDetail?.status === 'pending'">
      <van-action-bar-button type="danger" text="拒绝" @click="handleReject" />
      <van-action-bar-button type="primary" text="通过" @click="handleApprove" />
    </van-action-bar>

    <van-action-sheet
      v-model:show="showActions"
      :actions="actions"
      cancel-text="取消"
      close-on-click-action
      @select="onActionSelect"
    />

    <van-popup v-model:show="showRejectPopup" position="bottom" round>
      <div class="reject-popup">
        <div class="popup-header">
          <span>拒绝原因</span>
          <van-icon name="cross" @click="showRejectPopup = false" />
        </div>
        <van-field
          v-model="rejectReason"
          rows="4"
          autosize
          type="textarea"
          placeholder="请输入拒绝原因"
        />
        <div class="popup-actions">
          <van-button block type="primary" @click="confirmReject">确认拒绝</van-button>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showTransferPopup" position="bottom" round>
      <div class="reject-popup">
        <div class="popup-header">
          <span>转交审批</span>
          <van-icon name="cross" @click="showTransferPopup = false" />
        </div>
        <div class="user-list">
          <van-cell
            v-for="user in availableReviewers"
            :key="user.id"
            :title="user.name"
            :label="user.department"
            is-link
            @click="confirmTransfer(user)"
          />
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showCountersignPopup" position="bottom" round>
      <div class="reject-popup">
        <div class="popup-header">
          <span>加签审批</span>
          <van-icon name="cross" @click="showCountersignPopup = false" />
        </div>
        <van-field
          v-model="countersignRemark"
          rows="2"
          autosize
          type="textarea"
          placeholder="请输入加签意见（可选）"
        />
        <div class="popup-actions">
          <van-button block type="primary" @click="confirmCountersign">确认加签</van-button>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showReturnPopup" position="bottom" round>
      <div class="reject-popup">
        <div class="popup-header">
          <span>退回申请</span>
          <van-icon name="cross" @click="showReturnPopup = false" />
        </div>
        <van-field
          v-model="returnReason"
          rows="4"
          autosize
          type="textarea"
          placeholder="请输入退回原因"
        />
        <div class="popup-actions">
          <van-button block type="primary" @click="confirmReturn">确认退回</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showConfirmDialog, showSuccessToast, showToast } from 'vant'

const router = useRouter()
const route = useRoute()

const showActions = ref(false)
const showRejectPopup = ref(false)
const showTransferPopup = ref(false)
const showCountersignPopup = ref(false)
const showReturnPopup = ref(false)
const rejectReason = ref('')
const countersignRemark = ref('')
const returnReason = ref('')

const actions = [
  { name: '转交', value: 'transfer' },
  { name: '加签', value: 'countersign' },
  { name: '退回', value: 'return' },
  { name: '打印', value: 'print' }
]

const availableReviewers = [
  { id: 1, name: '李四', department: '技术部' },
  { id: 2, name: '赵六', department: '财务部' },
  { id: 3, name: '钱七', department: '运营部' }
]

const approvalDetail = ref<any>(null)

onMounted(() => {
  loadDetail()
})

const loadDetail = () => {
  const id = route.params.id
  approvalDetail.value = {
    id,
    title: '采购订单审批',
    type: 'purchase',
    typeLabel: '采购审批',
    amount: '¥58,000',
    status: 'pending',
    statusLabel: '待审批',
    applicant: '张三',
    department: '采购部',
    createTime: '2024-01-15 10:30',
    content: '申请采购办公用品一批，包括：打印机2台、办公桌椅5套、文件柜3个。总金额为58,000元。供应商：北京办公用品有限公司。交货日期：2024年1月25日前。',
    currentStep: 2,
    attachments: [
      { id: 1, name: '采购清单.xlsx', type: 'excel', url: '' },
      { id: 2, name: '供应商报价单.pdf', type: 'pdf', url: '' }
    ],
    flowSteps: [
      { id: 1, title: '提交申请', userName: '张三', time: '2024-01-15 10:30', remark: '' },
      { id: 2, title: '部门主管审批', userName: '李四', time: '2024-01-15 14:20', remark: '同意，请财务审核' },
      { id: 3, title: '财务审批', userName: '王五', time: '', remark: '' },
      { id: 4, title: '总经理审批', userName: '赵六', time: '', remark: '' }
    ],
    records: [
      { id: 1, userName: '李四', action: 'approve', time: '2024-01-15 14:20', remark: '同意，请财务审核' }
    ]
  }
}

const getStatusIcon = (status: string) => {
  const icons: Record<string, string> = {
    pending: 'clock-o',
    approved: 'passed',
    rejected: 'close'
  }
  return icons[status] || 'todo-list-o'
}

const getFileIcon = (type: string) => {
  const icons: Record<string, string> = {
    excel: 'description',
    pdf: 'description',
    word: 'description',
    image: 'photo-o'
  }
  return icons[type] || 'description'
}

const previewFile = (file: any) => {
  showToast(`预览文件: ${file.name}`)
}

const goBack = () => {
  router.back()
}

const handleApprove = async () => {
  try {
    await showConfirmDialog({
      title: '确认通过',
      message: '确定要通过该审批吗？'
    })
    showSuccessToast('审批通过')
    router.back()
  } catch {
    // 用户取消
  }
}

const handleReject = () => {
  showRejectPopup.value = true
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) {
    showToast('请输入拒绝原因')
    return
  }
  showSuccessToast('已拒绝')
  showRejectPopup.value = false
  router.back()
}

const onActionSelect = (action: any) => {
  switch (action.value) {
    case 'transfer':
      showTransferPopup.value = true
      break
    case 'countersign':
      showCountersignPopup.value = true
      break
    case 'return':
      showReturnPopup.value = true
      break
    case 'print':
      try {
        window.print()
        showSuccessToast('已发送打印请求')
      } catch {
        showSuccessToast('打印功能已触发')
      }
      break
  }
}

const confirmTransfer = (user: any) => {
  showSuccessToast(`已转交给 ${user.name}`)
  showTransferPopup.value = false
}

const confirmCountersign = async () => {
  try {
    await showConfirmDialog({
      title: '确认加签',
      message: countersignRemark.value
        ? `加签意见: ${countersignRemark.value}\n确定要加签该审批吗？`
        : '确定要加签该审批吗？'
    })
    showSuccessToast('加签成功')
    showCountersignPopup.value = false
    countersignRemark.value = ''
  } catch {
    // 用户取消
  }
}

const confirmReturn = () => {
  if (!returnReason.value.trim()) {
    showToast('请输入退回原因')
    return
  }
  showSuccessToast('已退回申请')
  showReturnPopup.value = false
  returnReason.value = ''
}
</script>

<style scoped lang="scss">
.approval-detail-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 60px;
}

.status-card {
  padding: 24px;
  text-align: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;

  &.approved {
    background: linear-gradient(135deg, #07c160 0%, #10b981 100%);
  }

  &.rejected {
    background: linear-gradient(135deg, #ee0a24 0%, #ef4444 100%);
  }

  .status-icon {
    margin-bottom: 8px;
  }

  .status-text {
    font-size: 18px;
    font-weight: 500;
    margin-bottom: 4px;
  }

  .status-time {
    font-size: 12px;
    opacity: 0.8;
  }
}

.content-box {
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}

.file-icon {
  margin-right: 8px;
  color: #1989fa;
}

.step-content {
  .step-title {
    font-size: 14px;
    font-weight: 500;
    color: #333;
  }

  .step-user {
    font-size: 12px;
    color: #666;
    margin-top: 4px;
  }

  .step-time {
    font-size: 12px;
    color: #999;
    margin-top: 2px;
  }

  .step-remark {
    font-size: 12px;
    color: #1989fa;
    margin-top: 4px;
    padding: 4px 8px;
    background: #f0f7ff;
    border-radius: 4px;
  }
}

.reject-popup {
  padding: 16px;

  .popup-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 16px;
    font-weight: 500;
    margin-bottom: 16px;
  }

  .popup-actions {
    margin-top: 16px;
  }
}
</style>