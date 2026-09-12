<template>
  <!-- 经典形态（资料模块，对标 ql361）：首页 / 上页 / 第(x/y)页 / 下页 / 尾页 / 跳转到 N 页 / 共 N 条记录 / 每页显示 N 行 -->
  <div
    v-if="variant === 'classic'"
    class="standard-pagination classic-pagination"
  >
    <a-button
      size="small"
      :disabled="currentPage <= 1"
      @click="goto(1)"
    >
      <span>首页</span>
    </a-button>
    <a-button
      size="small"
      :disabled="currentPage <= 1"
      @click="goto(currentPage - 1)"
    >
      <span>上页</span>
    </a-button>
    <span class="page-indicator">第({{ currentPage }}/{{ totalPages }})页</span>
    <a-button
      size="small"
      :disabled="currentPage >= totalPages"
      @click="goto(currentPage + 1)"
    >
      <span>下页</span>
    </a-button>
    <a-button
      size="small"
      :disabled="currentPage >= totalPages"
      @click="goto(totalPages)"
    >
      <span>尾页</span>
    </a-button>
    <span class="jump-box">
      跳转到
      <a-input-number
        v-model:value="jumpPage"
        size="small"
        :min="1"
        :max="totalPages"
        :controls="false"
        class="jump-input"
        @press-enter="goto(jumpPage)"
      />
      页
    </span>
    <a-button
      size="small"
      type="primary"
      @click="goto(jumpPage)"
    >
      <span>跳转</span>
    </a-button>
    <span class="total-box">共 {{ numericTotal }} 条记录</span>
    <span class="size-box">
      每页显示
      <a-select
        v-model:value="currentPageSize"
        size="small"
        class="size-select"
        :options="pageSizeOptions.map(v => ({ label: String(v), value: v }))"
        @change="onSizeChangeClassic"
      />
      行
    </span>
  </div>

  <div
    v-else
    class="standard-pagination"
  >
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
  /**
   * 形态：default = antd 翻页器；classic = 资料模块经典形态
   * （首页/上页/第(x/y)页/下页/尾页/跳转到 N 页/共 N 条记录/每页显示 N 行，对标 ql361）
   */
  variant?: 'default' | 'classic'
}>(), {
  current: 1,
  pageSize: 20,
  total: 0,
  pageSizeOptions: () => [10, 20, 50, 100],
  hideOnSinglePage: false,
  variant: 'default'
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
/** classic 形态的跳转输入框 */
const jumpPage = ref(props.current)

/** 总页数（classic 形态用） */
const totalPages = computed(() => {
  const size = Number(currentPageSize.value) || 20
  return Math.max(1, Math.ceil(numericTotal.value / size))
})

/** classic 形态跳页 */
function goto(page: number) {
  const target = Math.min(Math.max(1, Number(page) || 1), totalPages.value)
  if (target === currentPage.value) return
  currentPage.value = target
  jumpPage.value = target
  emit('update:current', target)
  emit('change', target, currentPageSize.value)
}

// 监听外部传入的值变化
watch(() => props.current, (val) => {
  currentPage.value = val
  jumpPage.value = val
})

/** classic 形态：每页行数变化（a-select 只回传 value） */
function onSizeChangeClassic(size: number) {
  currentPageSize.value = size
  currentPage.value = 1
  jumpPage.value = 1
  emit('update:current', 1)
  emit('update:pageSize', size)
  emit('change', 1, size)
}

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

/* ── classic 形态（资料模块，对标 ql361 分页栏） ── */
.classic-pagination {
  gap: 6px;
  font-size: 13px;
  color: #666;
  flex-wrap: wrap;
}
.classic-pagination .page-indicator {
  padding: 0 4px;
  white-space: nowrap;
}
.classic-pagination .jump-box,
.classic-pagination .size-box,
.classic-pagination .total-box {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}
.classic-pagination .total-box {
  margin-left: 8px;
}
.classic-pagination .jump-input {
  width: 52px;
}
.classic-pagination .size-select {
  width: 68px;
}
.classic-pagination :deep(.ant-input-number-input) {
  height: 24px;
  text-align: center;
}
</style>