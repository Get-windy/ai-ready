# AI-Ready 前端设计规范

> 统一的设计语言和组件库规范
> 设计师: ui-mnj0fukd
> 创建日期: 2026-04-27
> 版本: v1.0
> 任务ID: task_1777235384035_tuog7b6y9

---

## 目录

1. [设计原则](#1-设计原则)
2. [色彩规范](#2-色彩规范)
3. [字体规范](#3-字体规范)
4. [布局与间距](#4-布局与间距)
5. [组件规范](#5-组件规范)
6. [交互规范](#6-交互规范)
7. [响应式规范](#7-响应式规范)
8. [无障碍规范](#8-无障碍规范)

---

## 1. 设计原则

### 1.1 核心原则

- **一致性**：所有界面保持统一的视觉风格和交互模式
- **易用性**：简化操作流程，降低学习成本
- **美观性**：现代简洁的设计风格，注重细节
- **可访问性**：支持键盘导航、屏幕阅读器等辅助功能
- **性能优先**：轻量级实现，快速响应

### 1.2 设计语言

- **现代简约**：采用扁平化设计，避免过度装饰
- **科技感**：蓝色主色调，传达专业和可靠
- **清晰的层次**：通过色彩、大小、间距建立视觉层次
- **反馈及时**：每个操作都有明确的视觉反馈

---

## 2. 色彩规范

### 2.1 主色调

```css
/* 科技蓝 - 品牌主色 */
--primary: #1890FF;
--primary-light: #40A9FF;
--primary-lighter: #69C0FF;
--primary-dark: #096DD9;
--primary-darker: #0050B3;
--primary-bg: rgba(24, 144, 255, 0.1);
```

### 2.2 功能色

```css
/* 成功 - 绿色 */
--success: #52C41A;
--success-light: #95DE64;
--success-dark: #389E0D;
--success-bg: rgba(82, 196, 26, 0.1);

/* 警告 - 黄色 */
--warning: #FAAD14;
--warning-light: #FFD666;
--warning-dark: #D48806;
--warning-bg: rgba(250, 173, 20, 0.1);

/* 错误 - 红色 */
--error: #F5222D;
--error-light: #FF7875;
--error-dark: #CF1322;
--error-bg: rgba(245, 34, 45, 0.1);

/* 信息 - 蓝色 */
--info: #1890FF;
--info-light: #69C0FF;
--info-dark: #0050B3;
--info-bg: rgba(24, 144, 255, 0.1);
```

### 2.3 中性色

```css
/* 文本色 */
--text-primary: #262626;      /* 主要文本 */
--text-regular: #595959;      /* 常规文本 */
--text-secondary: #8C8C8C;     /* 次要文本 */
--text-placeholder: #BFBFBF;   /* 占位符文本 */
--text-disabled: #D9D9D9;     /* 禁用文本 */

/* 边框色 */
--border-base: #D9D9D9;        /* 基础边框 */
--border-light: #F0F0F0;       /* 浅色边框 */
--border-lighter: #FAFAFA;     /* 更浅边框 */
--border-dark: #BFBFBF;        /* 深色边框 */

/* 背景色 */
--bg-base: #FFFFFF;            /* 基础背景 */
--bg-page: #F5F5F5;            /* 页面背景 */
--bg-component: #FAFAFA;       /* 组件背景 */
--bg-component-light: #F5F5F5; /* 组件浅色背景 */

/* 阴影 */
--shadow-1: 0 1px 2px 0 rgba(0, 0, 0, 0.03);
--shadow-2: 0 1px 2px -1px rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.06);
--shadow-3: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
--shadow-4: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
```

### 2.4 深色模式

```css
[data-theme='dark'] {
  /* 主色调 */
  --primary: #177DDC;
  --primary-light: #1765AD;
  --primary-dark: #3C9AE8;
  --primary-bg: rgba(23, 125, 220, 0.15);

  /* 文本色 */
  --text-primary: #E8E8E8;
  --text-regular: #BFBFBF;
  --text-secondary: #8C8C8C;
  --text-placeholder: #595959;
  --text-disabled: #434343;

  /* 边框色 */
  --border-base: #434343;
  --border-light: #303030;
  --border-lighter: #262626;
  --border-dark: #595959;

  /* 背景色 */
  --bg-base: #1F1F1F;
  --bg-page: #141414;
  --bg-component: #1F1F1F;
  --bg-component-light: #262626;
}
```

---

## 3. 字体规范

### 3.1 字体族

```css
--font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
               'Helvetica Neue', Arial, 'Noto Sans', sans-serif,
               'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol',
               'Noto Color Emoji';

--font-family-code: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo,
                     Courier, monospace;
```

### 3.2 字号体系

| 用途 | 字号 | 行高 | 字重 | 使用场景 |
|------|------|------|------|----------|
| 大标题 | 30px | 38px | 600 | 页面主标题 |
| 中标题 | 24px | 32px | 600 | 模块标题 |
| 小标题 | 20px | 28px | 600 | 子模块标题 |
| 正文大 | 16px | 24px | 400 | 正文内容 |
| 正文 | 14px | 22px | 400 | 正文内容 |
| 正文小 | 12px | 20px | 400 | 辅助文本 |

```css
--font-size-xl: 30px;
--font-size-lg: 24px;
--font-size-md: 20px;
--font-size-base: 16px;
--font-size-sm: 14px;
--font-size-xs: 12px;

--line-height-xl: 38px;
--line-height-lg: 32px;
--line-height-md: 28px;
--line-height-base: 24px;
--line-height-sm: 22px;
--line-height-xs: 20px;
```

### 3.3 字重

```css
--font-weight-light: 300;
--font-weight-regular: 400;
--font-weight-medium: 500;
--font-weight-semibold: 600;
--font-weight-bold: 700;
```

### 3.4 字间距

```css
--letter-spacing-tight: -0.02em;
--letter-spacing-normal: 0;
--letter-spacing-wide: 0.02em;
--letter-spacing-wider: 0.05em;
```

---

## 4. 布局与间距

### 4.1 间距系统

采用 4px 基准的 8 点网格系统：

| 变量 | 值 | 使用场景 |
|------|-----|----------|
| --space-0 | 0 | 无间距 |
| --space-1 | 4px | 极小间距 |
| --space-2 | 8px | 小间距 |
| --space-3 | 12px | 较小间距 |
| --space-4 | 16px | 基础间距 |
| --space-5 | 20px | 中间距 |
| --space-6 | 24px | 中大间距 |
| --space-7 | 28px | 较大间距 |
| --space-8 | 32px | 大间距 |
| --space-10 | 40px | 超大间距 |
| --space-12 | 48px | 特大间距 |
| --space-16 | 64px | 极大间距 |

### 4.2 圆角规范

```css
--radius-xs: 2px;
--radius-sm: 4px;
--radius-base: 6px;
--radius-md: 8px;
--radius-lg: 12px;
--radius-xl: 16px;
--radius-full: 9999px;
```

### 4.3 页面布局

```
┌─────────────────────────────────────────────────────┐
│  Header (固定高度: 64px)                            │
├──────────┬──────────────────────────────────────────┤
│          │                                          │
│ Sidebar  │         Main Content                     │
│ (可折叠) │  (最小宽度: 1024px)                      │
│ 240px    │                                          │
│          │                                          │
├──────────┴──────────────────────────────────────────┤
│  Footer (固定高度: 48px)                            │
└─────────────────────────────────────────────────────┘
```

### 4.4 容器宽度

| 断点 | 容器最大宽度 |
|------|------------|
| xs | 100% |
| sm | 540px |
| md | 720px |
| lg | 960px |
| xl | 1140px |
| xxl | 1320px |

---

## 5. 组件规范

### 5.1 Button 按钮组件

#### 尺寸

| 尺寸 | 高度 | 内边距 | 字号 |
|------|------|--------|------|
| large | 40px | 12px 24px | 16px |
| default | 32px | 8px 16px | 14px |
| small | 24px | 4px 12px | 12px |

#### 类型

| 类型 | 背景色 | 文字色 | 边框 |
|------|--------|--------|------|
| primary | #1890FF | #FFFFFF | 无 |
| default | #FFFFFF | #262626 | #D9D9D9 |
| dashed | #FFFFFF | #262626 | dashed #D9D9D9 |
| text | transparent | #1890FF | 无 |
| link | transparent | #1890FF | 无 |

#### 状态

- **禁用**: opacity: 0.6, cursor: not-allowed
- **加载**: 显示 loading 图标，禁用点击
- **聚焦**: 0 0 0 2px rgba(24, 144, 255, 0.2)

### 5.2 Input 输入框组件

#### 尺寸

| 尺寸 | 高度 | 字号 |
|------|------|------|
| large | 40px | 16px |
| default | 32px | 14px |
| small | 24px | 12px |

#### 状态

| 状态 | 边框色 | 背景 |
|------|--------|------|
| 默认 | #D9D9D9 | #FFFFFF |
| 聚焦 | #1890FF | #FFFFFF |
| 错误 | #F5222D | #FFFFFF |
| 禁用 | #D9D9D9 | #F5F5F5 |
| 只读 | #D9D9D9 | #F5F5F5 |

#### 前后缀

- 前缀图标: padding-left: 8px
- 后缀图标: padding-right: 8px
- 清空按钮: 鼠标悬浮显示

### 5.3 Card 卡片组件

#### 基础样式

```css
.card {
  background: #FFFFFF;
  border: 1px solid #F0F0F0;
  border-radius: 8px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.03);
  padding: 24px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 16px;
}

.card-body {
  font-size: 14px;
  color: #595959;
  line-height: 22px;
}
```

#### 变体

| 变体 | 描述 |
|------|------|
| bordered | 带边框 |
| shadowed | 带阴影 |
| hoverable | 悬浮效果 |

### 5.4 Form 表单组件

#### 布局

| 模式 | 描述 |
|------|------|
| horizontal | 水平布局，标签在左 |
| vertical | 垂直布局，标签在上 |
| inline | 行内布局 |

#### 标签

| 尺寸 | 宽度 | 文字右对齐 |
|------|------|-----------|
| large | 120px | 是 |
| default | 100px | 是 |
| small | 80px | 是 |

#### 验证

- 实时验证: 失焦时触发
- 错误提示: 红色文字，图标提示
- 必填标识: 红色星号

---

## 6. 交互规范

### 6.1 动画时长

| 类型 | 时长 |
|------|------|
| 极快 | 100ms |
| 快 | 200ms |
| 正常 | 300ms |
| 慢 | 500ms |
| 极慢 | 1000ms |

### 6.2 缓动函数

```css
/* 线性 */
--ease-linear: linear;

/* 标准缓动 */
--ease-in: cubic-bezier(0.4, 0, 1, 1);
--ease-out: cubic-bezier(0, 0, 0.2, 1);
--ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);

/* 自定义缓动 */
--ease-elastic: cubic-bezier(0.68, -0.55, 0.265, 1.55);
--ease-back: cubic-bezier(0.68, -0.6, 0.32, 1.6);
```

### 6.3 悬浮状态

```css
:hover {
  opacity: 0.85;
  transition: opacity 200ms;
}

:active {
  opacity: 0.7;
  transition: opacity 100ms;
}
```

### 6.4 聚焦状态

```css
:focus {
  outline: none;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}
```

### 6.5 禁用状态

```css
:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  pointer-events: none;
}
```

---

## 7. 响应式规范

### 7.1 断点

| 断点 | 屏幕宽度 | 适用设备 |
|------|----------|----------|
| xs | < 576px | 手机竖屏 |
| sm | ≥ 576px | 手机横屏 |
| md | ≥ 768px | 平板竖屏 |
| lg | ≥ 992px | 平板横屏 |
| xl | ≥ 1200px | 桌面 |
| xxl | ≥ 1600px | 大屏桌面 |

### 7.2 响应式策略

- **移动优先**: 从移动端设计开始，逐步增强
- **弹性布局**: 使用 flexbox 和 grid
- **相对单位**: 使用 %、rem、vh、vw
- **媒体查询**: 针对不同断点调整布局

### 7.3 移动端适配

- 触摸目标最小尺寸: 44px × 44px
- 文字最小字号: 12px
- 输入框最小高度: 44px
- 按钮最小高度: 44px

---

## 8. 无障碍规范

### 8.1 键盘导航

- Tab 键: 焦点移动
- Enter/Space: 激活按钮和链接
- Esc: 关闭弹窗和菜单
- 方向键: 在列表中导航

### 8.2 焦点管理

- 焦点可见: 清晰的焦点样式
- 焦点顺序: 符合视觉顺序
- 焦点陷阱: 模态框内的焦点循环

### 8.3 ARIA 属性

- aria-label: 元素的文本标签
- aria-describedby: 元素的描述
- aria-expanded: 展开/折叠状态
- aria-hidden: 隐藏的元素
- role: 元素的语义角色

### 8.4 色彩对比度

- 正常文本: 至少 4.5:1
- 大文本 (≥18px): 至少 3:1
- 图标和图形: 至少 3:1

### 8.5 屏幕阅读器

- 提供有意义的 alt 文本
- 使用语义化 HTML 标签
- 为动态内容提供 aria-live 区域

---

## 附录

### A. 图标库

使用 Element Plus Icons Vue，包含 200+ 图标。

### B. CSS 变量速查

```css
/* 完整的 CSS 变量列表 */
:root {
  /* 主色调 */
  --primary: #1890FF;
  --primary-light: #40A9FF;
  --primary-dark: #096DD9;

  /* 功能色 */
  --success: #52C41A;
  --warning: #FAAD14;
  --error: #F5222D;
  --info: #1890FF;

  /* 文本色 */
  --text-primary: #262626;
  --text-regular: #595959;
  --text-secondary: #8C8C8C;

  /* 背景色 */
  --bg-base: #FFFFFF;
  --bg-page: #F5F5F5;

  /* 边框色 */
  --border-base: #D9D9D9;

  /* 字体 */
  --font-size-base: 14px;
  --line-height-base: 22px;

  /* 间距 */
  --space-4: 16px;
  --space-8: 32px;

  /* 圆角 */
  --radius-base: 6px;

  /* 阴影 */
  --shadow-2: 0 1px 2px -1px rgba(0, 0, 0, 0.1);
}
```

---

**文档版本**: v1.0
**最后更新**: 2026-04-27
**维护者**: ui-mnj0fukd