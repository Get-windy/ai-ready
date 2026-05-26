/**
 * 告警规则配置类型定义
 */

// 告警规则状态
export type AlertRuleStatus = 'enabled' | 'disabled';

// 告警级别
export type AlertSeverity = 'critical' | 'warning' | 'info';

// 告警规则类型
export type AlertRuleType = 'threshold' | 'trend' | 'anomaly' | 'composite';

// 比较操作符
export type ComparisonOperator = '>' | '>=' | '<' | '<=' | '==' | '!=';

// 监控指标类型
export type MetricType = 
  | 'ORDER_COUNT' | 'ORDER_AMOUNT' | 'ORDER_SUCCESS_RATE' | 'ORDER_AVG_AMOUNT'
  | 'PAYMENT_COUNT' | 'PAYMENT_AMOUNT' | 'PAYMENT_SUCCESS_RATE' | 'PAYMENT_FAILURE_RATE'
  | 'INVENTORY_TOTAL' | 'INVENTORY_TURNOVER' | 'INVENTORY_WARNING_COUNT' | 'INVENTORY_ACCURACY'
  | 'CUSTOMER_ACTIVE' | 'CUSTOMER_NEW' | 'CUSTOMER_RETENTION' | 'CUSTOMER_SATISFACTION'
  | 'PRODUCT_TOTAL' | 'PRODUCT_VIEWS' | 'PRODUCT_CONVERSION_RATE' | 'PRODUCT_AVG_RATING'
  | 'CPU_USAGE' | 'MEMORY_USAGE' | 'DISK_USAGE' | 'API_RESPONSE_TIME' | 'API_ERROR_RATE';

// 通知渠道
export type NotificationChannel = 'wecom' | 'email' | 'sms' | 'webhook';

// 告警规则
export interface AlertRule {
  id: string;
  name: string;
  description: string;
  type: AlertRuleType;
  severity: AlertSeverity;
  status: AlertRuleStatus;
  
  // 指标配置
  metricType: MetricType;
  metricName: string;
  resourceId?: string;
  
  // 阈值配置
  operator: ComparisonOperator;
  threshold: number;
  unit?: string;
  
  // 时间配置
  duration: number; // 持续时间（分钟）
  timeRange: number; // 评估时间范围（分钟）
  
  // 通知配置
  notificationChannels: NotificationChannel[];
  notifyUsers?: string[];
  notifyGroups?: string[];
  
  // 抑制配置
  suppressInterval?: number; // 抑制间隔（分钟）
  suppressStartTime?: string;
  suppressEndTime?: string;
  
  // 元数据
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
  triggerCount: number;
  lastTriggeredAt?: string;
}

// 告警规则查询参数
export interface AlertRuleQueryParams {
  page?: number;
  pageSize?: number;
  keyword?: string;
  severity?: AlertSeverity;
  status?: AlertRuleStatus;
  metricType?: MetricType;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

// 告警规则列表响应
export interface AlertRuleListResponse {
  list: AlertRule[];
  total: number;
  page: number;
  pageSize: number;
}

// 告警规则表单数据（创建/编辑）
export interface AlertRuleFormData {
  id?: string;
  name: string;
  description: string;
  type: AlertRuleType;
  severity: AlertSeverity;
  status: AlertRuleStatus;
  metricType: MetricType;
  resourceId?: string;
  operator: ComparisonOperator;
  threshold: number;
  unit?: string;
  duration: number;
  timeRange: number;
  notificationChannels: NotificationChannel[];
  notifyUsers?: string[];
  notifyGroups?: string[];
  suppressInterval?: number;
}

// 告警规则统计
export interface AlertRuleStatistics {
  total: number;
  enabled: number;
  disabled: number;
  bySeverity: Record<AlertSeverity, number>;
  byType: Record<AlertRuleType, number>;
  triggeredToday: number;
  triggeredThisWeek: number;
}

// 告警规则模板
export interface AlertRuleTemplate {
  id: string;
  name: string;
  description: string;
  category: string;
  defaultRule: Partial<AlertRule>;
}

// 告警规则验证结果
export interface AlertRuleValidationResult {
  valid: boolean;
  errors: string[];
  warnings: string[];
}

// 告警规则测试参数
export interface AlertRuleTestParams {
  ruleId: string;
  testValue: number;
  testTime?: string;
}

// 告警规则测试结果
export interface AlertRuleTestResult {
  wouldTrigger: boolean;
  currentValue?: number;
  threshold: number;
  operator: ComparisonOperator;
  message: string;
}

// 指标选项
export interface MetricOption {
  value: MetricType;
  label: string;
  category: 'order' | 'payment' | 'inventory' | 'customer' | 'product' | 'system';
  unit: string;
  description: string;
}

// 操作符选项
export interface OperatorOption {
  value: ComparisonOperator;
  label: string;
  description: string;
}

// 通知渠道配置
export interface NotificationChannelConfig {
  channel: NotificationChannel;
  enabled: boolean;
  name: string;
  icon: string;
  description: string;
}
