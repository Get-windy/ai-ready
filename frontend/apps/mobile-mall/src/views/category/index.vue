<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Sidebar, SidebarItem, Grid, GridItem, showLoadingToast, closeToast } from 'vant'
import { api } from '@/api'

const router = useRouter()

const categories = ref<any[]>([])
const subCategories = ref<any[]>([])
const activeCategory = ref(0)

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  
  try {
    const res = await api.product.getCategories()
    categories.value = res.data || [
      { id: 1, name: '电子产品', icon: 'phone' },
      { id: 2, name: '服装鞋帽', icon: 'shirt' },
      { id: 3, name: '家居用品', icon: 'home' },
      { id: 4, name: '食品饮料', icon: 'food' },
      { id: 5, name: '办公用品', icon: 'office' },
      { id: 6, name: '运动户外', icon: 'sport' },
      { id: 7, name: '美妆护肤', icon: 'beauty' },
      { id: 8, name: '母婴用品', icon: 'baby' }
    ]
    
    if (categories.value.length > 0) {
      loadSubCategories(categories.value[0].id)
    }
  } finally {
    closeToast()
  }
})

const loadSubCategories = async (categoryId: number) => {
  try {
    const res = await api.product.getSubCategories(categoryId)
    subCategories.value = res.data || [
      { id: 101, name: '手机通讯', image: '' },
      { id: 102, name: '电脑办公', image: '' },
      { id: 103, name: '数码配件', image: '' },
      { id: 104, name: '智能设备', image: '' },
      { id: 105, name: '家用电器', image: '' },
      { id: 106, name: '影音设备', image: '' }
    ]
  } catch {
    subCategories.value = [
      { id: 101, name: '手机通讯', image: '' },
      { id: 102, name: '电脑办公', image: '' },
      { id: 103, name: '数码配件', image: '' },
      { id: 104, name: '智能设备', image: '' },
      { id: 105, name: '家用电器', image: '' },
      { id: 106, name: '影音设备', image: '' }
    ]
  }
}

const handleCategoryChange = (index: number) => {
  activeCategory.value = index
  if (categories.value[index]) {
    loadSubCategories(categories.value[index].id)
  }
}

const handleSubCategoryClick = (subCategory: any) => {
  router.push(`/category/${subCategory.id}`)
}
</script>

<template>
  <div class="category-page">
    <div class="category-sidebar">
      <Sidebar v-model="activeCategory" @change="handleCategoryChange">
        <SidebarItem 
          v-for="category in categories"
          :key="category.id"
          :title="category.name"
        />
      </Sidebar>
    </div>
    
    <div class="category-content">
      <div class="content-header">
        <div class="header-title">{{ categories[activeCategory]?.name || '分类' }}</div>
      </div>
      
      <Grid :column-num="3" :border="false" class="sub-category-grid">
        <GridItem 
          v-for="sub in subCategories"
          :key="sub.id"
          @click="handleSubCategoryClick(sub)"
        >
          <div class="sub-category-item">
            <div class="sub-category-image">
              <img 
                v-if="sub.image" 
                :src="sub.image" 
                :alt="sub.name"
              />
              <div v-else class="placeholder-image">
                {{ sub.name.charAt(0) }}
              </div>
            </div>
            <div class="sub-category-name">{{ sub.name }}</div>
          </div>
        </GridItem>
      </Grid>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.category-page {
  display: flex;
  height: calc(100vh - 60px);
  background: #f7f8fa;
}

.category-sidebar {
  width: 85px;
  background: #fff;
  overflow-y: auto;
}

.category-content {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  
  .content-header {
    margin-bottom: 12px;
    
    .header-title {
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
  }
}

.sub-category-grid {
  .sub-category-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 8px;
    
    .sub-category-image {
      width: 60px;
      height: 60px;
      border-radius: 8px;
      background: #f0f1f3;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
      
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      
      .placeholder-image {
        font-size: 24px;
        color: #969799;
      }
    }
    
    .sub-category-name {
      margin-top: 8px;
      font-size: 12px;
      color: #333;
      text-align: center;
    }
  }
}
</style>