// 组件库主入口文件
import type { App } from 'vue';

// 基础组件
export { default as ARButton } from './base/button';
export { default as ARInput } from './base/input';
export { default as ARForm, ARFormItem, ARSelect } from './base/form';
export { default as ARTable, ARTableColumn } from './base/table';
export { default as ARDialog } from './base/dialog';

// 工具函数
export * from './utils';

// 样式
import './styles/index.scss';

// 组件库安装函数
export const install = (app: App, options = {}): void => {
  // 基础组件
  const components = import.meta.glob('./base/**/index.ts', { eager: true });
  for (const path in components) {
    const module = components[path] as any;
    if (module.default && module.default.install) {
      app.use(module.default);
    }
  }

  // 业务组件
  const businessComponents = import.meta.glob('./business/**/index.ts', { eager: true });
  for (const path in businessComponents) {
    const module = businessComponents[path] as any;
    if (module.default && module.default.install) {
      app.use(module.default);
    }
  }

  // 布局组件
  const layoutComponents = import.meta.glob('./layouts/**/index.ts', { eager: true });
  for (const path in layoutComponents) {
    const module = layoutComponents[path] as any;
    if (module.default && module.default.install) {
      app.use(module.default);
    }
  }
};

// 导出所有组件的类型
export * from './types';

export default {
  install,
  version: '0.2.0'
};