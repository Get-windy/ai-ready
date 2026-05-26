<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

interface BatchInfo {
  id: string
  batchNo: string
  productId: string
  productName: string
  productCode: string
  quantity: number
  productionDate: string
  expiryDate: string
  supplierId: string
  supplierName: string
  status: 'normal' | 'warning' | 'expired' | 'locked'
  locationCode: string
  qualityStatus: 'qualified' | 'pending' | 'rejected'
  remark: string
}

const batches = ref<BatchInfo[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const filterStatus = ref('')
const scanMode = ref(false)
const scannedBatchNo = ref('')

const statusMap = {
  normal: { label: '正常', color: '#07c160' },
  warning: { label: '临期', color: '#ff976a' },
  expired: { label: '过期', color: '#f44' },
  locked: { label: '锁定', color: '#969799' }
}

const qualityMap = {
  qualified: { label: '合格', color: '#07c160' },
  pending: { label: '待检', color: '#1988fa' },
  rejected: { label: '不合格', color: '#f44' }
}

onMounted(async () => {
  loadBatches()
})

const loadBatches = async () => {
  loading.value = true
  try {
    const response = await fetch('/api/v1/erp-batch-sn/batches', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        keyword: searchKeyword.value,
        status: filterStatus.value
      })
    })
    const data = await response.json()
    if (data.code === 200) {
      batches.value = data.data || []
    } else {
      batches.value = []
    }
  } catch (error) {
    batches.value = []
  } finally {
    loading.value = false
  }
}

const getMockBatches = (): BatchInfo[] => [
  {
    id: '1',
    batchNo: 'B20240115001',
    productId: 'P001',
    productName: '优质大米',
    productCode: 'SP001',
    quantity: 500,
    productionDate: '2024-01-15',
    expiryDate: '2025-01-15',
    supplierId: 'S001',
    supplierName: '东北粮仓',
    status: 'normal',
    locationCode: 'A-01-01',
    qualityStatus: 'qualified',
    remark: ''
  },
  {
    id: '2',
    batchNo: 'B20240110002',
    productId: 'P002',
    productName: '食用油',
    productCode: 'SP002',
    quantity: 200,
    productionDate: '2024-01-10',
    expiryDate: '2024-06-10',
    supplierId: 'S002',
    supplierName: '金龙鱼',
    status: 'warning',
    locationCode: 'A-02-03',
    qualityStatus: 'qualified',
    remark: '临期预警'
  },
  {
    id: '3',
    batchNo: 'B20231215003',
    productId: 'P003',
    productName: '调味品',
    productCode: 'SP003',
    quantity: 100,
    productionDate: '2023-12-15',
    expiryDate: '2024-03-15',
    supplierId: 'S003',
    supplierName: '海天味业',
    status: 'expired',
    locationCode: 'B-01-02',
    qualityStatus: 'rejected',
    remark: '已过期需处理'
  }
]

const handleScan = () => {
  scanMode.value = true
}

const handleScanComplete = (result: string) => {
  scannedBatchNo.value = result
  searchKeyword.value = result
  loadBatches()
  scanMode.value = false
}

const handleDetail = (batch: BatchInfo) => {
  router.push(`/batch/${batch.id}`)
}

const handleQualityCheck = (batch: BatchInfo) => {
  router.push(`/batch/${batch.id}/check`)
}

const handleLock = async (batch: BatchInfo) => {
  if (confirm(`确定锁定批次 ${batch.batchNo}？`)) {
    batch.status = 'locked'
    alert('批次已锁定')
  }
}

const handleUnlock = async (batch: BatchInfo) => {
  if (confirm(`确定解锁批次 ${batch.batchNo}？`)) {
    batch.status = 'normal'
    alert('批次已解锁')
  }
}

const handleDispose = async (batch: BatchInfo) => {
  if (confirm(`确定处理过期批次 ${batch.batchNo}？此操作不可撤销。`)) {
    batches.value = batches.value.filter(b => b.id !== batch.id)
    alert('批次已处理')
  }
}

const getDaysUntilExpiry = (expiryDate: string): number => {
  const today = new Date()
  const expiry = new Date(expiryDate)
  return Math.ceil((expiry.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
}

const formatDate = (date: string): string => {
  return date.replace(/-/g, '/')
}
</script>

<template>
  <div class="batch-page">
    <div class="page-header">
      <h1>批次管理</h1>
      <button class="scan-btn" @click="handleScan">
        <svg viewBox="0 0 24 24" width="20" height="20">
          <path fill="currentColor" d="M3 11h8V3H3v8zm2-6h4v4H5V5zm8-2v8h8V3h-8zm6 6h-4V5h4v4zM3 21h8v-8H3v8zm2-6h4v4H5v-4zm13-2h-2v3h-3v2h3v3h2v-3h3v-2h-3v-3z"/>
        </svg>
        扫码查询
      </button>
    </div>
    
    <div class="search-bar">
      <input 
        v-model="searchKeyword"
        type="text"
        placeholder="输入批次号/商品名称搜索"
        class="search-input"
        @keyup.enter="loadBatches"
      />
      <select v-model="filterStatus" class="status-filter" @change="loadBatches">
        <option value="">全部状态</option>
        <option value="normal">正常</option>
        <option value="warning">临期</option>
        <option value="expired">过期</option>
        <option value="locked">锁定</option>
      </select>
      <button class="search-btn" @click="loadBatches">搜索</button>
    </div>
    
    <div class="batch-stats">
      <div class="stat-item normal">
        <span class="stat-value">{{ batches.filter(b => b.status === 'normal').length }}</span>
        <span class="stat-label">正常</span>
      </div>
      <div class="stat-item warning">
        <span class="stat-value">{{ batches.filter(b => b.status === 'warning').length }}</span>
        <span class="stat-label">临期</span>
      </div>
      <div class="stat-item expired">
        <span class="stat-value">{{ batches.filter(b => b.status === 'expired').length }}</span>
        <span class="stat-label">过期</span>
      </div>
      <div class="stat-item locked">
        <span class="stat-value">{{ batches.filter(b => b.status === 'locked').length }}</span>
        <span class="stat-label">锁定</span>
      </div>
    </div>
    
    <div class="batch-list" v-if="!loading">
      <div 
        v-for="batch in batches"
        :key="batch.id"
        class="batch-card"
        :class="batch.status"
      >
        <div class="batch-header">
          <span class="batch-no">{{ batch.batchNo }}</span>
          <span 
            class="batch-status"
            :style="{ color: statusMap[batch.status].color }"
          >
            {{ statusMap[batch.status].label }}
          </span>
        </div>
        
        <div class="batch-body">
          <div class="product-info">
            <span class="product-name">{{ batch.productName }}</span>
            <span class="product-code">{{ batch.productCode }}</span>
          </div>
          
          <div class="batch-details">
            <div class="detail-row">
              <span class="label">数量:</span>
              <span class="value">{{ batch.quantity }}</span>
            </div>
            <div class="detail-row">
              <span class="label">库位:</span>
              <span class="value">{{ batch.locationCode }}</span>
            </div>
            <div class="detail-row">
              <span class="label">生产日期:</span>
              <span class="value">{{ formatDate(batch.productionDate) }}</span>
            </div>
            <div class="detail-row expiry">
              <span class="label">有效期至:</span>
              <span class="value" :class="{ warning: batch.status === 'warning', expired: batch.status === 'expired' }">
                {{ formatDate(batch.expiryDate) }}
                <span v-if="batch.status === 'warning'" class="days-left">
                  (剩余{{ getDaysUntilExpiry(batch.expiryDate) }}天)
                </span>
              </span>
            </div>
            <div class="detail-row">
              <span class="label">供应商:</span>
              <span class="value">{{ batch.supplierName }}</span>
            </div>
            <div class="detail-row">
              <span class="label">质量状态:</span>
              <span 
                class="value quality"
                :style="{ color: qualityMap[batch.qualityStatus].color }"
              >
                {{ qualityMap[batch.qualityStatus].label }}
              </span>
            </div>
          </div>
        </div>
        
        <div class="batch-actions">
          <button class="action-btn detail" @click="handleDetail(batch)">
            详情
          </button>
          <button 
            v-if="batch.qualityStatus === 'pending'"
            class="action-btn check"
            @click="handleQualityCheck(batch)"
          >
            质检
          </button>
          <button 
            v-if="batch.status !== 'locked'"
            class="action-btn lock"
            @click="handleLock(batch)"
          >
            锁定
          </button>
          <button 
            v-if="batch.status === 'locked'"
            class="action-btn unlock"
            @click="handleUnlock(batch)"
          >
            解锁
          </button>
          <button 
            v-if="batch.status === 'expired'"
            class="action-btn dispose"
            @click="handleDispose(batch)"
          >
            处理
          </button>
        </div>
      </div>
      
      <div v-if="batches.length === 0" class="empty-state">
        <svg viewBox="0 0 24 24" width="48" height="48">
          <path fill="#969799" d="M19 5v14H5V5h14m0-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z"/>
        </svg>
        <p>暂无批次数据</p>
      </div>
    </div>
    
    <div class="loading-state" v-if="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="scanMode" class="scan-overlay">
          <div class="scan-container">
            <div class="scan-header">
              <span>扫描批次条码</span>
              <button @click="scanMode = false">×</button>
            </div>
            <div class="scan-area">
              <video 
                ref="scanVideo"
                class="scan-video"
                autoplay
                playsinline
              ></video>
              <div class="scan-frame">
                <div class="corner tl"></div>
                <div class="corner tr"></div>
                <div class="corner bl"></div>
                <div class="corner br"></div>
              </div>
            </div>
            <div class="scan-tip">将条码放入框内自动扫描</div>
            <div class="manual-input">
              <input 
                v-model="scannedBatchNo"
                type="text"
                placeholder="手动输入批次号"
                @keyup.enter="handleScanComplete(scannedBatchNo)"
              />
              <button @click="handleScanComplete(scannedBatchNo)">确定</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style lang="scss" scoped>
.batch-page {
  padding: 16px;
  background: #f7f8fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  
  h1 {
    font-size: 18px;
    font-weight: 600;
    color: #333;
  }
  
  .scan-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 8px 16px;
    background: #1988fa;
    color: #fff;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    
    &:active {
      background: #0e7cd3;
    }
  }
}

.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  
  .search-input {
    flex: 1;
    padding: 10px 12px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    font-size: 14px;
    
    &:focus {
      border-color: #1988fa;
    }
  }
  
  .status-filter {
    padding: 10px 12px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    background: #fff;
  }
  
  .search-btn {
    padding: 10px 20px;
    background: #1988fa;
    color: #fff;
    border: none;
    border-radius: 4px;
    cursor: pointer;
  }
}

.batch-stats {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  
  .stat-item {
    flex: 1;
    padding: 12px;
    background: #fff;
    border-radius: 8px;
    text-align: center;
    
    .stat-value {
      font-size: 24px;
      font-weight: 600;
    }
    
    .stat-label {
      font-size: 12px;
      color: #969799;
    }
    
    &.normal .stat-value { color: #07c160; }
    &.warning .stat-value { color: #ff976a; }
    &.expired .stat-value { color: #f44; }
    &.locked .stat-value { color: #969799; }
  }
}

.batch-list {
  .batch-card {
    background: #fff;
    border-radius: 8px;
    margin-bottom: 12px;
    overflow: hidden;
    
    &.warning { border-left: 4px solid #ff976a; }
    &.expired { border-left: 4px solid #f44; }
    &.locked { border-left: 4px solid #969799; opacity: 0.7; }
    
    .batch-header {
      display: flex;
      justify-content: space-between;
      padding: 12px 16px;
      background: #f7f8fa;
      
      .batch-no {
        font-size: 14px;
        font-weight: 600;
        color: #333;
      }
      
      .batch-status {
        font-size: 12px;
        font-weight: 600;
      }
    }
    
    .batch-body {
      padding: 16px;
      
      .product-info {
        margin-bottom: 12px;
        
        .product-name {
          font-size: 16px;
          font-weight: 600;
          color: #333;
        }
        
        .product-code {
          font-size: 12px;
          color: #969799;
          margin-left: 8px;
        }
      }
      
      .batch-details {
        .detail-row {
          display: flex;
          margin-bottom: 8px;
          
          .label {
            width: 80px;
            font-size: 12px;
            color: #969799;
          }
          
          .value {
            font-size: 12px;
            color: #333;
            
            &.warning { color: #ff976a; }
            &.expired { color: #f44; }
            
            .days-left {
              font-size: 10px;
              color: #ff976a;
            }
            
            &.quality {
              font-weight: 600;
            }
          }
        }
      }
    }
    
    .batch-actions {
      display: flex;
      gap: 8px;
      padding: 12px 16px;
      border-top: 1px solid #ebedf0;
      
      .action-btn {
        flex: 1;
        padding: 8px;
        border: none;
        border-radius: 4px;
        font-size: 12px;
        cursor: pointer;
        
        &.detail {
          background: #1988fa;
          color: #fff;
        }
        
        &.check {
          background: #07c160;
          color: #fff;
        }
        
        &.lock {
          background: #f7f8fa;
          color: #333;
          border: 1px solid #dcdfe6;
        }
        
        &.unlock {
          background: #07c160;
          color: #fff;
        }
        
        &.dispose {
          background: #f44;
          color: #fff;
        }
      }
    }
  }
}

.empty-state, .loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #969799;
  
  p {
    margin-top: 12px;
    font-size: 14px;
  }
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #ebedf0;
  border-top-color: #1988fa;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.scan-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #000;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.scan-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  
  .scan-header {
    display: flex;
    justify-content: space-between;
    padding: 16px;
    color: #fff;
    
    span {
      font-size: 16px;
    }
    
    button {
      background: none;
      border: none;
      color: #fff;
      font-size: 24px;
    }
  }
  
  .scan-area {
    flex: 1;
    position: relative;
    
    .scan-video {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .scan-frame {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 200px;
      height: 100px;
      
      .corner {
        position: absolute;
        width: 20px;
        height: 20px;
        border: 3px solid #07c160;
        
        &.tl { top: 0; left: 0; border-right: none; border-bottom: none; }
        &.tr { top: 0; right: 0; border-left: none; border-bottom: none; }
        &.bl { bottom: 0; left: 0; border-right: none; border-top: none; }
        &.br { bottom: 0; right: 0; border-left: none; border-top: none; }
      }
    }
  }
  
  .scan-tip {
    padding: 16px;
    color: #fff;
    text-align: center;
    font-size: 14px;
  }
  
  .manual-input {
    padding: 16px;
    background: #fff;
    display: flex;
    gap: 8px;
    
    input {
      flex: 1;
      padding: 12px;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
    }
    
    button {
      padding: 12px 24px;
      background: #1988fa;
      color: #fff;
      border: none;
      border-radius: 4px;
    }
  }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>