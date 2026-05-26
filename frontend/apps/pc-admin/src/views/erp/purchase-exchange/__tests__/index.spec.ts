import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { message } from 'ant-design-vue'
import PurchaseExchangeIndex from '../index.vue'
import { ExchangeStatus } from '@/api/purchase-exchange'

// Mock API
vi.mock('@/api/purchase-exchange', () => ({
  purchaseExchangeApi: {
    page: vi.fn(() => Promise.resolve({
      data: {
        records: [
          {
            id: 1,
            exchangeNo: 'EX202604250001',
            originalOrderNo: 'PO202604250001',
            supplierName: '供应商A',
            exchangeDate: '2026-04-25',
            exchangeType: 1,
            totalAmount: 5000,
            status: ExchangeStatus.DRAFT,
            createdByName: '张三',
            createTime: '2026-04-25 10:00:00'
          }
        ],
        total: 1
      }
    })),
    submit: vi.fn(() => Promise.resolve({})),
    delete: vi.fn(() => Promise.resolve({}))
  },
  ExchangeStatus
}))

// Mock message
vi.mock('ant-design-vue', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn()
  }
}))

describe('PurchaseExchangeIndex', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders correctly', () => {
    const wrapper = mount(PurchaseExchangeIndex)
    expect(wrapper.find('.purchase-exchange-page').exists()).toBe(true)
    expect(wrapper.find('a-card').exists()).toBe(true)
  })

  it('displays correct status text', () => {
    const wrapper = mount(PurchaseExchangeIndex)
    const vm = wrapper.vm as any

    expect(vm.getStatusText(ExchangeStatus.DRAFT)).toBe('草稿')
    expect(vm.getStatusText(ExchangeStatus.PENDING_APPROVAL)).toBe('待审批')
    expect(vm.getStatusText(ExchangeStatus.APPROVED)).toBe('已审批')
    expect(vm.getStatusText(ExchangeStatus.EXCHANGING)).toBe('换货中')
    expect(vm.getStatusText(ExchangeStatus.COMPLETED)).toBe('已完成')
    expect(vm.getStatusText(ExchangeStatus.REJECTED)).toBe('已拒绝')
    expect(vm.getStatusText(ExchangeStatus.CANCELLED)).toBe('已取消')
  })

  it('displays correct status color', () => {
    const wrapper = mount(PurchaseExchangeIndex)
    const vm = wrapper.vm as any

    expect(vm.getStatusColor(ExchangeStatus.DRAFT)).toBe('default')
    expect(vm.getStatusColor(ExchangeStatus.PENDING_APPROVAL)).toBe('orange')
    expect(vm.getStatusColor(ExchangeStatus.APPROVED)).toBe('blue')
    expect(vm.getStatusColor(ExchangeStatus.EXCHANGING)).toBe('processing')
    expect(vm.getStatusColor(ExchangeStatus.COMPLETED)).toBe('success')
    expect(vm.getStatusColor(ExchangeStatus.REJECTED)).toBe('red')
    expect(vm.getStatusColor(ExchangeStatus.CANCELLED)).toBe('red')
  })

  it('displays correct exchange type text', () => {
    const wrapper = mount(PurchaseExchangeIndex)
    const vm = wrapper.vm as any

    expect(vm.getExchangeTypeText(1)).toBe('质量问题')
    expect(vm.getExchangeTypeText(2)).toBe('规格不符')
    expect(vm.getExchangeTypeText(3)).toBe('数量错误')
    expect(vm.getExchangeTypeText(4)).toBe('其他')
    expect(vm.getExchangeTypeText(999)).toBe('未知')
  })

  it('opens create modal when clicking create button', async () => {
    const wrapper = mount(PurchaseExchangeIndex)
    const vm = wrapper.vm as any

    vm.handleCreate()
    await flushPromises()

    expect(vm.formModalVisible).toBe(true)
    expect(vm.currentRecord).toBeNull()
  })

  it('opens edit modal with record data', async () => {
    const wrapper = mount(PurchaseExchangeIndex)
    const vm = wrapper.vm as any

    const mockRecord = {
      id: 1,
      exchangeNo: 'EX202604250001',
      status: ExchangeStatus.DRAFT
    }

    vm.handleEdit(mockRecord)
    await flushPromises()

    expect(vm.formModalVisible).toBe(true)
    expect(vm.currentRecord).toEqual(mockRecord)
  })

  it('opens detail modal', async () => {
    const wrapper = mount(PurchaseExchangeIndex)
    const vm = wrapper.vm as any

    const mockRecord = { id: 1, exchangeNo: 'EX202604250001' }
    vm.handleView(mockRecord)
    await flushPromises()

    expect(vm.detailModalVisible).toBe(true)
    expect(vm.currentRecord).toEqual(mockRecord)
  })

  it('opens approve modal for pending approval record', async () => {
    const wrapper = mount(PurchaseExchangeIndex)
    const vm = wrapper.vm as any

    const mockRecord = {
      id: 1,
      exchangeNo: 'EX202604250001',
      status: ExchangeStatus.PENDING_APPROVAL
    }

    vm.handleApprove(mockRecord)
    await flushPromises()

    expect(vm.approveModalVisible).toBe(true)
    expect(vm.currentRecord).toEqual(mockRecord)
  })

  it('opens track modal', async () => {
    const wrapper = mount(PurchaseExchangeIndex)
    const vm = wrapper.vm as any

    const mockRecord = { id: 1, exchangeNo: 'EX202604250001' }
    vm.handleTrack(mockRecord)
    await flushPromises()

    expect(vm.trackModalVisible).toBe(true)
    expect(vm.currentRecord).toEqual(mockRecord)
  })

  it('resets query params on reset', async () => {
    const wrapper = mount(PurchaseExchangeIndex)
    const vm = wrapper.vm as any

    vm.queryParams.exchangeNo = 'TEST'
    vm.queryParams.status = ExchangeStatus.DRAFT
    vm.queryParams.startDate = '2026-01-01'
    vm.queryParams.endDate = '2026-12-31'

    vm.handleReset()
    await flushPromises()

    expect(vm.queryParams.exchangeNo).toBe('')
    expect(vm.queryParams.status).toBeUndefined()
    expect(vm.queryParams.startDate).toBe('')
    expect(vm.queryParams.endDate).toBe('')
    expect(vm.dateRange).toBeNull()
  })

  it('handles date range change', async () => {
    const wrapper = mount(PurchaseExchangeIndex)
    const vm = wrapper.vm as any

    const dayjs = await import('dayjs')
    const startDate = dayjs.default('2026-01-01')
    const endDate = dayjs.default('2026-12-31')

    vm.handleDateChange([startDate, endDate])

    expect(vm.queryParams.startDate).toBe('2026-01-01')
    expect(vm.queryParams.endDate).toBe('2026-12-31')
  })

  it('handles date range clear', async () => {
    const wrapper = mount(PurchaseExchangeIndex)
    const vm = wrapper.vm as any

    vm.handleDateChange(null)

    expect(vm.queryParams.startDate).toBe('')
    expect(vm.queryParams.endDate).toBe('')
  })
})
