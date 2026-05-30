<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Swipe, SwipeItem, Image as VanImage } from 'vant'

interface Banner {
  id: number
  image: string
  url: string
}

defineProps<{
  banners: Banner[]
}>()

const router = useRouter()

const handleBannerClick = (url: string) => {
  if (url) {
    router.push(url)
  }
}
</script>

<template>
  <div v-if="banners.length > 0" class="banner-swiper">
    <Swipe
      :autoplay="3000"
      indicator-color="white"
      :lazy-render="true"
      :loop="banners.length > 1"
    >
      <SwipeItem
        v-for="banner in banners"
        :key="banner.id"
        @click="handleBannerClick(banner.url)"
      >
        <VanImage
          :src="banner.image"
          fit="cover"
          class="banner-image"
          :show-loading="true"
          lazy-load
        >
          <template #loading>
            <div class="banner-placeholder" />
          </template>
          <template #error>
            <div class="banner-placeholder">
              <van-icon name="photo-fail" size="32" color="#c8c9cc" />
            </div>
          </template>
        </VanImage>
      </SwipeItem>
    </Swipe>
  </div>
</template>

<style lang="scss" scoped>
.banner-swiper {
  margin: 12px;
  border-radius: 8px;
  overflow: hidden;

  :deep(.van-swipe) {
    border-radius: 8px;
  }

  :deep(.van-swipe__indicators) {
    bottom: 8px;
  }

  :deep(.van-swipe__indicator--active) {
    background-color: #fff;
  }
}

.banner-image {
  width: 100%;
  height: 160px;
  display: block;
}

.banner-placeholder {
  width: 100%;
  height: 160px;
  background-color: #f0f1f3;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
