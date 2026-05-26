<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Grid, GridItem, showLoadingToast, closeToast } from 'vant'
import { api } from '@/api'

const router = useRouter()

const statistics = ref({
  today: {
    deliveries: 0,
    completed: 0,
    failed: 0,
    income: 0,
    avgDuration: 0,
    avgDistance: 0
  },
  week: {
    deliveries: 0,
    completed: 0,
    failed: 0,
    income: 0,
    avgDuration: 0,
    avgDistance: 0
  },
  month: {
    deliveries: 0,
    completed: 0,
    failed: 0,
    income: 0,
    avgDuration: 0,
    avgDistance: 0
  },
  total: {
    deliveries: 0,
    completed: 0,
    failed: 0,
    income: 0,
    avgDuration: 0,
    avgDistance: 0,
    avgRating: 0
  }
})

const loading = ref(false)

onMounted(async () => {
  loadStatistics()
})

const loadStatistics = async () => {
  loading.value = true
  showLoadingToast({ message: '加载中...', forbidClick: true })
  
  try {
    const res = await api.delivery.getStatistics()
    statistics.value = res.data || {
      today: {
        deliveries: 8,
        completed: 7,
        failed: 1,
        income: 120,
        avgDuration: 35,
        avgDistance: 2500
      },
      week: {
        deliveries: 56,
        completed: 52,
        failed: 4,
        income: 840,
        avgDuration: 32,
        avgDistance: 2300
      },
      month: {
        deliveries: 240,
        completed: 220,
        failed: 20,
        income: 3600,
        avgDuration: 30,
        avgDistance: 2200
      },
      total: {
        deliveries: 2560,
        completed: 2400,
        failed: 160,
        income: 38400,
        avgDuration: 28,
        avgDistance: 2100,
        avgRating: 4.8
      }
    }
  } finally {
    loading.value = false
    closeToast()
  }
}

const formatDuration = (minutes: number) => {
  if (minutes < 60) return `${minutes}分钟`
  return `${Math.floor(minutes / 60)}小时${minutes % 60}分钟`
}

const formatDistance = (distance: number) => {
  if (distance < 1000) return `${distance}m`
  return `${(distance / 1000).toFixed(1)}km`
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="statistics-page">
    <NavBar 
      title="配送统计"
      left-arrow
      @click-left="goBack"
    />
    
    <div class="statistics-content">
      <div class="section">
        <div class="section-title">今日统计</div>
        <Grid :column-num="3" :border="false">
          <GridItem>
            <div class="stat-item">
              <div class="stat-value">{{ statistics.today.deliveries }}</div>
              <div class="stat-label">配送单数</div>
            </div>
          </GridItem>
          <GridItem>
            <div class="stat-item">
              <div class="stat-value success">{{ statistics.today.completed }}</div>
              <div class="stat-label">完成单数</div>
            </div>
          </GridItem>
          <GridItem>
            <div class="stat-item">
              <div class="stat-value danger">{{ statistics.today.failed }}</div>
              <div class="stat-label">失败单数</div>
            </div>
          </GridItem>
          <GridItem>
            <div class="stat-item">
              <div class="stat-value income">¥{{ statistics.today.income }}</div>
              <div class="stat-label">今日收入</div>
            </div>
          </GridItem>
          <GridItem>
            <div class="stat-item">
              <div class="stat-value">{{ formatDuration(statistics.today.avgDuration) }}</div>
              <div class="stat-label">平均用时</div>
            </div>
          </GridItem>
          <GridItem>
            <div class="stat-item">
              <div class="stat-value">{{ formatDistance(statistics.today.avgDistance) }}</div>
              <div class="stat-label">平均距离</div>
            </div>
          </GridItem>
        </Grid>
      </div>
      
      <div class="section">
        <div class="section-title">本周统计</div>
        <Grid :column-num="3" :border="false">
          <GridItem>
            <div class="stat-item">
              <div class="stat-value">{{ statistics.week.deliveries }}</div>
              <div class="stat-label">配送单数</div>
            </div>
          </GridItem>
          <GridItem>
            <div class="stat-item">
              <div class="stat-value success">{{ statistics.week.completed }}</div>
              <div class="stat-label">完成单数</div>
            </div>
          </GridItem>
          <GridItem>
            <div class="stat-item">
              <div class="stat-value income">¥{{ statistics.week.income }}</div>
              <div class="stat-label">本周收入</div>
            </div>
          </GridItem>
        </Grid>
      </div>
      
      <div class="section">
        <div class="section-title">本月统计</div>
        <Grid :column-num="3" :border="false">
          <GridItem>
            <div class="stat-item">
              <div class="stat-value">{{ statistics.month.deliveries }}</div>
              <div class="stat-label">配送单数</div>
            </div>
          </GridItem>
          <GridItem>
            <div class="stat-item">
              <div class="stat-value success">{{ statistics.month.completed }}</div>
              <div class="stat-label">完成单数</div>
            </div>
          </GridItem>
          <GridItem>
            <div class="stat-item">
              <div class="stat-value income">¥{{ statistics.month.income }}</div>
              <div class="stat-label">本月收入</div>
            </div>
          </GridItem>
        </Grid>
      </div>
      
      <div class="section total">
        <div class="section-title">累计统计</div>
        <Grid :column-num="3" :border="false">
          <GridItem>
            <div class="stat-item">
              <div class="stat-value">{{ statistics.total.deliveries }}</div>
              <div class="stat-label">总配送单</div>
            </div>
          </GridItem>
          <GridItem>
            <div class="stat-item">
              <div class="stat-value income">¥{{ statistics.total.income }}</div>
              <div class="stat-label">总收入</div>
            </div>
          </GridItem>
          <GridItem>
            <div class="stat-item">
              <div class="stat-value rating">{{ statistics.total.avgRating }}星</div>
              <div class="stat-label">平均评分</div>
            </div>
          </GridItem>
        </Grid>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.statistics-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.statistics-content {
  padding: 12px;
}

.section {
  margin-bottom: 16px;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin-bottom: 12px;
  }
  
  .stat-item {
    text-align: center;
    
    .stat-value {
      font-size: 20px;
      font-weight: 600;
      color: #333;
      
      &.success {
        color: #07c160;
      }
      
      &.danger {
        color: #f44;
      }
      
      &.income {
        color: #f44;
      }
      
      &.rating {
        color: #ff976a;
      }
    }
    
    .stat-label {
      font-size: 12px;
      color: #969799;
      margin-top: 4px;
    }
  }
  
  &.total {
    background: linear-gradient(135deg, #ff976a, #ffb6c1);
    
    .section-title {
      color: #fff;
    }
    
    .stat-item {
      .stat-value {
        color: #fff;
        
        &.income, &.rating {
          color: #fff;
        }
      }
      
      .stat-label {
        color: rgba(255, 255, 255, 0.8);
      }
    }
  }
}
</style>