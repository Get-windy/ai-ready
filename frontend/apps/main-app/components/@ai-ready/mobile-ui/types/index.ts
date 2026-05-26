/**
 * AI-Ready Mobile UI 组件库类型定义
 * @package @ai-ready/mobile-ui
 */

import type { Ref, ComputedRef } from 'vue'

// ==================== 基础类型 ====================

/**
 * 主题类型
 */
export type ThemeType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

/**
 * 尺寸类型
 */
export type SizeType = 'large' | 'normal' | 'small' | 'mini'

/**
 * 布局方向
 */
export type DirectionType = 'horizontal' | 'vertical'

/**
 * 对齐方式
 */
export type AlignType = 'left' | 'center' | 'right'

// ==================== 按钮组件类型 ====================

/**
 * 按钮变体类型
 */
export type ButtonVariant = 'solid' | 'outline' | 'ghost' | 'text'

/**
 * 按钮形状
 */
export type ButtonShape = 'default' | 'round' | 'circle'

/**
 * 按钮组件 Props 接口
 */
export interface IButtonProps {
  /** 按钮类型 */
  type?: ThemeType
  /** 按钮变体 */
  variant?: ButtonVariant
  /** 按钮尺寸 */
  size?: SizeType
  /** 按钮形状 */
  shape?: ButtonShape
  /** 是否禁用 */
  disabled?: boolean
  /** 是否加载中 */
  loading?: boolean
  /** 是否块级按钮 */
  block?: boolean
  /** 图标名称 */
  icon?: string
  /** 图标位置 */
  iconPosition?: 'left' | 'right'
  /** 自定义类名 */
  customClass?: string
  /** 自定义样式 */
  customStyle?: Record<string, string | number>
}

/**
 * 按钮组件事件接口
 */
export interface IButtonEmits {
  /** 点击事件 */
  (e: 'click', event: MouseEvent): void
  /** 触摸开始 */
  (e: 'touchstart', event: TouchEvent): void
  /** 触摸结束 */
  (e: 'touchend', event: TouchEvent): void
}

// ==================== 表单组件类型 ====================

/**
 * 输入框类型
 */
export type InputType = 'text' | 'password' | 'number' | 'tel' | 'email' | 'url' | 'search'

/**
 * 表单验证规则
 */
export interface IFormRule {
  /** 是否必填 */
  required?: boolean
  /** 验证消息 */
  message?: string
  /** 最小长度 */
  min?: number
  /** 最大长度 */
  max?: number
  /** 正则表达式 */
  pattern?: RegExp
  /** 自定义验证函数 */
  validator?: (value: unknown) => boolean | string | Promise<boolean | string>
  /** 触发时机 */
  trigger?: 'blur' | 'change' | 'submit' | ('blur' | 'change' | 'submit')[]
}

/**
 * 输入框组件 Props 接口
 */
export interface IInputProps {
  /** 输入框类型 */
  type?: InputType
  /** 绑定值 */
  modelValue?: string | number
  /** 占位符 */
  placeholder?: string
  /** 是否禁用 */
  disabled?: boolean
  /** 是否只读 */
  readonly?: boolean
  /** 是否可清空 */
  clearable?: boolean
  /** 最大长度 */
  maxlength?: number
  /** 是否显示字数统计 */
  showWordLimit?: boolean
  /** 是否自动聚焦 */
  autofocus?: boolean
  /** 输入框名称 */
  name?: string
  /** 验证规则 */
  rules?: IFormRule[]
  /** 自定义类名 */
  customClass?: string
  /** 前缀图标 */
  prefixIcon?: string
  /** 后缀图标 */
  suffixIcon?: string
}

/**
 * 输入框组件事件接口
 */
export interface IInputEmits {
  /** 更新值 */
  (e: 'update:modelValue', value: string): void
  /** 输入事件 */
  (e: 'input', value: string): void
  /** 改变事件 */
  (e: 'change', value: string): void
  /** 聚焦事件 */
  (e: 'focus', event: FocusEvent): void
  /** 失焦事件 */
  (e: 'blur', event: FocusEvent): void
  /** 清空事件 */
  (e: 'clear'): void
  /** 键盘按下 */
  (e: 'keydown', event: KeyboardEvent): void
}

// ==================== 列表组件类型 ====================

/**
 * 列表项数据接口
 */
export interface IListItem {
  /** 唯一标识 */
  id: string | number
  /** 标题 */
  title?: string
  /** 描述 */
  description?: string
  /** 图片URL */
  image?: string
  /** 标签 */
  tag?: string
  /** 标签类型 */
  tagType?: ThemeType
  /** 右侧文本 */
  rightText?: string
  /** 是否可点击 */
  clickable?: boolean
  /** 自定义数据 */
  [key: string]: unknown
}

/**
 * 列表组件 Props 接口
 */
export interface IListProps {
  /** 列表数据 */
  items: IListItem[]
  /** 是否显示分割线 */
  divider?: boolean
  /** 是否加载中 */
  loading?: boolean
  /** 是否还有更多 */
  hasMore?: boolean
  /** 是否为空 */
  empty?: boolean
  /** 空状态文本 */
  emptyText?: string
  /** 空状态图片 */
  emptyImage?: string
  /** 自定义类名 */
  customClass?: string
}

/**
 * 列表组件事件接口
 */
export interface IListEmits {
  /** 点击项 */
  (e: 'click', item: IListItem, index: number): void
  /** 加载更多 */
  (e: 'load-more'): void
  /** 下拉刷新 */
  (e: 'refresh'): void
}

// ==================== 卡片组件类型 ====================

/**
 * 卡片组件 Props 接口
 */
export interface ICardProps {
  /** 卡片标题 */
  title?: string
  /** 卡片副标题 */
  subtitle?: string
  /** 是否显示边框 */
  bordered?: boolean
  /** 是否可悬浮 */
  hoverable?: boolean
  /** 是否加载中 */
  loading?: boolean
  /** 自定义类名 */
  customClass?: string
  /** 封面图片 */
  cover?: string
  /** 操作区域 */
  actions?: Array<{
    text: string
    type?: ThemeType
    icon?: string
    disabled?: boolean
  }>
}

/**
 * 卡片组件事件接口
 */
export interface ICardEmits {
  /** 点击卡片 */
  (e: 'click'): void
  /** 点击操作 */
  (e: 'action', index: number): void
}

// ==================== 弹窗组件类型 ====================

/**
 * 弹窗位置
 */
export type DialogPosition = 'center' | 'top' | 'bottom' | 'left' | 'right'

/**
 * 弹窗组件 Props 接口
 */
export interface IDialogProps {
  /** 是否显示 */
  modelValue: boolean
  /** 弹窗标题 */
  title?: string
  /** 弹窗内容 */
  content?: string
  /** 弹窗位置 */
  position?: DialogPosition
  /** 是否显示遮罩 */
  overlay?: boolean
  /** 点击遮罩关闭 */
  closeOnClickOverlay?: boolean
  /** 是否显示关闭按钮 */
  showClose?: boolean
  /** 自定义类名 */
  customClass?: string
  /** 宽度 */
  width?: string | number
  /** 圆角 */
  round?: boolean
}

/**
 * 弹窗组件事件接口
 */
export interface IDialogEmits {
  /** 更新显示状态 */
  (e: 'update:modelValue', value: boolean): void
  /** 打开事件 */
  (e: 'open'): void
  /** 关闭事件 */
  (e: 'close'): void
  /** 确认事件 */
  (e: 'confirm'): void
  /** 取消事件 */
  (e: 'cancel'): void
}

// ==================== 加载组件类型 ====================

/**
 * 加载类型
 */
export type LoadingType = 'circular' | 'spinner' | 'dots'

/**
 * 加载组件 Props 接口
 */
export interface ILoadingProps {
  /** 加载类型 */
  type?: LoadingType
  /** 加载文本 */
  text?: string
  /** 文本大小 */
  textSize?: string | number
  /** 加载颜色 */
  color?: string
  /** 背景颜色 */
  background?: string
  /** 是否全屏 */
  fullscreen?: boolean
  /** 是否显示遮罩 */
  overlay?: boolean
  /** 自定义类名 */
  customClass?: string
}

// ==================== Toast 提示类型 ====================

/**
 * Toast 位置
 */
export type ToastPosition = 'top' | 'center' | 'bottom'

/**
 * Toast 选项接口
 */
export interface IToastOptions {
  /** 提示内容 */
  message: string
  /** 提示类型 */
  type?: ThemeType
  /** 显示位置 */
  position?: ToastPosition
  /** 显示时长(ms) */
  duration?: number
  /** 是否显示图标 */
  icon?: string
  /** 是否显示遮罩 */
  overlay?: boolean
  /** 是否禁止点击 */
  forbidClick?: boolean
  /** 关闭回调 */
  onClose?: () => void
}

// ==================== 组合式函数类型 ====================

/**
 * 触摸事件
 */
export interface ITouchEvent {
  startX: number
  startY: number
  deltaX: number
  deltaY: number
  offsetX: number
  offsetY: number
  direction: 'horizontal' | 'vertical' | ''
}

/**
 * 触摸组合式函数返回类型
 */
export interface IUseTouchReturn {
  touchStart: (event: TouchEvent) => void
  touchMove: (event: TouchEvent) => void
  touchEnd: (event: TouchEvent) => void
  touchState: Ref<ITouchEvent>
}

/**
 * 防抖/节流选项
 */
export interface IDebounceOptions {
  /** 等待时间(ms) */
  wait?: number
  /** 是否立即执行 */
  immediate?: boolean
}

/**
 * 倒计时组合式函数选项
 */
export interface ICountDownOptions {
  /** 总时长(秒) */
  total: number
  /** 是否自动开始 */
  autoStart?: boolean
  /** 间隔时间(ms) */
  interval?: number
}

/**
 * 倒计时组合式函数返回类型
 */
export interface IUseCountDownReturn {
  /** 剩余时间(秒) */
  remaining: Ref<number>
  /** 格式化后的时间 */
  formatted: ComputedRef<string>
  /** 是否运行中 */
  isRunning: Ref<boolean>
  /** 开始 */
  start: () => void
  /** 暂停 */
  pause: () => void
  /** 重置 */
  reset: () => void
}

// ==================== 主题配置类型 ====================

/**
 * 主题颜色配置
 */
export interface IThemeColors {
  primary: string
  success: string
  warning: string
  danger: string
  info: string
  text: string
