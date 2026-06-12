<script setup lang="ts">
import { ref } from 'vue'
import { NavBar, Search, Card, Tag, Empty, showLoadingToast, closeToast, showToast } from 'vant'
import { api } from '@/api'


const searchValue = ref('')
const results = ref<any[]>([])
const searched = ref(false)
const loading = ref(false)

const handleSearch = async () => {
  const keyword = searchValue.value.trim()
  if (!keyword) {
    showToast('请输入条码或商品编码')
    return
  }

  loading.value = true
  searched.value = true
  showLoadingToast({ message: '查询中...', forbidClick: true })

  try {
    const res = await api.inventory.query({ productCode: keyword })
    results.value = Array.isArray(res) ? res : (res as any)?.records || []
  } catch (err) {
    console.error('[库存] 查询失败', err)
    results.value = []
    showToast('查询失败')
  } finally {
    loading.value = false
    closeToast()
  }
}
</script>

<template>
  <div class="inventory-page">
    <NavBar title="库存查询" />

    <div class="search-bar">
      <Search
        v-model:value="searchValue"
        placeholder="输入条码或商品编码"
        @search="handleSearch"
        @clear="searched = false; results = []"
      />
    </div>

    <div class="content">
      <div v-if="!searched" class="hint">
        <div class="hint-icon">&#128269;</div>
        <div class="hint-text">输入条码或商品编码查询库存</div>
      </div>

      <Empty
        v-else-if="results.length === 0 && !loading"
        description="未找到相关库存"
      />

      <div v-else class="result-list">
        <Card
          v-for="(item, index) in results"
          :key="index"
          class="inv-card"
        >
          <template #title>
            <div class="card-header">
              <span class="product-name">{{ item.productName }}</span>
              <Tag color="#07c160">{{ item.productCode }}</Tag>
            </div>
          </template>

          <template #desc>
            <div class="card-info">
              <div class="info-row">
                <span class="label">库位:</span>
                <span class="value highlight">{{ item.locationCode || item.location }}</span>
              </div>
              <div class="info-row" v-if="item.batchNo">
                <span class="label">批次:</span>
                <span class="value">{{ item.batchNo }}</span>
              </div>
              <div class="info-row">
                <span class="label">库存:</span>
                <span class="value quantity">{{ item.quantity }}</span>
              </div>
              <div class="info-row">
                <span class="label">可用:</span>
                <span class="value available">{{ item.availableQuantity || item.quantity }}</span>
              </div>
            </div>
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.inventory-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.search-bar {
  background: #fff;
}

.content {
  padding: 12px;

  .hint {
    text-align: center;
    padding: 80px 20px;

    .hint-icon {
      font-size: 48px;
      margin-bottom: 16px;
    }

    .hint-text {
      color: #969799;
      font-size: 14px;
    }
  }
}

.result-list {
  .inv-card {
    margin-bottom: 12px;

    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;

      .product-name {
        font-size: 14px;
        font-weight: 600;
      }
    }

    .card-info {
      .info-row {
        display: flex;
        margin-top: 4px;

        .label {
          width: 50px;
          color: #969799;
        }

        .value {
          color: #333;

          &.highlight {
            color: #1988fa;
            font-weight: 600;
          }

          &.quantity {
            color: #333;
            font-weight: 600;
          }

          &.available {
            color: #07c160;
            font-weight: 600;
          }
        }
      }
    }
  }
}
</style>
