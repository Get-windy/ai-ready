/**
 * Sprint 29 监控大盘模块 TypeScript 类型定义
 */

export interface Panel {
  id: string;
  type: string;
  title: string;
  config: Record<string, any>;
  position: {
    x: number;
    y: number;
    w: number;
    h: number;
  };
}

export interface PanelType {
  id: string;
  name: string;
  description: string;
  icon: string;
}

export interface GlobalSettings {
  autoRefresh: boolean;
  refreshInterval: number;
  defaultTimeRange: string;
}

export interface GridSettings {
  columns: number;
  gap: number;
  minPanelHeight: number;
}

export interface PanelConfig {
  autoRefresh: boolean;
  refreshInterval: number;
  defaultTimeRange: string;
  theme: 'light' | 'dark';
  gridColumns: number;
  gridGap: number;
}

export interface DashboardData {
  panels: Panel[];
  globalSettings: GlobalSettings;
  gridSettings: GridSettings;
  lastUpdated: string;
}

export interface KpiData {
  id: string;
  title: string;
  value: number | string;
  unit: string;
  trend: 'up' | 'down' | 'stable';
  trendValue: string;
  status: 'success' | 'warning' | 'danger' | 'normal';
  icon?: string;
  sparklineData?: number[];
  description?: string;
}

export interface ChartData {
  title: string;
  chartType: string;
  series: Array<{
    name: string;
    data: Array<{ timestamp: string; value: number }>;
  }>;
  xLabels: string[];
  options: Record<string, any>;
}

export interface AlertData {
  id: string;
  level: 'P0' | 'P1' | 'P2' | 'P3';
  title: string;
  message: string;
  source: string;
  timestamp: string;
  acknowledged: boolean;
}
