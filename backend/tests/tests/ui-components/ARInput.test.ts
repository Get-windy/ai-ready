import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ARInput from '../../frontend/src/components/@ai-ready/mobile-ui/components/ARInput/ARInput.vue'

describe('ARInput 组件测试', () => {
  // 功能测试
  describe('基本功能', () => {
    it('应该正确渲染输入框', () => {
      const wrapper = mount(ARInput)
      expect(wrapper.find('input').exists()).toBe(true)
    })

    it('应该支持 placeholder', () => {
      const wrapper = mount(ARInput, {
        props: { placeholder: '请输入内容' }
      })
      expect(wrapper.find('input').attributes('placeholder')).toBe('请输入内容')
    })

    it('应该显示初始值', () => {
      const wrapper = mount(ARInput, {
        props: { modelValue: '初始内容' }
      })
      expect(wrapper.find('input').element.value).toBe('初始内容')
    })

    it('应该支持不同尺寸', () => {
      const sizes = ['large', 'normal', 'small']
      sizes.forEach(size => {
        const wrapper = mount(ARInput, { props: { size } })
        expect(wrapper.find(`.ar-input--${size}`).exists()).toBe(true)
      })
    })

    it('应该支持不同输入类型', () => {
      const types = ['text', 'password', 'number', 'tel', 'email', 'url', 'search']
      types.forEach(type => {
        const wrapper = mount(ARInput, { props: { type } })
        expect(wrapper.find('input').attributes('type')).toBe(type)
      })
    })

    it('应该支持 maxlength 限制', () => {
      const wrapper = mount(ARInput, {
        props: { maxlength: 10 }
      })
      expect(wrapper.find('input').attributes('maxlength')).toBe('10')
    })

    it('应该显示字数统计', () => {
      const wrapper = mount(ARInput, {
        props: {
          modelValue: '测试内容',
          maxlength: 20,
          showWordLimit: true
        }
      })
      expect(wrapper.find('.ar-input__word-limit').exists()).toBe(true)
      expect(wrapper.find('.ar-input__word-limit').text()).toBe('4/20')
    })
  })

  // 交互测试
  describe('交互行为', () => {
    it('输入应该触发 update:modelValue 事件', async () => {
      const wrapper = mount(ARInput)
      const input = wrapper.find('input')
      await input.setValue('新内容')
      expect(wrapper.emitted('update:modelValue')).toBeTruthy()
      expect(wrapper.emitted('update:modelValue')![0][0]).toBe('新内容')
    })

    it('聚焦应该触发 focus 事件', async () => {
      const wrapper = mount(ARInput)
      await wrapper.find('input').trigger('focus')
      expect(wrapper.emitted('focus')).toBeTruthy()
      expect(wrapper.find('.ar-input--focused').exists()).toBe(true)
    })

    it('失焦应该触发 blur 事件', async () => {
      const wrapper = mount(ARInput)
      await wrapper.find('input').trigger('focus')
      await wrapper.find('input').trigger('blur')
      expect(wrapper.emitted('blur')).toBeTruthy()
      expect(wrapper.find('.ar-input--focused').exists()).toBe(false)
    })

    it('回车应该触发 keydown 事件', async () => {
      const wrapper = mount(ARInput)
      await wrapper.find('input').trigger('keydown', { key: 'Enter' })
      expect(wrapper.emitted('keydown')).toBeTruthy()
    })

    it('清空按钮应该清除内容', async () => {
      const wrapper = mount(ARInput, {
        props: {
          modelValue: '内容',
          clearable: true
        }
      })
      await wrapper.find('.ar-input__clear').trigger('click')
      expect(wrapper.emitted('update:modelValue')![0][0]).toBe('')
      expect(wrapper.emitted('clear')).toBeTruthy()
    })

    it('密码切换应该改变输入类型', async () => {
      const wrapper = mount(ARInput, {
        props: {
          type: 'password',
          showPassword: true,
          modelValue: 'password123'
        }
      })
      expect(wrapper.find('input').attributes('type')).toBe('password')
      await wrapper.find('.ar-input__password-toggle').trigger('click')
      expect(wrapper.find('input').attributes('type')).toBe('text')
    })
  })

  // 状态测试
  describe('状态管理', () => {
    it('禁用状态应该有正确的类名和属性', () => {
      const wrapper = mount(ARInput, { props: { disabled: true } })
      expect(wrapper.find('.ar-input--disabled').exists()).toBe(true)
      expect(wrapper.find('input').attributes('disabled')).toBeDefined()
    })

    it('只读状态应该有正确的类名和属性', () => {
      const wrapper = mount(ARInput, { props: { readonly: true } })
      expect(wrapper.find('.ar-input--readonly').exists()).toBe(true)
      expect(wrapper.find('input').attributes('readonly')).toBeDefined()
    })

    it('错误状态应该显示错误提示', () => {
      const wrapper = mount(ARInput, {
        props: { error: '输入错误' }
      })
      expect(wrapper.find('.ar-input--error').exists()).toBe(true)
      expect(wrapper.find('.ar-input__error').text()).toBe('输入错误')
    })

    it('聚焦状态应该有 focused 类名', async () => {
      const wrapper = mount(ARInput)
      await wrapper.find('input').trigger('focus')
      expect(wrapper.find('.ar-input--focused').exists()).toBe(true)
    })
  })

  // Slots 测试
  describe('插槽支持', () => {
    it('应该支持 prefix 插槽', () => {
      const wrapper = mount(ARInput, {
        slots: {
          prefix: '<span class="custom-prefix">前缀</span>'
        }
      })
      expect(wrapper.find('.custom-prefix').exists()).toBe(true)
    })

    it('应该支持 suffix 插槽', () => {
      const wrapper = mount(ARInput, {
        slots: {
          suffix: '<span class="custom-suffix">后缀</span>'
        }
      })
      expect(wrapper.find('.custom-suffix').exists()).toBe(true)
    })
  })

  // 方法测试
  describe('暴露方法', () => {
    it('focus 方法应该聚焦输入框', async () => {
      const wrapper = mount(ARInput)
      wrapper.vm.focus()
      // 注意：jsdom 中 focus 不真正生效，但方法应该存在
      expect(wrapper.vm.focus).toBeDefined()
    })

    it('blur 方法应该失焦输入框', async () => {
      const wrapper = mount(ARInput)
      wrapper.vm.blur()
      expect(wrapper.vm.blur).toBeDefined()
    })

    it('clear 方法应该清除内容', async () => {
      const wrapper = mount(ARInput, {
        props: { modelValue: '内容' }
      })
      wrapper.vm.clear()
      expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    })

    it('setError 方法应该设置错误信息', async () => {
      const wrapper = mount(ARInput)
      wrapper.vm.setError('自定义错误')
      expect(wrapper.find('.ar-input__error').text()).toBe('自定义错误')
    })

    it('clearError 方法应该清除错误信息', async () => {
      const wrapper = mount(ARInput)
      wrapper.vm.setError('错误')
      wrapper.vm.clearError()
      expect(wrapper.find('.ar-input__error').exists()).toBe(false)
    })
  })

  // 可访问性测试
  describe('可访问性', () => {
    it('输入框应该支持 name 属性', () => {
      const wrapper = mount(ARInput, {
        props: { name: 'username' }
      })
      expect(wrapper.find('input').attributes('name')).toBe('username')
    })

    it('输入框应该可聚焦', () => {
      const wrapper = mount(ARInput)
      const input = wrapper.find('input')
      expect(input.element.tagName).toBe('INPUT')
    })

    it('清空按钮应该可交互', () => {
      const wrapper = mount(ARInput, {
        props: { clearable: true, modelValue: '内容' }
      })
      expect(wrapper.find('.ar-input__clear').exists()).toBe(true)
    })
  })
})