<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Cell, CellGroup, Button, Dialog, showDialog } from 'vant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const menuItems = [
  { icon: 'clock-o', title: '配送历史', path: '/user/history' },
  { icon: 'chart-trending-o', title: '配送统计', path: '/user/statistics' },
  { icon: 'cash-o', title: '收入明细', path: '/user/income' },
  { icon: 'comment-o', title: '客户评价', path: '/user/reviews' },
  { icon: 'setting-o', title: '设置', path: '/user/settings' },
  { icon: 'question-o', title: '帮助中心' }
]

const stats = ref({
  todayDeliveries: 0,
  todayIncome: 0,
  totalDeliveries: 0,
  totalIncome: 0
})

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    userStore.init()
  }

  stats.value = {
    todayDeliveries: 8,
    todayIncome: 120,
    totalDeliveries: 256,
    totalIncome: 3840
  }
})

const handleLogout = () => {
  Dialog.confirm({
    title: '提示',
    message: '确定要退出登录吗？'
  }).then(() => {
    userStore.logout()
    router.push('/login')
  }).catch((err) => { console.error('退出登录操作失败:', err) })
}

const handleMenuClick = (item: any) => {
  if (item.path) {
    router.push(item.path)
  } else {
    showDialog({
      title: '帮助中心',
      message: '如有疑问，请联系配送调度中心\n\n联系电话：400-888-0002\n工作时间：周一至周日 8:00-22:00\n\n常见问题：\n1. 如何接单配送？系统自动派单后，在"待配送"列表中点击"开始配送"\n2. 如何处理异常订单？在订单详情页点击"异常上报"提交情况\n3. 如何查看收入明细？进入"收入明细"查看每日/每月收入\n4. 如何联系客户？在订单详情中可拨打客户电话',
      confirmButtonText: '我知道了'
    })
  }
}
</script>

<template>
  <div class="user-page">
    <div class="user-header">
      <div v-if="userStore.isLoggedIn" class="user-info">
        <Avatar 
          :src="userStore.user?.avatar" 
          size="60"
        >
          {{ userStore.user?.nickname?.charAt(0) || 'D' }}
        </Avatar>
        <div class="user-detail">
          <div class="user-name">{{ userStore.user?.nickname || '配送员' }}</div>
          <div class="user-level">
            <span class="level-tag">金牌配送员</span>
          </div>
        </div>
        <Button 
          size="small" 
          plain 
          @click="router.push('/user/profile')"
        >
          编辑
        </Button>
      </div>
      
      <div v-else class="login-prompt">
        <Avatar size="60">D</Avatar>
        <div class="prompt-text">
          <div class="prompt-title">登录/注册</div>
          <div class="prompt-desc">登录后查看配送数据</div>
        </div>
        <Button 
          type="primary" 
          size="small"
          @click="router.push('/login')"
        >
          立即登录
        </Button>
      </div>
    </div>
    
    <div class="stats-card">
      <div class="stats-header">今日配送</div>
      <div class="stats-row">
        <div class="stat-item">
          <div class="stat-value">{{ stats.todayDeliveries }}</div>
          <div class="stat-label">配送单数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value income">¥{{ stats.todayIncome }}</div>
          <div class="stat-label">今日收入</div>
        </div>
      </div>
      
      <div class="stats-divider"></div>
      
      <div class="stats-header">累计数据</div>
      <div class="stats-row">
        <div class="stat-item">
          <div class="stat-value">{{ stats.totalDeliveries }}</div>
          <div class="stat-label">总配送单</div>
        </div>
        <div class="stat-item">
          <div class="stat-value income">¥{{ stats.totalIncome }}</div>
          <div class="stat-label">总收入</div>
        </div>
      </div>
    </div>
    
    <CellGroup inset class="menu-group">
      <Cell 
        v-for="item in menuItems"
        :key="item.title"
        :icon="item.icon"
        :title="item.title"
        is-link
        @click="handleMenuClick(item)"
      />
    </CellGroup>
    
    <div v-if="userStore.isLoggedIn" class="logout-section">
      <Button 
        block 
        type="danger" 
        plain
        @click="handleLogout"
      >
        退出登录
      </Button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.user-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.user-header {
  padding: 20px 16px;
  background: linear-gradient(135deg, #ff976a, #ffb6c1);
  color: #fff;
  
  .user-info {
    display: flex;
    align-items: center;
    
    .user-detail {
      flex: 1;
      margin-left: 16px;
      
      .user-name {
        font-size: 18px;
        font-weight: 600;
      }
      
      .user-level {
        margin-top: 4px;
        
        .level-tag {
          font-size: 12px;
          padding: 2px 8px;
          background: rgba(255, 255, 255, 0.2);
          border-radius: 4px;
        }
      }
    }
  }
  
  .login-prompt {
    display: flex;
    align-items: center;
    
    .prompt-text {
      flex: 1;
      margin-left: 16px;
      
      .prompt-title {
        font-size: 18px;
        font-weight: 600;
      }
      
      .prompt-desc {
        font-size: 14px;
        margin-top: 4px;
        opacity: 0.8;
      }
    }
  }
}

.stats-card {
  margin: -20px 16px 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  
  .stats-header {
    font-size: 14px;
    color: #969799;
    margin-bottom: 12px;
  }
  
  .stats-row {
    display: flex;
    justify-content: space-around;
    
    .stat-item {
      text-align: center;
      
      .stat-value {
        font-size: 24px;
        font-weight: 600;
        color: #333;
        
        &.income {
          color: #f44;
        }
      }
      
      .stat-label {
        font-size: 12px;
        color: #969799;
        margin-top: 4px;
      }
    }
  }
  
  .stats-divider {
    height: 1px;
    background: #eee;
    margin: 16px 0;
  }
}

.menu-group {
  margin: 16px;
}

.logout-section {
  margin: 16px;
}
</style>