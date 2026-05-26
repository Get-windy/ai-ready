<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Card from '@/components/common/Card.vue'
import Button from '@/components/common/Button.vue'
import Empty from '@/components/common/Empty.vue'

const router = useRouter()

const printers = ref<any[]>([])
const defaultPrinter = ref('')
const loading = ref(false)

onMounted(async () => {
  await loadPrinters()
})

const loadPrinters = async () => {
  loading.value = true
  try {
    printers.value = await window.electronAPI.printers.getPrinters()
    defaultPrinter.value = await window.electronAPI.printers.getDefaultPrinter()
  } finally {
    loading.value = false
  }
}

const handleSetDefault = async (printerName: string) => {
  await window.electronAPI.printers.setDefaultPrinter(printerName)
  defaultPrinter.value = printerName
}

const handleTestPrint = async (printerName: string) => {
  try {
    await window.electronAPI.printers.testPrint(printerName)
    alert('测试打印已发送到 ' + printerName)
  } catch (error) {
    alert('测试打印失败: ' + error)
  }
}

const handleRefresh = () => {
  loadPrinters()
}

const getStatusColor = (status: string) => {
  switch (status) {
    case 'ready': return '#07c160'
    case 'printing': return '#1988fa'
    case 'error': return '#f44'
    case 'offline': return '#969799'
    default: return '#969799'
  }
}

const getStatusLabel = (status: string) => {
  switch (status) {
    case 'ready': return '就绪'
    case 'printing': return '打印中'
    case 'error': return '错误'
    case 'offline': return '离线'
    default: return '未知'
  }
}
</script>

<template>
  <div class="printers-page">
    <div class="page-header">
      <h1>打印机管理</h1>
      <Button icon="refresh" @click="handleRefresh">刷新</Button>
    </div>
    
    <div class="printers-list" v-if="!loading">
      <div v-if="printers.length === 0" class="empty-container">
        <Empty description="未检测到打印机" />
      </div>
      
      <Card v-else>
        <div 
          v-for="printer in printers"
          :key="printer.name"
          class="printer-item"
        >
          <div class="printer-info">
            <div class="printer-name">
              {{ printer.name }}
              <span v-if="printer.name === defaultPrinter" class="default-badge">
                默认
              </span>
            </div>
            <div class="printer-details">
              <span class="status" :style="{ color: getStatusColor(printer.status) }">
                {{ getStatusLabel(printer.status) }}
              </span>
              <span class="type">{{ printer.type || '本地打印机' }}</span>
            </div>
          </div>
          
          <div class="printer-actions">
            <Button 
              v-if="printer.name !== defaultPrinter"
              size="small"
              @click="handleSetDefault(printer.name)"
            >
              设为默认
            </Button>
            <Button 
              size="small"
              @click="handleTestPrint(printer.name)"
            >
              测试打印
            </Button>
          </div>
        </div>
      </Card>
    </div>
    
    <div v-else class="loading-container">
      <span>加载中...</span>
    </div>
    
    <div class="help-section">
      <Card title="打印机设置帮助">
        <div class="help-content">
          <p>1. 确保打印机已正确安装并连接到电脑</p>
          <p>2. 选择一个默认打印机用于自动打印任务</p>
          <p>3. 点击"测试打印"验证打印机是否正常工作</p>
          <p>4. 如遇问题，请检查打印机驱动和连接状态</p>
        </div>
      </Card>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.printers-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    
    h1 {
      font-size: 24px;
      font-weight: 600;
      color: #333;
    }
  }
}

.printer-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #ebedf0;
  
  &:last-child {
    border-bottom: none;
  }
  
  .printer-info {
    .printer-name {
      font-size: 16px;
      font-weight: 600;
      color: #333;
      
      .default-badge {
        display: inline-flex;
        padding: 2px 8px;
        background: #1988fa;
        color: #fff;
        border-radius: 4px;
        font-size: 12px;
        margin-left: 8px;
      }
    }
    
    .printer-details {
      margin-top: 8px;
      
      .status {
        font-size: 14px;
        margin-right: 12px;
      }
      
      .type {
        font-size: 14px;
        color: #969799;
      }
    }
  }
  
  .printer-actions {
    display: flex;
    gap: 8px;
  }
}

.empty-container, .loading-container {
  padding: 60px 20px;
  text-align: center;
  color: #969799;
}

.help-section {
  margin-top: 24px;
  
  .help-content {
    padding: 16px;
    
    p {
      font-size: 14px;
      color: #333;
      margin-bottom: 8px;
      
      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}
</style>