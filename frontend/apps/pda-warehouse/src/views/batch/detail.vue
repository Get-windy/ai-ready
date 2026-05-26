<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

interface BatchDetail {
  id: string
  batchNo: string
  productId: string
  productName: string
  productCode: string
  productSpec: string
  productCategory: string
  quantity: number
  unit: string
  productionDate: string
  expiryDate: string
  shelfLifeDays: number
  supplierId: string
  supplierName: string
  supplierContact: string
  supplierPhone: string
  status: 'normal' | 'warning' | 'expired' | 'locked'
  locationCode: string
  warehouseName: string
  qualityStatus: 'qualified' | 'pending' | 'rejected'
  qualityCheckDate: string
  qualityChecker: string
  qualityRemark: string
  inboundDate: string
  inboundOrderNo: string
  inboundOperator: string
  remark: string
  traceabilityLogs: TraceabilityLog[]
}

interface TraceabilityLog {
  id: string
  actionType: 'inbound' | 'outbound' | 'transfer' | 'check' | 'lock' | 'unlock'
  actionTime: string
  operator: string
  fromLocation?: string
  toLocation?: string
  quantity?: number
  remark: string
}

const batch = ref<BatchDetail | null>(null)
const loading = ref(false)
const activeTab = ref('info')

const statusMap = {
  normal: { label: '正常', color: '#07c160', bg: '#e8f7e8' },
  warning: { label: '临期', color: '#ff976a', bg: '#fff3e0' },
  expired: { label: '过期', color: '#f44', bg: '#ffe8e8' },
  locked: { label: '锁定', color: '#969799', bg: '#f7f8fa' }
}

const qualityMap = {
  qualified: { label: '合格', color: '#07c160', icon: '✓' },
  pending: { label: '待检', color: '#1988fa', icon: '?' },
  rejected: { label: '不合格', color: '#f44', icon: '✗' }
}

const actionTypeMap = {
  inbound: { label: '入库', color: '#07c160' },
  outbound: { label: '出库', color: '#1988fa' },
  transfer: { label: '转移', color: '#ff976a' },
  check: { label: '质检', color: '#1988fa' },
  lock: { label: '锁定', color: '#969799' },
  unlock: { label: '解锁', color: '#07c160' }
}

const daysUntilExpiry = computed(() => {
  if (!batch.value) return 0
  const today = new Date()
  const expiry = new Date(batch.value.expiryDate)
  return Math.ceil((expiry.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
})

const expiryPercent = computed(() => {
  if (!batch.value) return 0
  const totalDays = batch.value.shelfLifeDays
  const remaining = daysUntilExpiry.value
  return Math.max(0, Math.min(100, (remaining / totalDays) * 100))
})

onMounted(async () => {
  const batchId = route.params.id as string
  await loadBatchDetail(batchId)
})

const loadBatchDetail = async (batchId: string) => {
  loading.value = true
  try {
    const data = await window.electronAPI?.batch?.getBatchDetail?.(batchId)
    batch.value = data || getMockBatchDetail(batchId)
  } finally {
    loading.value = false
  }
}

const getMockBatchDetail = (batchId: string): BatchDetail => ({
  id: batchId,
  batchNo: 'B20240115001',
  productId: 'P001',
  productName: '优质大米',
  productCode: 'SP001',
  productSpec: '5kg/袋',
  productCategory: '粮油米面',
  quantity: 500,
  unit: '袋',
  productionDate: '2024-01-15',
  expiryDate: '2025-01-15',
  shelfLifeDays: 365,
  supplierId: 'S001',
  supplierName: '东北粮仓',
  supplierContact: '张经理',
  supplierPhone: '13800138000',
  status: 'normal',
  locationCode: 'A-01-01',
  warehouseName: '主仓库',
  qualityStatus: 'qualified',
  qualityCheckDate: '2024-01-16',
  qualityChecker: '质检员李',
  qualityRemark: '外观正常，无异味',
  inboundDate: '2024-01-15',
  inboundOrderNo: 'PO20240115001',
  inboundOperator: '收货员王',
  remark: '',
  traceabilityLogs: [
    {
      id: '1',
      actionType: 'inbound',
      actionTime: '2024-01-15 10:30',
      operator: '收货员王',
      toLocation: 'A-01-01',
      quantity: 500,
      remark: '采购入库'
    },
    {
      id: '2',
      actionType: 'check',
      actionTime: '2024-01-16 09:00',
      operator: '质检员李',
      remark: '质量检测合格'
    }
  ]
})

const handleQualityCheck = () => {
  router.push(`/batch/${route.params.id}/check`)
}

const handleLock = async () => {
  if (confirm('确定锁定该批次？锁定后无法出库。')) {
    batch.value!.status = 'locked'
    alert('批次已锁定')
  }
}

const handleUnlock = async () => {
  if (confirm('确定解锁该批次？')) {
    batch.value!.status = 'normal'
    alert('批次已解锁')
  }
}

const handleBack = () => {
  router.push('/batch')
}

const formatDate = (date: string): string => {
  return date.replace(/-/g, '/')
}
</script>

<template>
  <div class="batch-detail-page">
    <div class="page-header">
      <button class="back-btn" @click="handleBack">
        <svg viewBox="0 0 24 24" width="20" height="20">
          <path fill="currentColor" d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
        </svg>
        返回
      </button>
      <h1>批次详情</h1>
      <div class="header-actions">
        <button 
          v-if="batch && batch.status !== 'locked'"
          class="action-btn lock"
          @click="handleLock"
        >
          锁定
        </button>
        <button 
          v-if="batch && batch.status === 'locked'"
          class="action-btn unlock"
          @click="handleUnlock"
        >
          解锁
        </button>
      </div>
    </div>
    
    <div class="loading-state" v-if="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    
    <div class="detail-content" v-if="batch && !loading">
      <div class="status-banner" :style="{ background: statusMap[batch.status].bg }">
        <div class="batch-info">
          <span class="batch-no">{{ batch.batchNo }}</span>
          <span class="batch-status" :style="{ color: statusMap[batch.status].color }">
            {{ statusMap[batch.status].label }}
          </span>
        </div>
        <div class="expiry-info">
          <div class="expiry-progress">
            <div class="progress-bar">
              <div 
                class="progress-fill"
                :style="{ 
                  width: expiryPercent + '%',
                  background: daysUntilExpiry > 30 ? '#07c160' : daysUntilExpiry > 0 ? '#ff976a' : '#f44'
                }"
              ></div>
            </div>
            <span class="progress-text">
              剩余 {{ daysUntilExpiry }} 天
            </span>
          </div>
        </div>
      </div>
      
      <div class="tabs">
        <button 
          :class="['tab', { active: activeTab === 'info' }]"
          @click="activeTab = 'info'"
        >
          基本信息
        </button>
        <button 
          :class="['tab', { active: activeTab === 'product' }]"
          @click="activeTab = 'product'"
        >
          商品信息
        </button>
        <button 
          :class="['tab', { active: activeTab === 'quality' }]"
          @click="activeTab = 'quality'"
        >
          质量状态
        </button>
        <button 
          :class="['tab', { active: activeTab === 'trace' }]"
          @click="activeTab = 'trace'"
        >
          追溯记录
        </button>
      </div>
      
      <div class="tab-content">
        <div v-show="activeTab === 'info'" class="info-section">
          <div class="info-card">
            <h3>批次基本信息</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">批次号</span>
                <span class="value">{{ batch.batchNo }}</span>
              </div>
              <div class="info-item">
                <span class="label">数量</span>
                <span class="value">{{ batch.quantity }} {{ batch.unit }}</span>
              </div>
              <div class="info-item">
                <span class="label">库位</span>
                <span class="value">{{ batch.locationCode }}</span>
              </div>
              <div class="info-item">
                <span class="label">仓库</span>
                <span class="value">{{ batch.warehouseName }}</span>
              </div>
              <div class="info-item">
                <span class="label">生产日期</span>
                <span class="value">{{ formatDate(batch.productionDate) }}</span>
              </div>
              <div class="info-item">
                <span class="label">有效期至</span>
                <span class="value expiry" :class="{ warning: daysUntilExpiry <= 30 && daysUntilExpiry > 0, expired: daysUntilExpiry <= 0 }">
                  {{ formatDate(batch.expiryDate) }}
                </span>
              </div>
              <div class="info-item">
                <span class="label">保质期</span>
                <span class="value">{{ batch.shelfLifeDays }} 天</span>
              </div>
              <div class="info-item">
                <span class="label">入库日期</span>
                <span class="value">{{ formatDate(batch.inboundDate) }}</span>
              </div>
              <div class="info-item">
                <span class="label">入库单号</span>
                <span class="value">{{ batch.inboundOrderNo }}</span>
              </div>
              <div class="info-item">
                <span class="label">入库操作员</span>
                <span class="value">{{ batch.inboundOperator }}</span>
              </div>
            </div>
          </div>
          
          <div class="info-card">
            <h3>供应商信息</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">供应商</span>
                <span class="value">{{ batch.supplierName }}</span>
              </div>
              <div class="info-item">
                <span class="label">联系人</span>
                <span class="value">{{ batch.supplierContact }}</span>
              </div>
              <div class="info-item">
                <span class="label">联系电话</span>
                <span class="value">{{ batch.supplierPhone }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <div v-show="activeTab === 'product'" class="info-section">
          <div class="info-card">
            <h3>商品信息</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">商品名称</span>
                <span class="value highlight">{{ batch.productName }}</span>
              </div>
              <div class="info-item">
                <span class="label">商品编码</span>
                <span class="value">{{ batch.productCode }}</span>
              </div>
              <div class="info-item">
                <span class="label">规格</span>
                <span class="value">{{ batch.productSpec }}</span>
              </div>
              <div class="info-item">
                <span class="label">分类</span>
                <span class="value">{{ batch.productCategory }}</span>
              </div>
              <div class="info-item">
                <span class="label">单位</span>
                <span class="value">{{ batch.unit }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <div v-show="activeTab === 'quality'" class="info-section">
          <div class="quality-card">
            <div class="quality-header">
              <div class="quality-icon" :style="{ background: qualityMap[batch.qualityStatus].bg }">
                <span :style="{ color: qualityMap[batch.qualityStatus].color }">
                  {{ qualityMap[batch.qualityStatus].icon }}
                </span>
              </div>
              <div class="quality-status">
                <span class="status-label" :style="{ color: qualityMap[batch.qualityStatus].color }">
                  {{ qualityMap[batch.qualityStatus].label }}
                </span>
                <span class="check-date">检测日期: {{ formatDate(batch.qualityCheckDate) }}</span>
              </div>
            </div>
            
            <div class="quality-details">
              <div class="detail-item">
                <span class="label">检测人员</span>
                <span class="value">{{ batch.qualityChecker }}</span>
              </div>
              <div class="detail-item">
                <span class="label">检测结果</span>
                <span class="value">{{ batch.qualityRemark }}</span>
              </div>
            </div>
            
            <button 
              v-if="batch.qualityStatus === 'pending'"
              class="check-btn"
              @click="handleQualityCheck"
            >
              进行质检
            </button>
          </div>
        </div>
        
        <div v-show="activeTab === 'trace'" class="info-section">
          <div class="trace-list">
            <div 
              v-for="log in batch.traceabilityLogs"
              :key="log.id"
              class="trace-item"
            >
              <div class="trace-icon" :style="{ background: actionTypeMap[log.actionType].bg }">
                <svg viewBox="0 0 24 24" width="16" height="16">
                  <path fill="currentColor" :style="{ color: actionTypeMap[log.actionType].color }" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                </svg>
              </div>
              <div class="trace-content">
                <div class="trace-header">
                  <span class="action-type" :style="{ color: actionTypeMap[log.actionType].color }">
                    {{ actionTypeMap[log.actionType].label }}
                  </span>
                  <span class="action-time">{{ log.actionTime }}</span>
                </div>
                <div class="trace-details">
                  <span class="operator">操作人: {{ log.operator }}</span>
                  <span v-if="log.quantity" class="quantity">数量: {{ log.quantity }}</span>
                  <span v-if="log.toLocation" class="location">位置: {{ log.toLocation }}</span>
                </div>
                <div v-if="log.remark" class="trace-remark">{{ log.remark }}</div>
              </div>
            </div>
            
            <div v-if="batch.traceabilityLogs.length === 0" class="empty-trace">
              <p>暂无追溯记录</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.batch-detail-page {
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
    flex: 1;
    text-align: center;
  }
  
  .back-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 8px 12px;
    background: none;
    color: #333;
    border: none;
    cursor: pointer;
  }
  
  .header-actions {
    display: flex;
    gap: 8px;
    
    .action-btn {
      padding: 8px 16px;
      border: none;
      border-radius: 4px;
      font-size: 12px;
      cursor: pointer;
      
      &.lock {
        background: #f7f8fa;
        color: #333;
        border: 1px solid #dcdfe6;
      }
      
      &.unlock {
        background: #07c160;
        color: #fff;
      }
    }
  }
}

.status-banner {
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  
  .batch-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    
    .batch-no {
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
    
    .batch-status {
      font-size: 14px;
      font-weight: 600;
    }
  }
  
  .expiry-info {
    .expiry-progress {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .progress-bar {
        flex: 1;
        height: 8px;
        background: #ebedf0;
        border-radius: 4px;
        overflow: hidden;
        
        .progress-fill {
          height: 100%;
          border-radius: 4px;
          transition: width 0.3s;
        }
      }
      
      .progress-text {
        font-size: 12px;
        color: #666;
      }
    }
  }
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  background: #fff;
  padding: 8px;
  border-radius: 8px;
  
  .tab {
    flex: 1;
    padding: 10px;
    background: none;
    border: none;
    font-size: 14px;
    color: #666;
    cursor: pointer;
    border-radius: 4px;
    
    &.active {
      background: #1988fa;
      color: #fff;
    }
  }
}

.tab-content {
  .info-section {
    .info-card {
      background: #fff;
      border-radius: 8px;
      padding: 16px;
      margin-bottom: 12px;
      
      h3 {
        font-size: 14px;
        font-weight: 600;
        color: #333;
        margin-bottom: 12px;
        padding-bottom: 8px;
        border-bottom: 1px solid #ebedf0;
      }
      
      .info-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 12px;
        
        .info-item {
          display: flex;
          flex-direction: column;
          gap: 4px;
          
          .label {
            font-size: 12px;
            color: #969799;
          }
          
          .value {
            font-size: 14px;
            color: #333;
            
            &.highlight {
              font-weight: 600;
            }
            
            &.expiry {
              &.warning { color: #ff976a; }
              &.expired { color: #f44; }
            }
          }
        }
      }
    }
    
    .quality-card {
      background: #fff;
      border-radius: 8px;
      padding: 16px;
      
      .quality-header {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 16px;
        
        .quality-icon {
          width: 48px;
          height: 48px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          
          span {
            font-size: 24px;
            font-weight: 600;
          }
        }
        
        .quality-status {
          .status-label {
            font-size: 18px;
            font-weight: 600;
          }
          
          .check-date {
            font-size: 12px;
            color: #969799;
            margin-top: 4px;
          }
        }
      }
      
      .quality-details {
        margin-bottom: 16px;
        
        .detail-item {
          display: flex;
          margin-bottom: 8px;
          
          .label {
            width: 80px;
            font-size: 12px;
            color: #969799;
          }
          
          .value {
            font-size: 14px;
            color: #333;
          }
        }
      }
      
      .check-btn {
        width: 100%;
        padding: 12px;
        background: #1988fa;
        color: #fff;
        border: none;
        border-radius: 4px;
        font-size: 14px;
        cursor: pointer;
      }
    }
    
    .trace-list {
      .trace-item {
        display: flex;
        gap: 12px;
        padding: 12px;
        background: #fff;
        border-radius: 8px;
        margin-bottom: 8px;
        
        .trace-icon {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        
        .trace-content {
          flex: 1;
          
          .trace-header {
            display: flex;
            justify-content: space-between;
            margin-bottom: 4px;
            
            .action-type {
              font-size: 14px;
              font-weight: 600;
            }
            
            .action-time {
              font-size: 12px;
              color: #969799;
            }
          }
          
          .trace-details {
            display: flex;
            gap: 12px;
            font-size: 12px;
            color: #666;
            margin-bottom: 4px;
          }
          
          .trace-remark {
            font-size: 12px;
            color: #969799;
          }
        }
      }
      
      .empty-trace {
        padding: 40px;
        text-align: center;
        color: #969799;
      }
    }
  }
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #969799;
  
  .loading-spinner {
    width: 32px;
    height: 32px;
    border: 3px solid #ebedf0;
    border-top-color: #1988fa;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }
  
  p {
    margin-top: 12px;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>