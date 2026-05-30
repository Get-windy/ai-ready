<script setup lang="ts">
import { computed } from 'vue'
import { Swipe, SwipeItem, Image as VanImage } from 'vant'

interface CategoryItem {
  icon: string
  name: string
  id: number | string
}

const props = defineProps<{
  categories: CategoryItem[]
}>()

const emit = defineEmits<{
  click: [categoryId: string]
}>()

const ITEMS_PER_PAGE = 8

const pages = computed(() => {
  const result: CategoryItem[][] = []
  for (let i = 0; i < props.categories.length; i += ITEMS_PER_PAGE) {
    result.push(props.categories.slice(i, i + ITEMS_PER_PAGE))
  }
  return result
})

const handleCategoryClick = (category: CategoryItem) => {
  emit('click', String(category.id))
}
</script>

<template>
  <div v-if="categories.length > 0" class="category-nav">
    <Swipe
      :loop="false"
      :lazy-render="true"
      indicator-color="#1988fa"
    >
      <SwipeItem v-for="(page, pageIndex) in pages" :key="pageIndex">
        <div class="category-grid">
          <div
            v-for="category in page"
            :key="category.id"
            class="category-item"
            @click="handleCategoryClick(category)"
          >
            <div class="category-icon">
              <VanImage
                v-if="category.icon"
                :src="category.icon"
                fit="cover"
                class="category-img"
                lazy-load
              >
                <template #loading>
                  <div class="icon-placeholder">{{ category.name.charAt(0) }}</div>
                </template>
                <template #error>
                  <div class="icon-placeholder">{{ category.name.charAt(0) }}</div>
                </template>
              </VanImage>
              <div v-else class="icon-placeholder">
                {{ category.name.charAt(0) }}
              </div>
            </div>
            <div class="category-name">{{ category.name }}</div>
          </div>
          <!-- Fill remaining slots to keep alignment -->
          <div
            v-for="n in ITEMS_PER_PAGE - page.length"
            :key="`empty-${n}`"
            class="category-item category-item--empty"
          />
        </div>
      </SwipeItem>
    </Swipe>
  </div>
</template>

<style lang="scss" scoped>
.category-nav {
  margin: 0 12px 12px;
  padding: 12px 0;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;

  :deep(.van-swipe__indicators) {
    bottom: 4px;
  }
}

.category-grid {
  display: flex;
  flex-wrap: wrap;
}

.category-item {
  width: 25%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 4px;
  box-sizing: border-box;
  cursor: pointer;

  &--empty {
    visibility: hidden;
  }
}

.category-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #f0f1f3;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  margin-bottom: 6px;

  .category-img {
    width: 100%;
    height: 100%;
  }

  .icon-placeholder {
    font-size: 20px;
    color: #969799;
    font-weight: 600;
  }
}

.category-name {
  font-size: 12px;
  color: #333;
  text-align: center;
  line-height: 1.2;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
