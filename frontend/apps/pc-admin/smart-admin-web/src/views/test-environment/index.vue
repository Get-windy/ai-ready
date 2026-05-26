<template>
  <div class="test-environment-page">
    <!-- Page Header -->
    <div class="page-header">
      <h1 class="page-title">测试环境管理</h1>
      <p class="page-subtitle">
        Sprint 27+1测试环境配置与监控
        <el-link type="primary" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新状态
        </el-link>
      </p>
    </div>

    <!-- Environment Status Cards -->
    <div class="environment-status-cards">
      <el-card class="status-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="card-title">环境概览</span>
            <el-tag :type="getOverviewStatusType(environmentOverview.status)" size="small">
              {{ getOverviewStatusText(environmentOverview.status) }}
            </el-tag>
          </div>
        </template>
        <div class="overview-content">
          <div class="overview-item">
            <span class="label">运行时长</span>
            <span class="value">{{ formatDuration(environmentOverview.uptime) }}</span>
          </div>
          <div class="overview-item">
            <span class="label">服务数量</span>
            <span class="value">{{ environmentOverview.serviceCount }}</span>
          </div>
          <div class="overview-item">
            <span class="label">API端点</span>
            <span class="value">{{ environmentOverview.apiEndpoints }}</span>
          </div>
          <div class="overview-item">
            <span class="label">最后部署</span>
            <span class="value">{{ formatDate(environmentOverview.lastDeployed) }}</span>
          </div>
        </div>
      </el-card>

      <el-card class="status-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="card-title">网络状态</span>
            <el-tag :type="networkStatus.type" size="small">
              {{ networkStatus.label }}
            </el-tag>
          </div>
        </template>
        <div class="network-content">
          <div class="network-item">
            <span class="label">延迟</span>
            <div class="network-value">
              <span class="value">{{ networkStatus.latency }}ms</span>
              <el-progress 
                :percentage="getLatencyScore(networkStatus.latency)" 
                :status="getLatencyStatus(networkStatus.latency)"
                :show-text="false"
                style="width: 100px; margin-left: 10px;"
              />
            </div>
          </div>
          <div class="network-item">
            <span class="label">可用性</span>
            <div class="network-value">
              <span class="value">{{ networkStatus.availability }}%</span>
              <el-progress 
                :percentage="networkStatus.availability" 
                :status="getAvailabilityStatus(networkStatus.availability)"
                style="width: 100px; margin-left: 10px;"
              />
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="status-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="card-title">安全状态</span>
            <el-tag :type="securityStatus.type" size="small">
              {{ securityStatus.label }}
            </el-tag>
          </div>
        </template>
        <div class="security-content">
          <div class="security-item">
            <span class="label">安全评分</span>
            <div class="security-value">
              <span class="score">{{ securityStatus.score }}/100</span>
              <el-rate 
                v-model="securityStatus.rating" 
                disabled 
                show-score
                text-color="#ff9900"
                score-template="{value} 分"
              />
            </div>
          </div>
          <div class="security-item">
            <span class="label">漏洞数量</span>
            <div class="security-value">
              <span class="value">{{ securityStatus.vulnerabilities }}</span>
              <el-tag v-if="securityStatus.vulnerabilities > 0" type="danger" size="small">
                需处理
              </el-tag>
              <el-tag v-else type="success" size="small">
                安全
              </el-tag>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- Service Management Table -->
    <el-card class="service-table-card" shadow="never">
      <template #header>
        <div class="table-header">
          <span class="card-title">服务管理</span>
          <div class="table-actions">
            <el-input
              v-model="serviceFilter"
              placeholder="搜索服务..."
              clearable
              style="width: 200px; margin-right: 10px;"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleAddService">
              <el-icon><Plus /></el-icon>
              新增服务
            </el-button>
            <el-button @click="handleRestartAll">
              <el-icon><Refresh /></el-icon>
              重启所有
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="filteredServices"
        style="width: 100%"
        v-loading="tableLoading"
        row-key="id"
      >
        <el-table-column prop="name" label="服务名称" width="180">
          <template #default="{ row }">
            <div class="service-name-cell">
              <el-icon :size="20" :color="getServiceColor(row.type)">
                <component :is="getServiceIcon(row.type)" />
              </el-icon>
              <span class="service-name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getServiceTypeTag(row.type)" size="small">
              {{ row.type }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag 
              :type="getStatusType(row.status)" 
              :effect="row.status === 'running' ? 'light' : 'plain'"
              size="small"
            >
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="version" label="版本" width="120" />
        
        <el-table-column prop="health" label="健康度" width="120">
          <template #default="{ row }">
            <div class="health-cell">
              <el-progress 
                :percentage="row.health" 
                :status="getHealthStatus(row.health)"
                :show-text="false"
                style="width: 60px; margin-right: 10px;"
              />
              <span>{{ row.health }}%</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="uptime" label="运行时间" width="140">
          <template #default="{ row }">
            {{ formatDuration(row.uptime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="endpoint" label="访问地址" width="200">
          <template #default="{ row }">
            <el-link 
              type="primary" 
              :href="row.endpoint" 
              target="_blank"
              :disabled="row.status !== 'running'"
            >
              {{ row.endpoint }}
            </el-link>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button 
                type="primary" 
                size="small" 
                @click="handleServiceDetail(row)"
                :disabled="row.status !== 'running'"
              >
                详情
              </el-button>
              
              <el-dropdown @command="(command) => handleServiceAction(row, command)">
                <el-button size="small">
                  操作<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="restart">
                      <el-icon><Refresh /></el-icon>重启
                    </el-dropdown-item>
                    <el-dropdown-item command="logs">
                      <el-icon><Document /></el-icon>查看日志
                    </el-dropdown-item>
                    <el-dropdown-item command="config" divided>
                      <el-icon><Setting /></el-icon>配置
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided class="danger">
                      <el-icon><Delete /></el-icon>删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalServices"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- Performance Charts Section -->
    <div class="charts-section">
      <el-card class="chart-card" shadow="never">
        <template #header>
          <span class="card-title">性能监控</span>
          <el-select v-model="performanceTimeRange" @change="handleTimeRangeChange" size="small">
            <el-option label="最近1小时" value="1h" />
            <el-option label="最近24小时" value="24h" />
            <el-option label="最近7天" value="7d" />
            <el-option label="最近30天" value="30d" />
          </el-select>
        </template>
        <div class="chart-container">
          <div ref="responseTimeChart" class="chart-item"></div>
        </div>
      </el-card>

      <el-card class="chart-card" shadow="never">
        <template #header>
          <span class="card-title">资源使用情况</span>
        </template>
        <div class="chart-container">
          <div ref="resourceChart" class="chart-item"></div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import { 
  Refresh, Search, Plus, ArrowDown, Document, 
  Setting, Delete, Cpu, Database, Network, 
  Cloud, Monitor, Service, Server
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 响应式数据
const environmentOverview = ref({
  status: 'healthy',
  uptime: 86400000, // 24小时
  serviceCount: 12,
  apiEndpoints: 45,
  lastDeployed: new Date(Date.now() - 3600000) // 1小时前
})

const networkStatus = ref({
  type: 'success',
  label: '正常',
  latency: 85,
  availability: 99.9
})

const securityStatus = ref({
  type: 'warning',
  label: '良好',
  score: 85,
  rating: 4,
  vulnerabilities: 2
})

const services = ref([
  { id: 1, name: '用户服务', type: 'user-service', status: 'running', version: '1.2.3', health: 98, uptime: 172800000, endpoint: 'http://user-service.test.ai-ready.com' },
  { id: 2, name: '订单服务', type: 'order-service', status: 'running', version: '2.1.0', health: 99, uptime: 86400000, endpoint: 'http://order-service.test.ai-ready.com' },
  { id: 3, name: '库存服务', type: 'inventory-service', status: 'running', version: '1.5.2', health: 97, uptime: 259200000, endpoint: 'http://inventory-service.test.ai-ready.com' },
  { id: 4, name: '支付服务', type: 'payment-service', status: 'degraded', version: '1.0.1', health: 65, uptime: 43200000, endpoint: 'http://payment-service.test.ai-ready.com' },
  { id: 5, name: '通知服务', type: 'notification-service', status: 'stopped', version: '1.3.4', health: 0, uptime: 0, endpoint: 'http://notification-service.test.ai-ready.com' },
  { id: 6, name: '日志服务', type: 'logging-service', status: 'running', version: '1.1.0', health: 100, uptime: 604800000, endpoint: 'http://logging-service.test.ai-ready.com' },
  { id: 7, name: '监控服务', type: 'monitoring-service', status: 'running', version: '2.0.0', health: 99, uptime: 86400000, endpoint: 'http://monitoring-service.test.ai-ready.com' },
  { id: 8, name: '认证服务', type: 'auth-service', status: 'running', version: '1.4.2', health: 98, uptime: 172800000, endpoint: 'http://auth-service.test.ai-ready.com' },
  { id: 9, name: '配置服务', type: 'config-service', status: 'running', version: '1.0.5', health: 95, uptime: 43200000, endpoint: 'http://config-service.test.ai-ready.com' },
  { id: 10, name: '网关服务', type: 'gateway-service', status: 'running', version: '2.3.1', health: 99, uptime: 259200000, endpoint: 'http://gateway.test.ai-ready.com' }
])

const serviceFilter = ref('')
const tableLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalServices = ref(services.value.length)

const performanceTimeRange = ref('24h')
const responseTimeChart = ref<HTMLElement | null>(null)
const resourceChart = ref<HTMLElement | null>(null)
let responseTimeEchart: ECharts | null = null
let resourceEchart: ECharts | null = null

// 计算属性
const filteredServices = computed(() => {
  const filtered = services.value.filter(service => {
    if (!serviceFilter.value) return true
    const filter = serviceFilter.value.toLowerCase()
    return (
      service.name.toLowerCase().includes(filter) ||
      service.type.toLowerCase().includes(filter) ||
      service.version.toLowerCase().includes(filter)
    )
  })
  totalServices.value = filtered.length
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filtered.slice(start, end)
})

// 方法
const getOverviewStatusType = (status: string) => {
  const map: Record<string, any> = {
    healthy: 'success',
    degraded: 'warning',
    unhealthy: 'danger',
    unknown: 'info'
  }
  return map[status] || 'info'
}

const getOverviewStatusText = (status: string) => {
  const map: Record<string, string> = {
    healthy: '健康',
    degraded: '降级',
    unhealthy: '异常',
    unknown: '未知'
  }
  return map[status] || status
}

const formatDuration = (milliseconds: number) => {
  if (milliseconds === 0) return '未运行'
  const seconds = Math.floor(milliseconds / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  
  if (days > 0) return `${days}天${hours % 24}小时`
  if (hours > 0) return `${hours}小时${minutes % 60}分钟`
  if (minutes > 0) return `${minutes}分钟${seconds % 60}秒`
  return `${seconds}秒`
}

const formatDate = (date: Date) => {
  return date.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getLatencyScore = (latency: number) => {
  if (latency < 50) return 100
  if (latency < 100) return 80
  if (latency < 200) return 60
  if (latency < 500) return 40
  return 20
}

const getLatencyStatus = (latency: number) => {
  if (latency < 100) return 'success'
  if (latency < 200) return ''
  if (latency < 500) return 'warning'
  return 'exception'
}

const getAvailabilityStatus = (availability: number) => {
  if (availability >= 99.9) return 'success'
  if (availability >= 99) return ''
  if (availability >= 95) return 'warning'
  return 'exception'
}

const getServiceColor = (type: string) => {
  const colors: Record<string, string> = {
    'user-service': '#1890ff',
    'order-service': '#52c41a',
    'inventory-service': '#722ed1',
    'payment-service': '#fa8c16',
    'notification-service': '#f5222d',
    'logging-service': '#13c2c2',
    'monitoring-service': '#2f54eb',
    'auth-service': '#eb2f96',
    'config-service': '#faad14',
    'gateway-service': '#a0d911'
  }
  return colors[type] || '#8c8c8c'
}

const getServiceIcon = (type: string) => {
  const iconMap: Record<string, any> = {
    'user-service': 'UserFilled',
    'order-service': 'ShoppingCart',
    'inventory-service': 'Box',
    'payment-service': 'Money',
    'notification-service': 'Bell',
    'logging-service': 'Document',
    'monitoring-service': Monitor,
    'auth-service': 'Lock',
    'config-service': Setting,
    'gateway-service': Network
  }
  return iconMap[type] || Service
}

const getServiceTypeTag = (type: string) => {
  const tagMap: Record<string, any> = {
    'user-service': 'primary',
    'order-service': 'success',
    'inventory-service': '',
    'payment-service': 'warning',
    'notification-service': 'danger',
    'logging-service': 'info',
    'monitoring-service': 'primary',
    'auth-service': 'success',
    'config-service': 'warning',
    'gateway-service': 'info'
  }
  return tagMap[type] || 'info'
}

const getStatusType = (status: string) => {
  const map: Record<string, any> = {
    running: 'success',
    degraded: 'warning',
    stopped: 'danger',
    starting: 'info',
    stopping: 'warning'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    running: '运行中',
    degraded: '降级',
    stopped: '已停止',
    starting: '启动中',
    stopping: '停止中'
  }
  return map[status] || status
}

const getHealthStatus = (health: number) => {
  if (health >= 90) return 'success'
  if (health >= 70) return ''
  if (health >= 50) return 'warning'
  return 'exception'
}

// 事件处理
const handleRefresh = () => {
  tableLoading.value = true
  setTimeout(() => {
    tableLoading.value = false
    ElMessage.success('数据已刷新')
  }, 1000)
}

const handleAddService = () => {
  ElMessageBox.prompt('请输入新服务名称', '新增服务', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /^.{2,50}$/,
    inputErrorMessage: '服务名称长度应为2-50个字符'
  }).then(({ value }) => {
    ElMessage.success(`服务"${value}"已添加`)
  }).catch(() => {
    // 用户取消
  })
}

const handleRestartAll = async () => {
  try {
    await ElMessageBox.confirm('确定要重启所有服务吗？', '重启确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    tableLoading.value = true
    setTimeout(() => {
      services.value.forEach(service => {
        if (service.status === 'running') {
          service.uptime = 0
          setTimeout(() => {
            service.status = 'running'
            service.uptime = 1000
            service.health = 100
          }, 100)
        }
      })
      tableLoading.value = false
      ElMessage.success('所有服务已重启')
    }, 2000)
  } catch {
    // 用户取消
  }
}

const handleServiceDetail = (service: any) => {
  ElMessage.info(`查看服务详情: ${service.name}`)
}

const handleServiceAction = (service: any, command: string) => {
  switch (command) {
    case 'restart':
      ElMessageBox.confirm(`确定要重启服务"${service.name}"吗？`, '重启确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        service.status = 'starting'
        service.uptime = 0
        setTimeout(() => {
          service.status = 'running'
          service.uptime = 1000
          service.health = 100
          ElMessage.success(`服务"${service.name}"已重启`)
        }, 2000)
      })
      break
    case 'logs':
      ElMessage.info(`查看服务日志: ${service.name}`)
      break
    case 'config':
      ElMessage.info(`配置服务: ${service.name}`)
      break
    case 'delete':
      ElMessageBox.confirm(`确定要删除服务"${service.name}"吗？`, '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }).then(() => {
        const index = services.value.findIndex(s => s.id === service.id)
        if (index > -1) {
          services.value.splice(index, 1)
          ElMessage.success(`服务"${service.name}"已删除`)
        }
      })
      break
  }
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
}

const handleTimeRangeChange = (range: string) => {
  // 这里应该重新加载对应时间范围的数据
  initCharts()
}

// 图表初始化
const initCharts = () => {
  if (!responseTimeChart.value || !resourceChart.value) return

  // 销毁之前的图表实例
  if (responseTimeEchart) {
    responseTimeEchart.dispose()
  }
  if (resourceEchart) {
    resourceEchart.dispose()
  }

  // 初始化响应时间图表
  responseTimeEchart = echarts.init(responseTimeChart.value)
  const responseTimeOption = {
    title: {
      text: 'API响应时间趋势',
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal'
      }
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const date = params[0].axisValue
        const value = params[0].value
        return `${date}<br/>平均响应时间: ${value}ms`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'],
      axisLabel: {
        color: '#666'
      }
    },
    yAxis: {
      type: 'value',
      name: '毫秒(ms)',
      min: 0,
      max: 500,
      axisLabel: {
        color: '#666',
        formatter: '{value}'
      }
    },
    series: [
      {
        name: '响应时间',
        type: 'line',
        smooth: true,
        data: [120, 200, 150, 80, 70, 110, 130],
        itemStyle: {
          color: '#1890ff'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
            { offset: 1, color: 'rgba(24, 144, 255, 0.05)' }
          ])
        }
      }
    ]
  }

  // 初始化资源使用图表
  resourceEchart = echarts.init(resourceChart.value)
  const resourceOption = {
    title: {
      text: '资源使用分布',
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c}%'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'center'
    },
    series: [
      {
        name: '资源使用',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 18,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: 35, name: 'CPU' },
          { value: 25, name: '内存' },
          { value: 20, name: '磁盘' },
          { value: 15, name: '网络' },
          { value: 5, name: '其他' }
        ],
        color: ['#1890ff', '#52c41a', '#722ed1', '#fa8c16', '#13c2c2']
      }
    ]
  }

  responseTimeEchart.setOption(responseTimeOption)
  resourceEchart.setOption(resourceOption)
}

// 生命周期
onMounted(() => {
  nextTick(() => {
    initCharts()
  })
  
  // 监听窗口大小变化，重新渲染图表
  const handleResize = () => {
    if (responseTimeEchart) responseTimeEchart.resize()
    if (resourceEchart) resourceEchart.resize()
  }
  window.addEventListener('resize', handleResize)
  
  onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    if (responseTimeEchart) responseTimeEchart.dispose()
    if (resourceEchart) resourceEchart.dispose()
  })
})
</script>

<style scoped lang="scss">
.test-environment-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 84px);
  
  .page-header {
    margin-bottom: 20px;
    
    .page-title {
      font-size: 24px;
      font-weight: 600;
      color: #303133;
      margin: 0 0 8px 0;
    }
    
    .page-subtitle {
      font-size: 14px;
      color: #606266;
      margin: 0;
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }
  
  .environment-status-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 16px;
    margin-bottom: 20px;
    
    .status-card {
      :deep(.el-card__header) {
        padding: 16px 20px;
        border-bottom: 1px solid #e8e8e8;
      }
      
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
      
      .card-title {
        font-size: 16px;
        font-weight: 500;
        color: #303133;
      }
      
      .overview-item,
      .network-item,
      .security-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
        padding: 4px 0;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .label {
          font-size: 14px;
          color: #606266;
        }
        
        .value {
          font-size: 16px;
          font-weight: 500;
          color: #303133;
        }
        
        .network-value,
        .security-value {
          display: flex;
          align-items: center;
          gap: 8px;
        }
        
        .score {
          font-size: 18px;
          font-weight: 600;
          color: #fa8c16;
        }
      }
    }
  }
  
  .service-table-card {
    margin-bottom: 20px;
    
    :deep(.el-card__header) {
      padding: 16px 20px;
      border-bottom: 1px solid #e8e8e8;
    }
    
    .table-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .table-actions {
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }
    
    .service-name-cell {
      display: flex;
      align-items: center;
      gap: 8px;
      
      .service-name {
        font-weight: 500;
      }
    }
    
    .health-cell {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    
    .action-buttons {
      display: flex;
      gap: 8px;
      
      :deep(.el-dropdown .el-button) {
        padding: 5px 8px;
      }
    }
    
    .table-footer {
      display: flex;
      justify-content: flex-end;
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #e8e8e8;
    }
  }
  
  .charts-section {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
    gap: 16px;
    
    .chart-card {
      :deep(.el-card__header) {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 16px 20px;
        border-bottom: 1px solid #e8e8e8;
      }
      
      .chart-container {
        height: 300px;
        padding: 0;
        
        .chart-item {
          width: 100%;
          height: 100%;
        }
      }
    }
  }
}

// 响应式设计
@media (max-width: 1200px) {
  .test-environment-page {
    .charts-section {
      grid-template-columns: 1fr;
    }
  }
}

@media (max-width: 768px) {
  .test-environment-page {
    padding: 12px;
    
    .environment-status-cards {
      grid-template-columns: 1fr;
    }
    
    .service-table-card {
      :deep(.el-card__body) {
        overflow-x: auto;
      }
    }
    
    .table-header {
      flex-direction: column;
      align-items: flex-start !important;
      gap: 12px;
      
      .table-actions {
        width: 100%;
        justify-content: space-between;
      }
    }
  }
}

// 暗色主题支持
:root.dark {
  .test-environment-page {
    background-color: #141414;
    
    .page-title {
      color: #e5e5e5;
    }
    
    .page-subtitle {
      color: #a3a3a3;
    }
    
    .status-card {
      background-color: #1f1f1f;
      border-color: #303030;
      
      :deep(.el-card__header) {
        border-bottom-color: #303030;
        background-color: #1f1f1f;
      }
    }
    
    .service-table-card {
      background-color: #1f1f1f;
      border-color: #303030;
      
      :deep(.el-card__header) {
        border-bottom-color: #303030;
        background-color: #1f1f1f;
      }
      
      .table-footer {
        border-top-color: #303030;
      }
    }
  }
}
</style>