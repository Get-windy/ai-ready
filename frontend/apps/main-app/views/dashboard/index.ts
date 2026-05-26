/**
 * 监控大盘模块 - 主入口文件
 */

import { createApp } from 'vue';
import DashboardLayout from './DashboardLayout.vue';

// Create dashboard app
const createDashboard = (containerId: string) => {
  const app = createApp(DashboardLayout);
  
  // Register global components
  // ... (component registration code)
  
  // Mount
  const container = document.getElementById(containerId);
  if (container) {
    app.mount(container);
  }
  
  return app;
};

// Export
export { createDashboard };

// Auto-mount if container exists
if (typeof window !== 'undefined') {
  const container = document.getElementById('dashboard-app');
  if (container) {
    createDashboard('dashboard-app');
  }
}
