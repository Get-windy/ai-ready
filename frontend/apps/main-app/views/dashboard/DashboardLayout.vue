<template>
  <div 
    class="dashboard-layout"
    :class="{ 
      'is-dark': isDark,
      'high-contrast': a11yConfig.highContrast,
      'reduce-motion': a11yConfig.reduceMotion
    }"
    role="main"
    aria-label="系统监控大盘"
  >
    <!-- Skip Link for Accessibility -->
    <a href="#dashboard-content" class="skip-link">跳转到主要内容</a>

    <!-- Dashboard Header -->
    <header class="dashboard-header" role="banner">
      <div class="header-left">
        <h1 class="page-title" id="dashboard-title">系统监控大盘</h1>
        <p class="page-subtitle" aria-live="polite">
          {{ lastUpdated }}
          <el-link 
            v-if="connectionStatus === 'connected'" 
            type="success" 
            :icon="CircleCheck"
            aria-label="连接状态：已连接"
          >
            已连接
          </el-link>
          <el-link 
            v-else 
            type="warning" 
            :icon="Warning"
            aria-label="连接状态：异常"
          >
            连接异常
          </el-link>
        </p>
      </div>
      <div class="header-right">
        <div class="header-actions">
          <!-- Theme Toggle -->
          <ThemeToggle />

          <!-- Search Button -->
          <el-tooltip content="搜索 (Ctrl+K)" placement="bottom">
            <el-button 
              :icon="Search" 
              circle
              @click="showSearch = true"
              aria-label="打开搜索"
            />
          </el-tooltip>

          <!-- Refresh Button -->
          <el-tooltip content="刷新 (R)" placement="bottom">
            <el-button 
              :icon="Refresh" 
              circle
              :loading="isLoading"
              @click="handleRefresh"
              aria-label="刷新数据"
            />
          </el-tooltip>
          
          <!-- Export Dropdown -->
          <el-dropdown @command="handleExport" aria-label="导出选项">
            <el-button type="primary" :icon="Download">
              导出报表
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu role="menu">
                <el-dropdown-item command="export png" role="menuitem">导出为PNG</el-dropdown-item>
                <el-dropdown-item command="export pdf" role="menuitem">导出为PDF</el-dropdown-item>
                <el-dropdown-item command="export csv" role="menuitem">导出为CSV</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- Accessibility Settings -->
          <el-tooltip content="无障碍设置" placement="bottom">
            <el-button 
              :icon="View" 
              circle
              @click="showA11ySettings = true"
              aria-label="无障碍设置"
            />
          </el-tooltip>
          
          <!-- Personalization -->
          <el-tooltip content="个性化配置" placement="bottom">
            <el-button 
              :icon="User"
              circle
              @click="showPersonalization = true"
              aria-label="个性化配置"
            />
          </el-tooltip>
          
          <!-- Configure Panel -->
          <el-button 
            type="info" 
            :icon="Setting"
            @click="handleConfigure"
          >
            配置面板
          </el-button>

          <!-- Keyboard Shortcuts Help -->
          <el-tooltip content="快捷键帮助 (Shift+?)" placement="bottom">
            <el-button 
              :icon="QuestionFilled"
              circle
              @click="showShortcutsHelp = true"
              aria-label="快捷键帮助"
            />
          </el-tooltip>
        </div>
      </div>
    </header>

    <!-- Dashboard Content -->
    <main 
      id="dashboard-content" 
      class="dashboard-grid" 
      ref="gridRef"
      @drop="handleDrop"
      @dragover="handleDragOver"
      role="region"
      aria-label="仪表板面板区域"
    >
      <draggable 
        v-model="panels" 
        item-key="id"
        @end="handleDragEnd"
        :disabled="!isEditing"
        role="list"
        aria-label="面板列表"
      >
        <template #item="{ element }">
          <DashboardPanel
            :panel="element"
            :is-editing="isEditing"
            :theme="currentTheme"
            @panel-updated="handlePanelUpdated"
            @panel-removed="handlePanelRemoved"
            @panel-expanded="handlePanelExpanded"
            role="listitem"
          />
        </template>
      </draggable>
    </main>

    <!-- Configure Dialog -->
    <el-dialog
      v-model="configDialogVisible"
      title="配置面板"
      width="800px"
      :destroy-on-close="true"
      :close-on-press-escape="true"
      aria-labelledby="config-title"
    >
      <div class="configure-content">
        <el-tabs v-model="configTab" aria-label="配置选项">
          <el-tab-pane label="添加面板" name="add">
            <h3 id="config-title">添加新面板</h3>
            <div class="panel-catalog" role="list">
              <el-card 
                v-for="panelType in availablePanelTypes" 
                :key="panelType.id"
                class="panel-card"
                shadow="hover"
                role="listitem"
              >
                <div class="panel-card-header">
                  <el-icon :size="24"><component :is="panelType.icon" /></el-icon>
                  <span class="panel-type-name">{{ panelType.name }}</span>
                </div>
                <div class="panel-card-body">
                  <p class="panel-description">{{ panelType.description }}</p>
                </div>
                <div class="panel-card-footer">
                  <el-button 
                    type="primary" 
                    size="small"
                    @click="addPanel(panelType.id)"
                  >
                    添加
                  </el-button>
                </div>
              </el-card>
            </div>
          </el-tab-pane>
          
          <el-tab-pane label="全局设置" name="settings">
            <el-form :model="globalSettings" label-width="120px">
              <el-form-item label="自动刷新">
                <el-switch 
                  v-model="globalSettings.autoRefresh"
                  aria-label="启用自动刷新"
                />
              </el-form-item>
              <el-form-item label="刷新间隔">
                <el-slider 
                  v-model="globalSettings.refreshInterval" 
                  :min="5" 
                  :max="300" 
                  :step="5"
                  aria-label="刷新间隔"
                />
              </el-form-item>
              <el-form-item label="时间范围">
                <el-select 
                  v-model="globalSettings.defaultTimeRange" 
                  style="width: 200px"
                  aria-label="默认时间范围"
                >
                  <el-option label="最近1小时" value="1h" />
                  <el-option label="最近6小时" value="6h" />
                  <el-option label="最近24小时" value="24h" />
                  <el-option label="最近7天" value="7d" />
                </el-select>
              </el-form-item>
            </el-form>
          </el-tab-pane>
          
          <el-tab-pane label="布局设置" name="layout">
            <el-form :model="gridSettings" label-width="120px">
              <el-form-item label="列数">
                <el-slider 
                  v-model="gridSettings.columns" 
                  :min="1" 
                  :max="12" 
                  :step="1"
                  aria-label="网格列数"
                />
              </el-form-item>
              <el-form-item label="间距">
                <el-slider 
                  v-model="gridSettings.gap" 
                  :min="8" 
                  :max="32" 
                  :step="4"
                  aria-label="面板间距"
                />
              </el-form-item>
              <el-form-item label="最小高度">
                <el-slider 
                  v-model="gridSettings.minPanelHeight" 
                  :min="200" 
                  :max="500" 
                  :step="10"
                  aria-label="面板最小高度"
                />
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveConfig">保存</el-button>
      </template>
    </el-dialog>

    <!-- Search Dialog -->
    <SearchDialog
      v-model="showSearch"
      @select-panel="handleSelectPanel"
      @refresh="handleRefresh"
      @open-settings="handleConfigure"
      @export="handleExport"
    />

    <!-- Keyboard Shortcuts Help -->
    <KeyboardShortcutsHelp v-model="showShortcutsHelp" />

    <!-- Accessibility Settings -->
    <AccessibilitySettings v-model="showA11ySettings" />

    <!-- Personalization Panel -->
    <PersonalizationPanel v-model="showPersonalization" />

    <!-- Screen Reader Announcer -->
    <div 
      id="sr-announcer" 
      aria-live="polite" 
      aria-atomic="true"
      class="sr-only"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue';
import { 
  Refresh, 
  CircleCheck, 
  Warning, 
  Download, 
  ArrowDown, 
  Setting,
  Search,
  View,
  User,
  QuestionFilled,
  Monitor,
  TrendCharts,
  BarChart,
  AlertCircle,
  Server,
  Database,
  Cpu
} from '@element-plus/icons-vue';
import Draggable from 'vuedraggable';
import DashboardPanel from './components/DashboardPanel.vue';
import ThemeToggle from './components/ThemeToggle.vue';
import SearchDialog from './components/SearchDialog.vue';
import KeyboardShortcutsHelp from './components/KeyboardShortcutsHelp.vue';
import AccessibilitySettings from './components/AccessibilitySettings.vue';
import PersonalizationPanel from './components/PersonalizationPanel.vue';
import { useTheme } from './composables/useTheme';
import { useAccessibility } from './composables/useAccessibility';
import { useKeyboardShortcuts, createCommonShortcuts } from './composables/useKeyboardShortcuts';
import { ElMessage } from 'element-plus';

// Types
interface Panel {
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

interface PanelType {
  id: string;
  name: string;
  description: string;
  icon: string;
}

interface GlobalSettings {
  autoRefresh: boolean;
  refreshInterval: number;
  defaultTimeRange: string;
}

interface GridSettings {
  columns: number;
  gap: number;
  minPanelHeight: number;
}

// Composables
const { isDark, toggleTheme } = useTheme();
const { config: a11yConfig, announce } = useAccessibility();
const { registerShortcut } = useKeyboardShortcuts();

// State
const isLoading = ref(false);
const lastUpdated = ref('');
const connectionStatus = ref('connected');
const isEditing = ref(false);
const configDialogVisible = ref(false);
const configTab = ref('add');
const gridRef = ref<HTMLElement | null>(null);
const showSearch = ref(false);
const showShortcutsHelp = ref(false);
const showA11ySettings = ref(false);
const showPersonalization = ref(false);

const currentTheme = computed(() => isDark.value ? 'dark' : 'light');

// Default panels
const defaultPanels: Panel[] = [
  {
    id: 'p1',
    type: 'kpi',
    title: '系统健康度',
    config: { metric: 'health', showTrend: true },
    position: { x: 0, y: 0, w: 3, h: 2 }
  },
  {
    id: 'p2',
    type: 'kpi',
    title: '平均响应时间',
    config: { metric: 'response_time', showTrend: true },
    position: { x: 3, y: 0, w: 3, h: 2 }
  },
  {
    id: 'p3',
    type: 'kpi',
    title: '当前吞吐量',
    config: { metric: 'throughput', showTrend: true },
    position: { x: 6, y: 0, w: 3, h: 2 }
  },
  {
    id: 'p4',
    type: 'kpi',
    title: '错误率',
    config: { metric: 'error_rate', showTrend: true },
    position: { x: 9, y: 0, w: 3, h: 2 }
  },
  {
    id: 'p5',
    type: 'chart',
    title: 'CPU使用率',
    config: { metric: 'cpu', chartType: 'area' },
    position: { x: 0, y: 2, w: 6, h: 4 }
  },
  {
    id: 'p6',
    type: 'chart',
    title: '内存使用情况',
    config: { metric: 'memory', chartType: 'line' },
    position: { x: 6, y: 2, w: 6, h: 4 }
  },
  {
    id: 'p7',
    type: 'chart',
    title: '每天请求数',
    config: { metric: 'requests', chartType: 'bar' },
    position: { x: 0, y: 6, w: 8, h: 4 }
  },
  {
    id: 'p8',
    type: 'alert',
    title: '最近告警',
    config: { limit: 10, levels: ['P1', 'P2', 'P3'] },
    position: { x: 8, y: 6, w: 4, h: 4 }
  }
];

const panels = ref<Panel[]>([...defaultPanels]);

// Available panel types
const availablePanelTypes: PanelType[] = [
  { 
    id: 'kpi', 
    name: 'KPI指标卡', 
    description: '展示关键业务指标，支持趋势显示',
    icon: 'TrendCharts'
  },
  { 
    id: 'chart', 
    name: '数据图表', 
    description: '线图、面积图、柱状图等多种图表类型',
    icon: 'BarChart'
  },
  { 
    id: 'alert', 
    name: '告警列表', 
    description: '展示实时告警信息，支持级别过滤',
    icon: 'AlertCircle'
  },
  { 
    id: 'status', 
    name: '服务状态', 
    description: '展示各服务运行状态和健康度',
    icon: 'Server'
  },
  { 
    id: 'database', 
    name: '数据库监控', 
    description: '数据库连接数、查询性能等指标',
    icon: 'Database'
  },
  { 
    id: 'resource', 
    name: '资源监控', 
    description: 'CPU、内存、磁盘、网络等系统资源',
    icon: 'Cpu'
  }
];

// Global settings
const globalSettings = reactive<GlobalSettings>({
  autoRefresh: true,
  refreshInterval: 30,
  defaultTimeRange: '1h'
});

const gridSettings = reactive<GridSettings>({
  columns: 12,
  gap: 16,
  minPanelHeight: 250
});

// Methods
const updateLastUpdated = () => {
  lastUpdated.value = new Date().toLocaleString('zh-CN');
};

const handleRefresh = async () => {
  isLoading.value = true;
  announce('正在刷新数据...', 'polite');
  
  // Simulate API calls
  await new Promise(resolve => setTimeout(resolve, 1000));
  
  // Update panel data
  panels.value.forEach(panel => {
    panel.config.lastUpdated = new Date().toISOString();
  });
  
  updateLastUpdated();
  isLoading.value = false;
  ElMessage.success('数据已刷新');
  announce('数据刷新完成', 'polite');
};

const handleExport = (command: string) => {
  ElMessage.info(`导出功能: ${command}`);
  announce(`正在${command}`, 'polite');
};

const handleConfigure = () => {
  configDialogVisible.value = true;
};

const handlePanelUpdated = (panel: Panel) => {
  const index = panels.value.findIndex(p => p.id === panel.id);
  if (index !== -1) {
    panels.value[index] = panel;
  }
};

const handlePanelRemoved = (panelId: string) => {
  const index = panels.value.findIndex(p => p.id === panelId);
  if (index !== -1) {
    panels.value.splice(index, 1);
    announce('面板已删除', 'polite');
  }
};

const handlePanelExpanded = (panelId: string, expanded: boolean) => {
  const panel = panels.value.find(p => p.id === panelId);
  if (panel) {
    panel.config.expanded = expanded;
  }
};

const addPanel = (type: string) => {
  const panelType = availablePanelTypes.find(t => t.id === type);
  const panel: Panel = {
    id: `p${Date.now()}`,
    type,
    title: panelType?.name || '新面板',
    config: {},
    position: { x: 0, y: 0, w: 4, h: 3 }
  };
  panels.value.push(panel);
  announce(`已添加${panel.title}面板`, 'polite');
  ElMessage.success('面板已添加');
};

const handleDragEnd = (event: any) => {
  console.log('Drag end:', event);
  announce('面板顺序已更新', 'polite');
};

const handleDragOver = (event: DragEvent) => {
  event.preventDefault();
};

const handleDrop = (event: DragEvent) => {
  event.preventDefault();
};

const saveConfig = () => {
  // Save configuration
  localStorage.setItem('dashboard_config', JSON.stringify({
    panels: panels.value,
    globalSettings,
    gridSettings
  }));
  
  configDialogVisible.value = false;
  ElMessage.success('配置已保存');
  announce('配置已保存', 'polite');
};

const handleSelectPanel = (panelId: string) => {
  // Scroll to panel or highlight it
  ElMessage.info(`已选择面板: ${panelId}`);
};

// Auto refresh timer
let refreshTimer: ReturnType<typeof setInterval> | null = null;

onMounted(() => {
  updateLastUpdated();
  
  // Check connection status
  checkConnection();
  
  // Load saved configuration
  loadConfig();
  
  // Start auto refresh if enabled
  if (globalSettings.autoRefresh) {
    refreshTimer = setInterval(() => {
      handleRefresh();
    }, globalSettings.refreshInterval * 1000);
  }

  // Register keyboard shortcuts
  const shortcuts = createCommonShortcuts({
    onRefresh: handleRefresh,
    onSearch: () => { showSearch.value = true; },
    onClose: () => {
      showSearch.value = false;
      showShortcutsHelp.value = false;
      showA11ySettings.value = false;
      showPersonalization.value = false;
      configDialogVisible.value = false;
    },
    onHelp: () => { showShortcutsHelp.value = true; }
  });
  
  shortcuts.forEach(registerShortcut);
});

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer);
  }
});

const checkConnection = async () => {
  try {
    // Simulate connection check
    await new Promise(resolve => setTimeout(resolve, 500));
    connectionStatus.value = 'connected';
  } catch {
    connectionStatus.value = 'disconnected';
  }
};

const loadConfig = () => {
  try {
    const saved = localStorage.getItem('dashboard_config');
    if (saved) {
      const config = JSON.parse(saved);
      if (config.panels) panels.value = config.panels;
      if (config.globalSettings) Object.assign(globalSettings, config.globalSettings);
      if (config.gridSettings) Object.assign(gridSettings, config.gridSettings);
    }
  } catch (e) {
    console.error('Failed to load dashboard config:', e);
  }
};
</script>

<style scoped>
.dashboard-layout {
  min-height: 100vh;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  transition: background 0.3s, color 0.3s;
}

/* Skip Link */
.skip-link {
  position: absolute;
  top: -40px;
  left: 0;
  background: #000;
  color: #fff;
  padding: 8px 16px;
  z-index: 10000;
  transition: top 0.3s;
  text-decoration: none;
}

.skip-link:focus {
  top: 0;
}

/* Screen Reader Only */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px;
  background: var(--el-bg-color-overlay);
  border-bottom: 1px solid var(--el-border-color-light);
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
  color: var(--el-text-color-primary);
}

.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-right {
  display: flex;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(var(--columns, 12), 1fr);
  gap: var(--gap, 16px);
  padding: 24px;
  min-height: calc(100vh - 180px);
}

.configure-content {
  max-height: 60vh;
  overflow-y: auto;
}

.panel-catalog {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}

.panel-card {
  transition: transform 0.2s, box-shadow 0.2s;
}

.panel-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.panel-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-card-header .el-icon {
  color: var(--el-color-primary);
}

.panel-type-name {
  font-weight: 500;
  font-size: 16px;
}

.panel-description {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.panel-card-footer {
  margin-top: 16px;
  text-align: right;
}

/* Dark theme support */
.dashboard-layout.is-dark {
  --el-bg-color: #1a1a2e;
  --el-bg-color-overlay: #16213e;
  --el-text-color-primary: #e8e8e8;
  --el-text-color-secondary: #b0b0b0;
  --el-border-color-light: #434343;
  --el-fill-color-light: #0f3460;
}

/* High contrast mode */
.dashboard-layout.high-contrast {
  --el-border-color: #000;
  --el-border-color-light: #000;
}

.dashboard-layout.high-contrast .dashboard-header {
  border-bottom-width: 2px;
}

.dashboard-layout.high-contrast .el-button {
  border-width: 2px;
}

/* Reduced motion */
.dashboard-layout.reduce-motion * {
  animation-duration: 0.01ms !important;
  animation-iteration-count: 1 !important;
  transition-duration: 0.01ms !important;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .dashboard-header {
    flex-direction: column;
    gap: 16px;
  }
  
  .header-actions {
    justify-content: flex-start;
  }
  
  .dashboard-grid {
    grid-template-columns: 1fr;
    padding: 16px;
  }
}

/* Focus styles for accessibility */
:deep(*:focus-visible) {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

/* Print styles */
@media print {
  .dashboard-header {
    position: static;
  }
  
  .header-actions {
    display: none;
  }
}
</style>