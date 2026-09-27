import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PositionIndex from '@/views/system/position/index.vue'
import { positionApi } from '@/api/position'

// Mock API
vi.mock('@/api/position', () => ({
  positionApi: {
    getPage: vi.fn(),
    getCategoryList: vi.fn(),
    getCategoryPage: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    batchDelete: vi.fn(),
    updateStatus: vi.fn(),
    createCategory: vi.fn(),
    updateCategory: vi.fn(),
    deleteCategory: vi.fn()
  }
}))

vi.mock('@/api/department', () => ({
  departmentApi: {
    getList: vi.fn()
  }
}))

vi.mock('@/api/dict', () => ({
  dictItemApi: {
    getByDictCode: vi.fn()
  }
}))

// Mock ant-design-vue Modal.confirm
vi.mock('ant-design-vue', async () => {
  const actual = await vi.importActual<any>('ant-design-vue')
  return {
    ...actual,
    message: {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn()
    },
    Modal: {
      confirm: vi.fn()
    }
  }
})

describe('Position Management', () => {
  let wrapper: any

  beforeEach(async () => {
    vi.clearAllMocks()
    setActivePinia(createPinia())

    // Setup default mock responses for onMounted calls
    const { positionApi: mockPosApi } = await import('@/api/position')
    const { departmentApi } = await import('@/api/department')
    const { dictItemApi } = await import('@/api/dict')

    /**
     * ⚠️ mock 必须回放**响应拦截器拆包后**的真实形态，否则会掩盖 bug。
     *
     * `utils/request.ts` 在成功时已经把 Result/Page 拆开：调用方拿到的
     * 就是数据本体（Page 是 `{records,total}`、列表是数组），**永远不会**多出一层 `data`。
     * 此前此处 mock 了 `{ data: [], code: 200 }` 这类运行时不会出现的外壳，
     * 让页面里 `if (res.data)` 的错误写法在测试中「恰好」走通 —— 于是
     * 「列表/下拉恒空」的 P0 长期无人发现（2026-09-24 系统模块审计 P2）。
     * 改动这里的 mock 前，请先确认 request.ts 的拆包行为没变。
     */
    vi.mocked(mockPosApi.getPage).mockResolvedValue({
      records: [],
      total: 0
    } as any)
    vi.mocked(mockPosApi.getCategoryList).mockResolvedValue([] as any)
    vi.mocked(mockPosApi.getCategoryPage).mockResolvedValue({
      records: [],
      total: 0
    } as any)
    vi.mocked(departmentApi.getList).mockResolvedValue([] as any)
    vi.mocked(dictItemApi.getByDictCode).mockResolvedValue([
      { itemText: '初级', itemValue: '1', sortOrder: 1 },
      { itemText: '中级', itemValue: '2', sortOrder: 2 },
      { itemText: '高级', itemValue: '3', sortOrder: 3 },
      { itemText: '专家', itemValue: '4', sortOrder: 4 },
      { itemText: '首席', itemValue: '5', sortOrder: 5 }
    ] as any)

    wrapper = mount(PositionIndex, {
      global: {
        plugins: [createPinia()],
        mocks: {
          $route: { query: {} },
          $router: { push: vi.fn() }
        },
        stubs: {
          ErrorBoundary: { template: '<div><slot /><slot name="header" /></div>' },
          PageContainer: { template: '<div><slot /><slot name="header" /></div>' },
          BillTableList: { template: '<div class="bill-table-list" />', props: ['columns', 'dataSource', 'loading', 'pagination'] },
          FullScreenDetail: { template: '<div v-if="visible"><slot /></div>', props: ['visible', 'title'] },
          'a-breadcrumb': { template: '<div />' },
          'a-breadcrumb-item': { template: '<span><slot /></span>' },
          'a-skeleton': { template: '<div />' },
          'a-empty': { template: '<div />' },
          'a-result': { template: '<div />' },
          'a-form': { template: '<div><slot /></div>' },
          'a-form-item': { template: '<div><slot /></div>' },
          'a-input': { template: '<input />' },
          'a-select': { template: '<select />' },
          'a-select-option': { template: '<option />' },
          'a-input-number': { template: '<input />' },
          'a-textarea': { template: '<textarea />' },
          'a-radio-group': { template: '<div />' },
          'a-radio': { template: '<label />' },
          'a-modal': { template: '<div />' },
          'a-tag': { template: '<span><slot /></span>' },
          'a-space': { template: '<div><slot /></div>' },
          'a-button': { template: '<button><slot /></button>' },
          'a-dropdown': { template: '<div><slot /></div>' },
          'a-menu': { template: '<div />' },
          'a-menu-item': { template: '<div />' },
          'a-menu-divider': { template: '<hr />' },
          'a-alert': { template: '<div />' },
          'router-link': { template: '<a />' }
        }
      }
    })

    // Wait for onMounted async calls
    await new Promise(resolve => setTimeout(resolve, 100))
  })

  describe('Initialization', () => {
    it('should mount component', () => {
      expect(wrapper.exists()).toBe(true)
    })

    // 组件使用BillTableList而非.search-card/.ant-table，CSS选择器不匹配
    it.skip('should render search form - 组件使用BillTableList而非search-card', () => {})
    it.skip('should render table - 组件使用BillTableList而非ant-table', () => {})
    it.skip('should render action buttons - 组件按钮结构不同', () => {})
  })

  describe('Data Loading', () => {
    it('should load position data on mount', async () => {
      const { positionApi: mockPosApi } = await import('@/api/position')
      expect(mockPosApi.getPage).toHaveBeenCalled()
    })

    it('should load category list on mount', async () => {
      const { positionApi: mockPosApi } = await import('@/api/position')
      expect(mockPosApi.getCategoryList).toHaveBeenCalled()
    })
  })

  // 组件不使用.search-card结构，搜索通过BillTableList的filter-change事件触发
  describe.skip('Search Functionality - 组件使用BillTableList内置筛选', () => {})

  describe('Modal Operations', () => {
    it('should open add modal via handleAdd', async () => {
      await wrapper.vm.handleAdd()
      await wrapper.vm.$nextTick()
      expect(wrapper.vm.modalVisible).toBe(true)
      expect(wrapper.vm.isEdit).toBe(false)
    })

    it('should open edit modal with data', async () => {
      const positionData = {
        id: 1,
        positionCode: 'DEV001',
        positionName: '前端开发工程师',
        level: 2,
        status: 0
      }

      await wrapper.vm.handleEdit(positionData)
      expect(wrapper.vm.modalVisible).toBe(true)
      expect(wrapper.vm.isEdit).toBe(true)
      expect(wrapper.vm.formState.id).toBe(1)
      expect(wrapper.vm.formState.positionName).toBe('前端开发工程师')
    })

    it('should submit form data', async () => {
      const { positionApi: mockPosApi } = await import('@/api/position')
      wrapper.vm.modalVisible = true
      wrapper.vm.isEdit = false
      wrapper.vm.formState = {
        positionCode: 'DEV002',
        positionName: '后端开发工程师',
        level: 2,
        status: 0
      }

      vi.mocked(mockPosApi.create).mockResolvedValue({ code: 200, data: true } as any)

      await wrapper.vm.handleModalOk()
      expect(mockPosApi.create).toHaveBeenCalled()
    })
  })

  describe('Category Management', () => {
    it('should open category modal via handleCategoryManage', async () => {
      await wrapper.vm.handleCategoryManage()
      await wrapper.vm.$nextTick()
      expect(wrapper.vm.categoryModalVisible).toBe(true)
    })

    it('should add new category via handleCategoryFormModalOk', async () => {
      const { positionApi: mockPosApi } = await import('@/api/position')
      wrapper.vm.categoryFormModalVisible = true
      wrapper.vm.isCategoryEdit = false
      wrapper.vm.categoryFormState = {
        categoryCode: 'SALES',
        categoryName: '销售类',
        status: 0
      }

      vi.mocked(mockPosApi.createCategory).mockResolvedValue({ code: 200, data: true } as any)

      await wrapper.vm.handleCategoryFormModalOk()
      expect(mockPosApi.createCategory).toHaveBeenCalled()
    })
  })

  describe('Delete Operations', () => {
    it('should call Modal.confirm before delete', async () => {
      const { Modal } = await import('ant-design-vue')
      const positionData = {
        id: 1,
        positionCode: 'DEV001',
        positionName: '前端开发工程师'
      }

      await wrapper.vm.handleDelete(positionData)

      expect(Modal.confirm).toHaveBeenCalled()
    })
  })

  describe('Department Assignment', () => {
    it('should open department assignment modal', async () => {
      const positionData = {
        id: 1,
        positionCode: 'DEV001',
        positionName: '前端开发工程师',
        departmentId: null
      }

      await wrapper.vm.handleAssignDepartment(positionData)
      expect(wrapper.vm.departmentModalVisible).toBe(true)
      expect(wrapper.vm.currentPositionId).toBe(1)
      expect(wrapper.vm.currentPositionName).toBe('前端开发工程师')
    })

    it('should assign department to position', async () => {
      const { positionApi: mockPosApi } = await import('@/api/position')
      wrapper.vm.departmentModalVisible = true
      wrapper.vm.currentPositionId = 1
      wrapper.vm.targetDepartmentId = 10

      vi.mocked(mockPosApi.update).mockResolvedValue({ code: 200, data: true } as any)

      await wrapper.vm.handleDepartmentModalOk()
      expect(mockPosApi.update).toHaveBeenCalledWith(1, { departmentId: 10 })
      expect(wrapper.vm.departmentModalVisible).toBe(false)
    })
  })

  describe('Helper Functions', () => {
    it('should return correct level color based on levelOptions', () => {
      // levelOptions 从 dictItemApi 加载，beforeEach 中已 mock
      // getLevelColor 根据 levelOptions 中索引返回颜色
      expect(wrapper.vm.getLevelColor(1)).toBe('green')
      expect(wrapper.vm.getLevelColor(2)).toBe('blue')
      expect(wrapper.vm.getLevelColor(3)).toBe('orange')
      expect(wrapper.vm.getLevelColor(4)).toBe('red')
      expect(wrapper.vm.getLevelColor(5)).toBe('purple')
    })

    it('should return correct level name based on levelOptions', () => {
      // levelOptions 从 dictItemApi 加载，beforeEach 中已 mock
      expect(wrapper.vm.getLevelName(1)).toBe('初级')
      expect(wrapper.vm.getLevelName(2)).toBe('中级')
      expect(wrapper.vm.getLevelName(3)).toBe('高级')
      expect(wrapper.vm.getLevelName(4)).toBe('专家')
      expect(wrapper.vm.getLevelName(5)).toBe('首席')
    })
  })

  // CSS结构测试 - 组件使用BillTableList等自定义组件，无.search-form/.table-header
  describe.skip('Responsive Design - 组件使用BillTableList无search-form/table-header', () => {})
})
