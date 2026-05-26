<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const supplierId = route.params.id as string

interface InquiryRecord {
  id: number
  inquiryNo: string
  inquiryTitle: string
  inquiryStatus: number
  createTime: string
  deadline: string
  quotationAmount: number
  quotationStatus: number
  quotationTime: string
  remark: string
}

interface SupplierInfo {
  supplierCode: string
  supplierName: string
}

const supplier = ref<SupplierInfo | null>(null)
const inquiries = ref<InquiryRecord[]>([])
const loading = ref(false)
const showCreateModal = ref(false)
const createForm = ref({
  inquiryTitle: '',
  deadline: '',
  remark: ''
})

const inquiryStatusOptions = [
  { value: 0, label: '待报价', color: '#1988fa' },
  { value: 1, label: '已报价', color: '#07c160' },
  { value: 2, label: '已接受', color: '#07c160' },
  { value: 3, label: '已拒绝', color: '#f44' },
  { value: 4, label: '已过期', color: '#969799' }
]

onMounted(async () => {
  await loadSupplier()
  await loadInquiries()
})

const loadSupplier = async () => {
  try {
    const response = await fetch(`/api/supplier/${supplierId}`)
    const data = await response.json()
    if (data.code === 200) {
      supplier.value = data.data
    }
  } catch {
    supplier.value = { supplierCode: 'SUP001', supplierName: '示例供应商' }
  }
}

const loadInquiries = async () => {
  loading.value = true
  try {
    const response = await fetch(`/api/v1/supplier-portal/inquiries/supplier/${supplierId}`)
    const data = await response.json()
    if (data.code === 200) {
      inquiries.value = data.data || []
    }
  } catch {
    inquiries.value = [
      { id: 1, inquiryNo: 'INQ202401001', inquiryTitle: '办公用品采购询价', inquiryStatus: 1, createTime: '2024-01-15', deadline: '2024-01-20', quotationAmount: 15000, quotationStatus: 1, quotationTime: '2024-01-18', remark: '' },
      { id: 2, inquiryNo: 'INQ202401002', inquiryTitle: '电子设备采购询价', inquiryStatus: 0, createTime: '2024-01-20', deadline: '2024-01-25', quotationAmount: 0, quotationStatus: 0, quotationTime: '', remark: '紧急采购' }
    ]
  } finally {
    loading.value = false
  }
}

const handleCreateInquiry = () => {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 7)
  createForm.value.deadline = tomorrow.toISOString().split('T')[0]
  showCreateModal.value = true
}

const submitInquiry = async () => {
  try {
    const response = await fetch(`/api/v1/supplier-portal/inquiries`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        supplierId: supplierId,
        ...createForm.value
      })
    })
    const data = await response.json()
    if (data.code === 200) {
      alert('询价单创建成功')
      showCreateModal.value = false
      loadInquiries()
    }
  } catch {
    alert('创建失败')
  }
}

const handleViewQuotation = (inquiry: InquiryRecord) => {
  if (inquiry.quotationStatus === 1) {
    alert(`报价金额: ¥${inquiry.quotationAmount}`)
  }
}

const handleAcceptQuotation = async (inquiry: InquiryRecord) => {
  if (confirm(`确定接受报价 ¥${inquiry.quotationAmount}？`)) {
    try {
      const response = await fetch(`/api/v1/supplier-portal/inquiries/${inquiry.id}/accept`, { method: 'POST' })
      const data = await response.json()
      if (data.code === 200) {
        alert('报价已接受')
        loadInquiries()
      }
    } catch {
      alert('操作失败')
    }
  }
}

const handleRejectQuotation = async (inquiry: InquiryRecord) => {
  const reason = prompt('请输入拒绝原因')
  if (reason) {
    try {
      const response = await fetch(`/api/v1/supplier-portal/inquiries/${inquiry.id}/reject?reason=${encodeURIComponent(reason)}`, { method: 'POST' })
      const data = await response.json()
      if (data.code === 200) {
        alert('报价已拒绝')
        loadInquiries()
      }
    } catch {
      alert('操作失败')
    }
  }
}

const handleBack = () => {
  router.push(`/supplier/detail/${supplierId}`)
}

const getStatusLabel = (status: number) => inquiryStatusOptions.find(s => s.value === status)?.label || '未知'
const getStatusColor = (status: number) => inquiryStatusOptions.find(s => s.value === status)?.color || '#969799'
const formatAmount = (amount: number) => amount ? `¥${amount.toLocaleString()}` : '-'
</script>

<template>
  <div class="inquiry-page">
    <div class="page-header">
      <button class="back-btn" @click="handleBack">← 返回</button>
      <h1>供应商询价报价</h1>
      <button class="create-btn" @click="handleCreateInquiry">+ 发起询价</button>
    </div>

    <div class="supplier-info" v-if="supplier">
      <span class="supplier-name">{{ supplier.supplierName }}</span>
      <span class="supplier-code">{{ supplier.supplierCode }}</span>
    </div>

    <div class="stats-row">
      <div class="stat-item">
        <span class="stat-value">{{ inquiries.length }}</span>
        <span class="stat-label">询价总数</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ inquiries.filter(i => i.inquiryStatus === 0).length }}</span>
        <span class="stat-label">待报价</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ inquiries.filter(i => i.inquiryStatus === 1).length }}</span>
        <span class="stat-label">已报价</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ inquiries.filter(i => i.inquiryStatus === 2).length }}</span>
        <span class="stat-label">已接受</span>
      </div>
    </div>

    <div class="inquiry-table" v-if="!loading">
      <table>
        <thead>
          <tr>
            <th>询价单号</th>
            <th>询价标题</th>
            <th>状态</th>
            <th>报价金额</th>
            <th>报价状态</th>
            <th>截止日期</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="inquiry in inquiries" :key="inquiry.id">
            <td>{{ inquiry.inquiryNo }}</td>
            <td>{{ inquiry.inquiryTitle }}</td>
            <td>
              <span class="status-badge" :style="{ color: getStatusColor(inquiry.inquiryStatus) }">
                {{ getStatusLabel(inquiry.inquiryStatus) }}
              </span>
            </td>
            <td>{{ formatAmount(inquiry.quotationAmount) }}</td>
            <td>
              <span v-if="inquiry.quotationStatus === 1" class="quoted">已报价</span>
              <span v-else class="pending">待报价</span>
            </td>
            <td>{{ inquiry.deadline }}</td>
            <td>{{ inquiry.createTime }}</td>
            <td class="actions">
              <button 
                v-if="inquiry.quotationStatus === 1"
                class="action-btn view"
                @click="handleViewQuotation(inquiry)"
              >查看报价</button>
              <button 
                v-if="inquiry.quotationStatus === 1 && inquiry.inquiryStatus === 1"
                class="action-btn accept"
                @click="handleAcceptQuotation(inquiry)"
              >接受</button>
              <button 
                v-if="inquiry.quotationStatus === 1 && inquiry.inquiryStatus === 1"
                class="action-btn reject"
                @click="handleRejectQuotation(inquiry)"
              >拒绝</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="inquiries.length === 0" class="empty-state">
        <p>暂无询价记录</p>
      </div>
    </div>

    <div class="loading-state" v-if="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div class="modal" v-if="showCreateModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>发起询价</h3>
          <button class="close-btn" @click="showCreateModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-item">
            <label>询价标题</label>
            <input v-model="createForm.inquiryTitle" type="text" placeholder="请输入询价标题" />
          </div>
          <div class="form-item">
            <label>报价截止日期</label>
            <input v-model="createForm.deadline" type="date" />
          </div>
          <div class="form-item">
            <label>备注</label>
            <textarea v-model="createForm.remark" rows="3" placeholder="请输入备注"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showCreateModal = false">取消</button>
          <button class="submit-btn" @click="submitInquiry">提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.inquiry-page {
  padding: 20px;
  background: #f5f7fa;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;

  .back-btn {
    padding: 8px 15px;
    background: #fff;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: pointer;
  }

  h1 {
    flex: 1;
    font-size: 20px;
    font-weight: 600;
    color: #333;
  }

  .create-btn {
    padding: 10px 20px;
    background: #1988fa;
    color: #fff;
    border: none;
    border-radius: 4px;
    cursor: pointer;
  }
}

.supplier-info {
  background: #fff;
  padding: 15px 20px;
  border-radius: 8px;
  margin-bottom: 20px;

  .supplier-name {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }

  .supplier-code {
    font-size: 14px;
    color: #969799;
    margin-left: 15px;
  }
}

.stats-row {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;

  .stat-item {
    flex: 1;
    background: #fff;
    border-radius: 8px;
    padding: 15px;
    text-align: center;

    .stat-value {
      font-size: 24px;
      font-weight: 600;
      color: #1988fa;
    }

    .stat-label {
      font-size: 12px;
      color: #969799;
    }
  }
}

.inquiry-table {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;

  table {
    width: 100%;
    border-collapse: collapse;

    th, td {
      padding: 12px 15px;
      text-align: left;
      border-bottom: 1px solid #ebedf0;
    }

    th {
      background: #f7f8fa;
      font-weight: 600;
      color: #333;
    }

    td {
      color: #666;

      .status-badge {
        font-weight: 600;
      }

      .quoted { color: #07c160; }
      .pending { color: #1988fa; }

      .actions {
        display: flex;
        gap: 8px;

        .action-btn {
          padding: 6px 12px;
          border: none;
          border-radius: 4px;
          font-size: 12px;
          cursor: pointer;

          &.view { background: #1988fa; color: #fff; }
          &.accept { background: #07c160; color: #fff; }
          &.reject { background: #f44; color: #fff; }
        }
      }
    }
  }

  .empty-state {
    padding: 40px;
    text-align: center;
    color: #969799;
  }
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;

  .spinner {
    width: 32px;
    height: 32px;
    border: 3px solid #ebedf0;
    border-top-color: #1988fa;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  p { margin-top: 10px; color: #969799; }
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;

  .modal-content {
    background: #fff;
    border-radius: 8px;
    width: 500px;

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 15px 20px;
      border-bottom: 1px solid #ebedf0;

      h3 { font-size: 16px; color: #333; }
      .close-btn {
        background: none;
        border: none;
        font-size: 20px;
        cursor: pointer;
      }
    }

    .modal-body {
      padding: 20px;

      .form-item {
        margin-bottom: 15px;

        label {
          display: block;
          font-size: 12px;
          color: #666;
          margin-bottom: 8px;
        }

        input, textarea {
          width: 100%;
          padding: 10px;
          border: 1px solid #dcdfe6;
          border-radius: 4px;
        }
      }
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      padding: 15px 20px;
      border-top: 1px solid #ebedf0;

      .cancel-btn, .submit-btn {
        padding: 10px 20px;
        border-radius: 4px;
        cursor: pointer;
      }

      .cancel-btn {
        background: #f7f8fa;
        color: #333;
        border: 1px solid #dcdfe6;
      }

      .submit-btn {
        background: #1988fa;
        color: #fff;
        border: none;
      }
    }
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>