/**
 * AI-Ready Vue组件测试
 */
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'

// =====================
// 按钮组件测试
// =====================
describe('Button组件', () => {
  const ButtonComponent = {
    template: `
      <button 
        :class="['btn', type ? 'btn-' + type : '']" 
        :disabled="loading || disabled"
        @click="handleClick"
      >
        <span v-if="loading" class="loading">加载中...</span>
        <slot v-else />
      </button>
    `,
    props: {
      type: { type: String, default: 'default' },
      loading: { type: Boolean, default: false },
      disabled: { type: Boolean, default: false }
    },
    emits: ['click'],
    methods: {
      handleClick(e: Event) {
        if (!this.loading && !this.disabled) {
          this.$emit('click', e)
        }
      }
    }
  }
  
  it('渲染默认按钮', () => {
    const wrapper = mount(ButtonComponent, {
      slots: { default: '点击' }
    })
    expect(wrapper.text()).toBe('点击')
    expect(wrapper.classes()).toContain('btn')
  })
  
  it('渲染不同类型按钮', () => {
    const primary = mount(ButtonComponent, { props: { type: 'primary' } })
    expect(primary.classes()).toContain('btn-primary')
    
    const danger = mount(ButtonComponent, { props: { type: 'danger' } })
    expect(danger.classes()).toContain('btn-danger')
  })
  
  it('loading状态禁用按钮', () => {
    const wrapper = mount(ButtonComponent, {
      props: { loading: true },
      slots: { default: '点击' }
    })
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
    expect(wrapper.find('.loading').exists()).toBe(true)
  })
  
  it('disabled属性禁用按钮', () => {
    const wrapper = mount(ButtonComponent, {
      props: { disabled: true }
    })
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })
  
  it('点击触发事件', async () => {
    const wrapper = mount(ButtonComponent, {
      slots: { default: '点击' }
    })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })
  
  it('loading状态不触发点击', async () => {
    const wrapper = mount(ButtonComponent, {
      props: { loading: true }
    })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toBeFalsy()
  })
})

// =====================
// 输入框组件测试
// =====================
describe('Input组件', () => {
  const InputComponent = {
    template: `
      <div class="input-wrapper">
        <input 
          :value="modelValue"
          :type="type"
          :placeholder="placeholder"
          :disabled="disabled"
          @input="$emit('update:modelValue', $event.target.value)"
        />
        <span v-if="error" class="error">{{ error }}</span>
      </div>
    `,
    props: {
      modelValue: { type: String, default: '' },
      type: { type: String, default: 'text' },
      placeholder: { type: String, default: '' },
      disabled: { type: Boolean, default: false },
      error: { type: String, default: '' }
    },
    emits: ['update:modelValue']
  }
  
  it('渲染输入框', () => {
    const wrapper = mount(InputComponent)
    expect(wrapper.find('input').exists()).toBe(true)
  })
  
  it('绑定v-model', async () => {
    const wrapper = mount(InputComponent, {
      props: { modelValue: 'test' }
    })
    expect(wrapper.find('input').element.value).toBe('test')
    
    await wrapper.find('input').setValue('new value')
    expect(wrapper.emitted('update:modelValue')![0]).toEqual(['new value'])
  })
  
  it('显示placeholder', () => {
    const wrapper = mount(InputComponent, {
      props: { placeholder: '请输入' }
    })
    expect(wrapper.find('input').attributes('placeholder')).toBe('请输入')
  })
  
  it('显示错误信息', () => {
    const wrapper = mount(InputComponent, {
      props: { error: '输入有误' }
    })
    expect(wrapper.find('.error').exists()).toBe(true)
    expect(wrapper.find('.error').text()).toBe('输入有误')
  })
  
  it('禁用状态', () => {
    const wrapper = mount(InputComponent, {
      props: { disabled: true }
    })
    expect(wrapper.find('input').attributes('disabled')).toBeDefined()
  })
})

// =====================
// 表格组件测试
// =====================
describe('Table组件', () => {
  const TableComponent = {
    template: `
      <table class="table">
        <thead>
          <tr>
            <th v-for="col in columns" :key="col.key">{{ col.title }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in data" :key="index">
            <td v-for="col in columns" :key="col.key">{{ row[col.key] }}</td>
          </tr>
        </tbody>
      </table>
    `,
    props: {
      columns: { type: Array, default: () => [] },
      data: { type: Array, default: () => [] }
    }
  }
  
  const columns = [
    { key: 'name', title: '姓名' },
    { key: 'age', title: '年龄' }
  ]
  
  const data = [
    { name: '张三', age: 25 },
    { name: '李四', age: 30 }
  ]
  
  it('渲染表头', () => {
    const wrapper = mount(TableComponent, {
      props: { columns, data: [] }
    })
    const headers = wrapper.findAll('th')
    expect(headers).toHaveLength(2)
    expect(headers[0].text()).toBe('姓名')
    expect(headers[1].text()).toBe('年龄')
  })
  
  it('渲染数据行', () => {
    const wrapper = mount(TableComponent, {
      props: { columns, data }
    })
    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(2)
  })
  
  it('渲染单元格数据', () => {
    const wrapper = mount(TableComponent, {
      props: { columns, data }
    })
    const cells = wrapper.findAll('tbody td')
    expect(cells[0].text()).toBe('张三')
    expect(cells[1].text()).toBe('25')
    expect(cells[2].text()).toBe('李四')
    expect(cells[3].text()).toBe('30')
  })
})

// =====================
// 模态框组件测试
// =====================
describe('Modal组件', () => {
  const ModalComponent = {
    template: `
      <Teleport to="body">
        <div v-if="visible" class="modal-overlay" @click.self="handleCancel">
          <div class="modal-content">
            <div class="modal-header">
              <h3>{{ title }}</h3>
              <button class="close-btn" @click="handleCancel">×</button>
            </div>
            <div class="modal-body">
              <slot />
            </div>
            <div class="modal-footer">
              <button class="btn-cancel" @click="handleCancel">取消</button>
              <button class="btn-confirm" @click="handleConfirm">确定</button>
            </div>
          </div>
        </div>
      </Teleport>
    `,
    props: {
      visible: { type: Boolean, default: false },
      title: { type: String, default: '提示' }
    },
    emits: ['update:visible', 'confirm', 'cancel'],
    methods: {
      handleCancel() {
        this.$emit('update:visible', false)
        this.$emit('cancel')
      },
      handleConfirm() {
        this.$emit('confirm')
        this.$emit('update:visible', false)
      }
    }
  }
  
  it('visible为false时不渲染', () => {
    const wrapper = mount(ModalComponent, {
      props: { visible: false }
    })
    expect(wrapper.find('.modal-overlay').exists()).toBe(false)
  })
  
  it('visible为true时渲染', () => {
    const wrapper = mount(ModalComponent, {
      props: { visible: true }
    })
    expect(wrapper.find('.modal-overlay').exists()).toBe(true)
  })
  
  it('显示标题', () => {
    const wrapper = mount(ModalComponent, {
      props: { visible: true, title: '确认删除' }
    })
    expect(wrapper.find('h3').text()).toBe('确认删除')
  })
  
  it('点击取消按钮关闭', async () => {
    const wrapper = mount(ModalComponent, {
      props: { visible: true }
    })
    await wrapper.find('.btn-cancel').trigger('click')
    expect(wrapper.emitted('cancel')).toBeTruthy()
    expect(wrapper.emitted('update:visible')![0]).toEqual([false])
  })
  
  it('点击确定按钮确认', async () => {
    const wrapper = mount(ModalComponent, {
      props: { visible: true }
    })
    await wrapper.find('.btn-confirm').trigger('click')
    expect(wrapper.emitted('confirm')).toBeTruthy()
  })
})

// =====================
// 分页组件测试
// =====================
describe('Pagination组件', () => {
  const PaginationComponent = {
    template: `
      <div class="pagination">
        <button class="prev" :disabled="current === 1" @click="changePage(current - 1)">上一页</button>
        <button 
          v-for="p in pages" 
          :key="p" 
          :class="['page', { active: p === current }]"
          @click="changePage(p)"
        >{{ p }}</button>
        <button class="next" :disabled="current === total" @click="changePage(current + 1)">下一页</button>
      </div>
    `,
    props: {
      current: { type: Number, default: 1 },
      total: { type: Number, default: 1 }
    },
    emits: ['change'],
    computed: {
      pages() {
        return Array.from({ length: this.total }, (_, i) => i + 1)
      }
    },
    methods: {
      changePage(page: number) {
        if (page >= 1 && page <= this.total) {
          this.$emit('change', page)
        }
      }
    }
  }
  
  it('渲染页码按钮', () => {
    const wrapper = mount(PaginationComponent, {
      props: { current: 1, total: 5 }
    })
    const pageButtons = wrapper.findAll('.page')
    expect(pageButtons).toHaveLength(5)
  })
  
  it('当前页高亮', () => {
    const wrapper = mount(PaginationComponent, {
      props: { current: 2, total: 5 }
    })
    const buttons = wrapper.findAll('.page')
    expect(buttons[1].classes()).toContain('active')
  })
  
  it('第一页禁用上一页按钮', () => {
    const wrapper = mount(PaginationComponent, {
      props: { current: 1, total: 5 }
    })
    expect(wrapper.find('.prev').attributes('disabled')).toBeDefined()
  })
  
  it('最后一页禁用下一页按钮', () => {
    const wrapper = mount(PaginationComponent, {
      props: { current: 5, total: 5 }
    })
    expect(wrapper.find('.next').attributes('disabled')).toBeDefined()
  })
  
  it('点击页码触发change事件', async () => {
    const wrapper = mount(PaginationComponent, {
      props: { current: 1, total: 5 }
    })
    await wrapper.findAll('.page')[2].trigger('click')
    expect(wrapper.emitted('change')![0]).toEqual([3])
  })
})