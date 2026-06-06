/**
 * 通用表单验证规则
 * 统一各模块的表单验证规则，避免页面重复定义
 */
import type { Rule } from 'ant-design-vue/es/form'

/** 必填规则（带字段名提示） */
export const requiredRule = (label: string = '此项'): Rule => ({
  required: true,
  message: `请输入${label}`,
  trigger: 'blur'
})

/** 必选规则 */
export const requiredSelectRule = (label: string = '此项'): Rule => ({
  required: true,
  message: `请选择${label}`,
  trigger: 'change'
})

/** 手机号规则 */
export const phoneRule: Rule = {
  pattern: /^1[3-9]\d{9}$/,
  message: '请输入有效的手机号码',
  trigger: 'blur'
}

/** 邮箱规则 */
export const emailRule: Rule = {
  type: 'email',
  message: '请输入有效的邮箱地址',
  trigger: 'blur'
}

/** 密码规则（最少6位） */
export const passwordRule = (min: number = 6): Rule => ({
  min,
  message: `密码长度不能少于 ${min} 位`,
  trigger: 'blur'
})

/** URL规则 */
export const urlRule: Rule = {
  pattern: /^https?:\/\/.+/,
  message: '请输入有效的URL地址',
  trigger: 'blur'
}

/** 数字范围规则 */
export const rangeRule = (min?: number, max?: number, label: string = '数值'): Rule => ({
  type: 'number',
  min,
  max,
  message: `${label}${min !== undefined ? `最小值为 ${min}` : ''}${min !== undefined && max !== undefined ? '，' : ''}${max !== undefined ? `最大值为 ${max}` : ''}`,
  trigger: 'blur'
})

/** 金额规则（正数，最多2位小数） */
export const amountRule: Rule = {
  pattern: /^(\d+)(\.\d{1,2})?$/,
  message: '请输入有效的金额（正数，最多2位小数）',
  trigger: 'blur'
}

/** 正整数规则 */
export const positiveIntRule: Rule = {
  pattern: /^[1-9]\d*$/,
  message: '请输入正整数',
  trigger: 'blur'
}

/** 通用字段规则集 */
export const commonRules: Record<string, Rule[]> = {
  username: [
    requiredRule('用户名'),
    { min: 2, max: 50, message: '用户名长度在 2-50 个字符', trigger: 'blur' }
  ],
  nickname: [requiredRule('昵称')],
  password: [requiredRule('密码'), passwordRule(6)],
  email: [emailRule],
  phone: [phoneRule],
  remark: [{ max: 500, message: '备注不能超过500个字符', trigger: 'blur' }],
  sort: [{ pattern: /^\d+$/, message: '请输入正整数', trigger: 'blur' }]
}
