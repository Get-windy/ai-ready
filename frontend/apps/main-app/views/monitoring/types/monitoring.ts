// 基础设施状态类型
export interface InfrastructureStatus {
  id: string
  name: string
  type: 'server' | 'database' | 'cache' | 'network' | 'storage'
  status: 'healthy' | 'warning' | 'critical' | 'offline'
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
  networkIn: number
  networkOut: number
  uptime: number
  lastCheckTime: string
}

// 服务健康状态类型
export interface ServiceHealth {
  id: string
  name: string
  version: string
  status: 'up' | 'down' | 'degraded'
  instances: number
  healthyInstances: number
  responseTime: number
  errorRate: number
  throughput: number
  dependencies: string[]
  lastUpdated: string
}

// 性能指标类型
export interface PerformanceMetric {
  timestamp: string
  value: number
  unit: string
}

export interface PerformanceData {
  metricName: string
  metricType: 'cpu' | 'memory' | 'disk' | 'network' | 'response_time' | 'error_rate'
  data: PerformanceMetric[]
  currentValue: number
  trend: 'up' | 'down' | 'stable'
  threshold: {
    warning: number
    critical: number
  }
}

// 告警类型
export interface AlertItem {
  id: string
  title: string
  description: string
  severity: 'info' | 'warning' | 'error' | 'critical'
  source: string
  status: 'active' | 'acknowledged' | 'resolved' | 'silenced'
  createdAt: string
  updatedAt: string
  acknowledgedAt?: string
  resolvedAt?: string
  assignedTo?: string
  tags: string[]
}

// 监控统计类型
export interface MonitoringStats {
  totalServices: number
  healthyServices: number
  totalAlerts: number
  activeAlerts: number
  avgResponseTime: number
  systemUptime: number
  dataPoints: number
}

// API响应类型
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

// 查询参数类型
export interface TimeRangeParams {
  startTime: string
  endTime: string
  interval?: string
}

export interface AlertQueryParams extends TimeRangeParams {
  severity?: AlertItem['severity']
  status?: AlertItem['status']
  source?: string
  limit?: number
  offset?: number
}