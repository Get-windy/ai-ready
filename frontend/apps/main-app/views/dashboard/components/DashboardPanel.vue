<template>
  <div 
    class="dashboard-panel"
    :class="[
      `panel-${panel.type}`,
      { 'panel-editing': isEditing },
      { 'panel-expanded': panel.config.expanded },
      `theme-${theme}`
    ]"
    :style="{
      gridColumn: `span ${panel.position.w}`,
      gridRow: `span ${panel.position.h}`,
      minHeight: `${gridConfig.minPanelHeight}px`
    }"
  >
    <!-- Panel Header -->
    <div class="panel-header">
      <div class="panel-title">
        <el-icon v-if="panelTypeIcon" :size="18">
          <component :is="panelTypeIcon" />
        </el-icon>
        <span>{{ panel.title }}</span>
      </div>
      <div class="panel-actions" v-if="isEditing">
        <el-dropdown @command="handleCommand">
          <el-button 
            :icon="More" 
            circle 
            size="small"
            type="primary"
          />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="edit">编辑</el-dropdown-item>
              <el-dropdown-item command="expand">展开/收起</el-dropdown-item>
              <el-dropdown-item command="remove" divided>删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- Panel Content -->
    <div 
      class="panel-content" 
      :class="{ 'content-expanded': panel.config.expanded }"
    >
      <component
        :is="panelComponent"
        :panel="panel"
        :theme="theme"
        @data-loaded="handleDataLoaded"
      />
    </div>

    <!-- Panel Footer -->
    <div class="panel-footer">
      <span class="panel-footer-left">
        <el-icon v-if="panel.config.lastUpdated" :title="`最后更新: ${panel.config.lastUpdated}`">
          <Timer />
        </el-icon>
        <span v-if="panel.config.lastUpdated" class="footer-text">
          {{ formatLastUpdated(panel.config.lastUpdated) }}
        </span>
      </span>
      <span class="panel-footer-right">
        <el-link 
          v-if="panel.config.autoRefresh" 
          type="success" 
          :icon="CircleCheck"
        >
          自动刷新
        </el-link>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, defineAsyncComponent } from 'vue';
import { More, Timer, CircleCheck } from '@element-plus/icons-vue';

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

// Props
interface Props {
  panel: Panel;
  isEditing: boolean;
  theme: 'light' | 'dark';
}

const props = withDefaults(defineProps<Props>(), {
  isEditing: false,
  theme: 'light'
});

// Emits
const emit = defineEmits<{
  (e: 'panelUpdated', panel: Panel): void;
  (e: 'panelRemoved', panelId: string): void;
  (e: 'panelExpanded', panelId: string, expanded: boolean): void;
}>();

// Grid config (from parent)
const gridConfig = defineModel<Record<string, any>>('gridConfig', {
  default: () => ({
    minPanelHeight: 250
  })
});

// Panel type configurations
const panelTypeConfigs: Record<string, { component: string; icon: string }> = {
  kpi: { component: 'KpiPanel', icon: 'TrendCharts' },
  chart: { component: 'ChartPanel', icon: 'BarChart' },
  alert: { component: 'AlertPanel', icon: 'AlertCircle' },
  status: { component: 'StatusPanel', icon: 'Server' },
  database: { component: 'DatabasePanel', icon: 'Database' },
  resource: { component: 'ResourcePanel', icon: 'Cpu' }
};

const panelTypeIcon = computed(() => {
  return panelTypeConfigs[props.panel.type]?.icon || 'Monitor';
});

// Dynamically import panel components
const KpiPanel = defineAsyncComponent(() => import('./KpiPanel.vue'));
const ChartPanel = defineAsyncComponent(() => import('./ChartPanel.vue'));
const AlertPanel = defineAsyncComponent(() => import('./AlertPanel.vue'));
const StatusPanel = defineAsyncComponent(() => import('./StatusPanel.vue'));
const DatabasePanel = defineAsyncComponent(() => import('./DatabasePanel.vue'));
const ResourcePanel = defineAsyncComponent(() => import('./ResourcePanel.vue'));

const panelComponent = computed(() => {
  return panelTypeConfigs[props.panel.type]?.component || 'KpiPanel';
});

// Methods
const handleCommand = (command: string) => {
  switch (command) {
    case 'edit':
      // Open edit dialog
      break;
    case 'expand':
      props.panel.config.expanded = !props.panel.config.expanded;
      emit('panelExpanded', props.panel.id, props.panel.config.expanded);
      break;
    case 'remove':
      // Confirm and remove
      break;
  }
};

const handleDataLoaded = (data: any) => {
  // Handle data loaded event
  console.log('Panel data loaded:', data);
};

const formatLastUpdated = (timestamp: string) => {
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
  return `${Math.floor(diff / 86400000)}天前`;
};
</script>

<style scoped>
.dashboard-panel {
  background: var(--panel-bg, var(--el-bg-color));
  border-radius: 8px;
  border: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  transition: all 0.3s;
  overflow: hidden;
}

.dashboard-panel:hover {
  border-color: var(--el-color-primary-light-7);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.dashboard-panel.panel-editing {
  border-color: var(--el-color-primary);
}

.dashboard-panel.panel-expanded {
  z-index: 10;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: var(--header-bg, var(--el-fill-color-light));
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.panel-title .el-icon {
  color: var(--el-color-primary);
}

.panel-actions .el-icon {
  color: var(--el-text-color-secondary);
}

.panel-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.panel-content.content-expanded {
  min-height: 300px;
}

.panel-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: var(--footer-bg, var(--el-fill-color-light));
  border-top: 1px solid var(--el-border-color-light);
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.panel-footer-left,
.panel-footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer-text {
  white-space: nowrap;
}

/* Theme variants */
:deep(.dashboard-panel.theme-dark) {
  --panel-bg: #1a1a2e;
  --header-bg: #16213e;
  --footer-bg: #0f3460;
}

:deep(.dashboard-panel.theme-light) {
  --panel-bg: #ffffff;
  --header-bg: #f5f7fa;
  --footer-bg: #f5f7fa;
}

/* Panel type specific styles */
:deep(.panel-kpi) {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}

:deep(.panel-chart) {
  height: 100%;
}

:deep(.panel-alert) {
  max-height: 400px;
}

:deep(.panel-status) {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .panel-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .panel-footer {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }
}
</style>
