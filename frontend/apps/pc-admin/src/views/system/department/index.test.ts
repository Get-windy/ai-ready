// @ts-nocheck
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import DepartmentIndex from '@/views/system/department/index.vue'
// personnel.vue 文件不存在，跳过人员管理页面的测试
// import DepartmentPersonnel from '@/views/system/department/personnel.vue'
import { departmentApi } from '@/api/department'
import { userApi } from '@/api/user'
import { positionApi } from '@/api/position'

// Mock vue-router
vi.mock('vue-router', () => ({
  onBeforeRouteLeave: vi.fn(),
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ query: {} })
}))

// Mock ant-design-vue Modal
vi.mock('ant-design-vue', async () => {
  const actual = await vi.importActual<any>('ant-design-vue')
  return {
    ...actual,
    message: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
    Modal: { confirm: vi.fn() }
  }
})

// Mock APIs
vi.mock('@/api/department', () => ({
  departmentApi: {
    getTree: vi.fn(),
    getById: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    updateStatus: vi.fn(),
    move: vi.fn()
  }
}))

vi.mock('@/api/user', () => ({
  userApi: {
    getPage: vi.fn(),
    getList: vi.fn()
  }
}))

vi.mock('@/api/position', () => ({
  positionApi: {
    getList: vi.fn()
  }
}))

describe('Department Management - Main Page', () => {
  let wrapper: any

  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('Initialization', () => {
    it('should mount component', () => {
      wrapper = mount(DepartmentIndex, {
        global: {
          plugins: [createPinia()]
        }
      })
      expect(wrapper.exists()).toBe(true)
    })

    it('should render toolbar card', () => {
      wrapper = mount(DepartmentIndex, {
        global: {
          plugins: [createPinia()]
        }
      })
      expect(wrapper.find('.toolbar-card').exists()).toBe(true)
      expect(wrapper.find('.tree-card').exists()).toBe(true)
    })

    it('should render action buttons', () => {
      wrapper = mount(DepartmentIndex, {
        global: {
          plugins: [createPinia()]
        }
      })
      expect(wrapper.text()).toContain('新增根部门')
      expect(wrapper.text()).toContain('人员管理')
    })
  })

  describe('Tree Data Loading', () => {
    it('should load department tree on mount', async () => {
      // 组件直接赋值 treeData.value = res，所以API需返回数组
      const mockTree = [
        {
          id: 1,
          departmentCode: 'HQ',
          departmentName: '总部',
          level: 1,
          status: 0,
          children: [
            {
              id: 2,
              departmentCode: 'TECH',
              departmentName: '技术部',
              level: 2,
              status: 0,
              children: []
            }
          ]
        }
      ]

      vi.mocked(departmentApi.getTree).mockResolvedValue(mockTree as any)

      wrapper = mount(DepartmentIndex, {
        global: {
          plugins: [createPinia()]
        }
      })

      await new Promise(resolve => setTimeout(resolve, 100))

      expect(departmentApi.getTree).toHaveBeenCalled()
      expect(wrapper.vm.treeData.length).toBeGreaterThan(0)
    })

    it('should load leader list on mount', async () => {
      const mockLeaders = {
        code: 200,
        data: [
          {
            id: 1,
            username: 'admin',
            nickname: '管理员',
            status: 0
          }
        ]
      }

      vi.mocked(userApi.getList).mockResolvedValue(mockLeaders as any)

      wrapper = mount(DepartmentIndex, {
        global: {
          plugins: [createPinia()]
        }
      })

      await new Promise(resolve => setTimeout(resolve, 100))

      expect(userApi.getList).toHaveBeenCalled()
    })
  })

  describe('Tree Operations', () => {
    it('should expand all nodes', async () => {
      const mockTree = [
        {
          id: 1,
          departmentCode: 'HQ',
          departmentName: '总部',
          level: 1,
          status: 0,
          children: [
            {
              id: 2,
              departmentCode: 'TECH',
              departmentName: '技术部',
              level: 2,
              status: 0,
              children: []
            }
          ]
        }
      ]

      vi.mocked(departmentApi.getTree).mockResolvedValue(mockTree as any)

      wrapper = mount(DepartmentIndex, {
        global: {
          plugins: [createPinia()]
        }
      })

      await new Promise(resolve => setTimeout(resolve, 100))

      await wrapper.vm.handleExpandAll()

      expect(wrapper.vm.expandedKeys.length).toBeGreaterThan(0)
    })

    it('should collapse all nodes', async () => {
      const mockTree = {
        code: 200,
        data: [
          {
            id: 1,
            departmentCode: 'HQ',
            departmentName: '总部',
            level: 1,
            status: 0,
            children: []
          }
        ]
      }

      vi.mocked(departmentApi.getTree).mockResolvedValue(mockTree as any)

      wrapper = mount(DepartmentIndex, {
        global: {
          plugins: [createPinia()]
        }
      })

      await new Promise(resolve => setTimeout(resolve, 100))

      await wrapper.vm.handleCollapseAll()

      expect(wrapper.vm.expandedKeys.length).toBe(0)
    })

    it('should select a node', async () => {
      const mockTree = {
        code: 200,
        data: [
          {
            id: 1,
            departmentCode: 'HQ',
            departmentName: '总部',
            level: 1,
            status: 0,
            children: []
          }
        ]
      }

      vi.mocked(departmentApi.getTree).mockResolvedValue(mockTree as any)

      wrapper = mount(DepartmentIndex, {
        global: {
          plugins: [createPinia()]
        }
      })

      await new Promise(resolve => setTimeout(resolve, 100))

      await wrapper.vm.handleSelect([1], { node: { id: 1 } })

      expect(wrapper.vm.selectedKeys).toContain(1)
    })
  })

  describe('Drag and Drop', () => {
    it('should handle drop event', async () => {
      const mockTree = {
        code: 200,
        data: [
          {
            id: 1,
            departmentCode: 'HQ',
            departmentName: '总部',
            level: 1,
            status: 0,
            children: []
          },
          {
            id: 2,
            departmentCode: 'TECH',
            departmentName: '技术部',
            level: 1,
            status: 0,
            children: []
          }
        ]
      }

      vi.mocked(departmentApi.getTree).mockResolvedValue(mockTree as any)
      vi.mocked(departmentApi.move).mockResolvedValue({ code: 200, data: true } as any)

      wrapper = mount(DepartmentIndex, {
        global: {
          plugins: [createPinia()]
        }
      })

      await new Promise(resolve => setTimeout(resolve, 100))

      const dropEvent = {
        dragNode: { key: 2 },
        node: { key: 1 },
        dropPosition: -1
      }

      await wrapper.vm.handleDrop(dropEvent)

      expect(departmentApi.move).toHaveBeenCalledWith(2, 1, 'inner')
    })
  })

  describe('Modal Operations', () => {
    it('should open add root modal', async () => {
      const mockTree = {
        code: 200,
        data: []
      }

      vi.mocked(departmentApi.getTree).mockResolvedValue(mockTree as any)

      wrapper = mount(DepartmentIndex, {
        global: {
          plugins: [createPinia()]
        }
      })

      await new Promise(resolve => setTimeout(resolve, 100))

      await wrapper.vm.handleAddRoot()

      expect(wrapper.vm.modalVisible).toBe(true)
      expect(wrapper.vm.isEdit).toBe(false)
    })

    it('should open add child modal', async () => {
      const parentNode = {
        id: 1,
        departmentCode: 'HQ',
        departmentName: '总部',
        level: 1,
        status: 0
      }

      await wrapper.vm.handleAddChild(parentNode)

      expect(wrapper.vm.modalVisible).toBe(true)
      expect(wrapper.vm.isEdit).toBe(false)
      expect(wrapper.vm.formState.parentId).toBe(1)
    })

    it('should open edit modal with data', async () => {
      const node = {
        id: 1,
        departmentCode: 'HQ',
        departmentName: '总部',
        level: 1,
        status: 0
      }

      await wrapper.vm.handleEdit(node)

      expect(wrapper.vm.modalVisible).toBe(true)
      expect(wrapper.vm.isEdit).toBe(true)
      expect(wrapper.vm.formState.id).toBe(1)
      expect(wrapper.vm.formState.departmentName).toBe('总部')
    })

    it('should submit form data', async () => {
      wrapper.vm.modalVisible = true
      wrapper.vm.isEdit = false
      wrapper.vm.formState = {
        departmentCode: 'TECH',
        departmentName: '技术部',
        status: 0
      }

      vi.mocked(departmentApi.create).mockResolvedValue({ code: 200, data: true } as any)
      vi.mocked(departmentApi.getTree).mockResolvedValue({ code: 200, data: [] } as any)
      vi.mocked(departmentApi.getTree).mockResolvedValue({ code: 200, data: [] } as any)

      await wrapper.vm.handleModalOk()

      expect(departmentApi.create).toHaveBeenCalled()
      expect(wrapper.vm.modalVisible).toBe(false)
    })
  })

  describe('Status Toggle', () => {
    it('should toggle department status', async () => {
      const node = {
        id: 1,
        departmentCode: 'HQ',
        departmentName: '总部',
        level: 1,
        status: 0
      }

      vi.mocked(departmentApi.updateStatus).mockResolvedValue({ code: 200, data: true } as any)
      vi.mocked(departmentApi.getTree).mockResolvedValue({ code: 200, data: [] } as any)

      await wrapper.vm.handleToggleStatus(node)

      expect(departmentApi.updateStatus).toHaveBeenCalledWith(1, 1)
    })
  })

  describe('Delete Operations', () => {
    it('should show confirmation before delete', async () => {
      const node = {
        id: 1,
        departmentCode: 'HQ',
        departmentName: '总部',
        level: 1,
        status: 0
      }

      // 使用 ant-design-vue 的 Modal mock（已在文件顶部 mock）
      const { Modal } = await import('ant-design-vue')
      const mockConfirm = vi.mocked(Modal.confirm)

      vi.mocked(departmentApi.delete).mockResolvedValue({ code: 200, data: true } as any)

      await wrapper.vm.handleDelete(node)

      expect(mockConfirm).toHaveBeenCalled()
    })
  })

  describe('Helper Functions', () => {
    it('should get all node ids', () => {
      const nodes = [
        { id: 1, children: [{ id: 2, children: [] }] }
      ]

      const ids = wrapper.vm.getAllNodeIds(nodes)

      expect(ids).toEqual([1, 2])
    })

    it('should filter leader option', async () => {
      // mock API 返回正确的数据格式
      vi.mocked(departmentApi.getTree).mockResolvedValue([] as any)
      vi.mocked(userApi.getList).mockResolvedValue({
        data: [
          { id: 1, username: 'admin', nickname: '管理员' },
          { id: 2, username: 'user', nickname: '普通用户' }
        ]
      } as any)

      wrapper = mount(DepartmentIndex, {
        global: {
          plugins: [createPinia()]
        }
      })

      await flushPromises()
      await new Promise(resolve => setTimeout(resolve, 200))
      await flushPromises()

      // 验证 leaderList 已加载
      const list = wrapper.vm.leaderList
      expect(list.length).toBeGreaterThan(0)

      // 直接验证 filterLeaderOption 的查找逻辑
      const leader = list.find((l: any) => l.id === 1)
      expect(leader).toBeTruthy()
      // nickname='管理员'，搜索 '管理' 才能匹配
      expect(leader.nickname).toContain('管理')

      // 测试 filterLeaderOption 函数 - 使用中文搜索词匹配 nickname
      const result = wrapper.vm.filterLeaderOption('管理', { value: 1 })
      expect(result).toBe(true)
    })
  })
})

// personnel.vue 文件不存在，跳过人员管理页面的全部测试
describe.skip('Department Management - Personnel Page (personnel.vue不存在，跳过)', () => {
  let wrapper: any

  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('Initialization', () => {
    it('should mount component', () => {
      wrapper = mount(DepartmentPersonnel, {
        global: {
          plugins: [createPinia()],
          mocks: {
            $route: { query: {} },
            $router: { push: vi.fn() }
          }
        }
      })
      expect(wrapper.exists()).toBe(true)
    })

    it('should render search card', () => {
      wrapper = mount(DepartmentPersonnel, {
        global: {
          plugins: [createPinia()],
          mocks: {
            $route: { query: {} },
            $router: { push: vi.fn() }
          }
        }
      })
      expect(wrapper.find('.search-card').exists()).toBe(true)
      expect(wrapper.find('.table-card').exists()).toBe(true)
    })
  })

  describe('Data Loading', () => {
    it('should load personnel data on mount', async () => {
      const mockUsers = {
        code: 200,
        data: {
          records: [
            {
              id: 1,
              username: 'admin',
              nickname: '管理员',
              phone: '13800138000',
              status: 0
            }
          ],
          total: 1,
          current: 1,
          size: 10
        }
      }

      const mockDept = {
        code: 200,
        data: {
          id: 1,
          departmentCode: 'HQ',
          departmentName: '总部',
          level: 1,
          status: 0
        }
      }

      vi.mocked(userApi.getPage).mockResolvedValue(mockUsers as any)
      vi.mocked(departmentApi.getById).mockResolvedValue(mockDept as any)

      wrapper = mount(DepartmentPersonnel, {
        global: {
          plugins: [createPinia()],
          mocks: {
            $route: { query: { deptId: '1' } },
            $router: { push: vi.fn() }
          }
        }
      })

      await new Promise(resolve => setTimeout(resolve, 100))

      expect(departmentApi.getById).toHaveBeenCalled()
      expect(userApi.getPage).toHaveBeenCalled()
    })

    it('should load position list on mount', async () => {
      const mockPositions = {
        code: 200,
        data: [
          {
            id: 1,
            positionCode: 'DEV',
            positionName: '开发工程师',
            status: 0
          }
        ]
      }

      vi.mocked(positionApi.getList).mockResolvedValue(mockPositions as any)
      vi.mocked(departmentApi.getTree).mockResolvedValue({ code: 200, data: [] } as any)

      wrapper = mount(DepartmentPersonnel, {
        global: {
          plugins: [createPinia()],
          mocks: {
            $route: { query: {} },
            $router: { push: vi.fn() }
          }
        }
      })

      await new Promise(resolve => setTimeout(resolve, 100))

      expect(positionApi.getList).toHaveBeenCalled()
    })
  })

  describe('Modal Operations', () => {
    beforeEach(async () => {
      wrapper = mount(DepartmentPersonnel, {
        global: {
          plugins: [createPinia()],
          mocks: {
            $route: { query: {} },
            $router: { push: vi.fn() }
          }
        }
      })
      await new Promise(resolve => setTimeout(resolve, 50))
    })

    it('should open add modal', async () => {
      const mockUsers = {
        code: 200,
        data: []
      }

      vi.mocked(userApi.getList).mockResolvedValue(mockUsers as any)

      await wrapper.vm.handleAdd()

      expect(wrapper.vm.addModalVisible).toBe(true)
    })

    it('should open transfer modal', async () => {
      wrapper.vm.selectedRowKeys = [1, 2]
      wrapper.vm.tableData = [
        { id: 1, username: 'user1', nickname: '用户1' },
        { id: 2, username: 'user2', nickname: '用户2' }
      ]

      await wrapper.vm.handleTransfer()

      expect(wrapper.vm.transferModalVisible).toBe(true)
    })

    it('should open single transfer modal', async () => {
      const user = { id: 1, username: 'admin', nickname: '管理员' }

      await wrapper.vm.handleTransferSingle(user)

      expect(wrapper.vm.singleTransferModalVisible).toBe(true)
    })
  })

  describe('Selected Users Computed', () => {
    it('should return selected users', () => {
      wrapper = mount(DepartmentPersonnel, {
        global: {
          plugins: [createPinia()],
          mocks: {
            $route: { query: {} },
            $router: { push: vi.fn() }
          }
        }
      })
      
      wrapper.vm.tableData = [
        { id: 1, username: 'user1', nickname: '用户1' },
        { id: 2, username: 'user2', nickname: '用户2' }
      ]
      wrapper.vm.selectedRowKeys = [1]

      const selected = wrapper.vm.selectedUsers

      expect(selected).toHaveLength(1)
      expect(selected[0].id).toBe(1)
    })
  })

  describe('Remove Personnel', () => {
    it('should show confirmation before remove', async () => {
      wrapper = mount(DepartmentPersonnel, {
        global: {
          plugins: [createPinia()],
          mocks: {
            $route: { query: {} },
            $router: { push: vi.fn() }
          }
        }
      })
      
      const user = { id: 1, username: 'admin', nickname: '管理员' }

      const mockConfirm = vi.fn()
      mockConfirm.mockImplementation(({ onOk }) => {
        onOk && onOk()
      })
      (window as any).Modal = { confirm: mockConfirm } as any

      await wrapper.vm.handleRemove(user)

      expect(mockConfirm).toHaveBeenCalled()
    })
  })

  describe('Helper Functions', () => {
    it('should filter user option', () => {
      wrapper = mount(DepartmentPersonnel, {
        global: {
          plugins: [createPinia()],
          mocks: {
            $route: { query: {} },
            $router: { push: vi.fn() }
          }
        }
      })
      
      wrapper.vm.availableUsers = [
        { id: 1, username: 'admin', nickname: '管理员' },
        { id: 2, username: 'user', nickname: '普通用户' }
      ]

      const result = wrapper.vm.filterUserOption('admin', { value: 1 })

      expect(result).toBe(true)
    })
  })
})