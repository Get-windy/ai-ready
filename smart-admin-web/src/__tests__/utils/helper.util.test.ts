/**
 * AI-Ready 工具函数测试
 */
import { describe, it, expect } from 'vitest'

// =====================
// 日期工具测试
// =====================
describe('日期工具函数', () => {
  const formatDate = (date: Date, format: string): string => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    
    return format
      .replace('YYYY', String(year))
      .replace('MM', month)
      .replace('DD', day)
      .replace('HH', hours)
      .replace('mm', minutes)
      .replace('ss', seconds)
  }
  
  it('格式化日期为YYYY-MM-DD', () => {
    const date = new Date(2026, 3, 1)
    expect(formatDate(date, 'YYYY-MM-DD')).toBe('2026-04-01')
  })
  
  it('格式化日期时间为完整格式', () => {
    const date = new Date(2026, 3, 1, 10, 30, 45)
    expect(formatDate(date, 'YYYY-MM-DD HH:mm:ss')).toBe('2026-04-01 10:30:45')
  })
  
  it('单数月份和日期补零', () => {
    const date = new Date(2026, 0, 5)
    expect(formatDate(date, 'YYYY-MM-DD')).toBe('2026-01-05')
  })
})

// =====================
// 字符串工具测试
// =====================
describe('字符串工具函数', () => {
  const truncate = (str: string, length: number): string => {
    if (str.length <= length) return str
    return str.slice(0, length) + '...'
  }
  
  const capitalize = (str: string): string => {
    return str.charAt(0).toUpperCase() + str.slice(1)
  }
  
  const maskPhone = (phone: string): string => {
    return phone.slice(0, 3) + '****' + phone.slice(-4)
  }
  
  const maskEmail = (email: string): string => {
    const [name, domain] = email.split('@')
    return name.slice(0, 2) + '***@' + domain
  }
  
  it('截断字符串', () => {
    expect(truncate('这是一个很长的字符串', 5)).toBe('这是一个很...')
    expect(truncate('短', 5)).toBe('短')
  })
  
  it('首字母大写', () => {
    expect(capitalize('hello')).toBe('Hello')
    expect(capitalize('WORLD')).toBe('WORLD')
  })
  
  it('电话号码脱敏', () => {
    expect(maskPhone('13812345678')).toBe('138****5678')
    expect(maskPhone('15998765432')).toBe('159****5432')
  })
  
  it('邮箱脱敏', () => {
    expect(maskEmail('test@example.com')).toBe('te***@example.com')
    expect(maskEmail('admin@company.cn')).toBe('ad***@company.cn')
  })
})

// =====================
// 数组工具测试
// =====================
describe('数组工具函数', () => {
  const unique = <T>(arr: T[]): T[] => [...new Set(arr)]
  
  const groupBy = <T>(arr: T[], key: keyof T): Record<string, T[]> => {
    return arr.reduce((acc, item) => {
      const groupKey = String(item[key])
      if (!acc[groupKey]) acc[groupKey] = []
      acc[groupKey].push(item)
      return acc
    }, {} as Record<string, T[]>)
  }
  
  const sortBy = <T>(arr: T[], key: keyof T, order: 'asc' | 'desc' = 'asc'): T[] => {
    return [...arr].sort((a, b) => {
      const aVal = a[key]
      const bVal = b[key]
      const cmp = aVal < bVal ? -1 : aVal > bVal ? 1 : 0
      return order === 'asc' ? cmp : -cmp
    })
  }
  
  it('数组去重', () => {
    expect(unique([1, 2, 2, 3, 3, 3])).toEqual([1, 2, 3])
    expect(unique(['a', 'b', 'a', 'c'])).toEqual(['a', 'b', 'c'])
  })
  
  it('按属性分组', () => {
    const data = [
      { type: 'A', value: 1 },
      { type: 'B', value: 2 },
      { type: 'A', value: 3 }
    ]
    const grouped = groupBy(data, 'type')
    expect(grouped['A']).toHaveLength(2)
    expect(grouped['B']).toHaveLength(1)
  })
  
  it('按属性排序', () => {
    const data = [{ age: 30 }, { age: 20 }, { age: 25 }]
    const sorted = sortBy(data, 'age')
    expect(sorted[0].age).toBe(20)
    expect(sorted[2].age).toBe(30)
  })
  
  it('降序排序', () => {
    const data = [{ age: 30 }, { age: 20 }, { age: 25 }]
    const sorted = sortBy(data, 'age', 'desc')
    expect(sorted[0].age).toBe(30)
    expect(sorted[2].age).toBe(20)
  })
})

// =====================
// 对象工具测试
// =====================
describe('对象工具函数', () => {
  const deepClone = <T>(obj: T): T => JSON.parse(JSON.stringify(obj))
  
  const omit = <T extends object, K extends keyof T>(obj: T, keys: K[]): Omit<T, K> => {
    const result = { ...obj }
    keys.forEach(key => delete result[key])
    return result
  }
  
  const pick = <T extends object, K extends keyof T>(obj: T, keys: K[]): Pick<T, K> => {
    const result = {} as Pick<T, K>
    keys.forEach(key => {
      if (key in obj) result[key] = obj[key]
    })
    return result
  }
  
  it('深拷贝对象', () => {
    const obj = { a: 1, b: { c: 2 } }
    const cloned = deepClone(obj)
    expect(cloned).toEqual(obj)
    expect(cloned).not.toBe(obj)
    expect(cloned.b).not.toBe(obj.b)
  })
  
  it('排除对象属性', () => {
    const obj = { a: 1, b: 2, c: 3 }
    expect(omit(obj, ['b'])).toEqual({ a: 1, c: 3 })
    expect(omit(obj, ['a', 'c'])).toEqual({ b: 2 })
  })
  
  it('选取对象属性', () => {
    const obj = { a: 1, b: 2, c: 3 }
    expect(pick(obj, ['a', 'b'])).toEqual({ a: 1, b: 2 })
  })
})

// =====================
// 验证工具测试
// =====================
describe('验证工具函数', () => {
  const isValidEmail = (email: string): boolean => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
  }
  
  const isValidPhone = (phone: string): boolean => {
    return /^1[3-9]\d{9}$/.test(phone)
  }
  
  const isValidPassword = (password: string): boolean => {
    return password.length >= 8 && 
           /[A-Z]/.test(password) && 
           /[a-z]/.test(password) && 
           /\d/.test(password)
  }
  
  it('验证邮箱格式', () => {
    expect(isValidEmail('test@example.com')).toBe(true)
    expect(isValidEmail('user.name@company.cn')).toBe(true)
    expect(isValidEmail('invalid')).toBe(false)
    expect(isValidEmail('@example.com')).toBe(false)
  })
  
  it('验证手机号格式', () => {
    expect(isValidPhone('13812345678')).toBe(true)
    expect(isValidPhone('15987654321')).toBe(true)
    expect(isValidPhone('12812345678')).toBe(false)
    expect(isValidPhone('1381234567')).toBe(false)
  })
  
  it('验证密码强度', () => {
    expect(isValidPassword('Admin123')).toBe(false) // 少于8位
    expect(isValidPassword('admin12345')).toBe(false) // 无大写
    expect(isValidPassword('ADMIN12345')).toBe(false) // 无小写
    expect(isValidPassword('Administrator')).toBe(false) // 无数字
    expect(isValidPassword('Admin@123')).toBe(true)
  })
})

// =====================
// URL工具测试
// =====================
describe('URL工具函数', () => {
  const parseQuery = (url: string): Record<string, string> => {
    const query = url.split('?')[1]
    if (!query) return {}
    return query.split('&').reduce((acc, pair) => {
      const [key, value] = pair.split('=')
      acc[decodeURIComponent(key)] = decodeURIComponent(value || '')
      return acc
    }, {} as Record<string, string>)
  }
  
  const buildQuery = (params: Record<string, any>): string => {
    return Object.entries(params)
      .filter(([, v]) => v !== undefined && v !== null)
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
      .join('&')
  }
  
  it('解析URL查询参数', () => {
    expect(parseQuery('https://example.com?name=test&age=20')).toEqual({
      name: 'test',
      age: '20'
    })
  })
  
  it('解析空URL返回空对象', () => {
    expect(parseQuery('https://example.com')).toEqual({})
  })
  
  it('构建查询字符串', () => {
    expect(buildQuery({ name: 'test', page: 1 })).toBe('name=test&page=1')
  })
  
  it('忽略undefined和null值', () => {
    expect(buildQuery({ name: 'test', value: undefined, other: null })).toBe('name=test')
  })
})