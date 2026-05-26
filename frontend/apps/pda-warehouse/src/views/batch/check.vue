<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

interface BatchInfo {
  id: string
  batchNo: string
  productName: string
  productCode: string
  quantity: number
  unit: string
  productionDate: string
  expiryDate: string
  supplierName: string
  locationCode: string
}

interface CheckItem {
  id: string
  name: string
  standard: string
  result: 'pass' | 'fail' | 'pending'
  value?: string
  remark?: string
}

interface QualityCheckForm {
  checkType: 'visual' | 'sampling' | 'full'
  checkItems: CheckItem[]
  overallResult: 'qualified' | 'rejected' | 'pending'
  remark: string
  checker: string
  checkTime: string
  photos: string[]
}

const batch = ref<BatchInfo | null>(null)
const loading = ref(false)
const submitting = ref(false)
const checkForm = ref<QualityCheckForm>({
  checkType: 'visual',
  checkItems: [],
  overallResult: 'pending',
  remark: '',
  checker: '',
  checkTime: '',
  photos: []
})

const checkTypeOptions = [
  { value: 'visual', label: '外观检查', desc: '检查商品外观、包装完整性' },
  { value: 'sampling', label: '抽样检测', desc: '抽取部分样品进行检测' },
  { value: 'full', label: '全面检测', desc: '对所有商品进行全面检测' }
]

const defaultCheckItems: CheckItem[] = [
  { id: '1', name: '外观检查', standard: '无破损、无变形、无污染', result: 'pending' },
  { id: '2', name: '包装完整性', standard: '包装完好、标签清晰', result: 'pending' },
  { id: '3', name: '颜色/气味', standard: '颜色正常、无异味', result: 'pending' },
  { id: '4', name: '温度检查', standard: '符合存储温度要求', result: 'pending' },
  { id: '5', name: '有效期核对', standard: '有效期符合要求', result: 'pending' }
]

const passCount = computed(() => checkForm.value.checkItems.filter(i => i.result === 'pass').length)
const failCount = computed(() => checkForm.value.checkItems.filter(i => i.result === 'fail').length)

onMounted(async () => {
  const batchId = route.params.id as string
  await loadBatchInfo(batchId)
  initCheckForm()
})

const loadBatchInfo = async (batchId: string) => {
  loading.value = true
  try {
    const data = await window.electronAPI?.batch?.getBatchDetail?.(batchId)
    batch.value = data || getMockBatchInfo(batchId)
  } finally {
    loading.value = false
  }
}

const getMockBatchInfo = (batchId: string): BatchInfo => ({
  id: batchId,
  batchNo: 'B20240115001',
  productName: '优质大米',
  productCode: 'SP001',
  quantity: 500,
  unit: '袋',
  productionDate: '2024-01-15',
  expiryDate: '2025-01-15',
  supplierName: '东北粮仓',
  locationCode: 'A-01-01'
})

const initCheckForm = () => {
  checkForm.value.checkItems = [...defaultCheckItems]
  checkForm.value.checker = '当前操作员'
  checkForm.value.checkTime = new Date().toLocaleString()
}

const handleCheckTypeChange = (type: 'visual' | 'sampling' | 'full') => {
  checkForm.value.checkType = type
  if (type === 'visual') {
    checkForm.value.checkItems = defaultCheckItems.slice(0, 3)
  } else if (type === 'sampling') {
    checkForm.value.checkItems = [...defaultCheckItems]
  } else {
    checkForm.value.checkItems = [...defaultCheckItems, 
      { id: '6', name: '重量核对', standard: '重量符合标称值', result: 'pending' },
      { id: '7', name: '质量抽检', standard: '符合质量标准', result: 'pending' }
    ]
  }
}

const handleItemResultChange = (itemId: string, result: 'pass' | 'fail') => {
  const item = checkForm.value.checkItems.find(i => i.id === itemId)
  if (item) {
    item.result = result
  }
  updateOverallResult()
}

const updateOverallResult = () => {
  if (failCount.value > 0) {
    checkForm.value.overallResult = 'rejected'
  } else if (passCount.value === checkForm.value.checkItems.length) {
    checkForm.value.overallResult = 'qualified'
  } else {
    checkForm.value.overallResult = 'pending'
  }
}

const handleTakePhoto = () => {
  alert('拍照功能需要调用设备摄像头')
}

const handleRemovePhoto = (index: number) => {
  checkForm.value.photos.splice(index, 1)
}

const handleSubmit = async () => {
  if (checkForm.value.overallResult === 'pending') {
    alert('请完成所有检测项目')
    return
  }
  
  submitting.value = true
  try {
    await window.electronAPI?.batch?.submitQualityCheck?.({
      batchId: route.params.id,
      ...checkForm.value
    })
    alert('质检结果已提交')
    router.push(`/batch/${route.params.id}`)
  } finally {
    submitting.value = false
  }
}

const handleBack = () => {
  router.push(`/batch/${route.params.id}`)
}

const formatDate = (date: string): string => {
  return date.replace(/-/g, '/')
}
</script>

<template>
  <div class="quality-check-page">
    <div class="page-header">
      <button class="back-btn" @click="handleBack">
        <svg viewBox="0 0 24 24" width="20" height="20">
          <path fill="currentColor" d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
        </svg>
        返回
      </button>
      <h1>质量检测</h1>
      <div class="header-placeholder"></div>
    </div>
    
    <div class="loading-state" v-if="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>
    
    <div class="check-content" v-if="batch && !loading">
      <div class="batch-summary">
        <div class="batch-info">
          <span class="batch-no">{{ batch.batchNo }}</span>
          <span class="product-name">{{ batch.productName }}</span>
        </div>
        <div class="batch-details">
          <span>数量: {{ batch.quantity }} {{ batch.unit }}</span>
          <span>库位: {{ batch.locationCode }}</span>
          <span>有效期: {{ formatDate(batch.expiryDate) }}</span>
        </div>
      </div>
      
      <div class="check-type-section">
        <h3>检测类型</h3>
        <div class="check-type-options">
          <div 
            v-for="option in checkTypeOptions"
            :key="option.value"
            :class="['type-option', { active: checkForm.checkType === option.value }]"
            @click="handleCheckTypeChange(option.value as any)"
          >
            <div class="type-radio">
              <svg v-if="checkForm.checkType === option.value" viewBox="0 0 24 24" width="16" height="16">
                <path fill="#1988fa" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
              </svg>
            </div>
            <div class="type-content">
              <span class="type-label">{{ option.label }}</span>
              <span class="type-desc">{{ option.desc }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="check-items-section">
        <h3>检测项目</h3>
        <div class="check-stats">
          <div class="stat-item pass">
            <span class="stat-value">{{ passCount }}</span>
            <span class="stat-label">合格</span>
          </div>
          <div class="stat-item fail">
            <span class="stat-value">{{ failCount }}</span>
            <span class="stat-label">不合格</span>
          </div>
          <div class="stat-item pending">
            <span class="stat-value">{{ checkForm.checkItems.length - passCount - failCount }}</span>
            <span class="stat-label">待检</span>
          </div>
        </div>
        
        <div class="check-items-list">
          <div 
            v-for="item in checkForm.checkItems"
            :key="item.id"
            class="check-item"
          >
            <div class="item-header">
              <span class="item-name">{{ item.name }}</span>
              <span class="item-standard">{{ item.standard }}</span>
            </div>
            <div class="item-actions">
              <button 
                :class="['result-btn', 'pass', { active: item.result === 'pass' }]"
                @click="handleItemResultChange(item.id, 'pass')"
              >
                <svg viewBox="0 0 24 24" width="16" height="16">
                  <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                </svg>
                合格
              </button>
              <button 
                :class="['result-btn', 'fail', { active: item.result === 'fail' }]"
                @click="handleItemResultChange(item.id, 'fail')"
              >
                <svg viewBox="0 0 24 24" width="16" height="16">
                  <path fill="currentColor" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
                </svg>
                不合格
              </button>
            </div>
            <div v-if="item.result === 'fail'" class="item-remark">
              <input 
                v-model="item.remark"
                type="text"
                placeholder="请输入不合格原因"
                class="remark-input"
              />
            </div>
          </div>
        </div>
      </div>
      
      <div class="photo-section">
        <h3>拍照留证</h3>
        <div class="photo-grid">
          <div 
            v-for="(photo, index) in checkForm.photos"
            :key="index"
            class="photo-item"
          >
            <img :src="photo" alt="检测照片" />
            <button class="remove-btn" @click="handleRemovePhoto(index)">×</button>
          </div>
          <button class="add-photo-btn" @click="handleTakePhoto">
            <svg viewBox="0 0 24 24" width="24" height="24">
              <path fill="#969799" d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/>
            </svg>
            <span>拍照</span>
          </button>
        </div>
      </div>
      
      <div class="remark-section">
        <h3>备注说明</h3>
        <textarea 
          v-model="checkForm.remark"
          placeholder="请输入检测备注..."
          class="remark-textarea"
          rows="3"
        ></textarea>
      </div>
      
      <div class="result-summary">
        <div class="result-header">
          <span class="result-label">检测结果:</span>
          <span 
            class="result-value"
            :class="checkForm.overallResult"
          >
            {{ checkForm.overallResult === 'qualified' ? '合格' : checkForm.overallResult === 'rejected' ? '不合格' : '待判定' }}
          </span>
        </div>
      </div>
      
      <div class="submit-section">
        <button 
          class="submit-btn"
          :disabled="checkForm.overallResult === 'pending' || submitting"
          @click="handleSubmit"
        >
          {{ submitting ? '提交中...' : '提交检测结果' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.quality-check-page {
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
  
  .header-placeholder {
    width: 60px;
  }
}

.batch-summary {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  
  .batch-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    
    .batch-no {
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
    
    .product-name {
      font-size: 14px;
      color: #666;
    }
  }
  
  .batch-details {
    display: flex;
    gap: 16px;
    font-size: 12px;
    color: #969799;
  }
}

.check-type-section {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  
  h3 {
    font-size: 14px;
    font-weight: 600;
    color: #333;
    margin-bottom: 12px;
  }
  
  .check-type-options {
    display: flex;
    flex-direction: column;
    gap: 8px;
    
    .type-option {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      border: 1px solid #dcdfe6;
      border-radius: 8px;
      cursor: pointer;
      
      &.active {
        border-color: #1988fa;
        background: #e8f4ff;
      }
      
      .type-radio {
        width: 20px;
        height: 20px;
        border: 2px solid #dcdfe6;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        
        svg {
          width: 16px;
          height: 16px;
        }
      }
      
      .type-content {
        .type-label {
          font-size: 14px;
          color: #333;
        }
        
        .type-desc {
          font-size: 12px;
          color: #969799;
          margin-left: 8px;
        }
      }
    }
  }
}

.check-items-section {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  
  h3 {
    font-size: 14px;
    font-weight: 600;
    color: #333;
    margin-bottom: 12px;
  }
  
  .check-stats {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
    
    .stat-item {
      flex: 1;
      padding: 12px;
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
      
      &.pass {
        background: #e8f7e8;
        .stat-value { color: #07c160; }
      }
      
      &.fail {
        background: #ffe8e8;
        .stat-value { color: #f44; }
      }
      
      &.pending {
        background: #e8f4ff;
        .stat-value { color: #1988fa; }
      }
    }
  }
  
  .check-items-list {
    .check-item {
      padding: 12px;
      border-bottom: 1px solid #ebedf0;
      
      &:last-child {
        border-bottom: none;
      }
      
      .item-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;
        
        .item-name {
          font-size: 14px;
          color: #333;
        }
        
        .item-standard {
          font-size: 12px;
          color: #969799;
        }
      }
      
      .item-actions {
        display: flex;
        gap: 8px;
        
        .result-btn {
          flex: 1;
          padding: 8px;
          border: 1px solid #dcdfe6;
          border-radius: 4px;
          background: #fff;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 4px;
          
          &.pass {
            &.active {
              background: #07c160;
              color: #fff;
              border-color: #07c160;
            }
          }
          
          &.fail {
            &.active {
              background: #f44;
              color: #fff;
              border-color: #f44;
            }
          }
        }
      }
      
      .item-remark {
        margin-top: 8px;
        
        .remark-input {
          width: 100%;
          padding: 8px;
          border: 1px solid #dcdfe6;
          border-radius: 4px;
          font-size: 12px;
        }
      }
    }
  }
}

.photo-section {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  
  h3 {
    font-size: 14px;
    font-weight: 600;
    color: #333;
    margin-bottom: 12px;
  }
  
  .photo-grid {
    display: flex;
    gap: 8px;
    
    .photo-item {
      width: 80px;
      height: 80px;
      border-radius: 8px;
      overflow: hidden;
      position: relative;
      
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      
      .remove-btn {
        position: absolute;
        top: 4px;
        right: 4px;
        width: 20px;
        height: 20px;
        background: #f44;
        color: #fff;
        border: none;
        border-radius: 50%;
        cursor: pointer;
      }
    }
    
    .add-photo-btn {
      width: 80px;
      height: 80px;
      border: 1px dashed #dcdfe6;
      border-radius: 8px;
      background: #f7f8fa;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      
      span {
        font-size: 12px;
        color: #969799;
        margin-top: 4px;
      }
    }
  }
}

.remark-section {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  
  h3 {
    font-size: 14px;
    font-weight: 600;
    color: #333;
    margin-bottom: 12px;
  }
  
  .remark-textarea {
    width: 100%;
    padding: 12px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    font-size: 14px;
    resize: none;
  }
}

.result-summary {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  
  .result-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .result-label {
      font-size: 14px;
      color: #333;
    }
    
    .result-value {
      font-size: 16px;
      font-weight: 600;
      
      &.qualified { color: #07c160; }
      &.rejected { color: #f44; }
      &.pending { color: #1988fa; }
    }
  }
}

.submit-section {
  .submit-btn {
    width: 100%;
    padding: 14px;
    background: #1988fa;
    color: #fff;
    border: none;
    border-radius: 8px;
    font-size: 16px;
    cursor: pointer;
    
    &:disabled {
      background: #dcdfe6;
      color: #969799;
      cursor: not-allowed;
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