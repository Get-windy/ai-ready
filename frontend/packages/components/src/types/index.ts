// 组件库类型定义

// 基础类型
export type Size = 'large' | 'default' | 'small' | 'mini';
export type Type = 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'default';
export type Direction = 'horizontal' | 'vertical';
export type Position = 'top' | 'right' | 'bottom' | 'left' | 'center';

// 按钮组件类型
export interface ButtonProps {
  type?: Type;
  size?: Size;
  icon?: string;
  loading?: boolean;
  disabled?: boolean;
  round?: boolean;
  block?: boolean;
  nativeType?: 'button' | 'submit' | 'reset';
}

// 输入框组件类型
export interface InputProps {
  modelValue?: string | number;
  type?: string;
  size?: Size;
  placeholder?: string;
  disabled?: boolean;
  readonly?: boolean;
  clearable?: boolean;
  showPassword?: boolean;
  showWordLimit?: boolean;
  prefixIcon?: string;
  suffixIcon?: string;
  error?: string;
  maxlength?: number;
  minlength?: number;
  autocomplete?: string;
  autofocus?: boolean;
  name?: string;
  form?: string;
  id?: string;
}

// 表格组件类型
export interface TableColumn<T = any> {
  prop?: string;
  label: string;
  width?: string | number;
  minWidth?: string | number;
  fixed?: boolean | 'left' | 'right';
  align?: 'left' | 'center' | 'right';
  headerAlign?: 'left' | 'center' | 'right';
  sortable?: boolean | 'custom';
  resizable?: boolean;
  formatter?: (row: T, column: TableColumn<T>, cellValue: any, index: number) => any;
  className?: string;
  style?: Record<string, any>;
}

// 对话框组件类型
export interface DialogProps {
  title?: string;
  visible?: boolean;
  width?: string | number;
  fullscreen?: boolean;
  top?: string | number;
  modal?: boolean;
  modalClass?: string;
  lockScroll?: boolean;
  customClass?: string;
  closeOnClickModal?: boolean;
  closeOnPressEscape?: boolean;
  showClose?: boolean;
  beforeClose?: (done: () => void) => void;
  center?: boolean;
  destroyOnClose?: boolean;
  appendToBody?: boolean;
}

// 布局组件类型
export interface LayoutProps {
  direction?: Direction;
}

// 分页组件类型
export interface PaginationProps {
  total: number;
  pageSize: number;
  currentPage: number;
  pageSizes?: number[];
  layout?: string;
  background?: boolean;
  small?: boolean;
  disabled?: boolean;
}

// 表单组件类型
export interface FormItemProps {
  label?: string;
  prop?: string;
  labelWidth?: string | number;
  required?: boolean;
  rules?: FormRule[];
  error?: string;
  showMessage?: boolean;
  inlineMessage?: boolean;
  size?: Size;
}

// 表单验证规则类型
export interface FormRule {
  required?: boolean;
  message?: string;
  trigger?: 'blur' | 'change' | ['blur', 'change'];
  min?: number;
  max?: number;
  len?: number;
  type?: 'string' | 'number' | 'boolean' | 'method' | 'regexp' | 'integer' | 'float' | 'array' | 'object' | 'enum' | 'date' | 'url' | 'hex' | 'email';
  pattern?: RegExp;
  validator?: (rule: FormRule, value: any, callback: (error?: string) => void) => void;
}

// 下拉选择组件类型
export interface SelectOption {
  label: string;
  value: any;
  disabled?: boolean;
}

// 日期选择器类型
export interface DatePickerProps {
  modelValue?: Date | string | number | Date[] | string[] | number[];
  type?: 'year' | 'month' | 'date' | 'dates' | 'week' | 'datetime' | 'datetimerange' | 'daterange' | 'monthrange';
  placeholder?: string | string[];
  startPlaceholder?: string;
  endPlaceholder?: string;
  format?: string;
  valueFormat?: string;
  readonly?: boolean;
  disabled?: boolean;
  editable?: boolean;
  clearable?: boolean;
  size?: Size;
  prefixIcon?: string;
  suffixesIcon?: string;
}

// 图标组件类型
export interface IconProps {
  name: string;
  size?: Size | number;
  color?: string;
  rotate?: number;
  spin?: boolean;
}

// 面包屑组件类型
export interface BreadcrumbItem {
  title: string;
  path?: string;
  to?: string | object;
  replace?: boolean;
  disabled?: boolean;
}

// 步骤条组件类型
export interface StepItem {
  title: string;
  description?: string;
  icon?: string;
  status?: 'wait' | 'process' | 'finish' | 'error' | 'success';
}

// 标签页组件类型
export interface TabItem {
  label: string;
  name: string;
  disabled?: boolean;
  closable?: boolean;
}

// 消息通知类型
export interface NotificationOptions {
  title?: string;
  message: string;
  type?: Type;
  duration?: number;
  position?: Position;
  showClose?: boolean;
  offset?: number;
  onClick?: () => void;
  onClose?: () => void;
}

// 消息弹窗类型
export interface MessageBoxOptions {
  title?: string;
  message: string;
  type?: 'alert' | 'confirm' | 'prompt';
  confirmButtonText?: string;
  cancelButtonText?: string;
  showCancelButton?: boolean;
  showConfirmButton?: boolean;
  inputType?: 'text' | 'textarea' | 'number' | 'password';
  inputValue?: string;
  inputPlaceholder?: string;
  inputValidator?: (value: string) => string | boolean;
  inputErrorMessage?: string;
  beforeClose?: (action: 'confirm' | 'cancel' | 'close', instance: any, done: () => void) => void;
}

// 加载类型
export interface LoadingOptions {
  target?: string | HTMLElement;
  body?: boolean;
  fullscreen?: boolean;
  lock?: boolean;
  text?: string;
  spinner?: string;
  background?: string;
  customClass?: string;
}

// ERP业务组件通用类型

// 供应商相关类型
export interface Supplier {
  id: string | number;
  name: string;
  code?: string;
  type?: string;
  contact?: string;
  phone?: string;
  email?: string;
  address?: string;
  status?: 'active' | 'inactive' | 'pending';
  rating?: number;
  tags?: string[];
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

// 采购订单相关类型
export interface PurchaseOrder {
  id: string | number;
  orderNo: string;
  supplierId: string | number;
  supplierName?: string;
  totalAmount: number;
  currency: string;
  status: 'draft' | 'pending' | 'approved' | 'rejected' | 'completed' | 'cancelled';
  items?: PurchaseOrderItem[];
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

export interface PurchaseOrderItem {
  id: string | number;
  orderId: string | number;
  productId: string | number;
  productName?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  specifications?: string;
  deliveryDate?: Date | string;
}

// 库存相关类型
export interface InventoryItem {
  id: string | number;
  productId: string | number;
  productName?: string;
  sku?: string;
  warehouseId: string | number;
  warehouseName?: string;
  quantity: number;
  unit?: string;
  minStock?: number;
  maxStock?: number;
  costPrice?: number;
  marketPrice?: number;
  location?: string;
  status?: 'in_stock' | 'out_of_stock' | 'low_stock' | 'reserved';
  lastUpdated?: Date | string;
}

// 财务相关类型
export interface FinancialRecord {
  id: string | number;
  type: 'income' | 'expense' | 'transfer';
  amount: number;
  currency: string;
  description?: string;
  category?: string;
  accountId?: string | number;
  accountName?: string;
  referenceId?: string | number;
  referenceType?: string;
  transactionDate: Date | string;
  createdAt?: Date | string;
}

// API响应类型
export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  message?: string;
  code?: string | number;
  timestamp?: Date | string;
}

export interface PaginatedResponse<T = any> extends ApiResponse<T[]> {
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

// 查询参数类型
export interface QueryParams {
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
  keyword?: string;
  filters?: Record<string, any>;
}

// 组件事件类型
export type ComponentEmit<T extends string, P = any> = {
  (event: T, payload: P): void;
};

// 通用组件Ref类型
export interface ComponentRef<T = any> {
  $el: HTMLElement;
  focus?: () => void;
  blur?: () => void;
  validate?: () => Promise<boolean>;
  clear?: () => void;
  reset?: () => void;
  [key: string]: any;
}

// 主题配置类型
export interface ThemeConfig {
  primaryColor?: string;
  successColor?: string;
  warningColor?: string;
  dangerColor?: string;
  infoColor?: string;
  borderRadius?: string;
  fontSize?: string;
  fontFamily?: string;
  darkMode?: boolean;
}

// 国际化类型
export interface LocaleMessages {
  [key: string]: string | LocaleMessages;
}

export interface LocaleConfig {
  locale: string;
  messages: LocaleMessages;
}

// 权限类型
export interface Permission {
  id: string;
  name: string;
  code: string;
  description?: string;
  type: 'menu' | 'button' | 'api' | 'data';
  parentId?: string;
  children?: Permission[];
}

// 用户类型
export interface User {
  id: string | number;
  username: string;
  name: string;
  email?: string;
  phone?: string;
  avatar?: string;
  department?: string;
  role?: string;
  status?: 'active' | 'inactive' | 'locked';
  permissions?: string[];
  lastLogin?: Date | string;
  createdAt?: Date | string;
}

export default {
  // 导出所有类型
  Size,
  Type,
  Direction,
  Position,
  ButtonProps,
  InputProps,
  TableColumn,
  DialogProps,
  LayoutProps,
  PaginationProps,
  FormItemProps,
  FormRule,
  SelectOption,
  DatePickerProps,
  IconProps,
  BreadcrumbItem,
  StepItem,
  TabItem,
  NotificationOptions,
  MessageBoxOptions,
  LoadingOptions,
  Supplier,
  PurchaseOrder,
  PurchaseOrderItem,
  InventoryItem,
  FinancialRecord,
  ApiResponse,
  PaginatedResponse,
  QueryParams,
  ComponentEmit,
  ComponentRef,
  ThemeConfig,
  LocaleMessages,
  LocaleConfig,
  Permission,
  User,
};