import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ExchangeApproveModal from '../components/ExchangeApproveModal.vue'
import { ExchangeStatus } from '@/api/purchase-exchange'

// Mock API
vi.mock('@/api/purchase-exchange', () => ({
  purchaseExchangeApi: {
    approve: vi.fn(() => Promise.resolve({})),
    reject: vi.fn(() => Promise.resolve({}))
  },
  ExchangeStatus
}))

// Mock ant-design-vue
vi.mock('ant-design-vue', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

describe('ExchangeApproveModal', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders modal with record data', () => {
    const mockRecord = {
      id: 1,
      exchangeNo: 'EX202604250001',
      originalOrderNo: 'PO202604250001',
      supplierName: '供应商A',
      exchangeDate: '2026-04-25',
      exchangeType: 1,
      exchangeReason: '质量问题',
      totalAmount: 5000,
      remark: '备注',
      status: ExchangeStatus.PENDING_APPROVAL
    }

    const wrapper = mount(ExchangeApproveModal, {
      props: {
        open: true,
        record: mockRecord as any
      }
    })

    expect(wrapper.find('a-modal').exists()).toBe(true)
    expect(wrapper.find('.approve-info').exists()).toBe(true)
  })

  it('displays correct exchange type text', () => {
    const mockRecord = {
      id: 1,
      exchangeNo: 'EX202604250001',
      originalOrderNo: 'PO202604250001',
      supplierName: '供应商A',
      exchangeDate: '2026-04-25',
      exchangeType: 1,
      exchangeReason: '质量问题',
      totalAmount: 5000,
      remark: '',
      status: ExchangeStatus.PENDING_APPROVAL
    }

    const wrapper = mount(ExchangeApproveModal, {
      props: {
        open: true,
        record: mockRecord as any
      }
    })

    const vm = wrapper.vm as any
    expect(vm.getExchangeTypeText(1)).toBe('质量问题')
    expect(vm.getExchangeTypeText(2)).toBe('规格不符')
    expect(vm.getExchangeTypeText(3)).toBe('数量错误')
    expect(vm.getExchangeTypeText(4)).toBe('其他')
  })

  it('defaults to approve', () => {
    const wrapper = mount(ExchangeApproveModal, {
      props: {
        open: true,
        record: {
          id: 1,
          exchangeNo: 'EX202604250001',
          status: ExchangeStatus.PENDING_APPROVAL
        } as any
      }
    })

    const vm = wrapper.vm as any
    expect(vm.formData.approved).toBe(true)
  })

  it('resets form on cancel', async () => {
    const wrapper = mount(ExchangeApproveModal, {
      props: {
        open: true,
        record: {
          id: 1,
          exchangeNo: 'EX202604250001',
          status: ExchangeStatus.PENDING_APPROVAL
        } as any
      }
    })

    const vm = wrapper.vm as any
    vm.formData.approved = false
    vm.formData.remark = '测试备注'

    vm.handleCancel()
    await flushPromises()

    expect(vm.formData.approved).toBe(true)
    expect(vm.formData.remark).toBe('')
  })
})
