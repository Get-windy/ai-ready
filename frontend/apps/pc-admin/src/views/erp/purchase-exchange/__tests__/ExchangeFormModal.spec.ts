import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ExchangeFormModal from '../components/ExchangeFormModal.vue'
import { ExchangeStatus } from '@/api/purchase-exchange'

// Mock API
vi.mock('@/api/purchase-exchange', () => ({
  purchaseExchangeApi: {
    create: vi.fn(() => Promise.resolve({ data: 1 })),
    update: vi.fn(() => Promise.resolve({})),
    getItems: vi.fn(() => Promise.resolve({
      data: [
        {
          id: 1,
          originalItemId: 1,
          productId: 1,
          productName: '商品A',
          productCode: 'P001',
          originalQuantity: 100,
          exchangeQuantity: 10,
          originalPrice: 50,
          exchangePrice: 50,
          unit: '件',
          warehouseId: 1,
          warehouseName: '主仓库'
        }
      ]
    }))
  },
  ExchangeStatus
}))

vi.mock('@/api/purchase', () => ({
  purchaseOrderApi: {
    page: vi.fn(() => Promise.resolve({
      data: {
        records: [
          { id: 1, orderNo: 'PO202604250001', supplierName: '供应商A' }
        ]
      }
    })),
    get: vi.fn(() => Promise.resolve({
      data: { id: 1, orderNo: 'PO202604250001', supplierName: '供应商A' }
    }))
  }
}))

// Mock ant-design-vue
vi.mock('ant-design-vue', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

describe('ExchangeFormModal', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders create modal correctly', () => {
    const wrapper = mount(ExchangeFormModal, {
      props: {
        visible: true,
        record: null
      }
    })

    expect(wrapper.find('a-modal').exists()).toBe(true)
  })

  it('renders edit modal with record data', async () => {
    const mockRecord = {
      id: 1,
      exchangeNo: 'EX202604250001',
      originalOrderId: 1,
      supplierName: '供应商A',
      exchangeDate: '2026-04-25',
      exchangeType: 1,
      exchangeReason: '质量问题',
      remark: '备注',
      status: ExchangeStatus.DRAFT
    }

    const wrapper = mount(ExchangeFormModal, {
      props: {
        visible: true,
        record: mockRecord
      }
    })

    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.isEdit).toBe(true)
    expect(vm.formData.exchangeType).toBe(1)
    expect(vm.formData.exchangeReason).toBe('质量问题')
  })

  it('calculates total amount correctly', async () => {
    const wrapper = mount(ExchangeFormModal, {
      props: {
        visible: true,
        record: null
      }
    })

    const vm = wrapper.vm as any
    vm.formData.items = [
      { exchangeQuantity: 10, exchangePrice: 50 },
      { exchangeQuantity: 5, exchangePrice: 100 }
    ]

    await flushPromises()

    expect(vm.totalAmount).toBe(1000) // 10*50 + 5*100 = 1000
  })

  it('removes item correctly', async () => {
    const wrapper = mount(ExchangeFormModal, {
      props: {
        visible: true,
        record: null
      }
    })

    const vm = wrapper.vm as any
    vm.formData.items = [
      { id: 1, productName: '商品A' },
      { id: 2, productName: '商品B' }
    ]

    vm.removeItem(0)
    await flushPromises()

    expect(vm.formData.items.length).toBe(1)
    expect(vm.formData.items[0].productName).toBe('商品B')
  })

  it('resets form on cancel', async () => {
    const wrapper = mount(ExchangeFormModal, {
      props: {
        visible: true,
        record: null
      }
    })

    const vm = wrapper.vm as any
    vm.formData.exchangeType = 2
    vm.formData.exchangeReason = '测试'

    vm.handleCancel()
    await flushPromises()

    expect(vm.formData.exchangeType).toBe(1)
    expect(vm.formData.exchangeReason).toBe('')
  })
})
