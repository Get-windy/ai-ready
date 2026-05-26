/**
 * AI-Ready 主题系统
 * @package @ai-ready/themes
 */

// 主题配置接口
export interface IThemeConfig {
  primaryColor: string;
  secondaryColor: string;
  backgroundColor: string;
  textColor: string;
  borderRadius: string;
  spacing: {
    xs: string;
    sm: string;
    md: string;
    lg: string;
    xl: string;
  };
}

// 默认主题（浅色主题）
export const defaultTheme: IThemeConfig = {
  primaryColor: '#409eff',
  secondaryColor: '#67c23a',
  backgroundColor: '#f5f7fa',
  textColor: '#303133',
  borderRadius: '4px',
  spacing: {
    xs: '4px',
    sm: '8px',
    md: '16px',
    lg: '24px',
    xl: '32px'
  }
};

// 暗色主题
export const darkTheme: IThemeConfig = {
  primaryColor: '#409eff',
  secondaryColor: '#67c23a',
  backgroundColor: '#141414',
  textColor: '#e5e5e5',
  borderRadius: '4px',
  spacing: {
    xs: '4px',
    sm: '8px',
    md: '16px',
    lg: '24px',
    xl: '32px'
  }
};

// 业务主题配置
export const businessThemes = {
  finance: {
    primaryColor: '#1890ff', // 金融蓝
    secondaryColor: '#52c41a', // 成功绿
    backgroundColor: '#fafafa',
    textColor: '#262626'
  },
  monitoring: {
    primaryColor: '#f5222d', // 监控红
    secondaryColor: '#fa8c16', // 警告橙
    backgroundColor: '#fff2e8',
    textColor: '#262626'
  },
  order: {
    primaryColor: '#722ed1', // 订单紫
    secondaryColor: '#13c2c2', // 流程青
    backgroundColor: '#f9f0ff',
    textColor: '#262626'
  }
};

/**
 * 获取当前主题配置
 */
export function getCurrentTheme(themeName: string = 'default'): IThemeConfig {
  const themes: Record<string, IThemeConfig> = {
    default: defaultTheme,
    dark: darkTheme,
    ...businessThemes
  };
  
  return themes[themeName] || defaultTheme;
}

/**
 * 生成CSS变量
 */
export function generateCSSVariables(theme: IThemeConfig): string {
  return `
    :root {
      --primary-color: ${theme.primaryColor};
      --secondary-color: ${theme.secondaryColor};
      --background-color: ${theme.backgroundColor};
      --text-color: ${theme.textColor};
      --border-radius: ${theme.borderRadius};
      --spacing-xs: ${theme.spacing.xs};
      --spacing-sm: ${theme.spacing.sm};
      --spacing-md: ${theme.spacing.md};
      --spacing-lg: ${theme.spacing.lg};
      --spacing-xl: ${theme.spacing.xl};
    }
  `;
}