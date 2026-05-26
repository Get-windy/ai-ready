# AI-Ready 测试环境前端组件库设计规范

**项目**: AI-Ready (智企连)
**版本**: Sprint 27+1
**创建日期**: 2026-04-27
**负责人**: UI设计师 (ui-mnj0fukd)

---

## 1. 设计原则

### 1.1 核心原则
- **一致性**: 统一的视觉语言和交互模式
- **可用性**: 易于理解和使用
- **可访问性**: 支持辅助技术和键盘导航
- **性能**: 快速加载和流畅交互
- **可维护性**: 代码结构清晰，易于扩展

### 1.2 设计哲学
1. **以用户为中心**: 关注用户需求和体验
2. **数据驱动**: 基于真实使用场景设计
3. **渐进增强**: 从基础功能开始，逐步增强
4. **移动优先**: 优先考虑移动端体验

---

## 2. 色彩系统

### 2.1 品牌色

| 名称 | 色值 | HEX | 用途 |
|------|------|-----|------|
| Primary | --ar-color-primary | #1989fa | 主按钮、链接、选中状态 |
| Primary Light | --ar-color-primary-light | #66b1ff | 悬停、背景 |
| Primary Dark | --ar-color-primary-dark | #0d7acc | 深色背景 |
| Success | --ar-color-success | #07c160 | 成功状态、正向操作 |
| Warning | --ar-color-warning | #ff976a | 警告状态、需注意 |
| Danger | --ar-color-danger | #ee0a24 | 错误状态、危险操作 |
| Info | --ar-color-info | #969799 | 信息提示、次要内容 |

### 2.2 文字色

| 名称 | 色值 | HEX | 用途 |
|------|------|-----|------|
| Text Primary | --ar-color-text-primary | #323233 | 主要文字 |
| Text Regular | --ar-color-text-regular | #606266 | 常规文字 |
| Text Secondary | --ar-color-text-secondary | #909399 | 次要文字 |
| Text Placeholder | --ar-color-text-placeholder | #c8c9cc | 占位符文字 |
| Text Disabled | --ar-color-text-disabled | #c0c4cc | 禁用文字 |

### 2.3 背景色

| 名称 | 色值 | HEX | 用途 |
|------|------|-----|------|
| Background | --ar-color-background | #f5f7fa | 页面背景 |
| Background Page | --ar-color-background-page | #ffffff | 白色背景 |
| Background Overlay | --ar-color-background-overlay | rgba(0,0,0,0.5) | 遮罩层 |
| Border | --ar-color-border | #dcdfe6 | 边框 |
| Border Light | --ar-color-border-light | #e4e7ed | 浅边框 |
| Border Lighter | --ar-color-border-lighter | #ebeef5 | 更浅边框 |
| Border Extra Light | --ar-color-border-extra-light | #f2f6fc | 极浅边框 |

### 2.4 状态色

| 状态 | 色值 | HEX | 用途 |
|------|------|-----|------|
| Focus | --ar-color-focus | rgba(24, 144, 255, 0.2) | 焦点外发光 |
| Hover | --ar-color-hover | rgba(0, 0, 0, 0.04) | 悬停背景 |
| Active | --ar-color-active | rgba(0, 0, 0, 0.08) | 点击背景 |
| Disabled | --ar-color-disabled | rgba(0, 0, 0, 0.25) | 禁用态 |

---

## 3. 字体规范

### 3.1 字体栈

```css
font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
             "Helvetica Neue", Arial, "Noto Sans", sans-serif,
             "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol",
             "Noto Color Emoji";
```

### 3.2 字体大小

| 名称 | 大小 | 行高 | 字重 | 用途 |
|------|------|------|------|------|
| XXS | 12px | 20px | 400 | 辅助文字 |
| XS | 13px | 20px | 400 | 标签、角标 |
| SM | 14px | 22px | 400 | 正文 |
| Base | 14px | 24px | 400 | 基础文字 |
| MD | 16px | 24px | 400 | 小标题 |
| LG | 18px | 28px | 500 | 标题 |
| XL | 20px | 28px | 500 | 大标题 |
| XXL | 24px | 32px | 500 | 特大标题 |
| XXXL | 32px | 40px | 600 | 页面标题 |

### 3.3 字重

| 名称 | 值 | 用途 |
|------|-----|------|
| Light | 300 | 大标题装饰 |
| Regular | 400 | 正文 |
| Medium | 500 | 小标题、强调 |
| Semibold | 600 | 标题 |
| Bold | 700 | 重要标题 |

### 3.4 行高

| 名称 | 值 | 用途 |
|------|-----|------|
| Compact | 1.2 | 紧凑标题 |
| Normal | 1.5 | 正文 |
| Relaxed | 1.75 | 说明文字 |

---

## 4. 间距规范

### 4.1 基础间距

| 名称 | 值 | 用途 |
|------|-----|------|
| XXS | 4px | 极小间距 |
| XS | 8px | 小间距 |
| SM | 12px | 中小间距 |
| MD | 16px | 中间距 |
| LG | 20px | 中大间距 |
| XL | 24px | 大间距 |
| XXL | 32px | 超大间距 |
| XXXL | 48px | 特大间距 |

### 4.2 组件间距

| 组件 | 内边距 | 外边距 | 间距 |
|------|--------|--------|------|
| 按钮 | 0 16px | 8px | 8px |
| 输入框 | 0 16px | 16px | 8px |
| 卡片 | 16px | 16px | 16px |
| 列表项 | 12px 16px | 0 | - |
| 弹窗 | 24px | - | - |
| 表单项 | 8px 0 | 16px | 16px |

---

## 5. 圆角规范

| 名称 | 值 | 用途 |
|------|-----|------|
| XXS | 2px | 小元素、标签 |
| XS | 4px | 输入框、复选框 |
| SM | 6px | 按钮、小卡片 |
| MD | 8px | 按钮、卡片 |
| LG | 12px | 大卡片、弹窗 |
| XL | 16px | 特大卡片 |
| Pill | 999px | 胶囊、圆形 |
| Circle | 50% | 圆形 |

---

## 6. 阴影规范

| 名称 | 值 | 用途 |
|------|-----|------|
| XS | 0 1px 2px rgba(0,0,0,0.05) | 轻微悬浮 |
| SM | 0 2px 4px rgba(0,0,0,0.08) | 小卡片 |
| MD | 0 4px 8px rgba(0,0,0,0.1) | 卡片悬浮 |
| LG | 0 8px 16px rgba(0,0,0,0.12) | 弹窗 |
| XL | 0 12px 24px rgba(0,0,0,0.15) | 大弹窗 |
| Focus | 0 0 0 2px rgba(24, 144, 255, 0.2) | 焦点外发光 |

---

## 7. 组件规范

### 7.1 按钮 (ARButton)

#### 尺寸
| 尺寸 | 高度 | 内边距 | 字体 | 圆角 |
|------|------|--------|------|------|
| Mini | 24px | 0 8px | 12px | 4px |
| Small | 32px | 0 12px | 13px | 4px |
| Medium | 36px | 0 16px | 14px | 6px |
| Large | 40px | 0 20px | 14px | 6px |

#### 变体
- **Primary**: 主按钮，品牌色填充
- **Default**: 默认按钮，白色背景
- **Dashed**: 虚线边框
- **Text**: 文本按钮
- **Link**: 链接样式

### 7.2 输入框 (ARInput)

#### 尺寸
| 尺寸 | 高度 | 内边距 | 字体 | 圆角 |
|------|------|--------|------|------|
| Small | 32px | 0 12px | 13px | 4px |
| Medium | 36px | 0 16px | 14px | 4px |
| Large | 40px | 0 20px | 14px | 6px |

#### 状态
- 默认: 灰色边框
- 聚焦: 主色边框 + 外发光
- 错误: 红色边框 + 错误提示
- 禁用: 灰色背景
- 只读: 无边框

### 7.3 卡片 (ARCard)

#### 结构
```
┌─────────────────────────┐
│   封面 (可选)            │
├─────────────────────────┤
│  标题    操作按钮(可选)   │
│  副标题                  │
├─────────────────────────┤
│                         │
│      内容区域            │
│                         │
└─────────────────────────┘
```

#### 尺寸
| 类型 | 内边距 | 圆角 | 阴影 |
|------|--------|------|------|
| Small | 12px | 8px | SM |
| Medium | 16px | 8px | MD |
| Large | 20px | 12px | LG |

### 7.4 列表 (ARList)

#### 列表项
| 属性 | 值 |
|------|-----|
| 高度 | 48-56px |
| 内边距 | 12px 16px |
| 分割线 | 1px solid var(--ar-color-border-light) |
| 点击态 | 背景色 var(--ar-color-hover) |
| 悬浮态 | scale(1.01) + 阴影 |

---

## 8. 交互规范

### 8.1 点击态
- 透明度: opacity: 0.7
- 缩放: transform: scale(0.98)
- 过渡: transition: all 0.2s ease

### 8.2 悬停态
- 透明度: opacity: 0.85
- 阴影: box-shadow 提升一级
- 过渡: transition: all 0.2s ease

### 8.3 禁用态
- 透明度: opacity: 0.4
- 指针: cursor: not-allowed
- 无交互: pointer-events: none

### 8.4 加载态
- 旋转动画: animation: spin 1s linear infinite
- 禁用交互: pointer-events: none
- 透明度: opacity: 0.7

### 8.5 焦点态
- 外发光: box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2)
- 轮廓: outline: 2px solid var(--ar-color-primary)
- 偏移: outline-offset: 2px

---

## 9. 响应式断点

### 9.1 断点定义

| 断点 | 宽度 | 说明 | 设备 |
|------|------|------|------|
| XS | <576px | 超小屏 | 手机竖屏 |
| SM | ≥576px | 小屏 | 手机横屏 |
| MD | ≥768px | 中屏 | 平板 |
| LG | ≥992px | 大屏 | 小笔记本 |
| XL | ≥1200px | 超大屏 | 桌面 |
| XXL | ≥1600px | 特大屏 | 大屏显示器 |

### 9.2 容器宽度

| 断点 | 容器宽度 |
|------|----------|
| XS | 100% |
| SM | 540px |
| MD | 720px |
| LG | 960px |
| XL | 1140px |
| XXL | 1320px |

---

## 10. 动画规范

### 10.1 缓动函数

| 名称 | 函数 | 用途 |
|------|------|------|
| Ease | cubic-bezier(0.25, 0.1, 0.25, 1) | 常规 |
| Ease In | cubic-bezier(0.42, 0, 1, 1) | 进入 |
| Ease Out | cubic-bezier(0, 0, 0.58, 1) | 离开 |
| Ease In Out | cubic-bezier(0.42, 0, 0.58, 1) | 进出 |

### 10.2 动画时长

| 名称 | 时长 | 用途 |
|------|------|------|
| Fast | 0.15s | 快速反馈 |
| Normal | 0.3s | 常规动画 |
| Slow | 0.5s | 复杂动画 |
| Slower | 1s | 页面切换 |

### 10.3 常用动画

```css
/* 淡入 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 淡入向上 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 旋转 */
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 弹跳 */
@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-25%);
  }
  75% {
    transform: translateY(-12%);
  }
}
```

---

## 11. 无障碍设计

### 11.1 触摸目标
- 最小尺寸: 44x44px
- 推荐尺寸: 48x48px
- 间距: 至少 8px

### 11.2 对比度
- 正文: ≥ 4.5:1
- 大文字 (≥18px): ≥ 3:1
- 图标: ≥ 3:1

### 11.3 焦点指示
- 清晰可见的焦点环
- 支持键盘导航 (Tab / Shift+Tab)
- 焦点顺序符合逻辑

### 11.4 屏幕阅读器
- 使用语义化 HTML 标签
- 添加 aria-label 属性
- 为状态变化提供提示
- 表单提供明确标签

### 11.5 ARIA 属性

| 组件 | 必需ARIA属性 | 可选ARIA属性 |
|------|-------------|-------------|
| 按钮 | aria-label | aria-pressed, aria-expanded |
| 输入框 | aria-label | aria-invalid, aria-describedby |
| 链接 | aria-label | - |
| 弹窗 | role="dialog", aria-modal="true" | aria-labelledby, aria-describedby |
| 下拉菜单 | role="menu", aria-expanded | aria-activedescendant |

---

## 12. 表单规范

### 12.1 表单布局

#### 垂直布局
- 标签在上
- 输入框在下方
- 错误提示在输入框下方

#### 水平布局
- 标签在左
- 输入框在右
- 错误提示在输入框下方

#### 行内布局
- 标签、输入框、错误提示在同一行
- 适合简单表单

### 12.2 表单验证

#### 实时验证
- onblur 失去焦点时验证
- oninput 输入时验证（可选）
- 显示即时反馈

#### 提交验证
- onsubmit 提交时验证
- 显示所有错误
- 定位到第一个错误

#### 错误提示
- 输入框变红
- 显示错误图标
- 显示错误文字
- 错误文字颜色: #ee0a24

### 12.3 表单组件

| 组件 | 说明 | 状态 |
|------|------|------|
| ARInput | 输入框 | ✅ 已实现 |
| ARInputNumber | 数字输入框 | ⏳ 待开发 |
| ARInputPassword | 密码输入框 | ✅ 已实现 |
| ARSelect | 选择器 | ⏳ 待开发 |
| ARCheckbox | 复选框 | ⏳ 待开发 |
| ARCheckboxGroup | 复选框组 | ⏳ 待开发 |
| ARRadiobutton | 单选框 | ⏳ 待开发 |
| ARRadiobuttonGroup | 单选框组 | ⏳ 待开发 |
| ARSwitch | 开关 | ⏳ 待开发 |
| ARSlider | 滑块 | ⏳ 待开发 |
| ARDatePicker | 日期选择器 | ⏳ 待开发 |
| ARTimePicker | 时间选择器 | ⏳ 待开发 |
| ARUpload | 上传 | ⏳ 待开发 |
| ARRate | 评分 | ⏳ 待开发 |
| ARTransfer | 穿梭框 | ⏳ 待开发 |
| ARForm | 表单容器 | ✅ 已实现 |
| ARFormItem | 表单项 | ✅ 已实现 |

---

## 13. 数据展示规范

### 13.1 表格 (ARTable)

#### 列宽
- 固定列宽: 推荐 80-200px
- 自适应列宽: 使用 flex
- 最小列宽: 80px

#### 行高
- 小行高: 40px
- 默认行高: 48px
- 大行高: 56px

#### 分页
- 每页显示: 10/20/50/100
- 页码显示: 最多 5 个
- 快速跳转: 支持输入页码

### 13.2 标签 (ARTag)

| 尺寸 | 高度 | 内边距 | 字体 | 圆角 |
|------|------|--------|------|------|
| Small | 20px | 0 8px | 12px | 2px |
| Medium | 24px | 0 10px | 12px | 4px |
| Large | 32px | 0 12px | 14px | 6px |

### 13.3 徽标 (ARBadge)

| 类型 | 尺寸 | 颜色 |
|------|------|------|
| 点 | 8px | 主色 |
| 数字 | 18px | 白色背景+红色文字 |
| 状态 | 16px | 对应状态色 |

---

## 14. 反馈组件规范

### 14.1 提示 (ARMessage)

| 类型 | 颜色 | 图标 | 持续时间 |
|------|------|------|----------|
| Success | #07c160 | ✓ | 3s |
| Warning | #ff976a | ! | 3s |
| Info | #1989fa | i | 3s |
| Error | #ee0a24 | ✕ | 4s |

### 14.2 弹窗 (ARDialog)

| 类型 | 宽度 | 最大宽度 | 圆角 |
|------|------|----------|------|
| Small | 400px | 90% | 8px |
| Medium | 600px | 90% | 8px |
| Large | 800px | 90% | 12px |

### 14.3 抽屉 (ARDrawer)

| 类型 | 宽度 |
|------|------|
| Small | 300px |
| Medium | 500px |
| Large | 700px |

---

## 15. 导航组件规范

### 15.1 菜单 (ARMenu)

#### 菜单项
- 高度: 48px
- 内边距: 0 16px
- 间距: 0
- 选中态: 背景色 + 左侧边框

#### 折叠状态
- 宽度: 64px
- 仅显示图标
- 悬停显示提示

### 15.2 标签页 (ARTabs)

#### 标签
- 高度: 40px
- 内边距: 0 16px
- 间距: 0
- 选中态: 底部边框

---

## 16. 代码规范

### 16.1 组件命名

#### 文件命名
- PascalCase: `ARButton.vue`
- Index 文件: `index.ts`

#### 组件名称
- 使用 AR 前缀
- PascalCase: `<ARButton />`

#### 类名命名
- BEM: `.ar-button__icon--primary`
- 命名空间: `ar-` 前缀

### 16.2 TypeScript 规范

#### 类型定义
```typescript
// Props 类型
export interface IButtonProps {
  type?: ThemeType
  size?: SizeType
  disabled?: boolean
  loading?: boolean
}

// 组件 Props 定义
const props = defineProps<IButtonProps>()
```

#### Emits 定义
```typescript
// Emits 类型
interface ButtonEmits {
  (e: 'click', event: MouseEvent): void
}

const emit = defineEmits<ButtonEmits>()
```

#### Ref 类型
```typescript
const count = ref<number>(0)
const list = ref<IItem[]>([])
```

### 16.3 样式规范

#### 使用 SCSS
- 使用嵌套（不超过 4 层）
- 使用变量
- 使用混入

#### 使用 CSS 变量
- 全局变量: `--ar-*`
- 组件变量: `--ar-{component}-*`

#### 使用 scoped
- 单文件组件使用 scoped
- 避免全局污染

---

## 17. 图标规范

### 17.1 图标尺寸

| 尺寸 | 值 | 用途 |
|------|-----|------|
| XXS | 12px | 内联图标 |
| XS | 14px | 小图标 |
| SM | 16px | 默认图标 |
| MD | 20px | 中等图标 |
| LG | 24px | 大图标 |
| XL | 32px | 特大图标 |
| XXL | 48px | 装饰图标 |

### 17.2 图标风格

- 线性图标
- 2px 描边
- 圆角端点
- 统一视角
- 一致比例

---

## 18. 主题定制

### 18.1 主题变量

```scss
// 主色
--ar-color-primary: #1989fa;
--ar-color-success: #07c160;
--ar-color-warning: #ff976a;
--ar-color-danger: #ee0a24;
--ar-color-info: #969799;

// 文字色
--ar-color-text-primary: #323233;
--ar-color-text-regular: #606266;
--ar-color-text-secondary: #909399;

// 背景色
--ar-color-background: #f5f7fa;
--ar-color-background-page: #ffffff;

// 边框色
--ar-color-border: #dcdfe6;
--ar-color-border-light: #e4e7ed;

// 圆角
--ar-radius-base: 4px;
--ar-radius-small: 2px;
--ar-radius-large: 8px;

// 阴影
--ar-shadow-base: 0 2px 4px rgba(0, 0, 0, 0.12);
--ar-shadow-light: 0 1px 2px rgba(0, 0, 0, 0.12);
--ar-shadow-lighter: 0 0 6px rgba(0, 0, 0, 0.04);

// 字体
--ar-font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
--ar-font-size-base: 14px;
--ar-font-size-small: 13px;
--ar-font-size-large: 16px;
```

### 18.2 深色模式

```scss
// 深色模式变量
[data-theme='dark'] {
  --ar-color-background: #1a1a1a;
  --ar-color-background-page: #2d2d2d;
  --ar-color-text-primary: #e5e5e5;
  --ar-color-text-regular: #c5c5c5;
  --ar-color-text-secondary: #a5a5a5;
  --ar-color-border: #404040;
  --ar-color-border-light: #4d4d4d;
}
```

---

## 19. 开发指南

### 19.1 组件开发流程

1. **需求分析**
   - 确定组件用途
   - 定义 API
   - 设计交互

2. **设计稿**
   - 视觉设计
   - 交互设计
   - 响应式设计

3. **开发**
   - 创建组件文件
   - 实现 Props
   - 实现 Emits
   - 实现方法
   - 编写样式

4. **测试**
   - 单元测试
   - 视觉测试
   - 交互测试

5. **文档**
   - 使用说明
   - API 文档
   - 示例代码

### 19.2 组件目录结构

```
components/
├── @ai-ready/
│   ├── component-name/
│   │   ├── index.ts           # 导出
│   │   ├── ComponentName.vue  # 组件
│   │   ├── types.ts           # 类型定义
│   │   ├── props.ts           # Props 定义
│   │   └── __tests__/         # 测试
│   │       ├── ComponentName.test.ts
│   │       └── ComponentName.visual.test.ts
```

---

## 20. 版本历史

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| 1.0.0 | 2026-04-27 | 初始版本，定义完整设计规范 |

---

**文档维护者**: UI设计师 (ui-mnj0fukd)
**最后更新**: 2026-04-27
**下次审核**: 2026-05-27