import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ARCard from '../../frontend/src/components/@ai-ready/mobile-ui/components/ARCard/ARCard.vue'

describe('ARCard 组件测试', () => {
  // 功能测试
  describe('基本功能', () => {
    it('应该正确渲染卡片', () => {
      const wrapper = mount(ARCard, {
        slots: {
          default: '卡片内容'
        }
      })
      expect(wrapper.find('.ar-card').exists()).toBe(true)
      expect(wrapper.text()).toContain('卡片内容')
    })

    it('应该显示标题和副标题', () => {
      const wrapper = mount(ARCard, {
        props: {
          title: '卡片标题',
          subtitle: '副标题内容'
        }
      })
      expect(wrapper.find('.ar-card__title').text()).toBe('卡片标题')
      expect(wrapper.find('.ar-card__subtitle').text()).toBe('副标题内容')
    })

    it('应该显示封面图片', () => {
      const wrapper = mount(ARCard, {
        props: {
          cover: 'https://example.com/image.jpg'
        }
      })
      expect(wrapper.find('.ar-card__cover').exists()).toBe(true)
      expect(wrapper.find('img').attributes('src')).toBe('https://example.com/image.jpg')
    })

    it('应该显示额外信息', () => {
      const wrapper = mount(ARCard, {
        props: {
          extra: '额外信息'
        }
      })
      expect(wrapper.find('.ar-card__extra').text()).toBe('额外信息')
    })

    it('应该支持不同阴影级别', () => {
      const shadows = ['none', 'sm', 'md', 'lg']
      shadows.forEach(shadow => {
        const wrapper = mount(ARCard, { props: { shadow } })
        if (shadow !== 'none') {
          expect(wrapper.find(`.ar-card--shadow-${shadow}`).exists()).toBe(true)
        }
      })
    })
  })

  // 状态测试
  describe('状态管理', () => {
    it('边框卡片应该有 bordered 类名', () => {
      const wrapper = mount(ARCard, { props: { bordered: true } })
      expect(wrapper.find('.ar-card--bordered').exists()).toBe(true)
    })

    it('hoverable 卡片应该有对应类名', () => {
      const wrapper = mount(ARCard, { props: { hoverable: true } })
      expect(wrapper.find('.ar-card--hoverable').exists()).toBe(true)
    })

    it('loading 状态应该禁用交互', () => {
      const wrapper = mount(ARCard, { props: { loading: true } })
      expect(wrapper.find('.ar-card--loading').exists()).toBe(true)
    })
  })

  // 交互测试
  describe('交互行为', () => {
    it('点击卡片应该触发事件', async () => {
      const wrapper = mount(ARCard, { props: { hoverable: true } })
      await wrapper.trigger('click')
      expect(wrapper.emitted('click')).toBeTruthy()
    })

    it('loading 状态不应触发点击', async () => {
      const wrapper = mount(ARCard, { props: { loading: true } })
      await wrapper.trigger('click')
      expect(wrapper.emitted('click')).toBeFalsy()
    })

    it('操作按钮应该触发 action 事件', async () => {
      const wrapper = mount(ARCard, {
        props: {
          actions: [
            { text: '操作1', type: 'primary' },
            { text: '操作2', type: 'default' }
          ]
        }
      })
      const buttons = wrapper.findAll('.ar-card__action')
      await buttons[0].trigger('click')
      expect(wrapper.emitted('action')).toBeTruthy()
      expect(wrapper.emitted('action')![0][0]).toBe(0)
    })
  })

  // Slots 测试
  describe('插槽支持', () => {
    it('应该支持 header 插槽', () => {
      const wrapper = mount(ARCard, {
        slots: {
          header: '<div class="custom-header">自定义头部</div>'
        }
      })
      expect(wrapper.find('.custom-header').exists()).toBe(true)
    })

    it('应该支持 cover 插槽', () => {
      const wrapper = mount(ARCard, {
        slots: {
          cover: '<div class="custom-cover">自定义封面</div>'
        }
      })
      expect(wrapper.find('.custom-cover').exists()).toBe(true)
    })

    it('应该支持 footer 插槽', () => {
      const wrapper = mount(ARCard, {
        slots: {
          footer: '<div class="custom-footer">自定义底部</div>'
        }
      })
      expect(wrapper.find('.custom-footer').exists()).toBe(true)
    })
  })

  // 可访问性测试
  describe('可访问性', () => {
    it('操作按钮应该可聚焦', () => {
      const wrapper = mount(ARCard, {
        props: {
          actions: [{ text: '操作' }]
        }
      })
      const button = wrapper.find('.ar-card__action')
      expect(button.element.tagName).toBe('BUTTON')
    })

    it('禁用操作按钮应该有 disabled 属性', () => {
      const wrapper = mount(ARCard, {
        props: {
          actions: [{ text: '操作', disabled: true }]
        }
      })
      const button = wrapper.find('.ar-card__action')
      expect(button.attributes('disabled')).toBeDefined()
    })
  })
})