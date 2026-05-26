import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ARButton from '../../frontend/src/components/@ai-ready/mobile-ui/components/ARButton/ARButton.vue'

describe('ARButton 组件测试', () => {
  // 功能测试
  describe('基本功能', () => {
    it('应该正确渲染按钮', () => {
      const wrapper = mount(ARButton, {
        slots: {
          default: '测试按钮'
        }
      })
      expect(wrapper.find('button').exists()).toBe(true)
      expect(wrapper.text()).toContain('测试按钮')
    })

    it('应该支持不同类型', () => {
      const types = ['primary', 'success', 'warning', 'danger', 'info']
      types.forEach(type => {
        const wrapper = mount(ARButton, { props: { type } })
        expect(wrapper.find(`.ar-button--${type}`).exists()).toBe(true)
      })
    })

    it('应该支持不同尺寸', () => {
      const sizes = ['large', 'normal', 'small', 'mini']
      sizes.forEach(size => {
        const wrapper = mount(ARButton, { props: { size } })
        expect(wrapper.find(`.ar-button--${size}`).exists()).toBe(true)
      })
    })

    it('应该支持不同形状', () => {
      const shapes = ['default', 'round', 'circle']
      shapes.forEach(shape => {
        const wrapper = mount(ARButton, { props: { shape } })
        expect(wrapper.find(`.ar-button--${shape}`).exists()).toBe(true)
      })
    })

    it('应该支持不同变体', () => {
      const variants = ['solid', 'outline', 'ghost', 'text']
      variants.forEach(variant => {
        const wrapper = mount(ARButton, { props: { variant } })
        expect(wrapper.find(`.ar-button--${variant}`).exists()).toBe(true)
      })
    })
  })

  // 交互测试
  describe('交互行为', () => {
    it('点击事件应该正常触发', async () => {
      const wrapper = mount(ARButton)
      await wrapper.trigger('click')
      expect(wrapper.emitted('click')).toBeTruthy()
    })

    it('禁用状态不应触发点击', async () => {
      const wrapper = mount(ARButton, { props: { disabled: true } })
      await wrapper.trigger('click')
      expect(wrapper.emitted('click')).toBeFalsy()
    })

    it('加载状态不应触发点击', async () => {
      const wrapper = mount(ARButton, { props: { loading: true } })
      await wrapper.trigger('click')
      expect(wrapper.emitted('click')).toBeFalsy()
    })

    it('触摸事件应该正常触发', async () => {
      const wrapper = mount(ARButton)
      await wrapper.trigger('touchstart')
      expect(wrapper.emitted('touchstart')).toBeTruthy()
      await wrapper.trigger('touchend')
      expect(wrapper.emitted('touchend')).toBeTruthy()
    })
  })

  // 状态测试
  describe('状态管理', () => {
    it('禁用状态应该有正确的类名', () => {
      const wrapper = mount(ARButton, { props: { disabled: true } })
      expect(wrapper.find('.ar-button--disabled').exists()).toBe(true)
    })

    it('加载状态应该显示加载图标', () => {
      const wrapper = mount(ARButton, { props: { loading: true } })
      expect(wrapper.find('.ar-button__loading').exists()).toBe(true)
    })

    it('块级按钮应该有 block 类名', () => {
      const wrapper = mount(ARButton, { props: { block: true } })
      expect(wrapper.find('.ar-button--block').exists()).toBe(true)
    })

    it('带图标按钮应该显示图标', () => {
      const wrapper = mount(ARButton, {
        props: { icon: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z' }
      })
      expect(wrapper.find('.ar-button__icon').exists()).toBe(true)
    })
  })

  // 可访问性测试
  describe('可访问性', () => {
    it('禁用按钮应该有 disabled 属性', () => {
      const wrapper = mount(ARButton, { props: { disabled: true } })
      expect(wrapper.find('button').attributes('disabled')).toBeDefined()
    })

    it('按钮应该支持键盘焦点', () => {
      const wrapper = mount(ARButton)
      const button = wrapper.find('button')
      expect(button.element.tagName).toBe('BUTTON')
    })
  })
})