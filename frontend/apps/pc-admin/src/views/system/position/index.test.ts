import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import PositionIndex from '@/views/system/position/index.vue'
import { positionApi } from '@/api/position'

// Mock API
vi.mock('@/api/position', () => ({
  positionApi: {
    getPage: vi.fn(),
    getCategoryList: vi.fn(),
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

describe('Position Management', () => {
  let wrapper: any

  beforeEach(() => {
    const pinia = createPinia()
    wrapper = mount(PositionIndex, {
      global: {
        plugins: [pinia]
      }
    })
  })

  describe('Initialization', () => {
    it('should mount component', () => {
      expect(wrapper.exists()).toBe(true)
    })

    it('should render search form', () => {
      expect(wrapper.find('.search-card').exists()).toBe(true)
      expect(wrapper.find('.search-form').exists()).toBe(true)
    })

    it('should render table', () => {
      expect(wrapper.find('.table-card').exists()).toBe(true)
      expect(wrapper.find('.ant-table').exists()).toBe(true)
    })

    it('should render action buttons', () => {
      expect(wrapper.find('.ant-btn-primary').exists()).toBe(true)
      expect(wrapper.text()).toContain('新增岗位')
      expect(wrapper.text()).toContain('分类管理')
    })
  })

  describe('Data Loading', () => {
    it('should load position data on mount', async () => {
      const mockData = {
        code: 200,
        data: {
          records: [
            {
              id: 1,
              positionCode: 'DEV001',
              positionName: '前端开发工程师',
              level: 2,
              status: 0
            }
          ],
          total: 1,
          current: 1,
          size: 10
        }
      }

      vi.mocked(positionApi.getPage).mockResolvedValue(mockData as any)

      // 重新加载组件
      wrapper.unmount()
      wrapper = mount(PositionIndex, {
        global: {
          plugins: [createPinia()]
        }
      })

      await new Promise(resolve => setTimeout(resolve, 100))

      expect(positionApi.getPage).toHaveBeenCalled()
    })

    it('should load category list on mount', async () => {
      const mockCategories = {
        code: 200,
        data: [
          {
            id: 1,
            categoryCode: 'TECH',
            categoryName: '技术类',
            status: 0
          }
        ]
      }

      vi.mocked(positionApi.getCategoryList).mockResolvedValue(mockCategories as any)

      wrapper.unmount()
      wrapper = mount(PositionIndex, {
        global: {
          plugins: [createPinia()]
        }
      })

      await new Promise(resolve => setTimeout(resolve, 100))

      expect(positionApi.getCategoryList).toHaveBeenCalled()
    })
  })

  describe('Search Functionality', () => {
    it('should handle search', async () => {
      const searchButton = wrapper.find('.search-card .ant-btn-primary')
      await searchButton.trigger('click')
      expect(positionApi.getPage).toHaveBeenCalled()
    })

    it('should handle reset', async () => {
      const resetButton = wrapper.findAll('.search-card .ant-btn')[1]
      await resetButton.trigger('click')
      
      // 检查搜索表单是否被重置
      // 这里需要访问组件的 searchForm 实例
      // 由于响应式系统，我们可以通过 wrapper.vm 访问
      expect(wrapper.vm.searchForm.positionName).toBe('')
      expect(wrapper.vm.searchForm.positionCode).toBe('')
    })
  })

  describe('Modal Operations', () => {
    it('should open add modal when clicking add button', async () => {
      const addButton = wrapper.find('.table-header .ant-btn-primary')
      await addButton.trigger('click')
      
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
      wrapper.vm.modalVisible = true
      wrapper.vm.isEdit = false
      wrapper.vm.formState = {
        positionCode: 'DEV002',
        positionName: '后端开发工程师',
        level: 2,
        status: 0
      }

      vi.mocked(positionApi.create).mockResolvedValue({ code: 200, data: true } as any)

      await wrapper.vm.handleModalOk()
      expect(positionApi.create).toHaveBeenCalled()
      expect(wrapper.vm.modalVisible).toBe(false)
    })
  })

  describe('Category Management', () => {
    it('should open category modal', async () => {
      const categoryButton = wrapper.findAll('.table-header .ant-btn')[1]
      await categoryButton.trigger('click')
      
      await wrapper.vm.$nextTick()
      expect(wrapper.vm.categoryModalVisible).toBe(true)
    })

    it('should add new category', async () => {
      wrapper.vm.categoryFormModalVisible = true
      wrapper.vm.isCategoryEdit = false
      wrapper.vm.categoryFormState = {
        categoryCode: 'SALES',
        categoryName: '销售类',
        status: 0
      }

      vi.mocked(positionApi.createCategory).mockResolvedValue({ code: 200, data: true } as any)

      await wrapper.vm.handleCategoryFormModalOk()
      expect(positionApi.createCategory).toHaveBeenCalled()
      expect(wrapper.vm.categoryFormModalVisible).toBe(false)
    })
  })

  describe('Delete Operations', () => {
    it('should show confirmation before delete', async () => {
      const positionData = {
        id: 1,
        positionCode: 'DEV001',
        positionName: '前端开发工程师'
      }

      // Mock Modal.confirm
      const mockConfirm = vi.fn()
      mockConfirm.mockImplementation(({ onOk }) => {
        onOk && onOk()
      })
      (window as any).Modal = { confirm: mockConfirm } as any

      vi.mocked(positionApi.delete).mockResolvedValue({ code: 200, data: true } as any)

      await wrapper.vm.handleDelete(positionData)
      
      // 验证删除 API 被调用
      expect(positionApi.delete).toHaveBeenCalledWith(1)
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
      wrapper.vm.departmentModalVisible = true
      wrapper.vm.currentPositionId = 1
      wrapper.vm.targetDepartmentId = 10

      vi.mocked(positionApi.update).mockResolvedValue({ code: 200, data: true } as any)

      await wrapper.vm.handleDepartmentModalOk()
      expect(positionApi.update).toHaveBeenCalledWith(1, { departmentId: 10 })
      expect(wrapper.vm.departmentModalVisible).toBe(false)
    })
  })

  describe('Helper Functions', () => {
    it('should return correct level color', () => {
      expect(wrapper.vm.getLevelColor(1)).toBe('green')
      expect(wrapper.vm.getLevelColor(2)).toBe('blue')
      expect(wrapper.vm.getLevelColor(3)).toBe('orange')
      expect(wrapper.vm.getLevelColor(4)).toBe('red')
      expect(wrapper.vm.getLevelColor(5)).toBe('purple')
    })

    it('should return correct level name', () => {
      expect(wrapper.vm.getLevelName(1)).toBe('初级')
      expect(wrapper.vm.getLevelName(2)).toBe('中级')
      expect(wrapper.vm.getLevelName(3)).toBe('高级')
      expect(wrapper.vm.getLevelName(4)).toBe('专家')
      expect(wrapper.vm.getLevelName(5)).toBe('首席')
    })
  })

  describe('Responsive Design', () => {
    it('should have responsive search form', () => {
      const searchForm = wrapper.find('.search-form')
      expect(searchForm.classes()).toContain('search-form')
    })

    it('should have responsive table header', () => {
      const tableHeader = wrapper.find('.table-header')
      expect(tableHeader.exists()).toBe(true)
    })
  })
})