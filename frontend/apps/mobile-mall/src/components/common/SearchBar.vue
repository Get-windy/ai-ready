<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from 'vant'

const props = withDefaults(defineProps<{
  /** 占位文案：首页传热词库的首条热词（mall_keyword），见设计文档 §4.1 */
  placeholder?: string
  /** 底色：默认跟随店铺主题色（--mall-primary，由后台 theme_color 下发） */
  background?: string
}>(), {
  placeholder: '搜索商品',
  background: 'var(--mall-primary, #1988fa)'
})

const emit = defineEmits<{
  search: [keyword: string]
}>()

const router = useRouter()
const searchValue = ref('')

const handleFocus = () => {
  router.push('/search')
}

const handleSearch = (keyword: string) => {
  emit('search', keyword)
  if (keyword) {
    router.push({ path: '/search', query: { keyword } })
  } else {
    router.push('/search')
  }
}
</script>

<template>
  <div class="search-bar-wrapper">
    <Search
      v-model="searchValue"
      shape="round"
      :placeholder="props.placeholder"
      :background="props.background"
      input-background="#fff"
      show-action
      action-text="搜索"
      @focus="handleFocus"
      @search="handleSearch"
    >
      <template #left-icon>
        <van-icon name="search" class="search-icon" />
      </template>
      <template #right-icon>
        <van-icon
          name="volume-o"
          class="voice-icon"
          color="#969799"
        />
      </template>
    </Search>
  </div>
</template>

<style lang="scss" scoped>
.search-bar-wrapper {
  :deep(.van-search__action) {
    color: #fff;
  }

  .search-icon {
    color: #969799;
    font-size: 16px;
  }

  .voice-icon {
    font-size: 18px;
    cursor: pointer;
  }
}
</style>
