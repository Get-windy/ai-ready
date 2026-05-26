// 审批流程可视化设计令牌系统
export const ApprovalDesignTokens = {
  // 色彩系统
  colors: {
    primary: {
      50: '#f0f9ff',
      100: '#e0f2fe',
      200: '#bae6fd',
      300: '#7dd3fc',
      400: '#38bdf8',
      500: '#0ea5e9', // 主色调
      600: '#0284c7',
      700: '#0369a1',
      800: '#075985',
      900: '#0c4a6e',
    },
    success: {
      50: '#f0fdf4',
      100: '#dcfce7',
      200: '#bbf7d0',
      300: '#86efac',
      400: '#4ade80',
      500: '#22c55e', // 通过状态
      600: '#16a34a',
      700: '#15803d',
      800: '#166534',
      900: '#14532d',
    },
    warning: {
      50: '#fffbeb',
      100: '#fef3c7',
      200: '#fde68a',
      300: '#fcd34d',
      400: '#fbbf24',
      500: '#f59e0b', // 待处理状态
      600: '#d97706',
      700: '#b45309',
      800: '#92400e',
      900: '#78350f',
    },
    danger: {
      50: '#fef2f2',
      100: '#fee2e2',
      200: '#fecaca',
      300: '#fca5a5',
      400: '#f87171',
      500: '#ef4444', // 拒绝状态
      600: '#dc2626',
      700: '#b91c1c',
      800: '#991b1b',
      900: '#7f1d1d',
    },
    neutral: {
      50: '#fafafa',
      100: '#f5f5f5',
      200: '#e5e5e5',
      300: '#d4d4d4',
      400: '#a3a3a3',
      500: '#737373', // 中性色
      600: '#525252',
      700: '#404040',
      800: '#262626',
      900: '#171717',
    },
    status: {
      pending: '#f59e0b',     // 待处理
      inProgress: '#0ea5e9',   // 审批中
      approved: '#22c55e',     // 已批准
      rejected: '#ef4444',     // 已拒绝
      executed: '#8b5cf6',     // 已执行
      cancelled: '#6b7280',    // 已取消
    },
  },

  // 间距系统
  spacing: {
    xs: '0.25rem',  // 4px
    sm: '0.5rem',   // 8px
    md: '1rem',     // 16px
    lg: '1.5rem',   // 24px
    xl: '2rem',     // 32px
    '2xl': '3rem',  // 48px
    '3xl': '4rem',  // 64px
  },

  // 圆角系统
  borderRadius: {
    none: '0',
    sm: '0.125rem',   // 2px
    md: '0.375rem',   // 6px
    lg: '0.5rem',     // 8px
    xl: '0.75rem',    // 12px
    full: '9999px',   // 圆形
  },

  // 字体系统
  typography: {
    fontFamily: {
      sans: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
      mono: "'JetBrains Mono', 'SF Mono', 'Roboto Mono', Consolas, 'Liberation Mono', Menlo, monospace",
    },
    fontSize: {
      xs: '0.75rem',     // 12px
      sm: '0.875rem',    // 14px
      base: '1rem',      // 16px
      lg: '1.125rem',    // 18px
      xl: '1.25rem',     // 20px
      '2xl': '1.5rem',   // 24px
      '3xl': '1.875rem', // 30px
      '4xl': '2.25rem',  // 36px
    },
    fontWeight: {
      normal: '400',
      medium: '500',
      semibold: '600',
      bold: '700',
    },
  },

  // 阴影系统
  shadows: {
    xs: '0 1px 2px 0 rgb(0 0 0 / 0.05)',
    sm: '0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)',
    md: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
    lg: '0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)',
    xl: '0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)',
  },

  // 动画系统
  animations: {
    durations: {
      fast: '150ms',
      normal: '300ms',
      slow: '500ms',
    },
    easings: {
      linear: 'linear',
      easeInOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
      easeOut: 'cubic-bezier(0, 0, 0.2, 1)',
      easeIn: 'cubic-bezier(0.4, 0, 1, 1)',
    },
  },

  // 响应式断点
  breakpoints: {
    sm: '640px',
    md: '768px',
    lg: '1024px',
    xl: '1280px',
    '2xl': '1536px',
  },

  // 节点尺寸
  nodeSizes: {
    small: {
      width: '80px',
      height: '80px',
    },
    medium: {
      width: '100px',
      height: '100px',
    },
    large: {
      width: '120px',
      height: '120px',
    },
  },

  // 连线样式
  connectionStyles: {
    normal: {
      stroke: '#94a3b8', // slate-400
      strokeWidth: '2',
      strokeDasharray: 'none',
    },
    active: {
      stroke: '#0ea5e9', // blue-500
      strokeWidth: '3',
      strokeDasharray: 'none',
    },
    approved: {
      stroke: '#22c55e', // green-500
      strokeWidth: '3',
      strokeDasharray: 'none',
    },
    rejected: {
      stroke: '#ef4444', // red-500
      strokeWidth: '3',
      strokeDasharray: '5,5',
    },
  },
} as const;

// 状态映射工具函数
export const getStatusColor = (status: string): string => {
  const statusMap: Record<string, string> = {
    pending: ApprovalDesignTokens.colors.status.pending,
    inProgress: ApprovalDesignTokens.colors.status.inProgress,
    approved: ApprovalDesignTokens.colors.status.approved,
    rejected: ApprovalDesignTokens.colors.status.rejected,
    executed: ApprovalDesignTokens.colors.status.executed,
    cancelled: ApprovalDesignTokens.colors.status.cancelled,
  };
  
  return statusMap[status] || ApprovalDesignTokens.colors.neutral[400];
};

// 状态显示文本映射
export const getStatusText = (status: string): string => {
  const textMap: Record<string, string> = {
    pending: '待提交',
    inProgress: '审批中',
    approved: '已批准',
    rejected: '已拒绝',
    executed: '已执行',
    cancelled: '已取消',
  };
  
  return textMap[status] || '未知状态';
};

// 状态图标映射
export const getStatusIcon = (status: string): string => {
  const iconMap: Record<string, string> = {
    pending: '⏳',
    inProgress: '🔄',
    approved: '✅',
    rejected: '❌',
    executed: '🚀',
    cancelled: '🚫',
  };
  
  return iconMap[status] || '❓';
};