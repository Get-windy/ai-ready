<template>
  <div class="monitoring-dashboard">
    <!-- Header -->
    <div class="dashboard-header">
      <div class="header-left">
        <h1 class="page-title">测试环境监控</h1>
        <p class="page-subtitle">
          最后更新: {{ lastUpdated }}
          <el-tag size="small" :type="connectionStatus.type">
            {{ connectionStatus.text }}
          </el-tag>
        </p>
      </div>
      <div class="header-right">
        <el-select v-model="selectedEnvironment" size="default" style="width: 140px">
          <el-option label="测试环境" value="test" />
          <el-option label="预发布环境" value="staging" />
          <el-option label="生产环境" value="production" />
        </el-select>
        <el-radio-group v-model="timeRange" size="default">
          <el-radio-button label="1h">1小时</el-radio-button>
          <el-radio-button label="6h">6小时</el-radio-button>
          <el-radio-button label="24h">24小时</el-radio-button>
        </el-radio-group>
        <el-button 
          :icon="Refresh" 
          circle
          :loading="isLoading"
          @click="handleRefresh"
        />
      </div>
    </div>

    <!-- KPI Cards -->
    <div class="kpi-section">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :md="6" v-for="kpi in kpiData" :key="kpi.id">
          <KpiCard :data="kpi" @click="handleKpiClick" />
        </el-col>
      </el-row>
    </div>

    <!-- Service Status -->
    <div class="section">
      <div class="section-header">
        <h2 class="section-title">
          <el-icon><Monitor /></el-icon>
          服务健康状态
        </h2>
        <el-link type="primary" @click="handleViewAllServices">
          查看全部
          <el-icon><ArrowRight /></el-icon>
        </el-link>
      </div>
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :md="6" v-for="service in services" :key="service.id">
          <ServiceStatusCard :service="service" @click="handleServiceClick" />
        </el-col>
      </el-row>
    </div>

    <!-- Charts & Alerts -->
    <el-row :gutter="16" class="charts-section">
      <el-col :xs="24" :lg="16">
        <PerformanceChart
          title="响应时间趋势"
          :series="responseTimeSeries"
          :loading="isLoading"
          unit="ms"
          :thresholds="{ warning: 500, danger: 1000 }"
          @refresh="handleChartRefresh"
        />
      </el-col>
      <el-col :xs="24" :lg="8">
        <AlertList
          :alerts="recentAlerts"
          :loading="isLoading"
          @acknowledge="handleAcknowledge"
          @acknowledgeAll="handleAcknowledgeAll"
          @viewAll="handleViewAllAlerts"
        />
      </el-col>
    </el-row>

    <!-- Throughput Chart -->
    <div class="section">
      <PerformanceChart
        title="吞吐量趋势"
        :series="throughputSeries"
        :loading="isLoading"
        chartType="area"
        unit=" req/s"
        @refresh="handleChartRefresh"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { Refresh, Monitor, ArrowRight } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import KpiCard from './components/KpiCard.vue';
import ServiceStatusCard from './components/ServiceStatusCard.vue';
import PerformanceChart from './components/PerformanceChart.vue';
import AlertList from './components/AlertList.vue';
import type { 
  KpiCardData, 
  ServiceStatus, 
  AlertItem, 
  MetricSeries,
  TimeRange 
} from './types/monitoring';

// State
const isLoading = ref(false);
const lastUpdated = ref('');
const selectedEnvironment = ref('test');
const timeRange = ref<TimeRange>('1h');
const connectionStatus = ref({ type: 'success' as const, text: '已连接' });

// Mock KPI Data
const kpiData = ref<KpiCardData[]>([
  {
    id: 'health',
    title: '系统健康度',
    value: 98.5,
    unit: '%',
    trend: 'up',
    trendValue: '+2.3%',
    status: 'success',
    icon: 'CircleCheck',
    sparklineData: [95, 96, 97, 96, 98, 98, 98.5],
    description: '所有核心服务正常运行'
  },
  {
    id: 'response',
    title: '平均响应时间',
    value: 245,
    unit: 'ms',
    trend: 'down',
    trendValue: '-12%',
    status: 'success',
    icon: 'Timer',
    sparklineData: [280, 270, 260, 255, 250, 248, 245],
    description: '响应速度良好'
  },
  {
    id: 'throughput',
    title: '吞吐量',
    value: 1250,
    unit: ' req/s',
    trend: 'up',
    trendValue: '+8.5%',
    status: 'success',
    icon: 'TrendCharts',
    sparklineData: [1100, 1120, 1150, 1180, 1200, 1220, 1250],
    description: '请求处理正常'
  },
  {
    id: 'error',
    title: '错误率',
    value: 0.12,
    unit: '%',
    trend: 'stable',
    trendValue: '0%',
    status: 'warning',
    icon: 'Warning',
    sparklineData: [0.15, 0.14, 0.13, 0.13, 0.12, 0.12, 0.12],
    description: '略高于阈值(0.1%)'
  }
]);

// Mock Service Data
const services = ref<ServiceStatus[]>([
  {
    id: 'api-gateway',
    name: 'API网关',
    type: 'api',
    status: 'running',
    uptime: '15天 3小时',
    lastCheck: new Date().toISOString(),
    latency: 45,
    connections: 1250,
    cpuUsage: 35,
    memoryUsage: 62
  },
  {
    id: 'database',
    name: '主数据库',
    type: 'database',
    status: 'running',
    uptime: '30天 12小时',
    lastCheck: new Date().toISOString(),
    latency: 12,
    connections: 245,
    cpuUsage: 45,
    memoryUsage: 78
  },
  {
    id: 'redis',
    name: 'Redis缓存',
    type: 'cache',
    status: 'warning',
    uptime: '15天 3小时',
    lastCheck: new Date().toISOString(),
    latency: 2,
    connections: 850,
    cpuUsage: 72,
    memoryUsage: 85,
    description: '内存使用率较高'
  },
  {
    id: 'kafka',
    name: '消息队列',
    type: 'queue',
    status: 'running',
    uptime: '20天 8小时',
    lastCheck: new Date().toISOString(),
    latency: 8,
    connections: 120,
    cpuUsage: 28,
    memoryUsage: 55
  }
]);

// Mock Alert Data
const alerts = ref<AlertItem[]>([
  {
    id: '1',
    level: 'P1',
    title: 'Redis内存使用率过高',
    message: 'Redis缓存内存使用率达到85%，建议扩容或清理缓存',
    source: 'Redis监控',
    timestamp: new Date(Date.now() - 5 * 60000).toISOString(),
    acknowledged: false
  },
  {
    id: '2',
    level: 'P2',
    title: 'API响应时间波动',
    message: '过去10分钟内API平均响应时间波动超过20%',
    source: 'API监控',
    timestamp: new Date(Date.now() - 15 * 60000).toISOString(),
    acknowledged: false
  },
  {
    id: '3',
    level: 'P3',
    title: '磁盘空间预警',
    message: '日志存储磁盘空间剩余15%，建议清理旧日志',
    source: '系统监控',
    timestamp: new Date(Date.now() - 2 * 3600000).toISOString(),
    acknowledged: true
  }
]);

const recentAlerts = computed(() => alerts.value.slice(0, 5));

// Mock Chart Data
const generateTimeSeriesData = (count: number, min: number, max: number): number[] => {
  return Array.from({ length: count }, () => 
    Math.floor(Math.random() * (max - min + 1)) + min
  );
};

const generateMetricSeries = (name: string, data: number[]): MetricSeries => {
  const now = Date.now();
  const interval = 5 * 60 * 1000; // 5 minutes
  
  return {
    name,
    data: data.map((value, index) => ({
      timestamp: new Date(now - (data.length - index) * interval).toISOString(),
      value
    }))
  };
};

const responseTimeSeries = ref<MetricSeries[]>([
  generateMetricSeries('API响应时间', generateTimeSeriesData(20, 150, 350))
]);

const throughputSeries = ref<MetricSeries[]>([
  generateMetricSeries('请求量', generateTimeSeriesData(20, 800, 1500))
]);

// Methods
const updateLastUpdated = () => {
  lastUpdated.value = new Date().toLocaleString('zh-CN');
};

const handleRefresh = async () => {
  isLoading.value = true;
  // Simulate API call
  await new Promise(resolve => setTimeout(resolve, 1000));
  
  // Update data
  responseTimeSeries.value = [
    generateMetricSeries('API响应时间', generateTimeSeriesData(20, 150, 350))
  ];
  throughputSeries.value = [
    generateMetricSeries('请求量', generateTimeSeriesData(20, 800, 1500))
  ];
  
  updateLastUpdated();
  isLoading.value = false;
  ElMessage.success('数据已刷新');
};

const handleKpiClick = (id: string) => {
  ElMessage.info(`查看KPI详情: ${id}`);
};

const handleServiceClick = (service: ServiceStatus) => {
  ElMessage.info(`查看服务详情: ${service.name}`);
};

const handleViewAllServices = () => {
  ElMessage.info('查看所有服务');
};

const handleChartRefresh = (range: TimeRange) => {
  ElMessage.info(`刷新图表数据: ${range}`);
  handleRefresh();
};

const handleAcknowledge = (id: string) => {
  const alert = alerts.value.find(a => a.id === id);
  if (alert) {
    alert.acknowledged = true;
    ElMessage.success('告警已确认');
  }
};

const handleAcknowledgeAll = () => {
  alerts.value.forEach(alert => {
    alert.acknowledged = true;
  });
  ElMessage.success('所有告警已确认');
};

const handleViewAllAlerts = () => {
  ElMessage.info('查看所有告警');
};

// Lifecycle
let refreshTimer: ReturnType<typeof setInterval> | null = null;

onMounted(() => {
  updateLastUpdated();
  
  // Auto refresh every 30 seconds
  refreshTimer = setInterval(() => {
    handleRefresh();
  }, 30000);
});

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer);
  }
});
</script>

<style scoped>
.monitoring-dashboard {
  padding: 24px;
  background: #F5F7FA;
  min-height: 100vh;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.page-subtitle {
  margin: 0;
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kpi-section {
  margin-bottom: 24px;
}

.section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title .el-icon {
  color: #409EFF;
}

.charts-section {
  margin-bottom: 24px;
}

.charts-section .el-col {
  margin-bottom: 16px;
}
</style>