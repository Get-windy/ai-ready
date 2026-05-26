// 组件库工具函数

/**
 * 生成随机ID
 */
export const generateId = (prefix = 'ar'): string => {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

/**
 * 格式化日期
 */
export const formatDate = (date: Date | string | number, format = 'YYYY-MM-DD HH:mm:ss'): string => {
  const d = date instanceof Date ? date : new Date(date);
  
  if (isNaN(d.getTime())) {
    return 'Invalid Date';
  }
  
  const pad = (n: number) => n.toString().padStart(2, '0');
  
  const replacements: Record<string, string> = {
    'YYYY': d.getFullYear().toString(),
    'MM': pad(d.getMonth() + 1),
    'DD': pad(d.getDate()),
    'HH': pad(d.getHours()),
    'mm': pad(d.getMinutes()),
    'ss': pad(d.getSeconds()),
    'M': (d.getMonth() + 1).toString(),
    'D': d.getDate().toString(),
    'H': d.getHours().toString(),
    'm': d.getMinutes().toString(),
    's': d.getSeconds().toString(),
  };
  
  return format.replace(/YYYY|MM|DD|HH|mm|ss|M|D|H|m|s/g, match => replacements[match]);
};

/**
 * 深拷贝对象
 */
export const deepClone = <T>(obj: T): T => {
  if (obj === null || typeof obj !== 'object') {
    return obj;
  }
  
  if (obj instanceof Date) {
    return new Date(obj.getTime()) as T;
  }
  
  if (obj instanceof Array) {
    return obj.map(item => deepClone(item)) as T;
  }
  
  if (typeof obj === 'object') {
    const clonedObj = {} as Record<string, any>;
    for (const key in obj) {
      if (Object.prototype.hasOwnProperty.call(obj, key)) {
        clonedObj[key] = deepClone((obj as Record<string, any>)[key]);
      }
    }
    return clonedObj as T;
  }
  
  return obj;
};

/**
 * 防抖函数
 */
export const debounce = <T extends (...args: any[]) => any>(
  func: T,
  wait: number,
  immediate = false
): ((...args: Parameters<T>) => void) => {
  let timeout: NodeJS.Timeout | null = null;
  
  return function (this: any, ...args: Parameters<T>): void {
    const later = () => {
      timeout = null;
      if (!immediate) {
        func.apply(this, args);
      }
    };
    
    const callNow = immediate && !timeout;
    
    if (timeout) {
      clearTimeout(timeout);
    }
    
    timeout = setTimeout(later, wait);
    
    if (callNow) {
      func.apply(this, args);
    }
  };
};

/**
 * 节流函数
 */
export const throttle = <T extends (...args: any[]) => any>(
  func: T,
  limit: number
): ((...args: Parameters<T>) => void) => {
  let inThrottle: boolean;
  
  return function (this: any, ...args: Parameters<T>): void {
    if (!inThrottle) {
      func.apply(this, args);
      inThrottle = true;
      setTimeout(() => (inThrottle = false), limit);
    }
  };
};

/**
 * 验证邮箱格式
 */
export const isValidEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

/**
 * 验证手机号码格式（中国）
 */
export const isValidPhone = (phone: string): boolean => {
  const phoneRegex = /^1[3-9]\d{9}$/;
  return phoneRegex.test(phone);
};

/**
 * 格式化金额
 */
export const formatCurrency = (
  amount: number | string,
  options: Intl.NumberFormatOptions = {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }
): string => {
  const num = typeof amount === 'string' ? parseFloat(amount) : amount;
  
  if (isNaN(num)) {
    return 'Invalid Amount';
  }
  
  return new Intl.NumberFormat('zh-CN', options).format(num);
};

/**
 * 生成UUID
 */
export const generateUUID = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

/**
 * 安全的JSON解析
 */
export const safeJSONParse = <T>(jsonString: string, defaultValue: T): T => {
  try {
    return JSON.parse(jsonString) as T;
  } catch {
    return defaultValue;
  }
};

/**
 * 安全的JSON序列化
 */
export const safeJSONStringify = (obj: any, defaultValue = '{}'): string => {
  try {
    return JSON.stringify(obj);
  } catch {
    return defaultValue;
  }
};

/**
 * 数组去重
 */
export const uniqueArray = <T>(arr: T[], key?: keyof T): T[] => {
  if (!key) {
    return Array.from(new Set(arr));
  }
  
  const seen = new Set();
  return arr.filter(item => {
    const value = item[key];
    if (seen.has(value)) {
      return false;
    }
    seen.add(value);
    return true;
  });
};

/**
 * 对象合并（深度合并）
 */
export const deepMerge = <T extends Record<string, any>>(target: T, ...sources: Partial<T>[]): T => {
  if (!sources.length) return target;
  
  const source = sources.shift();
  if (source === undefined) return target;
  
  for (const key in source) {
    if (Object.prototype.hasOwnProperty.call(source, key)) {
      if (source[key] && typeof source[key] === 'object' && !Array.isArray(source[key])) {
        if (!target[key] || typeof target[key] !== 'object') {
          target[key] = {} as any;
        }
        target[key] = deepMerge(target[key], source[key]);
      } else {
        target[key] = source[key];
      }
    }
  }
  
  return deepMerge(target, ...sources);
};

/**
 * 延迟函数
 */
export const delay = (ms: number): Promise<void> => {
  return new Promise(resolve => setTimeout(resolve, ms));
};

/**
 * 检查是否是空值
 */
export const isEmpty = (value: any): boolean => {
  if (value === null || value === undefined) return true;
  if (typeof value === 'string' && value.trim() === '') return true;
  if (Array.isArray(value) && value.length === 0) return true;
  if (typeof value === 'object' && Object.keys(value).length === 0) return true;
  return false;
};

/**
 * 获取对象路径值
 */
export const get = <T>(obj: any, path: string, defaultValue?: T): T | undefined => {
  const keys = path.split('.');
  let result = obj;
  
  for (const key of keys) {
    if (result && typeof result === 'object' && key in result) {
      result = result[key];
    } else {
      return defaultValue;
    }
  }
  
  return result as T;
};

/**
 * 设置对象路径值
 */
export const set = (obj: any, path: string, value: any): void => {
  const keys = path.split('.');
  let current = obj;
  
  for (let i = 0; i < keys.length - 1; i++) {
    const key = keys[i];
    if (!current[key] || typeof current[key] !== 'object') {
      current[key] = {};
    }
    current = current[key];
  }
  
  current[keys[keys.length - 1]] = value;
};

export default {
  generateId,
  formatDate,
  deepClone,
  debounce,
  throttle,
  isValidEmail,
  isValidPhone,
  formatCurrency,
  generateUUID,
  safeJSONParse,
  safeJSONStringify,
  uniqueArray,
  deepMerge,
  delay,
  isEmpty,
  get,
  set,
};