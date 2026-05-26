import { describe, it, expect } from 'vitest'
import { validateRoleCode, validateRoleName } from '../utils'

describe('角色权限管理模块基础测试', () => {
  describe('角色编码验证', () => {
    it('应该通过有效的角色编码', () => {
      const validCodes = ['admin', 'user_manager', 'project-admin', 'test123']
      validCodes.forEach(code => {
        const result = validateRoleCode(code)
        expect(result.valid).toBe(true)
        expect(result.message).toBeUndefined()
      })
    })

    it('应该拒绝无效的角色编码', () => {
      const invalidCodes = [
        { code: '', message: '角色编码不能为空' },
        { code: 'ab', message: '角色编码长度必须在3-50个字符之间' },
        { code: '123admin', message: '角色编码必须以字母开头' },
        { code: 'admin@test', message: '角色编码必须以字母开头，只能包含字母、数字、下划线和横线' }
      ]
      
      invalidCodes.forEach(({ code, message }) => {
        const result = validateRoleCode(code)
        expect(result.valid).toBe(false)
        expect(result.message).toBe(message)
      })
    })
  })

  describe('角色名称验证', () => {
    it('应该通过有效的角色名称', () => {
      const validNames = ['管理员', 'User Manager', '项目经理', '测试角色123']
      validNames.forEach(name => {
        const result = validateRoleName(name)
        expect(result.valid).toBe(true)
      })
    })

    it('应该拒绝无效的角色名称', () => {
      const invalidNames = [
        { name: '', message: '角色名称不能为空' },
        { name: 'a', message: '角色名称长度必须在2-100个字符之间' },
        { name: 'a'.repeat(101), message: '角色名称长度必须在2-100个字符之间' }
      ]
      
      invalidNames.forEach(({ name, message }) => {
        const result = validateRoleName(name)
        expect(result.valid).toBe(false)
        expect(result.message).toBe(message)
      })
    })
  })

  describe('工具函数', () => {
    it('应该生成唯一的ID', () => {
      const id1 = require('../utils').generateId('role_')
      const id2 = require('../utils').generateId('role_')
      
      expect(id1).toMatch(/^role_/)
      expect(id2).toMatch(/^role_/)
      expect(id1).not.toBe(id2)
    })

    it('应该正确格式化日期', () => {
      const date = new Date('2024-01-15T14:30:00Z')
      const formatted = require('../utils').formatDateTime(date, 'YYYY-MM-DD')
      expect(formatted).toBe('2024-01-15')
    })
  })
})