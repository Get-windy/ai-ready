<template>
  <div class="standard-pagination">
    <a-pagination
      v-model:current="currentPage"
      v-model:page-size="currentPageSize"
      :total="numericTotal"
      :page-size-options="pageSizeOptions"
      :show-size-changer="true"
      :show-quick-jumper="{ goButton: true }"
      :show-total="(total) => `共 ${total} 条`"
      :hide-on-single-page="false"
      :show-less-items="false"
      :simple="false"
      @change="onPageChange"
      @show-size-change="onPageSizeChange"
    >
      <template #buildOptionText="opt">
        <span>{{ opt.value }}条/页</span>
      </template>
    </a-pagination>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Pagination } from 'ant-design-vue'

defineOptions({ name: 'StandardPagination' })

const props = withDefaults(defineProps<{
  /** 当前页码 */
  current?: number
  /** 每页大小 */
  pageSize?: number
  /** 总条数 */
  total?: number | string
  /** 每页显示条数选项 */
  pageSizeOptions?: number[]
  /** 单页时是否隐藏分页器 */
  hideOnSinglePage?: boolean
}>(), {
  current: 1,
  pageSize: 20,
  total: 0,
  pageSizeOptions: () => [10, 20, 50, 100],
  hideOnSinglePage: false
})

/** total 可能是字符串(Long序列化)，转为数字 */
const numericTotal = computed(() => Number(props.total) || 0)

const emit = defineEmits<{
  'update:current': [number]
  'update:pageSize': [number]
  'change': [number, number]
}>()

const currentPage = ref(props.current)
const currentPageSize = ref(props.pageSize)

// 监听外部传入的值变化
watch(() => props.current, (val) => {
  currentPage.value = val
})

watch(() => props.pageSize, (val) => {
  currentPageSize.value = val
})

function onPageChange(page: number, pageSize: number) {
  currentPage.value = page
  currentPageSize.value = pageSize

  emit('update:current', page)
  emit('update:pageSize', pageSize)
  emit('change', page, pageSize)
}

function onPageSizeChange(current: number, size: number) {
  currentPageSize.value = size
  currentPage.value = 1 // 页码重置为1

  emit('update:current', 1)
  emit('update:pageSize', size)
  emit('change', 1, size)
}
</script>

<style scoped>
.standard-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px 0;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  margin-top: -1px;
  z-index: 1;
  flex-shrink: 0; /* 页面级组件：在 flex 布局中不被压缩 */
}

.standard-pagination :deep(.ant-pagination) {
  margin-right: 16px;
}

.pagination-info {
  color: #999;
  font-size: 13px;
  margin-left: 16px;
}
</style>