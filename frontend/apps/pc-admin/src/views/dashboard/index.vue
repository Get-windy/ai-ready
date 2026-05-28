<template>
  <div class="dashboard-home-page">
    <!-- Header -->
    <div class="page-header">
      <h1 class="page-title">监控大盘</h1>
      <p class="page-subtitle">
        系统整体运行状态实时监控
        <a @click="handleRefresh" style="color: #1890ff; cursor: pointer;">
          <ReloadOutlined />
          更新数据
        </a>
      </p>
    </div>

    <!-- KPI Cards Row -->
    <div class="kpi-row">
      <a-card class="kpi-card" :bordered="false">
        <template #title>
          <span class="card-title">系统健康度</span>
        </template>
        <div class="kpi-value">
          <span class="value">98.5</span>
          <span class="unit">%</span>
        </div>
        <div class="kpi-sub">
          <ArrowUpOutlined class="trend-icon up" />
          <span class="trend-value">+2.3%</span>
          <span class="trend-label">环比上期</span>
        </div>
      </a-card>

      <a-card class="kpi-card" :bordered="false">
        <template #title>
          <span class="card-title">平均响应时间</span>
        </template>
        <div class="kpi-value">
          <span class="value">245</span>
          <span class="unit">ms</span>
        </div>
        <div class="kpi-sub">
          <ArrowDownOutlined class="trend-icon down" />
          <span class="trend-value">-12%</span>
          <span class="trend-label">环比上期</span>
        </div>
      </a-card>

      <a-card class="kpi-card" :bordered="false">
        <template #title>
          <span class="card-title">当前吞吐量</span>
        </template>
        <div class="kpi-value">
          <span class="value">1250</span>
          <span class="unit">req/s</span>
        </div>
        <div class="kpi-sub">
          <ArrowUpOutlined class="trend-icon up" />
          <span class="trend-value">+8.5%</span>
          <span class="trend-label">环比上期</span>
        </div>
      </a-card>

      <a-card class="kpi-card" :bordered="false">
        <template #title>
          <span class="card-title">错误率</span>
        </template>
        <div class="kpi-value">
          <span class="value">0.12</span>
          <span class="unit">%</span>
        </div>
        <div class="kpi-sub">
          <DashboardOutlined class="trend-icon stable" />
          <span class="trend-value">0%</span>
          <span class="trend-label">环比上期</span>
        </div>
      </a-card>
    </div>

    <!-- Main Content Area -->
    <div class="main-content">
      <!-- Left Column -->
      <div class="left-column">
        <!-- Service Status -->
        <a-card class="status-card" :bordered="false">
          <template #title>
            <span class="card-title">服务健康状态</span>
          </template>
          <template #extra>
            <a @click="handleViewServices" style="color: #1890ff; cursor: pointer;">查看全部</a>
          </template>
          <div class="service-grid">
            <div 
              v-for="service in services" 
              :key="service.id" 
              class="service-item"
              :class="`status-${service.status}`"
              @click="handleServiceClick(service)"
            >
              <div class="service-icon">
                <CloudServerOutlined v-if="service.type === 'api'" :style="{ fontSize: '32px' }" />
                <DatabaseOutlined v-if="service.type === 'database'" :style="{ fontSize: '32px' }" />
                <DesktopOutlined v-if="service.type === 'cache' || service.type === 'queue'" :style="{ fontSize: '32px' }" />
              </div>
              <div class="service-info">
                <div class="service-name">{{ service.name }}</div>
                <div class="service-status">{{ serviceStatusText(service.status) }}</div>
              </div>
            </div>
          </div>
        </a-card>

        <!-- Alerts -->
        <a-card class="alerts-card" :bordered="false">
          <template #title>
            <span class="card-title">最近告警</span>
            <a-badge v-if="pendingAlerts > 0" :count="pendingAlerts" :status="pendingAlerts > 3 ? 'error' : 'warning'" />
          </template>
          <div class="alerts-list">
            <div 
              v-for="alert in recentAlerts" 
              :key="alert.id" 
              class="alert-item"
              :class="`level-${alert.level.toLowerCase()}`"
            >
              <div class="alert-header">
                <CloseCircleOutlined v-if="alert.level === 'P0' || alert.level === 'P1'" :class="`level-icon ${alert.level.toLowerCase()}`" />
                <WarningOutlined v-if="alert.level === 'P2'" :class="`level-icon ${alert.level.toLowerCase()}`" />
                <AlertOutlined v-if="alert.level === 'P3'" :class="`level-icon ${alert.level.toLowerCase()}`" />
                <span class="alert-title">{{ alert.title }}</span>
              </div>
              <div class="alert-message">{{ alert.message }}</div>
              <div class="alert-footer">
                <span class="alert-time">{{ formatTime(alert.timestamp) }}</span>
                <a-button 
                  v-if="!alert.acknowledged" 
                  type="primary" 
                  size="small"
                  @click="handleAcknowledge(alert)"
                >
                  确认
                </a-button>
              </div>
            </div>
          </div>
        </a-card>
      </div>

      <!-- Right Column -->
      <div class="right-column">
        <!-- Performance Chart -->
        <a-card class="chart-card" :bordered="false">
          <template #title>
            <span class="card-title">性能趋势</span>
          </template>
          <template #extra>
            <a-range-picker
              v-model:value="timeRange"
              show-time
              format="YYYY-MM-DD HH:mm:ss"
              size="small"
              @change="handleTimeRangeChange"
            />
          </template>
          <div ref="chartRef" class="echarts-container"></div>
        </a-card>

        <!-- Resource Usage -->
        <a-card class="resource-card" :bordered="false">
          <template #title>
            <span class="card-title">资源使用率</span>
          </template>
          <div class="resource-grid">
            <div class="resource-item">
              <div class="resource-header">
                <span class="resource-name">CPU</span>
                <span class="resource-value">{{ cpuUsage }}%</span>
              </div>
              <a-progress
                :percent="cpuUsage"
                :status="getCpuStatus"
                :stroke-width="16"
              />
            </div>

            <div class="resource-item">
              <div class="resource-header">
                <span class="resource-name">内存</span>
                <span class="resource-value">{{ memoryUsage }}%</span>
              </div>
              <a-progress
                :percent="memoryUsage"
                :status="getMemoryStatus"
                :stroke-width="16"
              />
            </div>

            <div class="resource-item">
              <div class="resource-header">
                <span class="resource-name">磁盘</span>
                <span class="resource-value">{{ diskUsage }}%</span>
              </div>
              <a-progress
                :percent="diskUsage"
                :status="getDiskStatus"
                :stroke-width="16"
              />
            </div>

            <div class="resource-item">
              <div class="resource-header">
                <span class="resource-name">网络</span>
                <span class="resource-value">{{ networkUsage }} MB/s</span>
              </div>
              <a-progress
                :percent="networkUsage / 100 * 10"
                :stroke-width="16"
              />
            </div>
          </div>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { message } from 'ant-design-vue';
import {
  ReloadOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  CloudServerOutlined,
  AlertOutlined,
  CloseCircleOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  DesktopOutlined
} from '@ant-design/icons-vue';
import * as ECharts from 'echarts';
import dayjs from 'dayjs';

// Types
interface Service {
  id: string;
  name: string;
  type: 'api' | 'database' | 'cache' | 'queue';
  status: 'running' | 'warning' | 'error';
}

interface Alert {
  id: string;
  level: 'P0' | 'P1' | 'P2' | 'P3';
  title: string;
  message: string;
  timestamp: string;
  acknowledged: boolean;
}

// State
const timeRange = ref<[dayjs.Dayjs, dayjs.Dayjs]>([
  dayjs().subtract(8, 'hour'),
  dayjs()
]);
const chartRef = ref<HTMLElement | null>(null);
let chartInstance: ECharts.ECharts | null = null;

// Services data
const services = ref<Service[]>([
  { id: 'api-gateway', name: 'API网关', type: 'api', status: 'running' },
  { id: 'database', name: '主数据库', type: 'database', status: 'running' },
  { id: 'redis', name: 'Redis缓存', type: 'cache', status: 'warning' },
  { id: 'kafka', name: '消息队列', type: 'queue', status: 'running' }
]);

// Alerts data
const alerts = ref<Alert[]>([
  { 
    id: '1', 
    level: 'P1', 
    title: 'Redis内存使用率过高', 
    message: 'Redis缓存内存使用率达到85%', 
    timestamp: new Date(Date.now() - 5 * 60000).toISOString(), 
    acknowledged: false 
  },
  { 
    id: '2', 
    level: 'P2', 
    title: 'API响应时间波动', 
    message: '过去10分钟内API平均响应时间波动超过20%', 
    timestamp: new Date(Date.now() - 15 * 60000).toISOString(), 
    acknowledged: false 
  },
  { 
    id: '3', 
    level: 'P3', 
    title: '磁盘空间预警', 
    message: '日志存储磁盘空间剩余15%', 
    timestamp: new Date(Date.now() - 2 * 3600000).toISOString(), 
    acknowledged: true 
  }
]);

// Resource data
const cpuUsage = ref(35);
const memoryUsage = ref(62);
const diskUsage = ref(45);
const networkUsage = ref(850);

// Computed
const recentAlerts = computed(() => alerts.value.slice(0, 5));
const pendingAlerts = computed(() => alerts.value.filter(a => !a.acknowledged).length);

const getCpuStatus = computed(() => 
  cpuUsage.value > 80 ? 'exception' : cpuUsage.value > 60 ? 'active' : 'normal'
);

const getMemoryStatus = computed(() => 
  memoryUsage.value > 80 ? 'exception' : memoryUsage.value > 60 ? 'active' : 'normal'
);

const getDiskStatus = computed(() => 
  diskUsage.value > 80 ? 'exception' : diskUsage.value > 60 ? 'active' : 'normal'
);

// Methods
const serviceStatusText = (status: string) => {
  const texts: Record<string, string> = {
    running: '运行中',
    warning: '预警',
    error: '异常'
  };
  return texts[status] || '未知';
};

const formatTime = (timestamp: string) => {
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
  return date.toLocaleString('zh-CN');
};

const handleRefresh = () => {
  message.success('数据已刷新');
};

const handleViewServices = () => {
  message.info('查看服务详情');
};

const handleServiceClick = (service: Service) => {
  message.info(`查看服务: ${service.name}`);
};

const handleAcknowledge = (alert: Alert) => {
  const index = alerts.value.findIndex(a => a.id === alert.id);
  if (index !== -1) {
    alerts.value[index].acknowledged = true;
  }
  message.success('告警已确认');
};

const handleTimeRangeChange = (dates: any) => {
  console.log('Time range changed:', dates);
};

// Chart initialization
const initChart = () => {
  if (!chartRef.value) return;
  
  chartInstance = ECharts.init(chartRef.value, 'light');
  
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const params0 = params[0];
        return `${params0.name}<br/>${params0.seriesName}: ${params0.value.toFixed(2)}ms`;
      }
    },
    legend: {
      data: ['响应时间', '错误率'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['07:00', '08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00']
    },
    yAxis: [
      {
        type: 'value',
        name: '响应时间(ms)',
        position: 'left',
        splitLine: {
          lineStyle: {
            type: 'dashed'
          }
        }
      },
      {
        type: 'value',
        name: '错误率(%)',
        position: 'right',
        min: 0,
        max: 1,
        splitLine: {
          lineStyle: {
            type: 'dashed'
          }
        }
      }
    ],
    series: [
      {
        name: '响应时间',
        type: 'line',
        smooth: true,
        data: [245, 238, 252, 248, 242, 247, 255, 249, 245],
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '错误率',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: [0.12, 0.11, 0.13, 0.12, 0.11, 0.12, 0.13, 0.12, 0.12],
        itemStyle: { color: '#F56C6C' }
      }
    ]
  };
  
  chartInstance.setOption(option);
};

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize();
  }
};

// Lifecycle
onMounted(() => {
  initChart();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (chartInstance) {
    chartInstance.dispose();
  }
});
</script>

<style scoped>
.dashboard-home-page {
  padding: 24px;
  background: #F5F7FA;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 8px 0;
  font-size: 32px;
  font-weight: 600;
  color: #303133;
}

.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 12px;
}

/* KPI Row */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.kpi-card {
  cursor: pointer;
  transition: all 0.3s;
}

.kpi-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-title {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

.kpi-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin: 16px 0;
}

.kpi-value .value {
  font-size: 36px;
  font-weight: 600;
  color: #303133;
}

.kpi-value .unit {
  font-size: 14px;
  color: #909399;
}

.kpi-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.trend-icon {
  font-size: 16px;
}

.trend-icon.up {
  color: #67C23A;
}

.trend-icon.down {
  color: #F56C6C;
}

.trend-icon.stable {
  color: #909399;
}

.trend-value {
  font-weight: 500;
}

.trend-value.up {
  color: #67C23A;
}

.trend-value.down {
  color: #F56C6C;
}

.trend-value.stable {
  color: #909399;
}

.trend-label {
  color: #909399;
}

/* Main Content */
.main-content {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 24px;
}

.left-column, .right-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Status Card */
.status-card :deep(.ant-card-head) {
  padding: 16px 20px;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.service-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.service-item:hover {
  background: #F5F7FA;
  transform: translateX(4px);
}

.service-item.status-running .service-icon {
  color: #67C23A;
}

.service-item.status-warning .service-icon {
  color: #E6A23C;
}

.service-item.status-error .service-icon {
  color: #F56C6C;
}

.service-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F0F2F5;
  border-radius: 8px;
}

.service-name {
  font-weight: 500;
  font-size: 14px;
  color: #303133;
}

.service-status {
  font-size: 12px;
  color: #909399;
}

/* Alerts Card */
.alerts-card :deep(.ant-card-head) {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.alerts-list {
  max-height: 300px;
  overflow-y: auto;
}

.alert-item {
  padding: 12px;
  background: white;
  border-radius: 8px;
  margin-bottom: 8px;
  transition: all 0.3s;
}

.alert-item:hover {
  background: #F5F7FA;
}

.alert-item.level-p0, .alert-item.level-p1 {
  border-left: 3px solid #F56C6C;
}

.alert-item.level-p2 {
  border-left: 3px solid #E6A23C;
}

.alert-item.level-p3 {
  border-left: 3px solid #409EFF;
}

.alert-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.level-icon {
  font-size: 16px;
}

.alert-item.level-p0 .level-icon, .alert-item.level-p1 .level-icon {
  color: #F56C6C;
}

.alert-item.level-p2 .level-icon {
  color: #E6A23C;
}

.alert-item.level-p3 .level-icon {
  color: #409EFF;
}

.alert-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.alert-message {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.alert-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

/* Chart Card */
.chart-card :deep(.ant-card-head) {
  padding: 16px 20px;
}

.echarts-container {
  width: 100%;
  height: 300px;
}

/* Resource Card */
.resource-card :deep(.ant-card-head) {
  padding: 16px 20px;
}

.resource-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.resource-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.resource-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
  font-size: 14px;
}

.resource-name {
  color: #303133;
}

.resource-value {
  color: #909399;
}

/* Responsive */
@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
  }
  
  .kpi-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .kpi-row {
    grid-template-columns: 1fr;
  }
  
  .service-grid {
    grid-template-columns: 1fr;
  }
}
</style>
