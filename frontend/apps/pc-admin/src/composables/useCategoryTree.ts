/**
 * CategoryTreePanel - 通用分类树面板组件
 *
 * 用于所有带分类树的列表页面（商品、客户、供应商等）
 * 左侧显示分类树，支持搜索、新增、编辑、删除分类
 */
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { ComputedRef, Ref } from 'vue'

export interface CategoryNode {
  id: string
  categoryName: string
  categoryCode?: string
  parentId?: string
  productCount?: number
  children?: CategoryNode[]
  [key: string]: any
}

export interface CategoryPanelProps {
  /** 分类树数据 */
  treeData: CategoryNode[]
  /** 加载状态 */
  loading?: boolean
  /** 加载失败状态 */
  error?: boolean
  /** 选中的分类ID */
  selectedId?: string
  /** 面板标题 */
  title?: string
}

export function useCategoryTree(
  categoryApi: {
    getTree: () => Promise<CategoryNode[]>
    create: (data: Partial<CategoryNode>) => Promise<boolean>
    update: (id: string, data: Partial<CategoryNode>) => Promise<boolean>
    delete: (id: string) => Promise<boolean>
  },
  options: {
    /** 选中的分类ID */
    selectedId: Ref<string>
    /** 数据刷新回调 */
    onDataRefresh?: () => void
  }
) {
  const categoryLoading = ref(false)
  const categoryError = ref(false)
  const categoryTree = ref<CategoryNode[]>([])
  const expandedKeys = ref<string[]>([])
  const categorySearch = ref('')
  const categoryModalVisible = ref(false)
  const categoryModalLoading = ref(false)
  const editingCategory = ref<CategoryNode | null>(null)

  const categoryFormRef = ref<any>(null)
  const categoryForm = ref<Partial<CategoryNode>>({
    categoryName: '',
    categoryCode: '',
    parentId: undefined,
    sortOrder: 0,
  })
  const categoryRules = {
    categoryName: [{ required: true, message: '请输入分类名称' }],
  }

  /** 过滤后的分类树 */
  const categoryTreeData: ComputedRef<CategoryNode[]> = computed(() => {
    if (!categorySearch.value) return categoryTree.value
    return filterTree(categoryTree.value, categorySearch.value)
  })

  function filterTree(tree: CategoryNode[], keyword: string): CategoryNode[] {
    return tree
      .map(node => ({
        ...node,
        children: node.children ? filterTree(node.children, keyword) : [],
      }))
      .filter(node =>
        node.categoryName.includes(keyword) ||
        (node.children && node.children.length > 0)
      )
  }

  async function fetchCategoryTree() {
    categoryLoading.value = true
    categoryError.value = false
    try {
      const data = await categoryApi.getTree()
      categoryTree.value = Array.isArray(data) ? data : []
      // 展开第一层
      const firstLevel = categoryTree.value.map(n => n.id)
      if (firstLevel.length > 0) {
        expandedKeys.value = [...firstLevel]
      }
    } catch (e) {
      console.error('[分类树] 加载失败', e)
      categoryError.value = true
    } finally {
      categoryLoading.value = false
    }
  }

  function retryCategoryTree() {
    fetchCategoryTree()
  }

  function onCategorySelect(keys: (string | number)[]) {
    const key = keys[0]
    options.selectedId.value = key != null ? String(key) : '0'
    options.onDataRefresh?.()
  }

  function onExpand(keys: (string | number)[]) {
    expandedKeys.value = keys.map(String)
  }

  function showCategoryModal(category: CategoryNode | null) {
    editingCategory.value = category
    if (category) {
      categoryForm.value = {
        categoryName: category.categoryName,
        categoryCode: category.categoryCode,
        parentId: category.parentId || undefined,
        sortOrder: category.sortOrder || 0,
      }
    } else {
      categoryForm.value = {
        categoryName: '',
        categoryCode: '',
        parentId: options.selectedId.value !== '0' ? options.selectedId.value : undefined,
        sortOrder: 0,
      }
    }
    categoryModalVisible.value = true
  }

  async function handleCategoryOk() {
    try {
      await categoryFormRef.value?.validate()
      categoryModalLoading.value = true
      const payload = { ...categoryForm.value }
      if (editingCategory.value) {
        await categoryApi.update(editingCategory.value.id, payload)
        message.success('更新成功')
      } else {
        await categoryApi.create(payload)
        message.success('新增成功')
      }
      categoryModalVisible.value = false
      await fetchCategoryTree()
      options.onDataRefresh?.()
    } catch (e: unknown) {
      if (e && typeof e === 'object' && 'errorFields' in e) return
      const msg = e instanceof Error ? e.message : '操作失败'
      message.error(msg)
    } finally {
      categoryModalLoading.value = false
    }
  }

  function handleCategoryCancel() {
    categoryModalVisible.value = false
  }

  return {
    // 状态
    categoryLoading,
    categoryError,
    categoryTree,
    categoryTreeData,
    expandedKeys,
    categorySearch,
    categoryModalVisible,
    categoryModalLoading,
    editingCategory,
    categoryFormRef,
    categoryForm,
    categoryRules,
    // 方法
    fetchCategoryTree,
    retryCategoryTree,
    onCategorySelect,
    onExpand,
    showCategoryModal,
    handleCategoryOk,
    handleCategoryCancel,
  }
}
