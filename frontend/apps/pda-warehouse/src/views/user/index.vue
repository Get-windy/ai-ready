<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Cell, CellGroup, Avatar, Button, Dialog } from 'vant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const menuItems = [
  { icon: 'clock-o', title: '作业历史', path: '/user/history' },
  { icon: 'chart-trending-o', title: '作业统计', path: '/user/statistics' },
  { icon: 'setting-o', title: '设置', path: '/user/settings' },
  { icon: 'question-o', title: '帮助中心' }
]

const stats = ref({
  todayTasks: 0,
  todayCompleted: 0,
  totalTasks: 0,
  totalCompleted: 0
})

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    userStore.init()
  }
  
  stats.value = {
    todayTasks: 12,
    todayCompleted: 10,
    totalTasks: 480,
    totalCompleted: 450
  }
})

const handleLogout = () => {
  Dialog.confirm({
    title: '提示',
    message: '确定要退出登录吗？'
  }).then(() => {
    userStore.logout()
    router.push('/login')
  }).catch(() => {})
}

const handleMenuClick = (item: any) => {
  if (item.path) {
    router.push(item.path)
  } else {
    Dialog.alert({ message: '功能开发中' })
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
          {{ userStore.user?.nickname?.charAt(0) || 'W' }}
        </Avatar>
        <div class="user-detail">
          <div class="user-name">{{ userStore.user?.nickname || '仓库员' }}</div>
          <div class="user-level">
            <span class="level-tag">高级仓库员</span>
          </div>
        </div>
        <Button 
          size="small" 
          plain 
          @click="Dialog.alert({ message: '功能开发中' })"
        >
          编辑
        </Button>
      </div>
      
      <div v-else class="login-prompt">
        <Avatar size="60">W</Avatar>
        <div class="prompt-text">
          <div class="prompt-title">登录/注册</div>
          <div class="prompt-desc">登录后查看作业数据</div>
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
      <div class="stats-header">今日作业</div>
      <div class="stats-row">
        <div class="stat-item">
          <div class="stat-value">{{ stats.todayTasks }}</div>
          <div class="stat-label">任务数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value success">{{ stats.todayCompleted }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>
      
      <div class="stats-divider"></div>
      
      <div class="stats-header">累计数据</div>
      <div class="stats-row">
        <div class="stat-item">
          <div class="stat-value">{{ stats.totalTasks }}</div>
          <div class="stat-label">总任务</div>
        </div>
        <div class="stat-item">
          <div class="stat-value success">{{ stats.totalCompleted }}</div>
          <div class="stat-label">已完成</div>
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
  background: linear-gradient(135deg, #07c160, #39b54a);
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
        
        &.success {
          color: #07c160;
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